package com.custom.framework.service;

import android.content.Context;

public abstract class BaseService {
	protected Context context;
	/**
	 * 是要否回收
	 */
	private boolean isRecover = false;
	/**
	 * 工作状态计数器
	 */
	private int workingCount = 0;
	/**
	 * 状态监听接口
	 */
	private onStatusChangeListener listener;

	public BaseService(Context context, onStatusChangeListener listener) {
		this.context = context;
		this.listener = listener;
		onCreate();
	}

	/**
	 * 回收
	 * 
	 * @param isNow
	 *            是否立即回收
	 * @return 是否被及时回收
	 */
	public synchronized boolean release(boolean isNow) {
		isRecover = true;
		if (isNow) {
			onDestroy();
			return true;
		} else {
			if (workingCount <= 0) {
				onDestroy();
				return true;
			}
			return false;
		}
	}

	/**
	 * 设置工作状态
	 * 
	 * @param isWorking
	 *            true在工作中 false则是可以回收
	 */
	public synchronized void setIsWorking(boolean isWorking) {
		if (isWorking) {
			workingCount++;
		} else {
			workingCount--;
			if (isRecover && workingCount <= 0) {
				release(true);
			}
		}
	}

	/**
	 * 当创建时候调用
	 */
	public void onCreate() {
		if (listener != null) {
			listener.onCreate(this);
		}
	}

	/**
	 * 当销毁时调用
	 */
	public void onDestroy() {
		if (listener != null)
			listener.onDestroy(this);
	}

	public abstract String getServiceName();

	public interface onStatusChangeListener {
		public void onCreate(BaseService service);

		public void onDestroy(BaseService service);
	}

	public interface Theft {
		public BaseService getBaseService();

		public boolean release(boolean b);
	}
}