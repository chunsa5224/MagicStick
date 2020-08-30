package com.example.magicstick;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.skt.Tmap.TMapGpsManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

import static com.example.magicstick.NavigationActivity.GpsToMeter;

public class NavigationService extends Service implements TMapGpsManager.onLocationChangedCallback, TextToSpeech.OnInitListener {
    private static String appKey ="l7xx9ed3bc26b00f404b816bb3b6e2f44ec9";
    private TMapGpsManager tMapGps = null;
    private TextToSpeech tts;
    private int index;
    final String TAG = getClass().getName();
    String [] coordinates;
    String[] navigation;
    double prev_lat;
    double prev_long;

    public NavigationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //현위치 좌표계 받아오기
        Log.d(TAG, "Background 실행~");
        tMapGps = new TMapGpsManager(this);
        tMapGps.setMinTime(1000);
        tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);
        tMapGps.OpenGps();
        tts = new TextToSpeech(this, this);
        index=0;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Background 실행~");
        navigation = intent.getExtras().getStringArray("navigation");
        coordinates= intent.getExtras().getStringArray("coordinates");
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onLocationChange(Location location) {
        Log.d(TAG, "Location Changed! ");
        if(coordinates==null || navigation==null){
            Log.d(TAG, "Coordinates and Navigation are null");

        }else {
            String peek = coordinates[index];
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            double p_longitude = Double.parseDouble(peek.split(",")[0]);
            double p_latitude = Double.parseDouble(peek.split(",")[1]);

            Log.d(TAG, "Current GPS : " + longitude + ", " + latitude);
            double dist = GpsToMeter(latitude,longitude,p_latitude,p_longitude);
            if(dist<=5){
                Log.d(TAG, "Current navigation : " + navigation[index]);
                speech(navigation[index]);
                prev_lat = p_latitude;
                prev_long = p_longitude;
                index++;

            }else if(dist>=40){
                Log.d(TAG, "wrong route!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                speech("경로를 벗어났습니다.");
                onDestroy();

            }else{
                Log.d(TAG," Go to " + p_longitude + " , " + p_latitude);
            }

        }

    }
    public void speech(String text){

        tts.setLanguage(Locale.KOREAN);
        tts.setPitch(1.0f);
        tts.setSpeechRate(1.0f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH,null);

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // 작업 성공
            int language = tts.setLanguage(Locale.KOREAN);  // 언어 설정
            if (language == TextToSpeech.LANG_MISSING_DATA
                    || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 언어 데이터가 없거나, 지원하지 않는경우
                //toast("지원하지 않는 언어입니다.");
                Log.d(TAG,"지원하지 않는 언어입니다.");
            }
        } else {
            //toast("speech fail! ");
            Log.d(TAG,"Speech Fail");
        }
    }
}
