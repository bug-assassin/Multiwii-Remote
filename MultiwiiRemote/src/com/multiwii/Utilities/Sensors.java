/*  MultiWii EZ-GUI
    Copyright (C) <2012>  Bartosz Szczygiel (eziosoft)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.multiwii.Utilities;

import java.lang.reflect.Method;
import java.util.Iterator;

//import com.google.android.gms.maps.model.LatLng;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.multiwii.multiwiiremote.App;

public class Sensors implements LocationListener {

    private final App app;
    private MagAccListener mMagAccListener = null;
	private GPSListener mGPSListener = null;

	Location location, oldLocation;

	boolean useFilter = true;
	LowPassFilter filterYaw = new LowPassFilter(0.03f);
	LowPassFilter filterPitch = new LowPassFilter(0.03f);
	LowPassFilter filterRoll = new LowPassFilter(0.03f);

	private LocationManager locationManager;
	private String provider;
	GeomagneticField geoField;

	public int PhoneNumSat = 0;
	public double PhoneLatitude = 0;
	public double PhoneLongitude = 0;
	public double PhoneAltitude = 0;
	public double PhoneSpeed = 0;
	public int PhoneFix = 0;
	public float PhoneAccuracy = 0;
	public float Declination = 0;

	//public LatLng MapCurrentPosition = new LatLng(0, 0);

	SensorManager m_sensorManager;
	Sensor rotateSensor;
	private float[] m_rotationMatrix = new float[9];
	private float[] m_orientation = new float[3];

	public float pitch = 0.f;
	public int heading = 0;
	public float roll = 0.f;

	private Context context;

	String mocLocationProvider;
	public boolean MockLocationWorking = false;

    public float getMaxValue() {
        return maxValue;
    }

    public float getMinValue() {
        return minValue;
    }

    private float minValue = 0;
	private float maxValue = 0;
	private Handler mHandler;
    private SensorEventListener rotateListener;

    public boolean isRotateSensorSupported() {
        return m_sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null;
    }

    public interface MagAccListener {
		
		public void onSensorsStateChangeMagAcc();
		
	}
	
	public interface GPSListener {
		
		public void onSensorsStateGPSLocationChange();

		public void onSensorsStateGPSStatusChange();
	}

	public void registerListener(MagAccListener listener) {
		this.mMagAccListener = listener;
	}
	
	public void registerListener(GPSListener listener) {
		this.mGPSListener = listener;
	}

	public void initMOCKLocation() {
		mocLocationProvider = LocationManager.GPS_PROVIDER;
		locationManager.addTestProvider(mocLocationProvider, false, false, false, false, true, true, true, 0, 5);
		locationManager.setTestProviderEnabled(mocLocationProvider, true);
		MockLocationWorking = true;
	}

	public boolean isMockEnabled() {
		try {
			int mock_location = Settings.Secure.getInt(context.getContentResolver(), "mock_location");
			if (mock_location == 0) {
				try {
					Settings.Secure.putInt(context.getContentResolver(), "mock_location", 1);
				} catch (Exception ex) {
				}
				mock_location = Settings.Secure.getInt(context.getContentResolver(), "mock_location");
			}

			if (mock_location == 0) {
				Toast.makeText(context, "Turn on the mock locations in your Android settings", Toast.LENGTH_LONG).show();
				return false;
			} else {
				return true;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return false;
	}

	public void setMOCKLocation(double Latitude, double Longitude, float Altitude, float Heading, float speed) {

		Location mockLocation = new Location(mocLocationProvider); // a string
		mockLocation.setLatitude(Latitude); // double
		mockLocation.setLongitude(Longitude);
		mockLocation.setAltitude(Altitude);
		mockLocation.setTime(System.currentTimeMillis());
		mockLocation.setAccuracy(1);
		mockLocation.setBearing(Heading);
		mockLocation.setSpeed(speed * 0.01f);

		try {
			Method locationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
			if (locationJellyBeanFixMethod != null) {
				locationJellyBeanFixMethod.invoke(mockLocation);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		locationManager.setTestProviderLocation(mocLocationProvider, mockLocation);

	}

	public void ClearMOCKLocation() {
		if (mocLocationProvider != null) {
			Log.d("aaa", "ClearMOCKLocation");

			locationManager.clearTestProviderEnabled(mocLocationProvider);
			locationManager.clearTestProviderLocation(mocLocationProvider);
			locationManager.clearTestProviderStatus(mocLocationProvider);
			locationManager.removeTestProvider(mocLocationProvider);
			start();
			MockLocationWorking = false;

		}
	}
	public Sensors(Context applicationContext, final App app) {
		this.context = applicationContext;
        this.app = app;
		m_sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        rotateSensor =  m_sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		//locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		maxValue = (float) rotateSensor.getMaximumRange();
		minValue = -maxValue;

        //sensor listener
        rotateListener = new SensorEventListener() {
            private float []rotationMatrix = new float[9];
            private float []orientation = new float[3];
            @Override
            public void onSensorChanged(SensorEvent event) {

                m_sensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                m_sensorManager.getOrientation(rotationMatrix, orientation);
                heading = (int)Math.toDegrees(orientation[0]);
                //calibrate mag with multiwii
                heading += 90;
                if(heading > 180){
                    heading -= 360;
                }

                pitch =  orientation[1]; //Pitch, rad
                roll =  orientation[2]; //Roll, rad
                if(useFilter) {
                    heading = filterYaw.lowPass(heading);
                    pitch = filterPitch.lowPass(pitch);
                    roll = filterRoll.lowPass(roll);
                }
                app.getMainActivity().onSensorsStateChangeRotate();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };


		//Criteria criteria = new Criteria();
		// if (!app.D)
		//criteria.setAccuracy(Criteria.ACCURACY_FINE);
		//provider = locationManager.getBestProvider(criteria, false);
		//if(provider != null)
		//location = locationManager.getLastKnownLocation(provider);
		/*if (location != null) {
			geoField = new GeomagneticField(Double.valueOf(location.getLatitude()).floatValue(), Double.valueOf(location.getLongitude()).floatValue(), Double.valueOf(location.getAltitude()).floatValue(), System.currentTimeMillis());
			Declination = geoField.getDeclination();

			//MapCurrentPosition = new LatLng(location.getLatitude(), location.getLongitude());

			oldLocation = location;
			
		}

		locationManager.addGpsStatusListener(new GpsStatus.Listener() {

			@Override
			public void onGpsStatusChanged(int event) {
				if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
					GpsStatus status = locationManager.getGpsStatus(null);
					Iterable<GpsSatellite> sats = status.getSatellites();
					Iterator<GpsSatellite> it = sats.iterator();

					PhoneNumSat = 0;
					while (it.hasNext()) {

						GpsSatellite oSat = (GpsSatellite) it.next();
						if (oSat.usedInFix())
							PhoneNumSat++;
					}

				}
				if (event == GpsStatus.GPS_EVENT_FIRST_FIX)
					PhoneFix = 1;

				if (mGPSListener != null)
					mGPSListener.onSensorsStateGPSStatusChange();
			}
		});*/
	}


	public void start() {
        //register rotate sensor listener
        if(this.app.getMainActivity().getChkUsePhoneHeading().isChecked()) {
            m_sensorManager.registerListener(rotateListener, rotateSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
	}

//	public void startMagACC() {
//		m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);
//	}
//
//	public void stopMagACC() {
//		m_sensorManager.unregisterListener(this);
//	}

	private void unregisterListeners() {
		//stopMagACC();
		mMagAccListener = null;
		mGPSListener = null;
		locationManager.removeUpdates(this);
	}

	public void stop() {
		m_sensorManager.unregisterListener(rotateListener);
	}


	private void computeOrientation() {
			if (mMagAccListener != null)
				mMagAccListener.onSensorsStateChangeMagAcc();
	}

	public void setFilter(float ALPHA) {
		if(ALPHA <= 0) {
		useFilter = ALPHA > 0;
		return;
		}
		if(ALPHA > 1)
			throw new ArithmeticException("Alpha must be between 0 and 1");
		filterYaw.setFilter(ALPHA);
		filterPitch.setFilter(ALPHA);
		filterRoll.setFilter(ALPHA);
	}
	@Override
	public void onLocationChanged(Location location) {

		oldLocation = this.location;
		this.location = location;

		PhoneLatitude = location.getLatitude();
		PhoneLongitude = location.getLongitude();
		PhoneAltitude = location.getAltitude();
		PhoneSpeed = location.getSpeed() * 100f;
		PhoneAccuracy = location.getAccuracy() * 100f;

		//MapCurrentPosition = new LatLng(location.getLatitude(), location.getLongitude());

		geoField = new GeomagneticField(Double.valueOf(location.getLatitude()).floatValue(), Double.valueOf(location.getLongitude()).floatValue(), Double.valueOf(location.getAltitude()).floatValue(), System.currentTimeMillis());
		Declination = geoField.getDeclination();

		if (mGPSListener != null)
			mGPSListener.onSensorsStateGPSLocationChange();
	}

	@Override
	public void onProviderDisabled(String provider) {

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	/*public LatLng getNextPredictedLocationOnlineMap() {
		if (location != null && oldLocation != null) {
			int lat = (int) ((location.getLatitude() + (location.getLatitude() - oldLocation.getLatitude())));
			int lon = (int) ((location.getLongitude() + (location.getLongitude() - oldLocation.getLongitude())));
			return new LatLng(lat, lon);
		} else
			return new LatLng(0, 0);

	}*/

}
