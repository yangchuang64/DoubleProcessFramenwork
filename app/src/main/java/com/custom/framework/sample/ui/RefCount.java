package com.custom.framework.sample.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;

import com.custom.framework.sample.CustomFrameworkApplication;
import com.custom.framework.service.CustomFrameworkService;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class RefCount {
	private static final int AUTO_GC_DELAY = 10000;

	private static final HashSet<Activity> _activities = new HashSet<Activity>();
	private static Handler _handler = null;
	private static boolean paused = false;

	private static File oomAdj = new File("/proc/self/oom_adj");

	private static Runnable gcAction = new Runnable() {
		@Override
		public void run() {
			if (isForegroundApp()) {
				getHandler().removeCallbacksAndMessages(null);
				getHandler().postDelayed(gcAction, AUTO_GC_DELAY);
			} else {
				Intent intent = new Intent(CustomFrameworkService.ACTIVI_GC);
				intent.setClass(CustomFrameworkApplication.getApplication(), CustomFrameworkService.class);
				CustomFrameworkApplication.getApplication().startService(intent);
				System.runFinalization();
				System.exit(0);
			}
		}
	};

	private static boolean isForegroundApp() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(oomAdj));
			String strAdj = reader.readLine();
			reader.close();
			int currentAdj = Integer.parseInt(strAdj);
			if (currentAdj <= 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	private static Handler getHandler() {
		if (_handler == null) {
			_handler = new Handler(Looper.getMainLooper());
		}

		return _handler;
	}

	public static void removeActivity(Activity activity) {
		_activities.remove(activity);

		if (_activities.size() == 0 && !paused) {
			getHandler().postDelayed(gcAction, AUTO_GC_DELAY);
		}
	}

	public static void addActivity(Activity activity) {
		getHandler().removeCallbacksAndMessages(null);
		_activities.add(activity);
	}

	public static void pauseAutoGC() {
		paused = true;
	}

	public static void resumeAutoGC() {
		paused = false;

		if (_activities.size() == 0 && !paused) {
			getHandler().postDelayed(gcAction, AUTO_GC_DELAY);
		}
	}
}
