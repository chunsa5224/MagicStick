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
    public static int currentIndex;
    final String TAG = getClass().getName();
    public static LinkedList<String> coordinates = new LinkedList<String>();
    public static LinkedList<String> navigation = new LinkedList<String>();
    static double prev_lat;
    static double prev_long;
    double curr_lat;
    double curr_long;
    static boolean isStopped=false;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Background 실행~");
        isStopped=false;
        //현위치 좌표계 받아오기
        tMapGps = new TMapGpsManager(this);
        tMapGps.setMinTime(1000);
        //실외 - 실제 코드에 사용
        tMapGps.setProvider(tMapGps.GPS_PROVIDER);
        //실내 - 디버깅용
        tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);
        tMapGps.OpenGps();

        //currentIndex=1;
        tts = new TextToSpeech(this, this);
        navigation = NavigationActivity.navigation;
        coordinates = NavigationActivity.coordinates;
        prev_long = NavigationActivity.prev_long;
        prev_lat=NavigationActivity.prev_lat;
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
            //isStopped=true;
            onDestroy();
        }else{
            speech("서비스 실행중");
            Log.d(TAG," Go to " + curr_lat + " , " + curr_long);

        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Stop the Service");
        while(tts.isSpeaking())
        tts.stop();
        tts.shutdown();
        Log.d(TAG, "TTS Destroyed");

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
        tts.speak(text, TextToSpeech.QUEUE_FLUSH,null);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TTS 연결 성공");
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
