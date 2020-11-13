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


public class NavigationActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback{


    private static String appKey ="l7xx9ed3bc26b00f404b816bb3b6e2f44ec9";
    private final TMapData tMapData = new TMapData();
    private TMapGpsManager tMapGps = null;
    private TMapView tMapView =null;
    private boolean m_bTrackingMode = true;
    public static boolean stopFlag=false;
    FindPath findPath = new FindPath();

    TMapPoint startPoint;
    TMapPoint endPoint;
    boolean nullLocation=true;

    final String TAG = getClass().getName();
    private Intent serviceIntent;
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

        locationSetting();

        serviceIntent = new Intent(NavigationActivity.this , NavigationService.class);

        mapview.addView(tMapView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nullLocation=true;
    }

    @Override
    public void onLocationChange(Location location) {
        Log.d(TAG, "Location Changed ! index: "+ nullLocation );
        if(m_bTrackingMode){
            tMapView.setLocationPoint(location.getLongitude(),location.getLatitude());
        }

        if(nullLocation) {
            startPoint = new TMapPoint(location.getLatitude(), location.getLongitude());
            Thread thread = new Thread(findPath);
            thread.start();
            nullLocation=false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        stopFlag=true;
        stopService(serviceIntent);
        tMapGps.CloseGps();
        super.onBackPressed();
    }

    public void locationSetting(){
        //현위치 좌표계 받아오기
        tMapGps = new TMapGpsManager(this);
        tMapGps.setMinTime(1000);
        tMapGps.setMinDistance(1);

        /*tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);*/

        tMapGps.setProvider(tMapGps.GPS_PROVIDER);
        /*if(tMapGps.getLocation()==new TMapPoint(0,0)){
            tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);
        }*/

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
                    Log.d(TAG,"find path");
                    Element root = document.getDocumentElement();

                    NodeList nodeListPlacemark = root.getElementsByTagName("Placemark");
                    Log.d(TAG,"Root element :" + root.getNodeName());

                    /*LinkedList<String> coordinates = new LinkedList<String>();
                    LinkedList<String> navigation = new LinkedList<String>();*/


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
                    /*serviceIntent.putExtra("Coordinate", coordinates);
                    serviceIntent.putExtra("Navigation", coordinates);*/
                    startService(serviceIntent);
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

}

