package com.custom.framework.service;

import java.util.HashMap;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.custom.framework.service.BaseService.Theft;
import com.custom.framework.service.BaseService.onStatusChangeListener;
import com.custom.framework.service.broadcast.LocalBroadcastServiceHandle;
import com.custom.framework.service.ICustomFrameworkService;

public class CustomFrameworkService extends Service {
	public static final String ACTIVI_GC = "activity_gc";

	private final HashMap<String, IBinder> serviceMap = new HashMap<String, IBinder>();
	private int serCount = 0;

	@Override
	public void onCreate() {
		initService();
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			return super.onStartCommand(intent, flags, startId);
		}
		if (ACTIVI_GC.equals(intent.getAction())) {
			if (serCount == 0) {
				eixt();
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		eixt();
	}

	public void eixt() {
		stopSelf();
		Log.i("service", "stopSelf");
		System.runFinalization();
		System.exit(0);
	}

	private void initService() {
//		LocalBroadcastManager.getInstance(this);
	}

	private void finalizeServices() {

	}

	@Override
	public IBinder onBind(Intent intent) {
		return remateService;
	}

	public onStatusChangeListener changeListener = new onStatusChangeListener() {

		@Override
		public void onDestroy(BaseService service) {
			serCount--;
			serviceMap.remove(service.getServiceName());
			if (serCount == 0) {
				eixt();
			}
		}

		@Override
		public void onCreate(BaseService service) {
			serCount++;
		}
	};

	private ICustomFrameworkService.Stub remateService = new ICustomFrameworkService.Stub() {

		@Override
		public void registerService(String name, IBinder service) throws RemoteException {
			serviceMap.put(name, service);
		}

		@Override
		public IBinder getService(String name) throws RemoteException {
			if (serviceMap.containsKey(name)) {
				return serviceMap.get(name);
			} else {
				IBinder ser = (IBinder) ServiceConfigHandle.getInstance().getService(CustomFrameworkService.this, name, changeListener);
				serviceMap.put(name, ser);
				return serviceMap.get(name);
			}
		}

		public IBinder getLocalBroacastService() throws RemoteException {
			return LocalBroadcastServiceHandle.getInstance().getLocalBroadcastService();
		}
		
		@Override
		public void exit(long delay, boolean force) throws RemoteException {
			if (force) {
				finalizeServices();
				System.exit(0);
			} else {
				Handler handler = new Handler(Looper.getMainLooper());
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						finalizeServices();
						stopSelf();
					}
				}, delay);
			}
		}

		public void releaseService(String name) {
			if (serviceMap.containsKey(name)) {
				Theft service = (Theft) serviceMap.get(name);
				if (service.release(false)) {
					serviceMap.remove(name);
				}
			}
		}

	};

}
