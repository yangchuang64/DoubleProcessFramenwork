package com.custom.framework.service.broadcast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.custom.framework.service.broadcast.ILocalBroadcastReceiver;
import com.custom.framework.service.broadcast.ILocalBroadcastService;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.IBinder;

public class LocalBroadcastServiceHandle {
	private static LocalBroadcastServiceHandle mLocalBroadcastServiceHandle;
	private LocalBroadcastService mLocalBroadcastService;
	
	public static LocalBroadcastServiceHandle getInstance() {
		if (mLocalBroadcastServiceHandle == null) {
			mLocalBroadcastServiceHandle = new LocalBroadcastServiceHandle();
		}
		return mLocalBroadcastServiceHandle;
	}
	
	private LocalBroadcastServiceHandle() {
		mLocalBroadcastService = new LocalBroadcastService();
	}
	
	public LocalBroadcastService getLocalBroadcastService() {
		return mLocalBroadcastService;
	}
	
	public static class LocalBroadcastService extends ILocalBroadcastService.Stub {
		protected final HashMap<IBinder, ILocalBroadcastReceiver> mBinderCache = new HashMap<IBinder, ILocalBroadcastReceiver>();
		protected final HashMap<IBinder, HashSet<String>> mReceivers = new HashMap<IBinder, HashSet<String>>();
		protected final HashMap<String, HashSet<IBinder>> mActions = new HashMap<String, HashSet<IBinder>>();
		protected final HashMap<String, Intent> mStickyBroadcasts = new HashMap<String, Intent>();
		protected final HashMap<BroadcastReceiver, LocalBroadcastReceiver> mLocalReceivers = new HashMap<BroadcastReceiver, LocalBroadcastReceiver>();

		@Override
		public Intent registerReceiver(IBinder receiver, String action) {
			System.out.println("registerReceiver");
			ILocalBroadcastReceiver recv = null;
			if (receiver != null) {
				synchronized (mReceivers) {
					HashSet<String> actions = mReceivers.get(receiver);
					if (actions == null) {
						actions = new HashSet<String>(1);
						mReceivers.put(receiver, actions);
					}
					actions.add(action);

					HashSet<IBinder> receivers = mActions.get(action);
					if (receivers == null) {
						receivers = new HashSet<IBinder>(1);
						mActions.put(action, receivers);
					}
					receivers.add(receiver);
				}
				synchronized (mBinderCache) {
					recv = mBinderCache.get(receiver);
					if (recv == null) {
						recv = ILocalBroadcastReceiver.Stub.asInterface(receiver);
						mBinderCache.put(receiver, recv);
					}
				}
			}

			synchronized (mStickyBroadcasts) {
				Intent intent = mStickyBroadcasts.get(action);
				if (intent != null) {
					try {
						recv.onReceive(intent);
					} catch (Exception e) {
					}
					return intent;
				}
			}

			return null;
		}

		@Override
		public void unregisterReceiver(IBinder receiver) {
			if (receiver != null) {
				synchronized (mReceivers) {
					HashSet<String> actions = mReceivers.remove(receiver);
					if (actions != null) {
						for (String action : actions) {
							HashSet<IBinder> receivers = mActions.get(action);
							if (receivers != null) {
								receivers.remove(receiver);
							}
						}
					}
				}
				synchronized (mBinderCache) {
					mBinderCache.remove(receiver);
				}
			}
		}

		@Override
		public boolean sendBroadcast(Intent intent) {
			synchronized (mReceivers) {
				final String action = intent.getAction();
				HashSet<IBinder> receivers = mActions.get(action);
				if (receivers != null) {
					for (Iterator<IBinder> iterator = receivers.iterator(); iterator.hasNext();) {
						IBinder receiver = iterator.next();
						try {
							mBinderCache.get(receiver).onReceive(intent);
						} catch (Exception e) {
							if (!receiver.isBinderAlive()) {
								iterator.remove();
							}
						}
					}
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean sendStickyBroadcast(Intent intent) {
			synchronized (mStickyBroadcasts) {
				mStickyBroadcasts.put(intent.getAction(), intent);
			}
			return sendBroadcast(intent);
		}

		@Override
		public void removeStickyBroadcast(Intent intent) {
			synchronized (mStickyBroadcasts) {
				mStickyBroadcasts.remove(intent.getAction());
			}
		}
	}

}
