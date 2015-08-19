package com.yy.voice.util;

import android.util.Log;

/**
 *
 * 打log专用的类
 *
 * @author yangyang.qian
 *
 */
public class LOG {

	private static final String TAG = "yy_voice";

	public static void I(String msg) {
		Log.i(TAG, msg);
	}

	public static void D(String msg) {
		Log.d(TAG, msg);
	}

	public static void E(String msg) {
		Log.e(TAG, msg);
	}
}
