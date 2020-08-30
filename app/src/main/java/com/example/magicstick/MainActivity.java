package com.example.magicstick;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
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

    public static InputStream inputStream;
    public static OutputStream outputStream;
    private SpeechRecognizer mRecognizer;
    private EditText editText;
    private TextToSpeech tts;
    private Runnable runnable;
    private BluetoothDevice bluetoothDevice;
    private Set<BluetoothDevice> bluetoothDeviceSet;
    private BluetoothSocket bluetoothSocket;
    private Thread workerThread = null; // 문자열 수신에 사용되는 쓰레드
    private byte[] readBuffer; // 수신 된 문자열을 저장하기 위한 버퍼



    private int readBufferPosition; // 버퍼 내 문자 저장 위치
    private int checknumber=0;
    private boolean ttsFlag = true;
    private boolean bluetoothFlag = false;
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    private final String TAG = getClass().getName();
    private static boolean isTtsFlag = false;    //TTS 활성화 상태
    private String device_name = "rasp";
    private static String appKey ="l7xx9ed3bc26b00f404b816bb3b6e2f44ec9";
    private final TMapData tMapData = new TMapData();
    TMapPoint endPoint;
    private Double latitude;
    private Double longitude;
    private String search;
    private TMapView tMapView =null;

    private SerialService service;

    private enum Connected { False, Pending, True }
    private boolean initialStart = true;
    private Connected connected = Connected.False;
    // 블루투스 사용 객체
    private BluetoothAdapter bluetoothAdapter;
    Intent intent;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText)findViewById(R.id.editText);
        tMapView = new TMapView(getApplicationContext());
        tMapView.setSKTMapApiKey(appKey);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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

        if(bluetoothAdapter==null){
            Toast.makeText(getApplicationContext(), "단말기가 블루투스를 지원하지 않습니다.", Toast.LENGTH_LONG).show();
            //finish(); // 앱 종료
        }else{
            if(bluetoothAdapter.isEnabled()){
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
            else{
                //블루투스 활성화
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }


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

        // SWIPE
        View view = findViewById(R.id.background_view);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
                    public void onSwipeTop() {
                        /*toast("swipe top");
                        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo ni = cm.getActiveNetworkInfo();
                        // 네트워크 연결 확인
                        if(ni.isConnected() && ni!=null){
                            if(ni.getType()==ConnectivityManager.TYPE_MOBILE){
                                //목적지 음성 입력 시작
                                Intent intent1 = new Intent(getApplicationContext(), NavigationActivity.class);
                                intent1.putExtra("destination", "공릉역");
                                startActivity(intent1);

                                try {
                                    //inputVoice();
                                } catch(SecurityException e) {
                                    e.printStackTrace();
                                }

                            }else if(ni.getType()==ConnectivityManager.TYPE_WIFI){
                                toast("와이파이 연결을 해제하고 데이터로 연결해주세요");
                            }
                        }else{
                            toast("데이터 연결을 해주세요!");
                        }
                        */
                        /*//디버깅용
                        Intent intent1 = new Intent(getApplicationContext(), NavigationActivity.class);
                        intent1.putExtra("destination", "공릉역");
                        intent1.putExtra("d_latitude",37.62558792);
                        intent1.putExtra("d_longitude",127.07298295);
                        startActivity(intent1);*/

                        Log.d(TAG, "TTS State : "+isTtsFlag);
                        if(isTtsFlag){
                            isTtsFlag=false;
                            Log.d(TAG, editText.getText().toString());
                            tts.stop();
                            //tts.shutdown();
                            mRecognizer.stopListening();
                            //mRecognizer.destroy();
                            Intent intent1 = new Intent(getApplicationContext(), NavigationActivity.class);
                            intent1.putExtra("destination", editText.getText().toString());
                            intent1.putExtra("d_latitude", latitude);
                            intent1.putExtra("d_longitude", longitude);
                            startActivity(intent1);
                        }else{
                            isTtsFlag=true;
                            inputVoice();
                        }


                    }
                    public void onSwipeBottom() {
                        Log.d(TAG, "TTS State : "+isTtsFlag);
                        if(!isTtsFlag){
                            toast("swipe bottom");
                            //블루투스 On

                            bluetoothAdapter.enable();

                            bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();
                            /*
                             // 리스트를 만듬
                             List<String> list = new ArrayList<>();
                             for(BluetoothDevice bluetoothDevice : bluetoothDeviceSet) {
                             list.add(bluetoothDevice.getName());
                             }
                             */
                            Log.d(TAG, "bluetooth : "+bluetoothDeviceSet);

                            connect();

                            //CheckTypesTask task = new CheckTypesTask();
                            //task.execute();
                        }else{
                            inputVoice();
                        }
                    }

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

                    public void onSwipeLeft(){
                        toast("swipe Left");

                    }

                }

        );

    }

    @Override
    protected void onStart() {
        Log.d(TAG, "Main Activity is on Start");
        super.onStart();
        if(service != null && !initialStart) {
            service.attach(this);
        }
        else {
            this.startService(new Intent(getApplicationContext(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
            this.bindService(new Intent(getApplicationContext(), SerialService.class), this, Context.BIND_AUTO_CREATE);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ttsFlag=sharedPreferences.getBoolean("voice_notification",false);
        bluetoothFlag = sharedPreferences.getBoolean("bluetooth",false);
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
        Log.d(TAG, "ttsFlag is " + ttsFlag);

    }


    @Override
    protected void onStop() {
        if(service != null && !isChangingConfigurations())
            service.detach();
            this.unbindService(this);
        Log.d(TAG, "Main Activity is on Stop");
        super.onStop();
        tts.stop();
        if(mRecognizer!=null) mRecognizer.destroy(); //mRecognizer.stopListening();//



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

    private void connect() {
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
        } catch (Exception e) {

            onSerialConnectError(e);

        }
    }
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        this.runOnUiThread(this::connect);

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

        service=null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service !=null) {
            initialStart = false;
            this.runOnUiThread(this::connect);
        }
    }



    private void disconnect() {
        connected = Connected.False;
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
        toast(new String(data));
    }


    @Override
    public void onSerialIoError(Exception e) {
        Log.d(TAG, "connection lost : "+e.getMessage());
        disconnect();
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
                    editText.setText(poiItems.get(0).getPOIName());
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

            String location = editText.getText().toString();
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
    //음성인식
    public void inputVoice() {
        //음성인식
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,5000);
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
    // 프로그레스 다이얼로그 생성
    private class CheckTypesTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog asyncDialog = new ProgressDialog(
                MainActivity.this);
        private String device_name = "raspberrypi";
        @Override
        protected void onPreExecute() {
            asyncDialog.setCancelable(false);
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage(device_name + "에 연결중입니다.");

            // show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

        // 백그라운드에서 실행
        @Override
        protected Void doInBackground(Void... arg0) {

            bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();
            /**
            // 리스트를 만듬
            List<String> list = new ArrayList<>();
            for(BluetoothDevice bluetoothDevice : bluetoothDeviceSet) {
                list.add(bluetoothDevice.getName());
            }
            */
            connect();
            return null;
        }

        // 백그라운드가 모두 끝난 후 실행
        @Override
        protected void onPostExecute(Void result) {
            asyncDialog.dismiss();
            super.onPostExecute(result);
        }
    }
    /*
    // 클릭 된 디바이스와 연결하는 함수
    public void connectDevice(String deviceName) {
        // 블루투스 연결 할 디바이스를 찾음

        for(BluetoothDevice device : bluetoothDeviceSet) {
            if(device.getName().contains(deviceName)) {
                bluetoothDevice = device;
                checknumber=1;
                //모듈화시 사용 , 변수 저장하여 넘김
                //Bundle args = new Bundle();
                //args.putString("device",device.getAddress());

                String deviceAddress = device.getAddress();
                service.attach((SerialListener) this);
                break;
            }
        }

        // UUID 생성
        UUID uuid = java.util.UUID.fromString("00000003-0000-1000-8000-00805F9B34FB");

        try {
            // 블루투스 소켓 생성
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            // 데이터 받기 위해 인풋스트림 생성
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            // 블루투스 수신 시작 호출
            //Intent intent = new Intent(bluetooth_connect.this, bluetooth_talk.class);
            //startActivity(intent);
        } catch (Exception e) {
            // 쓰레드에서 UI처리를 위한 핸들러
            Message msg = handler.obtainMessage();
            handler.sendMessage(msg);
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }

    }

    public void receiveData() {
        final Handler hand = new Handler();
        // 데이터를 수신하기 위한 버퍼를 생성
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        // 데이터를 수신하기 위한 쓰레드 생성
        workerThread = new Thread(new Runnable() {

            @Override

            public void run() {
                while(Thread.currentThread().isInterrupted()) {
                    try {
                        // 데이터를 수신했는지 확인합니다.
                        int byteAvailable = inputStream.available();
                        // 데이터가 수신 된 경우
                        if(byteAvailable > 0) {
                            // 입력 스트림에서 바이트 단위로 읽어 옵니다.
                            byte[] bytes = new byte[byteAvailable];
                            inputStream.read(bytes);
                            // 입력 스트림 바이트를 한 바이트씩 읽어 옵니다.
                            for(int i = 0; i < byteAvailable; i++) {
                                byte tempByte = bytes[i];
                                // 개행문자를 기준으로 받음(한줄)
                                if(tempByte == '\n') {
                                    // readBuffer 배열을 encodedBytes로 복사
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    // 인코딩 된 바이트 배열을 문자열로 변환
                                    final String text = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    hand.post(new Runnable() {

                                        @Override

                                        public void run() {
                                            Toast.makeText(getApplicationContext(), text,Toast.LENGTH_LONG).show();
                                        }

                                    });
                                } // 개행 문자가 아닐 경우
                                else {
                                    readBuffer[readBufferPosition++] = tempByte;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        // 10초마다 받아옴
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
        workerThread.start();
    }

     */
    // 핸들러 선언
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), "지팡이를 찾을수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    };

    private void toast(String msg){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show();
    }

}
