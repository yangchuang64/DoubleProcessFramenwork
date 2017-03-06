package com.custom.framework.sample;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

import com.custom.framework.contants.ApplicationConfig;
import com.custom.framework.db.CrossProcessPreference;
import com.custom.framework.sample.service.MainService;
import com.custom.framework.service.BaseService.onStatusChangeListener;
import com.custom.framework.service.CustomFrameworkService;
import com.custom.framework.service.ServiceConfigHandle;
import com.custom.framework.service.ServiceConfigHandle.ServiceConfig;
import com.custom.framework.service.core.ServiceData;
import com.custom.framework.service.core.ServiceManager;

public class CustomFrameworkApplication extends Application {
	public static CustomFrameworkApplication _instance;
	private static final int SET_FOREGROUND_PROCESS = ServiceData.getIPCOpcode("android.app.IActivityManager", "SET_PROCESS_FOREGROUND_TRANSACTION",
			-1);

	public static CustomFrameworkApplication getApplication() {
		return _instance;
	}

	@Override
	public void onCreate() {
		super.onCreate();
//		CrashHandler crashHandler = CrashHandler.getInstance();
//		crashHandler.init(getApplicationContext());
		_instance = this;
		initAntiTheft();
		startService(new Intent(this, getMainService()));
		CrossProcessPreference.instantiate(this);

		ServiceConfigHandle.getInstance().init(new MServiceConfig());
	}

	/**
	 * 防盗服务初始化
	 */
	public void initAntiTheft() {
		List<RunningAppProcessInfo> l = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getRunningAppProcesses();
		if (l != null) {
			RunningAppProcessInfo self = null;
			for (RunningAppProcessInfo info : l) {
				if (info.pid == android.os.Process.myPid()) {
					self = info;
					break;
				}
			}
			if (self != null) {
				System.out.println("process name:" + self.processName + " getPackageName:" + getPackageName());
				if (!self.processName.equals(getPackageName())) {
					ApplicationConfig.IS_SERVICE_PROCESS = true;
					if (ApplicationConfig.IS_SERVICE_PROCESS) {
						doSelfProtect();
					}
				}
			}
		}
	}

	public static Class getMainService() {
		return CustomFrameworkService.class;
	}

	private void doSelfProtect() {
		if (SET_FOREGROUND_PROCESS < 0)
			return;

		Parcel data = Parcel.obtain();
		Parcel reply = Parcel.obtain();
		try {
			IBinder token = new Binder();
			IBinder activityService = ServiceManager.getServiceManager().checkService("activity");
			data.writeInterfaceToken("android.app.IActivityManager");
			data.writeStrongBinder(token);
			data.writeInt(android.os.Process.myPid());
			data.writeInt(1);
			activityService.transact(SET_FOREGROUND_PROCESS, data, reply, 0);
			reply.readException();
		} catch (Exception e) {
		} finally {
			data.recycle();
			reply.recycle();
		}
	}

	class MServiceConfig extends ServiceConfig {

		@Override
		public Object getService(Context context, String className, onStatusChangeListener changeListener) {
			if (className.equals(MainService.class.getSimpleName()))
				return new MainService(context, changeListener);
			return null;
		}
	}
}
