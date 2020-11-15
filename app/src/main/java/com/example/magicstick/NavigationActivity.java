package com.example.magicstick;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;

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
import static com.example.magicstick.MainActivity.speech;


public class NavigationActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback{


    private static String appKey ="l7xx9ed3bc26b00f404b816bb3b6e2f44ec9";
    private final TMapData tMapData = new TMapData();
    private TMapGpsManager tMapGps = null;
    private TMapView tMapView =null;
    private boolean m_bTrackingMode = true;
    FindPath findPath = new FindPath();
    Thread thread;
    /*ObjectDetect od = new ObjectDetect();
    Set<String> objectList;
    Thread thread2;
    private Intent serviceIntent;*/



    TMapPoint startPoint;
    TMapPoint endPoint;
    public static boolean stopFlag=false;
    boolean nullLocation=true;

    final String TAG = getClass().getName();
    LinkedList<String> coordinates = new LinkedList<String>();
    LinkedList<String> navigation = new LinkedList<String>();
    double h_longitude;
    double h_latitude;
    double p_latitude;
    double p_longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_activitty);
        getSupportActionBar().setTitle("길찾기");

        Intent intent = getIntent();

        final LinearLayout mapview = findViewById(R.id.map_view);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(appKey);
        locationSetting();

        final EditText editText2 = findViewById(R.id.destination);
        final EditText editText = findViewById(R.id.departure);

        String destination =intent.getExtras().getString("destination");
        double d_latitude = intent.getExtras().getDouble("d_latitude");
        double d_longitude = intent.getExtras().getDouble("d_longitude");

        endPoint = new TMapPoint(d_latitude,d_longitude);
        editText2.setText(destination);


        /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        objectList = sharedPreferences.getStringSet("object_list1",null);
        if(detectionFlag){
            Log.d(TAG, "detection flag : " + detectionFlag);
            thread2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        try{
                            thread2.wait(2000);
                            Log.d(TAG,"object detect");
                            objectDetect();
                        }catch (Exception e){

                        }
                    }
                }
            });
            thread2 = new Thread(od);
            thread2.start();
        }*/
        mapview.addView(tMapView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nullLocation=true;
    }

    @Override
    public void onLocationChange(Location location) {
        Log.d(TAG, "Location Changed ! flag: "+ nullLocation );
        if(m_bTrackingMode){
            tMapView.setLocationPoint(location.getLongitude(),location.getLatitude());
        }

        if(nullLocation) {
            startPoint = new TMapPoint(location.getLatitude(), location.getLongitude());
            thread = new Thread(findPath);
            thread.start();
            nullLocation=false;
        }else{
            double curr_longitude = location.getLongitude();
            double curr_latitude = location.getLatitude();
            double dist = GpsToMeter(curr_latitude,curr_longitude, h_latitude,h_longitude);

            if(dist<=4){
                String navi = navigation.poll();
                Log.d(TAG, "Next Navigation : " + navi);
                speech(navi);
                if(!coordinates.isEmpty()){
                    String peek = coordinates.poll();
                    p_latitude= h_latitude;
                    p_longitude=h_longitude;
                    h_longitude = Double.parseDouble(peek.split(",")[0]);
                    h_latitude =Double.parseDouble(peek.split(",")[1]);
                }else{
                    speech("목적지에 도착하였습니다. 경로안내를 종료합니다.");
                    /*Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);*/
                    onStop();
                }

            }else if(wrongRoute(curr_latitude, curr_longitude)){
                Log.d(TAG, " Wrong Route");
                speech("경로를 벗어났습니다.");
                startPoint = new TMapPoint(curr_latitude,curr_longitude);
                thread = new Thread(findPath);
                thread.start();
            }


        }
    }

    @Override
    protected void onStop() {
        tMapGps.CloseGps();
        /*thread2.interrupt();
        tts.stop();
        tts.shutdown();*/
        super.onStop();
    }

    /*public void objectDetect() {
        String object = SerialService.object;
        Log.d(TAG , "넘어왔음 " + object);
        if (object!= null) {
            object = object.replaceAll("\n", "");
            Log.d(TAG, "detect ! " + object);
            String[] detect = object.split(" ");

            String speak = "";
            if (detect.length > 0) {
                for (String s : detect) {
                    if (objectList.contains(s)) {
                        s = s.replaceAll("_", " ");
                        speak += s + " ";
                        Log.d(TAG, "speak : " + s +"!");
                    }
                }
                if (speak != null) {
                    speech("Watch out for " + speak);
                    Log.d(TAG, "Watch out for " + speak);
                }
                SerialService.object = null;
            }
        }
    }*/

    public void locationSetting(){
        //현위치 좌표계 받아오기
        tMapGps = new TMapGpsManager(this);
        tMapGps.setMinTime(1000);
        tMapGps.setMinDistance(2);

        tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);

        /*tMapGps.setProvider(tMapGps.GPS_PROVIDER);*/

        tMapGps.OpenGps();

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

    }

    boolean wrongRoute(double curr_latitude,double curr_longitude){

        double a = GpsToMeter(curr_latitude,curr_longitude,p_latitude,p_longitude);
        double b = GpsToMeter(curr_latitude,curr_longitude,h_latitude,h_longitude);
        double c = GpsToMeter(p_latitude,p_longitude,h_latitude, h_longitude);
        //Log.d("", "meter : " + a +", "+ b + ", "+ c);
        double result = Math.sqrt(a*a - ( Math.pow(a*a-b*b+c*c,2) / (4*c*c) ) );
        //Log.d("Wrong Route?", result+"");
        if(result>=40) return true;
        else return false;
    }


    /*public class ObjectDetect extends Thread{
        @Override
        public void run() {
            while(!currentThread().isInterrupted()){
                Log.d(TAG,"object detect");
                try{
                    synchronized (this){
                        this.wait(1000);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                objectDetect();

            }
            super.run();
        }
    }*/

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
            Log.d(TAG, "경로그리기 with " + "departure : "+startPoint.getLongitude()+", "+ startPoint.getLatitude()+ " / destination : " + endPoint.getLongitude()+", " + endPoint.getLatitude() );

            tMapData.findPathDataAllType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint, new TMapData.FindPathDataAllListenerCallback() {
                @Override
                public void onFindPathDataAll(Document document) {
                    Log.d(TAG,"find path");
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

                    speech(navigation.poll());

                    String peek = coordinates.poll();
                    p_longitude = Double.parseDouble(peek.split(",")[0]);
                    p_latitude = Double.parseDouble(peek.split(",")[1]);

                    peek = coordinates.poll();
                    h_longitude = Double.parseDouble(peek.split(",")[0]);
                    h_latitude = Double.parseDouble(peek.split(",")[1]);


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

    /*public void speech(String text){
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }*/

}

