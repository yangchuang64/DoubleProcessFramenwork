package com.custom.framework.service;

interface ICustomFrameworkService {
	void registerService(in String name, in IBinder service);
	IBinder getService(in String name);
	IBinder getLocalBroacastService();
	void exit(long delay, boolean force);
	void releaseService(String name);
}