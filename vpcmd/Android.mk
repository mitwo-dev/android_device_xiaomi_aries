LOCAL_PATH := $(call my-dir)

#
# vpcmd
#
include $(CLEAR_VARS)

LOCAL_SRC_FILES := vpcmd.c loopback.c

LOCAL_MODULE := vpcmd
LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES := libc

include $(BUILD_EXECUTABLE)
