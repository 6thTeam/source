package com.example.hud;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.example.hud.R;
import com.google.common.collect.SetMultimap;
import com.skplanetx.tmapopenapi.LogManager;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
	

	// gps 속도,이동거리 관련멤버
	LocationManager manager;
	LocationManager manager2;
	LocationListener guideListener;
	LocationListener checkListener;
	String provider;
	String provider2;
	TextView speedtxt;
	TextView distancetxt;
	
	// 방향센서 관련 멤버
	SensorManager mSm;
    Compass compass;
    CompassSetting compasssetting;
    int azimuthValue; // 값 저장용
    
	
	SeekBar mSeekBar; // 밝기조절바
	
	// 남은시간, 목적지명, 가이드
	TextView remainDistancetxt;
	TextView destinationtxt;
	TextView guidetxt;
	
	// 현재시간, 경과시간,목적지이름, 시속단위,
	//운전시간이름,이동거리이름,남은거리이름
	String passTime;
	TextView ptimetxt;
	String currentTime;
	TextView ctimetxt;
	TextView destination_name;
	TextView speed_measure;
	TextView running_name;
	TextView total_name;
	TextView remain_name;
	
	ImageView direction_image; // 방향표시 이미지
	
	boolean hud_mode=false; // hud모드 오프셋
	
	boolean designate_destination = false; // 목적지 설정여부 오프셋
	
	String[] TotalData; // 맵 엑티비티에서 전달받을 값 저장
	// [0] : 목적지 명
	// [1] : 목적지 위도
	// [2] : 목적지 경도
	// [3] : 출발지 위도
	// [4] : 출발지 경도
	
	String[] start_coordinate; // 좌표 정보 저장
	String coordinate_type; // 좌표 타입 저장
	String next_coordinate_type = null; // 다음 좌표 타입 저장
	int guide_index = 0;
	GuideDirection guide;
	
	final static int GET_AREA=0;
	final static int GET_DATA=1;
	final static int GET_COLOR=2;
	final static int GET_TASTE=3;
	
	double curLatitude = 0;
	double curLongitude = 0;
	
	//tts, 색상변경 변수 선언
	TextToSpeech tts;
	public static final int SET_COLOR = Menu.FIRST+1;
	
	boolean tts_once_perform_fact = false;
	int guideValue = 67; // 가리킬 방향
	
	PathRestart pathrestart; //  경로 재설정 쓰레드
	
	String storage_turn_type = null; // 턴타입 정보 전역 저장
	String show_turn_type = null; // 실제로 출력할 턴타입
	ShowTurnType showturntype;
	boolean show_priority = false;
	
	int intentcolor = 0xffffffff;
	boolean left_right_direction_visible = true;
	
	//뒤로 2번클릭종료 클래스
	BackPressCloseHandler backPressCloseHandler;
	boolean turnState=false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		
		ctimetxt = (TextView)findViewById(R.id.current_time);
		ptimetxt = (TextView)findViewById(R.id.running_time);
		mSeekBar = (SeekBar)findViewById(R.id.horizontal_seekbar);
		speedtxt = (TextView)findViewById(R.id.speed);
		distancetxt = (TextView)findViewById(R.id.total_distance);
		remainDistancetxt = (TextView)findViewById(R.id.remain_distance);
		destinationtxt = (TextView)findViewById(R.id.destination);
		guidetxt = (TextView)findViewById(R.id.guide);
		direction_image = (ImageView)findViewById(R.id.img);
		destination_name = (TextView)findViewById(R.id.destination_name);;
		speed_measure = (TextView)findViewById(R.id.speed_measure);
		running_name = (TextView)findViewById(R.id.running_name);
		total_name = (TextView)findViewById(R.id.total_name);
		remain_name = (TextView)findViewById(R.id.remain_name);
		
		//뒤로 2번클릭 종료 객체생성
		backPressCloseHandler = new BackPressCloseHandler(this);
		
		// 현재시간, 경과시간, gps 쓰레드 수행
		CurrentTime currentTime = new CurrentTime();
		currentTime.start();
		PassTime passTime = new PassTime();
		passTime.start();
		GpsCheck speedcheck = new GpsCheck();
		speedcheck.start();
		GuideCheck gpssetting = new GuideCheck();
		gpssetting.start();
		//GPS켜져있는지 확인 및 세팅
		enableGPSSetting();
		
		//처음 어플시작시 tts실행
		OnInitListener MyTTSListener = new OnInitListener() {
			@Override
			public void onInit(int status) {
//				tts.speak("TTS 어플을 시작합니다.", TextToSpeech.QUEUE_FLUSH, null);
			}
		};
		//tts추가
		tts = new TextToSpeech(this, MyTTSListener);
		tts.setLanguage(Locale.KOREAN);
		tts.setPitch(1.0f);//목소리톤
		tts.setSpeechRate(0.9f);//목소리속도
		
		// 밝기조절바 이벤트 핸들러
		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

    		@Override
    		public void onStopTrackingTouch(SeekBar seekBar) {
    		}

    		@Override
    		public void onStartTrackingTouch(SeekBar seekBar) {
    			
    		}

    		// 이곳에 작성
    		@Override
    		public void onProgressChanged(SeekBar seekBar, int progress,
    				boolean fromUser) {
    			Settings.System.putInt(getContentResolver(), "screen_brightness", progress);
    		}
    	});
		
	}
	
	private void enableGPSSetting() {
		ContentResolver res = getContentResolver();
		//GPS가 켜져있는 확인
		boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(res,LocationManager.GPS_PROVIDER);
		if(!gpsEnabled){
			new AlertDialog.Builder(this)
			.setTitle("GPS설정").setMessage("GPS가 꺼져 있습니다.\nGPS를 켜시겠습니까?")
			.setPositiveButton("GPS켜기", new DialogInterface.OnClickListener() {
				//GPS설정화면으로 띄웁니다.
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent= new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(intent);
				}
			})
			.setNegativeButton("어플종료", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			})
			.setCancelable(false).show();
		}
	}

	// 버튼 이벤트
	public void setDestinationBtn_onClick(View v){
		// 목적지 설정 엑티비티 호출
		Intent intent = new Intent(MainActivity.this, DestinationActivity.class);
		startActivityForResult(intent,GET_AREA); // 목적지 설정 엑티비티 종료시 결과 값 요구. 키워드는 GET_AREA.
	}
	
	
	protected void onActivityResult (int requestCode, int resultCode, Intent data){
		switch (requestCode){
		case GET_AREA: // 목적지 설정 엑티비티 종료 시 호출
			if (resultCode == RESULT_OK){
				// 지도 엑티비티 호출. 호출시 목적지 명을 전달함.
				Intent intent = new Intent(MainActivity.this, MapActivity.class);
				intent.putExtra("AreaName", data.getStringExtra("SearchName"));
				startActivityForResult(intent, GET_DATA); // 지도 엑티비티 종료시 결과 값 요구. 키워드는 GET_DATA.	
			}
			break;
		case GET_DATA: // 맵 엑티비티 종료 시 호출
			if (resultCode == RESULT_OK){
				// 얻어온 데이터를 뿌린다.
				TotalData = data.getStringArrayExtra("Data");
				
				destinationtxt.setText(TotalData[0]);
			//	TotalData[5]=Integer.toString(1);
				guide = new GuideDirection(); // 방향안내 객체
				guide.Init(TotalData); // 초기화
				
				//guide_index = 1;
				guide_index = 0;
				start_coordinate = guide.GetCoordinate(++guide_index);
				coordinate_type = guide.GetCoordinateType();
				
				tts_once_perform_fact = false;
				
				compasssetting = new CompassSetting(); // 나침반 쓰레드 시작
				compasssetting.start();
				
				pathrestart = new PathRestart(); // 경로이탈 점검 쓰레드 시작
				pathrestart.start();
				
				show_priority=false;
				turnState = false;
				designate_destination = true;
				left_right_direction_visible = true;
			
				direction_image.setColorFilter(intentcolor);
			}
			break;
		case GET_COLOR: // 색상변경 엑티비티 종료시 호출
			if(resultCode==RESULT_OK){
				//ColorActivity에서 얻어온 데이터를 저장
				intentcolor = data.getExtras().getInt("colorData");
				//모든 색깔을 변경
				speedtxt.setTextColor(intentcolor);
				remainDistancetxt.setTextColor(intentcolor);
				destinationtxt.setTextColor(intentcolor);
				guidetxt.setTextColor(intentcolor);
				ptimetxt.setTextColor(intentcolor);
				ctimetxt.setTextColor(intentcolor);
				distancetxt.setTextColor(intentcolor);
				destination_name.setTextColor(intentcolor);
				speed_measure.setTextColor(intentcolor);
				running_name.setTextColor(intentcolor);
				total_name.setTextColor(intentcolor);
				remain_name.setTextColor(intentcolor);
				direction_image.setColorFilter(intentcolor);
			}
			break;
		case GET_TASTE:
			if (resultCode == RESULT_OK){
				TotalData = data.getStringArrayExtra("Taste_Data");
				
				destinationtxt.setText(TotalData[0]);
				
				guide = new GuideDirection(); // 방향안내 객체
				guide.Init(TotalData); // 초기화
				
				//guide_index = 1;
				guide_index = 0;
				start_coordinate = guide.GetCoordinate(++guide_index);
				coordinate_type = guide.GetCoordinateType();
				
				tts_once_perform_fact = false;
				
				compasssetting = new CompassSetting(); // 나침반 쓰레드 시작
				compasssetting.start();
				
				pathrestart = new PathRestart(); // 경로이탈 점검 쓰레드 시작
				pathrestart.start();
				
				show_priority=false;
				turnState = false;
				designate_destination = true;
				left_right_direction_visible = true;
				
				direction_image.setColorFilter(intentcolor);
			}
		break;
		}
		
	}
	
	// hud 모드
	public void hudBtn_onClick(View v){
		if(hud_mode==false){
			((ReverseAbleRelativeLayout)findViewById(R.id.reverse_layer)).setReverse(true);
			
			hud_mode=true;
		}
		else if(hud_mode==true){
			((ReverseAbleRelativeLayout)findViewById(R.id.reverse_layer)).setReverse(false);
			hud_mode=false;
		}
	}
	//맛집검색모드
	public void setTasteBtn_onClick(View v){
		Intent intent = new Intent(MainActivity.this, TasteMapActivity.class);
		startActivityForResult(intent,GET_TASTE); // 목적지 설정 엑티비티 종료시 결과 값 요구. 키워드는 GET_AREA.
		
		//Intent intent1 = new Intent(MainActivity.this, TasteMapActivity.class);
		//startActivity(intent1); 
	}
	
	/////////////////////// 방향 측정을 위한 쓰레드 //////////////////////////////////
	class CompassSetting extends Thread{
		public void run(){
			mHandler.sendEmptyMessage(3);
		}
	}
	///////////////////////////////////////////
	
	/////////////////////// 턴타입 출력을 위한 쓰레드 //////////////////////////////////
	class ShowTurnType extends Thread{
		int blink;
		public void run(){
			blink = 0;
			for(int i=0;i<6;i++){
				SetTurnType(blink);
				mHandler.sendEmptyMessage(5);
				blink = 1 - blink;
				try{Thread.sleep(500);} catch(InterruptedException e){;}
			}
			if(turnState==true){
				show_priority = true;
			}else{
				show_priority=false;
			}
		}
	}
	
	void SetTurnType(int blink){
		if(blink == 0){
			show_turn_type = "";
		}else{
			show_turn_type = storage_turn_type;
		}
	}
	///////////////////////////////////////////////////////////////////////////
	
	/////////////////////// 방향 표시를 위한 gps세팅 쓰레드 //////////////////////
	class GuideCheck extends Thread{
		public void run(){
			//try{Thread.sleep(1500);} catch(InterruptedException e){;}
			mHandler.sendEmptyMessage(2);
		}
	}
	//////////////////////////////////////////////////////////////////////////
	
	/////////////////// 현재시간 출력 쓰레드 //////////////////////////////
	class CurrentTime extends Thread{
		GetCurrentTime gettime = new GetCurrentTime();
		public void run(){
			int blink = 0; // 깜빡임 오프셋
			Calendar cal;
			while (true){
				cal = new GregorianCalendar(); // 현재 날짜를 받아옴
				currentTime = gettime.GetTime(cal,blink);
				mHandler.sendEmptyMessage(1);
				try{Thread.sleep(1000);} catch(InterruptedException e){;}
				blink = 1-blink;
			}
		}
	}
	/////////////////////////////////////////////////////////////////
	
	/////////////////// 경과시간 출력 쓰레드 //////////////////////////////
	class PassTime extends Thread{
		public void run(){
			long time = 0;
			while (true){
				time++;
				passTime = UpTime(time);
				mHandler.sendEmptyMessage(0);
				try{Thread.sleep(1000);} catch(InterruptedException e){;}
			}
		}
	}
	
	String UpTime(long msec){
		String result;
		result = String.format("%d시 %d분 %d초", 
				msec/3600%24, msec/60%60, msec%60);
		return result;
	}
	///////////////////////////////////////////////////////////////////
	
	/////////////////////경로이탈 재설정///////////////////////////////
	class PathRestart extends Thread{
		public void run(){
			while (true){
				mHandler.sendEmptyMessage(4);
				try{Thread.sleep(6000);} catch(InterruptedException e){;}
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////
	
	class GpsCheck extends Thread{
		public void run(){
			mHandler.sendEmptyMessage(6);
		}
	}
	
	// 쓰레드 요청 이벤트 핸들러
	Handler mHandler = new Handler(){
		public void handleMessage(Message msg){
			if(msg.what == 0){ // 경과시간 쓰레드 요청 처리
				ptimetxt.setText(passTime);
			}
			if(msg.what == 1){ // 현재시간 쓰레드 요청 처리
				ctimetxt.setText(currentTime);
			}
			if(msg.what == 2){ // gps 쓰레드 요청 처리
				manager2 = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
				guideListener = new GuideListener();
				
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				criteria.setAltitudeRequired(true);
				criteria.setBearingRequired(true);
				criteria.setSpeedRequired(true);
				
				provider2 = manager2.getBestProvider(criteria, true);
				manager2.requestLocationUpdates(provider2, 5, 0, guideListener);
			}
			if(msg.what == 3){ // 나침반 쓰레드 처리
				compass = new Compass();
				mSm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		        mSm.registerListener(compass, mSm.getDefaultSensor(
						Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
			}
			if(msg.what == 4){ // 경로이탈시 재설정 처리
				if(guideValue >= 125 && guideValue <= 235){
					guidetxt.setText("경로이탈. 경로재설정");
					tts.speak("경로를 재설정 합니다.", TextToSpeech.QUEUE_FLUSH, null);
					tts_once_perform_fact = false;
				
					TotalData[3]=Double.toString(curLatitude);
					TotalData[4]=Double.toString(curLongitude);
			
					guide.Init(TotalData); // 초기화				
					//guide_index = 1;
					guide_index = 0;
					start_coordinate = guide.GetCoordinate(++guide_index);
					coordinate_type = guide.GetCoordinateType();
					show_priority=false;
					turnState=false;
					//direction_image.setImageResource(R.drawable.wrong);
				}
			}
			if(msg.what == 5){
				if(show_turn_type.equals("")){
					direction_image.setColorFilter(Color.BLACK);
					direction_image.setImageResource(R.drawable.blink);
				}else if(show_turn_type.equals("좌회전")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.leftturn);
				}else if(show_turn_type.equals("우회전")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.rightturn);
				}else if(show_turn_type.equals("유턴")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.uturn);
				}else if(show_turn_type.equals("8시 방향 좌회전")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.leftturndown);
				}else if(show_turn_type.equals("10시 방향 좌회전")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.leftturnup);
				}else if(show_turn_type.equals("2시 방향 우회전")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.rightturnup);
				}else if(show_turn_type.equals("4시 방향 우회전")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.rightturndown);
				}else if(show_turn_type.startsWith("좌측")||show_turn_type.equals("9시 방향")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.left);
				}else if(show_turn_type.startsWith("우측")||show_turn_type.equals("3시 방향")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.right);
				}else if(show_turn_type.equals("1시 방향")||show_turn_type.equals("2시 방향")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.rightup);
				}else if(show_turn_type.equals("4시 방향")||show_turn_type.equals("5시 방향")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.rightdown);
				}else if(show_turn_type.equals("6시 방향")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.uturn);
				}else if(show_turn_type.equals("7시 방향")||show_turn_type.equals("8시 방향")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.leftdown);
				}else if(show_turn_type.equals("10시 방향")||show_turn_type.equals("11시 방향")){
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.leftup);
				}else{
					direction_image.setColorFilter(intentcolor);
					direction_image.setImageResource(R.drawable.up);
				}
			}
			if(msg.what == 6){
				manager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
				checkListener = new CheckListener();
				
				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				criteria.setAltitudeRequired(true);
				criteria.setBearingRequired(true);
				criteria.setSpeedRequired(true);
				
				provider = manager.getBestProvider(criteria, true);
				manager.requestLocationUpdates(provider, 5, 0, checkListener);
			}
		}
	};
	
	// 속도,이동거리,남은거리 측정 리스너
	class CheckListener implements LocationListener
	{
		float Speed; // 속도값 저장
		String Speedvalue; // 속도 텍스트뷰에 출력용
		
		double tmpDistance = 0.0; // 임시 이동거리 저장용
		double totalDistance = 0.0; // 총 이동거리 저장
		String Distancevalue; // 이동거리 텍스트뷰에 출력용
		
		// 이동거리 계산용
		double endLatitude = 0.0; // 최신 위도
		double endLongitude = 0.0; // 최신 경도
		double startLatitude = 0.0; // 이전 위도
		double startLongitude = 0.0; // 이전 경도
		double latitude, longitude; // 현재 위도,경도값 저장용
		float[] arrayDistance = new float[2]; // 거리 값
		
		// 남은거리 계산용
		double desLatitude = 0;
		double desLongitude = 0;
		float[] arrayDistance2 = new float[2];
		double tmpDistance2 = 0;
		int remainDistance = 0;
		float[] arrayDistance3 = new float[2];
		
		GetDistance getdistance = new GetDistance();
		
		public void onLocationChanged(Location location)
		{
			// 속도 측정
			Speed = location.getSpeed(); // m/s로 받아옴
			Speed *=3.6; // km/h로 반환
			Speedvalue = String.format("%.0f", Speed); // 반올림
			speedtxt.setText(Speedvalue); 
						
			// 이동거리 측정
			latitude = location.getLatitude();
			longitude = location.getLongitude();
						
			startLatitude = endLatitude;
			startLongitude = endLongitude;
			endLatitude = latitude;
			endLongitude = longitude;
			//Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, arrayDistance);
			//tmpDistance = arrayDistance[0];
			tmpDistance = getdistance.distance(startLatitude, startLongitude, endLatitude, endLongitude);
			if (tmpDistance > 0 && tmpDistance < 50){ // 값이 튀는것을 방지
				totalDistance += tmpDistance;
				Distancevalue = String.format("%.0f", totalDistance);
				distancetxt.setText(Distancevalue+" m");
			}
			
			// 목적지 지정이 끝난상태이면..
			if(designate_destination == true){
				desLatitude = Double.parseDouble(TotalData[1]);
				desLongitude = Double.parseDouble(TotalData[2]);
				curLatitude = location.getLatitude();
				curLongitude = location.getLongitude();
							
				// 남은거리 측정
				//Location.distanceBetween(curLatitude, curLongitude, desLatitude, desLongitude, arrayDistance2);
				//tmpDistance2 = arrayDistance2[0];
				tmpDistance2 = getdistance.distance(curLatitude, curLongitude, desLatitude, desLongitude);
				if(tmpDistance2 >= 1000){
					remainDistance = (int)(tmpDistance2 / 1000);
					remainDistancetxt.setText(Integer.toString(remainDistance)+"km");
				}
				else{
					remainDistance = (int)tmpDistance2;
					remainDistancetxt.setText(Integer.toString(remainDistance)+"m");
				}
			}
		}
		public void onProviderDisabled(String provider)
		{//사용할수 없게 된다면
			// TODO Auto-generated method stub
			
		}
		
		public void onProviderEnabled(String provider)
		{//사용 가능하게 되면
			// TODO Auto-generated method stub
		}
		
		public void onStatusChanged(String provider, int status, Bundle extras)
		{//상태값이 바뀌게 되면
			// TODO Auto-generated method stub
		}
	}
	
	// 방향 표시 리스너
	class GuideListener implements LocationListener
	{
		//방위 계산용
		int bearingValue; // 방위값
		GetBearing bearing = new GetBearing();
		double lineLatitude;
		double lineLongitude;
		double pointLatitude;
		double pointLongitude;
		String[] pointCoordinate;
		int coordinate_index=2;
		
		
		double pointDistance = 0;
		
		float[] arrayDistance4 = new float[2];
		double lineDistance;
		int perform_count=0;
		//방향타입
		String turntype;
		
		GetDistance getdistance = new GetDistance();
		
		public void onLocationChanged(Location location)
		{	
			// 목적지 지정이 끝난상태이면..
			if(designate_destination == true){
				if(coordinate_type.equals("Point")){ // 포인트 타입일 경우
					bearingValue = bearing.bearingP1toP2(curLatitude, curLongitude, lineLatitude, lineLongitude);
					pointCoordinate = guide.GetPointCoordinate(guide_index);
					pointLatitude = Double.parseDouble(pointCoordinate[1]);//point 위경도 저장
			    	pointLongitude = Double.parseDouble(pointCoordinate[0]);
					next_coordinate_type = guide.GetTurnType(guide_index); // 턴타입을 얻어옴
					turntype = guide.TranslateTurnType(next_coordinate_type);
					pointDistance = getdistance.distance(curLatitude, curLongitude, pointLatitude, pointLongitude);
					
					// 수행카운트 초기화
					if(tts_once_perform_fact == false){
						if(pointDistance>300&&pointDistance<=400){
							perform_count = 3;
							tts_once_perform_fact = true;
						}else if(pointDistance>100&&pointDistance<=200){
							perform_count = 2;
							tts_once_perform_fact = true;
						}else if(pointDistance>0&&pointDistance<=85){
							perform_count = 1;
							tts_once_perform_fact = true;
						}
						
					}
					
					//각각의 거리에서 앞에서 텍스트뷰와 tts로 방향표시 
					if(pointDistance>300&&pointDistance<=400&&perform_count==3&&(guideValue >= 295 || guideValue <= 65 || guideValue == 0)){
						guidetxt.setText("300m 앞 "+turntype+" 입니다.");
						tts.speak("300미터 앞.."+turntype+" 입니다.", TextToSpeech.QUEUE_FLUSH, null);
						storage_turn_type = turntype;
						show_priority = true;
						turnState=false;
						showturntype = new ShowTurnType();
						showturntype.start();
						perform_count--;
					}else if(pointDistance>100&&pointDistance<=200&&perform_count==2&&(guideValue >= 295 || guideValue <= 65 || guideValue == 0)){
						guidetxt.setText("100m 앞 "+turntype+" 입니다.");
						tts.speak("100미터 앞.."+turntype+" 입니다.", TextToSpeech.QUEUE_FLUSH, null);
						storage_turn_type = turntype;
						show_priority = true;
						turnState=false;
						showturntype = new ShowTurnType();
						showturntype.start();
						perform_count--;
					}else if(pointDistance>0&&pointDistance<=85&&perform_count==1&&(guideValue >= 295 || guideValue <= 65 || guideValue == 0)){
						guidetxt.setText("전방에 "+turntype+" 입니다.");
						tts.speak("전방에.."+turntype+" 입니다.", TextToSpeech.QUEUE_FLUSH, null);
						storage_turn_type = turntype;
						show_priority = true;
						turnState=true;
						showturntype = new ShowTurnType();
						showturntype.start();
						perform_count--;
					}
					
					// 목적 경유지 근접 도달시
					if(curLatitude >= lineLatitude-0.00023 && curLatitude <= lineLatitude+0.00023
							&& curLongitude >= lineLongitude-0.00023 && curLongitude <= lineLongitude+0.00023){
						if(guide.ArriveCheck() == true){ // 경유지가 목적지 종료 지점일 경우
							guidetxt.setText("목적지도착");
							tts.speak("목적지에 도착하였습니다.", TextToSpeech.QUEUE_FLUSH, null);
							tts.speak("길 안내를 종료 합니다.", TextToSpeech.QUEUE_ADD, null);
							mSm.unregisterListener(compass); // 리스너 해제
							designate_destination=false;
							direction_image.setImageResource(R.drawable.arrive);
						}
						else{ // 미 도착시
							left_right_direction_visible = false; // 좌우 방향 표시를 막는다.
							turnState=false;
							//guidetxt.setText("다음 경유지 지정");
							//tts.speak("다음 경유지 지정", TextToSpeech.QUEUE_FLUSH, null);
							guidetxt.setText("  ");
							
							start_coordinate = guide.GetCoordinate(++guide_index);
							coordinate_type = guide.GetCoordinateType();
							tts_once_perform_fact = false;
							show_priority=false;
						}
					}
					// 미 도달시
					else{
						// 초기 방향 계산 및 출력
						SetGuideValue();
						DrawDirection();
					}
				}
				else if(coordinate_type.equals("Line")){ // 라인 타입일 경우
					start_coordinate = guide.GetCoordinate(++guide_index);
					coordinate_type = guide.GetCoordinateType(); // 다음 노드타입을 얻어옴
					lineLatitude = Double.parseDouble(start_coordinate[1]);
					lineLongitude = Double.parseDouble(start_coordinate[0]);
					tts_once_perform_fact = false;
				}
			}
		}
		
		// 방향 그리기
		void DrawDirection(){
			if(show_priority == false){
				//동서남북 그림 남북(110도) 동서(70도)
				if(guideValue >= 295 || guideValue <= 65 || guideValue == 0){
					direction_image.setImageResource(R.drawable.up);
				}
				if(left_right_direction_visible == true){
					if(guideValue > 65 && guideValue < 125){
						direction_image.setImageResource(R.drawable.right);
					}
					else if(guideValue > 235 && guideValue < 295){
						direction_image.setImageResource(R.drawable.left);
					}
				}
				if(guideValue >= 125 && guideValue <= 235){ 
					direction_image.setImageResource(R.drawable.wrong);
				}
			}
		}
		
		// 방향값 계산
		void SetGuideValue(){
			if(azimuthValue == 0){
				guideValue = bearingValue - 360;
			}
			else if(bearingValue == 0){
				guideValue = 360 - azimuthValue;
			}
			else{
				guideValue = bearingValue -  azimuthValue;
			}
			
			if(guideValue<0){
				guideValue = 360 + guideValue;
			}
			//guidetxt.setText(Integer.toString(guideValue));
			
		}
		

		public void onProviderDisabled(String provider)
		{//사용할수 없게 된다면
			// TODO Auto-generated method stub
			
		}
		
		
		public void onProviderEnabled(String provider)
		{//사용 가능하게 되면
			// TODO Auto-generated method stub
			
		}
		
		
		public void onStatusChanged(String provider, int status, Bundle extras)
		{//상태값이 바뀌게 되면
			// TODO Auto-generated method stub
			
		}
		
	}
	
	// 방향 측정 센서 리스너
	class Compass implements SensorEventListener 
	{
		float azimuth;
		
		public void onAccuracyChanged(Sensor sensor, int accuracy)
		{
		}

		public void onSensorChanged(SensorEvent event)
		{
			float[] v = event.values;
			switch (event.sensor.getType()) 
			{
				case Sensor.TYPE_ORIENTATION:
					if (azimuth != v[0])
					{
						azimuth = v[0];
						// 가로모드에서 사용하므로 100도를 더한다.
						azimuthValue = (int)azimuth + 100;
						if(azimuthValue>360){azimuthValue = azimuthValue-360;}
					}
					break;
					
					
			}
		}
	}
	///////////////////////////메뉴버튼 눌러서 색깔지정/////////////////////////////
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		menu.add(0, SET_COLOR, Menu.NONE, "Color");
		return true;
	}

	///////////////////////색깔버튼 눌렀을 때의 이벤트//////////////////////////////////
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case SET_COLOR:
			// ColorActivity호출.
			Intent colorintent = new Intent(this, ColorActivity.class);
			startActivityForResult(colorintent, GET_COLOR);//액티비티 종료시 결과값 GET_COLOR요구
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/////////////////////////두번 back버튼시 종료///////////////////////////
	@Override
	public void onBackPressed() {
		//onbackPressed()호출
		backPressCloseHandler.onBackPressed();
	}
	
}
