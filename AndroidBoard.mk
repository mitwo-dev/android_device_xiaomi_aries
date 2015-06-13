LOCAL_PATH := $(call my-dir)

ifndef CM_BUILD
#-------------------------------------
# linux kernel compile
# -----------------------------------
#
TARGET_KERNEL_SOURCE := kernel/xiaomi/msm8960
ifeq ($(KERNEL_DEFCONFIG),)
    KERNEL_DEFCONFIG := aries-perf-user_defconfig
    include $(TARGET_KERNEL_SOURCE)/AndroidKernel.mk
$(INSTALLED_KERNEL_TARGET): $(TARGET_PREBUILT_KERNEL) | $(ACP) $(TARGET_PREBUILT_KERNEL_INCLUDE)
	$(transform-prebuilt-to-target)
endif
endif

