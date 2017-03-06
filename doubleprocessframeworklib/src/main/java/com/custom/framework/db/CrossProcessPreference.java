package com.custom.framework.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.custom.framework.contants.Intents;
import com.custom.framework.service.broadcast.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Pair;

public class CrossProcessPreference implements SharedPreferences {

	private boolean refresh;
	private Map<String, String> backendMap;
	private File backendFile;
	private File backendLock;
	private Context context;
	private long lastModified;
	private ArrayList<WeakReference<OnSharedPreferenceChangeListener>> listenerList = new ArrayList<WeakReference<OnSharedPreferenceChangeListener>>();
	private LocalBroadcastManager broadcastManager;

	private static CrossProcessPreference _instance;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Tolerate any exception, allows tiny mis-synchronization
			try {
				int pid = intent.getIntExtra(Intents.EXTRA_PID, android.os.Process.myPid());
				String[] keys = intent.getStringArrayExtra(Intents.EXTRA_KEY_NAME);
				if (pid != android.os.Process.myPid()) {
					forceRefresh();
				}
				if (keys != null) {
					sendNotify(keys);
				}
			} catch (Exception e) {
			}
		}
	};

	public static CrossProcessPreference getInstance() {
		return _instance;
	}

	public static void instantiate(Context context) {
		_instance = new CrossProcessPreference(context);
	}

	public static CrossProcessPreference getInstance(Context context) {
		return new CrossProcessPreference(context);
	}

	private CrossProcessPreference(Context context) {
		this.context = context;
		this.backendFile = this.context.getFileStreamPath(this.context.getPackageName() + "_config.json");
		this.backendLock = this.context.getFileStreamPath(this.context.getPackageName() + "_config.lock");
		this.backendMap = new HashMap<String, String>();
		this.broadcastManager = LocalBroadcastManager.getInstance(context);
		this.broadcastManager.registerReceiver(receiver, Intents.ACTION_PREFERENCE_CHANGED);

		if (!this.backendFile.exists()) {
			Map<String, ?> pref = PreferenceManager.getDefaultSharedPreferences(context).getAll();
			if (pref != null) {
				for (Map.Entry<String, ?> entry : pref.entrySet()) {
					backendMap.put(entry.getKey(), entry.getValue().toString());
				}
			}
			refresh = false;
			saveToFile();
		} else {
			refresh = true;
		}
	}

	public Map<String, String> getCurrentPreference() {
		return new HashMap<String, String>(_getMap());
	}

	public void recycle() {
		// fix ytf
//		this.broadcastManager.unregisterReceiver(receiver);
		this.broadcastManager.unregisterReceiver(receiver);
	}

	public void forceRefresh() {
		refresh = true;
	}

	private Map<String, String> _getMap() {
		if (!refresh && backendFile.lastModified() != lastModified) {
			refresh = true;
		}
		if (refresh) {
			refresh = false;
			backendMap.clear();
			backendMap.putAll(Collections.synchronizedMap(readFromFile()));
			lastModified = backendFile.lastModified();
		}

		return backendMap;
	}

	private Map<String, String> getMapLocked() {
		FileLock lock = null;
		try {
			lock = lockFile();
			_getMap();
		} catch (Exception e) {
		} finally {
			try {
				lock.release();
			} catch (Exception ee) {
			}
		}

		return backendMap;
	}

	private HashMap<String, String> readFromFile() {
		HashMap<String, String> result = new HashMap<String, String>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(backendFile)));
			JSONObject object = new JSONObject(reader.readLine());
			JSONArray names = object.names();
			for (int i = 0; i < names.length(); i++) {
				String key = names.getString(i);
				result.put(key, object.getString(key));
			}
			reader.close();
		} catch (Exception e) {
		}

		return result;
	}

	private void saveToFile() {
		try {
			JSONObject object = new JSONObject();
			for (Map.Entry<String, String> entry : backendMap.entrySet()) {
				object.put(entry.getKey(), entry.getValue());
			}
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(backendFile)));
			writer.write(object.toString());
			writer.close();
			lastModified = backendLock.lastModified();
		} catch (Exception e) {
		}
	}

	private FileLock lockFile() throws Exception {
		return new RandomAccessFile(backendLock, "rwd").getChannel().lock();
	}

	@Override
	public boolean contains(String key) {
		return getMapLocked().containsKey(key);
	}

	@Override
	public Editor edit() {
		return new Editor();
	}

	@Override
	public Map<String, ?> getAll() {
		return new HashMap<String, String>(backendMap);
	}

	@Override
	public boolean getBoolean(String key, boolean defValue) {
		String value = getMapLocked().get(key);
		if (value == null) {
			return defValue;
		} else {
			try {
				return Boolean.parseBoolean(value);
			} catch (Exception e) {
				return defValue;
			}
		}
	}

	@Override
	public float getFloat(String key, float defValue) {
		String value = getMapLocked().get(key);
		if (value == null) {
			return defValue;
		} else {
			try {
				return Float.parseFloat(value);
			} catch (Exception e) {
				return defValue;
			}
		}
	}

	@Override
	public int getInt(String key, int defValue) {
		String value = getMapLocked().get(key);
		if (value == null) {
			return defValue;
		} else {
			try {
				return Integer.parseInt(value);
			} catch (Exception e) {
				return defValue;
			}
		}
	}

	@Override
	public long getLong(String key, long defValue) {
		String value = getMapLocked().get(key);
		if (value == null) {
			return defValue;
		} else {
			try {
				return Long.parseLong(value);
			} catch (Exception e) {
				return defValue;
			}
		}
	}

	@Override
	public String getString(String key, String defValue) {
		String value = getMapLocked().get(key);
		if (value == null) {
			return defValue;
		} else {
			return value;
		}
	}

	public Set<String> getStringSet(String arg0, Set<String> arg1) {
		return arg1;
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		synchronized (listenerList) {
			for (int i = 0; i < listenerList.size(); i++) {
				WeakReference<OnSharedPreferenceChangeListener> wr = listenerList.get(i);
				if (wr.get() == listener) {
					return;
				}
			}
			listenerList.add(new WeakReference<OnSharedPreferenceChangeListener>(listener));
		}
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		synchronized (listenerList) {
			for (int i = 0; i < listenerList.size(); i++) {
				WeakReference<OnSharedPreferenceChangeListener> wr = listenerList.get(i);
				if (wr.get() == listener || wr.get() == null) {
					listenerList.remove(i--);
				}
			}
		}
	}

	private void sendNotify(String[] keys) {
		synchronized (listenerList) {
			for (int i = 0; i < listenerList.size(); i++) {
				OnSharedPreferenceChangeListener l = listenerList.get(i).get();
				if (l == null) {
					listenerList.remove(i--);
				} else {
					for (int j = 0; j < keys.length; j++) {
						l.onSharedPreferenceChanged(CrossProcessPreference.this, keys[j]);
					}
				}
			}
		}
	}

	public class Editor implements SharedPreferences.Editor {

		private static final int ACTION_CLEAR = 0;
		private static final int ACTION_PUT = 1;
		private static final int ACTION_REMOVE = 2;

		private ArrayList<Pair<Integer, Pair<String, String>>> actionList;

		private Editor() {
			actionList = new ArrayList<Pair<Integer, Pair<String, String>>>();
		}

		@Override
		public Editor clear() {
			actionList.add(new Pair<Integer, Pair<String, String>>(ACTION_CLEAR, null));
			return this;
		}

		@Override
		public boolean commit() {
			HashSet<String> keySet = new HashSet<String>();
			FileLock lock = null;

			try {
				lock = lockFile();
				_getMap();
				for (int i = 0; i < actionList.size(); i++) {
					switch (actionList.get(i).first) {
					case ACTION_CLEAR: {
						keySet.addAll(backendMap.keySet());
						backendMap.clear();
						break;
					}

					case ACTION_PUT: {
						String key = actionList.get(i).second.first;
						String value = actionList.get(i).second.second;
						keySet.add(key);
						backendMap.put(key, value);
						break;
					}

					case ACTION_REMOVE: {
						String key = actionList.get(i).second.first;
						keySet.add(key);
						backendMap.remove(key);
						break;
					}

					}
				}

				if (keySet.size() != 0) {
					saveToFile();
				}
			} catch (Exception e) {
			} finally {
				try {
					lock.release();
				} catch (Exception ee) {
				}
			}

			if (keySet.size() != 0) {
				// fix ytf
//				broadcastManager.sendBroadcast(new Intent(Intents.ACTION_PREFERENCE_CHANGED).putExtra(Intents.EXTRA_PID, android.os.Process.myPid())
//						.putExtra(Intents.EXTRA_KEY_NAME, keySet.toArray(new String[0])));
				broadcastManager.sendBroadcast(new Intent(Intents.ACTION_PREFERENCE_CHANGED).putExtra(Intents.EXTRA_PID, android.os.Process.myPid())
						.putExtra(Intents.EXTRA_KEY_NAME, keySet.toArray(new String[0])));
			}

			return true;
		}

		@Override
		public Editor putBoolean(String key, boolean value) {
			actionList.add(new Pair<Integer, Pair<String, String>>(ACTION_PUT, new Pair<String, String>(key, Boolean.toString(value))));
			return this;
		}

		@Override
		public Editor putFloat(String key, float value) {
			actionList.add(new Pair<Integer, Pair<String, String>>(ACTION_PUT, new Pair<String, String>(key, Float.toString(value))));
			return this;
		}

		@Override
		public Editor putInt(String key, int value) {
			actionList.add(new Pair<Integer, Pair<String, String>>(ACTION_PUT, new Pair<String, String>(key, Integer.toString(value))));
			return this;
		}

		@Override
		public Editor putLong(String key, long value) {
			actionList.add(new Pair<Integer, Pair<String, String>>(ACTION_PUT, new Pair<String, String>(key, Long.toString(value))));
			return this;
		}

		@Override
		public Editor putString(String key, String value) {
			actionList.add(new Pair<Integer, Pair<String, String>>(ACTION_PUT, new Pair<String, String>(key, value)));
			return this;
		}

		@Override
		public Editor remove(String key) {
			actionList.add(new Pair<Integer, Pair<String, String>>(ACTION_REMOVE, new Pair<String, String>(key, null)));
			return this;
		}

		public void apply() {
			commit();
		}

		public Editor putStringSet(String arg0, Set<String> arg1) {
			return this;
		}
	}
}