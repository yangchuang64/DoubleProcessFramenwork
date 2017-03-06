package com.custom.framework.service.broadcast;

import android.content.Intent;

interface ILocalBroadcastReceiver {
	void onReceive(in Intent intent);
}