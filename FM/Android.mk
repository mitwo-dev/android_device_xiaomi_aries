ifneq ($(filter aries,$(TARGET_DEVICE)),)
ifeq ($(QCOM_FM_ENABLED),true)

LOCAL_PATH:= $(call my-dir)
LOCAL_DIR_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, qcom/fmradio)
LOCAL_JNI_SHARED_LIBRARIES := libqcomfm_jni

LOCAL_MODULE:= qcom.fmradio

include $(BUILD_JAVA_LIBRARY)

include $(LOCAL_PATH)/jni/Android.mk
LOCAL_PATH := $(LOCAL_DIR_PATH)
include $(LOCAL_PATH)/fmapp2/Android.mk
LOCAL_PATH := $(LOCAL_DIR_PATH)
include $(LOCAL_PATH)/FMRecord/Android.mk
endif
endif
