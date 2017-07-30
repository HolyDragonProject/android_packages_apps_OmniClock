/*
 *  Copyright (C) 2016 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.omnirom.alarmclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;

import org.omnirom.deskclock.Utils;

public class CustomAnalogAppWidgetConfigure extends PreferenceActivity {

    public static final String KEY_SHOW_ALARM = "show_alarm";
    public static final String KEY_SHOW_DATE = "show_date";

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(Utils.getThemeResourceId(this));
        getListView().setBackgroundColor(Utils.getViewBackgroundColor(this));

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID,
        // finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        addPreferencesFromResource(org.omnirom.deskclock.R.xml.custom_analog_appwidget_configure);
        initPreference(KEY_SHOW_ALARM);
        initPreference(KEY_SHOW_DATE);
    }

    public void handleOkClick(View v) {
        CustomAnalogAppWidgetProvider.updateAfterConfigure(this, mAppWidgetId);
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private void initPreference(String key) {
        CheckBoxPreference b = (CheckBoxPreference) findPreference(key);
        b.setKey(key + "_" + String.valueOf(mAppWidgetId));
        b.setDefaultValue(true);
        b.setChecked(true);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(b.getKey(), true).commit();
    }

    public static void clearPrefs(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(KEY_SHOW_ALARM + "_" + id).commit();
        prefs.edit().remove(KEY_SHOW_DATE + "_" + id).commit();
    }

    public static void remapPrefs(Context context, int oldId, int newId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean oldValue = prefs.getBoolean(KEY_SHOW_ALARM + "_" + oldId, false);
        prefs.edit().putBoolean(KEY_SHOW_ALARM + "_" + newId, oldValue).commit();
        oldValue = prefs.getBoolean(KEY_SHOW_DATE + "_" + oldId, false);
        prefs.edit().putBoolean(KEY_SHOW_DATE + "_" + newId, oldValue).commit();

        prefs.edit().remove(KEY_SHOW_ALARM + "_" + oldId).commit();
        prefs.edit().remove(KEY_SHOW_DATE + "_" + oldId).commit();
    }
}
