package com.multiwii.multiwiiremote;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.WindowManager;



public abstract class BaseActivity extends Activity {
	
	protected App app;
	protected Menu menu;
	protected UpdateThread mUpdateThread;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		app = (App) getApplication();
	}
	protected void StartUpdateThread() {
		StopUpdateThread();
		mUpdateThread = new UpdateThread(this);
		mUpdateThread.start();
	}
	protected void StopUpdateThread() {
		if(mUpdateThread != null) mUpdateThread.cancel(); mUpdateThread = null;
	}
	public void KeepScreenOn(boolean keepOn) {
		if(keepOn)
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	@Override
	protected void onResume() {
		super.onResume();
		app.onResume();
		StartUpdateThread();
	}
	@Override
	protected void onPause() {
		super.onPause();
		StopUpdateThread();
		app.onPause();
	}
	@Override
	protected void onStop() {
		super.onStop();
	}
	//UI Update
	public abstract void UpdateUI();
	
	private synchronized void callFrequentTasks() {
		FrequentTasks();
	}
	public abstract void FrequentTasks();
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	private class UpdateThread extends Thread {
		private final BaseActivity mActivity;
		private boolean cancel = false;
		
		public UpdateThread(BaseActivity mActivity) {
			//Log.d(TAG, "create ConnectedThread");
			this.mActivity = mActivity;
		}

		public void run() {
			//Log.i(TAG, "BEGIN mConnectedWriteThread");

			while (!cancel) {
				
				mActivity.callFrequentTasks();
				
				if(cancel) break;
				
				try {
					Thread.sleep(app.RefreshRate);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}

		public void cancel() {
			cancel = true;
		}
	}
}
