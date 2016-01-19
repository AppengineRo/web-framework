package ro.appenigne.web.framework.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class CalendarUtils {
	public final static TimeZone timezone	= TimeZone.getTimeZone("Europe/Bucharest"); // timezone-ul folosit de aplicatie
	
	public static boolean isWeekend(Date now) {
		Calendar cal = Calendar.getInstance(timezone);
		cal.setTime(now);
		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			return true;
		}
		return false;
	}
	
	public static Date addDaysToDate(Date date, int days) {
		Calendar cal = Calendar.getInstance(timezone);
		cal.setTime(date);
		cal.add(Calendar.DAY_OF_MONTH, days);
		return cal.getTime();
	}
}
