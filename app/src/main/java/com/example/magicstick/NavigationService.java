package com.example.magicstick;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.skt.Tmap.TMapGpsManager;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;


import static com.example.magicstick.NavigationActivity.GpsToMeter;

public class NavigationService extends Service implements TMapGpsManager.onLocationChangedCallback, TextToSpeech.OnInitListener{
    //private static String appKey ="l7xx9ed3bc26b00f404b816bb3b6e2f44ec9";
    private TMapGpsManager tMapGps = null;
    private TextToSpeech tts;
    final String TAG = getClass().getName();
    public static LinkedList<String> coordinates = new LinkedList<String>();
    public static LinkedList<String> navigation = new LinkedList<String>();
    static double prev_lat;
    static double prev_long;
    double curr_lat;
    double curr_long;
    boolean wrong=false;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }


    @Override
    public void onCreate() {
        tts = new TextToSpeech(getApplicationContext(), this);
        //현위치 좌표계 받아오기
        tMapGps = new TMapGpsManager(this);
        tMapGps.setMinTime(1000);
        //실외 - 실제 코드에 사용
        tMapGps.setProvider(tMapGps.GPS_PROVIDER);
        /*//실내 - 디버깅용
        tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);*/
        tMapGps.OpenGps();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Background 실행~");

        navigation = NavigationActivity.navigation;
        coordinates = NavigationActivity.coordinates;
        prev_long = Double.parseDouble(coordinates.peek().split(",")[0]);
        prev_lat=Double.parseDouble(coordinates.peek().split(",")[1]);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    while(tts.isSpeaking()){
                        //Log.d(TAG, "TTS is speaking ");
                    }
                    objectDetect();
                }
            }
        });
        thread.start();

        Log.d(TAG, "onStartCommand");
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onLocationChange(Location location) {

        String peek = coordinates.peek();
        Log.d(TAG, "Location Changed! " + peek);
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        curr_long= Double.parseDouble(peek.split(",")[0]);
        curr_lat=Double.parseDouble(peek.split(",")[1]);
        Log.d(TAG, "current : " + curr_lat + "," +curr_long);

        double dist = GpsToMeter(latitude,longitude,curr_lat,curr_long);
        Log.d(TAG, "기준 좌표: "+curr_long +", " + curr_lat + "현위치 :" +longitude  + ", " + latitude + " 거리 :" + dist);

        if(dist<=5){
            Log.d(TAG, "Current navigation : " + navigation.peek());
            speech(navigation.peek());
            prev_lat = curr_lat;
            prev_long = curr_long;
            navigation.poll();
            coordinates.poll();

       }else if(wrongRoute(latitude,longitude)){
            Log.d(TAG, "wrong route!");
            speech("경로를 벗어났습니다.");
            wrong =true;
            onDestroy();
        }else{
            speech("서비스 실행중");
            Log.d(TAG," Go to " + curr_lat + " , " + curr_long);

        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service on Destroy ");
        if(wrong || navigation.isEmpty() || NavigationActivity.stopFlag){
            super.onDestroy();
            Log.d(TAG, "Stop the Service");
            /*tts.stop();
            tts.shutdown();*/
            Log.d(TAG, "TTS Destroyed");
        }
    }

    public void objectDetect(){
        String object = SerialService.object;
        if(object!=null){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            Set<String> customList = sharedPreferences.getStringSet("object_list2", null);
            if(customList.contains(object)){
                speech(object + "가 전방에 있습니다.");
            }
                SerialService.object=null;
        }
    }


    boolean wrongRoute(double latitude,double longitude){

        double a = GpsToMeter(latitude,longitude,prev_lat,prev_long);
        double b = GpsToMeter(latitude,longitude,curr_lat,curr_long);
        double c = GpsToMeter(prev_lat,prev_long,curr_lat, curr_long);
        //Log.d("", "meter : " + a +", "+ b + ", "+ c);
        double result = Math.sqrt(a*a - ( Math.pow(a*a-b*b+c*c,2) / (4*c*c) ) );
        //Log.d("Wrong Route?", result+"");
        if(result>=40) return true;
        else return false;
    }
    public void speech(String text){
        tts.setLanguage(Locale.KOREAN);
        tts.setPitch(1.0f);
        tts.setSpeechRate(1.0f);
        while(tts.isSpeaking()){
            //Log.d(TAG, "TTS is speaking ");
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH,null);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TTS 연결 성공");
            speech(navigation.peek());
            navigation.poll();
            coordinates.poll();
            // 작업 성공
            int language = tts.setLanguage(Locale.KOREAN);  // 언어 설정
            if (language == TextToSpeech.LANG_MISSING_DATA
                    || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 언어 데이터가 없거나, 지원하지 않는경우
                Log.d(TAG,"지원하지 않는 언어입니다.");
            }
        } else {
            Log.d(TAG,"Speech Fail");
        }
    }

}
