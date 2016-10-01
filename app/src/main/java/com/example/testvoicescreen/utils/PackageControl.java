
package com.example.testvoicescreen.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.util.Log;

public class PackageControl {

    private static String TAG = PackageControl.class.getSimpleName();

    public static Set<String> sKeyWords = new HashSet<String>();
    //package with name
    //public static Map<String, String> sPackageMap = new HashMap<String, String>();
    public static List<Entry> sPackageMap = new ArrayList<Entry>();
    
    public static List<String> sCurrentPackageList = new ArrayList<String>();

    public static class Entry {

        private final String packageName;
        private final String appName;
        
        public Entry(String pName, String aName) {
            packageName = pName;
            appName = aName;
        }

    }
    
    public static void getPackageList(Context context) {
        sPackageMap.clear();
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> pkgAppsList = context.getPackageManager().queryIntentActivities(
                mainIntent, 0);
        for (ResolveInfo packageInfo : pkgAppsList) {
            Log.d(TAG, "Installed package :" + packageInfo.activityInfo.packageName);
            try {
                Resources packageRes = context.getPackageManager().getResourcesForApplication(
                        packageInfo.activityInfo.packageName);
                Log.d(TAG, "Installed package labelRes : " + packageInfo.activityInfo.labelRes);
                if (packageInfo.activityInfo.labelRes != 0) {
                    String resName = packageRes.getString(packageInfo.activityInfo.labelRes);
                    Log.d(TAG, "Installed package resName :" + resName);
                    parseKeyword(resName);
                    sKeyWords.add(resName);
                    sPackageMap.add(new Entry(packageInfo.activityInfo.packageName, resName));
                }

            } catch (NameNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Log.d(TAG, "getIconResource : " + packageInfo.activityInfo.getIconResource());
            Log.d(TAG,
                    "Launch Activity :" +
                            context.getPackageManager().getLaunchIntentForPackage(
                                    packageInfo.activityInfo.packageName));
        }
        
        for(String str:sKeyWords) {
            Log.d(TAG, "Key words is " + str);
        }
    }


    public static void parseKeyword(String str) {
        
        String[] splited = str.split("\\s");
 
        for (String s: splited) {
            Log.d(TAG, "spring is " + s);
            Pattern p=Pattern.compile("[a-zA-Z]*");
            Matcher m = p.matcher(s);
            
            if(m.matches()){
                sKeyWords.add(s);
            }else{
                Log.d(TAG, s + " is not name");
            }
        }
    }
    
    public static void findTargetPackages(String keyword) {
        sCurrentPackageList.clear();
        for (Entry e : sPackageMap){
            String name = e.appName;
            if (name.contains(keyword)) {
                sCurrentPackageList.add(e.packageName);
                Log.d(TAG, "list packagename is" + e.packageName);
            }
        }
    }
    
    public static boolean isPackageName (String keyWord) {
        return sKeyWords.contains(keyWord);
    }
}
