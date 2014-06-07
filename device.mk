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

DEVICE_PACKAGE_OVERLAYS := device/xiaomi/aries/overlay

# This device is xhdpi.  However the platform doesn't
# currently contain all of the bitmaps at xhdpi density so
# we do this little trick to fall back to the hdpi version
# if the xhdpi doesn't exist.
PRODUCT_AAPT_CONFIG := normal hdpi xhdpi
PRODUCT_AAPT_PREF_CONFIG := xhdpi

# Dualboot
PRODUCT_COPY_FILES += \
	device/xiaomi/aries/recovery/mount_ext4_default.sh:recovery/root/res/dualboot/mount_ext4_default.sh \
	device/xiaomi/aries/recovery/mount_ext4_tdb.sh:recovery/root/res/dualboot/mount_ext4_tdb.sh

# Charger
PRODUCT_COPY_FILES += \
    device/xiaomi/aries/chargeonlymode:root/sbin/chargeonlymode

# Live Wallpapers
PRODUCT_PACKAGES += \
    LiveWallpapers \
    LiveWallpapersPicker \
    VisualizationWallpapers \
    librs_jni

# RIL
PRODUCT_COPY_FILES += \
    device/xiaomi/aries/libril.so:system/lib/libril.so

PRODUCT_COPY_FILES += \
    device/xiaomi/aries/WCNSS_cfg.dat:system/etc/firmware/wlan/prima/WCNSS_cfg.dat \
    device/xiaomi/aries/WCNSS_qcom_cfg.ini:system/etc/firmware/wlan/prima/WCNSS_qcom_cfg.ini \
    device/xiaomi/aries/WCNSS_qcom_wlan_nv.bin:system/etc/firmware/wlan/prima/WCNSS_qcom_wlan_nv.bin

ifneq ($(BUILD_KERNEL),true)
PRODUCT_COPY_FILES += \
    device/xiaomi/aries/kernel/exfat.ko:system/lib/modules/exfat.ko \
    device/xiaomi/aries/kernel/radio-iris-transport.ko:system/lib/modules/radio-iris-transport.ko \
    device/xiaomi/aries/kernel/wlan.ko:system/lib/modules/wlan.ko \
    device/xiaomi/aries/kernel/adsprpc.ko:system/lib/modules/adsprpc.ko
endif

PRODUCT_COPY_FILES += \
    device/xiaomi/aries/configs/snd_soc_msm_2x_Fusion3:system/etc/snd_soc_msm/snd_soc_msm_2x_Fusion3 \
    device/xiaomi/aries/configs/audio_policy.conf:system/etc/audio_policy.conf \
    device/xiaomi/aries/configs/audio_effects.conf:system/etc/audio_effects.conf

PRODUCT_COPY_FILES += \
    device/xiaomi/aries/thermald_h.conf:system/etc/thermald_h.conf \
    device/xiaomi/aries/thermald_l.conf:system/etc/thermald_l.conf

PRODUCT_COPY_FILES += \
    device/xiaomi/aries/configs/init.qcom.rc:root/init.qcom.rc \
    device/xiaomi/aries/configs/init.qcom.usb.rc:root/init.qcom.usb.rc \
    device/xiaomi/aries/configs/init.recovery.qcom.rc:root/init.recovery.qcom.rc \
    device/xiaomi/aries/configs/fstab.qcom:root/fstab.qcom \
    device/xiaomi/aries/configs/ueventd.qcom.rc:root/ueventd.qcom.rc \
    device/xiaomi/aries/configs/init.qcom.class_core.sh:root/init.qcom.class_core.sh \
    device/xiaomi/aries/configs/init.qcom.class_main.sh:root/init.qcom.class_main.sh \
    device/xiaomi/aries/configs/media_profiles.xml:system/etc/media_profiles.xml \
    device/xiaomi/aries/configs/media_codecs.xml:system/etc/media_codecs.xml \
    device/xiaomi/aries/configs/init.qcom.usb.sh:root/init.qcom.usb.sh \
    device/xiaomi/aries/configs/init.qcom.sh:root/init.qcom.sh

PRODUCT_COPY_FILES += \
    device/xiaomi/aries/init.qcom.bt.sh:system/etc/init.qcom.bt.sh \
    device/xiaomi/aries/init.qcom.fm.sh:system/etc/init.qcom.fm.sh \
    device/xiaomi/aries/init.qcom.post_boot.sh:system/etc/init.qcom.post_boot.sh

PRODUCT_COPY_FILES += \
    device/xiaomi/aries/bootanimation.zip:system/media/bootanimation.zip

# Prebuilt kl and kcm keymaps
PRODUCT_COPY_FILES += \
    device/xiaomi/aries/atmel_mxt_ts.kl:system/usr/keylayout/atmel_mxt_ts.kl \
    device/xiaomi/aries/apq8064-tabla-snd-card_Button_Jack.kl:system/usr/keylayout/apq8064-tabla-snd-card_Button_Jack.kl \
    device/xiaomi/aries/cyttsp-i2c.kl:system/usr/keylayout/cyttsp-i2c.kl \
    device/xiaomi/aries/gpio-keys.kl:system/usr/keylayout/gpio-keys.kl \
    device/xiaomi/aries/keypad_8960.kl:system/usr/keylayout/keypad_8960.kl \
    device/xiaomi/aries/keypad_8960_liquid.kl:system/usr/keylayout/keypad_8960_liquid.kl \
    device/xiaomi/aries/philips_remote_ir.kl:system/usr/keylayout/philips_remote_ir.kl \
    device/xiaomi/aries/samsung_remote_ir.kl:system/usr/keylayout/samsung_remote_ir.kl \
    device/xiaomi/aries/sensor00fn1a.kl:system/usr/keylayout/sensor00fn1a.kl \
    device/xiaomi/aries/ue_rf4ce_remote.kl:system/usr/keylayout/ue_rf4ce_remote.kl

# These are the hardware-specific features
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/handheld_core_hardware.xml:system/etc/permissions/handheld_core_hardware.xml \
    frameworks/native/data/etc/android.hardware.camera.flash-autofocus.xml:system/etc/permissions/android.hardware.camera.flash-autofocus.xml \
    frameworks/native/data/etc/android.hardware.camera.front.xml:system/etc/permissions/android.hardware.camera.front.xml \
    frameworks/native/data/etc/android.hardware.location.gps.xml:system/etc/permissions/android.hardware.location.gps.xml \
    frameworks/native/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    frameworks/native/data/etc/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml \
    frameworks/native/data/etc/android.hardware.sensor.proximity.xml:system/etc/permissions/android.hardware.sensor.proximity.xml \
    frameworks/native/data/etc/android.hardware.sensor.light.xml:system/etc/permissions/android.hardware.sensor.light.xml \
    frameworks/native/data/etc/android.hardware.sensor.gyroscope.xml:system/etc/permissions/android.hardware.sensor.gyroscope.xml \
    frameworks/native/data/etc/android.hardware.sensor.barometer.xml:system/etc/permissions/android.hardware.sensor.barometer.xml \
    frameworks/native/data/etc/android.hardware.touchscreen.multitouch.jazzhand.xml:system/etc/permissions/android.hardware.touchscreen.multitouch.jazzhand.xml \
    frameworks/native/data/etc/android.software.sip.voip.xml:system/etc/permissions/android.software.sip.voip.xml \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml \
    frameworks/native/data/etc/android.hardware.telephony.gsm.xml:system/etc/permissions/android.hardware.telephony.gsm.xml \
    frameworks/native/data/etc/android.hardware.telephony.cdma.xml:system/etc/permissions/android.hardware.telephony.cdma.xml \
    frameworks/native/data/etc/android.hardware.audio.low_latency.xml:system/etc/permissions/android.hardware.audio.low_latency.xml \
    frameworks/native/data/etc/android.hardware.bluetooth_le.xml:system/etc/permissions/android.hardware.bluetooth_le.xml \
    frameworks/native/data/etc/android.hardware.usb.host.xml:system/etc/permissions/android.hardware.usb.host.xml

# GPS configuration
PRODUCT_COPY_FILES += \
    device/xiaomi/aries/configs/gps.conf:system/etc/gps.conf

# OpenGL ES 3.0
PRODUCT_PROPERTY_OVERRIDES += \
    ro.opengles.version=196608

# OpenGL ES 2.0
#PRODUCT_PROPERTY_OVERRIDES += \
#    ro.opengles.version=131072

PRODUCT_PROPERTY_OVERRIDES += \
    ro.sf.lcd_density=320

# qcom
PRODUCT_PROPERTY_OVERRIDES += \
    ro.qc.sdk.audio.ssr=false \
    ro.qc.sdk.sensors.gestures=false

# Audio Configuration
PRODUCT_PROPERTY_OVERRIDES += \
    persist.audio.fluence.mode=endfire \
    persist.audio.vr.enable=false \
    persist.audio.handset.mic=digital \
    persist.audio.vns.mode=2 \
    af.resampler.quality=255 \
    qcom.hw.aac.encoder=true \
    lpa.decode=true \
    tunnel.decode=false \
    tunnel.audiovideo.decode=false

PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.aries.power_profile=middle

# Do not power down SIM card when modem is sent to Low Power Mode.
PRODUCT_PROPERTY_OVERRIDES += \
    persist.radio.apm_sim_not_pwdn=1

# Ril sends only one RIL_UNSOL_CALL_RING, so set call_ring.multiple to false
PRODUCT_PROPERTY_OVERRIDES += \
    ro.telephony.call_ring.multiple=0

PRODUCT_CHARACTERISTICS := nosdcard

PRODUCT_TAGS += dalvik.gc.type-precise

PRODUCT_PACKAGES += \
    Updater \
    Stk \
    Launcher3

PRODUCT_PROPERTY_OVERRIDES += \
    ro.product.mod_device=mi2_ivan

PRODUCT_PACKAGES += \
    CellBroadcastReceiver

PRODUCT_PACKAGES += \
    librs_jni \
    com.android.future.usb.accessory

# Filesystem management tools
PRODUCT_PACKAGES += \
    make_ext4fs \
    e2fsck \
    setup_fs

# QCOM Display
PRODUCT_PACKAGES += \
    libgenlock \
    libmemalloc \
    liboverlay \
    libqdutils \
    libtilerenderer \
    libI420colorconvert \
    hwcomposer.msm8960 \
    gralloc.msm8960 \
    copybit.msm8960 \
    memtrack.msm8960 \
    libemoji

# Audio
PRODUCT_PACKAGES += \
    alsa.msm8960 \
    audio_policy.msm8960 \
    audio.primary.msm8960 \
    audio.a2dp.default \
    audio.usb.default \
    audio.r_submix.default \
    libaudio-resampler \
    tinymix

# BT
PRODUCT_PACKAGES += \
    hci_qcomm_init

PRODUCT_PROPERTY_OVERRIDES += \
    ro.qualcomm.bt.hci_transport=smd

# Omx
PRODUCT_PACKAGES += \
    libOmxAacEnc \
    libOmxAmrEnc \
    libOmxCore \
    libOmxEvrcEnc \
    libOmxQcelp13Enc \
    libOmxVdec \
    libOmxVenc \
    libc2dcolorconvert \
    libdashplayer \
    libdivxdrmdecrypt \
    libmm-omxcore \
    libstagefrighthw

# Light
PRODUCT_PACKAGES += \
    lights.msm8960

# fmradio support
PRODUCT_PACKAGES += \
    qcom.fmradio \
    libqcomfm_jni \
    FM2 \
    FMRecord

PRODUCT_BOOT_JARS += qcom.fmradio

# transmitter isn't supported
PRODUCT_PROPERTY_OVERRIDES += \
	ro.fm.transmitter=false

PRODUCT_DEFAULT_PROPERTY_OVERRIDES += \
    rild.libpath=/system/lib/libril-qc-qmi-1.so

PRODUCT_PROPERTY_OVERRIDES += \
    telephony.lteOnCdmaDevice=0 \
    ril.subscription.types=NV,RUIM \
    ro.cdma.home.operator.numeric=46003 \
    ro.telephony.default_cdma_sub=0 \
    persist.omh.enabled=true

PRODUCT_PROPERTY_OVERRIDES += \
    drm.service.enabled=true

PRODUCT_PROPERTY_OVERRIDES += \
    wifi.interface=wlan0 \
    wifi.supplicant_scan_interval=15

PRODUCT_PROPERTY_OVERRIDES += \
    debug.egl.recordable.rgba8888=1

PRODUCT_PROPERTY_OVERRIDES += \
    ro.qc.sensors.wl_dis=true

# Qualcomm random numbers generated
PRODUCT_PACKAGES += qrngd

# QCOM Display
PRODUCT_PROPERTY_OVERRIDES += \
    debug.sf.hw=1 \
    debug.egl.hw=1 \
    debug.composition.type=dyn \
    persist.hwc.mdpcomp.enable=true \
    debug.mdpcomp.logs=0

# QC Perf
PRODUCT_PROPERTY_OVERRIDES += \
    ro.vendor.extension_library=/system/lib/libqc-opt.so

# QCOM
PRODUCT_PROPERTY_OVERRIDES += \
    com.qc.hardware=true

# USB
PRODUCT_DEFAULT_PROPERTY_OVERRIDES += \
    persist.sys.usb.config=mtp

# Gps
PRODUCT_PROPERTY_OVERRIDES += \
    persist.gps.qmienabled=true

# Dual boot init
PRODUCT_PACKAGES += \
  dualboot_init

PRODUCT_COPY_FILES += \
    device/xiaomi/aries/mount_ext4.sh:system/bin/mount_ext4.sh

$(call inherit-product, frameworks/native/build/phone-xhdpi-2048-dalvik-heap.mk)

# This is the aries-specific audio package
$(call inherit-product, frameworks/base/data/sounds/AudioPackage10.mk)
