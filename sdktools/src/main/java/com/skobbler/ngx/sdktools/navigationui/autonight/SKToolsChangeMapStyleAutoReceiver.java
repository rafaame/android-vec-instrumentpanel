/*
 * Copyright (c) 2013 SKOBBLER SRL.
 * Cuza Voda 1, Cluj-Napoca, Cluj, 400107, Romania
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of SKOBBLER SRL 
 * ("Confidential Information"). You shall not disclose such Confidential 
 * Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with SKOBBLER SRL.
 * 
 * Created on May 2, 2013 by Filip Tudic
 */
package com.skobbler.ngx.sdktools.navigationui.autonight;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.skobbler.ngx.sdktools.navigationui.SKToolsLogicManager;


/**
 * Defines a BroadcastReceiver that listens for the alarm manager that sends the
 * broadcast at sunrise/sunset hours in order to change the map styles (day/night)
 */
public class SKToolsChangeMapStyleAutoReceiver extends BroadcastReceiver {

    private static final String TAG = "ChangeMapStyleAutoReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "Received Broadcast from alarm manager to change the map style");
        if (!SKToolsLogicManager.getInstance().isNavigationStopped()) {
            SKToolsLogicManager.getInstance().computeMapStyle(SKToolsDateUtils.isDaytime());
        }
    }
}
