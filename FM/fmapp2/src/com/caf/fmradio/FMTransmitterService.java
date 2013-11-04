/*
 * Copyright (c) 2009-2013, The Linux Foundation. All rights reserved.
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

import java.lang.ref.WeakReference;

import android.app.Notification.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import qcom.fmradio.FmReceiver;
import qcom.fmradio.FmTransmitter;
import qcom.fmradio.FmRxEvCallbacksAdaptor;
import qcom.fmradio.FmTransmitterCallbacksAdaptor;
import qcom.fmradio.FmRxRdsData;
import qcom.fmradio.FmConfig;
import com.caf.utils.A2dpDeviceStatus;
import android.media.IRemoteControlDisplay;
import android.os.Bundle;
import android.os.RemoteException;
import android.graphics.Bitmap;
import android.media.IAudioService;
import android.media.MediaMetadataRetriever;


/**
 * Provides "background" FM Radio (that uses the hardware) capabilities,
 * allowing the user to switch between activities without stopping playback.
 */
public class FMTransmitterService extends Service
{
   private static final int FMTRANSMITTERSERVICE_STATUS = 102;
   private static final int FM_TX_PROGRAM_TYPE = 0;
   private static final int FM_TX_PROGRAM_ID = 0x1234;
   private static final int FM_TX_PS_REPEAT_COUNT = 1;

   private static final String FMRADIO_DEVICE_FD_STRING = "/dev/radio0";
   private static final String LOGTAG = "FMTxService";//FMRadio.LOGTAG;
   private static final String QFM_STRING ="QFMRADIO";

   private static FmReceiver mReceiver;
   private static FmTransmitter mTransmitter;
   private int mTunedFrequency = 0;

   private static FmSharedPreferences mPrefs;
   private IFMTransmitterServiceCallbacks mCallbacks;
   private WakeLock mWakeLock;
   private int mServiceStartId = -1;
   private boolean mServiceInUse = false;
   private boolean mMuted = false;
   private boolean mResumeAfterCall = false;

   private boolean mFMOn = false;
   private int mFMSearchStations = 0;

   private FmRxRdsData mFMRxRDSData=null;
   final Handler mHandler = new Handler();
   private BroadcastReceiver mHeadsetReceiver = null;
   boolean mHeadsetPlugged = false;
   // Track A2dp Device status changes
   private A2dpDeviceStatus mA2dpDeviceState = null;
   // interval after which we stop the service when idle
   private static final int IDLE_DELAY = 60000;

   private Notification.Builder mRadioNotification;
   private Notification mNotificationInstance;
   private NotificationManager mNotificationManager;
   private static String RText = " ";
   private IAudioService mAudioService;
   private AudioManager mAudioManager;
   private Metadata mMetadata;
   RdsDisplay mRds;

   public FMTransmitterService() {
   }

   @Override
   public void onCreate() {
      super.onCreate();

      mCallbacks = null;
      mPrefs = new FmSharedPreferences(this);

      PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
      mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
      mWakeLock.setReferenceCounted(false);

      // If the service was idle, but got killed before it stopped itself, the
      // system will relaunch it. Make sure it gets stopped again in that case.

      TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
      tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

      //register for A2DP utility interface
      mA2dpDeviceState = new A2dpDeviceStatus(getApplicationContext());
      Message msg = mDelayedStopHandler.obtainMessage();
      mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
      mAudioManager = new AudioManager(getApplicationContext());
      mMetadata = new Metadata();
      registerHeadsetListener();
      mRds = new RdsDisplay();
      mAudioManager.registerRemoteControlDisplay(mRds);
   }

   @Override
   public void onDestroy() {
      Log.d(LOGTAG, "onDestroy");
      if (isFmOn())
      {
         Log.e(LOGTAG, "Service being destroyed while still playing.");
      }

      // make sure there aren't any other messages coming
      mDelayedStopHandler.removeCallbacksAndMessages(null);
      mAudioManager.unregisterRemoteControlDisplay(mRds);

      /* Unregister the headset Broadcase receiver */
      if (mHeadsetReceiver != null) {
          unregisterReceiver(mHeadsetReceiver);
          mHeadsetReceiver = null;
      }
      /* Since the service is closing, disable the receiver */
      fmOff();

      TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
      tmgr.listen(mPhoneStateListener, 0);

      mWakeLock.release();
      super.onDestroy();
   }

   @Override
   public IBinder onBind(Intent intent) {
      mDelayedStopHandler.removeCallbacksAndMessages(null);
      mServiceInUse = true;
      /* Application/UI is attached, so get out of lower power mode */
      setLowPowerMode(false);
      Log.d(LOGTAG, "onBind");
      return mBinder;
   }

   @Override
   public void onRebind(Intent intent) {
      mDelayedStopHandler.removeCallbacksAndMessages(null);
      mServiceInUse = true;
      /* Application/UI is attached, so get out of lower power mode */
      setLowPowerMode(false);
      Log.d(LOGTAG, "onRebind");
   }

   @Override
   public void onStart(Intent intent, int startId) {
      Log.d(LOGTAG, "onStart");
      mServiceStartId = startId;
      mDelayedStopHandler.removeCallbacksAndMessages(null);

      // make sure the service will shut down on its own if it was
      // just started but not bound to and nothing is playing
      mDelayedStopHandler.removeCallbacksAndMessages(null);
      Message msg = mDelayedStopHandler.obtainMessage();
      mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
   }

   @Override
   public boolean onUnbind(Intent intent) {
      mServiceInUse = false;
      Log.d(LOGTAG, "onUnbind");

      /* Application/UI is not attached, so go into lower power mode */
      unregisterCallbacks();
      setLowPowerMode(true);
      if (isFmOn())
      {
         // something is currently playing, or will be playing once
         // an in-progress call ends, so don't stop the service now.
         return true;
      }

      stopSelf(mServiceStartId);
      return true;
   }

   /* Handle Phone Call + FM Concurrency */
   private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
      @Override
      public void onCallStateChanged(int state, String incomingNumber) {
          Log.d(LOGTAG, "onCallStateChanged: State - " + state );
          Log.d(LOGTAG, "onCallStateChanged: incomingNumber - " + incomingNumber );
          fmActionOnCallState(state );
      }

      // NEED TO CHECK ACTION TO BE TAKEN ON DATA ACTIVITY
   };
   private void fmActionOnCallState( int state ) {
       //if Call Status is non IDLE we need to Mute FM as well stop recording if
       //any. Similarly once call is ended FM should be unmuted.
           AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
           if((TelephonyManager.CALL_STATE_OFFHOOK == state)||
              (TelephonyManager.CALL_STATE_RINGING == state)) {
               if (state == TelephonyManager.CALL_STATE_RINGING) {
                   int ringvolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                   if (ringvolume == 0) {
                       return;
                   }
               }
               if( mFMOn == true) {
                   Log.d(LOGTAG, "posting for call state change");
                   mHandler.post(mChangeFMTxState);
                   mResumeAfterCall = true;
               }
           }
           else if (state == TelephonyManager.CALL_STATE_IDLE) {
              // start playing again
              if (mResumeAfterCall)
              {
                  Log.d(LOGTAG, "posting for call state change");
                  mHandler.post(mChangeFMTxState);
                  mResumeAfterCall = false;
              }
           }//idle
       }

   private Handler mDelayedStopHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
         // Check again to make sure nothing is playing right now
         if (isFmOn() || mServiceInUse)
         {
            return;
         }
         Log.d(LOGTAG, "mDelayedStopHandler: stopSelf");
         stopSelf(mServiceStartId);
      }
   };

   /* Show the FM Notification */
   public void startNotification() {
      mRadioNotification = new Notification.Builder(this)
              .setSmallIcon(R.drawable.ic_status_fm_tx)
              .setOngoing(true)
              .setWhen(0);

      PendingIntent resultIntent = PendingIntent.getActivity(this, 0,
              new Intent("com.caf.fmradio.FMTRANSMITTER_ACTIVITY"), 0);
      mRadioNotification.setContentIntent(resultIntent);

      mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
      if (isFmOn()) {
          mRadioNotification.setContentTitle(getString(R.string.app_name))
                  .setContentText(getTunedFrequencyString());
      } else {
          mRadioNotification.setContentTitle("")
                  .setContentText("");
      }
      mNotificationInstance = mRadioNotification.getNotification();
      mNotificationManager.notify(FMTRANSMITTERSERVICE_STATUS, mNotificationInstance);

      startForeground(FMTRANSMITTERSERVICE_STATUS, mNotificationInstance);

      mFMOn = true;
   }

   private void stop() {
      gotoIdleState();
      mFMOn = false;
   }

   private void gotoIdleState() {
      mDelayedStopHandler.removeCallbacksAndMessages(null);
      Message msg = mDelayedStopHandler.obtainMessage();
      mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
      //NotificationManager nm =
      //(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      //nm.cancel(FMTRANSMITTERSERVICE_STATUS);
      //setForeground(false);
      stopForeground(true);
   }

   /*
    * By making this a static class with a WeakReference to the Service, we
    * ensure that the Service can be GCd even when the system process still
    * has a remote reference to the stub.
    */
   static class ServiceStub extends IFMTransmitterService.Stub
   {
      WeakReference<FMTransmitterService> mService;

      ServiceStub(FMTransmitterService service)
      {
         mService = new WeakReference<FMTransmitterService>(service);
      }

      public boolean fmOn() throws RemoteException
      {
         return(mService.get().fmOn());
      }

      public boolean fmOff() throws RemoteException
      {
         return(mService.get().fmOff());
      }
      public boolean fmRadioReset() throws RemoteException
      {
         return(mService.get().fmRadioReset());
      }
      public boolean fmRestart() throws RemoteException
      {
         return(mService.get().fmRestart());
      }

      public boolean isFmOn()
      {
         return(mService.get().isFmOn());
      }
      public boolean fmReconfigure()
      {
         return(mService.get().fmReconfigure());
      }

      public void registerCallbacks(IFMTransmitterServiceCallbacks cb)
      throws RemoteException {
         mService.get().registerCallbacks(cb);
      }

      public boolean searchWeakStationList(int numStations)
      throws RemoteException {
         return(mService.get().searchWeakStationList(numStations));
      }

      public void unregisterCallbacks() throws RemoteException
      {
         mService.get().unregisterCallbacks();
      }

      public boolean tune(int frequency)
      {
         return(mService.get().tune(frequency));
      }

      public boolean cancelSearch()
      {
         return(mService.get().cancelSearch());
      }

      public String getRadioText()
      {
         return(mService.get().getRadioText());
      }

      public int[] getSearchList()
      {
         return(mService.get().getSearchList());
      }

      public boolean isInternalAntennaAvailable()
      {
         return(mService.get().isInternalAntennaAvailable());
      }
      public boolean isHeadsetPlugged()
      {
         return(mService.get().isHeadsetPlugged());
      }
      public boolean isCallActive()
      {
          return(mService.get().isCallActive());
      }
      public String getPSData()
      {
          return(mService.get().getPSData());
      }
   }

   private final IBinder mBinder = new ServiceStub(this);
   /*
    * Turn ON FM: Powers up FM hardware, and initializes the FM module
    *                                                                                 .
    * @return true if fm Enable api was invoked successfully, false if the api failed.
    */
   private boolean fmOn() {
      boolean bStatus=false;

      Log.d(LOGTAG, "fmOn");
      mWakeLock.acquire(10*1000);
      if (mTransmitter == null) {
          try {
               mTransmitter = new FmTransmitter(FMRADIO_DEVICE_FD_STRING, transmitCallbacks);
               Log.d(LOGTAG, "new transmitter created");
          } catch (InstantiationException e) {
               throw new RuntimeException("FmTx service not available!");
          }
      }
      if (mTransmitter != null)
      {
         if (isFmOn())
         {
            /* FM Is already on,*/
            bStatus = true;
            try {
                 if(mCallbacks != null) {
                    mCallbacks.onEnabled(true);
                 }
            } catch(RemoteException e) {
                 e.printStackTrace();
            }
            Log.d(LOGTAG, "mTransmitter is enabled");
         }
         else
         {
         // This sets up the FM radio device
             FmConfig config = FmSharedPreferences.getFMConfiguration();
             Log.d(LOGTAG, "fmOn: RadioBand   :"+ config.getRadioBand());
             Log.d(LOGTAG, "fmOn: Emphasis    :"+ config.getEmphasis());
             Log.d(LOGTAG, "fmOn: ChSpacing   :"+ config.getChSpacing());
             Log.d(LOGTAG, "fmOn: RdsStd      :"+ config.getRdsStd());
             Log.d(LOGTAG, "fmOn: LowerLimit  :"+ config.getLowerLimit());
             Log.d(LOGTAG, "fmOn: UpperLimit  :"+ config.getUpperLimit());

             boolean bFmRxEnabled = false;

             if (!mA2dpDeviceState.isDeviceAvailable()) {
                 bStatus = mTransmitter.enable(config);
             }
             if( false == bStatus ) {
                Log.e(LOGTAG,"FM Enable failed");
                return bStatus;
             }
             bStatus = mTransmitter.setTxPowerLevel(FmTransmitter.FM_TX_PWR_LEVEL_7);

             if( false == bStatus ) {
                Log.e(LOGTAG,"FM setPowerLevel failed");
                return bStatus;
             }

             Log.e(LOGTAG, "FMTx is on: Requesting to start FM TX");
             AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_FM_TX,
                                  AudioSystem.DEVICE_STATE_AVAILABLE, "");
         }

         if(true == bStatus )
         {
                bStatus = mTransmitter.setRdsOn();
                if( true != bStatus ) {
                    Log.d(LOGTAG, "FMTx setRdsOn failed");
                } else {
                    if(false == mTransmitter.getInternalAntenna()) {
                        Log.d(LOGTAG, "Setting internal antenna explicitly");
                        mTransmitter.setInternalAntenna(true);
                    }
                    startNotification();
                }
         }
         else
         {
             stop();
         }
      }
      return(bStatus);
   }

  /*
   * Turn OFF FM Operations: This disables all the current FM operations             .
   */
   private void fmOperationsOff() {

      Log.d(LOGTAG, "fmOperationsOff" );

      Log.e(LOGTAG, "FMTx is off: Requesting to stop FM Tx");
      AudioSystem.setDeviceConnectionState(AudioSystem.DEVICE_OUT_FM_TX,
                           AudioSystem.DEVICE_STATE_UNAVAILABLE, "");
   }
   /*
    * Turn OFF FM: Disable the FM Host and hardware                                  .
    *                                                                                 .
    * @return true if fm Disable api was invoked successfully, false if the api failed.
    */
   private boolean fmOff() {
      boolean bStatus=false;
      Log.d(LOGTAG, "fmOff");

      fmOperationsOff();

      // This will disable the FM radio device
      if (mTransmitter != null)
      {
         bStatus = mTransmitter.disable();
         mTransmitter = null;
      }
      /* Disable Receiver */
      if (mReceiver != null)
      {
         bStatus = mReceiver.disable();
         mReceiver = null;
      }
      RText = " ";
      stop();
      return(bStatus);
   }
  /*
   * Turn OFF FM: Disable the FM Host when hardware resets asynchronously            .
   *                                                                                 .
   * @return true if fm Reset api was invoked successfully, false if the api failed  .
   */
   private boolean fmRadioReset() {
      boolean bStatus=false;
      Log.d(LOGTAG, "fmRadioReset");
      fmOperationsOff();

      // This will disable the FM radio device
      if (mTransmitter != null)
      {
         bStatus = mTransmitter.reset();
         mTransmitter = null;
      }
      /* Disable Receiver */
      if (mReceiver != null)
      {
         bStatus = mReceiver.reset();
         mReceiver = null;
      }
      stop();
      return(bStatus);
   }

   /*
    * Restart FM Transmitter: Disables FM receiver mode or transmitter is already active
    * and Powers up FM hardware, and initializes the FM module
    *
    * @return true if fm Enable api was invoked successfully, false if the api failed.
    */

   private boolean fmRestart() {
      boolean bStatus=false;
      Log.d(LOGTAG, "fmRestart");

      /* First Disable Transmitter, if enabled */
      if (mTransmitter != null)
      {
         bStatus = mTransmitter.disable();
         mTransmitter = null;
         mFMOn = false;
      }

      /* Disable Receiver */
      if (mReceiver != null)
      {
         bStatus = mReceiver.disable();
         mReceiver = null;
      }
      try {
          Thread.sleep(100);//sleep needed for SoC to switch mode
      }
      catch ( Exception ex ) {
          Log.d( LOGTAG,  "RunningThread InterruptedException");
      }
      bStatus = fmOn();
      return(bStatus);
   }

   /* Returns whether FM hardware is ON.
    *
    * @return true if FM was tuned, searching. (at the end of
    * the search FM goes back to tuned).
    *
    */
   public boolean isFmOn() {
      return mFMOn;
   }

   /*
    *  ReConfigure the FM Setup parameters
    *  - Band
    *  - Channel Spacing (50/100/200 KHz)
    *  - Emphasis (50/75)
    *  - Frequency limits
    *  - RDS/RBDS standard
    *
    * @return true if configure api was invoked successfully, false if the api failed.
    */
   public boolean fmReconfigure() {
      boolean bStatus=false;
      Log.d(LOGTAG, "fmReconfigure");
      if (mTransmitter != null)
      {
         // This sets up the FM radio device
         FmConfig config = FmSharedPreferences.getFMConfiguration();
         Log.d(LOGTAG, "RadioBand   :"+ config.getRadioBand());
         Log.d(LOGTAG, "Emphasis    :"+ config.getEmphasis());
         Log.d(LOGTAG, "ChSpacing   :"+ config.getChSpacing());
         Log.d(LOGTAG, "RdsStd      :"+ config.getRdsStd());
         Log.d(LOGTAG, "LowerLimit  :"+ config.getLowerLimit());
         Log.d(LOGTAG, "UpperLimit  :"+ config.getUpperLimit());
         bStatus = mTransmitter.configure(config);
      }
      if (mCallbacks != null)
      {
         try
         {
            mCallbacks.onReconfigured();
         } catch (RemoteException e)
         {
            e.printStackTrace();
         }
      }
      return(bStatus);
   }

   /*
    * Register UI/Activity Callbacks
    */
   public void registerCallbacks(IFMTransmitterServiceCallbacks cb)
   {
      mCallbacks = cb;
   }

   /*
    *  unRegister UI/Activity Callbacks
    */
   public void unregisterCallbacks()
   {
      mCallbacks=null;
   }

   /* Tunes to the specified frequency
    *
    * @return true if Tune command was invoked successfully, false if not muted.
    *  Note: Callback FmRxEvRadioTuneStatus will be called when the tune
    *        is complete
    */
   public boolean tune(int frequency) {
      boolean bCommandSent=false;
      double doubleFrequency = frequency/1000.00;

      Log.d(LOGTAG, "tune:  " + doubleFrequency);
      if (mTransmitter != null)
      {
         mTransmitter.setStation(frequency);
         mTunedFrequency = frequency;
         bCommandSent = true;
      }
      return bCommandSent;
   }

   /* Search for the weakest 12 FM Stations in the current band.
    *
    * It searches in the forward direction relative to the current tuned station.
    * int numStations: maximum number of stations to search.
    *
    * @return true if Search command was invoked successfully, false if not muted.
    *  Note: 1. Callback FmRxEvSearchListComplete will be called when the Search
    *        is complete
    *        2. Callback FmRxEvRadioTuneStatus will also be called when tuned to
    *        the previously tuned station.
    */
   public boolean searchWeakStationList(int numStations)
   {

       boolean bStatus=false;
       FmConfig config = FmSharedPreferences.getFMConfiguration();

       if(null != mTransmitter) {
           mTransmitter.disable();
           mTransmitter = null;
           mFMOn = false;
       }
       if(null != mReceiver) {
           mReceiver.disable();
           mReceiver = null;
       }
       try {
           Thread.sleep(100);//SoC needs a delay to switch mode
       } catch (Exception ex) {
           Log.d( LOGTAG,  "RunningThread InterruptedException");
       }


       if(null == mReceiver) {
           try {
               mReceiver = new FmReceiver(FMRADIO_DEVICE_FD_STRING, fmCallbacks);
           }
           catch (InstantiationException e){
            throw new RuntimeException("FmTx service not available!");
           }
           if (mReceiver.getFMState() == mReceiver.FMState_Turned_Off) {
               bStatus = mReceiver.enable(config);
           } else {
               try {
                   Thread.sleep(100);
               } catch (Exception ex) {
                   Log.d( LOGTAG,  "RunningThread InterruptedException");
               }
               bStatus = mReceiver.enable(config);
           }
           if (!bStatus) {
               Log.e( LOGTAG,  "Search for weak station failed");
               return false;
           }
       }

       bStatus = mReceiver.setStation(config.getLowerLimit());

       Log.d(LOGTAG, "mReceiver.setStation:  bStatus: " + bStatus);
       bStatus = mReceiver.searchStationList( FmReceiver.FM_RX_SRCHLIST_MODE_WEAKEST,
                                              FmReceiver.FM_RX_SEARCHDIR_UP,
                                              numStations,
                                              0);

        mFMSearchStations = 0;//numStations;
        if(bStatus == false)
        {
           try
           {
              if (mCallbacks != null)
              {
                 mCallbacks.onSearchListComplete(false);
              }
           } catch (RemoteException e)
           {
              e.printStackTrace();
           }
        }
      return bStatus;
   }

   /* Cancel any ongoing Search (Seek/Scan/SearchStationList).
    *
    * @return true if Search command was invoked successfully, false if not muted.
    *  Note: 1. Callback FmRxEvSearchComplete will be called when the Search
    *        is complete/cancelled.
    *        2. Callback FmRxEvRadioTuneStatus will also be called when tuned to a station
    *        at the end of the Search or if the seach was cancelled.
    */
   public boolean cancelSearch()
   {
      boolean bStatus=false;
      if (mReceiver != null)
      {
         bStatus = mReceiver.cancelSearch();
         Log.d(LOGTAG, "mReceiver.cancelSearch: bStatus: " + bStatus);
         try
         {
            if (mCallbacks != null)
            {
               mCallbacks.onSearchListComplete(false);
            }
         } catch (RemoteException e)
         {
            e.printStackTrace();
         }

      }
      return bStatus;
   }

   /* Retrieves the basic String to be displayed on UI
    * Other than this static string the RDS String will be
    * queried by Tx Activity to update on UI
    */
   public String getRadioText() {
      String str = "Radio Text: Transmitting ";
      Log.d(LOGTAG, "Radio Text: [" + str + "]");
      return str;
   }

   /* Retrieves the station list from the SearchStationlist.
    *
    * @return Array of integers that represents the station frequencies.
    * Note: 1. This is a synchronous call that should typically called when
    *           Callback onSearchListComplete.
    */
   public int[] getSearchList()
   {
      int[] frequencyList = null;
      if (mReceiver != null)
      {
         Log.d(LOGTAG, "getSearchList: ");
         frequencyList = mReceiver.getStationList();
      }
      return frequencyList;
   }
   /* Set the FM Power Mode on the FM hardware SoC.
    * Typically used when UI/Activity is in the background, so the Host is interrupted less often.
    *
    * boolean bLowPower: true: Enable Low Power mode on FM hardware.
    *                    false: Disable Low Power mode on FM hardware. (Put into normal power mode)
    * @return true if set power mode api was invoked successfully, false if the api failed.
    */
   public boolean setLowPowerMode(boolean bLowPower)
   {
      boolean bCommandSent=false;
      if (mTransmitter != null)
      {
         Log.d(LOGTAG, "setLowPowerMode: " + bLowPower);
         if(bLowPower)
         {
            bCommandSent = mTransmitter.setPowerMode(FmTransmitter.FM_TX_LOW_POWER_MODE);
         }
         else
         {
            bCommandSent = mTransmitter.setPowerMode(FmTransmitter.FM_TX_NORMAL_POWER_MODE);
         }
      }
      return bCommandSent;
   }
   /** Determines if an internal Antenna is available.
    *
    * @return true if internal antenna is available, false if
    *         internal antenna is not available.
    */
   public boolean isInternalAntennaAvailable()
   {
      boolean bAvailable  = false;
      /* Update this when the API is available */

      if(null != mTransmitter ) {
          bAvailable = mTransmitter.getInternalAntenna();
          Log.d(LOGTAG, "internalAntennaAvailable: " + bAvailable);
      } else if( null != mReceiver ) {
          bAvailable = mReceiver.getInternalAntenna();
          Log.d(LOGTAG, "internalAntennaAvailable: " + bAvailable);
      }
      return bAvailable;
   }


   private FmTransmitterCallbacksAdaptor transmitCallbacks = new  FmTransmitterCallbacksAdaptor() {
      public void FmTxEvRDSGroupsAvailable() {
         // Do nothing
      }

      public void FmTxEvRDSGroupsComplete() {
         // Do nothing
      }
      public void FmTxEvContRDSGroupsComplete() {
      }

      public void FmTxEvTuneStatusChange(int freq) {

        Log.d(LOGTAG, "onTuneStatusChange\n");
        if (mCallbacks != null)
        {
           try
           {
              mCallbacks.onTuneStatusChanged(freq);
           } catch (RemoteException e)
           {
              e.printStackTrace();
           }
        }
        /* Update the frequency in the StatusBar's Notification */
        startNotification();

        String s = getPSData();
        if( true == mTransmitter.startPSInfo(
            s, FM_TX_PROGRAM_TYPE, FM_TX_PROGRAM_ID, FM_TX_PS_REPEAT_COUNT ) ) {
            if (mCallbacks != null)
            {
               try
               {
                  mCallbacks.onPSInfoSent(s);
               } catch (RemoteException e)
               {
                  e.printStackTrace();
               }
            }
        }

        if (mTransmitter != null ){
            mTransmitter.startRTInfo(RText, FM_TX_PROGRAM_TYPE, FM_TX_PROGRAM_ID );
        }

        try {
             if (mCallbacks != null ) {
                 mCallbacks.onMetaDataChanged(RText);
             } else {
                 Log.d(LOGTAG, "callback is not there");
             }
        } catch (RemoteException ex){
             ex.printStackTrace();
        }

      }

      public void FmTxEvRadioDisabled() {
         Log.d(LOGTAG, "onRadioDisabled");
         mFMOn = false;
         if((mServiceInUse) && (mCallbacks != null) ) {
            try {
                  mCallbacks.onDisabled();
            } catch(RemoteException e) {
                  e.printStackTrace();
            }
         }
      }
      public void FmTxEvRadioEnabled() {
         mFMOn = true;
         if((mServiceInUse) && (mCallbacks != null) ) {
            try {
                  mCallbacks.onEnabled(true);
            } catch(RemoteException e) {
                  e.printStackTrace();
            }
         }
      }
      public void FmTxEvRadioReset() {
         if(isFmOn()) {
            // Received Radio Disable event unexpectedly
            Log.d(LOGTAG, "FM is ON, reset FM");
            fmRadioReset();
            try {
                /* Notify the UI/Activity, only if the service is "bound"
                   by an activity and if Callbacks are registered
                */
                if((mServiceInUse) && (mCallbacks != null)) {
                    mCallbacks.onRadioReset();
                }
             }
             catch (RemoteException e) {
                e.printStackTrace();
             }
         }
      }
   };

   /* Receiver callbacks back from the FM Stack */
   FmRxEvCallbacksAdaptor fmCallbacks = new FmRxEvCallbacksAdaptor()
   {
      public void FmRxEvEnableReceiver()
      {
         Log.d(LOGTAG, "FmRxEvEnableReceiver");
      }
      public void FmRxEvDisableReceiver()
      {
         Log.d(LOGTAG, "FmRxEvDisableReceiver");
      }
      public void FmRxEvRadioReset()
      {
         Log.d(LOGTAG, "FmRxEvRadioReset");
         if(isFmOn()) {
             // Received Radio Reset event
             Log.d(LOGTAG, "FM is ON, reset FM");
             fmRadioReset();
             try
             {
                /* Notify the UI/Activity, only if the service is "bound"
                   by an activity and if Callbacks are registered
                */
                if((mServiceInUse) && (mCallbacks != null) )
                {
                    mCallbacks.onRadioReset();
                }
             }
             catch (RemoteException e)
             {
                e.printStackTrace();
             }
         }
      }

      public void FmRxEvRadioTuneStatus(int frequency)
      {
         Log.d(LOGTAG, "FmRxEvRadioTuneStatus: Tuned Frequency: " +frequency);
      }

      public void FmRxEvSearchListComplete()
      {
         Log.d(LOGTAG, "FmRxEvSearchListComplete");
         try
         {
            if (mCallbacks != null) {
                mCallbacks.onSearchListComplete(true);
            } else if(mReceiver != null) {
                mReceiver.disable();
            }
         } catch (RemoteException e)
         {
            e.printStackTrace();
         }
      }
   };


   /*
    *  Read the Tuned Frequency from the FM module.
    */
   private String getTunedFrequencyString() {
      double frequency = mTunedFrequency / 1000.0;
      String frequencyString = getString(R.string.stat_notif_tx_frequency, (""+frequency));
      return frequencyString;
   }
   /**
    * Registers an intent to listen for ACTION_HEADSET_PLUG
    * notifications. This intent is called to know if the headset
    * was plugged in/out
    */
   public void registerHeadsetListener() {
       if (mHeadsetReceiver == null) {
           mHeadsetReceiver = new BroadcastReceiver() {
               @Override
               public void onReceive(Context context, Intent intent) {
                   String action = intent.getAction();
                   if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                      Log.d(LOGTAG, "ACTION_HEADSET_PLUG Intent received");
                      // Listen for ACTION_HEADSET_PLUG broadcasts.
                      Log.d(LOGTAG, "mTransmitter: ACTION_HEADSET_PLUG");
                      Log.d(LOGTAG, "==> intent: " + intent);
                      Log.d(LOGTAG, "    state: " + intent.getIntExtra("state", 0));
                      Log.d(LOGTAG, "    name: " + intent.getStringExtra("name"));
                      mHeadsetPlugged = (intent.getIntExtra("state", 0) == 1);
                      mHandler.post(mChangeFMTxState);

                   } else if (mA2dpDeviceState.isA2dpStateChange(action)) {
                       if (mA2dpDeviceState.isConnected(intent)){
                           Log.d(LOGTAG, " A2DP connected");
                           mHeadsetPlugged = true;
                           if( mFMOn == true)
                           {
                               mHandler.post(mChangeFMTxState);
                           }
                       } else if( !mA2dpDeviceState.isConnected(intent)) {
                           Log.d(LOGTAG, "A2DP disconnected");
                           mHeadsetPlugged = false;
                           if( mFMOn == false) // when FM Tx App open, DISC BT
                           {
                               Log.d(LOGTAG, "posting for a2dp state change");
                               mHandler.post(mChangeFMTxState);
                           }
                       }
                   } else if (action.equals("HDMI_CONNECTED")) {
                       if( mFMOn == true) {
                           Log.d(LOGTAG, "posting for hdmi state change");
                           mHandler.post(mChangeFMTxState);
                       }
                   }
               }
           };
           IntentFilter iFilter = new IntentFilter();
           iFilter.addAction(Intent.ACTION_HEADSET_PLUG);
           iFilter.addAction(mA2dpDeviceState.getActionSinkStateChangedString());
           iFilter.addAction(mA2dpDeviceState.getActionPlayStateChangedString());
           iFilter.addAction("HDMI_CONNECTED");
           iFilter.addCategory(Intent.CATEGORY_DEFAULT);
           registerReceiver(mHeadsetReceiver, iFilter);
       }
   }
   final Runnable    mChangeFMTxState = new Runnable() {
       public void run() {
           boolean bStatus = false;

           Log.d(LOGTAG, "Enter change FM Tx State");
           /* Update the UI based on the state change of the headset/antenna*/
           if(mHeadsetPlugged) {
              bStatus =  cancelSearch();
              if(bStatus == false)
                 Log.e(LOGTAG, "Error in cancelling the search");
              if(isFmOn()) {
                 Log.d(LOGTAG, "disable called from headset handler");
                 bStatus = fmOff();
                 if(mServiceInUse && (mCallbacks != null) && (bStatus == true)) {
                    try {
                        mCallbacks.onDisabled();
                    } catch(RemoteException e) {
                        e.printStackTrace();
                    }
                 } else if(bStatus == false) {
                    Log.e(LOGTAG, "Error in turning off the FM TX ");
                 }
              } else if(mReceiver != null) {
                 bStatus = mReceiver.disable();
                 if(bStatus == true)
                    mReceiver = null;
                 else
                    Log.e(LOGTAG, "Error in disabling the FM RX");
              }
           }else {
              if(!isFmOn()) {
                 bStatus = fmOn();
                 if(mServiceInUse && (mCallbacks != null) && (bStatus == true)) {
                    try {
                        mCallbacks.onEnabled(true);
                    } catch(RemoteException e) {
                        e.printStackTrace();
                    }
                 } else if(bStatus == false) {
                    Log.e(LOGTAG, "Error in enabling the FM TX");
                 }
              }
           }
       }
   };
   private class RdsDisplay extends IRemoteControlDisplay.Stub {
        RdsDisplay() {
        }

        @Override
        public void setPlaybackState(int generationId, int state, long stateChangeTimeMs,
                  long currentPosMs, float speed) {
        }

        @Override
        public void setMetadata(int generationId, Bundle metadata) {
            updateMetadata(metadata);
        }

        @Override
        public void setTransportControlInfo(int generationId, int flags, int posCapabilities) {
        }

        @Override
        public void setArtwork(int generationId, Bitmap bitmap) {
        }

        @Override
        public void setAllMetadata(int generationId, Bundle metadata, Bitmap bitmap) {
         }

        @Override
        public void setCurrentClientId(int clientGeneration, PendingIntent clientMediaIntent,
                boolean clearing) {
        }

	@Override
	public void setEnabled(boolean enabled) {
	}

   }

   class Metadata {
        private String artist;
        private String trackTitle;
        private String albumTitle;

        public Metadata() {
            artist = null;
            trackTitle = null;
            albumTitle = null;
        }

        public String toString() {
            return "Metadata[artist=" + artist + " trackTitle=" + trackTitle + " albumTitle=" +
                   albumTitle + "]";
        }
   }

   private String getMdString(Bundle data, int id) {
        return data.getString(Integer.toString(id));
   }

   private void updateMetadata(Bundle data) {
        String oldMetadata = mMetadata.toString();
        mMetadata.artist = getMdString(data, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
        mMetadata.trackTitle = getMdString(data, MediaMetadataRetriever.METADATA_KEY_TITLE);
        mMetadata.albumTitle = getMdString(data, MediaMetadataRetriever.METADATA_KEY_ALBUM);
        Log.v(LOGTAG, "mMetadata=" + mMetadata.toString());
        if (mTransmitter != null ){
            RText = mMetadata.albumTitle  +":" + mMetadata.trackTitle +":"+ mMetadata.artist;
            Log.d(LOGTAG,"RT string size is "+RText.length());
            mTransmitter.startRTInfo(RText, FM_TX_PROGRAM_TYPE, FM_TX_PROGRAM_ID );
        }

        try {
             if (mCallbacks != null ) {
                 mCallbacks.onMetaDataChanged(RText);
             } else {
                 Log.d(LOGTAG, "callback is not there");
             }
        } catch (RemoteException ex){
             ex.printStackTrace();
        }
   }
   public boolean isHeadsetPlugged() {
       if (mA2dpDeviceState.isDeviceAvailable())
          mHeadsetPlugged = true;
       return mHeadsetPlugged;
   }
   public boolean isCallActive() {
       TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
       if( TelephonyManager.CALL_STATE_IDLE !=tmgr.getCallState() )
           return true;
       return false;
   }
   public String getPSData(){
       return QFM_STRING;
   }
}
