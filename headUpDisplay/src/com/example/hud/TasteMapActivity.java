package com.example.hud;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapMarkerItem;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapView;
import com.skp.Tmap.TMapView.OnCalloutRightButtonClickCallback;
import com.skp.Tmap.TMapView.OnClickListenerCallback;
import com.skplanetx.tmapopenapi.LogManager;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class TasteMapActivity extends Activity implements OnClickListenerCallback{
	private static final String SOAP_ACTION = "http://jaehwan.com/sayHello";  
	private static final String METHOD_NAME = "sayHello";    
	private static final String NAMESPACE = "http://jaehwan.com"; 
	private static final String URL = "http://119.56.205.175:2200/HUDServer/services/HUDServer?wsdl";
	String str="";

	TMapView mMapView;
	TMapData tmapdata;
	Bitmap bitmap;

	double makerLatitude=0, makerLongitude=0;
	
	TMapPoint[] tpoint = new TMapPoint[10]; 
	TMapMarkerItem[] tItem;
	
	// gps관련
	LocationManager locationManager;
	LocationListener sLocationListener;
	String locationProvider;
	Location cur_location;
	TMapPoint Source,Dest;
	
	DocumentBuilderFactory factory;
	DocumentBuilder builder;
	InputSource source;
	Document doc;
	Element root;
	NodeList items;
	String taste_name;
	
	String TotalData[]; // 메인엑티비티에 전달할 값들
	// [0] : 목적지 명
	// [1] : 목적지 위도
	// [2] : 목적지 경도
	// [3] : 출발지 위도
	// [4] : 출발지 경도
		
	
	Button backBtn, searchBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_taste);
		
		backBtn = (Button)findViewById(R.id.backBtn);
		searchBtn = (Button)findViewById(R.id.searchBtn);
		
		TotalData = new String[5];
		
		searchBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(makerLatitude==0&&makerLongitude==0){
					
					Toast.makeText(getApplicationContext(), "목적지를 지정해주세요.", Toast.LENGTH_LONG).show();
				}else{
					Intent intent = new Intent();
					intent.putExtra("Taste_Data", TotalData);
					setResult(RESULT_OK, intent); // 메인에 데이터 전달
					finish();
				}
			}
		});
		
		backBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		
		mMapView = new TMapView(this);        // TmapView 객체생성
		RelativeLayout mMainRelativeLayout =(RelativeLayout)findViewById(R.id.map_layout);
		mMainRelativeLayout.addView(mMapView);
		configureMapView(); // 키값 설정
		GpsSetting();
		
		bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.icon);
		
		Thread networkThread = new Thread() {  
			@Override   
			public void run() {     
				try {      
					SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);  
					SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);         
					envelope.setOutputSoapObject(request);                    
					HttpTransportSE ht = new HttpTransportSE(URL);          
					ht.call(SOAP_ACTION, envelope);         
					final  SoapPrimitive response = (SoapPrimitive)envelope.getResponse();          
					str = response.toString();         
					runOnUiThread (new Runnable(){      
						public void run() {       
							source = new InputSource();
							source.setCharacterStream(new StringReader(str));
							
							factory = DocumentBuilderFactory.newInstance();
							try {
								builder = factory.newDocumentBuilder();
								doc = builder.parse(source);
								root = doc.getDocumentElement();
								items = root.getElementsByTagName("POINT");
								
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							Node item1 = items.item(0);
							TMapPoint tpoint1 = new TMapPoint(Double.parseDouble(findNode(item1.getFirstChild(),"LATITUDE").getFirstChild().getNodeValue()), 
									Double.parseDouble(findNode(item1.getFirstChild(),"LONGITUDE").getFirstChild().getNodeValue()));
							TMapMarkerItem tItem1 = new TMapMarkerItem();
							tItem1.setTMapPoint(tpoint1);
							tItem1.setIcon(bitmap);
							mMapView.addMarkerItem("Testid1", tItem1);
							tItem1.setName(findNode(item1.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem1.setCanShowCallout(true);//풍선뷰 사용여부
							tItem1.setCalloutTitle(findNode(item1.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem1.setCalloutSubTitle(findNode(item1.getFirstChild(),"DESCRIPTION").getFirstChild().getNodeValue());
							
							Node item2 = items.item(1);
							TMapPoint tpoint2 = new TMapPoint(Double.parseDouble(findNode(item2.getFirstChild(),"LATITUDE").getFirstChild().getNodeValue()), 
									Double.parseDouble(findNode(item2.getFirstChild(),"LONGITUDE").getFirstChild().getNodeValue()));
							TMapMarkerItem tItem2 = new TMapMarkerItem();
							tItem2.setTMapPoint(tpoint2);
							tItem2.setIcon(bitmap);
							mMapView.addMarkerItem("Testid2", tItem2);
							tItem2.setName(findNode(item2.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem2.setCanShowCallout(true);//풍선뷰 사용여부
							tItem2.setCalloutTitle(findNode(item2.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem2.setCalloutSubTitle(findNode(item2.getFirstChild(),"DESCRIPTION").getFirstChild().getNodeValue());
							
							Node item3 = items.item(2);
							TMapPoint tpoint3 = new TMapPoint(Double.parseDouble(findNode(item3.getFirstChild(),"LATITUDE").getFirstChild().getNodeValue()), 
									Double.parseDouble(findNode(item3.getFirstChild(),"LONGITUDE").getFirstChild().getNodeValue()));
							TMapMarkerItem tItem3 = new TMapMarkerItem();
							tItem3.setTMapPoint(tpoint3);
							tItem3.setIcon(bitmap);
							mMapView.addMarkerItem("Testid3", tItem3);
							tItem3.setName(findNode(item3.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem3.setCanShowCallout(true);//풍선뷰 사용여부
							tItem3.setCalloutTitle(findNode(item3.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem3.setCalloutSubTitle(findNode(item3.getFirstChild(),"DESCRIPTION").getFirstChild().getNodeValue());
							
							Node item4 = items.item(3);
							TMapPoint tpoint4 = new TMapPoint(Double.parseDouble(findNode(item4.getFirstChild(),"LATITUDE").getFirstChild().getNodeValue()), 
									Double.parseDouble(findNode(item4.getFirstChild(),"LONGITUDE").getFirstChild().getNodeValue()));
							TMapMarkerItem tItem4 = new TMapMarkerItem();
							tItem4.setTMapPoint(tpoint4);
							tItem4.setIcon(bitmap);
							mMapView.addMarkerItem("Testid4", tItem4);
							tItem4.setName(findNode(item4.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem4.setCanShowCallout(true);//풍선뷰 사용여부
							tItem4.setCalloutTitle(findNode(item4.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem4.setCalloutSubTitle(findNode(item4.getFirstChild(),"DESCRIPTION").getFirstChild().getNodeValue());
							
							Node item5 = items.item(4);
							TMapPoint tpoint5 = new TMapPoint(Double.parseDouble(findNode(item5.getFirstChild(),"LATITUDE").getFirstChild().getNodeValue()), 
									Double.parseDouble(findNode(item5.getFirstChild(),"LONGITUDE").getFirstChild().getNodeValue()));
							TMapMarkerItem tItem5 = new TMapMarkerItem();
							tItem5.setTMapPoint(tpoint5);
							tItem5.setIcon(bitmap);
							mMapView.addMarkerItem("Testid5", tItem5);
							tItem5.setName(findNode(item5.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem5.setCanShowCallout(true);//풍선뷰 사용여부
							tItem5.setCalloutTitle(findNode(item5.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem5.setCalloutSubTitle(findNode(item5.getFirstChild(),"DESCRIPTION").getFirstChild().getNodeValue());
							
							Node item6 = items.item(5);
							TMapPoint tpoint6 = new TMapPoint(Double.parseDouble(findNode(item6.getFirstChild(),"LATITUDE").getFirstChild().getNodeValue()), 
									Double.parseDouble(findNode(item6.getFirstChild(),"LONGITUDE").getFirstChild().getNodeValue()));
							TMapMarkerItem tItem6 = new TMapMarkerItem();
							tItem6.setTMapPoint(tpoint6);
							tItem6.setIcon(bitmap);
							mMapView.addMarkerItem("Testid6", tItem6);
							tItem6.setName(findNode(item6.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem6.setCanShowCallout(true);//풍선뷰 사용여부
							tItem6.setCalloutTitle(findNode(item6.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem6.setCalloutSubTitle(findNode(item6.getFirstChild(),"DESCRIPTION").getFirstChild().getNodeValue());
							
							Node item7 = items.item(6);
							TMapPoint tpoint7 = new TMapPoint(Double.parseDouble(findNode(item7.getFirstChild(),"LATITUDE").getFirstChild().getNodeValue()), 
									Double.parseDouble(findNode(item7.getFirstChild(),"LONGITUDE").getFirstChild().getNodeValue()));
							TMapMarkerItem tItem7 = new TMapMarkerItem();
							tItem7.setTMapPoint(tpoint7);
							tItem7.setIcon(bitmap);
							mMapView.addMarkerItem("Testid7", tItem7);
							tItem7.setName(findNode(item7.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem7.setCanShowCallout(true);//풍선뷰 사용여부
							tItem7.setCalloutTitle(findNode(item7.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem7.setCalloutSubTitle(findNode(item7.getFirstChild(),"DESCRIPTION").getFirstChild().getNodeValue());
							
							Node item8 = items.item(7);
							TMapPoint tpoint8 = new TMapPoint(Double.parseDouble(findNode(item8.getFirstChild(),"LATITUDE").getFirstChild().getNodeValue()), 
									Double.parseDouble(findNode(item8.getFirstChild(),"LONGITUDE").getFirstChild().getNodeValue()));
							TMapMarkerItem tItem8 = new TMapMarkerItem();
							tItem8.setTMapPoint(tpoint8);
							tItem8.setIcon(bitmap);
							mMapView.addMarkerItem("Testid8", tItem8);
							tItem8.setName(findNode(item8.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem8.setCanShowCallout(true);//풍선뷰 사용여부
							tItem8.setCalloutTitle(findNode(item8.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem8.setCalloutSubTitle(findNode(item8.getFirstChild(),"DESCRIPTION").getFirstChild().getNodeValue());
							
							Node item9 = items.item(8);
							TMapPoint tpoint9 = new TMapPoint(Double.parseDouble(findNode(item9.getFirstChild(),"LATITUDE").getFirstChild().getNodeValue()), 
									Double.parseDouble(findNode(item9.getFirstChild(),"LONGITUDE").getFirstChild().getNodeValue()));
							TMapMarkerItem tItem9 = new TMapMarkerItem();
							tItem9.setTMapPoint(tpoint9);
							tItem9.setIcon(bitmap);
							mMapView.addMarkerItem("Testid9", tItem9);
							tItem9.setName(findNode(item9.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem9.setCanShowCallout(true);//풍선뷰 사용여부
							tItem9.setCalloutTitle(findNode(item9.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem9.setCalloutSubTitle(findNode(item9.getFirstChild(),"DESCRIPTION").getFirstChild().getNodeValue());
							
							Node item10 = items.item(9);
							TMapPoint tpoint10 = new TMapPoint(Double.parseDouble(findNode(item10.getFirstChild(),"LATITUDE").getFirstChild().getNodeValue()), 
									Double.parseDouble(findNode(item10.getFirstChild(),"LONGITUDE").getFirstChild().getNodeValue()));
							TMapMarkerItem tItem10 = new TMapMarkerItem();
							tItem10.setTMapPoint(tpoint10);
							tItem10.setIcon(bitmap);
							mMapView.addMarkerItem("Testid10", tItem10);
							tItem10.setName(findNode(item10.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem10.setCanShowCallout(true);//풍선뷰 사용여부
							tItem10.setCalloutTitle(findNode(item10.getFirstChild(),"NAME").getFirstChild().getNodeValue());
							tItem10.setCalloutSubTitle(findNode(item10.getFirstChild(),"DESCRIPTION").getFirstChild().getNodeValue());
							
						}       
					});
				}      
				catch (Exception e) {  
					e.printStackTrace();   
				}   
			} 
		};  
		networkThread.start();
		
		
	}
	
	// 노드 찾기
	Node findNode(Node node, String node_name){
		while(!node.getNodeName().equals(node_name)){
			node = node.getNextSibling();
		}
		return node;
	}
		 
	private void GpsSetting() {
		// gps관련
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		sLocationListener = new MyLocationListener();
		locationProvider = locationManager.getBestProvider(new Criteria(), true);		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 10,sLocationListener);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 0, sLocationListener);
		cur_location = locationManager.getLastKnownLocation(locationProvider);
		//36.14521891466144, 128.39462828325208
		mMapView.setCenterPoint(128.41616709999994, 36.1395032);
		mMapView.setZoomLevel(14);
		
	}
	class MyLocationListener implements LocationListener{
		@Override
		public void onLocationChanged(Location arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub
			
		}
	}
		// 키값 세팅쓰레드
		private void configureMapView() {
			new Thread() {
				@Override
				public void run() {
					mMapView.setSKPMapApiKey("c617d098-3020-3ec6-ae04-24835166c225");
				}
			}.start();
		}
		@Override
		public boolean onPressEvent(ArrayList<TMapMarkerItem> arg0,
				ArrayList<TMapPOIItem> arg1, TMapPoint arg2, PointF arg3) {
			try {
				// TODO Auto-generated method stub
				makerLatitude = arg0.get(0).getTMapPoint().getLatitude();
				makerLongitude = arg0.get(0).getTMapPoint().getLongitude();
				taste_name = arg0.get(0).getName();
				
				TotalData[0] = arg0.get(0).getName();
				TotalData[1] = Double.toString(makerLatitude);
				TotalData[2] = Double.toString(makerLongitude);
				TotalData[3] = Double.toString(cur_location.getLatitude());
				TotalData[4] = Double.toString(cur_location.getLongitude());
				
				
				
				//Toast.makeText(getApplicationContext(), taste_name, Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}
		@Override
		public boolean onPressUpEvent(ArrayList<TMapMarkerItem> arg0,
				ArrayList<TMapPOIItem> arg1, TMapPoint arg2, PointF arg3) {
			// TODO Auto-generated method stub
			return false;
		}
		
}