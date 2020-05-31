package com.example.magicstick;

import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import static java.lang.Thread.sleep;


public class MainActivity extends AppCompatActivity {

    Intent intent;
    SpeechRecognizer mRecognizer;
    final String TAG = getClass().getName();
    EditText editText;
    int STT_RESULT =0;
    TextToSpeech tts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText)findViewById(R.id.editText);
        //final TMapData tMapData = new TMapData();


        //권한 요청
        if(Build.VERSION.SDK_INT>=23){
            if((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_NETWORK_STATE)!=PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED)){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE},1);
            }else {
                Log.d(TAG, "Permissions are granted");
            }
        }else{
            Toast.makeText(this, "이 앱이 마이크와 위치에 접근하도록 허용합니다.",Toast.LENGTH_SHORT);
            Log.d(TAG, "Permissions are granted");
        }

        //음성출력
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status!= TextToSpeech.ERROR){
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });
        tts.setPitch(1.0f);
        tts.setSpeechRate(1.0f);


        //음성인식
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        mRecognizer.setRecognitionListener(listener);


        ImageButton voiceBtn = findViewById(R.id.search_voice_btn);
        voiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast("음성인식");
                try {
                    mRecognizer.startListening(intent);
                } catch(SecurityException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu) ;
        return true ;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grandResults){
        super.onRequestPermissionsResult(requestCode,permissions, grandResults);
        if(Build.VERSION.SDK_INT>=23){
            if(grandResults[0]==PackageManager.PERMISSION_GRANTED){
                Log.v(TAG, "Permission: " + permissions[0] + " was " + grandResults[0]);
            }
        }
    }

    public void searchLocation(final EditText editText) throws ParserConfigurationException, SAXException, IOException {

        TMapData tMapData = new TMapData();
        ArrayList poiItems = tMapData.findAllPOI("SKT타워");
        Log.d("poiItems : ", poiItems.toString());
        for(int i=0; i<poiItems.size(); i++){
            TMapPOIItem item = (TMapPOIItem) poiItems.get(i);
            Log.d("POI Name : " , item.getPOIName().toString() + ", " + "Address : " + item.getPOIAddress().replace("null", "")+", Point" + item.getPOIPoint().toString());
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();


        if (id == R.id.setting_btn) {
            Log.d(this.getClass().getName(), "onOptionsItemSelected 실행");
            Intent intentSubActivity = new Intent(this, SettingActivity.class);
            startActivity(intentSubActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //음성인식
    private RecognitionListener listener = new RecognitionListener() {

        @Override
        public void onReadyForSpeech(Bundle params){
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            if(SpeechRecognizer.ERROR_RECOGNIZER_BUSY==8){
                Log.d(TAG, "ERROR_RECOGNIZER_BUSY");
                mRecognizer.stopListening();
            }
            Log.d(TAG, "error " + error);
            toast("error");
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "STT_Result = " + STT_RESULT);
            String key = SpeechRecognizer.RESULTS_RECOGNITION;
            ArrayList<String> mResult = results.getStringArrayList(key);
            String [] rs = new String[mResult.size()];
            mResult.toArray(rs);
            String speak = rs[0];

            if(STT_RESULT==0){
                STT_RESULT=1;
                Log.d(TAG, "목적지 : " + rs[0]);
                speak = rs[0] + " 를 목적지로 정하시겠어요?";
                tts.speak(speak,TextToSpeech.QUEUE_FLUSH,null);
                editText.setText(rs[0]);
                while(tts.isSpeaking()){
                    //Log.d(TAG, "wait for speaking");
                    mRecognizer.stopListening();
                }
                //mRecognizer.stopListening();
                mRecognizer.startListening(intent);
            }else{
                toast("대답");
                if(rs[0].equals("네")){
                    Log.d(TAG, "get 네");
                    tts.shutdown();
                    mRecognizer.stopListening();
                    mRecognizer.destroy();
                    Intent intent = new Intent(getApplicationContext(), NavigationActivity.class);
                    intent.putExtra("destination", editText.getText().toString());
                    startActivity(intent);
                }else{
                    Log.d(TAG, "REASK");
                    STT_RESULT=0;
                    editText.setText("");
                    toast("목적지 말해요");
                    //mRecognizer.stopListening();
                    mRecognizer.startListening(intent);
                }

            }

        }

        @Override
        public void onPartialResults(Bundle partialResults){
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }

    };


    private void toast(String msg){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show();
    }

}
