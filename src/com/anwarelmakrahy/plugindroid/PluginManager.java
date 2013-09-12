package com.anwarelmakrahy.plugindroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class PluginManager {
	
	private static List<String> registeredPlugins = new ArrayList<String>();
	private static Map<String, PluginDetails> loadedPlugins = new HashMap<String, PluginDetails>();
	
	public static void registerPlugin(String name) {
		registeredPlugins.add(name);
	}
	
	private static PackageBroadcastReceiver packageBroadcastReceiver;
	private static IntentFilter packageFilter;
	
	private static PackageManager pm;
	
	private static PluginDetails pluginDetails;
	public static void loadPlugins(Context context, String pluginSignature) {
		
		if (pm == null)
			pm = context.getPackageManager();
		
		if (!isPkgBReceiverRunning) {
			packageBroadcastReceiver = new PackageBroadcastReceiver();
			packageFilter = new IntentFilter();
			packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
			packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
			packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
			packageFilter.addDataScheme("package");
			
			context.registerReceiver(packageBroadcastReceiver, packageFilter);
			isPkgBReceiverRunning = true;
		}
		
		loadPackages(context);
	}	
	
	private static void loadPackages(Context context) {
		loadedPlugins.clear();
		for (int i=0; i<registeredPlugins.size(); i++) {
			PackageInfo pkgInfo = getPackageInfo(context, registeredPlugins.get(i));
			
			if (pkgInfo == null) break;
			
			pluginDetails = new PluginDetails(
					registeredPlugins.get(i),
					pkgInfo,
					pm);
			
			loadedPlugins.put(registeredPlugins.get(i), pluginDetails);
		}
	}
	
	public static Map<String, PluginDetails> getLoadedPlugins() {
		return loadedPlugins;
	}
	
	public static class PluginDetails {	
		PluginDetails(String packageName, PackageInfo info, PackageManager pm) {
			this.packageName = packageName;
			this.pkgInfo = info;
			this.pm = pm;
		}
		
		public PackageInfo getPackageInfo() {
			return pkgInfo;
		}
		
		public String getPackageName() {
			return packageName;
		}
		
		public PackageManager getPackageManager() {
			return pm;
		}
		
		private String packageName;
		private PackageInfo pkgInfo;
		private PackageManager pm;
	}
	
	private static void unregisteredPlugin(String packageName) {
		if (registeredPlugins.contains(packageName))
			registeredPlugins.remove(packageName);
	
		if (loadedPlugins.containsKey(packageName))
			loadedPlugins.remove(packageName);
	}
	
	private static boolean isPkgBReceiverRunning = false;
	private static class PackageBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			String packageName = intent.getData().getSchemeSpecificPart();
			
			if (Intent.ACTION_PACKAGE_REMOVED.equals(action) &&
					registeredPlugins.contains(packageName)) {
				unregisteredPlugin(packageName);
			}
			else if (Intent.ACTION_PACKAGE_ADDED.equals(action) &&
					registeredPlugins.contains(packageName)) {
				loadPackages(context);
			}
			
			if (packageTableChangeListener != null)
				packageTableChangeListener.packageTableChanged(action, packageName);

		}
	}
	
	private static PackageTableChangeListener packageTableChangeListener;
	public static void setPackageTableChangerListener(PackageTableChangeListener listener) {
		if (listener != null)
			packageTableChangeListener = listener;
	}
	
	public static abstract class PackageTableChangeListener {
		protected void 
		packageTableChanged(
				String action, 
				String packageName) {
		}
	}
	
	
	
	public static Class<?> newPluginContext(Context context, String packageName) {
		if (!loadedPlugins.containsKey(packageName))
			return null;
		
		DexClassLoader dLoader = new DexClassLoader(
				loadedPlugins.get(packageName).getPackageInfo().applicationInfo.sourceDir,
				context.getFilesDir().getAbsolutePath(),
				null,
				ClassLoader.getSystemClassLoader().getParent());
		
		try {
			Class<?> newClass = dLoader.loadClass(packageName + ".ifs");
			return newClass;
		} catch (ClassNotFoundException e) {
			return null;
		}
	
	}
	
	private static PackageInfo getPackageInfo(
			Context context, 
			String packageName) {
	    try {
	        return context.getPackageManager().getPackageInfo(
	        		packageName, 
	        		PackageManager.GET_META_DATA);

	    } catch (Exception e) {
	        return null;
	    }
	}
}
