package com.custom.framework.service.broadcast;

import android.content.Intent;

interface ILocalBroadcastService {
	boolean sendBroadcast(in Intent intent);
	boolean sendStickyBroadcast(in Intent intent);
	void removeStickyBroadcast(in Intent intent);
	Intent registerReceiver(in IBinder receiver, in String action);
	void unregisterReceiver(in IBinder receiver);
}