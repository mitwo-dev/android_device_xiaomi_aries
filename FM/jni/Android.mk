LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
android_hardware_fm.cpp

LOCAL_SHARED_LIBRARIES := \
        libnativehelper \
        libcutils

LOCAL_C_INCLUDES += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include
LOCAL_ADDITIONAL_DEPENDENCIES := $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr

LOCAL_CFLAGS += -include bionic/libc/kernel/arch-arm/asm/posix_types.h
LOCAL_CFLAGS += -include bionic/libc/kernel/arch-arm/asm/byteorder.h
LOCAL_CFLAGS += -include bionic/libc/kernel/common/linux/types.h
LOCAL_CFLAGS += -include bionic/libc/kernel/common/linux/posix_types.h
LOCAL_CFLAGS += -include bionic/libc/kernel/common/linux/socket.h

LOCAL_MODULE := libqcomfm_jni
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
