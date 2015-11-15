#
# Copyright (C) 2011 The Android Open-Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Inherit from msm8960-common
-include device/xiaomi/msm8960-common/BoardConfigCommon.mk

LOCAL_PATH := device/xiaomi/aries

# Vendor Init
TARGET_UNIFIED_DEVICE := true
TARGET_INIT_VENDOR_LIB := libinit_msm
TARGET_LIBINIT_DEFINES_FILE := device/xiaomi/aries/init/init_aries.c

TARGET_BOARD_INFO_FILE       := $(LOCAL_PATH)/board-info.txt

TARGET_RELEASETOOLS_EXTENSIONS := $(LOCAL_PATH)

# Audio
BOARD_HAVE_AUDIENCE_ES310 := true
BOARD_HAVE_NEW_QCOM_CSDCLIENT   := true
BOARD_HAVE_CSD_FAST_CALL_SWITCH := true

# Bluetooth
BOARD_BLUETOOTH_BDROID_BUILDCFG_INCLUDE_DIR := $(LOCAL_PATH)/bluetooth

# Camera
USE_DEVICE_SPECIFIC_CAMERA := true

# Filesystem
TARGET_USERIMAGES_USE_EXT4         := true
BOARD_BOOTIMAGE_PARTITION_SIZE     := 15728640 # 15MB
BOARD_RECOVERYIMAGE_PARTITION_SIZE := 15728640 # 15MB
BOARD_SYSTEMIMAGE_PARTITION_SIZE   := 536870912 # 512MB
BOARD_USERDATAIMAGE_PARTITION_SIZE := 3758096384 # 3.5GB
BOARD_PERSISTIMAGE_PARTITION_SIZE  := 8388608 # 8MB
BOARD_CACHEIMAGE_PARTITION_SIZE    := 402653184 # 384MB
BOARD_FLASH_BLOCK_SIZE             := 131072 # (BOARD_KERNEL_PAGESIZE * 64)

-include vendor/xiaomi/aries/BoardConfigVendor.mk

