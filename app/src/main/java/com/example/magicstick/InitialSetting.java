package com.example.magicstick;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


public class InitialSetting extends AppCompatActivity {

    private final String TAG = getClass().getName();
    static int i=0;
    static String [] objectArray = {"vehicle", "person", "traffic light", "stop", "pole", "big obstacle", "small obstacle", "carrier", "chair"};
    SpeechRecognizer mRecognizer;
    Intent intent;
    EditText editText;
    private Runnable runnable;
    TextToSpeech tts;
    boolean permissionFlag= false;
    View view;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if(status== TextToSpeech.SUCCESS){
                tts.setLanguage(Locale.KOREA);
                Log.d(TAG, "initial setting completed");
                tts.speak("도움말입니다.긴급전화를 설정하겠습니다. 연락처에 저장된 보호자의 이름을 말해주세요.",TextToSpeech.QUEUE_FLUSH,null);
                /*thread.start();
                try{
                    thread.join();
                }catch (Exception e){

                }*/
                while(tts.isSpeaking()){}
                Log.d(TAG, "start STT");
                SpeechToText();
            }
        });

        setContentView(R.layout.activity_customize_objectlist);
        /*textView = findViewById(R.id.textView);*/
        view = findViewById(R.id.imageView3);

        setInitialStart();
        /*while(true){
            if(setInitialStart()){
                speech("도움말입니다.긴급전화를 설정하겠습니다. 연락처에 저장된 보호자의 이름을 말해주세요.");
                break;
            }
        }*/
        /*objectArray = getResources().getStringArray(R.array.array_object);*/




    }


    public class EmergencySetting extends Thread{
        @Override
        public void run() {

            Log.d("Thread :" ,"TTS start");
            tts.speak("도움말입니다.긴급전화를 설정하겠습니다. 연락처에 저장된 보호자의 이름을 말해주세요.",TextToSpeech.QUEUE_FLUSH,null);


        }
    }

    public void setEmergencyCall(String search){
        tts.speak(search +" 를 긴급 전화로 설정하시겠습니까?",TextToSpeech.QUEUE_FLUSH,null);
        /*speech(search +" 를 긴급 전화로 설정하시겠습니까?");*/

        view.setOnTouchListener(new OnSwipeTouchListener(this){
            @Override
            public void onSwipeTop() {
                //yes
                mRecognizer.destroy();
                String result = getContact(getApplicationContext(), search);
                Log.d(TAG,"전화번호 : "+ result);
                tts.speak("다음으로 위험물체 설정을 하겠습니다.",TextToSpeech.QUEUE_FLUSH,null);
                while(tts.isSpeaking()){}
                tts.speak(objectArray[0],TextToSpeech.QUEUE_FLUSH,null);
                objectCustomize();
                return;
            }

            @Override
            public void onSwipeBottom() {
                /*speech("연락처에 저장된 보호자의 이름을 말해주세요.");*/
                tts.speak("연락처에 저장된 보호자의 이름을 말해주세요.",TextToSpeech.QUEUE_FLUSH,null);
                while(tts.isSpeaking()){}
                SpeechToText();
                return;
            }
        });

    }

    @Override
    public void onBackPressed() {
        tts.stop();
        tts.shutdown();
        Log.d("initial Setting", "on back pressed");
        if(mRecognizer!=null){
            mRecognizer.destroy();
        }
        super.onBackPressed();
    }

    public String getContact(Context context, String search){
        Cursor cursor;
        String result="";
        try{
            Uri uContactsUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String strProjection  = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;

            cursor = getContentResolver().query(uContactsUri, null, null,
                    null, strProjection);
            cursor.moveToFirst();

            String name = "";
            String number = "";
            int nameColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int numberTypeColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);

            while(!cursor.isAfterLast() && result.equals("")){
                name = cursor.getString(nameColumn);
                number = cursor.getString(numberColumn);
                int numberType = Integer.valueOf(cursor.getString(numberTypeColumn));

                if(name.equals(search)){
                    result = number;
                }
                name = "";
                number= "";
                cursor.moveToNext();
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public void objectCustomize(){

        int vehicle = R.array.vehicle;
        int person= R.array.person;
        int traffic_light= R.array.traffic_light;
        int stop= R.array.stop;
        int pole= R.array.pole;
        int big_obstacle= R.array.big_obstacle;
        int small_obstacle= R.array.small_obstacle;
        int carrier= R.array.carrier;
        int chair= R.array.chair;

        int [] idx = {vehicle, person, traffic_light, stop, pole,big_obstacle,small_obstacle,carrier,chair};



        view.setOnTouchListener(new OnSwipeTouchListener(this){

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();

            Set<String> currArray = new HashSet<>();


            //Yes
            public void onSwipeTop(){
                if(i<objectArray.length){
                    /*currArray.add(objectArray[i]);*/
                    String [] arr = getResources().getStringArray(idx[i]);

                    for(String s : arr){
                        currArray.add(s);
                    }
                    i++;
                    if (i < objectArray.length) {
                        tts.speak(objectArray[i],TextToSpeech.QUEUE_FLUSH,null);
                    }else {
                        editor.putStringSet("object_list1", currArray);
                        editor.commit();
                        Log.d("", "stop");
                        /*Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);*/
                        onBackPressed();
                    }
                }
                return;
            }

            //No
            public void onSwipeBottom(){
                if(i<objectArray.length){
                    i++;
                    if (i < objectArray.length) {
                        tts.speak(objectArray[i],TextToSpeech.QUEUE_FLUSH,null);

                    }else{
                        editor.putStringSet("object_list1", currArray);
                        editor.commit();
                        Log.d("", "stop");
                        onBackPressed();
                    }
                }
                return;
            }

        });

    }

    public boolean setInitialStart(){
        //Permission Request
        if(Build.VERSION.SDK_INT>=23){
            if((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_NETWORK_STATE)!=PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS))!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS,Manifest.permission.RECORD_AUDIO},1);
            }else {
                Log.d("InitialSetting", "Permissions are granted");
                return true;
            }
        }else{
            Toast.makeText(this, "이 앱이 마이크와 위치에 접근하도록 허용합니다.",Toast.LENGTH_SHORT);
            Log.d("InitialSetting", "Permissions are granted");
            return true;
        }
        return true;
    }

    public void SpeechToText() {
        //음성인식
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        mRecognizer.setRecognitionListener(listener);
        mRecognizer.startListening(intent);
        /*while(tts.isSpeaking()){}*/
        runnable = () -> mRecognizer.startListening(intent);
    }

    private RecognitionListener listener = new RecognitionListener() {

        @Override
        public void onReadyForSpeech(Bundle params){
            toast("말하세요");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech");
        }

        @Override
        public void onError(int error) {

            String message;
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    mRecognizer.stopListening();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }
            Log.d(TAG, "error " + message);

            if(error!=SpeechRecognizer.ERROR_RECOGNIZER_BUSY && error != SpeechRecognizer.ERROR_CLIENT){
                mRecognizer.startListening(intent);
            }else{
                toast("error");
            }
        }

        @Override
        public void onResults(Bundle results) {

            mRecognizer.stopListening();

            String key = SpeechRecognizer.RESULTS_RECOGNITION;
            ArrayList<String> mResult = results.getStringArrayList(key);
            String [] rs = new String[mResult.size()];
            mResult.toArray(rs);


            Log.d(TAG, "rs[0] : "+rs[0]);
            String search = rs[0];
            setEmergencyCall(search);

            mRecognizer.destroy();

        }

        @Override
        public void onPartialResults(Bundle partialResults){
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }

    };

    private void toast(String msg){
        Toast.makeText(this ,msg, Toast.LENGTH_LONG).show();
    }


}
