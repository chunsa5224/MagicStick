package com.example.magicstick;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import androidx.annotation.Nullable;

public class SettingPreferenceFragment extends PreferenceFragment {
    SharedPreferences prefs;

    ListPreference objectPreference;
    PreferenceScreen objectScreen;
    BluetoothAdapter mBluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_preference);
        objectPreference =(ListPreference)findPreference("object_list");
        objectScreen = (PreferenceScreen) findPreference("object_screen");
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        prefs.registerOnSharedPreferenceChangeListener(prefListener);



    }


    SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals("bluetooth")){
                if(prefs.getBoolean("bluetooth",true)){
                    Log.d(getClass().getName(), "Bluetooth on");
                    mBluetoothAdapter.enable();
                    Intent intent = new Intent(bluetooth_main.BLUETOOTH_SERVICE);
                    startActivityForResult(intent,0);


                }else{
                    Log.d(getClass().getName(), "Bluetooth off");
                    mBluetoothAdapter.disable();
                }




            }
        }
    };


}
