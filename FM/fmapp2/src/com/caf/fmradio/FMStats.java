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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import qcom.fmradio.FmReceiver;
import android.os.SystemProperties;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.os.SystemClock;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import android.os.SystemProperties;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Formatter;
import java.util.Locale;

public class FMStats extends Activity  {

    EditText txtbox1;
    Button button1;
    Button button2;
    TextView tv1;
    Button SetButton;
    Button RunButton;
    ProgressBar  pbar;
    TableLayout tLayout;

    private FmReceiver mReceiver;

    TextView  bandSweepSettingButton;

    /*Data structure for band*/
    private class Band {
      public int lFreq;
      public int hFreq;
      public int Spacing;
      public int cur_freq;
    }
    /* Data structure for Result*/
    private class Result {

      private String mFreq;
      private String mRSSI;
      private String mIoC;
      private String mIntDet;
      private String mMpxDcc;
      private String mSINR;


      public void setFreq(String aFreq) {
         this.mFreq = aFreq;
      }

      public String getFreq() {
         return mFreq;
      }

      public void setRSSI(String aRSSI) {
         this.mRSSI = aRSSI;
      }

      public String getRSSI() {
         return mRSSI;
      }

      public void setIoC(String aIoC) {
         this.mIoC = aIoC;
      }

      public String getIoC() {
         return mIoC;
      }

      public void setIntDet(String aIntDet) {
         this.mIntDet = aIntDet;
      }

      public String getIntDet() {
         return mIntDet;
      }

      public void setMpxDcc(String aMpxDcc) {
         this.mMpxDcc = aMpxDcc;
      }

      public String getMpxDcc() {
         return mMpxDcc;
      }
      public void setSINR(String aSINR) {
         this.mSINR = aSINR;
      }

      public String getSINR() {
         return mSINR;
      }
    };

    /*constant column header*/
    Result mColumnHeader = new Result();

    boolean mTestRunning = false;
    FmRfItemSelectedListener mSpinFmRfListener =
                                     new FmRfItemSelectedListener();
    RfCfgItemSelectedListener mSpinRfCfgListener =
                                     new RfCfgItemSelectedListener();
    CfgRfItemSelectedListener1 mSpinCfgRfListener1 = null;
    CfgRfItemSelectedListener2 mSpinCfgRfListener2 = null;
    BandSweepMthdsSelectedListener mSweepMthdsListener =
                                     new BandSweepMthdsSelectedListener();

    int  mTestSelected = 0;
    boolean mIsSearching = false;
    private static String LOGTAG = "FMStats";
    private IFMRadioService mService = null;
    private Thread mMultiUpdateThread = null;
    private static final int STATUS_UPDATE = 1;
    private static final int STATUS_DONE = 2;
    private static final int RECORDTIMER_UPDATE = 3;
    private static final int RECORDTIMER_EXPIRED = 4;
    private static final int STOP_ROW_ID = 200;
    private static final int NEW_ROW_ID = 300;
    private int mStopIds = STOP_ROW_ID;
    private int mNewRowIds = NEW_ROW_ID;
    private static final int SCAN_DWELL_PERIOD = 1;

    private static final int CUR_FREQ_TEST = 0;
    private static final int CUR_MULTI_TEST = 1;
    private static final int SEARCH_TEST =2;
    private static final int SWEEP_TEST = 3;
    private Band mBand = null;
    private Band mSync = null;
    int Lo = 1, Auto = 0;

    private FileOutputStream mFileCursor = null;
    private String mCurrentFileName = null;

    Spinner spinOptionFmRf;
    Spinner spinOptionBandSweepMthds;
    ArrayAdapter<CharSequence> adaptCfgRf;
    ArrayAdapter<CharSequence> adaptRfCfg;
    ArrayAdapter<CharSequence> adaptFmRf;
    ArrayAdapter<CharSequence> bandSweepMthds;

    private static boolean mIsTransportSMD = false;

    private static final int MPX_DCC = 0;
    private static final int SINR_INTF = 1;
    private static final int MIN_SINR_FIRST_STAGE = -128;
    private static final int MAX_SINR_FIRST_STAGE = 127;
    private static final int MIN_RMSSI_FIRST_STAGE = -128;
    private static final int MAX_RMSSI_FIRST_STAGE = 127;
    private static final int MIN_CF0TH12 = -2147483648;
    private static final int MAX_CF0TH12 = 2147483647;
    private static final int MIN_SINR_TH = -128;
    private static final int MAX_SINR_TH = 127;
    private static final int MIN_SINR_SAMPLES = 0;
    private static final int MAX_SINR_SAMPLES = 255;
    private static final int MIN_AF_JMP_RMSSI_TH = 0;
    private static final int MAX_AF_JMP_RMSSI_TH = 65535;
    private static final int MIN_GD_CH_RMSSI_TH = -128;
    private static final int MAX_GD_CH_RMSSI_TH = 127;
    private static final int MIN_AF_JMP_RMSSI_SAMPLES = 0;
    private static final int MAX_AF_JMP_RMSSI_SAMPLES = 255;

    private static final int DIALOG_BAND_SWEEP_SETTING = 1;

    private int prevDwellTime = 2; //2secs
    private int prevDelayTime = 0;//0secs
    private int prevSweepMthd = 0; //Manual (using band min, max)

    private int curSweepMthd = 0;

    private Thread mRecordUpdateHandlerThread = null;
    boolean mRecording = false;


    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale
                                                       .getDefault());
    private static final Object[] sTimeArgs = new Object[5];

    private final String FREQ_LIST_FILE_NAME = "/freq_list_comma_separated.txt";
    private static final String BAND_SWEEP_START_DELAY_TIMEOUT = "com.caf.fmradio.SWEEP_START_DELAY_EXP";
    private static final String BAND_SWEEP_DWELL_DELAY_TIMEOUT = "com.caf.fmradio.SWEEP_DWELL_DELAY_EXP";

    private BroadcastReceiver mBandSweepDelayExprdListener = null;
    private BroadcastReceiver mBandSweepDwellExprdListener = null;

    private WakeLock mWakeLock;

    private GetNextFreqInterface mNextFreqInterface;
    private CommaSeparatedFreqFileReader mFreqFileReader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.fmstats);

        spinOptionFmRf = (Spinner)findViewById(R.id.spinner);
        adaptFmRf = ArrayAdapter.createFromResource
                       (this, R.array.stats_options
                         , android.R.layout.simple_spinner_item);
        adaptFmRf.setDropDownViewResource
                   (android.R.layout.simple_spinner_dropdown_item);
        if (spinOptionFmRf != null) {
           spinOptionFmRf.setAdapter(adaptFmRf);
           spinOptionFmRf.setOnItemSelectedListener(mSpinFmRfListener);
        }

        bandSweepMthds = ArrayAdapter.createFromResource
                          (this, R.array.band_sweep_methods,
                            android.R.layout.simple_spinner_item);

        checkTransportLayer();
        if (!isTransportLayerSMD()) {
            mSpinCfgRfListener1 = new CfgRfItemSelectedListener1();
            adaptCfgRf = ArrayAdapter.createFromResource(
                           this, R.array.cfg_rf1,
                           android.R.layout.simple_spinner_item);
        }else {
            mSpinCfgRfListener2 = new CfgRfItemSelectedListener2();
            adaptCfgRf = ArrayAdapter.createFromResource(
                           this, R.array.cfg_rf2,
                           android.R.layout.simple_spinner_item);
        }
        adaptRfCfg = ArrayAdapter.createFromResource(
            this, R.array.rf_cfg, android.R.layout.simple_spinner_item);

        tLayout = (TableLayout) findViewById(R.id.maintable);

        if(mReceiver == null)
            mReceiver = new FmReceiver();

        long curTime = System.currentTimeMillis();
        mCurrentFileName = "FMStats_".concat(
                                 Long.toString(curTime).concat(".txt")
                              );
        Log.e(LOGTAG,"Filename is " + mCurrentFileName);
        try {
            mFileCursor = openFileOutput(
                                 mCurrentFileName,
                                 Context.MODE_PRIVATE);
            if(null != mFileCursor) {
               Log.e(LOGTAG, "location of the file is"+getFilesDir());
            }
        }catch (IOException e) {
            e.printStackTrace();
            Log.e(LOGTAG,"Couldn't create the file to writeLog");
            mCurrentFileName = null;
        }

        if (false == bindToService(this, osc)) {
            Log.d(LOGTAG, "onCreate: Failed to Start Service");
        }else {
            Log.d(LOGTAG, "onCreate: Start Service completed successfully");
        }

        /*Initialize the column header with
        constant values*/
        mColumnHeader.setFreq("Freq");
        mColumnHeader.setRSSI("RMSSI");
        mColumnHeader.setIoC("IoC");
        mColumnHeader.setSINR("SINR");
        mColumnHeader.setMpxDcc("Offset");
        mColumnHeader.setIntDet("IntDet");

        bandSweepSettingButton = (TextView)findViewById(R.id.BandSweepSetting);
        if(bandSweepSettingButton != null) {
           bandSweepSettingButton.setOnClickListener(mClicktBandSweepSettingListener);
        }

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

        registerBandSweepDelayExprdListener();
        registerBandSweepDwellExprdListener();
    }

    @Override
    public void onStart() {
       super.onStart();
       if(isRecording()) {
          Log.d(LOGTAG, "onStart");
          initiateRecordThread();
       }
    }

    @Override
    public void onStop() {
       Log.d(LOGTAG, "onStop");
       super.onStop();
       if(isRecording()) {
          try {
               if(null != mRecordUpdateHandlerThread) {
                  mRecordUpdateHandlerThread.interrupt();
               }
          }catch (NullPointerException e) {
               e.printStackTrace();
          }
       }
    }

    @Override
    public void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        stopCurTest();
        if(mUIUpdateHandlerHandler != null) {
           mUIUpdateHandlerHandler.removeCallbacksAndMessages(null);
        }
        if(mHandler != null) {
           mHandler.removeCallbacksAndMessages(null);
        }
        unRegisterBroadcastReceiver(mBandSweepDelayExprdListener);
        unRegisterBroadcastReceiver(mBandSweepDwellExprdListener);
        if(null != mFileCursor ) {
            try {
                mFileCursor.close();
            } catch (IOException e) {
                 e.printStackTrace();
           }
        }

        unbindFromService(this);
        Log.d(LOGTAG, "onDestroy: unbindFromService completed");
        mReceiver = null;
        mService = null;
        removeDialog(DIALOG_BAND_SWEEP_SETTING);
        mWakeLock.release();
        super.onDestroy();
    }

    private View.OnClickListener mOnRunListener = new View.OnClickListener() {
        public void onClick(View v) {
           if(false == mTestRunning) {
              clearPreviousTestResults();
              mTestRunning = true;
              if(mTestSelected == SWEEP_TEST) {
                 disableBandSweepSetting();
              }
              runCurrentTest();
           }else {
              mTestRunning = false;
              SetButtonState(true);
              /*Stop the thread by interrupting it*/
              if(mMultiUpdateThread != null) {
                 mMultiUpdateThread.interrupt();
                 mMultiUpdateThread = null;
              }

              if(SEARCH_TEST == mTestSelected) {
                 try {
                      mService.cancelSearch();
                 }catch (RemoteException e) {
                      e.printStackTrace();
                 }
              }
              if(mTestSelected == SWEEP_TEST) {
                 stopBandSweep();
                 sendStatusDoneMsg();
                 enableBandSweepSetting();
              }
           }
       }
    };

    private void clearPreviousTestResults() {
       TableLayout tl = (TableLayout)findViewById(R.id.maintable);
       if (tl != null) {
           tl.removeAllViewsInLayout();
       }
       mNewRowIds = NEW_ROW_ID;
    }

    private void SetButtonState(boolean state) {
       // Get the TableRow
       Button RunButton = (Button)findViewById(R.id.Runbutton);
       ProgressBar  pbar = (ProgressBar)findViewById(R.id.progressbar);
       /*Update the state of the button based on
        state*/
       if(state) {
          if(RunButton != null) {
             RunButton.setText(R.string.test_run);
          }
          if(pbar != null) {
             pbar.setVisibility(View.INVISIBLE);
          }
          if(mTestSelected == SWEEP_TEST) {
             enableBandSweepSetting();
          }
       }else {
          if(RunButton != null) {
             RunButton.setText("Stop Test");
          }
          if(pbar != null) {
             pbar.setVisibility(View.VISIBLE);
          }
       }
    }

    private void chooseFMRFoption(){
       String[] szTestInformation = getResources().getStringArray(
                        R.array.stats_options);
       final StringBuilder szbTestHeader = new StringBuilder();
       szbTestHeader.append("running test:").append
                              (szTestInformation[mTestSelected]);
       String szTestHeader = new String(szbTestHeader);
       switch(mTestSelected)
       {
       case 1:
               RunButton = (Button)findViewById(R.id.Runbutton);
               if(RunButton != null) {
                  RunButton.setVisibility(View.INVISIBLE);
               }
               pbar = (ProgressBar) findViewById(R.id.progressbar);
               if(pbar != null) {
                  pbar.setVisibility(View.INVISIBLE);
               }
               adaptCfgRf.setDropDownViewResource
                           (android.R.layout.simple_spinner_dropdown_item);
               spinOptionFmRf.setAdapter(adaptCfgRf);
               if(isTransportLayerSMD())
                  spinOptionFmRf.setOnItemSelectedListener
                                   (mSpinCfgRfListener2);
               else
                  spinOptionFmRf.setOnItemSelectedListener
                                   (mSpinCfgRfListener1);
               break;
       case 2:
               txtbox1 = (EditText)findViewById(R.id.txtbox1);
               tv1 = (TextView)findViewById(R.id.label);
               if(txtbox1 != null) {
                  txtbox1.setVisibility(View.INVISIBLE);
               }
               if(tv1 != null) {
                  tv1.setVisibility(View.INVISIBLE);
               }
               Button SetButton = (Button)findViewById(R.id.Setbutton);
               if(SetButton != null) {
                  SetButton.setVisibility(View.INVISIBLE);
               }
               adaptRfCfg.setDropDownViewResource
                               (android.R.layout.simple_spinner_dropdown_item);
               spinOptionFmRf.setAdapter(adaptRfCfg);
               spinOptionFmRf.setOnItemSelectedListener(mSpinRfCfgListener);
               break;
        }
    }

    private View.OnClickListener mOnSetRmssitListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
               int rdel = Integer.parseInt(a);
               Log.d(LOGTAG, "Value of RMSSI DELTA is : " + rdel);
               mReceiver.setRmssiDel(rdel);
          }catch (NumberFormatException e) {
               Log.e(LOGTAG, "Value entered is not in correct format: " + a);
               txtbox1.setText("");
          }catch (NullPointerException e) {
               e.printStackTrace();
          }
       }
    };

    private View.OnClickListener mOnSetRxRePeatCount = new View.OnClickListener() {
        public void onClick(View v) {
            String a;
            a =  txtbox1.getText().toString();
            try {
                 int count = Integer.parseInt(a);
                 Log.d(LOGTAG, "Value entered for mOnSetRxRePeatCount: " + count);
                 if((count < 0) ||
                     (count > 255))
                     return;
                 if(mService != null) {
                    try {
                         mService.setRxRepeatCount(count);
                    } catch (RemoteException e) {
                         e.printStackTrace();
                    }
                 }
            } catch (NumberFormatException e) {
                 Log.e(LOGTAG, "Value entered is not in correct format : " + a);
                 txtbox1.setText("");
            }
        }
    };

    private View.OnClickListener mOnSetSigThListener =
     new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int rdel = Integer.parseInt(a);
              Log.d(LOGTAG, "Value of Signal Th. is : " + rdel);
              mReceiver.setSignalThreshold(rdel);
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format: " + a);
              txtbox1.setText("");
          }catch (NullPointerException e) {
              e.printStackTrace();
          }
      }
    };

    private View.OnClickListener mOnSetSinrSmplCntListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int rdel = Integer.parseInt(a);
              Log.d(LOGTAG, "Value of Sinr Samples count is : " + rdel);
              if(mService != null) {
                 try {
                     mService.setSinrSamplesCnt(rdel);
                 }catch (RemoteException e) {
                     e.printStackTrace();
                 }
              }
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format: " + a);
              txtbox1.setText("");
          }
      }
    };

    private View.OnClickListener mOnSetSinrThListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int rdel = Integer.parseInt(a);
              Log.d(LOGTAG, "Value of Sinr Th is : " + rdel);
              if(mService != null) {
                 try {
                     mService.setSinrTh(rdel);
                 }catch (RemoteException e) {
                     e.printStackTrace();
                 }
              }
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format: " + a);
              txtbox1.setText("");
          }
      }
    };

    private View.OnClickListener mOnSetIntfLowThListener =
     new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int rdel = Integer.parseInt(a);
              Log.d(LOGTAG, "Value of Intf Det Low Th is : " + rdel);
              if(mService != null) {
                 try {
                     mService.setIntfDetLowTh(rdel);
                 }catch (RemoteException e) {
                     e.printStackTrace();
                 }
              }
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format: " + a);
              txtbox1.setText("");
          }
      }
    };
    private View.OnClickListener mOnSetIntfHighThListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int rdel = Integer.parseInt(a);
              Log.d(LOGTAG, "Value of Intf Det Low Th is : " + rdel);
              if(mService != null) {
                 try {
                     mService.setIntfDetHighTh(rdel);
                 }catch (RemoteException e) {
                     e.printStackTrace();
                 }
              }
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format : " + a);
              txtbox1.setText("");
          }
       }
    };

    private View.OnClickListener mOnSetSinrFirstStageListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int sinr = Integer.parseInt(a);
              Log.d(LOGTAG, "Value entered for SINR FIRST STAGE is : " + sinr);
              if((sinr < MIN_SINR_FIRST_STAGE) ||
                     (sinr > MAX_SINR_FIRST_STAGE))
                  return;
              if(mService != null) {
                 try {
                     mService.setSinrFirstStage(sinr);
                 }catch (RemoteException e) {
                     e.printStackTrace();
                 }
              }
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format : " + a);
              txtbox1.setText("");
          }
       }
    };

    private View.OnClickListener mOnSetRmssiFirstStageListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int rmssi = Integer.parseInt(a);
              Log.d(LOGTAG, "Value entered for RMSSI FIRST STAGE is: " + rmssi);
              if((rmssi < MIN_RMSSI_FIRST_STAGE) ||
                     (rmssi > MAX_RMSSI_FIRST_STAGE))
                  return;
              if(mService != null) {
                 try {
                     mService.setRmssiFirstStage(rmssi);
                 }catch (RemoteException e) {
                     e.printStackTrace();
                 }
              }
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format : " + a);
              txtbox1.setText("");
          }
       }
    };

    private View.OnClickListener mOnSetCFOMeanThListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int cf0 = Integer.parseInt(a);
              Log.d(LOGTAG, "Value entered for CF0TH12 is: " + cf0);
              if((cf0 < MIN_CF0TH12) ||
                     (cf0 > MAX_CF0TH12))
                  return;
              if(mService != null) {
                 try {
                     mService.setCFOMeanTh(cf0);
                 }catch (RemoteException e) {
                     e.printStackTrace();
                 }
              }
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format : " + a);
              txtbox1.setText("");
          }
       }
    };

    private View.OnClickListener mOnSetSearchMPXDCCListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          Log.d(LOGTAG, "Value entered for search is: MPX DCC");
          if(mService != null) {
             try {
                  mService.setSearchAlgoType(MPX_DCC);
             }catch (RemoteException e) {
                  e.printStackTrace();
             }
          }
       }
    };

    private View.OnClickListener mOnSetSearchSinrIntfListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          Log.d(LOGTAG, "Value entered for search is: SINR INTF");
          if(mService != null) {
             try {
                  mService.setSearchAlgoType(SINR_INTF);
             }catch (RemoteException e) {
                  e.printStackTrace();
             }
          }
       }
    };

    private View.OnClickListener mOnSetAfJmpRmssiThListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int th = Integer.parseInt(a);
              Log.d(LOGTAG, "Value entered for AfJmpRmssiTh is: " + th);
              if((th < MIN_AF_JMP_RMSSI_TH) ||
                     (th > MAX_AF_JMP_RMSSI_TH))
                  return;
              if(mService != null) {
                 try {
                     mService.setAfJmpRmssiTh(th);
                 }catch (RemoteException e) {
                     e.printStackTrace();
                 }
              }
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format : " + a);
              txtbox1.setText("");
          }
       }
    };

    private View.OnClickListener mOnSetGdChRmssiThListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int th = Integer.parseInt(a);
              Log.d(LOGTAG, "Value entered for Good channel Rmssi Th is: " + th);
              if((th < MIN_GD_CH_RMSSI_TH) ||
                     (th > MAX_GD_CH_RMSSI_TH))
                  return;
              if(mService != null) {
                 try {
                     mService.setGoodChRmssiTh(th);
                 }catch (RemoteException e) {
                     e.printStackTrace();
                 }
              }
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format : " + a);
              txtbox1.setText("");
          }
       }
    };

    private View.OnClickListener mOnSetAfJmpRmssiSmplsCntListener =
    new View.OnClickListener() {
       public void onClick(View v) {
          String a;
          a = txtbox1.getText().toString();
          try {
              int cnt = Integer.parseInt(a);
              Log.d(LOGTAG, "Value entered for AfJmpRmssiSamples is: " + cnt);
              if((cnt < MIN_AF_JMP_RMSSI_SAMPLES) ||
                     (cnt > MAX_AF_JMP_RMSSI_SAMPLES))
                  return;
              if(mService != null) {
                 try {
                     mService.setAfJmpRmssiSamplesCnt(cnt);
                 }catch (RemoteException e) {
                     e.printStackTrace();
                 }
              }
          }catch (NumberFormatException e) {
              Log.e(LOGTAG, "Value entered is not in correct format : " + a);
              txtbox1.setText("");
          }
       }
    };

    public class CfgRfItemSelectedListener1 implements OnItemSelectedListener {
        public void onItemSelected(
                     AdapterView<?> parent, View view, int pos, long id) {
            Log.d("Table","onItemSelected is hit with " + pos);
            txtbox1 = (EditText)findViewById(R.id.txtbox1);
            tv1 = (TextView)findViewById(R.id.label);
            Button SetButton = (Button)findViewById(R.id.Setbutton);
            tLayout.setVisibility(View.INVISIBLE);
            switch(pos)
            {
            case 0:
                   if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                   }
                   if (tv1 != null) {
                       tv1.setText(R.string.enter_rssi);
                       tv1.setVisibility(View.VISIBLE);
                   }
                   if (SetButton != null) {
                       SetButton.setText(R.string.set_rmmsi_delta);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetRmssitListener);
                   }
                   break;
            case 1:
                   if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                   }
                   if (tv1 != null) {
                       tv1.setText(R.string.enter_sigth);
                       tv1.setVisibility(View.VISIBLE);
                   }
                   if (SetButton != null) {
                       SetButton.setText(R.string.set_sigth);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetSigThListener);
                   }
                   break;
            case 2:
                   tLayout.removeAllViewsInLayout();
                   mNewRowIds = NEW_ROW_ID;
                   tLayout.setVisibility(View.VISIBLE);
                   if (txtbox1 != null) {
                       txtbox1.setVisibility(View.INVISIBLE);
                   }
                   if (tv1 != null) {
                       tv1.setVisibility(View.INVISIBLE);
                   }
                   if (SetButton != null) {
                       SetButton.setVisibility(View.INVISIBLE);
                   }
                   adaptRfCfg.setDropDownViewResource(
                              android.R.layout.simple_spinner_dropdown_item);
                   spinOptionFmRf.setAdapter(adaptRfCfg);
                   spinOptionFmRf.setOnItemSelectedListener(
                                                 mSpinRfCfgListener);
                   break;
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do Nothing
        }
    }

    public class CfgRfItemSelectedListener2 implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent,
                                    View view, int pos, long id) {
            Log.d("Table","onItemSelected is hit with " + pos);
            int ret = Integer.MAX_VALUE;
            txtbox1 = (EditText) findViewById(R.id.txtbox1);
            tv1 = (TextView) findViewById(R.id.label);
            button1 = (Button)findViewById(R.id.SearchMpxDcc);
            button2 = (Button)findViewById(R.id.SearchSinrInt);
            Button SetButton = (Button)findViewById(R.id.Setbutton);
            tLayout.setVisibility(View.INVISIBLE);
            switch(pos)
            {
                case 0:
                    if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setText(R.string.enter_SinrSmplsCnt);
                       tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setText(R.string.set_SinrSmplsCnt);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetSinrSmplCntListener);
                    }
                    break;
                case 1:
                    if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setText(R.string.enter_SinrTh);
                       tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setText(R.string.set_SinrTh);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetSinrThListener);
                    }
                    break;
                case 2:
                    if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setText(R.string.enter_IntfLowTh);
                       tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setText(R.string.set_IntfLowTh);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetIntfLowThListener);
                    }
                    break;
                case 3:
                    if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setText(R.string.enter_IntfHighTh);
                       tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setText(R.string.set_IntfHighTh);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetIntfHighThListener);
                    }
                    break;
                case 4:
                    if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setText(R.string.enter_SinrFirstStage);
                       tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setText(R.string.set_SinrFirstStage);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetSinrFirstStageListener);
                    }
                    break;
                case 5:
                    if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setText(R.string.enter_RmssiFirstStage);
                       tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setText(R.string.set_RmssiFirstStage);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetRmssiFirstStageListener);
                    }
                    break;
                case 6:
                    if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setText(R.string.enter_CF0Th12);
                       tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setText(R.string.set_CF0Th12);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetCFOMeanThListener);
                    }
                    break;
                case 7:
                    if (txtbox1 != null) {
                        txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setVisibility(View.INVISIBLE);
                    }
                    if(button1 != null) {
                       button1.setText(R.string.search_algo_mpx);
                       button1.setVisibility(View.VISIBLE);
                       button1.setOnClickListener(mOnSetSearchMPXDCCListener);
                    }
                    if(button2 != null) {
                       button2.setText(R.string.search_algo_sinrint);
                       button2.setVisibility(View.VISIBLE);
                       button2.setOnClickListener(mOnSetSearchSinrIntfListener);
                    }
                    break;
                case 8:
                    if (txtbox1 != null) {
                        txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setText("");
                        tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setVisibility(View.INVISIBLE);
                    }
                    try {
                        if(mService != null)
                           ret = mService.getSinrSamplesCnt();
                           Log.d(LOGTAG, "Get Sinr Samples Count: " + ret);
                           if((ret >= MIN_SINR_SAMPLES) &&
                                  (ret <= MAX_SINR_SAMPLES))
                              tv1.setText(" " + String.valueOf(ret));
                    }catch (RemoteException e) {
                    }
                    break;
                case 9:
                    if (txtbox1 != null) {
                        txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setText("");
                        tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setVisibility(View.INVISIBLE);
                    }
                    try {
                        if(mService != null)
                           ret = mService.getSinrTh();
                           Log.d(LOGTAG, "Get Sinr Threshold: " + ret);
                           if((ret >= MIN_SINR_TH) &&
                                   (ret <= MAX_SINR_TH))
                              tv1.setText(" " + String.valueOf(ret));
                    }catch (RemoteException e) {

                    }
                    break;
                case 10:
                    if (txtbox1 != null) {
                        txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setText("");
                        tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setVisibility(View.INVISIBLE);
                    }
                    try {
                        if(mService != null) {
                           ret = mService.getSinrFirstStage();
                           Log.d(LOGTAG, "Get Sinr First Stage: " + ret);
                           if (ret >= MIN_SINR_FIRST_STAGE &&
                                   ret <= MAX_SINR_FIRST_STAGE)
                               tv1.setText(" " + String.valueOf(ret));
                        }
                    }catch (RemoteException e) {

                    }
                    break;
                case 11:
                    if (txtbox1 != null) {
                        txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setText("");
                        tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setVisibility(View.INVISIBLE);
                    }
                    try {
                        if(mService != null) {
                           ret = mService.getRmssiFirstStage();
                           Log.d(LOGTAG, "Get Rmssi First Stage: " + ret);
                           if (ret >= MIN_RMSSI_FIRST_STAGE &&
                                   ret <= MAX_RMSSI_FIRST_STAGE)
                               tv1.setText(" " + String.valueOf(ret));
                        }
                    }catch (RemoteException e) {

                    }
                    break;
                case 12:
                    if (txtbox1 != null) {
                        txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setText("");
                        tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setVisibility(View.INVISIBLE);
                    }
                    try {
                        if(mService != null) {
                           ret = mService.getCFOMeanTh();
                           Log.d(LOGTAG, "Get CF0 Threshold: " + ret);
                           if (ret >= MIN_CF0TH12 &&
                                   ret <= MAX_CF0TH12)
                               tv1.setText(" " + String.valueOf(ret));
                        }
                    }catch (RemoteException e) {

                    }
                    break;
                case 13:
                    if (txtbox1 != null) {
                        txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setText("");
                        tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setVisibility(View.INVISIBLE);
                    }
                    try {
                        if(mService != null) {
                           ret = mService.getSearchAlgoType();
                           Log.d(LOGTAG, "Search Type: " + ret);
                           if (ret == MPX_DCC)
                               tv1.setText(R.string.search_algo_mpx);
                           else if(ret == SINR_INTF)
                               tv1.setText(R.string.search_algo_sinrint);
                        }
                    }catch (RemoteException e) {

                    }
                    break;
                case 14:
                    if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setText(R.string.enter_AfJmpRmssiTh);
                       tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setText(R.string.set_AfJmpRmssiTh);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetAfJmpRmssiThListener);
                    }
                    break;
                case 15:
                    if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setText(R.string.enter_GdChRmssiTh);
                       tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setText(R.string.set_GdChRmssiTh);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetGdChRmssiThListener);
                    }
                    break;
                case 16:
                    if (txtbox1 != null) {
                       txtbox1.setText(R.string.type_rd);
                       txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setText(R.string.enter_AfJmpRmssiSmplsCnt);
                       tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setText(R.string.set_AfJmpRmssiSmplsCnt);
                       SetButton.setVisibility(View.VISIBLE);
                       SetButton.setOnClickListener(mOnSetAfJmpRmssiSmplsCntListener);
                    }
                    break;
                case 17:
                    if (txtbox1 != null) {
                        txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setText("");
                        tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setVisibility(View.INVISIBLE);
                    }
                    try {
                        if(mService != null) {
                           ret = mService.getAfJmpRmssiTh();
                           Log.d(LOGTAG, "Get Af Jmp Rmssi Th: " + ret);
                           if ((ret >= MIN_AF_JMP_RMSSI_TH) &&
                                  (ret <= MAX_AF_JMP_RMSSI_TH))
                               tv1.setText(" " + String.valueOf(ret));
                        }
                    }catch (RemoteException e) {

                    }
                    break;
                case 18:
                    if (txtbox1 != null) {
                        txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setText("");
                        tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setVisibility(View.INVISIBLE);
                    }
                    try {
                        if(mService != null) {
                           ret = mService.getGoodChRmssiTh();
                           Log.d(LOGTAG, "Get GoodChRmssi Threshold: " + ret);
                           if ((ret >= MIN_GD_CH_RMSSI_TH) &&
                                  (ret <= MAX_GD_CH_RMSSI_TH))
                               tv1.setText(" " + String.valueOf(ret));
                        }
                    }catch (RemoteException e) {

                    }
                    break;
                case 19:
                    if (txtbox1 != null) {
                        txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setText("");
                        tv1.setVisibility(View.VISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setVisibility(View.INVISIBLE);
                    }
                    try {
                        if(mService != null) {
                           ret = mService.getAfJmpRmssiSamplesCnt();
                           Log.d(LOGTAG, "Get AfJmpRmssiSamples count: " + ret);
                           if ((ret >= MIN_AF_JMP_RMSSI_SAMPLES) &&
                                  (ret <= MAX_AF_JMP_RMSSI_SAMPLES))
                               tv1.setText(" " + String.valueOf(ret));
                        }
                    }catch (RemoteException e) {

                    }
                    break;
                case 20:
                    tLayout.removeAllViewsInLayout();
                    mNewRowIds = NEW_ROW_ID;
                    tLayout.setVisibility(View.VISIBLE);
                    if (txtbox1 != null) {
                       txtbox1.setVisibility(View.INVISIBLE);
                    }
                    if (tv1 != null) {
                       tv1.setVisibility(View.INVISIBLE);
                    }
                    if (SetButton != null) {
                       SetButton.setVisibility(View.INVISIBLE);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    adaptRfCfg.setDropDownViewResource(
                              android.R.layout.simple_spinner_dropdown_item);
                    spinOptionFmRf.setAdapter(adaptRfCfg);
                    spinOptionFmRf.setOnItemSelectedListener(mSpinRfCfgListener);
                    break;
                case 21:
                    if (txtbox1 != null) {
                        txtbox1.setText(R.string.type_rd);
                        txtbox1.setVisibility(View.VISIBLE);
                    }
                    if (tv1 != null) {
                        tv1.setText(R.string.enter_RxRePeatCount);
                        tv1.setVisibility(View.VISIBLE);
                    }
                    if (SetButton != null) {
                        SetButton.setText(R.string.set_RxRePeatCount);
                        SetButton.setVisibility(View.VISIBLE);
                        SetButton.setOnClickListener(mOnSetRxRePeatCount);
                    }
                    if(button1 != null) {
                       button1.setVisibility(View.INVISIBLE);
                    }
                    if(button2 != null) {
                       button2.setVisibility(View.INVISIBLE);
                    }
                    break;
            }
        }
        public void onNothingSelected(AdapterView<?> parent) {
            // Do Nothing
        }
    }

    public class RfCfgItemSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent,
                                    View view, int pos, long id) {
            Log.d("Table","onItemSelected is hit with "+pos);
            tLayout.setVisibility(View.INVISIBLE);
            if (mTestRunning)
                stopCurTest();
            switch(pos)
            {
                case 0:
                case 1:
                case 2:
                case 3:
                    mTestSelected = pos;
                    tLayout.removeAllViewsInLayout();
                    mNewRowIds = NEW_ROW_ID;
                    tLayout.setVisibility(View.VISIBLE);
                    RunButton = (Button)findViewById(R.id.Runbutton);
                    if (RunButton != null) {
                       RunButton.setText(R.string.test_run);
                       RunButton.setVisibility(View.VISIBLE);
                       RunButton.setOnClickListener(mOnRunListener);
                    }
                    if(mTestSelected == SWEEP_TEST) {
                       enableBandSweepSetting();
                    }else {
                       disableBandSweepSetting();
                    }
                    break;
                case 4:
                    RunButton = (Button)findViewById(R.id.Runbutton);
                    if (RunButton != null) {
                       RunButton.setVisibility(View.INVISIBLE);
                    }
                    pbar = (ProgressBar) findViewById(R.id.progressbar);
                    if (pbar != null) {
                       pbar.setVisibility(View.INVISIBLE);
                    }
                    adaptCfgRf.setDropDownViewResource(
                                     android.R.layout.simple_spinner_dropdown_item);
                    spinOptionFmRf.setAdapter(adaptCfgRf);
                    if(isTransportLayerSMD())
                       spinOptionFmRf.setOnItemSelectedListener(mSpinCfgRfListener2);
                    else
                       spinOptionFmRf.setOnItemSelectedListener(mSpinCfgRfListener1);
                    disableBandSweepSetting();
                    break;
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do Nothing
        }
    }
    public class FmRfItemSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            Log.d("Table","onItemSelected is hit with "+pos);
            mTestSelected = pos;
            tLayout.setVisibility(View.INVISIBLE);
            chooseFMRFoption();
        }
        public void onNothingSelected(AdapterView<?> parent) {
            // Do Nothing
        }
    }
    private void checkTransportLayer() {
        String transportLayer = "";

        transportLayer = SystemProperties.get("ro.qualcomm.bt.hci_transport");
        if(transportLayer.equals("smd"))
           mIsTransportSMD = true;
    }
    private boolean isTransportLayerSMD() {
        return mIsTransportSMD;
    }
    private void createResult(Result aRes) {
        // Get the TableLayout
        TableLayout tl = (TableLayout) findViewById(R.id.maintable);
        if (tl == null) {
           return;
        }

         /* Create a new row to be added. */
        mNewRowIds++;
        TableRow tr2 = new TableRow(getApplicationContext());
        int width = getWindowManager().getDefaultDisplay().getWidth();
        tr2.setLayoutParams(new LayoutParams(
                     LayoutParams.FILL_PARENT,
                     LayoutParams.WRAP_CONTENT));
        tr2.setId(mNewRowIds);
        /* Create a Button to be the row-content. */
        TextView colFreq = new TextView(getApplicationContext());
        colFreq.setText(aRes.getFreq());
        colFreq.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        colFreq.setWidth(width/4);
                /* Add Button to row. */
        tr2.addView(colFreq);

        TextView colRMSSI = new TextView(getApplicationContext());
        colRMSSI.setText(aRes.getRSSI());
        colRMSSI.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        colRMSSI.setWidth(width/4);
        tr2.addView(colRMSSI);

        TextView colIoC = new TextView(getApplicationContext());
        colIoC.setText(aRes.getIoC());
        colIoC.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        colIoC.setWidth(width/4);
        tr2.addView(colIoC);
        if(isTransportLayerSMD())
        {
             TextView colSINR = new TextView(getApplicationContext());
             colSINR.setText(aRes.getSINR());
             colSINR.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
             colSINR.setWidth(width/4);
             tr2.addView(colSINR);
        } else
        {
             TextView colMpxDcc = new TextView(getApplicationContext());
             colMpxDcc.setText(aRes.getMpxDcc());
             colMpxDcc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
             colMpxDcc.setWidth(width/4);
             tr2.addView(colMpxDcc);
        }
          /* Add row to TableLayout. */
          /* Add row to TableLayout. */
        tl.addView(tr2,new TableLayout.LayoutParams(
             LayoutParams.FILL_PARENT,
             LayoutParams.WRAP_CONTENT));
        if(null != mFileCursor)
        {
           try {
                 StringBuilder tempStr = new StringBuilder();
                 tempStr.append(String.format("%10s", aRes.getFreq()));
                 tempStr.append(String.format("%10s", aRes.getRSSI()));
                 tempStr.append(String.format("%10s", aRes.getIoC()));
                 tempStr.append(String.format("%10s", aRes.getIntDet()));
                 if(isTransportLayerSMD())
                 {
                    tempStr.append(String.format("%10s", aRes.getSINR()));
                 } else
                 {
                    tempStr.append(String.format("%10s", aRes.getMpxDcc()));
                 }
                 tempStr.append("\r\n");
                 String testStr = new String(tempStr);
                 mFileCursor.write(testStr.getBytes());
           } catch(IOException ioe) {
                 ioe.printStackTrace();
           }
        }
    }

    private void runCurrentTest() {
        Log.d(LOGTAG, "The test being run is" +mTestSelected);

        //get test summary
        String[] szTestInformation = getResources().getStringArray(
                        R.array.rf_cfg);
        final StringBuilder szbTestHeader = new StringBuilder();
        szbTestHeader.append("running test:").append(szTestInformation[mTestSelected]);
        szbTestHeader.append("\r\n");
        String szTestHeader = new String(szbTestHeader);
        if(null != mFileCursor ) {
           try {
                mFileCursor.write(szTestHeader.getBytes());
           } catch (IOException ioe) {
                ioe.printStackTrace();
           }
        }
        switch(mTestSelected)
        {
        case CUR_FREQ_TEST:
             Log.d(LOGTAG,"Current Freq test is going to run");
             int freq = FmSharedPreferences.getTunedFrequency();
             Result res = GetFMStatsForFreq(freq);
             createResult(mColumnHeader);
             if(res != null)
                createResult(res);
              mTestRunning = false;
              break;
        case CUR_MULTI_TEST:
             /*Set it to ready to Stop*/
             SetButtonState(false);
             createResult(mColumnHeader);

             if(mMultiUpdateThread == null) {
                mMultiUpdateThread = new Thread(null, getMultipleResults,
                                                 "MultiResultsThread");
             }
             /* Launch dummy thread to simulate the transfer progress */
             Log.d(LOGTAG, "Thread State: " + mMultiUpdateThread.getState());
             if(mMultiUpdateThread.getState() == Thread.State.TERMINATED) {
                mMultiUpdateThread = new Thread(null, getMultipleResults,
                                                 "MultiResultsThread");
             }
             /* If the thread state is "new" then the thread has not yet started */
             if(mMultiUpdateThread.getState() == Thread.State.NEW) {
                mMultiUpdateThread.start();
             }
             // returns and UI in different thread.
             break;
        case SEARCH_TEST:
             try {
                 Log.d(LOGTAG, "start scanning\n");
                 if(isTransportLayerSMD()) {
                    Log.d(LOGTAG,"Scanning with 0 scan time");
                    if (mReceiver != null)
                        mIsSearching = mReceiver.searchStations(FmReceiver.FM_RX_SRCH_MODE_SCAN,
                                           SCAN_DWELL_PERIOD, FmReceiver.FM_RX_SEARCHDIR_UP);
                 }else {
                    mIsSearching = mService.scan(0);
                 }
             }catch (RemoteException e) {
                 e.printStackTrace();
             }

             if(mIsSearching) {
                 /*Set it to Ready to Stop*/
                 SetButtonState(false);
                 createResult(mColumnHeader);
                 Log.d(LOGTAG, "Created the results and cancel UI\n");
             }else {
                 mTestRunning = false;
             }
             break;
        case SWEEP_TEST:
             int Spacing = FmSharedPreferences.getChSpacing();
             int lowerFreq = FmSharedPreferences.getLowerLimit();
             int higherFreq = FmSharedPreferences.getUpperLimit();
             try {
                 Log.d(LOGTAG, "Going to set low side injection\n");
                 mService.setHiLoInj(Lo);
             }catch (RemoteException e) {
                 e.printStackTrace();
             }
             /* Set it to Ready to stop*/
             SetButtonState(false);
             createResult(mColumnHeader);
             getFMStatsInBand(lowerFreq, higherFreq, Spacing);
             break;
        }
    }

    /* Thread processing */
    private Runnable getMultipleResults = new Runnable() {
       public void run() {
          /*Collect the data for the current frequency
           20 times*/
          int freq = FmSharedPreferences.getTunedFrequency();

          for(int i = 0; i < 20 && !Thread.currentThread().isInterrupted(); i++) {
              try {
                   Thread.sleep(500);
                   Message updateUI = new Message();
                   updateUI.what = STATUS_UPDATE;
                   updateUI.obj = (Object)GetFMStatsForFreq(freq);
                   if(updateUI.obj == null)
                        break;
                   mUIUpdateHandlerHandler.sendMessage(updateUI);
              }catch (InterruptedException e) {
                   /*break the loop*/
                   break;
              }
            }
            mTestRunning = false;
            Message updateStop = new Message();
            updateStop.what = STATUS_DONE;
            mUIUpdateHandlerHandler.sendMessage(updateStop);
       }
    };

    private void getFMStatsInBand(int lFreq, int hFreq, int Spacing) {
       if(null == mBand) {
          mBand = new Band();
       }
       mBand.lFreq = lFreq;
       mBand.hFreq = hFreq;
       mBand.cur_freq = lFreq;
       if(Spacing == 0) {
          mBand.Spacing = 200; // 200KHz
       }else if(Spacing == 1) {
          mBand.Spacing = 100; // 100KHz
       }else {
          mBand.Spacing = 50;
       }

       setAlarm(prevDelayTime * 1000, BAND_SWEEP_START_DELAY_TIMEOUT);
    }

    /* Thread processing */
    private Runnable getManualSweepResults = new Runnable() {
       public void run() {
           try {
               if(mBand == null) {
                  return;
               }
               mWakeLock.acquire(10 * 1000);
               if(mBand.cur_freq <= mBand.hFreq) {
                  if(!tuneAndUpdateSweepResult(mBand.cur_freq)) {
                     sendStatusDoneMsg();
                     return;
                  }
                  mBand.cur_freq += mBand.Spacing;
                  if(mBand.cur_freq > mBand.hFreq) {
                     sendStatusDoneMsg();
                  }else {
                     setAlarm(prevDwellTime * 1000, BAND_SWEEP_DWELL_DELAY_TIMEOUT);
                  }
               }else {
                  sendStatusDoneMsg();
               }
           }catch(Exception e) {
               e.printStackTrace();
           }
       }
    };

    private void sendStatusDoneMsg() {
       mTestRunning = false;
       Message updateStop = new Message();
       updateStop.what = STATUS_DONE;
       try {
           Log.d(LOGTAG, "Going to set auto hi-lo injection\n");
           mService.setHiLoInj(Auto);
       } catch (RemoteException e) {
           e.printStackTrace();
       }
       if(mUIUpdateHandlerHandler != null) {
          Log.d(LOGTAG, "Sending message to stop test");
          mUIUpdateHandlerHandler.sendMessage(updateStop);
       }
    }

    private Runnable getFileSweepResults = new Runnable() {
       public void run() {
         boolean status = true;
         int freq;

         try {
              mWakeLock.acquire(10 * 1000);
              freq = mNextFreqInterface.getNextFreq();
              for(; (status = (!mNextFreqInterface.errorOccured()) &
                     (!Thread.currentThread().isInterrupted()));
                          freq = mNextFreqInterface.getNextFreq()) {
                  if(validFreq(freq)) {
                     if(!tuneAndUpdateSweepResult(freq)) {
                        status = false;
                        break;
                     }else {
                        setAlarm(prevDwellTime * 1000,
                                  BAND_SWEEP_DWELL_DELAY_TIMEOUT);
                        break;
                     }
                  }
              }
              if(!status) {
                 sendStatusDoneMsg();
                 mNextFreqInterface.Stop();
                 mNextFreqInterface = null;
              }
         }catch (Exception e) {
              e.printStackTrace();
         }
       }
    };

    private boolean validFreq(int freq) {
       if((freq >= mBand.lFreq) && (freq <= mBand.hFreq)
           &&
          (((freq - mBand.lFreq) / mBand.Spacing) >= 0)) {
           return true;
       }else {
           return false;
       }
    }

    private boolean tuneAndUpdateSweepResult(int freq) {
       try {
            if(!mService.tune(freq)) {
               Log.e(LOGTAG, "tune failed");
               return false;
            }
            mSync = new Band();
            synchronized(mSync) {
                mSync.wait(); //wait till notified
            }
            mSync = null;
            Message updateUI = new Message();
            updateUI.what = STATUS_UPDATE;
            updateUI.obj = (Object)GetFMStatsForFreq(freq);
            if(updateUI.obj == null) {
               return false;
            }else {
               mUIUpdateHandlerHandler.sendMessage(updateUI);
               Log.d(LOGTAG,"highFerq is " + mBand.hFreq);
            }
       }catch (RemoteException e) {
            Log.e(LOGTAG, "SweepResults:Tune failed\n");
            return false;
       }catch (InterruptedException e) {
            return false;
       }
       return true;
    }

    private Result GetFMStatsForFreq(int freq)
    {
        Result result = new Result();
        Log.d(LOGTAG,"freq is "+freq);
        result.setFreq(Integer.toString(freq));
        byte nRssi = 0;
        int nIoC = 0;
        int dummy = 0;
        int nIntDet = 0;
        int nMpxDcc = 0;
        byte nSINR = 0;
        if((null != mService)) {
           try {
               dummy = mService.getRssi();
               if (dummy != Integer.MAX_VALUE) {
                   nRssi = (byte)dummy;
                   result.setRSSI(Byte.toString(nRssi));
               } else {
                   return null;
               }
           } catch (RemoteException e) {
               e.printStackTrace();
           } catch(Exception e) {
               e.printStackTrace();
           }

           try {
               nIoC = mService.getIoC();
               if (nIoC != Integer.MAX_VALUE)
                   result.setIoC(Integer.toString(nIoC));
               else
                   return null;
           } catch (RemoteException e) {
               e.printStackTrace();
           } catch(Exception e) {
               e.printStackTrace();
           }

           if(isTransportLayerSMD()) {
              try {
                  dummy = mService.getSINR();
                  if (dummy != Integer.MAX_VALUE) {
                      nSINR = (byte)dummy;
                      result.setSINR(Integer.toString(nSINR));
                  } else {
                      return null;
                  }
              } catch (RemoteException e) {
                  e.printStackTrace();
              } catch(Exception e) {
                  e.printStackTrace();
              }
           } else {
              try {
                  nMpxDcc = mService.getMpxDcc();
                  if (nMpxDcc != Integer.MAX_VALUE)
                      result.setMpxDcc(Integer.toString(nMpxDcc));
                  else
                      return null;
              } catch (RemoteException e) {
                  e.printStackTrace();
              }catch(Exception e) {
                  e.printStackTrace();
              }
           }

           try {
               nIntDet = mService.getIntDet();
               if (nIntDet != Integer.MAX_VALUE)
                   result.setIntDet(Integer.toString(nIntDet));
               else
                   return null;
           } catch (RemoteException e) {
               e.printStackTrace();
           }catch (Exception e) {
               e.printStackTrace();
           }
        } else {
           return null;
        }
        return result;
   }


    private Handler mUIUpdateHandlerHandler = new Handler() {
            public void handleMessage(Message msg) {
               switch (msg.what)
               {
               case STATUS_UPDATE:
                    Result myRes = (Result) msg.obj;
                    Log.d(LOGTAG,"Status update is" +myRes.mFreq);
                    createResult(myRes);
                    break;
               case STATUS_DONE:
                    SetButtonState(true);
                    break;
               case RECORDTIMER_EXPIRED:
                    Log.d(LOGTAG, "mUIUpdateHandlerHandler - RECORDTIMER_EXPIRED");
                    if(!isRecording()) {
                       Log.d(LOGTAG, "Stop Recording");
                       stopRecording();
                    }
                   break;
               case RECORDTIMER_UPDATE:
                   Log.d(LOGTAG, "mUIUpdateHandlerHandler - RECORDTIMER_UPDATE");
                   updateExpiredRecordTime();
                   break;
               }
            }
    };

    public IFMRadioService sService = null;
    private HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<Context, ServiceBinder>();

    public boolean bindToService(Context context) {
       Log.e(LOGTAG, "bindToService: Context");
       return bindToService(context, null);
    }

    public boolean bindToService(Context context, ServiceConnection callback) {
       Log.e(LOGTAG, "bindToService: Context with serviceconnection callback");
       context.startService(new Intent(context, FMRadioService.class));
       ServiceBinder sb = new ServiceBinder(callback);
       sConnectionMap.put(context, sb);
       return context.bindService((new Intent()).setClass(context,
                                                          FMRadioService.class), sb, 0);
    }

    public void unbindFromService(Context context) {
       ServiceBinder sb = (ServiceBinder) sConnectionMap.remove(context);
       Log.e(LOGTAG, "unbindFromService: Context");
       if (sb == null)
       {
          Log.e(LOGTAG, "Trying to unbind for unknown Context");
          return;
       }
       context.unbindService(sb);
       if (sConnectionMap.isEmpty())
       {
          // presumably there is nobody interested in the service at this point,
          // so don't hang on to the ServiceConnection
          sService = null;
       }
    }

    private class ServiceBinder implements ServiceConnection
    {
       ServiceConnection mCallback;
       ServiceBinder(ServiceConnection callback) {
          mCallback = callback;
       }

       public void onServiceConnected(ComponentName className, android.os.IBinder service) {
          sService = IFMRadioService.Stub.asInterface(service);
          if (mCallback != null)
          {
             Log.e(LOGTAG, "onServiceConnected: mCallback");
             mCallback.onServiceConnected(className, service);
          }
       }

       public void onServiceDisconnected(ComponentName className) {
          if (mCallback != null)
          {
             mCallback.onServiceDisconnected(className);
          }
          sService = null;
       }
    }


    private ServiceConnection osc = new ServiceConnection() {
          public void onServiceConnected(ComponentName classname, IBinder obj) {
             mService = IFMRadioService.Stub.asInterface(obj);
             Log.e(LOGTAG, "ServiceConnection: onServiceConnected: ");
             if (mService != null)
             {
                try
                {
                   mService.registerCallbacks(mServiceCallbacks);

                } catch (RemoteException e)
                {
                   e.printStackTrace();
                }
                if(isRecording()) {
                   initiateRecordThread();
                }
                return;
             } else
             {
                Log.e(LOGTAG, "IFMRadioService onServiceConnected failed");
             }
             finish();
          }
          public void onServiceDisconnected(ComponentName classname) {
          }
       };


       private IFMRadioServiceCallbacks.Stub  mServiceCallbacks = new IFMRadioServiceCallbacks.Stub()
       {
          public void onEnabled()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onEnabled :");
             invalidateOptionsMenu();
          }

          public void onDisabled()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onDisabled :");
             stopAllOperations();
          }

          public void onRadioReset()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onRadioReset :");
             stopAllOperations();
          }

          public void onTuneStatusChanged()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onTuneStatusChanged :");
             if (mTestRunning)
                 mHandler.post(mTuneComplete);
          }

          public void onProgramServiceChanged()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onProgramServiceChanged :");
          }

          public void onRadioTextChanged()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onRadioTextChanged :");
          }
          public void onExtenRadioTextChanged()
          {
             Log.d(LOGTAG, "Extended Radio Text changed:");
          }
          public void onAlternateFrequencyChanged()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onAlternateFrequencyChanged :");
          }

          public void onSignalStrengthChanged()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onSignalStrengthChanged :");
          }

          public void onSearchComplete()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onSearchComplete :");
             /*Stop the update*/
             mTestRunning = false;
             Message updateStop = new Message();
             updateStop.what = STATUS_DONE;
             mUIUpdateHandlerHandler.sendMessage(updateStop);
          }
          public void onSearchListComplete()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onSearchListComplete :");

          }

          public void onMute(boolean bMuted)
          {
             Log.d(LOGTAG, "mServiceCallbacks.onMute :" + bMuted);
          }

          public void onAudioUpdate(boolean bStereo)
          {
             Log.d(LOGTAG, "mServiceCallbacks.onAudioUpdate :" + bStereo);
          }

          public void onStationRDSSupported(boolean bRDSSupported)
          {
             Log.d(LOGTAG, "mServiceCallbacks.onStationRDSSupported :" + bRDSSupported);
          }
          public void onRecordingStopped()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onDisabled :");
             stopRecording();
          }
          public void onFinishActivity()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onFinishActivity:");
          }
          public void onRecordingStarted()
          {
             Log.d(LOGTAG, "mServiceCallbacks.onRecordingStarted:");
             int durationInMins = FmSharedPreferences.getRecordDuration();
             Log.e(LOGTAG, " Fected duration: " + durationInMins);
             initiateRecordDurationTimer(durationInMins);
             invalidateOptionsMenu();
          }
      };
      /* Radio Vars */
     private Handler mHandler = new Handler();

     private Runnable mTuneComplete = new Runnable(){
         public void run(){
             if((null != mMultiUpdateThread) &&(null != mSync))
             {
                 synchronized(mSync){
                     mSync.notify();
                 }
             }
            if((mTestSelected == SEARCH_TEST) && (mService != null)) {
                /* On every Tune Complete generate the result for the current
                Frequency*/
                Message updateUI = new Message();
                updateUI.what = STATUS_UPDATE;
                int freq = FmSharedPreferences.getTunedFrequency();
                updateUI.obj = (Object)GetFMStatsForFreq(freq);
                if (updateUI.obj == null)
                    updateUI.what = STATUS_DONE;
                mUIUpdateHandlerHandler.sendMessage(updateUI);
            }
         }
     };

     private void stopCurTest() {
         if (mTestRunning) {
             switch(mTestSelected) {
             case CUR_FREQ_TEST:
                  break;
             case SWEEP_TEST:
                  stopBandSweep();
                  sendStatusDoneMsg();
                  break;
             case CUR_MULTI_TEST:
                  if (mMultiUpdateThread != null)
                      mMultiUpdateThread.interrupt();
                  break;
             case SEARCH_TEST:
                  mHandler.removeCallbacks(mTuneComplete);
                  if (mService != null) {
                      try {
                           Message updateStop = new Message();
                           updateStop.what = STATUS_DONE;
                           mUIUpdateHandlerHandler.sendMessage(updateStop);
                           mService.cancelSearch();
                      } catch (RemoteException e) {
                           e.printStackTrace();
                      }
                  }
                  break;
             }
             mTestRunning = false;
         }
     }

     @Override
     protected Dialog onCreateDialog(int id) {
         AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
         switch(id)
         {
         case DIALOG_BAND_SWEEP_SETTING:
              return createBandSweepDialog(id, dlgBuilder);
         }
         return null;
     }

     private Dialog createBandSweepDialog(int id, AlertDialog.Builder dlgBuilder) {
         LayoutInflater inflater = LayoutInflater.from(this);
         final View listview = inflater.inflate(R.layout.band_sweep_setting, null);
         spinOptionBandSweepMthds = (Spinner)listview.findViewById(R.id.band_sweep_spinner);
         final EditText delayBox = (EditText)listview.findViewById(R.id.txtboxDelayTime);
         final EditText dwellBox = (EditText)listview.findViewById(R.id.txtboxDwellTime);

         if(delayBox != null) {
            delayBox.setText("" + prevDelayTime);
         }
         if(dwellBox != null) {
            dwellBox.setText("" + prevDwellTime);
         }
         if(spinOptionBandSweepMthds != null) {
            spinOptionBandSweepMthds.setAdapter(bandSweepMthds);
            spinOptionBandSweepMthds.setOnItemSelectedListener(mSweepMthdsListener);
            spinOptionBandSweepMthds.setSelection(prevSweepMthd);
         }else {
            Log.e(LOGTAG, "could not find spinner for methods\n");
         }
         dlgBuilder.setView(listview)
                .setPositiveButton(R.string.band_sweep_setting_set, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        prevSweepMthd = curSweepMthd;
                        if(delayBox != null) {
                           String s = delayBox.getText().toString();
                           prevDelayTime = Integer.parseInt(s);
                        }
                        if(dwellBox != null) {
                           String s = dwellBox.getText().toString();
                           prevDwellTime = Integer.parseInt(s);
                        }
                        removeDialog(DIALOG_BAND_SWEEP_SETTING);
                    }
                })
                .setNegativeButton(R.string.band_sweep_setting_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        removeDialog(DIALOG_BAND_SWEEP_SETTING);
                    }
                });
         return dlgBuilder.create();
     }

    private View.OnClickListener mClicktBandSweepSettingListener = new View.OnClickListener() {
         public void onClick(View v) {
            showDialog(DIALOG_BAND_SWEEP_SETTING);
         }
    };

    private void enableBandSweepSetting() {
         if(bandSweepSettingButton != null) {
            bandSweepSettingButton.setEnabled(true);
            bandSweepSettingButton.setVisibility(View.VISIBLE);
         }
    }

    private void disableBandSweepSetting() {
         if(bandSweepSettingButton != null) {
            bandSweepSettingButton.setEnabled(false);
            bandSweepSettingButton.setVisibility(View.INVISIBLE);
         }
    }

    public class BandSweepMthdsSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            Log.d("Band Sweep Methods","onItemSelected is hit with " + pos);
            curSweepMthd = pos;
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do Nothing
        }
    }

    private void registerBandSweepDelayExprdListener() {
        if(mBandSweepDelayExprdListener == null) {
           mBandSweepDelayExprdListener = new BroadcastReceiver() {
              @Override
               public void onReceive(Context context, Intent intent) {
                   Log.d(LOGTAG, "Received Band sweep delay expired");
                   mWakeLock.acquire(5 * 1000);
                   StartBandSweep();
               }
           };
           IntentFilter intentFilter = new IntentFilter(BAND_SWEEP_START_DELAY_TIMEOUT);
           registerReceiver(mBandSweepDelayExprdListener, intentFilter);
        }
    }

    private void registerBandSweepDwellExprdListener() {
        if(mBandSweepDwellExprdListener == null) {
           mBandSweepDwellExprdListener = new BroadcastReceiver() {
              @Override
               public void onReceive(Context context, Intent intent) {
                   Log.d(LOGTAG, "received Band sweep Dwell expired");
                   if(mTestRunning) {
                      mWakeLock.acquire(5 * 1000);
                      ResumeBandSweep();
                   }
               }
           };
           IntentFilter intentFilter = new IntentFilter(BAND_SWEEP_DWELL_DELAY_TIMEOUT);
           registerReceiver(mBandSweepDwellExprdListener, intentFilter);
        }
    }

    private void unRegisterBroadcastReceiver(BroadcastReceiver receiver) {
        if(receiver != null) {
           unregisterReceiver(receiver);
           receiver = null;
        }
    }

    private void setAlarm(long duration, String action) {
        Intent i = new Intent(action);
        AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + duration, pi);
    }

    private void cancelAlarm(String action) {
        Intent i = new Intent(action);
        AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        am.cancel(pi);
    }

    private void StartBandSweep() {
       if(mMultiUpdateThread == null ||
            (mMultiUpdateThread.getState() == Thread.State.TERMINATED)) {
          if(prevSweepMthd == 0) {
             mMultiUpdateThread = new Thread(null, getManualSweepResults,
                                           "MultiResultsThread");
          }else {
             mNextFreqInterface = new CommaSeparatedFreqFileReader(getFilesDir() + FREQ_LIST_FILE_NAME);
             mMultiUpdateThread = new Thread(null, getFileSweepResults,
                                           "MultiResultsThread");
          }
       }

       /* If the thread state is "new" then the thread has not yet started */
       if(mMultiUpdateThread.getState() == Thread.State.NEW) {
          mMultiUpdateThread.start();
       }
    }

    private void ResumeBandSweep() {
       if(mMultiUpdateThread == null ||
            (mMultiUpdateThread.getState() == Thread.State.TERMINATED)) {
          if(prevSweepMthd == 0) {
             mMultiUpdateThread = new Thread(null, getManualSweepResults,
                                           "MultiResultsThread");
          }else {
             mMultiUpdateThread = new Thread(null, getFileSweepResults,
                                           "MultiResultsThread");
          }
       }

       /* If the thread state is "new" then the thread has not yet started */
       if(mMultiUpdateThread.getState() == Thread.State.NEW) {
          mMultiUpdateThread.start();
       }
    }

    private void stopBandSweep() {
       cancelAlarm(BAND_SWEEP_START_DELAY_TIMEOUT);
       cancelAlarm(BAND_SWEEP_DWELL_DELAY_TIMEOUT);
       if(mMultiUpdateThread != null) {
          mMultiUpdateThread.interrupt();
       }
       if(mNextFreqInterface != null) {
          mNextFreqInterface.Stop();
          mNextFreqInterface = null;
       }
    }

    public boolean isRecording() {
       if(mService == null)
          return false;
       try {
            return mService.isFmRecordingOn();
       }catch(RemoteException e) {
            return false;
       }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_rf_stats, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item;

        item = menu.findItem(R.id.menu_recording);
        if(item != null && !isRecording()) {
           item.setTitle(R.string.menu_record_start);
           item.setEnabled(isFmOn());
        }else if(item != null) {
           item.setTitle(R.string.menu_record_stop);
           setRecordDurationDisplay(menu, R.id.menu_record_duration);
        }
        return true;
    }

    private void setRecordDurationDisplay(Menu menu, int id) {
       MenuItem item;
       long timeNow;
       long seconds;

       if(menu == null)
          return;
       item = menu.findItem(id);
       if(item != null) {
          timeNow = SystemClock.elapsedRealtime();
          seconds = (timeNow - getRecordingStartTime()) / 1000;
          item.setTitle(makeTimeString(seconds));
       }
    }

    private void startRecording() {
      if(isFmOn()) {
         try {
              mService.startRecording();
         }catch(RemoteException e) {
              e.printStackTrace();
         }
      }
    }

    private void stopRecording() {
       if(null != mRecordUpdateHandlerThread) {
          mRecordUpdateHandlerThread.interrupt();
       }
       if(mService != null) {
          try {
               mService.stopRecording();
           }catch (RemoteException e) {
               e.printStackTrace();
           }
        }
        invalidateOptionsMenu();
    }

    private long getRecordingStartTime() {
       if(mService == null)
          return 0;
       try {
            return mService.getRecordingStartTime();
       }catch(RemoteException e) {
            return 0;
       }
    }

    private void initiateRecordDurationTimer(long mins ) {
       Log.d(LOGTAG, "Stop Recording in mins : " + mins);
       initiateRecordThread();
    }

    private void initiateRecordThread() {
      if(mRecordUpdateHandlerThread == null) {
         mRecordUpdateHandlerThread = new Thread(null, doRecordProcessing,
                                                "RecordUpdateThread");
      }
      /* Launch the dummy thread to simulate the transfer progress */
      Log.d(LOGTAG, "Thread State: " + mRecordUpdateHandlerThread.getState());
      if(mRecordUpdateHandlerThread.getState() == Thread.State.TERMINATED) {
         mRecordUpdateHandlerThread = new Thread(null, doRecordProcessing,
                                                "RecordUpdateThread");
      }
      /* If the thread state is "new" then the thread has not yet started */
      if(mRecordUpdateHandlerThread.getState() == Thread.State.NEW) {
         mRecordUpdateHandlerThread.start();
      }
   }

   /* Recorder Thread processing */
   private Runnable doRecordProcessing = new Runnable() {
      public void run() {
         while(isRecording() &&
                 (!Thread.currentThread().isInterrupted())) {
               try {
                    Thread.sleep(500);
                    Message statusUpdate = new Message();
                    statusUpdate.what = RECORDTIMER_UPDATE;
                    mUIUpdateHandlerHandler.sendMessage(statusUpdate);
               }catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
               }
               if(!isRecording()) {
                  Message finished = new Message();
                  finished.what = RECORDTIMER_EXPIRED;
                  mUIUpdateHandlerHandler.sendMessage(finished);
               }
         }
      }
   };

   private void updateExpiredRecordTime() {
      int vis = View.VISIBLE;
      if(isRecording()) {
         invalidateOptionsMenu();
      }
   }

   private String makeTimeString(long secs) {
      String durationformat = getString(R.string.durationformat);

      /** Provide multiple arguments so the format can be changed easily by
       *  modifying the xml.
       **/
      sFormatBuilder.setLength(0);

      final Object[] timeArgs = sTimeArgs;
      timeArgs[0] = secs / 3600;
      timeArgs[1] = secs / 60;
      timeArgs[2] = (secs / 60) % 60;
      timeArgs[3] = secs;
      timeArgs[4] = secs % 60;

      return sFormatter.format(durationformat, timeArgs).toString();
   }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
          case R.id.menu_recording:
               if(isRecording()) {
                  stopRecording();
               }else {
                  startRecording();
               }
               break;
        }
        return true;
    }

    private boolean isFmOn() {
       boolean status = false;

       if(mService != null) {
          try {
               status = mService.isFmOn();
          }catch(RemoteException e) {
          }
       }
       return status;
    }

    private void stopAllOperations() {
       stopCurTest();
       stopRecording();
    }
}
