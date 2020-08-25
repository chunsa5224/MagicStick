package com.example.magicstick;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


import java.util.ArrayList;


public class TextToSpeech extends Thread{
    final String TAG = getClass().getName();
    SpeechRecognizer mRecognizer;
    Intent intent;
    Context mContext;
    EditText editText;
    Runnable runnable;


    public TextToSpeech(Context context){
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View v= inflater.inflate(R.layout.activity_main,null);
        editText = (EditText) v.findViewById(R.id.editText);

    }
    public void run(){
        inputVoice();
    }

    RecognitionListener listener = new RecognitionListener() {

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
                    //speak = rs[0] + " 를 목적지로 정하시겠어요?";

                    Log.d(TAG, "목적지 : " + rs[0]);


                }else{
                    Log.d(TAG, rs[0]);

                    if(rs[0].equals("네")){

                        Log.d(TAG, "Navigation activity will start");
                        STT_RESULT=0;

                        mRecognizer.stopListening();
                        mRecognizer.cancel();
                        mRecognizer.destroy();

                        Intent intent1 = new Intent((MainActivity) mContext, NavigationActivity.class);
                        intent1.putExtra("destination", editText.getText().toString());
                        mContext.startActivity(intent1);

                    }else{
                        Log.d(TAG, "Ask again");
                        STT_RESULT=0;
                        editText.setText(rs[0]);

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
    public void inputVoice(){
        //음성인식
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,5000);

        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, mContext.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext.getApplicationContext());
        mRecognizer.setRecognitionListener(listener);
        mRecognizer.startListening(intent);
        runnable= new Runnable() {
            @Override
            public void run() {
                mRecognizer.startListening(intent);
            }
        };
    }
    private void toast(String msg){
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }
}
