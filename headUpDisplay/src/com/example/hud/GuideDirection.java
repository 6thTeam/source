package com.example.hud;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.skp.openplatform.android.sdk.api.APIRequest;
import com.skp.openplatform.android.sdk.common.PlanetXSDKConstants.CONTENT_TYPE;
import com.skp.openplatform.android.sdk.common.PlanetXSDKConstants.HttpMethod;
import com.skp.openplatform.android.sdk.common.PlanetXSDKException;
import com.skp.openplatform.android.sdk.common.RequestBundle;
import com.skp.openplatform.android.sdk.common.RequestListener;
import com.skp.openplatform.android.sdk.common.ResponseMessage;
import com.skplanetx.tmapopenapi.LogManager;

public class GuideDirection {
	APIRequest api;
	RequestBundle requestBundle;
	String URL = "https://apis.skplanetx.com/tmap/routes";
	Map<String, Object> param;
	String hndResult = "";
	
	DocumentBuilderFactory factory;
	DocumentBuilder builder;
	InputSource source;
	Document doc;
	Element root;
	NodeList items;
	
	String[] point_coordinate;
	String[] start_coordinate;
	String coordinate_type;
	
	boolean arrive_fact=false;
	
	// 초기화
	void Init(String[] coordinate){
		commWithOpenApiServer(coordinate);
		while(hndResult.equals("")){} // 데이터 로딩될때까지 대기
		source = new InputSource();
		source.setCharacterStream(new StringReader(hndResult));
		factory = DocumentBuilderFactory.newInstance();
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(source);
			root = doc.getDocumentElement();
			items = root.getElementsByTagName("Placemark");
		} catch (Exception e) {
			e.printStackTrace();
		} 	
		
	}
	
	// 포인트 타입 좌표배열을 가져옴
	String[] GetPointCoordinate(int index){
		Node point; // 포인트
	 	Node coordinate; // 좌표값 저장 요소
	 	try {
	 		Node item = items.item(index);
	 		point = findNode(item.getFirstChild(),"Point");
	 		coordinate = findNode(point.getFirstChild(),"coordinates");
	 		point_coordinate = coordinate.getFirstChild().getNodeValue().split(",");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	 
	 	return point_coordinate;
	}
	
	// 좌표배열을 가져옴
	String[] GetCoordinate(int index){
		Node point; // 포인트
 		Node coordinate; // 좌표값 저장 요소
 		try {
 			Node item = items.item(index);
			
 			if(distinguishNode(item.getFirstChild(), "LineString")){
 				point = findNode(item.getFirstChild(),"LineString");
 				coordinate = findNode(point.getFirstChild(),"coordinates");
 				start_coordinate = coordinate.getFirstChild().getNodeValue().split(",| ");
 				SetCoordinateType("Line");
 			}
 			else if(distinguishNode(item.getFirstChild(), "Point")){
 				point = findNode(item.getFirstChild(),"Point");
 				coordinate = findNode(point.getFirstChild(),"coordinates");
 				start_coordinate = coordinate.getFirstChild().getNodeValue().split(",");
 				SetCoordinateType("Point");
 				if(findNode(item.getFirstChild(), "tmap:pointType").
 						getFirstChild().getNodeValue().equals("E")){
 					SetArrive();
 				}
 			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		 
 		return start_coordinate;
	}
	
	// 다음 좌표타입이 포인트 타입인지 체크함
	boolean CheckNextCoordinate(int index){
		try {
 			Node item = items.item(index);
 			if(distinguishNode(item.getFirstChild(), "Point")){
 				return true;
 			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	void SetArrive(){
		arrive_fact=true;
	}
	
	boolean ArriveCheck(){
		return arrive_fact;
	}
	
	// 턴타입 세팅
	String GetTurnType(int index){
		Node type;
		String turntype = null;
		try {
 			Node item = items.item(index);
 			type = findNode(item.getFirstChild(),"tmap:turnType");
 			turntype = type.getFirstChild().getNodeValue();
 			return turntype;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return turntype;
	}
	
	// 턴타입 해석
	String TranslateTurnType(String turnvalue){
		// tmap:nodeType = POINT일 때 응답됩니다.
		String turntype;
		if(turnvalue.equals("11")){
			turntype = "직진";
		}
		else if(turnvalue.equals("12")){
			turntype = "좌회전";
		}
		else if(turnvalue.equals("13")){
			turntype = "우회전";
		}
		else if(turnvalue.equals("14")){
			turntype = "유턴";
		}
		else if(turnvalue.equals("15")){
			turntype = "P턴";
		}
		else if(turnvalue.equals("16")){
			turntype = "8시 방향 좌회전";
		}
		else if(turnvalue.equals("17")){
			turntype = "10시 방향 좌회전";
		}
		else if(turnvalue.equals("18")){
			turntype = "2시 방향 우회전";
		}
		else if(turnvalue.equals("19")){
			turntype = "4시 방향 우회전";
		}
		else if(turnvalue.equals("101")){
			turntype = "우측 고속도로 입구";
		}
		else if(turnvalue.equals("102")){
			turntype = "좌측 고속도로 입구";
		}
		else if(turnvalue.equals("103")){
			turntype = "전방 고속도로 입구";
		}
		else if(turnvalue.equals("104")){
			turntype = "우측 고속도로 출구";
		}
		else if(turnvalue.equals("105")){
			turntype = "좌측 고속도로 출구";
		}
		else if(turnvalue.equals("106")){
			turntype = "전방 고속도로 출구";
		}
		else if(turnvalue.equals("111")){
			turntype = "우측 도시고속도로 입구";
		}
		else if(turnvalue.equals("112")){
			turntype = "좌측 도시고속도로 입구";
		}
		else if(turnvalue.equals("113")){
			turntype = "전방 도시고속도로 입구";
		}
		else if(turnvalue.equals("114")){
			turntype = "우측 도시고속도로 출구";
		}
		else if(turnvalue.equals("115")){
			turntype = "좌측 도시고속도로 출구";
		}
		else if(turnvalue.equals("116")){
			turntype = "전방 도시고속도로 출구";
		}
		else if(turnvalue.equals("117")){
			turntype = "우측 방향";
		}
		else if(turnvalue.equals("118")){
			turntype = "좌측 방향";
		}
		else if(turnvalue.equals("119")){
			turntype = "지하차도";
		}
		else if(turnvalue.equals("120")){
			turntype = "고가도로";
		}
		else if(turnvalue.equals("121")){
			turntype = "터널";
		}
		else if(turnvalue.equals("122")){
			turntype = "교량";
		}
		else if(turnvalue.equals("123")){
			turntype = "지하차도 옆";
		}
		else if(turnvalue.equals("124")){
			turntype = "고가도로 옆";
		}
		else if(turnvalue.equals("131")){
			turntype = "1시 방향";
		}
		else if(turnvalue.equals("132")){
			turntype = "2시 방향";
		}
		else if(turnvalue.equals("133")){
			turntype = "3시 방향";
		}
		else if(turnvalue.equals("134")){
			turntype = "4시 방향";
		}
		else if(turnvalue.equals("135")){
			turntype = "5시 방향";
		}
		else if(turnvalue.equals("136")){
			turntype = "6시 방향";
		}
		else if(turnvalue.equals("137")){
			turntype = "7시 방향";
		}
		else if(turnvalue.equals("138")){
			turntype = "8시 방향";
		}
		else if(turnvalue.equals("139")){
			turntype = "9시 방향";
		}
		else if(turnvalue.equals("140")){
			turntype = "10시 방향";
		}
		else if(turnvalue.equals("141")){
			turntype = "11시 방향";
		}
		else if(turnvalue.equals("142")){
			turntype = "12시 방향";
		}
		else if(turnvalue.equals("200")){
			turntype = "출발지";
		}
		else if(turnvalue.equals("201")){
			turntype = "도착지";
		}
		else if(turnvalue.equals("151")){
			turntype = "휴게소";
		}
		else{
			turntype = "";
		}
		return turntype;
	}
	
	String GetCoordinateType(){
		return coordinate_type;
	}
	
	void SetCoordinateType(String type){
		if(type.equals("Line")){
			coordinate_type = "Line";
		}
		else if(type.equals("Point")){
			coordinate_type = "Point";
		}
		else if(type.equals("Arrive")){
			coordinate_type = "Arrive";
		}
	}
	 
	 // 노드 구별
	 boolean distinguishNode(Node node, String node_name){
		 while(!node.getNodeName().equals(node_name)){
			 node = node.getNextSibling();
			 if(node == node.getParentNode().getLastChild()){
			 	return false;
			 }
	 	 }
		 return true;
	 }
		
	 // 노드 찾기
	 Node findNode(Node node, String node_name){
		 while(!node.getNodeName().equals(node_name)){
				node = node.getNextSibling();
		 }
		 return node;
	 }
	 
	public void commWithOpenApiServer(String[] coordinate) {
        // AppKey 세팅
        api = new APIRequest();
        APIRequest.setAppKey("c617d098-3020-3ec6-ae04-24835166c225");
        
        // url�� ���ԵǴ� �Ķ���� ����
        param = new HashMap<String, Object>();
        param.put("version", "1");
        param.put("endX", coordinate[2]);
        param.put("endY", coordinate[1]);
        //param.put("endX", "128.3308250");
        //param.put("endY", "36.1282650");
        param.put("reqCoordType", "WGS84GEO");
        param.put("resCoordType", "WGS84GEO");
        param.put("startX", coordinate[4]);
        param.put("startY", coordinate[3]);
        param.put("directionOption", 1);
     //   param.put
        //param.put("startX", "128.3933975");
        //param.put("startY", "36.1460739");
        
        // ȣ��� ���� �� ����
        requestBundle = new RequestBundle();
        requestBundle.setUrl(URL);
        requestBundle.setParameters(param);
        requestBundle.setHttpMethod(HttpMethod.POST);
        requestBundle.setResponseType(CONTENT_TYPE.XML);
         
        try {
            api.request(requestBundle, reqListener);         
        } catch (PlanetXSDKException e) {
            e.printStackTrace();
        }
    }
	
	RequestListener reqListener = new RequestListener() {
		@Override
		public void onPlanetSDKException(PlanetXSDKException e) {
			
		}
		@Override
		public void onComplete(ResponseMessage response) {
			hndResult = response.toString();
		}
     };
	
}
