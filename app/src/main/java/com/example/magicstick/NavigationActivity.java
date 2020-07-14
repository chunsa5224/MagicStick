package com.example.magicstick;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.widget.EditText;
import android.widget.LinearLayout;

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


public class NavigationActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback{


    private static String appKey ="l7xx9ed3bc26b00f404b816bb3b6e2f44ec9";
    private final TMapData tMapData = new TMapData();
    private TMapGpsManager tMapGps = null;
    private TMapView tMapView =null;
    private boolean m_bTrackingMode = true;
    final String TAG = getClass().getName();
    TMapPoint startPoint;
    TMapPoint endPoint;
    Handler handler = new Handler();
    Runnable runnable;
    int index=-1;
    String [] navigation ;
    String [] coordinates;
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


        editText.setText("현위치");

        startPoint = tMapGps.getLocation();


//        Log.d(TAG,"현위치 좌표계 : " + startPoint.getLatitude()+", "+ startPoint.getLongitude() );

       // startPoint = new TMapPoint(35.171573, 129.176099);


        //장소 입력받아 좌표계 가져오기
        tMapData.findAllPOI(destination, new TMapData.FindAllPOIListenerCallback() {

            @Override
            public void onFindAllPOI(ArrayList<TMapPOIItem> poiItems) {
                Log.d(TAG, "First poi item : "+poiItems.get(0).getPOIName() + ", Point : " + poiItems.get(0).getPOIPoint().toString());
                endPoint = poiItems.get(0).getPOIPoint();
                Log.d(TAG,"POI item : " + endPoint.getLatitude()+", " + endPoint.getLongitude());
                findPath(startPoint,endPoint);
                // 통합 리스트 들고오는 부분
                        /*for(int i=0; i<poiItems.size(); i++){
                            TMapPOIItem item = poiItems.get(i);
                            Log.d("POI Name : " , item.getPOIName().toString() + ", " + "Address : " + item.getPOIAddress().replace("null", "")+", Point" + item.getPOIPoint().toString());
                        }*/

            }

        });

        // 보행자 경로탐색

        //new Waiter().execute();
        //Log.d(TAG,"Waiter 끝! 현위치 좌표계 : " + startPoint.getLatitude()+", "+ startPoint.getLongitude() );


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
        if(m_bTrackingMode){
            tMapView.setLocationPoint(location.getLongitude(),location.getLatitude());
        }
        if(index==-1){
            startPoint = new TMapPoint(location.getLatitude(), location.getLongitude());
            index++;
        }

        if(coordinates!=null){
            Log.d(TAG,"coordinates : " + coordinates[0]);
            if(location.getLatitude() == Double.parseDouble(coordinates[index].split(",")[0])
                    && location.getLongitude() == Double.parseDouble(coordinates[index].split(",")[1]) ){
                Log.d(TAG, "Current Navigation : " + navigation[index]);
                index++;
            }
        }



        Log.d(TAG, "onLocationChange : " + startPoint.getLatitude() +" , " + startPoint.getLongitude());
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

                    coordinates=new String [nodeListPlacemarkItem.getLength()];
                    navigation=new String [nodeListPlacemarkItem.getLength()];

                    for( int j=0; j<nodeListPlacemarkItem.getLength(); j++ ) {
                        if( nodeListPlacemarkItem.item(j).getNodeName().equals("Point") ) {
                            String n = nodeListPlacemarkItem.item(j).getTextContent().trim();
                            Log.d(TAG, "location : " + n);
                            coordinates[j]=n;
                        }
                         if( nodeListPlacemarkItem.item(j).getNodeName().equals("description") ) {
                             String n = nodeListPlacemarkItem.item(j).getTextContent().trim();
                             Log.d(TAG, "Navigation : " + n);
                             navigation[j] = n;
                        }
                    }

                }

            }

        });

        tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine polyLine) {
                polyLine.setLineWidth(4);
                polyLine.setLineColor(Color.BLUE);
                tMapView.addTMapPath(polyLine);
            }
        });
    }

    class Waiter extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "Waiter");
            while(startPoint==null || endPoint == null){
                try{Thread.sleep(1000); Log.d(TAG, "wait for GPS...");}catch (Exception e){Log.d(TAG,"error in water");}
            }
            handler.post(runnable);
            findPath(startPoint, endPoint);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid){
            super.onPostExecute(aVoid);
        }
    }

}

