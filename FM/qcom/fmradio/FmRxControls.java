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

package qcom.fmradio;
import android.util.Log;


class FmRxControls
{

   private boolean mStateStereo;
   private boolean mStateMute;
   private int mFreq;

   static final int FREQ_MUL = 1000;
   static final int SEEK_FORWARD = 0;
   static final int SEEK_BACKWARD = 1;
   static final int SCAN_FORWARD = 2;
   static final int SCAN_BACKWARD = 3;
   static final int FM_DIGITAL_PATH = 0;
   static final int FM_ANALOG_PATH  = 1;
   private int mSrchMode;
   private int mScanTime;
   private int mSrchDir;
   private int mSrchListMode;
   private int mPrgmType;
   private int mPrgmId;
   private static final String TAG = "FmRxControls";


   /* V4l2 Controls */
   private static final int V4L2_CID_PRIVATE_BASE                          = 0x8000000;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SRCHMODE              = V4L2_CID_PRIVATE_BASE + 1;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SCANDWELL             = V4L2_CID_PRIVATE_BASE + 2;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SRCHON                = V4L2_CID_PRIVATE_BASE + 3;
   private static final int V4L2_CID_PRIVATE_TAVARUA_STATE                 = V4L2_CID_PRIVATE_BASE + 4;
   private static final int V4L2_CID_PRIVATE_TAVARUA_TRANSMIT_MODE         = V4L2_CID_PRIVATE_BASE + 5;
   private static final int V4L2_CID_PRIVATE_TAVARUA_RDSGROUP_MASK         = V4L2_CID_PRIVATE_BASE + 6;
   private static final int V4L2_CID_PRIVATE_TAVARUA_REGION                = V4L2_CID_PRIVATE_BASE + 7;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SIGNAL_TH             = V4L2_CID_PRIVATE_BASE + 8;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SRCH_PTY              = V4L2_CID_PRIVATE_BASE + 9;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SRCH_PI               = V4L2_CID_PRIVATE_BASE + 10;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SRCH_CNT              = V4L2_CID_PRIVATE_BASE + 11;
   private static final int V4L2_CID_PRIVATE_TAVARUA_EMPHASIS              = V4L2_CID_PRIVATE_BASE + 12;
   private static final int V4L2_CID_PRIVATE_TAVARUA_RDS_STD               = V4L2_CID_PRIVATE_BASE + 13;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SPACING               = V4L2_CID_PRIVATE_BASE + 14;
   private static final int V4L2_CID_PRIVATE_TAVARUA_RDSON                 = V4L2_CID_PRIVATE_BASE + 15;
   private static final int V4L2_CID_PRIVATE_TAVARUA_RDSGROUP_PROC         = V4L2_CID_PRIVATE_BASE + 16;
   private static final int V4L2_CID_PRIVATE_TAVARUA_LP_MODE               = V4L2_CID_PRIVATE_BASE + 17;
   private static final int V4L2_CID_PRIVATE_TAVARUA_IOVERC                = V4L2_CID_PRIVATE_BASE + 24;
   private static final int V4L2_CID_PRIVATE_TAVARUA_INTDET                = V4L2_CID_PRIVATE_BASE + 25;
   private static final int V4L2_CID_PRIVATE_TAVARUA_MPX_DCC               = V4L2_CID_PRIVATE_BASE + 26;
   private static final int V4L2_CID_PRIVATE_TAVARUA_AF_JUMP               = V4L2_CID_PRIVATE_BASE + 27;
   private static final int V4L2_CID_PRIVATE_TAVARUA_RSSI_DELTA            = V4L2_CID_PRIVATE_BASE + 28;
   private static final int V4L2_CID_PRIVATE_TAVARUA_HLSI                  = V4L2_CID_PRIVATE_BASE + 29;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SET_AUDIO_PATH        = V4L2_CID_PRIVATE_BASE + 41;
   private static final int V4L2_CID_PRIVATE_SINR                          = V4L2_CID_PRIVATE_BASE + 44;
   private static final int V4L2_CID_PRIVATE_TAVARUA_ON_CHANNEL_THRESHOLD  = V4L2_CID_PRIVATE_BASE + 0x2D;
   private static final int V4L2_CID_PRIVATE_TAVARUA_OFF_CHANNEL_THRESHOLD = V4L2_CID_PRIVATE_BASE + 0x2E;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SINR_THRESHOLD        = V4L2_CID_PRIVATE_BASE + 0x2F;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SINR_SAMPLES          = V4L2_CID_PRIVATE_BASE + 0x30;
   private static final int V4L2_CID_PRIVATE_SPUR_FREQ                     = V4L2_CID_PRIVATE_BASE + 0x31;
   private static final int V4L2_CID_PRIVATE_SPUR_FREQ_RMSSI               = V4L2_CID_PRIVATE_BASE + 0x32;
   private static final int V4L2_CID_PRIVATE_SPUR_SELECTION                = V4L2_CID_PRIVATE_BASE + 0x33;
   private static final int V4L2_CID_PRIVATE_AF_RMSSI_TH                   = V4L2_CID_PRIVATE_BASE + 0x36;
   private static final int V4L2_CID_PRIVATE_AF_RMSSI_SAMPLES              = V4L2_CID_PRIVATE_BASE + 0x37;
   private static final int V4L2_CID_PRIVATE_GOOD_CH_RMSSI_TH              = V4L2_CID_PRIVATE_BASE + 0x38;
   private static final int V4L2_CID_PRIVATE_SRCHALGOTYPE                  = V4L2_CID_PRIVATE_BASE + 0x39;
   private static final int V4L2_CID_PRIVATE_CF0TH12                       = V4L2_CID_PRIVATE_BASE + 0x3A;
   private static final int V4L2_CID_PRIVATE_SINRFIRSTSTAGE                = V4L2_CID_PRIVATE_BASE + 0x3B;
   private static final int V4L2_CID_PRIVATE_RMSSIFIRSTSTAGE               = V4L2_CID_PRIVATE_BASE + 0x3C;
   private static final int V4L2_CID_PRIVATE_RXREPEATCOUNT                 = V4L2_CID_PRIVATE_BASE + 0x3D;

   private static final int V4L2_CTRL_CLASS_USER = 0x980000;
   private static final int V4L2_CID_BASE        = V4L2_CTRL_CLASS_USER | 0x900;
   private static final int V4L2_CID_AUDIO_MUTE  = V4L2_CID_BASE + 9;

   private int sOnData  ;
   private int sOffData ;



   /*
    * Turn on FM Rx/Tx.
    * Rx = 1 and Tx = 2
    */
   public void fmOn(int fd, int device) {
      int re;
      FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_STATE, device );
      setAudioPath(fd, false);
      re = FmReceiverJNI.SetCalibrationNative(fd);
      if (re != 0)
         Log.d(TAG,"Calibration failed");
   }

   /*
    * Turn off FM Rx/Tx
    */
   public void fmOff(int fd){
      FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_STATE, 0 );
   }

   /*
    * set mute control
    */
   public void muteControl(int fd, boolean on) {
      if (on)
      {
         int err = FmReceiverJNI.setControlNative(fd, V4L2_CID_AUDIO_MUTE, 3 );
      } else
      {
         int err = FmReceiverJNI.setControlNative(fd, V4L2_CID_AUDIO_MUTE, 0 );
      }
   }
   /*
    * Get Interference over channel
    */
   public int IovercControl(int fd)
   {
      int ioverc = FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_IOVERC);
      Log.d(TAG, "IOVERC value is : "+ioverc);
      return ioverc;
   }
   /*
    * Get IntDet
    */
   public int IntDet(int fd)
   {
      int intdet =  FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_INTDET);
      Log.d(TAG, "IOVERC value is : "+intdet);
      return intdet;
   }

   /*
    * Get MPX_DCC
    */
   public int Mpx_Dcc(int fd)
   {
      int mpx_dcc =  FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_MPX_DCC);
      Log.d(TAG, "MPX_DCC value is : " + mpx_dcc);
      return mpx_dcc;
   }

   /*
    * Set Hi-Low injection
    */
   public int setHiLoInj(int fd, int inj)
   {
      int re =  FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_HLSI, inj);
      return re;
   }

   /*
    * Set On channel threshold
    */
   public int setOnChannelThreshold(int fd, int sBuff)
   {
      int re = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_ON_CHANNEL_THRESHOLD, sBuff);
      if ( re < 0)
         Log.e(TAG, "Failed to set On channel threshold data");
      return re;
   }

   /*
    * Set Off channel threshold
    */
   public int setOffChannelThreshold(int fd, int sBuff)
   {
      int re = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_OFF_CHANNEL_THRESHOLD, sBuff);
      if ( re < 0)
         Log.e(TAG, "Failed to set Off channel Threshold data");
      return re;
   }

   /*
    * Get On channel threshold
    */
   public int getOnChannelThreshold(int fd)
   {
      return FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_ON_CHANNEL_THRESHOLD);
   }

   /*
    * Get Off channel threshold
    */
   public int getOffChannelThreshold(int fd)
   {
      return FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_OFF_CHANNEL_THRESHOLD);
   }

   /*
    * Set sinr threshold
    */
   public int setSINRThreshold(int fd, int sBuff)
   {
      int re = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SINR_THRESHOLD, sBuff);
      if ( re < 0)
         Log.e(TAG, "Failed to set SINR threshold data");
      return re;
   }

   /*
    * Set number of sinr samples to take in to account for SINR avg calculation
    */
   public int setSINRsamples(int fd, int sBuff)
   {
      int re = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SINR_SAMPLES, sBuff);
      if ( re < 0)
         Log.e(TAG, "Failed to set SINR samples ");
      return re;
   }

   /*
    * Get SINR threshold
    */
   public int getSINRThreshold(int fd)
   {
      return  FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SINR_THRESHOLD);
   }

   /*
    * Get SINR samples
    */
   public int getSINRsamples(int fd)
   {
      return  FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SINR_SAMPLES);
   }

   /*
    * Get RMSSI Delta
    */
   public int getRmssiDelta(int fd)
   {
      int rmssiDel =  FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_RSSI_DELTA);
      return rmssiDel;
   }

   /*
    * Set RMSSI Delta
    */
   public int setRmssiDel(int fd, int delta)
   {
      int re =  FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_RSSI_DELTA, delta);
      return re;
   }

   /*
    * Set the audio path as analog/digital
    */
   public int setAudioPath(int fd, boolean value)
   {
      int mode;
      if (value)
         mode = FM_ANALOG_PATH;
      else
         mode = FM_DIGITAL_PATH;
      int re =  FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_TAVARUA_SET_AUDIO_PATH, mode);
      return re;
   }

   /*
    * Tune FM core to specified freq.
    */
   public int setStation(int fd) {
      Log.d(TAG, "** Tune Using: "+fd);
      int ret = FmReceiverJNI.setFreqNative(fd, mFreq);
      Log.d(TAG, "** Returned: "+ret);
      return ret;
   }

  /*
   * Get currently tuned freq
   */
   public int getTunedFrequency(int fd) {
      int frequency = FmReceiverJNI.getFreqNative(fd);
      Log.d(TAG, "getTunedFrequency: "+frequency);
      return frequency;
   }

   public int getFreq (){
      return mFreq;
   }

   public void setFreq (int f){
      mFreq = f;
   }
    /*
    * Get SINR value
    */
   public int getSINR(int fd)
   {
      return  FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_SINR);
   }


   /*
    * Start search list for auto presets
    */
   public int searchStationList (int fd, int mode, int preset_num,
                                   int dir, int pty )
   {
      int re;


     /* set search mode. */
      re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_TAVARUA_SRCHMODE, mode);
      if (re != 0) {
         return re;
      }

      /* set number of stations to be returned in the list */
      re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_TAVARUA_SRCH_CNT, preset_num);
      if (re != 0) {
         return re;
      }

      // RDS search list?
      if (pty > 0 ){
        re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_TAVARUA_SRCH_PTY, pty);
      }
      if (re != 0) {
         return re;
      }

      /* This triigers the search and once completed the FM core generates
       * searchListComplete event */
      re = FmReceiverJNI.startSearchNative (fd, dir );
      if (re != 0) {
         return re;
      }
      else {
         return 0;
      }

   }

   /* Read search list from buffer */
   public int[] stationList (int fd)
   {
         int freq = 0;
         int i=0, j = 0;
         int station_num = 0;
         float real_freq = 0;
         int [] stationList;
         byte [] sList = new byte[100];
         int tmpFreqByte1=0;
         int tmpFreqByte2=0;
         float lowBand, highBand;


         lowBand  = (float) (FmReceiverJNI.getLowerBandNative(fd) / 1000.00);
         highBand = (float) (FmReceiverJNI.getUpperBandNative(fd) / 1000.00);

         Log.d(TAG, "lowBand: " + lowBand);
         Log.d(TAG, "highBand: " + highBand);

         FmReceiverJNI.getBufferNative(fd, sList, 0);

         if ((int)sList[0] >0) {
            station_num = (int)sList[0];
         }
         stationList = new int[station_num+1];
         Log.d(TAG, "station_num: " + station_num);

         for (i=0;i<station_num;i++) {
            freq = 0;
            Log.d(TAG, " Byte1 = " + sList[i*2+1]);
            Log.d(TAG, " Byte2 = " + sList[i*2+2]);
            tmpFreqByte1 = sList[i*2+1] & 0xFF;
            tmpFreqByte2 = sList[i*2+2] & 0xFF;
            Log.d(TAG, " tmpFreqByte1 = " + tmpFreqByte1);
            Log.d(TAG, " tmpFreqByte2 = " + tmpFreqByte2);
            freq = (tmpFreqByte1 & 0x03) << 8;
            freq |= tmpFreqByte2;
            Log.d(TAG, " freq: " + freq);
            real_freq  = (float)(freq * 50) + (lowBand * FREQ_MUL);//tuner.rangelow * FREQ_MUL;
            Log.d(TAG, " real_freq: " + real_freq);
            if ( (real_freq < (lowBand * FREQ_MUL)) || (real_freq > (highBand * FREQ_MUL)) ) {
               Log.e(TAG, "Frequency out of band limits");
            }
            else {
               stationList[j] = (int)(real_freq);
               Log.d(TAG, " stationList: " + stationList[j]);
               j++;
            }
         }

        try {
          // mark end of list
           stationList[station_num] = 0;
        }
        catch (ArrayIndexOutOfBoundsException e) {
           Log.d(TAG, "ArrayIndexOutOfBoundsException !!");
        }

        return stationList;

   }


   /* configure various search parameters and start search */
   public int searchStations (int fd, int mode, int dwell,
                               int dir, int pty, int pi){
      int re = 0;


      Log.d(TAG, "Mode is " + mode + " Dwell is " + dwell);
      Log.d(TAG, "dir is "  + dir + " PTY is " + pty);
      Log.d(TAG, "pi is " + pi + " id " +  V4L2_CID_PRIVATE_TAVARUA_SRCHMODE);



      re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_TAVARUA_SRCHMODE, mode);
      if (re != 0) {
          Log.e(TAG, "setting of search mode failed");
          return re;
      }
      re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_TAVARUA_SCANDWELL, dwell);
      if (re != 0) {
          Log.e(TAG, "setting of scan dwell time failed");
          return re;
      }
      if (pty != 0)
      {
         re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_TAVARUA_SRCH_PTY, pty);
         if (re != 0) {
             Log.e(TAG, "setting of PTY failed");
             return re;
         }
      }

      if (pi != 0)
      {
         re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_TAVARUA_SRCH_PI, pi);
         if (re != 0) {
             Log.e(TAG, "setting of PI failed");
             return re;
         }
      }

      re = FmReceiverJNI.startSearchNative (fd, dir );
      return re;
   }

   /* force mono/stereo mode */
   public int stereoControl(int fd, boolean stereo) {

     if (stereo){
       return  FmReceiverJNI.setMonoStereoNative (fd, 1);
     }
     else {
       return  FmReceiverJNI.setMonoStereoNative (fd, 0);
     }


   }


   public void searchRdsStations(int mode,int dwelling,
                                 int direction, int RdsSrchPty, int RdsSrchPI){
   }

   /*   public void searchStationList(int listMode,int direction,
                                 int listMax,int pgmType) {
   }
   */

   /* cancel search in progress */
   public void cancelSearch (int fd){
      FmReceiverJNI.cancelSearchNative(fd);
   }

   /* Set LPM. This disables all FM core interrupts */
   public int setLowPwrMode (int fd, boolean lpmOn){

      int re=0;

      if (lpmOn){
        re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_TAVARUA_LP_MODE, 1);
      }
      else {
        re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_TAVARUA_LP_MODE, 0);
      }

      return re;

   }

   /* get current powermode of the FM core. 1 for LPM and 0 Normal mode */
   public int getPwrMode (int fd) {

      int re=0;

      re = FmReceiverJNI.getControlNative (fd, V4L2_CID_PRIVATE_TAVARUA_LP_MODE);

      return re;

   }

   public int updateSpurTable(int fd, int freq, int rmssi, boolean enable) {

      int re;

      re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_SPUR_FREQ, freq);
      if (re < 0) {
        Log.e(TAG, "Failed to program the Spur frequency value");
        return re;
      }

      re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_SPUR_FREQ_RMSSI, rmssi);
      if (re < 0) {
        Log.e(TAG, "Failed to program the RMSSI level of the Spur frequency");
        return re;
      }

      if (enable) {
        re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_SPUR_SELECTION, 1);
      }
      else {
        re = FmReceiverJNI.setControlNative (fd, V4L2_CID_PRIVATE_SPUR_SELECTION, 0);
      }
      if (re < 0) {
        Log.e(TAG, "Failed to program Spur selection");
        return re;
      }

      return re;
   }

   public int configureSpurTable(int fd) {
      return FmReceiverJNI.configureSpurTable(fd);
   }

   public int getAFJumpRmssiTh(int fd) {
      return FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_AF_RMSSI_TH);
   }

   public boolean setAFJumpRmssiTh(int fd, int th) {
      int re;
      re = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_AF_RMSSI_TH, th);
      if (re < 0) {
           Log.e(TAG, "Error in setting AF jmp Rmssi Threshold");
           return false;
      } else {
           return true;
      }
   }

   public int getAFJumpRmssiSamples(int fd) {
      return FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_AF_RMSSI_SAMPLES);
   }

   public boolean setAFJumpRmssiSamples(int fd, int samples) {
      int re;
      re = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_AF_RMSSI_SAMPLES, samples);
      if (re < 0) {
           Log.e(TAG, "Error in setting AF jmp Rmssi Samples");
           return false;
      } else {
           return true;
      }
   }

   public int getGdChRmssiTh(int fd) {
      return FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_GOOD_CH_RMSSI_TH);
   }

   public boolean setGdChRmssiTh(int fd, int th) {
      int re;
      re = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_GOOD_CH_RMSSI_TH, th);
      if (re < 0) {
           Log.e(TAG, "Error in setting Good channel Rmssi Threshold");
           return false;
      } else {
           return true;
      }
   }

   public int getSearchAlgoType(int fd) {
      return FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_SRCHALGOTYPE);
   }

   public boolean setSearchAlgoType(int fd, int saerchType) {
      int ret;
      ret = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_SRCHALGOTYPE, saerchType);
      if(ret < 0) {
         Log.e(TAG, "Error in setting Search Algo type");
         return false;
      }else {
         return true;
      }
   }

   public int getSinrFirstStage(int fd) {
      return FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_SINRFIRSTSTAGE);
   }

   public boolean setSinrFirstStage(int fd, int sinr) {
      int ret;
      ret = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_SINRFIRSTSTAGE, sinr);
      if(ret < 0) {
         Log.e(TAG, "Error in setting Sinr First Stage Threshold");
         return false;
      }else {
         return true;
      }
   }

   public int getRmssiFirstStage(int fd) {
      return FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_RMSSIFIRSTSTAGE);
   }

   public boolean setRmssiFirstStage(int fd, int rmssi) {
      int ret;
      ret = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_RMSSIFIRSTSTAGE, rmssi);
      if(ret < 0) {
         Log.e(TAG, "Error in setting Rmssi First stage Threshold");
         return false;
      }else {
         return true;
      }
   }

   public int getCFOMeanTh(int fd) {
      return FmReceiverJNI.getControlNative(fd, V4L2_CID_PRIVATE_CF0TH12);
   }

   public boolean setCFOMeanTh(int fd, int th) {
      int ret;
      ret = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_CF0TH12, th);
      if(ret < 0) {
         Log.e(TAG, "Error in setting Mean CFO Threshold");
         return false;
      }else {
         return true;
      }
   }

   public boolean setPSRxRepeatCount(int fd, int count) {
      int ret;
      ret = FmReceiverJNI.setControlNative(fd, V4L2_CID_PRIVATE_RXREPEATCOUNT, count);
      if (ret < 0) {
          return false;
      } else {
          return true;
      }
   }

}
