package com.multiwii.multiwiiremote;

import java.lang.ref.WeakReference;

import com.multiwii.Utilities.Utilities;
import com.multiwii.communication.Communication;
import com.multiwii.communication.CommunicationMode;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;

public class MainActivityCommunicationHandler extends Handler {
	    private final WeakReference<MainActivity> mActivity;

	    public MainActivityCommunicationHandler(MainActivity activity) {
	      mActivity = new WeakReference<MainActivity>(activity);
	    }
		@Override
		public void handleMessage(Message msg) {
			final MainActivity myNewActivity = mActivity.get();
			if(myNewActivity != null)
			switch (msg.what) {
			case Communication.MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
					case Communication.STATE_CONNECTED:
						myNewActivity.setStatus("Connected");
						if(myNewActivity.app.comMode == CommunicationMode.WIFI)
							 new CountDownTimer(5000, 10) {
							     public void onTick(long millisUntilFinished ) {
							    	 myNewActivity.setStatus(millisUntilFinished + "");
							         }
							     public void onFinish() {
							    	 /*if(myNewActivity.isCamera)
							    	 myNewActivity.startWebCam();*/
							    	 myNewActivity.setStatus("Connected");
							     }
							  }.start();
					 break;
				}
				break;
			case App.SENSORSCHANGED:
				myNewActivity.onSensorsStateChangeMagAcc();
				break;
			case Communication.MESSAGE_TOAST:
				Utilities.showToast(msg.getData().getString(Communication.TOAST), myNewActivity);
				break;
			case 7: //Update UI
				myNewActivity.UpdateUI();
				break;
			}
		}
		
	};
