/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
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

package com.codeaurora.fmrecording;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.ComponentName;
import android.util.Log;
import java.lang.String;


public class FMRecordingReceiver extends BroadcastReceiver {

    private static final String TAG = "FMRecordingReceiver";
    public static final String ACTION_FM =
           "codeaurora.intent.action.FM";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received intent: " + action);
        if((action != null) && action.equals(ACTION_FM)) {
            Log.d(TAG, "FM intent received");
            Intent in = new Intent();
            in.putExtras(intent);
            in.setClass(context, FMRecordingService.class);
            int state = intent.getIntExtra("state", 0);
            boolean startService = true;

            if (state == 1) {
                Log.d(TAG, "FM ON intent received");
                startService = true;
                context.startService(in);
            } else if(state == 0){
                Log.d(TAG, "FM OFF intent received");
                startService = false;
                context.stopService(in);
            }
        }
   }
}
