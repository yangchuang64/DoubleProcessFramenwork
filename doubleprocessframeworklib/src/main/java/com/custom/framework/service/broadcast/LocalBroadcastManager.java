package com.custom.framework.service.broadcast;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.custom.framework.ServiceManager;
import com.custom.framework.ServiceManager.ServiceConnectedListener;
import com.custom.framework.service.ICustomFrameworkService;
import com.custom.framework.service.broadcast.ILocalBroadcastService;

public class LocalBroadcastManager {

	public static final String SERVICE_NAME = "LocalBroadcastManager";

	private static LocalBroadcastManager _instance;

	public static synchronized LocalBroadcastManager getInstance(Context context) {
		if (_instance == null) {
			_instance = new LocalBroadcastManager(context.getApplicationContext());
		}
		return _instance;
	}

	public LocalBroadcastManager(Context context) {
		mAppContext = context;
		ServiceManager.getInstance().connect(context, new ServiceConnectedListener() {

			@Override
			public void onServiceConnected(IBinder service) {
				try {
					synchronized (this) {
						mBroadcastManager = ILocalBroadcastService.Stub.asInterface(ICustomFrameworkService.Stub.asInterface(service).getLocalBroacastService());
						if (mBroadcastManager != null) {
							batchApply();
						}
					}
				} catch (Exception e) {
					synchronized (this) {
						mBroadcastManager = null;
					}
				}
			}
		});
	}

	private final ArrayList<Intent> mPendingBroadcasts = new ArrayList<Intent>();
	protected final HashMap<String, Intent> mStickyBroadcasts = new HashMap<String, Intent>();
	protected final HashMap<BroadcastReceiver, LocalBroadcastReceiver> mLocalReceivers = new HashMap<BroadcastReceiver, LocalBroadcastReceiver>();
	protected Context mAppContext;
	protected ILocalBroadcastService mBroadcastManager;

	public Context getContext() {
		return mAppContext;
	}
	
	public boolean sendBroadcast(Intent intent) {
		try {
			return mBroadcastManager.sendBroadcast(intent);
		} catch (Exception e) {
			mPendingBroadcasts.add(intent);
			return false;
		}
	}

	public boolean sendStickyBroadcast(Intent intent) {
		try {
			return mBroadcastManager.sendStickyBroadcast(intent);
		} catch (Exception e) {
			mStickyBroadcasts.put(intent.getAction(), intent);
			return false;
		}
	}

	public void removeStickyBroadcast(Intent intent) {
		try {
			mBroadcastManager.removeStickyBroadcast(intent);
		} catch (Exception e) {
			mStickyBroadcasts.remove(intent.getAction());
		}
	}

	public Intent registerReceiver(BroadcastReceiver receiver, String action) {
		LocalBroadcastReceiver lbr = null;
		if (receiver != null) {
			synchronized (mLocalReceivers) {
				lbr = mLocalReceivers.get(receiver);
				if (lbr == null) {
					lbr = new LocalBroadcastReceiver(mAppContext, receiver, action);
					mLocalReceivers.put(receiver, lbr);
				} else {
					lbr.addAction(action);
				}
			}
		}

		synchronized (this) {
			try {
				return mBroadcastManager.registerReceiver(lbr, action);
			} catch (Exception e) {
				return mStickyBroadcasts.get(action);
			}
		}
	}

	public void unregisterReceiver(BroadcastReceiver receiver) {
		LocalBroadcastReceiver lbr = null;
		synchronized (mLocalReceivers) {
			lbr = mLocalReceivers.remove(receiver);
		}

		synchronized (this) {
			try {
				mBroadcastManager.unregisterReceiver(lbr);
			} catch (Exception e) {
			}
		}

		if (lbr != null) {
			lbr.receiver = null;
			lbr.actions = null;
		}
	}

	private void batchApply() {
		// batch apply mPendingBroadcasts, mStickyBroadcasts, mLocalReceivers;
		synchronized (mLocalReceivers) {
			try {
				for (LocalBroadcastReceiver lbr : mLocalReceivers.values()) {
					for (String action : lbr.actions) {
						mBroadcastManager.registerReceiver(lbr, action);
					}
				}
			} catch (Exception e) {
			}
		}

		try {
			for (Intent intent : mStickyBroadcasts.values()) {
				mBroadcastManager.sendStickyBroadcast(intent);
			}
			for (Intent intent : mPendingBroadcasts) {
				mBroadcastManager.sendBroadcast(intent);
			}
		} catch (Exception e) {
		} finally {
			mStickyBroadcasts.clear();
			mPendingBroadcasts.clear();
		}
	}
}
