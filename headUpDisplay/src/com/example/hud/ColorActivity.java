package com.example.hud;

import com.skplanetx.tmapopenapi.LogManager;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;

public class ColorActivity extends Activity {
	
	public static final int [] colors = new int[] { //21가지 색깔설정
		0xffffffff,0xffff00ff,0xffff007f,0xff00ffff,0xffffb4b4,0xff00ff00,0xffffff00};
	
	GridView grid; //색깔버튼을 정렬되게 배치하기 위해 GridView 선언
	Button closeBtn;//돌아가기 버튼
	ColorDataAdapter adapter; //ColorDataAdapter 내부클래스 선언
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_color);
		this.setTitle("Color");
		grid = (GridView)findViewById(R.id.colorGrid);
		closeBtn = (Button) findViewById(R.id.closeBtn);
	    grid.setBackgroundColor(Color.GRAY);
	    grid.setVerticalSpacing(4);
	    grid.setHorizontalSpacing(4);
		
	    adapter = new ColorDataAdapter(this);
        grid.setAdapter(adapter);
        grid.setNumColumns(adapter.getNumColumns());
        
		closeBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
	}
	
	
	class ColorDataAdapter extends BaseAdapter {
		
		
		Context mContext;
		int rowCount;
		int columnCount;
		public ColorDataAdapter(Context context) {
			super();
			mContext = context;
			rowCount = 1;
			columnCount = 7;
		}
		
		public int getNumColumns() {
			return columnCount;
		}
		
		@Override
		public int getCount() {
			return rowCount * columnCount;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return colors[position];
		}

		@Override
		public long getItemId(int position) {
			return colors[position];
		}

		@Override
		public View getView( final int position, View convertView, ViewGroup group) {
			// TODO Auto-generated method stub
		
			GridView.LayoutParams params = new GridView.LayoutParams(
					GridView.LayoutParams.MATCH_PARENT,
					GridView.LayoutParams.MATCH_PARENT);
			
			Button colorBtn = new Button(mContext);
			colorBtn.setText(" ");
			colorBtn.setLayoutParams(params);	
			colorBtn.setPadding(4, 4, 4, 4);
			colorBtn.setBackgroundColor(colors[position]);
			colorBtn.setHeight(99);
			
			colorBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(ColorActivity.this, MainActivity.class);
					intent.putExtra("colorData", colors[position]);
					setResult(RESULT_OK, intent);
					finish();
				}
			});
			return colorBtn;
		}
	}
}

