package com.example.magicstick;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
    boolean nullLocation=true;
    static double prev_lat;
    static double prev_long;
    static double curr_lat;
    static double curr_long;


    public static LinkedList<String> coordinates = new LinkedList<String>();
    public static LinkedList<String> navigation = new LinkedList<String>();

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
        double d_latitude = intent.getExtras().getDouble("d_latitude");
        double d_longitude = intent.getExtras().getDouble("d_longitude");
        endPoint = new TMapPoint(d_latitude,d_longitude);
        editText2.setText(destination);


        //현위치 좌표계 받아오기
        tMapGps = new TMapGpsManager(this);
        tMapGps.setMinTime(1000);
        //실외 - 실제 코드에 사용
        tMapGps.setProvider(tMapGps.GPS_PROVIDER);
        //실내 - 디버깅용
        tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);
        tMapGps.OpenGps();


        tts = new TextToSpeech(this, this);


        editText.setText("현위치");

        startPoint = tMapGps.getLocation();

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
        Log.d(TAG, "Location Changed ! index: "+ nullLocation );
        if(m_bTrackingMode){
            tMapView.setLocationPoint(location.getLongitude(),location.getLatitude());
        }
        FindPath findPath = new FindPath();
        if(nullLocation) {
            startPoint = new TMapPoint(location.getLatitude(), location.getLongitude());
            Thread thread = new Thread(findPath);
            thread.start();
            nullLocation=false;

        }

        if(coordinates.peek()==null || navigation.peek()==null){
            Log.d(TAG, "Coordinates and Navigation are null");

        }else {
            String peek = coordinates.peek();
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            curr_long = Double.parseDouble(peek.split(",")[0]);
            curr_lat = Double.parseDouble(peek.split(",")[1]);

            Log.d(TAG, "Current GPS : " + longitude + ", " + latitude);
            double dist = GpsToMeter(latitude,longitude,curr_lat,curr_long);
            if(dist<=5){
                Log.d(TAG, "Current navigation : " + navigation.peek());
                speech(navigation.peek());
                prev_lat = curr_lat;
                prev_long = curr_long;
                coordinates.poll();
                navigation.poll();

            }else if(wrongRoute(latitude,longitude)){
                Log.d(TAG, "wrong route!");
                Thread thread = new Thread(findPath);
                thread.start();

            }else{
                Log.d(TAG," Go to " + curr_lat + " , " + curr_long);
            }

        }


    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus){
            Log.d(TAG, "Activity 가 화면 보여짐! ");
            NavigationService.isStopped = true;
            Intent serviceIntent = new Intent(this, NavigationService.class);
            stopService(serviceIntent);

            coordinates= NavigationService.coordinates;
            navigation= NavigationService.navigation;


        }else{
            Log.d(TAG, "lose focus");
            while(tts.isSpeaking())
                tts.stop();
            tts.shutdown();
            Intent serviceIntent = new Intent(this, NavigationService.class);
            startService(serviceIntent);
            Log.d(TAG, "Navigation Activity is on Stop");
            if(tts== null) {
                Log.d(TAG, "TTS Destroyed");
            }

        }
    }

    @Override
    protected void onStop() {

        super.onStop();


    }

    static double GpsToMeter(double lat1, double lon1, double lat2, double lon2) {

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1609.344;
        return (dist);
    }


    // This function converts decimal degrees to radians
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    // This function converts radians to decimal degrees
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    boolean wrongRoute(double latitude,double longitude){

        double a = GpsToMeter(latitude,longitude,prev_lat,prev_long);
        double b = GpsToMeter(latitude,longitude,curr_lat,curr_long);
        double c = GpsToMeter(prev_lat,prev_long,curr_lat, curr_long);
        Log.d("", "meter : " + a +", "+ b + ", "+ c);
        double result = Math.sqrt(a*a - ( Math.pow(a*a-b*b+c*c,2) / (4*c*c) ) );
        Log.d("Wrong Route?", result+"");
        if(result>=40) return true;
        else return false;
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

    public class FindPath extends Thread{
        @Override
        public void run(){
            findPath(startPoint,endPoint);
        }

        @Override
        public void interrupt() {
            super.interrupt();
        }

        public void findPath (TMapPoint startPoint, TMapPoint endPoint){
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
                    Log.d(TAG, "First Navigation");
                    prev_lat = Double.parseDouble(coordinates.peek().split(",")[1]);
                    prev_long = Double.parseDouble(coordinates.peek().split(",")[0]);
                    coordinates.poll();
                    speech(navigation.peek());
                    navigation.poll();
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
    }


    private void toast(String msg){
        Toast.makeText(this,msg, Toast.LENGTH_LONG).show();
    }

}

