#include <utils/Log.h>
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
#include <fcntl.h>	/* File control definitions */
#include <errno.h>	/* Error number definitions */
#include <termios.h>	/* POSIX terminal control definitions */
#include <time.h>

#include "voiceproc.h"

//********************************************************
char headsetMic[3][255] = {
	"amix 'DEC5 MUX' 'ADC2'",
	"amix 'DYN MICBIAS MUX' MB1",
	"amix 'IIR1 INP1 MUX' DEC5"
};

char receiverMic[3][255] = {
	"amix 'DEC5 MUX' 'ADC2'",
	"amix 'DYN MICBIAS MUX' MB2",
	"amix 'IIR1 INP1 MUX' DEC5",
};

//********************************************************
char headsetRX[7][255] = {
	"amix 'RX1 MIX1 INP1' IIR1",
	"amix 'RX2 MIX1 INP1' IIR1",
	"amix 'HPHL DAC Switch' 1",
	"amix 'HPHL Volume' 80%",
	"amix 'HPHR Volume' 80%",
	"amix 'RX1 Digital Volume' 60%",
	"amix 'RX2 Digital Volume' 60%",
};

char receiver[3][255] = {
	"amix 'RX1 MIX1 INP1' IIR1",
	"amix 'RX1 Digital Volume' 74%",
	"amix 'DAC1 Switch' 1",
};

char speaker[6][255] = {
	"amix 'RX3 MIX1 INP1' IIR1",
	"amix 'RX4 DSM MUX' 'DSM_INV'",
	"amix 'RX3 Digital Volume' 74%",
	"amix 'RX4 Digital Volume' 74%",
	"amix 'LINEOUT1 Volume' '80%'",
	"amix 'LINEOUT3 Volume' '80%'",
};

char disable[17][255] = {
	"amix 'RX1 MIX1 INP1' 'ZERO'",
	"amix 'RX2 MIX1 INP1' 'ZERO'",
	"amix 'RX3 MIX1 INP1' 'ZERO'",
	"amix 'RX4 MIX1 INP1' 'ZERO'",
	"amix 'IIR1 INP1 MUX' 'ZERO'",
	"amix 'DEC5 MUX' 'ZERO'",
	"amix 'RX4 DSM MUX' 'ZERO'",
	"amix 'HPHL DAC Switch' 0",
	"amix 'HPHL Volume' '0%'",
	"amix 'HPHR Volume' '0%'",
	"amix 'RX1 Digital Volume' 0%",
	"amix 'RX2 Digital Volume' 0%",
	"amix 'RX3 Digital Volume' 0%",
	"amix 'RX4 Digital Volume' 0%",
	"amix 'LINEOUT1 Volume' '0%'",
	"amix 'LINEOUT3 Volume' '0%'",
	"amix 'DYN MICBIAS MUX' 0",
};

char disable_VoiceProcessing[1][255] = {
	"vpcmd -c 801c0000",
};

void disableVoiceProcessing()
{
	printf("%s: Disable Voice Processsing\n");
	system(disable_VoiceProcessing[0]);
}

int doRouting_Audience_Codec(int mode, int enable)
{
	int rc = 0;
	int retry = 4;
	int fd_codec = -1;
	int dwNewPath = 0;
	int dwNewPreset = ES310_PRESET_HANDSET_INCALL_WB;

	if (enable == 0) {
		dwNewPath = ES310_PATH_SUSPEND;
		goto ROUTE;
	}

	switch (mode) {
	case 1:
		dwNewPath = ES310_PATH_HANDSET;
		break;

	case 2:
		dwNewPath = ES310_PATH_HEADSET;
		break;

	case 3:
		dwNewPath = ES310_PATH_HANDSFREE;
		break;

	case 4:
		dwNewPath = ES310_PATH_BACKMIC;
		break;

	default:
		dwNewPath = ES310_PATH_HANDSET;
		break;
	}

ROUTE:
	printf("doRouting_Audience_Codec: dwNewPath=%d\n", dwNewPath);

	if (fd_codec < 0) {
		fd_codec = open("/dev/audience_es310", O_RDWR);
		if (fd_codec < 0) {
			printf("Cannot open either audience_es310 device (%d)\n", fd_codec);
			return -1;
		}
	}

	retry = 4;
	do {
		printf("ioctl VOICEPROC_SET_CONFIG newPath:0x%x, retry:%d\n", dwNewPath, (4-retry));
		rc = ioctl(fd_codec, VOICEPROC_SET_CONFIG, &dwNewPath);

		if (rc == 0)
			break;
		else
			printf("VOICEPROC_SET_CONFIG rc=%d\n", rc);
	} while (--retry);

       if (enable != 0)
       {
            retry = 4;
            do {
                printf("ioctl VOICEPROC_SET_PRESET newPreset:%d, retry:0x%x\n", dwNewPreset, (4-retry));
                rc = ioctl(fd_codec, VOICEPROC_SET_PRESET, &dwNewPreset);

                if (rc == 0)
                    break;
                else
                    printf("VOICEPROC_SET_PRESET rc=%d\n", rc);
             } while (--retry);
        }

	/*Close driver first incase we need to do audience HW reset when ES310_SET_CONFIG failed.*/

	close(fd_codec);
	fd_codec = -1;

	disableVoiceProcessing();
	return 0;
}

void disableAudience()
{
	int i = 0;

	doRouting_Audience_Codec(0, 0);
	for (i = 0; i < 17; i++)
       {
	    printf("@_@ close WCD9310 all path --> %s\n", disable[i]);
           system(disable[i]);
       }
}

void HandsetMonoRx_HandsetTx_loopback()
{
	int i = 0;
	printf("@_@ loopback 1\n");
	doRouting_Audience_Codec(1, 1);

	for (i = 0; i < 3; i++) {
		printf("@_@ set TX command --> %s\n", receiverMic[i]);
		system(receiverMic[i]);
	}

	for (i = 0; i < 3; i++) {
		printf("@_@ set RX command -->%s\n", receiver[i]);
		system(receiver[i]);
	}
}

void HeadsetStereoRx_HeadsetMonoTX_loopback()
{
	int i = 0;

	printf("@_@ loopback 2\n");
	doRouting_Audience_Codec(2, 1);

	for (i = 0; i < 3; i++) {
		printf("@_@ set TX command --> %s\n", headsetMic[i]);
		system(headsetMic[i]);
	}

	for (i = 0; i < 7; i++) {
		printf("@_@ set RX command -->%s\n", headsetRX[i]);
		system(headsetRX[i]);
	}
}

void BTSCORx_BTSCOTx_loopback()
{
	printf("@_@ loopback 3\n");
}

void HeadsetStereoRx_HandsetMonoTx_loopback()
{
	int i = 0;

	printf("@_@ loopback 4\n");
	doRouting_Audience_Codec(1, 1);
	for (i = 0; i < 3; i++) {
		printf("@_@ set TX command --> %s\n", receiverMic[i]);
		system(receiverMic[i]);
	}

	for (i = 0; i < 7; i++) {
		printf("@_@ set RX command -->%s\n", headsetRX[i]);
		system(headsetRX[i]);
	}
}

void HeadsetStereoRx_AMICSecondary_TX_loopback()
{
	int i = 0;

	printf("@_@ loopback 5\n");
	doRouting_Audience_Codec(4, 1);
	for (i = 0; i < 3; i++) {
		printf("@_@ set TX command --> %s\n", receiverMic[i]);
		system(receiverMic[i]);
	}

	for (i = 0; i < 7; i++) {
		printf("@_@ set RX command -->%s\n", headsetRX[i]);
		system(headsetRX[i]);
	}
}

void HandsetMonoRx_HeadsetMonoTx_loopback()
{
	int i = 0;

	printf("@_@ loopback 6\n");
	doRouting_Audience_Codec(2, 1);

	for (i = 0; i < 3; i++) {
		printf("@_@ set TX command --> %s\n", headsetMic[i]);
		system(headsetMic[i]);
	}

	for (i = 0; i < 3; i++) {
		printf("@_@ set RX command -->%s\n", receiver[i]);
		system(receiver[i]);
	}
}

void SpeakerMonoRX_HeadsetMonoTx_loopback()
{
	int i = 0;

	printf("@_@ loopback 7\n");
	doRouting_Audience_Codec(2, 1);
	for (i = 0; i < 3; i++) {
		printf("@_@ set TX command --> %s\n", headsetMic[i]);
		system(headsetMic[i]);
	}

	for (i = 0; i < 6; i++) {
		printf("@_@ set RX command -->%s\n", speaker[i]);
		system(speaker[i]);
	}
}

