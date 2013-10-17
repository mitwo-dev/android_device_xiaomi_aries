/*
 * Copyright (c) 2009-2013, The Linux Foundation. All rights reserved.
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


/**
 * This class contains all interfaces and types needed to control the FM transmitter.
 * @hide
 */
public class FmTransmitter extends FmTransceiver
{
   private final String TAG = "FmTransmitter";
 /**
   *  An object that contains the PS Features that SoC supports
   *
   *  @see #getPSFeatures
   */
    public class FmPSFeatures
    {
       public int maxPSCharacters;
       public int maxPSStringRepeatCount;
    };


  /**
    *  Command types for the RDS group transmission.
    *  This is used as argument to #transmitRdsGroupControl to
    *  control the RDS group transmission.
    *
    *  @see #transmitRdsGroupControl
    */

   public static final int RDS_GRPS_TX_PAUSE    =  0;           /* Pauses the Group transmission*/

   public static final int RDS_GRPS_TX_RESUME   =  1;          /* Resumes the Group transmission*/

   public static final int RDS_GRPS_TX_STOP     =  2;        /* Stops and clear the Group transmission */

   public static final int FM_TX_MAX_PS_LEN           =  (96+1);
   public static final int FM_TX_MAX_RT_LEN           =  (64-1); /*One space to include NULL*/

   private static final int MAX_PS_CHARS = 97;
   private static final int MAX_PS_REP_COUNT = 15;
   private static final int MAX_RDS_GROUP_BUF_SIZE = 62;

   private FmTransmitterCallbacksAdaptor mTxCallbacks;
   private boolean mPSStarted = false;
   private boolean mRTStarted = false;
   private static final int V4L2_CID_PRIVATE_BASE = 0x8000000;
   private static final int V4L2_CID_PRIVATE_TAVARUA_ANTENNA   = V4L2_CID_PRIVATE_BASE + 18;

   /**
    * Power settings
    *
    * @see #setPowerMode
    * @see #getPowerMode
    */
   public static final int FM_TX_NORMAL_POWER_MODE   =0;
   public static final int FM_TX_LOW_POWER_MODE      =1;

   /**
    * Transmit Power level settings
    *
    * @see #setTxPowerLevel
    */
   public static final int FM_TX_PWR_LEVEL_0   =0;
   public static final int FM_TX_PWR_LEVEL_1   =1;
   public static final int FM_TX_PWR_LEVEL_2   =2;
   public static final int FM_TX_PWR_LEVEL_3   =3;
   public static final int FM_TX_PWR_LEVEL_4   =4;
   public static final int FM_TX_PWR_LEVEL_5   =5;
   public static final int FM_TX_PWR_LEVEL_6   =6;
   public static final int FM_TX_PWR_LEVEL_7   =7;


   /**
    *  Constructor for the transmitter class that takes path to
    *  radio device and event callback adapter
    */
   public FmTransmitter(String path, FmTransmitterCallbacksAdaptor callbacks) throws InstantiationException{

       mTxEvents = new FmTxEventListner();
       mControl = new FmRxControls();
       mTxCallbacks = callbacks;
   }

   /*==============================================================
   FUNCTION:  enable
   ==============================================================*/
   /**
    *  Enables the FM device in Transmit Mode.
    *  <p>
    *  This is a synchronous method used to initialize the FM
    *  device in transmitt mode. If already initialized this function will
    *  intialize the Fm device with default settings. Only after
    *  successfully calling this function can many of the FM device
    *  interfaces be used.
    *  <p>
    *  When enabling the transmitter, the application must also
    *  provide the regional settings in which the transmitter will
    *  operate. These settings (included in argument
    *  configSettings) are typically used for setting up the FM
    *  Transmitter for operating in a particular geographical
    *  region. These settings can be changed after the FM driver
    *  is enabled through the use of the function {@link
    *  #configure}.
    *  <p>
    *  This command can only be issued by the owner of an FM
    *  transmitter.
    *
    *  @param configSettings  the settings to be applied when
    *                           turning on the radio
    *  @return true if Initialization succeeded, false if
    *          Initialization failed.
    *  <p>
    *  @see #enable
    *  @see #registerTransmitClient
    *  @see #disable
    *
    */
   public boolean enable (FmConfig configSettings){
      boolean status = false;

      int state = getFMState();
      if (state == FMState_Tx_Turned_On) {
          Log.d(TAG, "enable: FM Tx already turned On and running");
          return status;
      }else if (state == subPwrLevel_FMTurning_Off) {
          Log.v(TAG, "FM is in the process of turning off.Pls wait for sometime.");
          return status;
      }else if((state == subPwrLevel_FMTx_Starting)
                ||(state == subPwrLevel_FMRx_Starting)) {
          Log.v(TAG, "FM is in the process of turning On.Pls wait for sometime.");
          return status;
      }else if((state == FMState_Srch_InProg)
                ||(state == FMState_Rx_Turned_On)) {
          Log.v(TAG, "FM Rx is turned on");
          return status;
      }
      setFMPowerState(subPwrLevel_FMTx_Starting);
      Log.v(TAG, "enable: CURRENT-STATE : FMOff ---> NEW-STATE : FMTxStarting");
      status = super.enable(configSettings, FmTransceiver.FM_TX);
      if(status == true) {
         registerTransmitClient(mTxCallbacks);
         mRdsData = new FmRxRdsData(sFd);
      } else {
         status = false;
         Log.e(TAG, "enable: failed to turn On FM TX");
         Log.e(TAG, "enable: CURRENT-STATE : FMTxStarting ---> NEW-STATE : FMOff");
         setFMPowerState(FMState_Turned_Off);
      }
      return status;
   }
   /*==============================================================
   FUNCTION:  setRdsOn
   ==============================================================*/
   /**
   *
   *    This function enables RDSCTRL register for SoC.
   *
   *    <p>
   *    This API enables the ability of the FM driver
   *    to send Program Service, RadioText  information.
   *
   *
   *    @return true if the command was placed successfully, false
   *            if command failed.
   *
   */
   public boolean setRdsOn (){

      if (mRdsData == null)
         return false;
      // Enable RDS
      int re = mRdsData.rdsOn(true);

      if (re ==0)
        return true;

      return false;
   }

   /*==============================================================
   FUNCTION:  disable
   ==============================================================*/
   /**
    *  Disables the FM Transmitter Device.
    *  <p>
    *  This is a synchronous command used to disable the FM
    *  device. This function is expected to be used when the
    *  application no longer requires use of the FM device. Once
    *  called, most functionality offered by the FM device will be
    *  disabled until the application re-enables the device again
    *  via {@link #enable}.
    *
    *  <p>
    *  @return true if disabling succeeded, false if disabling
    *          failed.
    *
    *  @see #enable
    *  @see #registerTransmitClient
    */
   public boolean disable(){
      boolean status = false;
      int state;

      state = getFMState();
      switch(state) {
         case FMState_Turned_Off:
              Log.d(TAG, "FM already tuned Off.");
              return true;
         case subPwrLevel_FMTx_Starting:
              /*
               * If, FM is in the process of turning On, then wait for
               * the turn on operation to complete before turning off.
               */
              Log.d(TAG, "disable: FM not yet turned On...");
              try {
                   Thread.sleep(100);
              } catch (InterruptedException e) {
                   e.printStackTrace();
              }
              /* Check for the state of FM device */
              state = getFMState();
              if(state == subPwrLevel_FMTx_Starting) {
                 Log.e(TAG, "disable: FM in bad state");
                 return status;
              }
              break;
         case subPwrLevel_FMTurning_Off:
              /*
               * If, FM is in the process of turning Off, then wait for
               * the turn off operation to complete.
               */
              Log.v(TAG, "disable: FM is getting turned Off.");
              return status;
      }
      setFMPowerState(subPwrLevel_FMTurning_Off);
      Log.v(TAG, "disable: CURRENT-STATE : FMTxOn ---> NEW-STATE : FMTurningOff");
      //Stop all the RDS transmissions if there any
      if(mPSStarted) {
         if(!stopPSInfo()) {
            Log.d(TAG, "FmTrasmitter:stopPSInfo failed\n");
         }
      }
      if(mRTStarted) {
        if(!stopRTInfo()) {
           Log.d(TAG, "FmTrasmitter:stopRTInfo failed\n");
        }
      }
      if(!transmitRdsGroupControl(RDS_GRPS_TX_STOP) ) {
         Log.d(TAG, "FmTrasmitter:transmitRdsGroupControl failed\n");
      }
      super.disable();
      return true;
   }

   /*==============================================================
   FUNCTION:  reset
   ==============================================================*/
   /**
   *    Reset the FM Device.
   *    <p>
   *    This is a synchronous command used to reset the state of FM
   *    device in case of unrecoverable error. This function is
   *    expected to be used when the client receives unexpected
   *    notification of radio disabled. Once called, most
   *    functionality offered by the FM device will be disabled
   *    until the client re-enables the device again via
   *    {@link #enable}.
   *    <p>
   *    @return true if reset succeeded, false if reset failed.
   *    @see #enable
   *    @see #disable
   *    @see #registerTransmitClient
   */
   public boolean reset(){
      boolean status = false;
      int state = getFMState();

      if(state == FMState_Turned_Off) {
         Log.d(TAG, "FM already turned Off.");
         return false;
      }
      setFMPowerState(FMState_Turned_Off);
      Log.v(TAG, "reset: NEW-STATE : FMState_Turned_Off");
      status = unregisterTransmitClient();
      release("/dev/radio0");
      return status;
   }

  /*==============================================================
   FUNCTION:  setStation
   ==============================================================*/
   /**
    *    Tunes the FM device to the specified FM frequency.
    *    <p>
    *    This method tunes the FM device to a station specified by the
    *    provided frequency. Only valid frequencies within the band
    *    set by enable or configure can be tuned by this function.
    *    Attempting to tune to frequencies outside of the set band
    *    will result in an error.
    *    <p>
    *    Once tuning to the specified frequency is completed, the
    *    event callback FmTransmitterCallbacks::onTuneStatusChange will be called.
    *
    *    @param frequencyKHz  Frequency (in kHz) to be tuned
    *                         (Example: 96500 = 96.5Mhz)
    *   @return true if setStation call was placed successfully,
    *           false if setStation failed.
    */
   public boolean setStation (int frequencyKHz) {

      //Stop  If there is any ongoing RDS transmissions
      boolean status = false;
      if( mPSStarted ){
          Log.d(TAG,"FmTransmitter:setStation mPSStarted");
         if( !stopPSInfo() ) return status;
      }
      if( mRTStarted ) {
          Log.d(TAG,"FmTransmitter:setStation mRTStarted");
        if(!stopRTInfo()) return status;
      }
      if(!transmitRdsGroupControl(RDS_GRPS_TX_STOP) )return status;

      Log.d(TAG, "FmTrasmitter:SetStation\n");
      status = super.setStation(frequencyKHz);

      return status;
   }
   /*==============================================================
   FUNCTION:  setPowerMode
   ==============================================================*/
   /**
   *    Puts the driver into or out of low power mode.
   *
   *    <p>
   *    This is an synchronous command which can put the FM
   *    device and driver into and out of low power mode. Low power mode
   *    should be used when the receiver is tuned to a station and only
   *    the FM audio is required. The typical scenario for low power mode
   *    is when the FM application is no longer visible.
   *
   *    <p>
   *    While in low power mode, all normal FM and RDS indications from
   *    the FM driver will be suppressed. By disabling these indications,
   *    low power mode can result in fewer interruptions and this may lead
   *    to a power savings.
   *
   *    <p>
   *    @param powerMode the new driver operating mode.
   *
   *    @return true if setPowerMode succeeded, false if
   *            setPowerMode failed.
   */
   public boolean setPowerMode(int powerMode){

      int re;

      if (powerMode == FM_TX_LOW_POWER_MODE) {
        re = mControl.setLowPwrMode (sFd, true);
      }
      else {
        re = mControl.setLowPwrMode (sFd, false);
      }

      if (re == 0)
         return true;
      return false;
   }


   /*==============================================================
   FUNCTION:  getPSFeatures
   ==============================================================*/
   /**
    *  This function returns the features supported by the FM
    *  driver when using {@link #setPSInfo}.
    *  <p>
    *  This function is used to get the features the FM driver
    *  supports when transmitting Program Service information.
    *  Included in the returned features is the number of Program
    *  Service (PS) characters which can be transmitted using
    *  {@link #setPSInfo}. If the driver supports continuous
    *  transmission of Program Service Information, this function
    *  will return a value greater than 0 for
    *  FmPSFeatures.maxPSCharacters. Although the RDS/RBDS
    *  standard defines each Program Service (PS) string as eight
    *  characters in length, the FM driver may have the ability to
    *  accept a string that is greater than eight character. This
    *  extended string will thenbe broken up into multiple strings
    *  of length eight and transmitted continuously.
    *  <p>
    *  When transmitting more than one string, the application may
    *  want to control the timing of how long each string is
    *  transmitted. Included in the features returned from this
    *  function is the maximum Program Service string repeat count
    *  (FmPSFeatures.maxPSStringRepeatCount). When using the
    *  function {@link #setPSInfo}, the application can specify how
    *  many times each string is repeated before the next string is
    *  transmitted.
    *
    *  @return the Program service maximum characters and repeat
    *          count
    *
    *  @see #setPSInfo
    *
    */
   public FmPSFeatures getPSFeatures(){
      FmPSFeatures psFeatures = new FmPSFeatures();

      psFeatures.maxPSCharacters = MAX_PS_CHARS;
      psFeatures.maxPSStringRepeatCount = MAX_PS_REP_COUNT;
      return psFeatures;
   }

   /*==============================================================
   FUNCTION:  startPSInfo
   ==============================================================*/
   /**
    *  Continuously transmit RDS/RBDS Program Service information
    *  over an already tuned station.
    *  <p>
    *  This is a synchronous function used to continuously transmit Program
    *  Service information over an already tuned station. While
    *  Program Service information can be transmitted using {@link
    *  #transmitRdsGroups} and 0A/0B groups, this function makes
    *  the same output possible with limited input needed from the
    *  application.
    *  <p>
    *  Included in the Program Service information is an RDS/RBDS
    *  program type (PTY), and one or more Program Service
    *  strings. The program type (PTY) is used to describe the
    *  content being transmitted and follows the RDS/RBDS program
    *  types described in the RDS/RBDS specifications.
    *  <p>
    *  Program Service information also includes an eight
    *  character string. This string can be used to display any
    *  information, but is typically used to display information
    *  about the audio being transmitted. Although the RDS/RBDS
    *  standard defines a Program Service (PS) string as eight
    *  characters in length, the FM driver may have the ability to
    *  accept a string that is greater than eight characters. This
    *  extended string will then be broken up into multiple eight
    *  character strings which will be transmitted continuously.
    *  All strings passed to this function must be terminated by a
    *  null character (0x00).
    *  <p>
    *  When transmitting more than one string, the application may
    *  want to control the timing of how long each string is
    *  transmitted. To control this timing and to ensure that the FM
    *  receiver receives each string, the application can specify
    *  how many times each string is repeated before the next string
    *  is transmitted. This command can only be issued by the owner
    *  of an FM transmitter.
    *  <p>
    *  Maximux Programme service string lenght that can be sent is
    *  FM_TX_MAX_PS_LEN. If the application sends PS string longer than
    *  this threshold, string will be truncated to FM_TX_MAX_PS_LEN.
    *
    *  @param psStr the program service strings to transmit
    *  @param pty   the program type to use in the program Service
    *               information.
    *  @param pi    the program type to use in the program Service
    *               information.
    *  @param repeatCount the number of times each 8 char string is
    *                     repeated before next string
    *
    *  @return true if PS information was successfully sent to the
    *             driver, false if PS information could not be sent
    *             to the driver.
    *
    *  @see #getPSFeatures
    *  @see #stopPSInfo
    */
   public boolean startPSInfo(String psStr, int pty, int pi, int repeatCount){

       //Set the PTY
       if((pty < 0) || (pty > 31 )) {
           Log.d(TAG,"pTy is expected from 0 to 31");
           return false;
       }

       int err = FmReceiverJNI.setPTYNative( sFd, pty );
       if( err < 0 ){
           Log.d(TAG,"setPTYNative is failure");
          return false;
       }

       if((pi < 0) || (pi > 65535)) {
           Log.d(TAG,"pi is expected from 0 to 65535");
           return false;
       }

       //Set the PI
       err = FmReceiverJNI.setPINative( sFd, pi );
       if( err < 0 ){
           Log.d(TAG,"setPINative is failure");
          return false;
       }

       if((repeatCount < 0) || (repeatCount > 15)) {
           Log.d(TAG,"repeat count is expected from 0 to 15");
           return false;
       }

       err = FmReceiverJNI.setPSRepeatCountNative( sFd, repeatCount );
       if( err < 0 ){
           Log.d(TAG,"setPSRepeatCountNative is failure");
          return false;
       }

       if( psStr.length() > FM_TX_MAX_PS_LEN ){
          /*truncate the PS string to
          MAX possible length*/
          psStr = psStr.substring( 0, FM_TX_MAX_PS_LEN );

       }

       err = FmReceiverJNI.startPSNative( sFd, psStr , psStr.length() );
       Log.d(TAG,"return for startPS is "+err);

       if( err < 0 ){
           Log.d(TAG, "FmReceiverJNI.startPSNative returned false\n");
           return false;

       }   else {
           Log.d(TAG,"startPSNative is successful");
          mPSStarted = true;
          return true;
       }
   }

   /*==============================================================
   FUNCTION:  stopPSInfo
   ==============================================================*/
   /**
    *  Stops an active Program Service transmission.
    *
    *  <p>
    *  This is a synchrnous function used to stop an active Program Service transmission
    *  started by {@link #startPSInfo}.
    *
    *  @return true if Stop PS information was successfully sent to
    *             the driver, false if Stop PS information could not
    *             be sent to the driver.
    *
    *  @see #getPSFeatures
    *  @see #startPSInfo
    *
    */
   public boolean stopPSInfo(){
       int err =0;
       if( (err =FmReceiverJNI.stopPSNative( sFd )) < 0  ){
           Log.d(TAG,"return for startPS is "+err);
          return false;
       }    else{
           Log.d(TAG,"stopPSNative is successful");
          mPSStarted = false;
          return true;
       }
   }


   /*==============================================================
   FUNCTION:  startRTInfo
   ==============================================================*/
   /**
    *  Continuously transmit RDS/RBDS RadioText information over an
    *  already tuned station.
    *
    *  <p>
    *  This is a synchronous function used to continuously transmit RadioText
    *  information over an already tuned station. While RadioText
    *  information can be transmitted using
    *  {@link #transmitRdsGroups} and 2A/2B groups, this function
    *  makes the same output possible with limited input needed from
    *  the application.
    *  <p>
    *  Included in the RadioText information is an RDS/RBDS program type (PTY),
    *  and a single string of up to 64 characters. The program type (PTY) is used
    *  to describe the content being transmitted and follows the RDS/RBDS program
    *  types described in the RDS/RBDS specifications.
    *  <p>
    *  RadioText information also includes a string that consists of up to 64
    *  characters. This string can be used to display any information, but is
    *  typically used to display information about the audio being transmitted.
    *  This RadioText string is expected to be at 64 characters in length, or less
    *  than 64 characters and terminated by a return carriage (0x0D). All strings
    *  passed to this function must be terminated by a null character (0x00).
    *  <p>
    *  <p>
    *  Maximux Radio Text string length that can be sent is
    *  FM_TX_MAX_RT_LEN. If the application sends RT string longer than
    *  this threshold, string will be truncated to FM_TX_MAX_RT_LEN.
    *
    *  @param rtStr the Radio Text string to transmit
    *  @param pty the program type to use in the Radio text
    *             transmissions.
    *  @param pi the program identifier to use in the Radio text
    *             transmissions.
    *
    *  @return true if RT information String was successfully sent
    *             to the driver, false if RT information string
    *             could not be sent to the driver.
    *
    *  @see #stopRTInfo
    */
   public boolean startRTInfo(String rtStr, int pty, int pi){

       if((pty < 0) || (pty > 31 )) {
           Log.d(TAG,"pTy is expected from 0 to 31");
           return false;
       }
       //Set the PTY
       int err = FmReceiverJNI.setPTYNative( sFd, pty );
       if( err < 0 ){
           Log.d(TAG,"setPTYNative is failure");
          return false;
       }

       if((pi < 0) || (pi > 65535)) {
           Log.d(TAG,"pi is expected from 0 to 65535");
           return false;
       }

       err = FmReceiverJNI.setPINative( sFd, pi );
       if( err < 0 ){
           Log.d(TAG,"setPINative is failure");
          return false;
       }


       if( rtStr.length() > FM_TX_MAX_RT_LEN )
       {
          //truncate it to max length
          rtStr = rtStr.substring( 0, FM_TX_MAX_RT_LEN );
       }

       err = FmReceiverJNI.startRTNative( sFd, rtStr, rtStr.length() );

       if( err < 0 ){
          Log.d(TAG, "FmReceiverJNI.startRTNative returned false\n");
          return false;
       }   else {
           Log.d(TAG,"mRTStarted is true");
          mRTStarted = true;
          return true;
       }
   }

   /*==============================================================
   FUNCTION:  stopRTInfo
   ==============================================================*/
   /**
    *  Stops an active Radio Text information transmission.
    *
    *  <p>
    *  This is a synchrnous function used to stop an active Radio Text
    *  transmission started by {@link #startRTInfo}.
    *
    *  @return true if Stop RT information was successfully sent to
    *             the driver, false if Stop RT information could not
    *             be sent to the driver.
    *
    *  @see #startRTInfo
    *
    */
   public boolean stopRTInfo(){

      if( FmReceiverJNI.stopRTNative( sFd ) < 0  ){
          Log.d(TAG,"stopRTNative is failure");
          return false;
       }    else{
           Log.d(TAG,"mRTStarted is false");
          mRTStarted = false;
          return true;
       }
   }

  /*==============================================================
   FUNCTION:  getRdsGroupBufSize
   ==============================================================*/
   /**
    *  Get the maximum number of RDS/RBDS groups which can be passed
    *  to the FM driver.
    *  <p>
    *  This is a function used to determine the maximum RDS/RBDS
    *  buffer size for use when calling {@link #transmitRdsGroups}
    *
    *  @return the maximum number of RDS/RBDS groups which can be
    *  passed to the FM driver at any one time.
    *
    */
   public int getRdsGroupBufSize(){

   return MAX_RDS_GROUP_BUF_SIZE;
   }


   /*==============================================================
   FUNCTION:  transmitRdsGroups
   ==============================================================*/
   /**
    *  This function will transmit RDS/RBDS groups
    *  over an already tuned station.
    *  This is an asynchronous function used to transmit RDS/RBDS
    *  groups over an already tuned station. This functionality is
    *  is currently unsupported.
    *  <p>
    *  This function accepts a buffer (rdsGroups) containing one or
    *  more RDS groups. When sending this buffer, the application
    *  must also indicate how many groups should be taken from this
    *  buffer (numGroupsToTransmit). It may be possible that the FM
    *  driver can not accept the number of group contained in the
    *  buffer and will indicate how many group were actually
    *  accepted through the return value.
    *
    *  <p>
    *  The FM driver will indicate to the application when it is
    *  ready to accept more data via both the
    *  "onRDSGroupsAvailable()" and "onRDSGroupsComplete()" events
    *  callbacks. The "onRDSGroupsAvailable()" callback will
    *  indicate to the application that the FM driver can accept
    *  additional groups even though all groups may not have been
    *  passed to the FM transmitter. The onRDSGroupsComplete()
    *  callback will indicate when the FM driver has a complete
    *  buffer to transmit RDS data. In many cases all data passed to
    *  the FM driver will be passed to the FM hardware and only a
    *  onRDSGroupsComplete() event will be generated by the
    *  FM driver.
    *  <p> If the application attempts to send more groups than the
    *  FM driver can handle, the application must wait until it
    *  receives a onRDSGroupsAvailable or a onRDSGroupsComplete
    *  event before attempting to transmit more groups. Failure to
    *  do so may result in no group being consumed by the FM driver.
    *  <p> It is important to note that switching between continuous
    *  and non-continuous transmission of RDS groups can only happen
    *  when no RDS/RBDS group transmission is underway. If an
    *  RDS/RBDS group transmission is already underway, the
    *  application must wait for a onRDSGroupsComplete. If the application
    *  wishes to switch from continuous to non-continuous (or
    *  vice-versa) without waiting for the current transmission to
    *  complete, the application can clear all remaining groups
    *  using the {@link #transmitRdsGroupControl} command.
    *  <p>
    *  Once completed, this command will generate a
    *  onRDSGroupsComplete event to all registered applications.
    *
    *  @param rdsGroups The RDS/RBDS groups buffer to transmit.
    *  @param numGroupsToTransmit The number of groups in the buffer
    *                             to transmit.
    *
    *  @return The number of groups the FM driver actually accepted.
    *          A value >0 indicates the command was successfully
    *          accepted and a return value of "-1" indicates error.
    *
    *  @see #transmitRdsGroupControl
    */

   public int transmitRdsGroups(byte[] rdsGroups, long numGroupsToTransmit){
      /*
       * This functionality is currently unsupported
       */

    return -1;
   }
   /*==============================================================
   FUNCTION:  transmitContRdsGroups
   ==============================================================*/
   /**
    *  This function will continuously transmit RDS/RBDS groups over an already tuned station.
    *  <p>
    *  This is an asynchronous function used to continuously
    *  transmit RDS/RBDS groups over an already tuned station.
    *  This functionality is currently unsupported.
    *  <p>
    *  This function accepts a buffer (rdsGroups) containing one or
    *  more RDS groups. When sending this buffer, the application
    *  must also indicate how many groups should be taken from this
    *  buffer (numGroupsToTransmit). It may be possible that the FM
    *  driver can not accept the number of group contained in the
    *  buffer and will indicate how many group were actually
    *  accepted through the return value.
    *
    *  <p>
    *  Application can send a complete RDS group buffer for the transmission.
    *  This data will be sent continuously to the driver. Only single RDS
    *  group can be continuously transmitter at a time. So, application has to
    *  send the complete RDS buffer it intends to transmit. trying to pass the
    *  single buffer in two calls will be interprted as two different RDS/RBDS
    *  groups and hence all the unset groups will be cleared.
    *  <p>
    *  As continuous RDS/RBDS group transmission is done over single buffer,
    *  Application has to wait for the "onContRDSGroupsComplete()" callback
    *  to initiate the further RDS/RBDS group transmissions. Failure to
    *  do so may result in no group being consumed by the FM driver.
    *  <p> It is important to note that switching between continuous
    *  and non-continuous transmission of RDS groups can only happen
    *  when no RDS/RBDS group transmission is underway. If an
    *  RDS/RBDS group transmission is already underway, the
    *  application must wait for a onRDSGroupsComplete or onContRDSGroupsComplete.
    *   If the application wishes to switch from continuous to non-continuous (or
    *  vice-versa) without waiting for the current transmission to
    *  complete, the application can clear all remaining groups
    *  using the {@link #transmitRdsGroupControl} command.
    *  <p>
    *  Once completed, this command will generate a
    *  onRDSContGroupsComplete event to all registered applications.
    *
    *  @param rdsGroups The RDS/RBDS groups buffer to transmit.
    *  @param numGroupsToTransmit The number of groups in the buffer
    *                             to transmit.
    *
    *  @return The number of groups the FM driver actually accepted.
    *          A value >0 indicates the command was successfully
    *          accepted and a return value of "-1" indicates error.
    *
    *  @see #transmitRdsGroupControl
    */

   public int transmitRdsContGroups(byte[] rdsGroups, long numGroupsToTransmit){
      /*
       * This functionality is currently unsupported.
       */
     return -1;
   }

   /*==============================================================
   FUNCTION:  transmitRdsGroupControl
   ==============================================================*/
   /**
    *  Pause/Resume RDS/RBDS group transmission, or stop and clear
    *  all RDS groups.
    *  <p>
    *  This is a function used to pause/resume RDS/RBDS
    *  group transmission, or stop and clear all RDS groups. This
    *  function can be used to control continuous and
    *  non-continuous RDS/RBDS group transmissions. This functionality
    *  is currently unsupported.
    *  <p>
    *  @param ctrlCmd The Tx RDS group control.This should be one of the
    *                 contants RDS_GRPS_TX_PAUSE/RDS_GRPS_TX_RESUME/RDS_GRPS_TX_STOP
    *
    *  @return true if RDS Group Control command was
    *             successfully sent to the driver, false if RDS
    *             Group Control command could not be sent to the
    *             driver.
    *
    *  @see #rdsGroupControlCmdType
    *  @see #transmitRdsGroups
    */
   public boolean transmitRdsGroupControl(int ctrlCmd){
      boolean bStatus = true;
      /*
       * This functionality is currently unsupported.
       */
      int val = 0;
      switch( ctrlCmd ) {
         case RDS_GRPS_TX_PAUSE:break;
         case RDS_GRPS_TX_RESUME:break;
         case RDS_GRPS_TX_STOP:break;
         default:
                /*Shouldn't reach here*/
         bStatus = false;
      }
      return bStatus;
   }

   /*==============================================================
   FUNCTION:  setTxPowerLevel
   ==============================================================*/
   /**
    *  Sets the transmitter power level.
    *
    *  <p>
    *  This is a function used for setting the power level of
    *  Tx device.
    *  <p>
    *  @param powLevel The Tx power level value to be set. The value should be
    *                  in range 0-7.If input is -ve level will be set to 0
    *                  and if it is above 7 level will be set to max i.e.,7.
    *
    *  @return true on success, false on failure.
    *
    */
   public boolean setTxPowerLevel(int powLevel){
      boolean bStatus = true;
       int err = FmReceiverJNI.setTxPowerLevelNative( sFd, powLevel );
       if( err < 0 ){
           Log.d(TAG,"setTxPowerLevel is failure");
          return false;
      }
      return bStatus;
   }

   /*
    * getFMState() returns:
    *     '0' if FM State  is OFF
    *     '1' if FM Rx     is On
    *     '2' if FM Tx     is On
    *     '3' if FM device is Searching
    */
   public int getFMState() {
      /* Current State of FM device */
      int currFMState = FmTransceiver.getFMPowerState();
      return currFMState;
   }
};
