#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <fcntl.h>
#include <stdint.h>
#include <sys/mman.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <linux/types.h>
#include <linux/ioctl.h>
#include <fcntl.h>   /* File control definitions */
#include <errno.h>   /* Error number definitions */
#include <termios.h> /* POSIX terminal control definitions */
#include <time.h>
#include <utils/Log.h>

#include "voiceproc.h"

#define BUFSIZE_UART 1024
#define VOICEPROC_MAX_FW_SIZE	(32 * 4096)
struct termios ti;

/* Paramaters to set the baud rate */
#define FLOW_CTL	0x0001
#define BOTHER		0x00001000
#define ARM_NCCS	19

#define TCGETS2		_IOR('T',0x2A, struct termios2)
#define TCSETS2		_IOW('T',0x2B, struct termios2)

/* Termios2 structure for setting the Custom baud rate */
struct termios2 {
	tcflag_t c_iflag;	/* input mode flags */
	tcflag_t c_oflag;	/* output mode flags */
	tcflag_t c_cflag;	/* control mode flags */
	tcflag_t c_lflag;	/* local mode flags */
	cc_t c_line;		/* line discipline */
	cc_t c_cc[ARM_NCCS];	/* control characters */
	speed_t c_ispeed;	/* input speed */
	speed_t c_ospeed;	/* output speed */
};

#define DEBUG 0
#define CMD_UART_SYNC (0x00)
#define CMD_UART_BOOT (0x01)
#define CMD_I2C_SYNC (0x80000000)

#define UART_DEVICE_NAME_LEN 64
static char uart_device_name[UART_DEVICE_NAME_LEN] = "/dev/ttyHS2";

static void setHigherBaudrate(int uart_fd, int baud)
{
	struct termios2 ti2;

	/* Flush non-transmitted output data,
	 * non-read input data or both
	 */
	tcflush(uart_fd, TCIFLUSH);

	/* Set the UART flow control */

	ti.c_cflag |= 1;

	/* ti.c_cflag |= (CLOCAL | CREAD | CSTOPB); */
	/*	ti.c_cflag &= ~(CRTSCTS | PARENB); */

	/*
	 * Enable the receiver and set local mode + 2 STOP bits
	 */
	ti.c_cflag |= (CLOCAL | CREAD | CSTOPB);
	/* 8 data bits */
	ti.c_cflag &= ~CSIZE;
	ti.c_cflag |= CS8;
	/* diable HW flow control and parity check */
	ti.c_cflag &= ~(CRTSCTS | PARENB);

	/* choose raw input */
	ti.c_lflag &= ~(ICANON | ECHO);
	/* choose raw output */
	ti.c_oflag &= ~OPOST;
	/* ignore break condition, CR and parity error */
	ti.c_iflag |= (IGNBRK | IGNPAR);
	ti.c_iflag &= ~(IXON | IXOFF | IXANY);
	ti.c_cc[VMIN] = 0;
	ti.c_cc[VTIME] = 10;

	/*
	 * Set the parameters associated with the UART
	 * The change will occur immediately by using TCSANOW
	 */
	if (tcsetattr(uart_fd, TCSANOW, &ti) < 0) {
		printf("Can't set port settings\n");
		return;
	}

	tcflush(uart_fd, TCIFLUSH);

	/* Set the actual baud rate */
	ioctl(uart_fd, TCGETS2, &ti2);
	ti2.c_cflag &= ~CBAUD;
	ti2.c_cflag |= BOTHER;
	ti2.c_ospeed = baud;
	ti2.c_ispeed = baud;
	ioctl(uart_fd, TCSETS2, &ti2);
}

int uartStreaming(int fd, char **argv)
{
	int ret = 0, i, ch=0;
	int img_size = 0;
	int read_size = 0;
	int uart_fd;
	unsigned int tmp;
	FILE *stream;
	unsigned char *buf;
	unsigned char msg[4] = {0x80, 0x28, 0x00, 0x00};
	unsigned char start[4] = {0x80, 0x25, 0x00, 0x01};
	unsigned char stop[4] = {0x80, 0x25, 0x00, 0x00};
	unsigned char temp[4] = {0x00, 0x00, 0x00, 0x00};
	unsigned int cur_read_bytes = 0;

	stream = fopen("/data/stream", "wb");
	if (stream == NULL) {
		printf("unable to open file /data/stream\n");
		return -1;
	}

	sscanf(argv[0], "%x", &tmp);
	msg[3] = (unsigned char)tmp;
	for (i = 0; i < 8; i++) {
		ch += (tmp>>i) & 0x00000001;
	}

	if (ch > 4) {
		printf("streaming engin cannot support more than 4 channnels\n");
		fclose(stream);
		return -1;
	}

	sscanf(argv[1], "%d", &tmp);
	img_size = 5 * tmp * 8000 * 2;

	buf = (unsigned char*) malloc(img_size);
	if (buf == NULL) {
		printf( "unable allocate memory for streaming\n");
		fclose(stream);
		return -1;
	}

	/* init uart port */
	uart_fd = open(uart_device_name, O_RDWR | O_NOCTTY | O_NDELAY);
	if (uart_fd < 0) {
		printf("fail to open uart port %s\n", uart_device_name);
		free(buf);
	        fclose(stream);
	        return -1;
	} else {
		printf("open uart port %s  ok\n", uart_device_name);
		fcntl(uart_fd, F_SETFL, 0);
		setHigherBaudrate(uart_fd, 921600);
	}

	/* setup streaming */
	ret = ioctl(fd, VOICEPROC_WRITE_MSG, msg);
	if (ret < 0) {
		printf("ioctl VOICEPROC_WRITE_MSG (0x%.2x%.2x%.2x%.2x) error, ret = %d\n", msg[0],msg[1],msg[2],msg[3],ret);
		ret = -1;
		goto errors;
	} else
		printf("ioctl VOICEPROC_SET_CONFIG (0x%.2x%.2x%.2x%.2x) OK\n", msg[0],msg[1],msg[2],msg[3]);

	usleep(20000);

	ret = ioctl(fd, VOICEPROC_READ_DATA, temp);
	if (ret < 0) {
		printf("ioctl VOICEPROC_READ_DATA error, ret = %d\n", ret);
		ret = -1;
		goto errors;
	} else
		printf("ioctl VOICEPROC_READ_DATA (0x%.2x%.2x%.2x%.2x) OK\n", temp[0],temp[1],temp[2],temp[3]);
	/* setup streaming end */

	/* kick off streaming */
	ret = ioctl(fd, VOICEPROC_WRITE_MSG, start);
	if (ret < 0) {
		printf("ioctl VOICEPROC_WRITE_MSG (0x%.2x%.2x%.2x%.2x) error, ret = %d\n", start[0],start[1],start[2],start[3],ret);
		ret = -1;
		goto errors;
	} else
		printf("ioctl VOICEPROC_SET_CONFIG (0x%.2x%.2x%.2x%.2x) OK\n", start[0],start[1],start[2],start[3]);

	usleep(20000);

	ret = ioctl(fd, VOICEPROC_READ_DATA, temp);
	if (ret < 0) {
		printf("ioctl VOICEPROC_READ_DATA error, ret = %d\n", ret);
		ret = -1;
		goto errors;
	} else
		printf("ioctl VOICEPROC_READ_DATA (0x%.2x%.2x%.2x%.2x) OK\n", temp[0],temp[1],temp[2],temp[3]);
	/* done kick off streaming */

	/* streaming now */
	read_size = 0;
	i = 0;
	while (i < img_size) {
                cur_read_bytes = (img_size - i) < BUFSIZE_UART ? (img_size - i) : BUFSIZE_UART;
		ret = read(uart_fd, buf+i, cur_read_bytes);
		if (ret == -1)
			printf("Error, voiceproc uart read: %s\n", strerror(errno));
		else if (ret != cur_read_bytes)
			printf("voiceproc_uart_read %d byte, expected %d byte\n", ret, cur_read_bytes);

		read_size += ret;
		i += ret;
	}
	if (read_size != img_size)
	printf("Error, UART readCnt %d != img_size %d\n", read_size, img_size);

	/* stop streaming */
	ret = ioctl(fd, VOICEPROC_WRITE_MSG, stop);
	if (ret < 0) {
		printf("ioctl VOICEPROC_WRITE_MSG (0x%.2x%.2x%.2x%.2x) error, ret = %d\n", stop[0],stop[1],stop[2],stop[3],ret);
		ret = -1;
		goto errors;
	} else
		printf("ioctl VOICEPROC_SET_CONFIG (0x%.2x%.2x%.2x%.2x) OK\n", stop[0],stop[1],stop[2],stop[3]);

	usleep(20000);

	ret = ioctl(fd, VOICEPROC_READ_DATA, temp);
	if (ret < 0) {
		printf("ioctl VOICEPROC_READ_DATA error, ret = %d\n", ret);
		ret = -1;
		goto errors;
	} else
		printf("ioctl VOICEPROC_READ_DATA (0x%.2x%.2x%.2x%.2x) OK\n", temp[0],temp[1],temp[2],temp[3]);
	/* done stop streaming */

	fwrite(buf, sizeof(char), read_size, stream);

errors:
	free(buf);
	fclose(stream);
	close(uart_fd);

	return ret;
}

int sendDownloadCmd(int uart_fd)
{
	unsigned char respBuffer = 0xcc, tmp;
	int nretry = 10, rc;
	int BytesWritten, readCnt;

	tmp = 0x00;
	BytesWritten = write(uart_fd, &tmp, 1);
	if (BytesWritten == -1)
		printf("error writing synccmd to comm port: %s\n", strerror(errno));
	else
		printf("Uart_write BytesWritten = %i\n", BytesWritten);

	/* Spec: Host wait 1ms after send sync byte, and before read it back */
	usleep(1000);
	readCnt = read(uart_fd, &respBuffer, 1);
	if (readCnt == -1)
		printf("error reading bootcmd from comm port: %s\n", strerror(errno));
	else
		printf("readCnt = %d, respBuffer = %.2x\n", readCnt, respBuffer);
	usleep(1000);

	/* Spec: Host must send the Boot Command within 150ms */
	tmp = 0x01;
	BytesWritten = write(uart_fd, &tmp, 1);
	if (BytesWritten == -1)
		printf("error writing bootcmd to comm port: %s\n", strerror(errno));
	else
		printf("Uart_write BytesWritten = %i\n", BytesWritten);

	/* Spec: Host wait 1ms after send boot byte, and before read it back */
	usleep(1000);
	readCnt = read(uart_fd, &respBuffer, 1);
	if (readCnt == -1)
		printf("error reading bootcmd from comm port: %s\n", strerror(errno));
	else
		printf("readCnt = %d, respBuffer = %.2x\n", readCnt, respBuffer);

	if (respBuffer == 1)
		return 1;

	return 0;
}

int uartSendBinaryFile(int uart_fd, const char* img)
{
	int ret = -1, write_size;
	int i = 0;
	printf("voiceproc_uart_sendImg %s\n", img);
	struct voiceproc_img fwimg;
	char char_tmp = 0;
	unsigned char local_vpimg_buf[VOICEPROC_MAX_FW_SIZE], *ptr = local_vpimg_buf;
	int rc = 0, fw_fd = -1;
	ssize_t nr;
	size_t remaining;
	struct stat fw_stat;

	fw_fd = open(img, O_RDONLY);
	if (fw_fd < 0) {
		printf("Fail to open %s\n", img);
		goto ld_img_error;
	}

	rc = fstat(fw_fd, &fw_stat);
	if (rc < 0) {
		printf("Cannot stat file %s: %s\n", img, strerror(errno));
		goto ld_img_error;
	}

	remaining = (int)fw_stat.st_size;

	printf("Firmware %s size %d\n", img, remaining);

	if (remaining > sizeof(local_vpimg_buf)) {
		printf("File %s size %d exceeds internal limit %d\n",
			 img, remaining, sizeof(local_vpimg_buf));
		goto ld_img_error;
	}

	while (remaining) {
		nr = read(fw_fd, ptr, remaining);
		if (nr < 0) {
			printf("Error reading firmware: %s\n", strerror(errno));
			goto ld_img_error;
		} else if (!nr) {
			if (remaining)
				printf("EOF reading firmware %s while %d bytes remain\n",
					 img, remaining);
			break;
		}
		remaining -= nr;
		ptr += nr;
	}

	close (fw_fd);
	fw_fd = -1;

	fwimg.buf = local_vpimg_buf;
	fwimg.img_size = (int)(fw_stat.st_size - remaining);
	printf("voiceproc_uart_sendImg firmware Total %d bytes\n", fwimg.img_size);
	i = 0;
	write_size = 0;

	while (i < fwimg.img_size) {
		ret = write(uart_fd, fwimg.buf+i,
			(fwimg.img_size - i) < BUFSIZE_UART ? (fwimg.img_size-i) : BUFSIZE_UART);
		if (ret == -1)
			printf("Error, voiceproc uart write: %s\n", strerror(errno));
		else
			printf(".");

		write_size += ret;
		i += BUFSIZE_UART;
	}
	printf("\n");
	if (write_size != fwimg.img_size)
		printf("Error, UART writeCnt %d != img_size %d\n", write_size, fwimg.img_size);
	else
		printf("UART writeCnt is %d verus img_size is %d\n", write_size, fwimg.img_size);

ld_img_error:
	if (fw_fd >= 0)
		close(fw_fd);
	return rc;
}

int uart_load_binary(int fd, char **argv)
{
	int  ret = 0;
	int  uart_fd;

	ret = ioctl(fd, VOICEPROC_RESET_CMD);
	if (!ret)
		printf("voiceproc_reset VOICEPROC_RESET_CMD OK\n");
	else {
		printf("voiceproc_reset VOICEPROC_RESET_CMD error %s\n", strerror(errno));
		return -1;
	}

	/* init uart port */
	uart_fd = open(uart_device_name, O_RDWR | O_NOCTTY | O_NDELAY);
	if (uart_fd < 0) {
		printf("fail to open uart port %s\n", uart_device_name);
		return -1;
	}
	printf("open uart port %s  ok\n", uart_device_name);
	fcntl(uart_fd, F_SETFL, 0);

	/* one stage download */
	setHigherBaudrate(uart_fd, 500000);
	printf("using baudrate 500000\n");

	/* reset voice processor */
	usleep(10000);

	ret = sendDownloadCmd(uart_fd);
	if (!ret) {
		printf("error sending download command, abort. \n");
		return -1;
	}

	uartSendBinaryFile(uart_fd, argv[0]);
	/* deinit uart port */
	close(uart_fd);

	return ret;
}

int uart_load_binary_high_speed(int fd, char **argv)
{
	int  ret = 0;
	int  uart_fd;
	struct termios options;
	struct termios2 options2;

	ret = ioctl(fd, VOICEPROC_RESET_CMD);
	if (!ret)
		printf("voiceproc_reset VOICEPROC_RESET_CMD OK\n");
	else {
		printf("voiceproc_reset VOICEPROC_RESET_CMD error %s\n", strerror(errno));
		return -1;
	}

	/* init uart port */
	uart_fd = open(uart_device_name, O_RDWR | O_NOCTTY | O_NDELAY);
	if (uart_fd < 0) {
		printf("fail to open uart port %s\n", uart_device_name);
		return -1;
	}
	printf("open uart port %s  ok\n", uart_device_name);
	fcntl(uart_fd, F_SETFL, 0);

	/* First stage download */
	setHigherBaudrate(uart_fd, 28800);

	/* reset voice processor */
	usleep(10000);

	ret = sendDownloadCmd(uart_fd);
	if (!ret) {
		printf("error sending 1st download command, abort. \n");
		close(uart_fd);
		return -1;
	}

	/* Spec: Host must send the initial image1 within 150ms */
	uartSendBinaryFile(uart_fd, argv[0]);
	printf("Send init image done\n");

	/* Second stage download */
	/* Set the actual baud rate, make sure to drain the buffer */
	if (tcgetattr(uart_fd, &options) < 0) {
		printf("Can't get port settings\n");
		close(uart_fd);
		return -1;
	} else if (tcsetattr(uart_fd, TCSADRAIN, &options) < 0) {
		printf("Can't set port settings\n");
		close(uart_fd);
		return -1;
	}
	if (ioctl(uart_fd, TCGETS2, &options2) < 0) {
		printf("Can't get port settings 2\n");
		close(uart_fd);
		return -1;
	} else {
		options2.c_cflag &= ~CBAUD;
		options2.c_cflag |= BOTHER;
		options2.c_ospeed = 3000000;
		options2.c_ispeed = 3000000;
		if (ioctl(uart_fd, TCSETS2, &options2) < 0) {
			printf("Can't set port settings 2\n");
			close(uart_fd);
			return -1;
		}
	}
	tcflush(uart_fd, TCIFLUSH);

	printf("using baudrate 3000000, ret=%d\n", ret);
	/* Spec: The Host switches its UART baud rate to the faster rate, and waits 10ms */
	usleep(10000);

	printf("Send 2nd Download command\n");
	ret = sendDownloadCmd(uart_fd);
	if (!ret) {
		printf("error sending 2nd download command, abort. \n");
		close(uart_fd);
		return -1;
	}

	uartSendBinaryFile(uart_fd, argv[1]);
	/* Spec: After the last byte of Image2 is sent, the Host must wait 1ms */
	usleep(1000);

	/* deinit uart port */
	close(uart_fd);

	return ret;
}

int i2c_load_binary(int fd, char **argv)
{
	int  ret = 0;
	int  uart_fd;

	ret = ioctl(fd, VOICEPROC_RESET_CMD);
	if (!ret)
		printf("voiceproc_reset VOICEPROC_RESET_CMD OK\n");
	else {
		printf("voiceproc_reset VOICEPROC_RESET_CMD error %s\n", strerror(errno));
		return -1;
	}

	return ret;
}

int load_macro(int fd, char **argv)
{
	FILE *macro;
	unsigned char msg[2], msg2[4], tmp;
	int buff = 1, i = 1, ret;

	macro = fopen(argv[0], "r");
	if (macro == NULL) {
		printf( "unable to open file %s\n", argv[0]);
		return -1;
	}

	do {
		tmp = (char)buff;
		if (tmp == 0x3b)
			i = 0;
		if (i == 1) {
			if (fscanf(macro, "%x", msg)) {
				msg2[0] = msg[1];
				msg2[1] = msg[0];
				if (fscanf(macro, "%x", msg)) {
					msg2[2] = msg[1];
					msg2[3] = msg[0];
					ret = ioctl(fd, VOICEPROC_WRITE_MSG, msg2);
					if (ret < 0) {
						printf("ioctl VOICEPROC_WRITE_MSG (0x%.2x%.2x%.2x%.2x) error, ret = %d\n", msg2[0],msg2[1],msg2[2],msg2[3],ret);
						fclose(macro);
						return -1;
					} else
						printf("ioctl VOICEPROC_WRITE_MSG (0x%.2x%.2x%.2x%.2x) OK\n", msg2[0],msg2[1],msg2[2],msg2[3]);

					usleep(20000);

					ret = ioctl(fd, VOICEPROC_READ_DATA, msg2);
					if (ret < 0) {
						printf("ioctl VOICEPROC_READ_DATA error, ret = %d\n", ret);
						fclose(macro);
						return -1;
					} else
						printf("ioctl VOICEPROC_READ_DATA (0x%.2x%.2x%.2x%.2x) OK\n", msg2[0],msg2[1],msg2[2],msg2[3]);
				}
			}
		}
		if (tmp == 0x0a)
			i = 1;
	} while ((buff = fgetc(macro)) != EOF);

	fclose(macro);
	return 0;
}

int get_build_label(int fd)
{
	unsigned char firstChar[4] = {0x80, 0x20, 0x00, 0x00};
	unsigned char nextChar[4] = {0x80, 0x21, 0x00, 0x00};
	unsigned char msg[4] = {0x00, 0x00, 0x00, 0x01};
	int ret;

	/* get 1st build label char */
	ret = ioctl(fd, VOICEPROC_WRITE_MSG, firstChar);
	if (ret < 0) {
		printf("ioctl VOICEPROC_WRITE_MSG (0x%.2x%.2x%.2x%.2x) error, ret = %d\n", firstChar[0],firstChar[1],firstChar[2],firstChar[3],ret);
		return -1;
	}

	usleep(20000);

	ret = ioctl(fd, VOICEPROC_READ_DATA, msg);
	if (ret < 0) {
		printf("ioctl VOICEPROC_READ_DATA error, ret = %d\n", ret);
		return -1;
	}
	printf("%c", msg[3]);

	while (msg[3]) {
		/* get next build label char */
		ret = ioctl(fd, VOICEPROC_WRITE_MSG, nextChar);
		if (ret < 0) {
			printf("\n");
			printf("ioctl VOICEPROC_WRITE_MSG (0x%.2x%.2x%.2x%.2x) error, ret = %d\n", nextChar[0],nextChar[1],nextChar[2],nextChar[3],ret);
			return -1;
		}
		usleep(20000);

		ret = ioctl(fd, VOICEPROC_READ_DATA, msg);
		if (ret < 0) {
			printf("\n");
			printf("ioctl VOICEPROC_READ_DATA error, ret = %d\n", ret);
			return -1;
		}
		printf("%c", msg[3]);
	}
	printf("\n");

	return 0;
}

int send_cmd(int fd, char **argv)
{
	int ret, tmp;
	char *temp;
	unsigned char msg[4] = {0x80, 0x00, 0x00, 0x00}, i;

	tmp = strlen(argv[0]);
	if (tmp == 10)
		temp = argv[0] + 2;
	else if (tmp == 8)
		temp = argv[0];
	else {
		printf("invalid 4 byte Hex format, must be in 0xAAAAAAAA or AAAAAAAA formats\n");
		return -1;
	}

	sscanf(temp, "%x",msg);
	i = msg[0];
	msg[0] = msg[3];
	msg[3] = i;
	i = msg[1];
	msg[1] = msg[2];
	msg[2] = i;

	ret = ioctl(fd, VOICEPROC_WRITE_MSG, msg);
	if (ret < 0) {
		printf("ioctl VOICEPROC_WRITE_MSG (0x%.2x%.2x%.2x%.2x) error, ret = %d\n", msg[0],msg[1],msg[2],msg[3],ret);
		return -1;
	} else
		printf("ioctl VOICEPROC_WRITE_MSG (0x%.2x%.2x%.2x%.2x) OK\n", msg[0],msg[1],msg[2],msg[3]);

	usleep(20000);

	ret = ioctl(fd, VOICEPROC_READ_DATA, msg);
	if (ret < 0) {
		printf("ioctl VOICEPROC_READ_DATA error, ret = %d\n", ret);
		return -1;
	} else
		printf("ioctl VOICEPROC_READ_DATA (0x%.2x%.2x%.2x%.2x) OK\n", msg[0],msg[1],msg[2],msg[3]);

	return 0;
}

void show_usage(void)
{
	printf("Support commands:\n");
	printf(" -c COMMAND\n");
	printf("	To issue low level command\n");
	printf("	COMMAND is a hex string, such as 0x80000000\n");
	printf(" -bl\n");
	printf("	To get firmware build label\n");
	printf(" -m MACRO\n");
	printf("	To load macro from a file\n");
	printf(" -l FIRMWARE_PATH [ -d UART_PATH ]\n");
	printf("	To load firmware through a uart device\n");
	printf("	FIRMWARE_PATH: the location of the firmware image\n");
	printf("	UART_PATH: which uart to use\n");
	printf(" -li FIRMWARE_PATH\n");
	printf("	To load firmware through i2c device\n");
	printf(" -us CODE [ -d UART_PATH ]\n");
	printf("	Do uart streaming\n");
	printf("	CODE: hex value to indicate channels\n");
	printf("	UART_PATH: which uart to use\n");
	printf(" -sleep\n");
	printf("	Put the chip into sleep mode\n");
	printf(" -wakeup\n");
	printf("	Wakeup the chip from sleep mode\n");
	printf(" -loopback\n");
	printf("   Loopback testing\n");
}

int main(int argc, char **argv)
{
	int ret = 0;
	int fd;
	char **curr_argv;

	fd = open("/dev/audience_es310", O_RDONLY|O_NONBLOCK, 0);
	if (fd < 0) {
		printf("cannot open VOICEPROC driver %d\n", fd);
		return -1;
	} else {
		printf("open VOICEPROC driver return %d\n", fd);
	}

	argc--;
	argv++;
	if (argc > 0) {
		if (!strcmp(argv[0],"-c")) {
			argc--;
			argv++;
			if (argc == 0) {
				printf("send cmd: requires a hex cmd\n");
				show_usage();
				goto out;
			}
			while (argc > 0) {
				if (send_cmd(fd, argv) < 0) {
					printf("send cmd error, abort\n");
					goto out;
				}
				argc--;
				argv++;
			}
		} else if (!strcmp(argv[0],"-bl")) {
			if (get_build_label(fd) < 0) {
				printf("get_build_label error, abort\n");
				goto out;
			}
		} else if (!strcmp(argv[0],"-loopback")) {
			if (atoi(argv[1]) == 0) {
				printf("@_@ close all audio path");
				disableAudience();
				goto out;
			}

			printf("@_@ close all audio path");

			disableAudience();
			switch (atoi(argv[1])) {
			case 1:
				HandsetMonoRx_HandsetTx_loopback();
				break;
			case 2:
				HeadsetStereoRx_HeadsetMonoTX_loopback();
				break;
			case 3:
				BTSCORx_BTSCOTx_loopback();
				break;
			case 4:
				HeadsetStereoRx_HandsetMonoTx_loopback();
				break;
			case 5:
				HeadsetStereoRx_AMICSecondary_TX_loopback();
				break;
			case 6:
				HandsetMonoRx_HeadsetMonoTx_loopback();
				break;
			case 7:
				SpeakerMonoRX_HeadsetMonoTx_loopback();
				break;
			default:
				printf("Please help to enter the correct testin case");
				break;
			}
		} else if (!strcmp(argv[0],"-m")) {
			argc--;
			argv++;
			if (argc == 0) {
				printf("load macro: requires a path\n");
				show_usage();
				goto out;
			} else if (load_macro(fd, argv) < 0) {
				printf("load_macro error, abort\n");
				goto out;
			}
		} else if (!strcmp(argv[0],"-l")) {
			argc--;
			argv++;
			if (argc == 0) {
				printf("load binary: requires a path\n");
				show_usage();
				goto out;
			}
			curr_argv = argv;
			argv++;
			if ((argc == 3) && !strncmp(argv[0], "-d", 2)) {
				memset(uart_device_name, 0, UART_DEVICE_NAME_LEN);
				strncpy(uart_device_name, argv[1], UART_DEVICE_NAME_LEN - 1);
			}
			if (uart_load_binary(fd, curr_argv) < 0) {
				printf("load_binary error, abort\n");
				goto out;
			}
		} else if (!strcmp(argv[0],"-lh")) {
			argc--;
			argv++;
			if (argc < 2) {
				printf("load binary: requires two firmwar images\n");
				show_usage();
				goto out;
			}
			if (uart_load_binary_high_speed(fd, argv) < 0) {
				printf("load_binary error, abort\n");
				goto out;
			}
		} else if (!strcmp(argv[0],"-li")) {
			argc--;
			argv++;
			if (argc == 0) {
				printf("load binary: requires a path\n");
				show_usage();
				goto out;
			} else if (i2c_load_binary(fd, argv) < 0) {
				printf("load_binary error, abort\n");
				goto out;
			}
		} else if (!strcmp(argv[0],"-us")) {
			argc--;
			argv++;
			if (argc == 0) {
				printf("streaming setup requires a hex setup code: 0x01 pri, 0x02 sec, 0x04 clean"
					" speech, 0x08 FE in, 0x10 FE out, or the add result for multi-channels.\n");
				show_usage();
				goto out;
			}
			argc--;
			if (argc == 0) {
				printf("streaming setup requires time field in seconds\n");
				show_usage();
				goto out;
			}
			argc--;
			curr_argv = argv;
			argv++;
			if ((argc == 2) && !strncmp(argv[0], "-d", 2)) {
				argv++;
				memset(uart_device_name, 0, UART_DEVICE_NAME_LEN);
				strncpy(uart_device_name, argv[1], UART_DEVICE_NAME_LEN - 1);
			}
			if (uartStreaming(fd, curr_argv) < 0) {
				printf("uartSreaming error, abort\n");
				goto out;
			}
		} else if (!strcmp(argv[0],"-sleep")) {
			ret = ioctl(fd, VOICEPROC_SLEEP_CMD, NULL);
			if (ret < 0)
				printf("ioctl sleep error, ret = %d\n", ret);
			else
				printf("ioctl sleep OK\n");
		} else if (!strcmp(argv[0],"-wakeup")) {
			ret = ioctl(fd, VOICEPROC_WAKEUP_CMD, NULL);
			if (ret < 0)
				printf("ioctl wakeup error, ret = %d\n", ret);
			else
				printf("ioctl wakeup OK\n");
		} else {
			printf("%s switch is not supported\n", argv[0]);
			show_usage();
		}
	}

out:
	close(fd);
	return 0;
}
