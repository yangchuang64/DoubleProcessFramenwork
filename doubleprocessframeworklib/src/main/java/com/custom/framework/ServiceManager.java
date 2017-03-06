package com.custom.framework;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.custom.framework.service.CustomFrameworkService;
import com.custom.framework.service.ICustomFrameworkService;

/**
 * ui获取service 实体工厂
 * 
 * @author dinghui
 * 
 */
public class ServiceManager{
	private static ServiceManager _instance;
	private ICustomFrameworkService serviceBinder;

	private Map<Integer, ServiceBean> pendingListener = new HashMap<Integer, ServiceBean>();
	private Set<ServiceConnectedListener> connectListener = new HashSet<ServiceConnectedListener>();
	private ServiceDisconnectedListener mServiceDisconnectedListener;

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			serviceBinder = ICustomFrameworkService.Stub.asInterface(arg1);
			Iterator<ServiceConnectedListener> listener = connectListener.iterator();
			while (listener.hasNext()) {
				listener.next().onServiceConnected(arg1);
				listener.remove();
			}

			if (pendingListener.size() > 0) {
				for (Integer key : pendingListener.keySet()) {
					ServiceBean sb = pendingListener.get(key);
					Iterator<String> names = sb.name.iterator();
					while (names.hasNext()) {
						try {
							sb.listener.onServiceConnected((IBinder) serviceBinder.getService(names.next()));
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
//			DingFramework.getApplication().bindService(new Intent(DingFramework.getApplication(), DingService.class), this, Context.BIND_AUTO_CREATE);
			if (mServiceDisconnectedListener != null)
				mServiceDisconnectedListener.onServiceDisconnected();
		}
	};

	public static ServiceManager getInstance() {
		if (_instance == null) {
			_instance = new ServiceManager();
		}
		return _instance;
	}

	/**
	 * 获取service 实体
	 * 
	 * @param name
	 * @param listener
	 * @param context
	 * @return
	 */
	public synchronized Object getService(Class serviceCls, Class serviceStubCls, ServiceConnectedListener listener, Context context) {
		String serviceName = serviceCls.getSimpleName();
		if (serviceBinder != null) {
			Object service;
			try {
				IBinder binder = serviceBinder.getService(serviceName);

				Class stub = Class.forName(serviceStubCls.toString().replace("interface", "").trim() + "$Stub");
				Method[] methods = stub.getMethods();
				Method asInterface = null;
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].getName().equals("asInterface")) {
						asInterface = methods[i];
					}
				}
				service = asInterface.invoke(stub, binder);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			if (listener != null) {
				addPending(serviceName, listener, context);
				// listener.onServiceConnected(service);
			}
			return service;
		}
		if (listener != null) {
			addPending(serviceName, listener, context);
		}
		return null;
	}

	/**
	 * 建立service链接
	 * 
	 * @param listener
	 */
	public synchronized void connect(Context context, ServiceConnectedListener listener) {
		context.bindService(new Intent(context, CustomFrameworkService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
		connectListener.add(listener);
	}

	/**
	 * 判断是否链接
	 * 
	 * @return
	 */
	public boolean isConnect() {
		return serviceBinder != null;
	}

	private void addPending(String name, ServiceConnectedListener listener, Context context) {
		if (!pendingListener.containsKey(Integer.valueOf(context.hashCode()))) {
			ServiceBean sb = new ServiceBean();
			sb.name.add(name);
			sb.listener = listener;
			pendingListener.put(Integer.valueOf(context.hashCode()), sb);
		} else {
			pendingListener.get(Integer.valueOf(context.hashCode())).name.add(name);
		}
	}

	/**
	 * 释放service
	 * 
	 * @param context
	 */
	public void releaseService(Context context) {
		if (pendingListener.containsKey(Integer.valueOf(context.hashCode()))) {
			ServiceBean sb = pendingListener.get(Integer.valueOf(context.hashCode()));
			Iterator<String> names = sb.name.iterator();
			while (names.hasNext()) {
				try {
					serviceBinder.releaseService(names.next());
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			pendingListener.remove(Integer.valueOf(context.hashCode()));
		}
	}

	public static interface ServiceConnectedListener {
		public void onServiceConnected(IBinder service);
	}

	public interface ServiceDisconnectedListener {
		public void onServiceDisconnected();
	}

	private class ServiceBean {
		public Set<String> name = new HashSet<String>();
		public ServiceConnectedListener listener;
	}
}
