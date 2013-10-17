/*
 * Copyright (c) 2009-2012, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of The Linux Foundation nor
 *            the names of its contributors may be used to endorse or promote
 *            products derived from this software without specific prior written
 *            permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#define LOG_TAG "fmradio"

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"
#include "utils/Log.h"
#include "utils/misc.h"
#include <cutils/properties.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <media/tavarua.h>
#include <linux/videodev2.h>
#include <math.h>

#define RADIO "/dev/radio0"
#define FM_JNI_SUCCESS 0L
#define FM_JNI_FAILURE -1L
#define SEARCH_DOWN 0
#define SEARCH_UP 1
#define TUNE_MULT 16000
#define HIGH_BAND 2
#define LOW_BAND  1
#define CAL_DATA_SIZE 23
#define V4L2_CTRL_CLASS_USER 0x00980000
#define V4L2_CID_PRIVATE_IRIS_SET_CALIBRATION           (V4L2_CTRL_CLASS_USER + 0x92A)
#define V4L2_CID_PRIVATE_TAVARUA_ON_CHANNEL_THRESHOLD   (V4L2_CTRL_CLASS_USER + 0x92B)
#define V4L2_CID_PRIVATE_TAVARUA_OFF_CHANNEL_THRESHOLD  (V4L2_CTRL_CLASS_USER + 0x92C)
#define TX_RT_LENGTH       63
#define WAIT_TIMEOUT 200000 /* 200*1000us */
#define TX_RT_DELIMITER    0x0d
#define PS_LEN    9
#define STD_BUF_SIZE 256
enum search_dir_t {
    SEEK_UP,
    SEEK_DN,
    SCAN_UP,
    SCAN_DN
};


using namespace android;

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_acquireFdNative
        (JNIEnv* env, jobject thiz, jstring path)
{
    int fd;
    int i, retval=0, err;
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    char versionStr[40] = {'\0'};
    int init_success = 0;
    jboolean isCopy;
    v4l2_capability cap;
    const char* radio_path = env->GetStringUTFChars(path, &isCopy);
    if(radio_path == NULL){
        return FM_JNI_FAILURE;
    }
    fd = open(radio_path, O_RDONLY, O_NONBLOCK);
    if(isCopy == JNI_TRUE){
        env->ReleaseStringUTFChars(path, radio_path);
    }
    if(fd < 0){
        return FM_JNI_FAILURE;
    }
    //Read the driver verions
    err = ioctl(fd, VIDIOC_QUERYCAP, &cap);

    ALOGD("VIDIOC_QUERYCAP returns :%d: version: %d \n", err , cap.version );

    if( err >= 0 ) {
       ALOGD("Driver Version(Same as ChipId): %x \n",  cap.version );
       /*Conver the integer to string */
       sprintf(versionStr, "%d", cap.version );
       property_set("hw.fm.version", versionStr);
    } else {
       return FM_JNI_FAILURE;
    }
    /*Set the mode for soc downloader*/
    property_set("hw.fm.mode", "normal");
    /* Need to clear the hw.fm.init firstly */
    property_set("hw.fm.init", "0");
    property_set("ctl.start", "fm_dl");
    sched_yield();
    for(i=0; i<45; i++) {
        property_get("hw.fm.init", value, NULL);
        if (strcmp(value, "1") == 0) {
            init_success = 1;
            break;
        } else {
            usleep(WAIT_TIMEOUT);
        }
    }
    ALOGE("init_success:%d after %f seconds \n", init_success, 0.2*i);
    if(!init_success) {
        property_set("ctl.stop", "fm_dl");
       // close the fd(power down)

       close(fd);
        return FM_JNI_FAILURE;
    }
    return fd;
}

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_closeFdNative
    (JNIEnv * env, jobject thiz, jint fd)
{
    int i = 0;
    int cleanup_success = 0;
    char value = 0, retval =0;

    property_set("ctl.stop", "fm_dl");
    close(fd);
    return FM_JNI_SUCCESS;
}

/********************************************************************
 * Current JNI
 *******************************************************************/

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_getFreqNative
    (JNIEnv * env, jobject thiz, jint fd)
{
    int err;
    struct v4l2_frequency freq;
    freq.type = V4L2_TUNER_RADIO;
    err = ioctl(fd, VIDIOC_G_FREQUENCY, &freq);
    if(err < 0){
      return FM_JNI_FAILURE;
    }
    return ((freq.frequency*1000)/TUNE_MULT);
}

/*native interface */
static jint android_hardware_fmradio_FmReceiverJNI_setFreqNative
    (JNIEnv * env, jobject thiz, jint fd, jint freq)
{
    int err;
    double tune;
    struct v4l2_frequency freq_struct;
    freq_struct.type = V4L2_TUNER_RADIO;
    freq_struct.frequency = (freq*TUNE_MULT/1000);
    err = ioctl(fd, VIDIOC_S_FREQUENCY, &freq_struct);
    if(err < 0){
            return FM_JNI_FAILURE;
    }
    return FM_JNI_SUCCESS;
}

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_setControlNative
    (JNIEnv * env, jobject thiz, jint fd, jint id, jint value)
{
    struct v4l2_control control;
    int i;
    int err;
    ALOGE("id(%x) value: %x\n", id, value);
    control.value = value;

    control.id = id;
    for(i=0;i<3;i++) {
        err = ioctl(fd,VIDIOC_S_CTRL,&control);
        if(err >= 0){
            return FM_JNI_SUCCESS;
        }
    }
    ALOGE("setControl native returned with err %d", err);
    return FM_JNI_FAILURE;
}

static jint android_hardware_fmradio_FmReceiverJNI_SetCalibrationNative
     (JNIEnv * env, jobject thiz, jint fd, jbyteArray buff)
{

    struct v4l2_ext_control ext_ctl;
    char tmp[CAL_DATA_SIZE] = {0x00};
    int err;
    FILE* cal_file;

    cal_file = fopen("/data/app/Riva_fm_cal", "r" );
    if(cal_file != NULL) {
        ext_ctl.id = V4L2_CID_PRIVATE_IRIS_SET_CALIBRATION;
        if (fread(&tmp[0],1,CAL_DATA_SIZE,cal_file) < CAL_DATA_SIZE)
        {
            ALOGE("File read failed");
            return FM_JNI_FAILURE;
        }
        ext_ctl.string = tmp;
        ext_ctl.size = CAL_DATA_SIZE;
        struct v4l2_ext_controls v4l2_ctls;

        v4l2_ctls.ctrl_class = V4L2_CTRL_CLASS_USER,
        v4l2_ctls.count   = 1,
        v4l2_ctls.controls  = &ext_ctl;
        err = ioctl(fd, VIDIOC_S_EXT_CTRLS, &v4l2_ctls );
        if(err >= 0){
            return FM_JNI_SUCCESS;
        }
    }else {
        return FM_JNI_SUCCESS;
    }
  return FM_JNI_SUCCESS;
}
/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_getControlNative
    (JNIEnv * env, jobject thiz, jint fd, jint id)
{
    struct v4l2_control control;
    int err;
    ALOGE("id(%x)\n", id);

    control.id = id;
    err = ioctl(fd,VIDIOC_G_CTRL,&control);
    if(err < 0){
        return FM_JNI_FAILURE;
    }
    return control.value;
}

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_startSearchNative
    (JNIEnv * env, jobject thiz, jint fd, jint dir)
{
    ALOGE("startSearchNative: Issuing the VIDIOC_S_HW_FREQ_SEEK");
    struct v4l2_hw_freq_seek hw_seek;
    int err;
    hw_seek.seek_upward = dir;
    hw_seek.type = V4L2_TUNER_RADIO;
    err = ioctl(fd,VIDIOC_S_HW_FREQ_SEEK,&hw_seek);
    if(err < 0){
        ALOGE("startSearchNative: ioctl failed!!! with error %d\n", err);
        return FM_JNI_FAILURE;
    } else
        ALOGE("startSearchNative: ioctl succedded!!!");
    return FM_JNI_SUCCESS;
}

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_cancelSearchNative
    (JNIEnv * env, jobject thiz, jint fd)
{
    struct v4l2_control control;
    int err;
    control.id=V4L2_CID_PRIVATE_TAVARUA_SRCHON;
    control.value=0;
    err = ioctl(fd,VIDIOC_S_CTRL,&control);
    if(err < 0){
        return FM_JNI_FAILURE;
    }
    return FM_JNI_SUCCESS;
}

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_getRSSINative
    (JNIEnv * env, jobject thiz, jint fd)
{
    struct v4l2_tuner tuner;
    int err;

    tuner.index = 0;
    tuner.signal = 0;
    err = ioctl(fd, VIDIOC_G_TUNER, &tuner);
    if(err < 0){
        return FM_JNI_FAILURE;
    }
    return tuner.signal;
}

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_setBandNative
    (JNIEnv * env, jobject thiz, jint fd, jint low, jint high)
{
    struct v4l2_tuner tuner;
    int err;

    tuner.index = 0;
    tuner.signal = 0;
    tuner.rangelow = low * (TUNE_MULT/1000);
    tuner.rangehigh = high * (TUNE_MULT/1000);
    err = ioctl(fd, VIDIOC_S_TUNER, &tuner);
    if(err < 0){
        return FM_JNI_FAILURE;
    }
    return FM_JNI_SUCCESS;
}

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_getLowerBandNative
    (JNIEnv * env, jobject thiz, jint fd)
{
    struct v4l2_tuner tuner;
    int err;
    tuner.index = 0;

    err = ioctl(fd, VIDIOC_G_TUNER, &tuner);
    if(err < 0){
        ALOGE("low_band value: <%x> \n", tuner.rangelow);
        return FM_JNI_FAILURE;
    }
    return ((tuner.rangelow * 1000)/ TUNE_MULT);
}

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_getUpperBandNative
    (JNIEnv * env, jobject thiz, jint fd)
{
    struct v4l2_tuner tuner;
    int err;
    tuner.index = 0;

    err = ioctl(fd, VIDIOC_G_TUNER, &tuner);
    if(err < 0){
        ALOGE("high_band value: <%x> \n", tuner.rangehigh);
        return FM_JNI_FAILURE;
    }
    return ((tuner.rangehigh * 1000) / TUNE_MULT);
}

static jint android_hardware_fmradio_FmReceiverJNI_setMonoStereoNative
    (JNIEnv * env, jobject thiz, jint fd, jint val)
{

    struct v4l2_tuner tuner;
    int err;

    tuner.index = 0;
    err = ioctl(fd, VIDIOC_G_TUNER, &tuner);

    if(err < 0)
        return FM_JNI_FAILURE;

    tuner.audmode = val;
    err = ioctl(fd, VIDIOC_S_TUNER, &tuner);

    if(err < 0)
        return FM_JNI_FAILURE;

    return FM_JNI_SUCCESS;

}



/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_getBufferNative
 (JNIEnv * env, jobject thiz, jint fd, jbooleanArray buff, jint index)
{
    int err;
    jboolean isCopy;
    struct v4l2_requestbuffers reqbuf;
    struct v4l2_buffer v4l2_buf;
    memset(&reqbuf, 0, sizeof (reqbuf));
    enum v4l2_buf_type type = V4L2_BUF_TYPE_PRIVATE;
    reqbuf.type = V4L2_BUF_TYPE_PRIVATE;
    reqbuf.memory = V4L2_MEMORY_USERPTR;
    jboolean *bool_buffer = env->GetBooleanArrayElements(buff,&isCopy);
    memset(&v4l2_buf, 0, sizeof (v4l2_buf));
    v4l2_buf.index = index;
    v4l2_buf.type = type;
    v4l2_buf.length = STD_BUF_SIZE;
    v4l2_buf.m.userptr = (unsigned long)bool_buffer;
    err = ioctl(fd,VIDIOC_DQBUF,&v4l2_buf);
    if(err < 0){
        /* free up the memory in failure case*/
        env->ReleaseBooleanArrayElements(buff, bool_buffer, 0);
        return FM_JNI_FAILURE;
    }

    /* Always copy buffer and free up the memory */
    env->ReleaseBooleanArrayElements(buff, bool_buffer, 0);

    return v4l2_buf.bytesused;
}

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_getRawRdsNative
 (JNIEnv * env, jobject thiz, jint fd, jbooleanArray buff, jint count)
{

    return (read (fd, buff, count));

}

/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_setNotchFilterNative(JNIEnv * env, jobject thiz,jint fd, jint id, jboolean aValue)
{
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    int init_success = 0,i;
    char notch[20] = {0x00};
    struct v4l2_control control;
    int err;
    /*Enable/Disable the WAN avoidance*/
    property_set("hw.fm.init", "0");
    if (aValue)
       property_set("hw.fm.mode", "wa_enable");
    else
       property_set("hw.fm.mode", "wa_disable");

    property_set("ctl.start", "fm_dl");
    sched_yield();
    for(i=0; i<10; i++) {
       property_get("hw.fm.init", value, NULL);
       if (strcmp(value, "1") == 0) {
          init_success = 1;
          break;
       } else {
          usleep(WAIT_TIMEOUT);
       }
    }
    ALOGE("init_success:%d after %f seconds \n", init_success, 0.2*i);

    property_get("notch.value", notch, NULL);
    ALOGE("Notch = %s",notch);
    if (!strncmp("HIGH",notch,strlen("HIGH")))
        control.value = HIGH_BAND;
    else if(!strncmp("LOW",notch,strlen("LOW")))
        control.value = LOW_BAND;
    else
        control.value = 0;

    ALOGE("Notch value : %d", control.value);
    control.id = id;
    err = ioctl(fd, VIDIOC_S_CTRL,&control );
    if(err < 0){
          return FM_JNI_FAILURE;
    }
    return FM_JNI_SUCCESS;
}


/* native interface */
static jint android_hardware_fmradio_FmReceiverJNI_setAnalogModeNative(JNIEnv * env, jobject thiz, jboolean aValue)
{
    int i=0;
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    char firmwareVersion[80];

    /*Enable/Disable Analog Mode FM*/
    property_set("hw.fm.init", "0");
    if (aValue) {
        property_set("hw.fm.isAnalog", "true");
    } else {
        property_set("hw.fm.isAnalog", "false");
    }
    property_set("hw.fm.mode","config_dac");
    property_set("ctl.start", "fm_dl");
    sched_yield();
    for(i=0; i<10; i++) {
       property_get("hw.fm.init", value, NULL);
       if (strcmp(value, "1") == 0) {
          return 1;
       } else {
          usleep(WAIT_TIMEOUT);
       }
    }

    return 0;
}




/*
 * Interfaces added for Tx
*/

/*native interface */
static jint android_hardware_fmradio_FmReceiverJNI_setPTYNative
    (JNIEnv * env, jobject thiz, jint fd, jint pty)
{
    ALOGE("->android_hardware_fmradio_FmReceiverJNI_setPTYNative\n");
    struct v4l2_control control;

    control.id = V4L2_CID_RDS_TX_PTY;
    control.value = pty & MASK_PTY;

    int err;
    err = ioctl(fd, VIDIOC_S_CTRL,&control );
    if(err < 0){
            return FM_JNI_FAILURE;
    }
    return FM_JNI_SUCCESS;
}

static jint android_hardware_fmradio_FmReceiverJNI_setPINative
    (JNIEnv * env, jobject thiz, jint fd, jint pi)
{
    ALOGE("->android_hardware_fmradio_FmReceiverJNI_setPINative\n");

    struct v4l2_control control;

    control.id = V4L2_CID_RDS_TX_PI;
    control.value = pi & MASK_PI;

    int err;
    err = ioctl(fd, VIDIOC_S_CTRL,&control );
    if(err < 0){
		ALOGE("->pty native failed");
            return FM_JNI_FAILURE;
    }

    return FM_JNI_SUCCESS;
}

static jint android_hardware_fmradio_FmReceiverJNI_startRTNative
    (JNIEnv * env, jobject thiz, jint fd, jstring radio_text, jint count )
{
    ALOGD("->android_hardware_fmradio_FmReceiverJNI_startRTNative\n");

    struct v4l2_ext_control ext_ctl;
    struct v4l2_ext_controls v4l2_ctls;

    int err = 0;
    jboolean isCopy = false;
    char* rt_string1 = NULL;
    char* rt_string = (char*)env->GetStringUTFChars(radio_text, &isCopy);
    if(rt_string == NULL ){
        ALOGE("RT string is not valid \n");
        return FM_JNI_FAILURE;
    }

    rt_string1 = (char*) malloc(TX_RT_LENGTH + 1);
    if (rt_string1 == NULL) {
       ALOGE("out of memory \n");
       env->ReleaseStringUTFChars(radio_text, rt_string);
       return FM_JNI_FAILURE;
    }
    memset (rt_string1, 0, TX_RT_LENGTH + 1);
    memcpy(rt_string1, rt_string, count);

    if(count < TX_RT_LENGTH)
       rt_string1[count] = TX_RT_DELIMITER;

    ext_ctl.id     = V4L2_CID_RDS_TX_RADIO_TEXT;
    ext_ctl.string = rt_string1;
    ext_ctl.size   = strlen(rt_string1) + 1;

    /* form the ctrls data struct */
    v4l2_ctls.ctrl_class = V4L2_CTRL_CLASS_FM_TX,
    v4l2_ctls.count      = 1,
    v4l2_ctls.controls   = &ext_ctl;


    err = ioctl(fd, VIDIOC_S_EXT_CTRLS, &v4l2_ctls );
    env->ReleaseStringUTFChars(radio_text, rt_string);
    if (rt_string1 != NULL) {
        free(rt_string1);
        rt_string1 = NULL;
    }
    if(err < 0){
        ALOGE("VIDIOC_S_EXT_CTRLS for start RT returned : %d\n", err);
        return FM_JNI_FAILURE;
    }

    ALOGD("->android_hardware_fmradio_FmReceiverJNI_startRTNative is SUCCESS\n");
    return FM_JNI_SUCCESS;
}

static jint android_hardware_fmradio_FmReceiverJNI_stopRTNative
    (JNIEnv * env, jobject thiz, jint fd )
{
    ALOGE("->android_hardware_fmradio_FmReceiverJNI_stopRTNative\n");
    int err;
    struct v4l2_control control;
    control.id = V4L2_CID_PRIVATE_TAVARUA_STOP_RDS_TX_RT;

    err = ioctl(fd, VIDIOC_S_CTRL , &control);
    if(err < 0){
            return FM_JNI_FAILURE;
    }
    ALOGE("->android_hardware_fmradio_FmReceiverJNI_stopRTNative is SUCCESS\n");
    return FM_JNI_SUCCESS;
}

static jint android_hardware_fmradio_FmReceiverJNI_startPSNative
    (JNIEnv * env, jobject thiz, jint fd, jstring buff, jint count )
{
    ALOGD("->android_hardware_fmradio_FmReceiverJNI_startPSNative\n");

    struct v4l2_ext_control ext_ctl;
    struct v4l2_ext_controls v4l2_ctls;
    int l;
    int err = 0;
    jboolean isCopy = false;
    char *ps_copy = NULL;
    const char *ps_string = NULL;

    ps_string = env->GetStringUTFChars(buff, &isCopy);
    if (ps_string != NULL) {
        l = strlen(ps_string);
        if ((l > 0) && ((l + 1) == PS_LEN)) {
             ps_copy = (char *)malloc(sizeof(char) * PS_LEN);
             if (ps_copy != NULL) {
                 memset(ps_copy, '\0', PS_LEN);
                 memcpy(ps_copy, ps_string, (PS_LEN - 1));
             } else {
                 env->ReleaseStringUTFChars(buff, ps_string);
                 return FM_JNI_FAILURE;
             }
        } else {
             env->ReleaseStringUTFChars(buff, ps_string);
             return FM_JNI_FAILURE;
        }
    } else {
        return FM_JNI_FAILURE;
    }

    env->ReleaseStringUTFChars(buff, ps_string);

    ext_ctl.id     = V4L2_CID_RDS_TX_PS_NAME;
    ext_ctl.string = ps_copy;
    ext_ctl.size   = PS_LEN;

    /* form the ctrls data struct */
    v4l2_ctls.ctrl_class = V4L2_CTRL_CLASS_FM_TX,
    v4l2_ctls.count      = 1,
    v4l2_ctls.controls   = &ext_ctl;

    err = ioctl(fd, VIDIOC_S_EXT_CTRLS, &v4l2_ctls);
    if (err < 0) {
        ALOGE("VIDIOC_S_EXT_CTRLS for Start PS returned : %d\n", err);
        free(ps_copy);
        return FM_JNI_FAILURE;
    }

    ALOGD("->android_hardware_fmradio_FmReceiverJNI_startPSNative is SUCCESS\n");
    free(ps_copy);

    return FM_JNI_SUCCESS;
}

static jint android_hardware_fmradio_FmReceiverJNI_stopPSNative
    (JNIEnv * env, jobject thiz, jint fd)
{
    ALOGE("->android_hardware_fmradio_FmReceiverJNI_stopPSNative\n");
    struct v4l2_control control;
    control.id = V4L2_CID_PRIVATE_TAVARUA_STOP_RDS_TX_PS_NAME;

    int err;
    err = ioctl(fd, VIDIOC_S_CTRL , &control);
    if(err < 0){
            return FM_JNI_FAILURE;
    }
    ALOGE("->android_hardware_fmradio_FmReceiverJNI_stopPSNative is SUCCESS\n");
    return FM_JNI_SUCCESS;
}

static jint android_hardware_fmradio_FmReceiverJNI_configureSpurTable
    (JNIEnv * env, jobject thiz, jint fd)
{
    ALOGD("->android_hardware_fmradio_FmReceiverJNI_configureSpurTable\n");
    int retval = 0;
    struct v4l2_control control;

    control.id = V4L2_CID_PRIVATE_UPDATE_SPUR_TABLE;
    retval = ioctl(fd, VIDIOC_S_CTRL, &control);
    if (retval < 0) {
            ALOGE("configureSpurTable: Failed to Write the SPUR Table\n");
            return FM_JNI_FAILURE;
    } else
            ALOGD("configureSpurTable: SPUR Table Configuration successful\n");

    return FM_JNI_SUCCESS;
}

static jint android_hardware_fmradio_FmReceiverJNI_setPSRepeatCountNative
    (JNIEnv * env, jobject thiz, jint fd, jint repCount)
{
    ALOGE("->android_hardware_fmradio_FmReceiverJNI_setPSRepeatCountNative\n");

    struct v4l2_control control;

    control.id = V4L2_CID_PRIVATE_TAVARUA_TX_SETPSREPEATCOUNT;
    control.value = repCount & MASK_TXREPCOUNT;

    int err;
    err = ioctl(fd, VIDIOC_S_CTRL,&control );
    if(err < 0){
            return FM_JNI_FAILURE;
    }

    ALOGE("->android_hardware_fmradio_FmReceiverJNI_setPSRepeatCountNative is SUCCESS\n");
    return FM_JNI_SUCCESS;
}

static jint android_hardware_fmradio_FmReceiverJNI_setTxPowerLevelNative
    (JNIEnv * env, jobject thiz, jint fd, jint powLevel)
{
    ALOGE("->android_hardware_fmradio_FmReceiverJNI_setTxPowerLevelNative\n");

    struct v4l2_control control;

    control.id = V4L2_CID_TUNE_POWER_LEVEL;
    control.value = powLevel;

    int err;
    err = ioctl(fd, VIDIOC_S_CTRL,&control );
    if(err < 0){
            return FM_JNI_FAILURE;
    }

    ALOGE("->android_hardware_fmradio_FmReceiverJNI_setTxPowerLevelNative is SUCCESS\n");
    return FM_JNI_SUCCESS;
}

/*
 * JNI registration.
 */
static JNINativeMethod gMethods[] = {
        /* name, signature, funcPtr */
        { "acquireFdNative", "(Ljava/lang/String;)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_acquireFdNative},
        { "closeFdNative", "(I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_closeFdNative},
        { "getFreqNative", "(I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_getFreqNative},
        { "setFreqNative", "(II)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_setFreqNative},
        { "getControlNative", "(II)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_getControlNative},
        { "setControlNative", "(III)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_setControlNative},
        { "startSearchNative", "(II)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_startSearchNative},
        { "cancelSearchNative", "(I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_cancelSearchNative},
        { "getRSSINative", "(I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_getRSSINative},
        { "setBandNative", "(III)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_setBandNative},
        { "getLowerBandNative", "(I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_getLowerBandNative},
        { "getUpperBandNative", "(I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_getUpperBandNative},
        { "getBufferNative", "(I[BI)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_getBufferNative},
        { "setMonoStereoNative", "(II)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_setMonoStereoNative},
        { "getRawRdsNative", "(I[BI)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_getRawRdsNative},
       { "setNotchFilterNative", "(IIZ)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_setNotchFilterNative},
        { "startRTNative", "(ILjava/lang/String;I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_startRTNative},
        { "stopRTNative", "(I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_stopRTNative},
        { "startPSNative", "(ILjava/lang/String;I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_startPSNative},
        { "stopPSNative", "(I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_stopPSNative},
        { "setPTYNative", "(II)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_setPTYNative},
        { "setPINative", "(II)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_setPINative},
        { "setPSRepeatCountNative", "(II)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_setPSRepeatCountNative},
        { "setTxPowerLevelNative", "(II)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_setTxPowerLevelNative},
       { "setAnalogModeNative", "(Z)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_setAnalogModeNative},
        { "SetCalibrationNative", "(I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_SetCalibrationNative},
        { "configureSpurTable", "(I)I",
            (void*)android_hardware_fmradio_FmReceiverJNI_configureSpurTable},

};

int register_android_hardware_fm_fmradio(JNIEnv* env)
{
        return jniRegisterNativeMethods(env, "qcom/fmradio/FmReceiverJNI", gMethods, NELEM(gMethods));
}

jint JNI_OnLoad(JavaVM *jvm, void *reserved)
{
  JNIEnv *e;
  int status;
   ALOGE("FM : loading QCOMM FM-JNI\n");
  
   if(jvm->GetEnv((void **)&e, JNI_VERSION_1_6)) {
       ALOGE("JNI version mismatch error");
      return JNI_ERR;
   }

   if ((status = register_android_hardware_fm_fmradio(e)) < 0) {
       ALOGE("jni adapter service registration failure, status: %d", status);
      return JNI_ERR;
   }
   return JNI_VERSION_1_6;
}
