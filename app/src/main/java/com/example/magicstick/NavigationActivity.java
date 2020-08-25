package com.example.magicstick;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;


public class NavigationActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback, TextToSpeech.OnInitListener{


    private static String appKey ="l7xx9ed3bc26b00f404b816bb3b6e2f44ec9";
    private final TMapData tMapData = new TMapData();
    private TMapGpsManager tMapGps = null;
    private TMapView tMapView =null;
    private boolean m_bTrackingMode = true;
    final String TAG = getClass().getName();
    private TextToSpeech tts;
    TMapPoint startPoint;
    TMapPoint endPoint;
    Handler handler = new Handler();
    Runnable runnable;
    int index=-1;
    double prev_lat;
    double prev_long;
    LinkedList<String> coordinates = new LinkedList<String>();
    LinkedList<String> navigation = new LinkedList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_activitty);
        getSupportActionBar().setTitle("길찾기");

        Intent intent = getIntent();

        final LinearLayout mapview = findViewById(R.id.map_view);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(appKey);

        final EditText editText2 = findViewById(R.id.destination);
        final EditText editText = findViewById(R.id.departure);

        String destination =intent.getExtras().getString("destination");
        editText2.setText(destination);


        //현위치 좌표계 받아오기
        tMapGps = new TMapGpsManager(this);
        tMapGps.setMinTime(1000);
        tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);
        tMapGps.setProvider(tMapGps.GPS_PROVIDER);
        tMapGps.OpenGps();

        tts = new TextToSpeech(this, this);


        editText.setText("현위치");

        startPoint = tMapGps.getLocation();



        //장소 입력받아 좌표계 가져오기
        tMapData.findAllPOI(destination, new TMapData.FindAllPOIListenerCallback() {

            @Override
            public void onFindAllPOI(ArrayList<TMapPOIItem> poiItems) {
                Log.d(TAG, "First poi item : "+poiItems.get(0).getPOIName() + ", Point : " + poiItems.get(0).getPOIPoint().toString());
                endPoint = poiItems.get(0).getPOIPoint();
                Log.d(TAG,"POI item : " + endPoint.getLatitude()+", " + endPoint.getLongitude());
                //findPath(startPoint,endPoint);
            }

        });


        // T MAP setting
        tMapView.setCompassMode(true);
        tMapView.setIconVisibility(true);
        tMapView.setZoomLevel(15);
        tMapView.setMapType(TMapView.MAPTYPE_STANDARD);  //일반지도
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN);
        tMapView.setTrackingMode(true);
        tMapView.setSightVisible(true);
        tMapView.setHttpsMode(true);
        tMapView.setLocationPoint(startPoint.getLongitude(),startPoint.getLatitude());


        mapview.addView(tMapView);
    }


    @Override
    public void onLocationChange(Location location) {
        Log.d(TAG,"index"+String.valueOf(index));
        Log.d(TAG, "Location Changed !");
        if(m_bTrackingMode){
            tMapView.setLocationPoint(location.getLongitude(),location.getLatitude());
        }
        if(index==-1){
            startPoint = new TMapPoint(location.getLatitude(), location.getLongitude());
            findPath(startPoint,endPoint);
            index = 0;
        }

        if(coordinates.peek()==null || navigation.peek()==null){
            Log.d(TAG, "Coordinates and Navigation are null");

        }else {
            boolean in = Distance(location);
            if (in == false) {
                Log.d(TAG, "wrong route!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                startPoint = new TMapPoint(location.getLatitude(), location.getLongitude());
                findPath(startPoint, endPoint);
            }



                String peek = coordinates.peek();

                double longitude = Math.round(location.getLongitude() * 1000000) / 1000000.0;
                double latitude = Math.round(location.getLatitude() * 1000000) / 1000000.0;
                double p_longitude = Math.round(Double.parseDouble(peek.split(",")[0]) * 1000000) / 1000000.0;
                double p_latitude = Math.round(Double.parseDouble(peek.split(",")[1]) * 1000000) / 1000000.0;
                Log.d(TAG, "Current GPS : " + longitude + ", " + latitude);

                if (Math.abs(longitude - p_longitude) <= 0.000002 && Math.abs(latitude - p_latitude) <= 0.000002) {
                    Log.d(TAG, "Current navigation : " + navigation.peek());
                    speech(navigation.peek());
                    prev_lat = p_latitude;
                    prev_long = p_longitude;
                    coordinates.poll();
                    navigation.poll();

                } else {
                    Log.d(TAG, " Go to " + p_longitude + " , " + p_latitude);

                }


        }

    }

    private boolean Distance(Location location){
        String peek = coordinates.peek();
        double longitude = Math.round(location.getLongitude()*1000000)/1000000.0;
        double latitude = Math.round(location.getLatitude()*1000000)/1000000.0;
        double p_longitude = Math.round(Double.parseDouble(peek.split(",")[0])*1000000)/1000000.0;
        double p_latitude = Math.round(Double.parseDouble(peek.split(",")[1])*1000000)/1000000.0;

        double a = prev_lat - p_latitude;
        double b = p_longitude - prev_long;
        double c = prev_long*p_latitude - p_longitude*prev_lat;
        double dist = (double)Math.abs(a*longitude + b*latitude + c) / (double)Math.sqrt(a*a + b*b);
        Log.d(TAG, String.valueOf(dist));
        if (dist < 0.001){
            return true;
        } else{
            return false;
        }

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // 작업 성공
            int language = tts.setLanguage(Locale.KOREAN);  // 언어 설정
            if (language == TextToSpeech.LANG_MISSING_DATA
                    || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 언어 데이터가 없거나, 지원하지 않는경우
                toast("지원하지 않는 언어입니다.");
            }
        } else {
            toast("speech fail! ");
        }

    }

    public void speech(String text){

        tts.setLanguage(Locale.KOREAN);
        tts.setPitch(1.0f);
        tts.setSpeechRate(1.0f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH,null);

    }


    public void findPath(TMapPoint startPoint, TMapPoint endPoint){
        Log.d(TAG, "경로그리기 with " + "departure : "+startPoint.getLatitude()+", "+ startPoint.getLongitude()+ " / destination : " + endPoint.getLatitude()+", " + endPoint.getLongitude() );

        tMapData.findPathDataAllType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint, new TMapData.FindPathDataAllListenerCallback() {
            @Override
            public void onFindPathDataAll(Document document) {

                Element root = document.getDocumentElement();

                NodeList nodeListPlacemark = root.getElementsByTagName("Placemark");
                Log.d(TAG,"Root element :" + root.getNodeName());

                for( int i=0; i<nodeListPlacemark.getLength(); i++ ) {
                    NodeList nodeListPlacemarkItem = nodeListPlacemark.item(i).getChildNodes();

                    for( int j=0; j<nodeListPlacemarkItem.getLength(); j++ ) {
                        if( nodeListPlacemarkItem.item(j).getNodeName().equals("Point")) {
                            String c = nodeListPlacemarkItem.item(31).getTextContent().trim();
                            String n = nodeListPlacemarkItem.item(7).getTextContent().trim();
                            coordinates.add(c);
                            navigation.add(n);
                            Log.d(TAG, i + " GPS : " + c);
                            Log.d(TAG, i+ " Navigation : " + n);
                        }

                    }
                }

            }

        });

        tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine polyLine) {
                polyLine.setLineWidth(7);
                polyLine.setLineColor(Color.RED);
                tMapView.addTMapPath(polyLine);
            }
        });

    }

    private void toast(String msg){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show();
    }

}

