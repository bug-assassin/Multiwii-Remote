package com.multiwii.multiwiiremote;


import view.joystick.JoystickView;
import com.multiwii.Utilities.Utilities;
import com.multiwii.communication.DeviceListActivity;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ToggleButton;
import static com.multiwii.multiwiiremote.RCSignals.*;

public class MainActivity extends BaseActivity {
	private enum InputMode {
		ACCELEROMETER, TOUCH
	}

	private InputMode inputMode = InputMode.TOUCH;
	private JoystickView joystick;

	private ToggleButton auxBtn[] = new ToggleButton[4];
	private TextView txtHeader;
	private TextView txtStatus;
	private TextView txtUIDebug;
	private CheckBox chkUsePhoneHeading;

	public RCSignals rc;
	// private byte timer = 0;
	// private Camera mCam;
	MainActivityEvents mEvents;
	private MainActivityCommunicationHandler mHandler;

	private final int BLUETOOTH_SEARCH_RETURN = 1;
	private final int SETTINGS_MODIFY = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//KeepScreenOn(app.KeepScreenOn);
		KeepScreenOn(true);
		
		rc = new RCSignals();
		// mCam = new Camera((MjpegView) this.findViewById(R.id.webcamView));
		mEvents = new MainActivityEvents(this);
		mHandler = new MainActivityCommunicationHandler(this);
		
		txtStatus = (TextView) findViewById(R.id.status);
		txtHeader = (TextView) findViewById(R.id.throttleView);
		txtUIDebug = (TextView) findViewById(R.id.debugTxt);
		for (int x = 0; x < auxBtn.length; x++)
			auxBtn[x] = (ToggleButton) findViewById(getResources().getIdentifier("aux" + (x + 1) + "Btn", "id",	getPackageName()));
		joystick = (JoystickView) findViewById(R.id.joystickView);
		chkUsePhoneHeading = (CheckBox) findViewById(R.id.chkUsePhoneHeading);
		
		Init();
	}

	private void Init() {
		app.SetHandler(mHandler); //App class will automatically bind to commMW

		chkUsePhoneHeading.setOnCheckedChangeListener(mEvents.mCheckChangeListener);
		((Button) findViewById(R.id.switchModes)).setOnClickListener(mEvents.mClickListener);
		joystick.setOnJostickMovedListener(mEvents._listener);
		for (int x = 0; x < auxBtn.length; x++)
			auxBtn[x].setOnClickListener(mEvents.mClickListener);
		auxBtn[0].setEnabled(false);
		
		settingsModified();
	}
	
	public void FrequentTasks() {
		mHandler.sendEmptyMessage(7);
		if(app.commMW.Connected) {
		//Create payload TODO
			app.protocol.SendRequestMSP_SET_RAW_RC(rc.get()); //TODO Check that delay isnt too big from other tasks
		}
		app.FrequentTasks();
	}
	
	public void setStatus(String status) {
		this.txtStatus.setText(status);
		app.Status = status;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case BLUETOOTH_SEARCH_RETURN:
				if (resultCode == Activity.RESULT_OK) {
					String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					app.protocol.Connect(address, app.BaudRate, app.BluetoothConnectionStartDelay);
				}
			break;
			case SETTINGS_MODIFY:
				settingsModified();
			break;
		}
	}
	private void settingsModified() {
		rc.ThrottleResolution = app.ThrottleResolution;
		rc.TrimRoll = app.TrimRoll;
		rc.TrimPitch = app.TrimPitch;
		rc.RollPitchLimit = app.RollPitchLimit;
		
		if (app.AuxTextChanged) {
			setAuxbtnTxt(auxBtn[0], app.Aux1Txt);
			setAuxbtnTxt(auxBtn[1], app.Aux2Txt);
			setAuxbtnTxt(auxBtn[2], app.Aux3Txt);
			setAuxbtnTxt(auxBtn[3], app.Aux4Txt);
			app.AuxTextChanged = false;
		}
		setStatus("Ready " + app.comMode.toString());
	}
	private void setAuxbtnTxt(ToggleButton mButton, String text) {
		mButton.setText(text);
		mButton.setTextOn(text);
		mButton.setTextOff(text);
	}
	public void Connect() {
		switch (app.comMode) {
		case BLUETOOTH:
			Intent searchBtDevices = new Intent(this, DeviceListActivity.class);
			startActivityForResult(searchBtDevices, BLUETOOTH_SEARCH_RETURN);
			break;
		case WIFI:
			app.protocol.Connect(app.IpAddress, app.IpPort, app.WifiConnectionStartDelay);
			break;
		}
	}
	public void UpdateUI() {
		txtHeader.setText(rc.adjustMode.getValue() + rc.get(rc.adjustMode.getId()));
		txtUIDebug.setText(app.UIDebug ? rc.toStringNoThrottle() + "Heading: " + app.sensors.Heading : "");
		txtStatus.setText(app.Status);
	}
	
	// int requests[] = new int[] { MultirotorData.MSP_DEBUG,
	// MultirotorData.MSP_ALTITUDE, MultirotorData.MSP_RC };
	public void onSensorsStateChangeMagAcc() {
		if (inputMode == InputMode.ACCELEROMETER) {
			float xCoordinate = (float) Utilities.mapCons(-app.sensors.Pitch,
					app.sensors.getAccelMinValue(),
					app.sensors.getAccelMaxValue(),
					-joystick.getMovementRange(), joystick.getMovementRange());
			float yCoordinate = (float) Utilities.mapCons(-app.sensors.Roll,
					app.sensors.getAccelMinValue(),
					app.sensors.getAccelMaxValue(),
					-joystick.getMovementRange(), joystick.getMovementRange());
			joystick.setCoordinates(xCoordinate, yCoordinate);
		}
	}

	public void aux1_Click(View v) {
		// Replaced with When Throttle >= 1030
	}

	public void aux2_Click(View v) {
		rc.set(AUX2, ((ToggleButton) v).isChecked());
	}

	public void aux3_Click(View v) {
		rc.set(AUX3, ((ToggleButton) v).isChecked());
	}

	public void aux4_Click(View v) {
		rc.set(AUX4, ((ToggleButton) v).isChecked());
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		app.stop();
	}
	// ///////////////////Menu///////////////////
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.connect:
			Connect();
			break;
		case R.id.accelOn:
			if (!app.sensors.isAccelSupported())
				Utilities.showToast("Sorry, your device does not have an accelerometer", this);
			else {
				switch (inputMode) {
					case TOUCH:
						inputMode = InputMode.ACCELEROMETER;
					break;
					case ACCELEROMETER:
						inputMode = InputMode.TOUCH;
						joystick.returnHandleToCenter();
					break;
				}
			}
			break;
		case R.id.menu_settings:
			if (rc.isFlying()) {
				Utilities.showToast(
						"Set throttle under 1100 to access settings", this);
			} else {
				this.startActivityForResult(new Intent(this, Settings.class), SETTINGS_MODIFY);
			}
			break;
		}
		return super.onContextItemSelected(item);
	}

	// /////////////////////////////End Menu/////////////////////////////
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
	if(app.PreventExitWhenFlying && rc.isFlying() && (event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_APP_SWITCH || event.getKeyCode() == KeyEvent.KEYCODE_HOME))
	{
		Utilities.showToast("Detected flying. Please put throttle under 1100.", this);
		return true;
	}
		if (event.getAction() == KeyEvent.ACTION_DOWN)
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				rc.adjustRcValue(-1);
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				rc.adjustRcValue(1);
				return true;
			case KeyEvent.KEYCODE_SEARCH:
				app.setManualModeOn(!app.getManualMode());
				return true;
			}
		return super.dispatchKeyEvent(event);
	}
}
