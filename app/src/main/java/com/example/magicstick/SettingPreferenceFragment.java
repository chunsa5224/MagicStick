package com.example.magicstick;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import androidx.annotation.Nullable;

public class SettingPreferenceFragment extends PreferenceFragment {
    SharedPreferences prefs;

    ListPreference objectPreference;
    PreferenceScreen objectScreen;

    @Override
    public void onCreate(@Nullable Bundle savedInstaceState){
        super.onCreate(savedInstaceState);

        addPreferencesFromResource(R.xml.settings_preference);
        objectPreference =(ListPreference)findPreference("object_list");
        objectScreen = (PreferenceScreen) findPreference("object_screen");
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());



    }
    SharedPreferences.OnSharedPreferenceChangeListener prefListner = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        }
    };
}
