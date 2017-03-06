package com.custom.framework.sample.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;

import com.custom.framework.ServiceManager;
import com.custom.framework.ServiceManager.ServiceConnectedListener;

public class BaseActivity extends Activity implements ServiceConnectedListener {
	private static final int STATE_UNKNOWN = -1;
	private static final int STATE_CREATE = 0;
	private static final int STATE_START = 1;
	private static final int STATE_RESTART = 2;
	private static final int STATE_RESUME = 3;
	private static final int STATE_PAUSE = 4;
	private static final int STATE_STOP = 5;
	private static final int STATE_DESTROY = 6;

	private int state = STATE_UNKNOWN;
	private Bundle savedInstanceState;

	@Override
	protected final void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		state = STATE_CREATE;
		this.savedInstanceState = savedInstanceState;
		if (!ServiceManager.getInstance().isConnect()) {
			ServiceManager.getInstance().connect(this, this);
		} else {
			onServiceConnected(null);
		}
	}

	@Override
	protected final void onDestroy() {
		super.onDestroy();
		state = STATE_DESTROY;
		if (ServiceManager.getInstance().isConnect()) {
			onUIDestroy();
		}
		ServiceManager.getInstance().releaseService(this);
	}

	/**
	 * 如果要进入onCreate 请复写该方法
	 * 
	 * @param savedInstanceState
	 */
	protected void onUICreate(Bundle savedInstanceState) {
	}

	/**
	 * 如果要进入onDestroy 请复写该方法
	 */
	protected void onUIDestroy() {
	}

	@Override
	protected final void onPause() {
		super.onPause();
		state = STATE_PAUSE;
		if (ServiceManager.getInstance().isConnect()) {
			onUIPause();
		}
	}

	@Override
	protected final void onResume() {
		super.onResume();
		state = STATE_RESUME;
		if (ServiceManager.getInstance().isConnect()) {
			onUIResume();
		}
	}

	@Override
	protected final void onRestart() {
		super.onRestart();
		state = STATE_RESTART;
		if (ServiceManager.getInstance().isConnect()) {
			onUIRestart();
		}
	}

	@Override
	protected final void onStart() {
		super.onStart();
		RefCount.addActivity(this);
		state = STATE_START;
		if (ServiceManager.getInstance().isConnect()) {
			onUIStart();
		}
	}

	@Override
	protected final void onStop() {
		super.onStop();
		RefCount.removeActivity(this);
		state = STATE_STOP;
		if (ServiceManager.getInstance().isConnect()) {
			onUIStop();
		}
	}

	public void onUIRestart() {
	}

	public void onUIStart() {
	}

	public void onUIResume() {
	}

	public void onUIPause() {
	}

	public void onUIStop() {
	}

	public Object getService(Class serviceName, Class serviceStubName) {
		return ServiceManager.getInstance().getService(serviceName, serviceStubName, this, this);
	}

	@Override
	public void onServiceConnected(IBinder service) {
		switch (state) {
		case STATE_CREATE:
			onUICreate(savedInstanceState);
			break;

		case STATE_RESTART:
			onUICreate(savedInstanceState);
			onUIStart();
			onUIResume();
			onUIPause();
			onUIStop();
			onUIRestart();
			break;

		case STATE_START:
			onUICreate(savedInstanceState);
			onUIStart();
			break;

		case STATE_RESUME:
			onUICreate(savedInstanceState);
			onUIStart();
			onUIResume();
			break;

		case STATE_PAUSE:
			onUICreate(savedInstanceState);
			onUIStart();
			onUIResume();
			onUIPause();
			break;

		case STATE_STOP:
			onUICreate(savedInstanceState);
			onUIStart();
			onUIResume();
			onUIPause();
			onUIStop();
			break;

		case STATE_DESTROY:
			break;
		}
	}
}
