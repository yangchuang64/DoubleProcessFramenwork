package com.custom.framework.service.broadcast;

import java.util.HashSet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Pair;

import com.custom.framework.service.broadcast.ILocalBroadcastReceiver;

public class LocalBroadcastReceiver extends ILocalBroadcastReceiver.Stub {
	private static final int MSG_RECEIVED_MESSAGE = 1;
	private static Handler _handler;
	private static Context mContext;

	public BroadcastReceiver receiver;
	public HashSet<String> actions;

	public LocalBroadcastReceiver(Context context, BroadcastReceiver receiver, String action) {
		mContext = context;
		this.receiver = receiver;
		this.actions = new HashSet<String>(1);
		this.actions.add(action);
	}

	public void addAction(String action) {
		actions.add(action);
	}

	@Override
	public void onReceive(Intent intent) throws RemoteException {
		getHandler().obtainMessage(MSG_RECEIVED_MESSAGE, new Pair<BroadcastReceiver, Intent>(receiver, intent)).sendToTarget();
	}

	private synchronized Handler getHandler() {
		if (_handler == null) {
//			_handler = new Handler(DingFramework.getApplication().getMainLooper()) {
			_handler = new Handler(Looper.getMainLooper()) {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case MSG_RECEIVED_MESSAGE:
						@SuppressWarnings("unchecked")
						Pair<BroadcastReceiver, Intent> pair = (Pair<BroadcastReceiver, Intent>) msg.obj;
//						pair.first.onReceive(DingFramework.getApplication(), pair.second);
						pair.first.onReceive(mContext, pair.second);
						break;
					}
				}
			};
		}

		return _handler;
	}
}
