package com.multiwii.multiwiiremote;



import java.util.Locale;

import com.multiwii.communication.Bluetooth;
import com.multiwii.communication.Communication;
import com.multiwii.communication.CommunicationMode;
import com.multiwii.communication.Wifi;
import com.multiwii.protocol.MultiWii230;
import com.multiwii.protocol.MultirotorData;
import com.multiwii.Utilities.Sensors;
import com.multiwii.Utilities.SoundManager;
import com.multiwii.Utilities.TTS;
import com.multiwii.Utilities.Utilities;
import com.multiwii.Utilities.VarioSoundClass;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import static com.multiwii.multiwiiremote.App.SettingsConstants.*;

public class App extends Application implements Sensors.MagAccListener {

	/////Settings variables/////////
	public int LowSignalThreshold;
	public int RollPitchLimit;
	public boolean UIDebug;
	public int RefreshRate = 50; //TODO ADD TO XML
	public int TrimRoll;
	public int TrimPitch;
	public int ThrottleResolution;
	public int BaudRate;
	public String IpAddress;
	public int IpPort;
	public boolean UseCamera;
	public float SensorFilterAlpha;
	public boolean PreventExitWhenFlying;
	public boolean TextToSpeach;
	public CommunicationMode comMode;
	
	public String Aux1Txt;
	public String Aux2Txt;
	public String Aux3Txt;
	public String Aux4Txt;
	
	public enum SettingsConstants {
		LOWSIGNALTHRESHOLD(30),
		TEXTTOSPEACH(true),
		UIDEBUG(false), 
		//REFRESHRATE(40), //TODO add xml
		TRIMROLL(0), 
		TRIMPITCH(0),
		THROTTLERESOLUTION(10),
		BAUDRATE("115200"),
		IPADDRESS("192.168.1.1"),
		IPPORT(8082),
		AUX1TXT("AUX 1"),
		AUX2TXT("AUX 2"),
		AUX3TXT("AUX 3"),
		AUX4TXT("AUX 4"),
		USECAMERA(false),
		SENSORFILTERALPHA(0.03f),
		//KEEPSCREENON(true),
		PREVENTEXITWHENFLYING(true),
		ROLLPITCHLIMIT(250);
	
		private String value;
		private Boolean bValue;
		private Float fValue;
		private Integer iValue;
		
		SettingsConstants(String value) {
			this.value = value;
		}
		SettingsConstants(boolean value) {
			this(value + "");
			this.bValue = value;
		}
		SettingsConstants(float value) {
			this(value + "");
			this.fValue = value;
		}
		SettingsConstants(int value) {
			this(value + "");
			this.iValue = value;
		}
		
		public String DefaultS() {
			return value;
		}
		public boolean DefaultB() {
			return bValue;
		}
		public float DefaultF() {
			return fValue;
		}
		public int DefaultI() {
			return iValue;
		}
		
		public boolean validate(Object newValue) {
			if(bValue != null) { //Object should be a boolean
				return Utilities.IsBoolean(newValue);
			}
			else if(fValue != null) {
				return Utilities.IsFloat(newValue);
			} 
			else if(iValue != null) {
				return Utilities.IsInteger(newValue);
			}
			return true;
		}
	}
	
	//////////Constants///////////////////
	private static final String TAG = "App";
	public final static int SENSORSCHANGED = 9;
	public final int WifiConnectionStartDelay = 5000;
	public final int BluetoothConnectionStartDelay = 0;
	//Sounds
	public final int LOW_SIGNAL = 0;
	public final int LOW_PHONE_BATTERY = 0;
	public final int BLIP = 0;
	///////End Constants//////////////////
	
	public boolean D = true;
	private boolean ManualMode = true;
	public boolean UsePhoneHeading = false;
	public int WriteRepeatDelayMillis = 40;
	public boolean AuxTextChanged = false;
	public String Status = "";
	
	/////////////Objects///////////
	public Sensors sensors;
	private Handler mHandler;
	
	public TTS tts;
	public SoundManager soundManager;
	public VarioSoundClass varioSoundClass;
	
	public Communication commMW;
	public MultirotorData protocol;
	private SharedPreferences prefs;
	private Editor prefsEditor;
	private RepeatTimer signalStrengthTimer = new RepeatTimer(5000);
	/////////End Objects/////////////
	
	public void SetHandler(Handler mHandler) {
		this.mHandler = mHandler;
	}

	@Override
	public void onCreate() {
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefsEditor = prefs.edit();
		
		sensors = new Sensors(getApplicationContext());
		sensors.registerListener(this);
		
		Init();
		
		tts = new TTS(getApplicationContext());
		prepareSounds();
		
		varioSoundClass = new VarioSoundClass();
	}

	public void Init() {
		ReadSettings();
	}

	public void ReadSettings() {
		LowSignalThreshold = Integer.parseInt(prefs.getString(LOWSIGNALTHRESHOLD.toString(), LOWSIGNALTHRESHOLD.DefaultS()));
		TextToSpeach = prefs.getBoolean(TEXTTOSPEACH.toString(), TEXTTOSPEACH.DefaultB());
		UIDebug = prefs.getBoolean(UIDEBUG.toString(), UIDEBUG.DefaultB());
		TrimRoll = Integer.parseInt(prefs.getString(TRIMROLL.toString(), TRIMROLL.DefaultS()));
		TrimPitch = Integer.parseInt(prefs.getString(TRIMPITCH.toString(), TRIMPITCH.DefaultS()));
		ThrottleResolution = Integer.parseInt(prefs.getString(THROTTLERESOLUTION.toString(), THROTTLERESOLUTION.DefaultS()));
		BaudRate = Integer.parseInt(prefs.getString(BAUDRATE.toString(), BAUDRATE.DefaultS()));
		IpAddress = prefs.getString(IPADDRESS.toString(), IPADDRESS.DefaultS());
		IpPort = Integer.parseInt(prefs.getString(IPPORT.toString(), IPPORT.DefaultS()));
		
		Aux1Txt = prefs.getString(AUX1TXT.toString(), AUX1TXT.DefaultS());
		Aux2Txt = prefs.getString(AUX2TXT.toString(), AUX2TXT.DefaultS());
		Aux3Txt = prefs.getString(AUX3TXT.toString(), AUX3TXT.DefaultS());
		Aux4Txt = prefs.getString(AUX4TXT.toString(), AUX4TXT.DefaultS());
		AuxTextChanged = true;
		
		UseCamera = prefs.getBoolean(USECAMERA.toString(), USECAMERA.DefaultB());
		PreventExitWhenFlying = prefs.getBoolean(PREVENTEXITWHENFLYING.toString(), PREVENTEXITWHENFLYING.DefaultB());
		RollPitchLimit = Integer.parseInt(prefs.getString(ROLLPITCHLIMIT.toString(), ROLLPITCHLIMIT.DefaultS()));
		
		sensors.setFilter(Float.parseFloat(prefs.getString(SENSORFILTERALPHA.toString(), SENSORFILTERALPHA.DefaultS())));
		comMode = CommunicationMode.valueOf(prefs.getString("comMode", "Bluetooth").toUpperCase(Locale.US));
		updateComMode();
	}

	@SuppressLint("NewApi")
	public void SaveSettings() {
		prefsEditor.putString(LOWSIGNALTHRESHOLD.toString(), LowSignalThreshold + "");
		prefsEditor.putBoolean(TEXTTOSPEACH.toString(), TextToSpeach);
		prefsEditor.putBoolean(UIDEBUG.toString(), UIDebug);
		prefsEditor.putString(TRIMROLL.toString(), TrimRoll + "");
		prefsEditor.putString(TRIMPITCH.toString(), TrimPitch + "");
		prefsEditor.putString(THROTTLERESOLUTION.toString(), ThrottleResolution + "");
		prefsEditor.putString(BAUDRATE.toString(), BaudRate + "");
		prefsEditor.putString(IPADDRESS.toString(), IpAddress);
		prefsEditor.putString(IPPORT.toString(), IpPort + "");
		
		prefsEditor.putString(AUX1TXT.toString(), Aux1Txt);
		prefsEditor.putString(AUX2TXT.toString(), Aux2Txt);
		prefsEditor.putString(AUX3TXT.toString(), Aux3Txt);
		prefsEditor.putString(AUX4TXT.toString(), Aux4Txt);
		
		prefsEditor.putBoolean(USECAMERA.toString(), UseCamera);
		prefsEditor.putBoolean(PREVENTEXITWHENFLYING.toString(), PreventExitWhenFlying);
		prefsEditor.putString(ROLLPITCHLIMIT.toString(), RollPitchLimit + "");
		
		prefsEditor.putString(SENSORFILTERALPHA.toString(), SensorFilterAlpha + "");
		//comMode = CommunicationMode.valueOf(prefs.getString("comMode", "Bluetooth").toUpperCase(Locale.US));
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD)
			prefsEditor.apply();
		else
			prefsEditor.commit();
	}
	public void FrequentTasks() {
		if(commMW.Connected) {
		
			if(signalStrengthTimer.isTime()) {
				signalStrengthTimer.reset();
				int signalStrength = commMW.getStrength();
				if(signalStrength != 0 && signalStrength < LowSignalThreshold) {
					Say("Signal Low " + signalStrength);
				}
			}
			
		}
		//TODO check low phone battery
	}
	protected void updateComMode() {
	if(commMW != null && commMW.getMode() == comMode) return;
	
		if (commMW != null) commMW.Close();
		
		switch (comMode) {
		case BLUETOOTH:
			commMW = new Bluetooth(getApplicationContext());
			WriteRepeatDelayMillis = 40;
			break;
		case WIFI:
			commMW = new Wifi(getApplicationContext());
			WriteRepeatDelayMillis = 20;
			break;
		}
		if (mHandler != null) commMW.SetHandler(mHandler);
		SelectProtocol();
	}
	
	public void SelectProtocol() {
		if(protocol != null) protocol.stop();
		
		protocol = new MultiWii230(commMW);
	}

	public boolean getManualMode() {
		return ManualMode;
	}

	public void setManualModeOn(boolean ManualMode) {
		this.ManualMode = ManualMode;
		Say(ManualMode ? "Manual" : "Automatic");
	}
	
	public void Say(String text) {
		if (TextToSpeach) tts.Speak(text);
	}
	private void prepareSounds() {
		soundManager = new SoundManager(getApplicationContext());
		/*soundManager.addSound(LOW_SIGNAL, R.raw.lowSignal);
		soundManager.addSound(LOW_PHONE_BATTERY, R.raw.lowPhoneBattery);
		soundManager.addSound(BLIP, R.raw.blip);*///TODO
	}
	
	public void stop() {
			sensors.stop();
			if(protocol != null) protocol.stop();
			if (commMW != null) commMW.Close();
	}
	
	@Override
	public void onTerminate() {
		stop();
	}

	@Override
	public void onSensorsStateChangeMagAcc() {
		mHandler.sendEmptyMessage(SENSORSCHANGED);
	}
}
