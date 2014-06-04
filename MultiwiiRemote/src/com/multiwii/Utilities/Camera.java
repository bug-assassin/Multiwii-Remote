package com.multiwii.Utilities;

import com.multiwii.multiwiiremote.App;

import android.app.Activity;
import view.mjpeg.MjpegInputStream;
import view.mjpeg.MjpegView;

public class Camera {
	private MjpegView mv;
	private Activity mActivity;

	public Camera(MjpegView mv, Activity mActivity) {
		this.mv = mv;
		this.mActivity = mActivity;
	}

	public void start() {
		String URL = "http://" + ((App) mActivity.getApplication()).IpAddress
				+ ":8080/?action=stream";
		mv.setSource(MjpegInputStream.read(URL));
		mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
	}

	public void stop() {
		mv.stopPlayback();
	}
}
