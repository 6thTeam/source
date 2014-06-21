package com.example.hud;

import java.util.ArrayList;

import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapData.FindAllPOIListenerCallback;
import com.skp.Tmap.TMapData.FindPathDataListenerCallback;
import com.skp.Tmap.TMapMarkerItem;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;
import com.skplanetx.tmapopenapi.LogManager;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MapActivity extends Activity{
	TMapView mMapView;
	TMapData tmapdata;
	TMapPOIItem item = null;
	ArrayList<String> Location_list = new ArrayList<String>();
	ArrayAdapter<String> adapter;
	ListView searchlist;
	
	//위도 경도 배열선언
	ArrayList<Double> lalist = new ArrayList<Double>();
	ArrayList<Double> longlist = new ArrayList<Double>();
	ArrayAdapter<Double> ladapter,longadapter;
	
	// gps관련
	LocationManager locationManager;
	LocationListener sLocationListener;
	String locationProvider;
	Location cur_location;
	TMapPoint Source,Dest;
	
	String TotalData[]; // 메인엑티비티에 전달할 값들
	// [0] : 목적지 명
	// [1] : 목적지 위도
	// [2] : 목적지 경도
	// [3] : 출발지 위도
	// [4] : 출발지 경도
	
	boolean select_offset = false; // 리스트 선택 여부
	boolean gps_receive = false;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.activity_map);
		
		mMapView = new TMapView(this);        // TmapView 객체생성
		RelativeLayout mMainRelativeLayout =(RelativeLayout)findViewById(R.id.map_layout);
		mMainRelativeLayout.addView(mMapView);
		
		
		configureMapView(); // 키값 설정
		
		TotalData = new String[5];
		
		//ListView 추가
		adapter = new ArrayAdapter<String>(this,
					R.layout.list_item, Location_list);
		searchlist = (ListView)findViewById(R.id.searchlist);
		searchlist.setAdapter(adapter);
		
		ladapter = new ArrayAdapter<Double>(this,android.R.layout.simple_list_item_1,lalist);
		longadapter = new ArrayAdapter<Double>(this,android.R.layout.simple_list_item_1,longlist);
		
		// 검색값을 받아옴
		Intent intent = getIntent();
		String strData = intent.getExtras().getString("AreaName");
		
		// POI 통합검색
		tmapdata = new TMapData();
		tmapdata.findAllPOI(strData, new FindAllPOIListenerCallback(){
			public void onFindAllPOI(ArrayList<TMapPOIItem> poiItem){
				for(int i=0; i<poiItem.size(); i++){	
					item = poiItem.get(i);
					Location_list.add(item.getPOIName() +" "+ item.getPOIAddress().replace("null",""));
					lalist.add(item.getPOIPoint().getLatitude());
					longlist.add(item.getPOIPoint().getLongitude());
				}
				
				adapter.notifyDataSetChanged();
				
			}
		});
		// 1초대기 후 리스트뷰 강제 클릭이벤트 수행
		try{Thread.sleep(2000);} catch(InterruptedException e){;}
		searchlist.performClick();
		
		// 예외처리
		if(Location_list.size()==0){
			Location_list.add("검색 결과가 없습니다.");
		}
		
		else{
			searchlist.setOnItemClickListener(mItemClickListener);
		}
		
		GpsSetting();
		
	}
	
	// 버튼 클릭 이벤트
	public void confirm_onClick(View v){
		if(select_offset == false){
			Toast.makeText(MapActivity.this, "목적지를 선택해주세요.", Toast.LENGTH_SHORT).show();
		}
		else{
			Intent intent = new Intent();
			intent.putExtra("Data", TotalData);
			setResult(RESULT_OK, intent); // 메인에 데이터 전달
			finish();
		}
	}
	
	public void cancel_onClick(View v){
		setResult(RESULT_CANCELED);
		finish();
	}
	
	
	// 리스트뷰 셀렉터
	AdapterView.OnItemClickListener mItemClickListener =
			new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long id) {
			// TODO Auto-generated method stub
			double latitude = ladapter.getItem(position); // 위도 저장
			double longitude = longadapter.getItem(position); // 경도 저장
			if(gps_receive == true){
				Source = new TMapPoint(cur_location.getLatitude(),cur_location.getLongitude()); // 출발지 좌표설정
			}else{
				Source = new TMapPoint(37.5666102, 126.9783881); // 출발지 좌표설정
			}
			Dest = new TMapPoint(latitude, longitude); // 목적지 좌표 설정
			//LogManager.printLog("출발지위경도 : "+ Source.getLongitude()+" , "+Source.getLatitude());
			//LogManager.printLog("목적지위경도 : "+ Dest.getLongitude()+" , "+Dest.getLatitude());
			// 지도에 출발지,목적지 선긋기
			tmapdata.findPathData(Source, Dest, new FindPathDataListenerCallback() {
				@Override
				public void onFindPathData(TMapPolyLine polyLine) {
					mMapView.addTMapPath(polyLine);
					
					//경로선의 거리에 따라 지도의 중심에 표시하기 
					if(polyLine.getDistance()<1500){
						mMapView.setZoomLevel(15);
						mMapView.setCenterPoint(((Source.getLongitude()+Dest.getLongitude())/2)
								, ((Source.getLatitude()+Dest.getLatitude())/2)-0.005, false);
					}
					else if(polyLine.getDistance()>1500&&polyLine.getDistance()<5000){
						mMapView.setZoomLevel(14);
						mMapView.setCenterPoint(((Source.getLongitude()+Dest.getLongitude())/2)
								, ((Source.getLatitude()+Dest.getLatitude())/2)-0.01, false);
					}
					else if(polyLine.getDistance()>5000 && polyLine.getDistance()<10000){	
						mMapView.setZoomLevel(13);
						mMapView.setCenterPoint(((Source.getLongitude()+Dest.getLongitude())/2)
								, ((Source.getLatitude()+Dest.getLatitude())/2)-0.01, false);
					}
					else if(polyLine.getDistance()>10000 && polyLine.getDistance()<20000){
						mMapView.setZoomLevel(12);
						mMapView.setCenterPoint(((Source.getLongitude()+Dest.getLongitude())/2)
								, ((Source.getLatitude()+Dest.getLatitude())/2)-0.025, false);
					}
					else if(polyLine.getDistance()>20000 && polyLine.getDistance()<40000){
						mMapView.setZoomLevel(11);
						mMapView.setCenterPoint(((Source.getLongitude()+Dest.getLongitude())/2)
							, ((Source.getLatitude()+Dest.getLatitude())/2)-0.01, false);
					}
					else if(polyLine.getDistance()>30000 && polyLine.getDistance()<60000){
						mMapView.setZoomLevel(10);
						mMapView.setCenterPoint(((Source.getLongitude()+Dest.getLongitude())/2)
								, ((Source.getLatitude()+Dest.getLatitude())/2)-0.07, false);
					}
					else if(polyLine.getDistance()>60000 && polyLine.getDistance()<100000){
						mMapView.setZoomLevel(9);
						mMapView.setCenterPoint(((Source.getLongitude()+Dest.getLongitude())/2)
							, ((Source.getLatitude()+Dest.getLatitude())/2)-0.07, false);
					}
					else if(polyLine.getDistance()>100000 && polyLine.getDistance()<200000){
						mMapView.setZoomLevel(8);
						mMapView.setCenterPoint(((Source.getLongitude()+Dest.getLongitude())/2)
								, ((Source.getLatitude()+Dest.getLatitude())/2)-0.2, false);
					}
					else {
						mMapView.setZoomLevel(7);
						mMapView.setCenterPoint(((Source.getLongitude()+Dest.getLongitude())/2)
								, ((Source.getLatitude()+Dest.getLatitude())/2)-0.3, false);
					}
				}				
			});
			
			// 메인엑티비티에 전달할 값 저장
			TotalData[0] = Location_list.get(position); // 목적지명
			TotalData[1] = Double.toString(latitude); // 목적지 위도
			TotalData[2] = Double.toString(longitude); // 목적지 경도
			if(gps_receive == true){
				TotalData[3] = Double.toString(cur_location.getLatitude()); // 출발지 위도
				TotalData[4] = Double.toString(cur_location.getLongitude()); // 출발지 경도
			}else{
				TotalData[3] = Double.toString(37.5666102); // 출발지 위도
				TotalData[4] = Double.toString(126.9783881); // 출발지 경도
			}
			
			select_offset = true;
		}
	};
	
	// gps세팅
	private void GpsSetting() {
		new Thread() {
			@Override
			public void run() {
				mHandler.sendEmptyMessage(0);
			}
		}.start();
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
	
	// 쓰레드 요청 이벤트 핸들러
	Handler mHandler = new Handler(){
		public void handleMessage(Message msg){
			if(msg.what == 0){ 
				// gps관련
				locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
				sLocationListener = new MyLocationListener();
				locationProvider = locationManager.getBestProvider(new Criteria(), true);
//				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 10,sLocationListener);
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 6000, 10, sLocationListener);
				if(locationManager.getLastKnownLocation(locationProvider)!=null){
					cur_location = locationManager.getLastKnownLocation(locationProvider);
					mMapView.setCenterPoint(cur_location.getLongitude(), cur_location.getLatitude(),true);
					gps_receive = true;
				}else{
					Toast.makeText(getApplicationContext(), "GPS수신이 완료되지 않았습니다. 현재위치를 받아올 수 없습니다.", Toast.LENGTH_LONG).show();
					mMapView.setCenterPoint(126.9783881, 37.5666102 ,true);
					gps_receive = false;
				}
			}
		}
	};
	
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
}
