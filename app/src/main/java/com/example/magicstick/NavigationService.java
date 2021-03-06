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
    final String TAG = getClass().getName();
    private TMapGpsManager tMapGps = null;
    private TextToSpeech tts;
    private Thread mThread;

    static LinkedList<String> coordinates = new LinkedList<String>();
    static LinkedList<String> navigation = new LinkedList<String>();
    static Set<String> objectList;
    static double prev_lat;
    static double prev_long;
    double curr_lat;
    double curr_long;
    boolean wrong;

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
        /*Log.d(TAG, "onStartCommand");
        tMapGps = new TMapGpsManager(this);
        tMapGps.setMinTime(1000);
        tMapGps.setMinDistance(2);

        *//*tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);*//*
        tMapGps.setProvider(tMapGps.GPS_PROVIDER);
*//*
        if(tMapGps.getLocation()==new TMapPoint(0,0)){
            tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);
        }
*//*
        tMapGps.OpenGps();
        wrong = false;
        tts = new TextToSpeech(getApplicationContext(), this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        objectList = sharedPreferences.getStringSet("object_list1",null);

        navigation = NavigationActivity.navigation;
        coordinates = NavigationActivity.coordinates;

        *//*navigation = (LinkedList<String>) intent.getSerializableExtra("Navigation");
        coordinates = (LinkedList<String>) intent.getSerializableExtra("Coordinate");*//*
        prev_long = Double.parseDouble(coordinates.peek().split(",")[0]);
        prev_lat=Double.parseDouble(coordinates.peek().split(",")[1]);

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    while(tts.isSpeaking()){
                        //Log.d(TAG, "TTS is speaking ");
                    }
                    objectDetect();
                }
            }
        });
        mThread.start();*/
        return START_NOT_STICKY;
    }

    @Override
    public void onLocationChange(Location location) {

        String peek = coordinates.peek();
        Log.d(TAG, "Location Changed! " + peek);
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        curr_long= Double.parseDouble(peek.split(",")[0]);
        curr_lat=Double.parseDouble(peek.split(",")[1]);
        Log.d(TAG, "current : " +curr_long + "," + curr_lat );

        double dist = GpsToMeter(latitude,longitude,curr_lat,curr_long);
        Log.d(TAG, "기준 좌표: "+curr_long +", " + curr_lat + " 현위치 :" +longitude  + ", " + latitude + " 거리 :" + dist);

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
            // navigationActivity 를 실행해야될듯
            /*Intent intent = new Intent(getApplicationContext(), NavigationActivity.class);
            intent.putExtra("destination", destination);
            intent.putExtra("d_latitude", latitude);
            intent.putExtra("d_longitude", longitude);
            startActivity(intent);*/
            stopSelf();
        }else{
            speech("서비스 실행중");
            Log.d(TAG," Go to " + curr_lat + " , " + curr_long);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy flag " + NavigationActivity.stopFlag);
        tts.stop();
        tts.shutdown();
        super.onDestroy();
        Log.d(TAG, "Stop the Service");

        if(wrong || navigation.isEmpty() || NavigationActivity.stopFlag){
            Log.d(TAG, "TTS Destroyed");
        }
    }

    public void objectDetect(){
        String object = SerialService.object;

        if(object!=null){
            object = object.replaceAll("\n","");
            Log.d(TAG,"detect ! " + object);
            String [] detect = object.split(" ");
            for(int i=0; i<detect.length; i++){
                detect[i] = detect[i].replaceAll("_", " ");
            }
            String speak = "";
            if(detect.length>0){
                for(String s: detect){
                    if(objectList.contains(s)){
                        speak += s;
                    }
                }
                if(speak!=null){
                    speech("Watch out for "+speak);
                    Log.d(TAG, "Watch out for "+speak);
                }
                SerialService.object=null;
        }
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
