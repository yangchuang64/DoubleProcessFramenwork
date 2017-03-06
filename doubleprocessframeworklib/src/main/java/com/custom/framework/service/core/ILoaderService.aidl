package com.custom.framework.service.core;

interface ILoaderService {
	int getLoaderVersion();
	int[] getLoadedExpansions();
	int registerUid(String apiKey);
	int validatePid(String apiKey, int pid);
	int injectProcess(String soName, String soFile, String target, String service, int funcId, int version, boolean waitfor);
	int registerProxy(String service, String proxy);
	void unregisterProxy(String service);
	int createProcess(String cmdLine, int uid, boolean waitfor);
	int killProcess(int pid, int signal);
	int loadExpansion(String soFile);
	IBinder getBuffer(int which);
	IBinder setUidBlackWhiteList(boolean forBlackList, in int[] uids);
	void resetActionCache();
	int getInjectionVersion(String soName);
	void reboot(String apiKey, int remoteCode);
	int injectProcess2(String soName, String soFile, String target, String service, int funcId, int version);
}