package com.example.magicstick;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static java.lang.Thread.sleep;

public class NavigationActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback{

    private double longitude;
    private double latitude;
    private static String appKey ="l7xx9ed3bc26b00f404b816bb3b6e2f44ec9";
    private final TMapData tMapData = new TMapData();
    private TMapGpsManager tMapGps = null;
    private TMapView tMapView =null;
    private boolean m_bTrackingMode = true;
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

        String destinatiton =intent.getExtras().getString("destination");
        editText2.setText(destinatiton);


        //현위치 좌표계 받아오기
        tMapGps = new TMapGpsManager(this);
        tMapGps.OpenGps();
        tMapGps.setMinTime(1000);
        tMapGps.setProvider(tMapGps.NETWORK_PROVIDER);
        //tMapGps.setProvider(tMapGps.GPS_PROVIDER);

        editText.setText("현위치");
        TMapPoint startPoint = tMapGps.getLocation();
        Log.d("현위치 좌표계 : ",startPoint.getLatitude()+", "+ startPoint.getLongitude() );

//        TMapPoint startPoint = new TMapPoint(35.171573, 129.176099);


        //장소 입력받아 좌표계 가져오기
        tMapData.findAllPOI(destinatiton, new TMapData.FindAllPOIListenerCallback() {

            @Override
            public void onFindAllPOI(ArrayList<TMapPOIItem> poiItems) {
                Log.d("First poi item : ", poiItems.get(0).getPOIName() + ", Point : " + poiItems.get(0).getPOIPoint().toString());
                longitude = poiItems.get(0).getPOIPoint().getLongitude();
                latitude = poiItems.get(0).getPOIPoint().getLatitude();
                Log.d("POI item : ",latitude+", " + longitude );
                // 통합 리스트 들고오는 부분
                        /*for(int i=0; i<poiItems.size(); i++){
                            TMapPOIItem item = poiItems.get(i);
                            Log.d("POI Name : " , item.getPOIName().toString() + ", " + "Address : " + item.getPOIAddress().replace("null", "")+", Point" + item.getPOIPoint().toString());
                        }*/
            }

        });

        // 보행자 경로탐색
        while(latitude==0 & longitude ==0 & tMapGps.getLocation()!=null){
            try {
                Log.d(getClass().getName(),"wait 1 sec for latitude & longitude");
                sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        TMapPoint endPoint = new TMapPoint(latitude, longitude);
        Log.d("경로그리기 with ", "departure : "+startPoint.getLatitude()+", "+ startPoint.getLongitude()+ " / destination : " + latitude+", " + longitude );
        tMapData.findPathDataAllType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint, new TMapData.FindPathDataAllListenerCallback() {
            @Override
            public void onFindPathDataAll(Document document) {
                Element root = document.getDocumentElement();
                NodeList nodeListPlacemark = root.getElementsByTagName("Placemark");
                for( int i=0; i<nodeListPlacemark.getLength(); i++ ) {
                    NodeList nodeListPlacemarkItem = nodeListPlacemark.item(i).getChildNodes();
                    for( int j=0; j<nodeListPlacemarkItem.getLength(); j++ ) {
                        if( nodeListPlacemarkItem.item(j).getNodeName().equals("description") ) {
                            Log.d("Navigation : ", nodeListPlacemarkItem.item(j).getTextContent().trim() );
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


        // T MAP setting
        tMapView.setCompassMode(true);
        tMapView.setIconVisibility(true);
        tMapView.setZoomLevel(15);
        tMapView.setMapType(TMapView.MAPTYPE_STANDARD);  //일반지도
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN);
        tMapView.setTrackingMode(true);
        tMapView.setSightVisible(true);
        tMapView.setHttpsMode(true);

/*
        tMapView.setLocationPoint(129.176099,35.171573 );
        tMapView.setCenterPoint(129.176099,35.171573 );*/





        mapview.addView(tMapView);
    }


    @Override
    public void onLocationChange(Location location) {
        if(m_bTrackingMode){
            tMapView.setLocationPoint(location.getLongitude(),location.getLatitude());
        }
    }
}

