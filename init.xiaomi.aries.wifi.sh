#!/system/bin/sh
echo "*** WI-FI chip ID is not specified in /persist/wlan_chip_id **"
echo "*** Use the default WCN driver.                             **"
setprop wlan.driver.ath 0

# The property below is used in Qcom SDK for softap to determine
# the wifi driver config file
setprop wlan.driver.config /system/etc/firmware/wlan/prima/WCNSS_qcom_cfg.ini

# If wifi driver is built-in to the kernel, it will export 
# a file which we must touch so that the driver knows that 
# userspace is ready to handle firmware download requests.  See
# if an appropriately named device file is present
wcnssnode=`ls /dev/wcnss*`
echo 1 > $wcnssnode
# Plumb down the device serial number
serialno=`getprop ro.serialno`
echo $serialno > /sys/devices/platform/wcnss_wlan.0/serial_number

