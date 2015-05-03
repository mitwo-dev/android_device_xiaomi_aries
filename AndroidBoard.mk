LOCAL_PATH := $(cal my-dir)
#-------------------------------------
# linux kernel compile
# -----------------------------------
#
TARGET_KERNEL_SOURCE := kernel/msm8960
ifeq ($(KERNEL_DEFCONFIG),)
    KERNEL_DEFCONFIG := aries-perf-user_defconfig
    include kernel/msm8960/AndroidKernel.mk
$(INSTALLED_KERNEL_TARGET): $(TARGET_PREBUILT_KERNEL) | $(ACP) $(TARGET_PREBUILT_KERNEL_INCLUDE)
	$(transform-prebuilt-to-target)
endif

