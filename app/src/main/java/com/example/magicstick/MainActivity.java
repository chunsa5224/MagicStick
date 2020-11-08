package com.example.magicstick;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements ServiceConnection, SerialListener {

    private final String TAG = getClass().getName();
    private EditText editText;
    private TextToSpeech tts;
    private SpeechRecognizer mRecognizer;
    private Runnable runnable;
    private boolean ttsFlag = true;
    private boolean focusFlag = false;
    private boolean customizingFlag = false;
    Intent intent;

    //Bluetooth
    private int checknumber=0;
    private BluetoothDevice bluetoothDevice;
    private Set<BluetoothDevice> bluetoothDeviceSet;
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    private static boolean isTtsFlag = false;    //TTS 활성화 상태
    private String device_name = "rasp";
    private SerialService service;
    private enum Connected { False, Pending, True }
    private boolean initialStart = true;
    private Connected connected = Connected.False;
    private BluetoothAdapter bluetoothAdapter; //블루투스 사용 객체

    //T map
    private static String appKey ="l7xx9ed3bc26b00f404b816bb3b6e2f44ec9";
    private final TMapData tMapData = new TMapData();
    private TMapView tMapView =null;
    public TMapPoint endPoint;
    private Double latitude;
    private Double longitude;
    private String search;
    private String destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText)findViewById(R.id.editText);
        tMapView = new TMapView(getApplicationContext());
        tMapView.setSKTMapApiKey(appKey);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setInitialStart();

        if(!customizingFlag){
            Intent customIntent = new Intent(getApplicationContext(),CustomizeObjectlist.class);
            startActivity(customIntent);
            customizingFlag=true;
        }
        String [] objectArray = getResources().getStringArray(R.array.array_object);
        for(String s : objectArray){
            Log.d(TAG, s);
        }

        if(bluetoothAdapter==null){
            Toast.makeText(getApplicationContext(), "단말기가 블루투스를 지원하지 않습니다.", Toast.LENGTH_LONG).show();
        }else{
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }

        // SWIPE
        View view = findViewById(R.id.background_view);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
                    // Object Detection + Navigation
                    public void onSwipeTop() {

                        toast("swipe top");
                        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo ni = cm.getActiveNetworkInfo();
                        // 네트워크 연결 확인
                        if(ni.isConnected() && ni!=null){
                            if(ni.getType()==ConnectivityManager.TYPE_MOBILE){
                                //목적지 음성 입력 시작
                                Log.d(TAG, "TTS State : "+isTtsFlag);
                                // 목적지 입력 후 재확인 process
                                if(isTtsFlag){
                                    isTtsFlag=false;
                                    editText.setText(destination);
                                    Log.d(TAG, editText.getText().toString());
                                    mRecognizer.stopListening();

                                    //블루투스 연결 및 Object Detection 시작
                                    bluetoothAdapter.enable();
                                    bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();
                                    Log.d(TAG, "bluetooth : "+bluetoothDeviceSet);

                                    // Navigation 시작
                                    if(connect()){
                                        Log.d(TAG, "connect");
                                        Intent intent1 = new Intent(getApplicationContext(), NavigationActivity.class);
                                        intent1.putExtra("destination", editText.getText().toString());
                                        intent1.putExtra("d_latitude", latitude);
                                        intent1.putExtra("d_longitude", longitude);
                                        startActivity(intent1);
                                    }
                                }else{
                                    // 목적지 입력 process
                                    isTtsFlag=true;
                                    inputVoice();
                                }

                            }else if(ni.getType()==ConnectivityManager.TYPE_WIFI){
                                toast("와이파이 연결을 해제하고 데이터로 연결해주세요");
                            }
                        }else{
                            toast("데이터 연결을 해주세요!");
                        }

                        /* 디버깅용
                        Intent intent1 = new Intent(getApplicationContext(), NavigationActivity.class);
                        intent1.putExtra("destination", "공릉역");
                        intent1.putExtra("d_latitude",37.62558792);
                        intent1.putExtra("d_longitude",127.07298295);
                        startActivity(intent1);*/

                    }
                    // Bluetooth Connection and object detection
                    public void onSwipeBottom() {
                        Log.d(TAG, "TTS State : "+isTtsFlag);
                        if(!isTtsFlag){
                            toast("swipe bottom");
                            //블루투스 On
                            bluetoothAdapter.enable();

                            bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();
                            Log.d(TAG, "bluetooth : "+bluetoothDeviceSet);

                            focusFlag=true;
                            connect();
                            Log.d(TAG, "connect");
                            //CheckTypesTask task = new CheckTypesTask();
                            //task.execute();
                        }else{
                            inputVoice();
                        }
                    }
                    // Bluetooth setting
                    public void onSwipeRight() {
                        toast("swipe Right");
                        if (bluetoothDevice == null){
                            toast("no connection Bluetooth");
                            return;
                        }
                        else {
                            toast("start Object Detection");
                            try{

                            }catch (Exception e){
                                // 쓰레드에서 UI처리를 위한 핸들러
                                Message msg = handler.obtainMessage();
                                handler.sendMessage(msg);
                                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                                startActivity(intent);
                            }
                        }
                    }
                    // Disconnect
                    public void onSwipeLeft(){
                        toast("swipe Left");
                        disconnect();
                    }
                }
        );
    }

    public void setInitialStart(){
        //Permission Request
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
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "Main Activity is on Start");
        super.onStart();

        if(service != null && !initialStart) {
            service.attach(this);
        }

        /*
        //음성출력 설정값
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ttsFlag=sharedPreferences.getBoolean("voice_notification",false);
        Log.d(TAG, "ttsFlag is " + ttsFlag);
         */

        // 음성출력
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

    }

    @Override
    public void onResume() {
        super.onResume();
        // 음성출력
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
        editText.setText("");

        if(initialStart && service !=null) {
            initialStart = false;
            this.runOnUiThread(this::connect);
        }
    }

    @Override
    protected void onStop() {
        if(service != null && isChangingConfigurations()){
            service.detach();
            this.unbindService(this);
        }
        Log.d(TAG, "Main Activity is on Stop");
        super.onStop();
        if(mRecognizer!=null) mRecognizer.destroy(); //mRecognizer.stopListening();//
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu) ;
        return true ;
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


    public class FindPOI extends Thread{

        @Override
        public void run() {
            findPOI();
        }
        public void findPOI(){
            tMapData.findAllPOI(search, new TMapData.FindAllPOIListenerCallback() {

                @Override
                public void onFindAllPOI(ArrayList<TMapPOIItem> poiItems) {
                    Log.d(TAG, "First poi item : "+poiItems.get(0).getPOIName() + ", Point : " + poiItems.get(0).getPOIPoint().toString());
                    //editText.setText(poiItems.get(0).getPOIName());
                    destination = poiItems.get(0).getPOIName();
                    endPoint = poiItems.get(0).getPOIPoint();
                    latitude = endPoint.getLatitude();
                    longitude = endPoint.getLongitude();
                    Log.d(TAG,"POI item : " +latitude +", " + longitude);
                }

            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String location = destination;
            while(location==null){}
            String speak = location + " 를 목적지로 정하시겠어요?";

            Log.d(TAG, "목적지 : " + location);
            tts.speak(speak,TextToSpeech.QUEUE_FLUSH,null);
            Thread.currentThread().interrupt();
        }

        @Override
        public void interrupt() {
            super.interrupt();
        }
    }
    // STT
    public void inputVoice() {
        //음성인식
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        mRecognizer.setRecognitionListener(listener);
        mRecognizer.startListening(intent);
        runnable = new Runnable() {
            @Override
            public void run() {
                mRecognizer.startListening(intent);
            }
        };
    }

    //Bluetooth connection
    private boolean connect() {
        this.startService(new Intent(getApplicationContext(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
        this.bindService(new Intent(getApplicationContext(), SerialService.class), this, Context.BIND_AUTO_CREATE);
        try {
            String deviceAddress = null;
            for(BluetoothDevice devices : bluetoothDeviceSet) {
                if(devices.getName().contains(device_name)) {
                    bluetoothDevice = devices;
                    checknumber=1;
                    //모듈화시 사용 , 변수 저장하여 넘김
                    //Bundle args = new Bundle();
                    //args.putString("device",device.getAddress());

                    deviceAddress = devices.getAddress();
                }
            }
            Log.d(TAG, "Device name "+bluetoothDevice);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);


            Log.d(TAG, "Device2 : "+device);

            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(this.getApplicationContext(), device);
            Log.d(TAG, "get socket : "+socket.getName());
            toast(socket.getName());


            service.connect(socket);
            return true;

        } catch (Exception e) {
            onSerialConnectError(e);
            return false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        Log.d(TAG, "Service connected");
        this.runOnUiThread(this::connect);

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service=null;
    }

    private void disconnect() {
        connected = Connected.False;
        Log.d(TAG, "Bluetooth disconnected");
        service.disconnect();
    }
    //serial Listener
    @Override
    public void onSerialConnect() {
        Log.d(TAG, "Connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        Log.d(TAG, "connection Failed : "+e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    private void receive(byte[] data) {
        String object = new String(data);
        if(focusFlag){
            Log.d(TAG, "Detect ! "+object);
            tts.speak(object,TextToSpeech.QUEUE_FLUSH,null);
            toast(object);
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        Log.d(TAG, "connection lost : "+e.getMessage());
        disconnect();
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
            search = rs[0];


            FindPOI findPOI = new FindPOI();
            Thread thread = new Thread(findPOI);
            thread.start();

            //mRecognizer.destroy();

        }

        @Override
        public void onPartialResults(Bundle partialResults){
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }

    };

    // 핸들러 선언
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), "지팡이를 찾을수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    };

    private void toast(String msg){
        Toast.makeText(this ,msg, Toast.LENGTH_LONG).show();
    }

}
