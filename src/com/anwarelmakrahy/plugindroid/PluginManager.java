package com.anwarelmakrahy.plugindroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.Log;

public class PluginManager {

	private static final String LOG_TAG = "PluginDroid";
	
	private static final String PLUGIN_STANDALONE = "com.anwarelmakahy.plugindroid.standalone";
	private static final String PLUGIN_SHARED = "com.anwarelmakahy.plugindroid.shared";
	
	private static List<String> registeredPlugins = new ArrayList<String>();
	private static Map<String, PluginDetails> loadedPlugins = new HashMap<String, PluginDetails>();
	
	public static void registerPlugin(String name) {
		registeredPlugins.add(name);
	}
	
	private static PackageBroadcastReceiver packageBroadcastReceiver;
	private static IntentFilter packageFilter;
	
	private static PluginDetails pluginDetails;
	public static void loadPlugins(Context context, String pluginSignature) {
		PackageManager pm = context.getPackageManager();
		
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
		
		Intent baseIntent = new Intent(pluginSignature);
		baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        List<ResolveInfo> list = pm.queryIntentServices(
        		baseIntent, 
        		PackageManager.GET_RESOLVED_FILTER 
        		);

		
        for (int i=0; i<list.size(); i++) {
            ResolveInfo info = list.get(i);
            ServiceInfo sinfo = info.serviceInfo;
			IntentFilter filter = info.filter;
			
			if (sinfo != null) {
				
				for( Iterator<String> actionIterator = filter.actionsIterator(); actionIterator.hasNext();) {
					String packageName = actionIterator.next();
					if (registeredPlugins.contains(packageName)) {
						
						pluginDetails = new PluginDetails();
						pluginDetails.packageName = packageName;
						
						// Get Plugin Category
						if (filter.hasCategory(PLUGIN_STANDALONE))
							pluginDetails.type = PLUGIN_STANDALONE;
						else if (filter.hasCategory(PLUGIN_SHARED))
							pluginDetails.type = PLUGIN_SHARED;
						
						
						
						Log.i(LOG_TAG, "Package " + pluginDetails.packageName + " Loaded");
						loadedPlugins.put(packageName, pluginDetails);
						break;
					}
				}
			}

        }
	}
	
	public Map<String, PluginDetails> getLoadedPlugins() {
		return loadedPlugins;
	}
	
	public static class PluginDetails {
		String packageName;
		String type;
	}
	
	private static void removeRegisteredPlugin(String packageName) {
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
				removeRegisteredPlugin(packageName);
			}
			else if (Intent.ACTION_PACKAGE_ADDED.equals(action) &&
					registeredPlugins.contains(packageName)) {
				Log.i(LOG_TAG, "New installed plugin" + packageName);
			}

		}
	}
}
