LOCAL_PATH := $(cal my-dir)
#-------------------------------------
# linux kernel compile
# -----------------------------------
#
TARGET_KERNEL_SOURCE := kernel/xiaomi/aries
ifeq ($(KERNEL_DEFCONFIG),)
ifeq ($(TARGET_BUILD_VARIANT),eng)
#    KERNEL_DEFCONFIG := aries-perf-eng_defconfig
    KERNEL_DEFCONFIG := aries-perf-usr_defconfig
else
    KERNEL_DEFCONFIG := aries-perf-usr_defconfig
endif

ifneq ($(BUILD_KERNEL),true)
    include device/xiaomi/aries/kernel/AndroidKernel.mk
else
    include kernel/xiaomi/aries/AndroidKernel.mk
endif

$(INSTALLED_KERNEL_TARGET): $(TARGET_PREBUILT_KERNEL) | $(ACP) $(TARGET_PREBUILT_KERNEL_INCLUDE)
	$(transform-prebuilt-to-target)
endif

