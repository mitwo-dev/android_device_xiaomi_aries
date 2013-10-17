/*
 * Copyright (c) 2011-2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of The Linux Foundation nor
 *            the names of its contributors may be used to endorse or promote
 *            products derived from this software without specific prior written
 *            permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.caf.fmradio;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.ComponentName;
import android.util.Log;
import android.os.SystemProperties;
import java.io.FileReader;
import java.io.File;
import java.lang.String;


public class FMTransmitterConfigReceiver extends BroadcastReceiver {

    private static FileReader socinfo_fd;
    private static char[] socinfo = new char[20];
    private static String build_id = "1";

    private static final String TAG = "FMFolderConfigReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received intent: " + action);
        if((action != null) && action.equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d(TAG, "boot complete intent received");
            boolean isFmTransmitterSupported = SystemProperties.getBoolean("ro.fm.transmitter",true);

            if ("msm7630_surf".equals(SystemProperties.get("ro.board.platform"))) {
                Log.d(TAG,"this is msm7630_surf");
                try {
                    File f = new File("/sys/devices/soc0/build_id");
                    if (f.exists()) {
                        socinfo_fd = new FileReader("/sys/devices/soc0/build_id");
                    } else {
                        socinfo_fd = new FileReader("/sys/devices/system/soc/soc0/build_id");
                    }
                    socinfo_fd.read(socinfo,0,20);
                    socinfo_fd.close();
                } catch(Exception e) {
                    Log.e(TAG,"Exception in FileReader");
                }
                Log.d(TAG, "socinfo=" +socinfo);
                build_id = new String(socinfo,17,1);
                Log.d(TAG, "build_id=" +build_id);
            }
            if ((!isFmTransmitterSupported) || (build_id.equals("0"))) {
            PackageManager pManager = context.getPackageManager();
               if (pManager != null) {
                   Log.d(TAG, "disableing the FM Transmitter");
                   ComponentName fmTransmitter = new ComponentName("com.caf.fmradio", "com.caf.fmradio.FMTransmitterActivity");
                   pManager.setComponentEnabledSetting(fmTransmitter, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                                    PackageManager.DONT_KILL_APP);
               }
           }
        }
   }
}
