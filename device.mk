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

# This file includes all definitions that apply to ALL aries devices, and
# are also specific to aries devices
#
# Everything in this directory will become public

LOCAL_PATH := device/xiaomi/aries

DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

# Wi-Fi
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/wifi/WCNSS_qcom_cfg.ini:system/etc/wifi/WCNSS_qcom_cfg.ini\

# Audio
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/snd_soc_msm_2x_Fusion3:system/etc/snd_soc_msm/snd_soc_msm_2x_Fusion3

# Media
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/media_profiles.xml:system/etc/media_profiles.xml

# Ramdisk
PRODUCT_PACKAGES += \
    init.target.rc

# Camera
PRODUCT_PACKAGES += \
    camera.msm8960

# syspart selector
PRODUCT_PACKAGES += \
    syspart_select

# Prebuilt kl and kcm keymaps
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/keylayout/apq8064-tabla-snd-card_Button_Jack.kl:system/usr/keylayout/apq8064-tabla-snd-card_Button_Jack.kl \

# These are the hardware-specific features
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.telephony.cdma.xml:system/etc/permissions/android.hardware.telephony.cdma.xml

$(call inherit-product, frameworks/native/build/phone-xhdpi-2048-dalvik-heap.mk)

