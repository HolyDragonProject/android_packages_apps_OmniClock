package org.omnirom.deskclock;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.omnirom.deskclock.alarms.AlarmConstants;
import org.omnirom.deskclock.alarms.AlarmService;
import org.omnirom.deskclock.alarms.AlarmStateManager;
import org.omnirom.deskclock.provider.AlarmInstance;

/**
 * Created by maxl on 9/1/16.
 */
public class AlarmPluginFactory {

    public static Intent getAlarmServiceIntent(Context context, AlarmInstance instance, boolean preAlarm) {
        Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        return intent;
    }
}
