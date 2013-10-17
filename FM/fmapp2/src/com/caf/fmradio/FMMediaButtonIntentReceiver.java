/*
 * Copyright (c) 2012-2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.caf.fmradio;

import android.content.Intent;
import android.content.IntentFilter;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.ComponentName;
import android.util.Log;
import android.view.KeyEvent;
import android.os.Bundle;
import java.lang.Object;

public class FMMediaButtonIntentReceiver extends BroadcastReceiver {

private static final String TAG = "FMMediaButtonIntentReceiver";
public static final String FM_MEDIA_BUTTON = "com.caf.fmradio.action.MEDIA_BUTTON";
public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
       if ((action != null) && action.equals("android.intent.action.MEDIA_BUTTON")) {
           KeyEvent event = (KeyEvent)
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

           if (event == null) {
               return;
           }
           int keycode = event.getKeyCode();
           int key_action = event.getAction();
           if (((KeyEvent.KEYCODE_HEADSETHOOK == keycode) &&
               (key_action == KeyEvent.ACTION_DOWN)) ||
               (KeyEvent.KEYCODE_MEDIA_PAUSE == keycode) ||
               (KeyEvent.KEYCODE_MEDIA_PLAY == keycode)) {

               Log.d(TAG, "ACTION_MEDIA_BUTTON intent received for ACTION_DOWN");
               Intent i = new Intent(FM_MEDIA_BUTTON);
               i.putExtra(Intent.EXTRA_KEY_EVENT, event);
               context.sendBroadcast(i);
           }
       }
   }
}
