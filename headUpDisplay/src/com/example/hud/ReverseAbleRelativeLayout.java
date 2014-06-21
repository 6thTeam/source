package com.example.hud;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class ReverseAbleRelativeLayout extends RelativeLayout{

	private boolean isReverse = false;
	public boolean isReverse() {
		return isReverse;
	}
	
	public void setReverse(boolean isReverse){
		this.isReverse = isReverse;
	}
	
	public ReverseAbleRelativeLayout(Context context){
		super(context);
	}

	public ReverseAbleRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReverseAbleRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	protected void dispatchDraw(Canvas arg0) {
		if(isReverse == true){
			Matrix matrix = arg0.getMatrix();
			matrix.setScale(-1, 1, arg0.getWidth()/2, arg0.getHeight()/2);
			matrix.setScale(1, -1, arg0.getWidth()/2, arg0.getHeight()/2);
			arg0.setMatrix(matrix);
			
		}
		else{
			Matrix matrix = arg0.getMatrix();
			matrix.setScale(1, 1, arg0.getWidth()/2, arg0.getHeight()/2);
			arg0.setMatrix(matrix);
		}
		super.dispatchDraw(arg0);
	}
}
