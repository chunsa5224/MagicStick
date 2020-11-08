package com.example.magicstick;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import androidx.annotation.Nullable;

public class SettingPreferenceFragment extends PreferenceFragment {

    SharedPreferences prefs;
    MultiSelectListPreference objectPreference;
    PreferenceScreen objectScreen;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_preference);
        objectPreference =(MultiSelectListPreference)findPreference("object_list1");
        objectScreen = (PreferenceScreen) findPreference("object_screen");
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        prefs.registerOnSharedPreferenceChangeListener(prefListener);

    }


    SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        }
    };


}
