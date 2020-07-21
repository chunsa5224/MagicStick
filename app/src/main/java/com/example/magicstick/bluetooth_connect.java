package com.example.magicstick;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class bluetooth_connect extends AppCompatActivity {
    // onActivity Result의 RequestCode 식별자
    private static final int REQUEST_NEW_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // 블루투스 사용 객체
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private Set<BluetoothDevice> bluetoothDeviceSet;
    private BluetoothSocket bluetoothSocket;
    public static InputStream inputStream;
    public static OutputStream outputStream;
    private Thread workerThread = null; // 문자열 수신에 사용되는 쓰레드

    private byte[] readBuffer; // 수신 된 문자열을 저장하기 위한 버퍼

    private int readBufferPosition; // 버퍼 내 문자 저장 위치


    private Button buttonSend; // 송신하기 위한 버튼


    // xml 객체
    private ListView listView_pairing_devices; // 페어링 된 디바이스 리스트뷰 객체


    // 일반 객체
    private String device_name;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        // 리스트 뷰 객체와 xml id 연결
        listView_pairing_devices = (ListView)findViewById(R.id.listview_pairing_devices);

        // 버튼 객체 생성 및 xml id 연결
        Button button_setting_menu = (Button)findViewById(R.id.button_setting_menu);
        // "디바이스 신규 등록" 버튼 클릭 이벤트
        button_setting_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 블루투스 설정 화면 띄워주기
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                // 종료 후 onActivityResult 호출, requestCode가 넘겨짐
                startActivityForResult(intent, REQUEST_NEW_DEVICE);
            }
        });

        // 블루투스 활성화 확인 함수 호출
        checkBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            // 블루투스 설정화면 종료 시
            case REQUEST_NEW_DEVICE :
                checkBluetooth();
                break;
            // 블루투스 활성화 선택 종료 시
            case REQUEST_ENABLE_BT :
                // 활성화 버튼을 눌렀을 때
                if(resultCode == RESULT_OK) {
                    selectDevice();
                }
                // 취소 버튼을 눌렀을 때
                else if(resultCode == RESULT_CANCELED) {
                    Toast.makeText(getApplicationContext(), "블루투스를 활성화 하지 않아 앱을 종료합니다.", Toast.LENGTH_LONG).show();
                    // 앱 종료
                    finish();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 블루투스 활성화 확인 함수
    public void checkBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 단말기가 블루투스를 지원하지 않을 때
        if(bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "단말기가 블루투스를 지원하지 않습니다.", Toast.LENGTH_LONG).show();
            finish(); // 앱 종료
        }
        // 블루투스를 지원할 때
        else {
            // 블루투스가 활성화 상태
            if(bluetoothAdapter.isEnabled()) {
                // 블루투스 선택 함수 호출
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
            // 블루투스가 비 활성화 상태
            else {
                // 블루투스를 활성화
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }
    }

    // 블루투스 선택 함수
    public void selectDevice() {
        // 페어링 된 디바이스 목록을 불러옴
        bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();

        // 리스트를 만듬
        List<String> list = new ArrayList<>();
        for(BluetoothDevice bluetoothDevice : bluetoothDeviceSet) {
            list.add(bluetoothDevice.getName());
        }

        // 어레이어뎁터로 리스트뷰에 리스트를 생성
        final ArrayAdapter arrayAdapter = new ArrayAdapter(bluetooth_connect.this, android.R.layout.simple_list_item_1, list);
        listView_pairing_devices.setAdapter(arrayAdapter);
        listView_pairing_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // 리스트 뷰의 아이템을 클릭했을 때
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                device_name = "raspberrypi";
                //device_name = arrayAdapter.getItem(position).toString();

                // 프로그레스 다이얼로그 생성
                CheckTypesTask task = new CheckTypesTask();
                task.execute();
            }
        });
    }

    // 클릭 된 디바이스와 연결하는 함수
    public void connectDevice(String deviceName) {
        // 블루투스 연결 할 디바이스를 찾음
        for(BluetoothDevice device : bluetoothDeviceSet) {
            if(device.getName().contains(deviceName)) {
                bluetoothDevice = device;
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
            receiveData();
            // 블루투스 수신 시작 호출
            //Intent intent = new Intent(bluetooth_connect.this, bluetooth_talk.class);
            //startActivity(intent);

        }
        catch (Exception e) {
            // 쓰레드에서 UI처리를 위한 핸들러
            Message msg = handler.obtainMessage();
            handler.sendMessage(msg);
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
                        // 1초마다 받아옴
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
        workerThread.start();
    }
    // 핸들러 선언
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), "블루투스 연결을 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
        }
    };

    // 프로그레스 다이얼로그 생성
    private class CheckTypesTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog asyncDialog = new ProgressDialog(
                bluetooth_connect.this);

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
            connectDevice(device_name);
            return null;
        }

        // 백그라운드가 모두 끝난 후 실행
        @Override
        protected void onPostExecute(Void result) {
            asyncDialog.dismiss();
            super.onPostExecute(result);
        }
    }
}

