/*
 * Copyright (c) 2011,2012, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of The Linux Foundation nor
 *      the names of its contributors may be used to endorse or promote
 *      products derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package qcom.fmradio;

import android.util.Log;
import qcom.fmradio.FmTransceiver;

class FmTxEventListner {


    private final int EVENT_LISTEN  = 1;

    private final int TUNE_EVENT = 1;                   /*Tune completion event*/
    private final int TXRDSDAT_EVENT = 16;               /*RDS Group available event*/
    private final int TXRDSDONE_EVENT = 17;              /*RDS group complete event */
    private final int RADIO_DISABLED = 18;
    private final int READY_EVENT = 0;
    private byte []buff = new byte[128];
    private Thread mThread;
    private static final String TAG = "FMTxEventListner";

    public void startListner (final int fd, final FmTransmitterCallbacks cb) {
        /* start a thread and listen for messages */
        mThread = new Thread(){
            public void run(){

                Log.d(TAG, "Starting Tx Event listener " + fd);

                while((!Thread.currentThread().isInterrupted())) {
                   try {
                        int index = 0;
                        int freq = 0;
                        Log.d(TAG, "getBufferNative called");
                        int eventCount = FmReceiverJNI.getBufferNative
                                                       (fd, buff, EVENT_LISTEN);
                        Log.d(TAG, "Received event. Count: " + eventCount);

                        for(index = 0; index < eventCount; index++) {
                            Log.d(TAG, "Received <" +buff[index]+ ">" );
                            switch(buff[index]){
                                case READY_EVENT:
                                   Log.d(TAG, "Got RADIO_ENABLED");
                                   if(FmTransceiver.getFMPowerState() ==
                                       FmTransceiver.subPwrLevel_FMTx_Starting) {
                                      /*Set the state as FMRxOn */
                                      FmTransceiver.setFMPowerState
                                         (FmTransceiver.FMState_Tx_Turned_On);
                                      Log.v(TAG, "TxEvtList: CURRENT-STATE:" +
                                            "FMTxStarting ---> NEW-STATE : FMTxOn");
                                      cb.FmTxEvRadioEnabled();
                                   }
                                   break;
                                case TUNE_EVENT:
                                   Log.d(TAG, "Got TUNE_EVENT");
                                   freq = FmReceiverJNI.getFreqNative(fd);
                                   if (freq > 0)
                                       cb.FmTxEvTuneStatusChange(freq);
                                   else
                                       Log.e(TAG, "get frqency cmd failed");
                                   break;
                                case TXRDSDAT_EVENT:
                                   Log.d(TAG, "Got TXRDSDAT_EVENT");
                                   cb.FmTxEvRDSGroupsAvailable();
                                   break;
                                case TXRDSDONE_EVENT:
                                   Log.d(TAG, "Got TXRDSDONE_EVENT");
                                   cb.FmTxEvContRDSGroupsComplete();
                                   break;
                                case RADIO_DISABLED:
                                   Log.d(TAG, "Got RADIO_DISABLED");
                                   if(FmTransceiver.getFMPowerState() ==
                                      FmTransceiver.subPwrLevel_FMTurning_Off) {
                                      /*Set the state as FMOff */
                                      FmTransceiver.setFMPowerState
                                            (FmTransceiver.FMState_Turned_Off);
                                      Log.v(TAG, "TxEvtList:CURRENT-STATE :" +
                                            "FMTurningOff ---> NEW-STATE: FMOff");
                                      FmTransceiver.release("/dev/radio0");
                                      cb.FmTxEvRadioDisabled();
                                      Thread.currentThread().interrupt();
                                   } else {
                                      Log.d(TAG, "Unexpected RADIO_DISABLED recvd");
                                      cb.FmTxEvRadioReset();
                                   }
                                   break;
                                default:
                                   Log.d(TAG, "Unknown event");
                                   break;
                            }//switch
                        }//for
                      }catch (Exception ex) {
                        Log.d( TAG,  "RunningThread InterruptedException");
                        Thread.currentThread().interrupt();
                      }//try
               }//while
               Log.d(TAG, "Came out of the while loop");
          }
        };
        mThread.start();
    }

    public void stopListener(){
        Log.d(TAG, "Thread Stopped\n");
        //Thread stop is deprecate API
        //Interrupt the thread and check for the thread status
        // and return from the run() method to stop the thread
        //properly
        Log.d(TAG, "stopping the Listener\n");

        if(mThread != null) {
            mThread.interrupt();
        }
        Log.d(TAG, "Thread Stopped\n");
    }
}
