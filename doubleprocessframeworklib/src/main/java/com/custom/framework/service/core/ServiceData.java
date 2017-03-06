package com.custom.framework.service.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class ServiceData {

	private static final String IPC_Prefix = "TRANSACTION_";
	private static final String Stub_Prefix = "$Stub";

	public static int getIPCOpcode(String className, String opcodeName, int defaultValue) {
		try {
			Class<?> cls = Class.forName(className);
			Field field = cls.getDeclaredField(opcodeName);
			field.setAccessible(true);
			Integer code = field.getInt(null);
			return code;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static Integer[] getFuzzledIPCOpcode(String className, String opCodeName, int defaultValue) {
		Set<Integer> codes = new HashSet<Integer>();
		try {
			String infName = className.replace(Stub_Prefix, "");
			Class<?> cls = Class.forName(className);
			Class<?> inf = Class.forName(infName);
			Method[] methods = inf.getDeclaredMethods();
			Method stdMethod = null;
			for (Method method : methods) {
				if (method.getName().equals(opCodeName))
					stdMethod = method;
			}
			Class<?>[] stdParameters = stdMethod.getParameterTypes();
			String transactionName = IPC_Prefix + opCodeName;
			Field[] fields = cls.getDeclaredFields();
			for (Field field : fields) {
				if (field.getType() != int.class)
					continue;
				String fieldName = field.getName();
				if (!fieldName.startsWith(transactionName))
					continue;
				if (fieldName.equals(transactionName)) {
					field.setAccessible(true);
					codes.add(field.getInt(null));
					continue;
				}
				fieldName = fieldName.replaceFirst(IPC_Prefix, "");
				for (Method method : methods) {
					if (method.getName().equals(fieldName)) {
						Class<?>[] parameters = method.getParameterTypes();
						if (parameters.length < stdParameters.length)
							continue;
						boolean match = true;
						for (int i = 0; i < stdParameters.length; i++)
							if (parameters[i] != stdParameters[i]) {
								match = false;
								break;
							}
						if (match) {
							field.setAccessible(true);
							codes.add(field.getInt(null));
							continue;
						}
					}
				}
			}
		} catch (Exception e) {
			codes.add(defaultValue);
		}

		return codes.toArray(new Integer[0]);
	}

	public static class PowerManager {
		public static class OpCodes {
			public static final int TRANSACTION_reboot = getIPCOpcode("android.os.IPowerManager$Stub", "TRANSACTION_reboot",
					android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
		}
	}
}
