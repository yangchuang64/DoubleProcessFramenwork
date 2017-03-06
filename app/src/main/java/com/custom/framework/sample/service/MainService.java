package com.custom.framework.sample.service;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import com.custom.framework.contants.Intents;
import com.custom.framework.sample.MainActivity;
import com.custom.framework.service.BaseService;
import com.custom.framework.service.BaseService.Theft;
import com.custom.framework.service.BaseService.onStatusChangeListener;
import com.custom.framework.service.broadcast.LocalBroadcastServiceHandle;

public class MainService extends IMainService.Stub implements Theft {
	public static final String SERVICE_NAME = "userService";

	private Thefts thefts;
	int i = 0;

	public MainService(Context context, onStatusChangeListener listener) {
		thefts = new Thefts(context, listener);
	}

	@Override
	public void update() throws RemoteException {
		Log.i("framework", "service update");
		new Thread() {
			public void run() {
				thefts.setIsWorking(true);
				Intent intent = new Intent(MainActivity.ACTION_UPDATE);
				intent.putExtra(Intents.EXTRAS_SER_RESULT, "" + i++);
				LocalBroadcastServiceHandle.getInstance().getLocalBroadcastService().sendBroadcast(intent);
				thefts.setIsWorking(false);
			};
		}.start();
	}

	public BaseService getBaseService() {
		return thefts;
	}

	@Override
	public boolean release(boolean b) {
		return thefts.release(b);
	}

	public class Thefts extends BaseService {

		public Thefts(Context context, onStatusChangeListener listener) {
			super(context, listener);
		}

		@Override
		public String getServiceName() {
			return SERVICE_NAME;
		}

	}
}
