package com.themelisx.hellas4x4;

import android.location.Location;
import androidx.annotation.NonNull;
import android.util.Log;

import java.util.Locale;

public class LocationConverter {

    private static final String TAG = "LocationConverter";

    private static String getLatitudeAsDMS(double lat){
    //public static String getLatitudeAsDMS(Location location, int decimalPlace){
        String strLatitude = Location.convert(lat, Location.FORMAT_SECONDS);
        //strLatitude = replaceDelimiters(strLatitude, decimalPlace);
        strLatitude = replaceDelimiters(strLatitude);
        strLatitude = strLatitude + " N";
        return strLatitude;
    }

    //public static String getLongitudeAsDMS(Location location, int decimalPlace){
    private static String getLongitudeAsDMS(double lon){
        String strLongitude = Location.convert(lon, Location.FORMAT_SECONDS);
        //strLongitude = replaceDelimiters(strLongitude, decimalPlace);
        strLongitude = replaceDelimiters(strLongitude);
        strLongitude = strLongitude + " W";
        return strLongitude;
    }

    @NonNull
    private static String replaceDelimiters(String str) {
    //private static String replaceDelimiters(String str, int decimalPlace) {
        str = str.replaceFirst(":", "° ");
        str = str.replaceFirst(":", "' ");
        /*
        int pointIndex = str.indexOf(DecimalFormatSymbols.getInstance().getDecimalSeparator());
        int endIndex = pointIndex + 1 + decimalPlace;
        if(endIndex < str.length()) {
            str = str.substring(0, endIndex);
        }*/
        str = str + "\"";
        return str;
    }

    public static String LocationLatToStr(double lat, boolean useDegrees) {

        if (useDegrees) {
            return getLatitudeAsDMS(lat);
        } else {
            try {
                return String.format(Locale.US, "%.6f", lat).replace(',', '.');
            } catch (Exception e) {
                Log.e(TAG, "Error converting location to string");
                return "";
            }
        }
    }

    public static String LocationLonToStr(double lon, boolean useDegrees) {

        if (useDegrees) {
            return getLongitudeAsDMS(lon);
        } else {
            try {
                return String.format(Locale.US, "%.6f", lon).replace(',', '.');
            } catch (Exception e) {
                Log.e(TAG, "Error converting location to string");
                return "";
            }
        }
    }
}