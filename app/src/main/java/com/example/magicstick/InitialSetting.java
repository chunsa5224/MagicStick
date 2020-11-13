package com.example.magicstick;


import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class InitialSetting extends AppCompatActivity {

    static int i=0;
    static String [] objectArray;

    TextView textView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_customize_objectlist);
        textView = findViewById(R.id.textView2);
        setInitialStart();
        TextToSpeech tts = new android.speech.tts.TextToSpeech(getApplicationContext(), new android.speech.tts.TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });

        tts.setLanguage(Locale.KOREAN);
        tts.setPitch(1.0f);
        tts.setSpeechRate(1.0f);
        tts.speak("지금부터 장애물 설정을 하겠습니다.",TextToSpeech.QUEUE_FLUSH,null);

        objectArray = getResources().getStringArray(R.array.array_object);
        textView.setText(objectArray[0]);
        tts.speak(objectArray[0],TextToSpeech.QUEUE_FLUSH,null);

        View view = findViewById(R.id.background_view);

        view.setOnTouchListener(new OnSwipeTouchListener(this){

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();

            Set<String> currArray = new HashSet<>();

            //Yes
            public void onSwipeTop(){
                if(i<objectArray.length){
                    currArray.add(objectArray[i]);
                    i++;
                    if (i < objectArray.length) {
                        textView.setText(objectArray[i]);
                        tts.speak(objectArray[i],TextToSpeech.QUEUE_FLUSH,null);
                    }else {
                        editor.putStringSet("object_list1", currArray);
                        editor.commit();
                        Log.d("", "stop");
                        finish();

                    }
                }
            }

            //No
            public void onSwipeBottom(){
                if(i<objectArray.length){
                    i++;
                    if (i < objectArray.length) {
                        textView.setText(objectArray[i]);
                        tts.speak(objectArray[i],TextToSpeech.QUEUE_FLUSH,null);
                    }else{
                        editor.putStringSet("object_list1", currArray);
                        editor.commit();
                        Log.d("", "stop");
                        finish();
                    }
                }
            }

        });

    }

    public void setInitialStart(){
        //Permission Request
        if(Build.VERSION.SDK_INT>=23){
            if((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_NETWORK_STATE)!=PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED)){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CALL_PHONE},1);
            }else {
                Log.d("InitialSetting", "Permissions are granted");
            }
        }else{
            Toast.makeText(this, "이 앱이 마이크와 위치에 접근하도록 허용합니다.",Toast.LENGTH_SHORT);
            Log.d("InitialSetting", "Permissions are granted");
        }
    }

}
