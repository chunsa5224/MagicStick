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
    //private static String appKey ="l7xx9ed3bc26b00f404b816bb3b6e2f44ec9";
    private TMapGpsManager tMapGps = null;
    private TextToSpeech tts;
    private int currentIndex;
    final String TAG = getClass().getName();
    String [] coordinates;
    String[] navigation;
    double prev_lat;
    double prev_long;
    double curr_lat;
    double curr_long;
    boolean isStopSelf=false;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        if(coordinates==null || isStopSelf){
            Log.d(TAG, "Stop the Service");
            if(tts != null) {
                tts.stop();
                tts.shutdown();
                Log.d(TAG, "TTS Destroyed");
            }
            super.onDestroy();
        }

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
        currentIndex=1;



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Background 실행~");
        navigation = intent.getExtras().getStringArray("navigation");
        coordinates= intent.getExtras().getStringArray("coordinates");
        Log.d(TAG, "onStartCommand");
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onLocationChange(Location location) {
        Log.d(TAG, "Location Changed! ");

        String peek = coordinates[currentIndex];
        String prev = coordinates[currentIndex-1];
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        curr_long= Double.parseDouble(peek.split(",")[0]);
        curr_lat=Double.parseDouble(peek.split(",")[1]);
        prev_long=Double.parseDouble(prev.split(",")[0]);
        prev_lat=Double.parseDouble(prev.split(",")[1]);
        Log.d(TAG, "current : " + curr_lat + "," +curr_long);

        double dist = GpsToMeter(latitude,longitude,curr_lat,curr_long);
        Log.d(TAG, "기준 좌표: "+curr_long +", " + curr_lat + "현위치 :" +longitude  + ", " + latitude + " 거리 :" + dist);

        if(dist<=5){
            Log.d(TAG, "Current navigation : " + navigation[currentIndex]);
            speech(navigation[currentIndex]);
            prev_lat = curr_lat;
            prev_long = curr_long;
            currentIndex++;

        }else if(wrongRoute(latitude,longitude)){
            Log.d(TAG, "wrong route!");
            speech("경로를 벗어났습니다.");
            isStopSelf=true;
            stopSelf();
        }else{
            Log.d(TAG," Go to " + curr_lat + " , " + curr_long);

        }
    }
    boolean wrongRoute(double latitude,double longitude){

        double a = GpsToMeter(latitude,longitude,prev_lat,prev_long);
        double b = GpsToMeter(latitude,longitude,curr_lat,curr_long);
        double c = GpsToMeter(prev_lat,prev_long,curr_lat, curr_long);
        Log.d("", "meter : " + a +", "+ b + ", "+ c);
        double result = Math.sqrt(a*a - ( Math.pow(a*a-b*b+c*c,2) / (4*c*c) ) );
        Log.d("Wrong Route?", result+"");
        if(result>=40) return true;
        else return false;
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
