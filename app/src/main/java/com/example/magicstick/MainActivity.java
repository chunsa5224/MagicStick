package com.example.magicstick;

import android.Manifest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;

import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;



public class MainActivity extends AppCompatActivity {

    Intent intent;
    SpeechRecognizer mRecognizer;
    final String TAG = getClass().getName();
    EditText editText;
    TextToSpeech tts;
    Handler handler = new Handler();
    Runnable runnable;
    boolean ttsFlag = true;
    boolean bluetoothFlag = false;
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText)findViewById(R.id.editText);
        //final TMapData tMapData = new TMapData();

        //Setting 값 들고오는 부분

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

        if(mBluetoothAdapter==null){
            //블루투스 안되는 기종
        }else{
            if(!mBluetoothAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);



            }
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

        ImageButton voiceBtn = findViewById(R.id.search_voice_btn);
        voiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //임시
                Intent intent1 = new Intent(getApplicationContext(), NavigationActivity.class);
                intent1.putExtra("destination", "공덕역 6번출구");
                startActivity(intent1);

                try {
                    //inputVoice();
                } catch(SecurityException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ttsFlag=sharedPreferences.getBoolean("voice_notification",false);
        bluetoothFlag = sharedPreferences.getBoolean("bluetooth",false);
        Log.d(TAG, "ttsFlag is " + ttsFlag);
    }

    public void inputVoice(){
        //음성인식
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,5000);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        mRecognizer.setRecognitionListener(listener);
        mRecognizer.startListening(intent);
        runnable= new Runnable() {
            @Override
            public void run() {
                mRecognizer.startListening(intent);
            }
        };
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

        boolean doubleResult =true;
        int STT_RESULT =0;

        @Override
        public void onReadyForSpeech(Bundle params){
            toast("말하세요");
        }

        @Override
        public void onBeginningOfSpeech() {
            doubleResult =false;
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
            toast("error");

            if(error!=SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
                mRecognizer.startListening(intent);
            }
        }

        @Override
        public void onResults(Bundle results) {

            if(!doubleResult){

                Log.d(TAG, "STT_Result = " + STT_RESULT);

                String key = SpeechRecognizer.RESULTS_RECOGNITION;
                ArrayList<String> mResult = results.getStringArrayList(key);
                String [] rs = new String[mResult.size()];
                mResult.toArray(rs);
                String speak = "";
                doubleResult=true;

                Log.d(TAG, "rs[0] : "+rs[0]);

                if(STT_RESULT==0){
                    editText.setText(rs[0]);
                    mRecognizer.stopListening();
                    STT_RESULT=1;
                    speak = rs[0] + " 를 목적지로 정하시겠어요?";

                    Log.d(TAG, "목적지 : " + rs[0]);
                    tts.speak(speak,TextToSpeech.QUEUE_FLUSH,null);

                    new Waiter().execute();

                }else{
                    Log.d(TAG, rs[0]);

                    if(rs[0].equals("네")){

                        Log.d(TAG, "Navigation activity will start");
                        STT_RESULT=0;

                        mRecognizer.stopListening();
                        mRecognizer.cancel();
                        mRecognizer.destroy();

                        Intent intent1 = new Intent(getApplicationContext(), NavigationActivity.class);
                        intent1.putExtra("destination", editText.getText().toString());
                        startActivity(intent1);

                    }else{
                        Log.d(TAG, "Ask again");
                        STT_RESULT=0;
                        editText.setText("");

                        tts.speak("목적지를 다시 말해주세요",TextToSpeech.QUEUE_FLUSH,null);

                        new Waiter().execute();
                    }

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


    class Waiter extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {

            while(tts.isSpeaking()){
                try{Thread.sleep(1000); Log.d(TAG, "tts is speaking now...");}catch (Exception e){}
            }

            Log.d(TAG, "tts is done.");
            handler.post(runnable);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid){
            super.onPostExecute(aVoid);
        }
    }
}
