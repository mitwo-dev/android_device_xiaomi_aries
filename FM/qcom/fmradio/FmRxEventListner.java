/*
 * Copyright (c) 2009,2012, The Linux Foundation. All rights reserved.
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
import qcom.fmradio.FmReceiver;
import qcom.fmradio.FmTransceiver;
import java.util.Arrays;
import android.util.Log;


class FmRxEventListner {

    private final int EVENT_LISTEN = 1;

    private final int STD_BUF_SIZE = 256;

    private enum FmRxEvents {
      READY_EVENT,
      TUNE_EVENT,
      SEEK_COMPLETE_EVENT,
      SCAN_NEXT_EVENT,
      RAW_RDS_EVENT,
      RT_EVENT,
      PS_EVENT,
      ERROR_EVENT,
      BELOW_TH_EVENT,
      ABOVE_TH_EVENT,
      STEREO_EVENT,
      MONO_EVENT,
      RDS_AVAL_EVENT,
      RDS_NOT_AVAL_EVENT,
      TAVARUA_EVT_NEW_SRCH_LIST,
      TAVARUA_EVT_NEW_AF_LIST
    }

    private Thread mThread;
    private static final String TAG = "FMRadio";

    public void startListner (final int fd, final FmRxEvCallbacks cb) {
        /* start a thread and listen for messages */
        mThread = new Thread(){
            public void run(){
                byte [] buff = new byte[STD_BUF_SIZE];
                Log.d(TAG, "Starting listener " + fd);

                while ((!Thread.currentThread().isInterrupted())) {

                    try {
                        int index = 0;
                        int state = 0;
                        Arrays.fill(buff, (byte)0x00);
                        int freq = 0;
                        int eventCount = FmReceiverJNI.getBufferNative (fd, buff, EVENT_LISTEN);

                        if (eventCount >= 0)
                            Log.d(TAG, "Received event. Count: " + eventCount);

                        for (  index = 0; index < eventCount; index++ ) {
                            Log.d(TAG, "Received <" +buff[index]+ ">" );

                            switch(buff[index]){
                            case 0:
                                Log.d(TAG, "Got READY_EVENT");
                                if(FmTransceiver.getFMPowerState() == FmTransceiver.subPwrLevel_FMRx_Starting) {
                                    /*Set the state as FMRxOn */
                                    FmTransceiver.setFMPowerState(FmTransceiver.FMState_Rx_Turned_On);
                                    Log.v(TAG, "RxEvtList: CURRENT-STATE : FMRxStarting ---> NEW-STATE : FMRxOn");
                                    cb.FmRxEvEnableReceiver();
                                }
                                else if (FmTransceiver.getFMPowerState() == FmTransceiver.subPwrLevel_FMTurning_Off) {
                                    /*Set the state as FMOff */
                                    FmTransceiver.setFMPowerState(FmTransceiver.FMState_Turned_Off);
                                    Log.v(TAG, "RxEvtList: CURRENT-STATE : FMTurningOff ---> NEW-STATE : FMOff");
                                    FmTransceiver.release("/dev/radio0");
                                    cb.FmRxEvDisableReceiver();
                                    Thread.currentThread().interrupt();
                                }
                                break;
                            case 1:
                                Log.d(TAG, "Got TUNE_EVENT");
                                freq = FmReceiverJNI.getFreqNative(fd);
                                if (freq > 0)
                                    cb.FmRxEvRadioTuneStatus(freq);
                                else
                                    Log.e(TAG, "get frequency command failed");
                                break;
                            case 2:
                                Log.d(TAG, "Got SEEK_COMPLETE_EVENT");
                                state = FmReceiver.getSearchState();
                                switch(state) {
                                   case FmTransceiver.subSrchLevel_SeekInPrg :
                                   case FmTransceiver.subSrchLevel_ScanInProg:
                                      Log.v(TAG, "Current state is " + state);
                                      FmReceiver.setSearchState(FmTransceiver.subSrchLevel_SrchComplete);
                                      Log.v(TAG, "RxEvtList: CURRENT-STATE : Search ---> NEW-STATE : FMRxOn");
                                      cb.FmRxEvSearchComplete(FmReceiverJNI.getFreqNative(fd));
                                      break;
                                   case FmTransceiver.subSrchLevel_SrchAbort:
                                      Log.v(TAG, "Current state is SRCH_ABORTED");
                                      Log.v(TAG, "Aborting on-going search command...");
                                      FmReceiver.setSearchState(FmTransceiver.subSrchLevel_SrchComplete);
                                      Log.v(TAG, "RxEvtList: CURRENT-STATE : Search ---> NEW-STATE : FMRxOn");
                                      cb.FmRxEvSearchCancelled();
                                      break;
                                }
                                break;
                            case 3:
                                Log.d(TAG, "Got SCAN_NEXT_EVENT");
                                cb.FmRxEvSearchInProgress();
                                break;
                            case 4:
                                Log.d(TAG, "Got RAW_RDS_EVENT");
                                cb.FmRxEvRdsGroupData();
                                break;
                            case 5:
                                Log.d(TAG, "Got RT_EVENT");
                                cb.FmRxEvRdsRtInfo();
                                break;
                            case 6:
                                Log.d(TAG, "Got PS_EVENT");
                                cb.FmRxEvRdsPsInfo();
                                break;
                            case 7:
                                Log.d(TAG, "Got ERROR_EVENT");
                                break;
                            case 8:
                                Log.d(TAG, "Got BELOW_TH_EVENT");
                                cb.FmRxEvServiceAvailable (false);
                                break;
                            case 9:
                                Log.d(TAG, "Got ABOVE_TH_EVENT");
                                cb.FmRxEvServiceAvailable(true);
                                break;
                            case 10:
                                Log.d(TAG, "Got STEREO_EVENT");
                                cb.FmRxEvStereoStatus (true);
                                break;
                            case 11:
                                Log.d(TAG, "Got MONO_EVENT");
                                cb.FmRxEvStereoStatus (false);
                                break;
                            case 12:
                                Log.d(TAG, "Got RDS_AVAL_EVENT");
                                cb.FmRxEvRdsLockStatus (true);
                                break;
                            case 13:
                                Log.d(TAG, "Got RDS_NOT_AVAL_EVENT");
                                cb.FmRxEvRdsLockStatus (false);
                                break;
                            case 14:
                                Log.d(TAG, "Got NEW_SRCH_LIST");
                                state = FmReceiver.getSearchState();
                                switch(state) {
                                   case FmTransceiver.subSrchLevel_SrchListInProg:
                                      Log.v(TAG, "FmRxEventListener: Current state is AUTO_PRESET_INPROGRESS");
                                      FmReceiver.setSearchState(FmTransceiver.subSrchLevel_SrchComplete);
                                      Log.v(TAG, "RxEvtList: CURRENT-STATE : Search ---> NEW-STATE : FMRxOn");
                                      cb.FmRxEvSearchListComplete ();
                                      break;
                                   case FmTransceiver.subSrchLevel_SrchAbort:
                                      Log.v(TAG, "Current state is SRCH_ABORTED");
                                      Log.v(TAG, "Aborting on-going SearchList command...");
                                      FmReceiver.setSearchState(FmTransceiver.subSrchLevel_SrchComplete);
                                      Log.v(TAG, "RxEvtList: CURRENT-STATE : Search ---> NEW-STATE : FMRxOn");
                                      cb.FmRxEvSearchCancelled();
                                      break;
                                }
                                break;
                            case 15:
                                Log.d(TAG, "Got NEW_AF_LIST");
                                cb.FmRxEvRdsAfInfo();
                                break;
                            case 18:
                                Log.d(TAG, "Got RADIO_DISABLED");
                                if (FmTransceiver.getFMPowerState() == FmTransceiver.subPwrLevel_FMTurning_Off) {
                                    /*Set the state as FMOff */
                                    FmTransceiver.setFMPowerState(FmTransceiver.FMState_Turned_Off);
                                    Log.v(TAG, "RxEvtList: CURRENT-STATE : FMTurningOff ---> NEW-STATE : FMOff");
                                    FmTransceiver.release("/dev/radio0");
                                    cb.FmRxEvDisableReceiver();
                                    Thread.currentThread().interrupt();
                                } else {
                                    Log.d(TAG, "Unexpected RADIO_DISABLED recvd");
                                    cb.FmRxEvRadioReset();
                                }
                                break;
                            case 19:
                                FmTransceiver.setRDSGrpMask(0);
                                break;
                            case 20:
                                Log.d(TAG, "got RT plus event");
                                cb.FmRxEvRTPlus();
                                break;
                            case 21:
                                Log.d(TAG, "got eRT event");
                                cb.FmRxEvERTInfo();
                                break;
                            default:
                                Log.d(TAG, "Unknown event");
                                break;
                            }
                        }//end of for
                    } catch ( Exception ex ) {
                        Log.d( TAG,  "RunningThread InterruptedException");
                        ex.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        mThread.start();
    }

    public void stopListener(){
        //mThread.stop();
        //Thread stop is deprecate API
        //Interrupt the thread and check for the thread status
        // and return from the run() method to stop the thread
        //properly
        Log.d( TAG,  "stopping the Listener\n");
        if( mThread != null ) {
         mThread.interrupt();
        }
    }

}
