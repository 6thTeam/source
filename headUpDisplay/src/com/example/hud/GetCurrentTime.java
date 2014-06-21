package com.example.hud;

import java.util.Calendar;

public class GetCurrentTime {
	String GetTime(Calendar cal, int blink){
		String result;
		int minute = cal.get(Calendar.MINUTE);
		
		if(minute < 10){
			if(blink == 0){
				result = String.format("%d : 0%d", cal.get(Calendar.HOUR_OF_DAY), 
					cal.get(Calendar.MINUTE));
			}
			else{
				result = String.format("%d   0%d", cal.get(Calendar.HOUR_OF_DAY), 
					cal.get(Calendar.MINUTE));
			}
		}
		else{
			if(blink == 0){
				result = String.format("%d : %d", cal.get(Calendar.HOUR_OF_DAY), 
						cal.get(Calendar.MINUTE));
			}
			else{
				result = String.format("%d   %d", cal.get(Calendar.HOUR_OF_DAY), 
						cal.get(Calendar.MINUTE));
			}
		}
		return result;
	}
}
