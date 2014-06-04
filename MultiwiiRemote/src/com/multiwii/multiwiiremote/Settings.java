package com.multiwii.multiwiiremote;

import java.util.regex.Pattern;

import com.multiwii.Utilities.Utilities;
import com.multiwii.multiwiiremote.App.SettingsConstants;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;


public class Settings extends PreferenceActivity {

		private Preference[] mPreferenceEntries;
		private Activity currentActivity;
		 private static final String IPADDRESS_PATTERN = 
		"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
		
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		currentActivity = this;
		SettingsConstants[] myConst = SettingsConstants.values();
		mPreferenceEntries = new Preference[myConst.length];
		
		for (int i = 0; i < myConst.length; i++) {
			mPreferenceEntries[i] = this.getPreferenceScreen().findPreference(myConst[i].toString().trim());
			mPreferenceEntries[i].setOnPreferenceChangeListener(changeListener);
		}
	}
	protected void onPause() {
        super.onPause();
        ((App) getApplication()).ReadSettings();
    }
	
	Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			boolean result = validate(preference, newValue);
			
			if(!result) Utilities.showToast("Invalid value", currentActivity);
			
			return result;
		}
	};
	
	private boolean validate(Preference preference, Object newValue) {
		SettingsConstants setting = SettingsConstants.valueOf(preference.getKey());
		
		switch(setting) {
			case IPADDRESS: return isValidIpAddress(newValue);
			case BAUDRATE: return isValidBaudRate(newValue);
			case TRIMROLL: return isTrimInRange(newValue);
			case TRIMPITCH: return isTrimInRange(newValue);
			default: return setting.validate(newValue);
		}
	}
	private boolean isValidIpAddress(Object newValue) {
		return Pattern.compile(IPADDRESS_PATTERN).matcher(newValue.toString()).matches();
	}
	private boolean isValidBaudRate(Object newValue) {
		return Utilities.IsInteger(newValue); //TODO For now
	}
	private boolean isTrimInRange(Object newValue) {
		if(!Utilities.IsInteger(newValue)) return false;
		
		int x = Integer.parseInt(newValue.toString());
		
		return Math.abs(x) < 500; //TODO associate rc_value 
	}
}