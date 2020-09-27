package com.themelisx.hellas4x4;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class App extends Application {

	public static String[] format = {"DDD.DDDDD°", "DDD° MM' SS.S\""};
	private static boolean isDayNow;

	private static final String LOGTAG = "App";
	public static String[] lang = {"en", "el"};
	public static String[] languages = {
			"English",
			"Ελληνικά"
	};

	private static App sInstance = null;

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
	}

	public static App instance() {
		if (sInstance == null) {
			sInstance = new App();
		}
		return sInstance;
	}

	/*
	//=== Singleton part
	private App() {
	}
	*/
	public static void setIsDayOrNight() {

		isDayNow = isDay(false, 0);

	}

	public String formatDate(Context context, Date date, boolean withTime)
	{
		String result = "";
		DateFormat dateFormat;

		if (date != null) {
			try {
				dateFormat = android.text.format.DateFormat.getDateFormat(context);
				result = dateFormat.format(date);

				if (withTime) {
					dateFormat = android.text.format.DateFormat.getTimeFormat(context);
					result += " " + dateFormat.format(date);
				}
			} catch (Exception e) {
				Log.e("App", "Date formatting error");
			}
		}

		return result;
	}

	private static android.location.Location getStoredLastLocation(){

		final String PREF_LAST_LOCATION_LNG = "PREF_LAST_LOCATION_LNG";
		final String PREF_LAST_LOCATION_LAT = "PREF_LAST_LOCATION_LAT";

		if (instance().getContext() != null) {
			SharedPreferences sp = instance().getContext().getSharedPreferences(instance().getContext().getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
			float lat = sp.getFloat(PREF_LAST_LOCATION_LAT, -1000);
			float lng = sp.getFloat(PREF_LAST_LOCATION_LNG, -1000);
			if (lat == -1000 || lng == -1000) {
				return null;
			}
			android.location.Location l = new android.location.Location("APP_STORAGE");
			l.setLongitude((double) lng);
			l.setLatitude((double) lat);
			return l;
		} else {
			return null;
		}
	}

	public static boolean isDay(boolean forceHour, int hour) {
		boolean ret = false;

		Location location;
		android.location.Location lastLocation = getStoredLastLocation();
		if ( lastLocation == null ) {
			location = new Location(40.736851, 22.920227); //thessaloniki
		} else {
			location = new Location(lastLocation.getLatitude(), lastLocation.getLongitude());
		}

		//SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, "Europe/Athens");
		//Calendar today = new GregorianCalendar(TimeZone.getTimeZone("Europe/Athens"));
		SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault().getID());
		Calendar today = new GregorianCalendar(TimeZone.getTimeZone(TimeZone.getDefault().getID()));

		//Log.e("tst", "timezone:" + TimeZone.getDefault().getID());

		String sSunrise = "00:00";
		String sSunset = "00:00";

		sSunrise = calculator.getOfficialSunriseForDate(today);
		sSunset = calculator.getOfficialSunsetForDate(today);

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Date date;

			date = sdf.parse("01-01-2016 " + sSunrise + ":00");
			Calendar cal1 = Calendar.getInstance();
			cal1.setTimeInMillis(date.getTime());
			cal1.add(Calendar.MINUTE, 30);  //30 minutes after sunrise
			int h1 = cal1.get(Calendar.HOUR_OF_DAY);
			int m1 = cal1.get(Calendar.MINUTE);

			date = sdf.parse("01-01-2016 " + sSunset + ":00");
			Calendar cal2 = Calendar.getInstance();
			cal2.setTimeInMillis(date.getTime());
			cal2.add(Calendar.MINUTE, 30);	//30 minutes after sunset
			int h2 = cal2.get(Calendar.HOUR_OF_DAY);
			int m2 = cal2.get(Calendar.MINUTE);

			Calendar now = Calendar.getInstance();
			now.setTimeInMillis(System.currentTimeMillis());
			if ( forceHour ) {
				now.set(Calendar.HOUR_OF_DAY, hour);
			}
			//now.set(Calendar.HOUR_OF_DAY, 18);
			//now.set(Calendar.MINUTE, 54);

			int hNow = now.get(Calendar.HOUR_OF_DAY);
			int mNow = now.get(Calendar.MINUTE);

			//Log.e(TAG, "hour:" + String.valueOf(hNow));
			//Log.e(TAG, "minute:" + String.valueOf(mNow));

			if (hNow == h1) {
				if (mNow >= m1) {
					ret = true;
				}
			} else if (hNow == h2) {
				if (mNow <= m2) {
					ret = true;
				}
			} else {
				if ( (hNow > h1) && (hNow < h2)) {
					ret = true;
				}
			}

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}
	
	public static void shutdown(){
		if (sInstance != null){
			sInstance = null;
		}
		
	}

	//=== END Singleton part

	private String MaicLanguage = "el";
	//private String MaicLanguageScript = "";
	//private String useCountry = null;
	private boolean firstRun = true;
	private boolean firstRunWebkit = true;
	//private boolean mapViewPermitted;
	private boolean weHaveWebKit;

	private Context mContext;

	@TargetApi(Build.VERSION_CODES.N)
	public Locale getCurrentLocale(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
			return getContext().getResources().getConfiguration().getLocales().get(0);
		} else{
			//noinspection deprecation
			return getContext().getResources().getConfiguration().locale;
		}
	}

	private int getMaicVersion(Context context) {
		int version = -1;
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo("com.mls.voice_center", 0);
			version = pInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e1) {
			Log.e(this.getClass().getSimpleName(), "Name not found", e1);
		}
		return version;
	}

	public boolean init(Context context){
		mContext = context;


		return true;
	}
	
	public Context getContext(){
		return mContext;
	}


	public String formatDistance(double number) {
		int iNumber = (int) number;
		//=== round 2 places
		int digitCount = String.valueOf(iNumber).length();
		if (digitCount > 2){
			int roundParameter = (int) Math.pow(10.0, (double)(digitCount - 2));
			iNumber = (iNumber / (roundParameter) * roundParameter);
			//123 -> 120
			//1234 -> 1200
		}
		String suffix = mContext.getString(R.string.suffix_meters);
		if (digitCount <= 3)// 1, 12, 123
			return String.valueOf(iNumber) + suffix;
		number = iNumber / 1000.0;
		suffix = mContext.getString(R.string.suffix_kilometers);
		if (digitCount == 4)
			return String.valueOf(number) + suffix;
		return String.valueOf((int)number) + suffix;
	}
	
	public void navigateTo(Context context, String name, double lat, double lng){

		if ( name != null ) {
			Log.i(LOGTAG, "navigateTo: " + name);
		} else {
			Log.e(LOGTAG, "navigateTo: null name given");
			//reportAnalyticsServiceUsage("NAVIGATE_NULL", String.format(Locale.US, "%f", lng) + ", " + String.format(Locale.US, "%f", lat));
		}

		try{
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/maps?daddr=" + lat + "," + lng));
			mContext.startActivity(intent);

		} catch (ActivityNotFoundException e) {
			Toast.makeText(mContext, mContext.getResources().getString(R.string.navigator_not_installed) , Toast.LENGTH_SHORT).show();
		}

	}
	
	public static class Point {
		public double longitude;
		public double latitude;
		public String name;
		public String snippet;
		public int resIconID;
	}
	
	public static class GoogleMapsInfo{
		public List<Point> points;
		public int resTitleIcon;
		public int resTitleText;
	}
	
	public void showMap(GoogleMapsInfo info){
		mGoogleMapsInfo = info;
		Intent i = new Intent(mContext, MapActivity.class);
		mContext.startActivity(i);
	}
	
	private GoogleMapsInfo mGoogleMapsInfo;
	
	public GoogleMapsInfo getGoogleMapsInfo(){
		return mGoogleMapsInfo;
	}
	
	public String getMyString(int resID){
		return mContext.getString(resID);
	}

}
