package com.example.hud;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class DestinationActivity extends Activity {
	Button backBtn, searchBtn;
	EditText searchText;
	String text;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_destination);
		backBtn = (Button)findViewById(R.id.backBtn);
		searchBtn = (Button)findViewById(R.id.searchBtn);
		searchText = (EditText)findViewById(R.id.editText1);
		
		searchBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra("SearchName", searchText.getText().toString());
				setResult(RESULT_OK,intent);
				finish();
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
	}

	

}
