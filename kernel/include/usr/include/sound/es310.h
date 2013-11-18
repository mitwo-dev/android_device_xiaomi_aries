#ifndef __LINUX_ES310_H
#define __LINUX_ES310_H

#define PRESET_BASE 0x80310000
#define ES310_PRESET_HANDSET_INCALL_NB		    (PRESET_BASE)
#define ES310_PRESET_HEADSET_INCALL_NB 	           (PRESET_BASE + 1)
#define ES310_PRESET_HANDSFREE_REC_NB		    (PRESET_BASE + 2)
#define ES310_PRESET_HANDSFREE_INCALL_NB		    (PRESET_BASE + 3)
#define ES310_PRESET_HANDSET_INCALL_WB	           (PRESET_BASE + 4)
#define ES310_PRESET_HEADSET_INCALL_WB		    (PRESET_BASE + 5)
#define ES310_PRESET_AUDIOPATH_DISABLE               (PRESET_BASE + 6)
#define ES310_PRESET_HANDSFREE_INCALL_WB	    (PRESET_BASE + 7)
#define ES310_PRESET_HANDSET_VOIP_WB		           (PRESET_BASE + 8)
#define ES310_PRESET_HEADSET_VOIP_WB                   (PRESET_BASE + 9)
#define ES310_PRESET_HANDSFREE_REC_WB                 (PRESET_BASE + 10)
#define ES310_PRESET_HANDSFREE_VOIP_WB               (PRESET_BASE + 11)
#define ES310_PRESET_VOICE_RECOGNIZTION_WB       (PRESET_BASE + 12)
#define ES310_PRESET_HEADSET_REC_WB                     (PRESET_BASE + 13)
#define ES310_PRESET_ANALOG_BYPASS	                   (PRESET_BASE + 14)
#define ES310_PRESET_HEADSET_MIC_ANALOG_BYPASS    (PRESET_BASE + 15)

#define ES310_IOCTL_MAGIC ';'
#define ES310_BOOTUP_INIT _IOW(ES310_IOCTL_MAGIC, 1, struct es310img *)
#define ES310_SET_CONFIG _IOW(ES310_IOCTL_MAGIC, 2, unsigned int *)
#define ES310_SET_PARAM _IOW(ES310_IOCTL_MAGIC, 4, struct ES310_config_data *)
#define ES310_SYNC_CMD _IO(ES310_IOCTL_MAGIC, 9)
#define ES310_SLEEP_CMD _IO(ES310_IOCTL_MAGIC, 11)
#define ES310_RESET_CMD _IO(ES310_IOCTL_MAGIC, 12)
#define ES310_WAKEUP_CMD _IO(ES310_IOCTL_MAGIC, 13)
#define ES310_MDELAY _IOW(ES310_IOCTL_MAGIC, 14, unsigned int)
#define ES310_READ_FAIL_COUNT _IOR(ES310_IOCTL_MAGIC, 15, unsigned int *)
#define ES310_READ_SYNC_DONE _IOR(ES310_IOCTL_MAGIC, 16, bool *)
#define ES310_READ_DATA _IOR(ES310_IOCTL_MAGIC, 17, unsigned long *)
#define ES310_WRITE_MSG _IOW(ES310_IOCTL_MAGIC, 18, unsigned long)
#define ES310_SET_PRESET _IOW(ES310_IOCTL_MAGIC, 19, unsigned long)

enum ES310_PathID {
        ES310_PATH_SUSPEND = 0,
        ES310_PATH_HANDSET,
        ES310_PATH_HEADSET,
        ES310_PATH_HANDSFREE,
        ES310_PATH_BACKMIC,
        ES310_PATH_MAX
};

struct ES310_config_data {
	unsigned int len;
	unsigned int unknown;
	unsigned char *data;
};

#ifdef __KERNEL__
struct es310_platform_data {
	uint32_t gpio_es310_reset;
	uint32_t gpio_es310_clk;
	uint32_t gpio_es310_wakeup;
	uint32_t gpio_es310_mic_switch;
	int (*power_on) (int on);
	const char* fw_name;
};
#endif

#endif
