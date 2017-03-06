package com.custom.framework.service;

import com.custom.framework.service.BaseService.onStatusChangeListener;

import android.content.Context;
import android.util.Log;

public class ServiceConfigHandle {
	private static ServiceConfigHandle _instance;
	private ServiceConfig mServiceConfig;

	public static ServiceConfigHandle getInstance() {
		if (_instance == null) {
			synchronized (ServiceConfigHandle.class) {
				if (_instance == null) {
					Log.i("DingService", "my pid: " + android.os.Process.myPid());
					_instance = new ServiceConfigHandle();
				}
			}
		}
		return _instance;
	}

	public void init(ServiceConfig serviceConfig) {
		mServiceConfig = serviceConfig;
	}

	/**
	 * 这里请返回 Service实体类 Service跨进程ipc传入UI时调用
	 * 
	 * @param name
	 * @return
	 */
	public Object getService(Context context, String name, onStatusChangeListener changeListener) {
		if (mServiceConfig != null)
			return mServiceConfig.getService(context, name, changeListener);
		return null;
	}

	public static abstract class ServiceConfig {
		public abstract Object getService(Context context, String className, onStatusChangeListener changeListener);
	}
}
