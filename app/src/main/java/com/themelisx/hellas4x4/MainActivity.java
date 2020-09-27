package com.themelisx.hellas4x4;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.core.app.ActivityCompat;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jjoe64.graphview.BuildConfig;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements Listener {


    class savedLocations {
        ArrayList<myLocation> mySavedlocations = null;
    }

    public boolean useDegrees = false;
    private int checkedItem;

    private static final int REQUEST = 111;
    private static final String TAG = "MainActivity";
    private static final float GPS_MIN_DISTANCE = 50;
    private static final long GPS_MIN_TIME = 1000;

    private static final double DEF_ZOOM = 19.0;
    //private List<Point> mPoints;
    MapView map = null;
    TextView location_lat;
    TextView location_lng;
    TextView location_alt;
    TextView location_inFix;
    TextView location_inView;
    TextView location_accuracy;
    TextView textViewTitle;
    TextView openMapsLogo;
    ImageView mSosButton;
    LinearLayout footer;
    ImageView btnHideInfo;

    GraphView graph;
    //StaticLabelsFormatter staticLabelsFormatter;
    BarGraphSeries<DataPoint> series = null;

    float maxValue = 30;

    boolean viewsAreVisible = true;

    private LocationManager locManager = null;
    private LocationListener locListener = null;

    //App.GoogleMapsInfo gmInfo = null;
    myLocation lastLocation = null;
    MyLocationNewOverlay mLocationOverlay = null;
    CompassOverlay mCompassOverlay = null;

	/*
	double minLat = Double.MAX_VALUE;
	double maxLat = Double.MIN_VALUE;
	double minLong = Double.MAX_VALUE;
	double maxLong = Double.MIN_VALUE;
	*/

    final String PREF_LAST_LOCATION_LNG = "PREF_LAST_LOCATION_LNG";
    final String PREF_LAST_LOCATION_LAT = "PREF_LAST_LOCATION_LAT";
    final String PREF_LAST_LOCATION_ALT = "PREF_LAST_LOCATION_ALT";

    final String PREF_VIEWS_VISIBLE = "PREF_VIEWS_VISIBLE";
    final String PREF_USE_DEGREES = "PREF_USE_DEGREES";
    final String PREF_SAVED_LOCATIONS = "PREF_SAVED_LOCATIONS";
    final String PREF_USERNAME = "PREF_USERNAME";

    public void onBtnDegrees(View view) {
        SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
        useDegrees = !sp.getBoolean(PREF_USE_DEGREES, false);
        SharedPreferences.Editor esp = sp.edit();
        esp.putBoolean(PREF_USE_DEGREES, useDegrees);
        esp.commit();
        if (lastLocation != null) {
            if (useDegrees) {
                location_lat.setText(LocationConverter.LocationLatToStr(lastLocation.getLat(), useDegrees));
                location_lng.setText(LocationConverter.LocationLonToStr(lastLocation.getLon(), useDegrees));
            } else {
                location_lat.setText(String.format("%s %s", LocationConverter.LocationLatToStr(lastLocation.getLat(), useDegrees), getString(R.string.location_lat)));
                location_lng.setText(String.format("%s %s", LocationConverter.LocationLonToStr(lastLocation.getLon(), useDegrees), getString(R.string.location_lon)));
            }
        }
    }

    public void onBtnSettings(View view) {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }

    public Drawable resizeImage(int resId, int iconWidth, int iconHeight) {

        Bitmap BitmapOrg = BitmapFactory.decodeResource(getResources(), resId);

        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();

        float scaleWidth = ((float) iconWidth) / width;
        float scaleHeight = ((float) iconHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);

        return new BitmapDrawable(getResources(), resizedBitmap);

    }

    private void ShareMyData(boolean useDegrees)
    {
        if ( lastLocation == null ) {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.waiting_gps), Toast.LENGTH_SHORT).show();
            return;
        }

        final Intent intent = new Intent(Intent.ACTION_SEND);

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.getDefault());

        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.my_location) );
        intent.putExtra(Intent.EXTRA_TEXT,
                getResources().getString(R.string.date_time) + ": " +
                        dateFormat.format(System.currentTimeMillis()) + "\n" +
                        getResources().getString(R.string.location_lat) + ": " + LocationConverter.LocationLatToStr(lastLocation.getLat(), useDegrees) + "\n" +
                        getResources().getString(R.string.location_lon) + ": " + LocationConverter.LocationLonToStr(lastLocation.getLon(), useDegrees) + "\n" +
                        String.format("%s: %s", getString(R.string.altitude),
                                (lastLocation.getAlt() != 0) ?
                                        String.format(Locale.US, "%d%s", Math.round(lastLocation.getAlt()), getString(R.string.suffix_meters)) :
                                        getString(R.string.unknown)) + "\n" +
                        //getResources().getString(R.string.address_found) + ": " + note_edit_adr.getText().toString() + "\n" +
                        //getResources().getString(R.string.notes) + ": " + lastLocation.getNote() + "\n\n" +
                        "https://maps.google.com/maps?q=" + LocationConverter.LocationLatToStr(lastLocation.getLat(), false) + "+" + LocationConverter.LocationLonToStr(lastLocation.getLon(), false)
        );

        startActivity(Intent.createChooser(intent, getString(R.string.loading)));

        //finish();
    }

    @SuppressLint("StaticFieldLeak")
    void DoShare()
    {
        new AsyncTask<Integer, Integer, Boolean>()
        {
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(MainActivity.this, "", getResources().getString(R.string.loading));
            }

            @Override
            protected Boolean doInBackground(Integer... params)
            {
                if (params == null) {
                    return false;
                }
                try {
                    ShareMyData(useDegrees);
                } catch (Exception e) {
                    //The task failed
                    return false;
                }
                //The task succeeded
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result)
            {
                progressDialog.dismiss();
            }
        }.execute(2000);

    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST: {
                if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                        (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                        (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                        (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    //Toast.makeText(getBaseContext(), getString(R.string.permission_ok), Toast.LENGTH_SHORT).show();
                    recreate();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
                    builder.setTitle(getString(R.string.app_name));
                    builder.setMessage(getString(R.string.permissions_denied));
                    builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            finish();
                            dialog.dismiss();

                        }
                    });
                    builder.show();
                }
                break;
            }
        }
    }

    void findViewsById() {
        map = findViewById(R.id.map);
        location_lat = findViewById(R.id.location_lat);
        location_lng = findViewById(R.id.location_lng);
        location_alt = findViewById(R.id.location_alt);

        location_inFix = findViewById(R.id.location_inFix);
        location_inView = findViewById(R.id.location_inView);
        location_accuracy = findViewById(R.id.location_accuracy);

        graph = findViewById(R.id.satellitesGraph);
        footer = findViewById(R.id.footer);
        btnHideInfo = findViewById(R.id.btnHideInfo);
        textViewTitle = findViewById(R.id.textViewTitle);
        openMapsLogo = findViewById(R.id.openMapsLogo);

        mSosButton = (ImageView) findViewById(R.id.btnSos);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            boolean missing_permission_storage = false;
            boolean missing_permission_gps = false;

            if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                    (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {

                Log.e(getClass().getSimpleName(), "missing permission: external storage");
                missing_permission_storage = true;
            }

            if ((checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                    (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {

                missing_permission_gps = true;
            }

            if (missing_permission_storage || missing_permission_gps) {

                if (missing_permission_storage && missing_permission_gps) {
                    requestPermissions(new String[]{
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST);
                } else {
                    if (missing_permission_gps) {
                        requestPermissions(new String[]{
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST);
                    }

                    if (missing_permission_storage) {
                        requestPermissions(new String[]{
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST);
                    }
                }
                return;
            }
        }

        /*if(!getResources().getBoolean(R.bool.isTablet)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }*/

        setContentView(R.layout.activity_main);

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        //gmInfo = App.instance().getGoogleMapsInfo();
        //mapIcon.setImageResource(gmInfo.resTitleIcon);

        findViewsById();

        SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
        //Give negative cause it will reversed
        viewsAreVisible = !sp.getBoolean(PREF_VIEWS_VISIBLE, true);
        useDegrees = sp.getBoolean(PREF_USE_DEGREES, false);
        onHideInfo(null);

        textViewTitle.setText(getString(R.string.app_name));
        location_inFix.setText(String.format("%d %s", 0, getResources().getString(R.string.in_fix)));
        location_inView.setText(String.format("%d %s", 0, getResources().getString(R.string.in_view)));
        location_accuracy.setText("");

        location_inFix.setTextColor(getResources().getColor(R.color.red));

        graph.removeAllSeries();
        series = new BarGraphSeries<>();
        graph.addSeries(series);
        series.setValueDependentColor(new ValueDependentColor<DataPoint>() {
            @Override
            public int get(DataPoint data) {

                double snr = data.getY();
                if (snr >= 11 && snr <= 20) {
                    return Color.rgb(0xff, 0x80, 0x00);
                    //return R.color.orange;
                } else if (snr > 20 && snr <= 30) {
                    return Color.rgb(0xff, 0xff, 0x00);
                } else if (snr > 30 && snr <= 50) {
                    return Color.rgb(0xaa, 0xff, 0x00);
                } else if (snr > 40) {
                    return Color.rgb(0x00, 0xff, 0x00);
                } else {
                    return Color.rgb(0xff, 0x00, 0x00);
                }
            }
        });

        graph.getViewport().setMinX(0);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(maxValue);
        graph.getViewport().setYAxisBoundsManual(true);

        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        graph.getGridLabelRenderer().setGridColor(getResources().getColor(R.color.white));
        graph.getGridLabelRenderer().setHorizontalLabelsColor(getResources().getColor(R.color.white));
        graph.getGridLabelRenderer().setVerticalLabelsColor(getResources().getColor(R.color.white));
        /*
        staticLabelsFormatter = new StaticLabelsFormatter(graph);
        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);


        */
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        //graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        //graph.getGridLabelRenderer().setHorizontalAxisTitle("Satellites");
        //graph.getGridLabelRenderer().setVerticalAxisTitle("Signal");

        location_lat.setText(getString(R.string.waiting_gps));
        location_lng.setText(getString(R.string.waiting_gps));
        location_alt.setText(getString(R.string.waiting_gps));

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        IMapController mapController = map.getController();

        mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(mCompassOverlay);

        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(getClass().getSimpleName(), "No permission to access location");
            } else {
                Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                if (location != null) {
                    mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
                    map.getOverlays().add(mLocationOverlay);

                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putDouble("lat", location.getLatitude());
                    bundle.putDouble("lng", location.getLongitude());
                    bundle.putDouble("alt", location.getAltitude());
                    msg.setData(bundle);

                    locationHandler.sendMessage(msg);
                }
            }
        }

        /*
        int newSize = (gmInfo.points.size() > 1) ? 64 : 96;

        if (gmInfo.points.size() > 0) {

            for (App.Point point : gmInfo.points) {
                OverlayItem mOverlayItem = new OverlayItem(
                        point.name,
                        point.snippet,
                        new GeoPoint(point.latitude, point.longitude));
                //mOverlayItem.setMarker(getResources().getDrawable(point.resIconID));
                mOverlayItem.setMarker(resizeImage(point.resIconID, newSize, newSize));
                items.add(mOverlayItem);

				/ *
				if (point.latitude < minLat) { minLat = point.latitude; }
				if (point.latitude > maxLat) { maxLat = point.latitude; }
				if (point.longitude < minLong) { minLong = point.longitude; }
				if (point.longitude > maxLong) { maxLong = point.longitude; }
				* /
            }
        }*/

        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {

                        new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogCustom))
                                .setTitle(item.getTitle())
                                .setMessage(item.getSnippet())
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .show();
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {

                        //App.instance().navigateTo(MapActivity.this, item.getTitle(), item.getPoint().getLatitude(), item.getPoint().getLongitude());
                        return false;
                    }
                }, this);

        mOverlay.setFocusItemsOnTap(false);
        map.getOverlays().add(mOverlay);

        /*

        if ( gmInfo.points.size() > 0 ) {
            if (gmInfo.points.size() > 1) {
                //BoundingBox boundingBox = new BoundingBox(maxLat, minLong, minLat, maxLong);
                //map.zoomToBoundingBox(boundingBox, false);
                //mapController.setCenter(new GeoPoint((minLat + maxLat) / 2.0, (minLong + maxLong) / 2.0));

				/ *
				if (mLocationOverlay != null) {
					mLocationOverlay.runOnFirstFix(new Runnable() {
						public void run() {
							map.getController().animateTo(mLocationOverlay.getMyLocation());
						}
					});

				}* /
                mapController.setZoom(DEF_ZOOM - 4);

            } else {
                mapController.setZoom(DEF_ZOOM);
            }
            mapController.setCenter(new GeoPoint(gmInfo.points.get(0).latitude,gmInfo.points.get(0).longitude));
        }*/

        if (mLocationOverlay != null) {
            mLocationOverlay.runOnFirstFix(new Runnable() {
                public void run() {

                    if (mLocationOverlay != null) {

                        GeoPoint loc = mLocationOverlay.getMyLocation();

                        if (map != null && loc != null) {
                            Message msg = new Message();
                            Bundle bundle = new Bundle();
                            bundle.putDouble("lat", loc.getLatitude());
                            bundle.putDouble("lng", loc.getLongitude());
                            bundle.putDouble("alt", loc.getAltitude());
                            msg.setData(bundle);

                            locationHandler.sendMessage(msg);
                        }

                    }
                }
            });

        }

        mapController.setZoom(DEF_ZOOM);
        map.invalidate();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String lastLocale = settings.getString("locale", "none");
        if (lastLocale.equals("none")) {
            selectLanguage(true);
        }
    }

    void selectLanguage(final boolean updateUI) {

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.select_language));

        builder.setSingleChoiceItems(App.languages, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItem = which;
            }
        });

        builder.setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                SharedPreferences.Editor editor = settings.edit();
                editor.putString("locale", "done");
                editor.apply();

                LocaleHelper.setLocale(MainActivity.this, App.lang[checkedItem]);
                recreate();
            }
        });

        builder.setNegativeButton(getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("MissingPermission")
    private DataPoint[] generateData() {

        DataPoint[] values = null;

        if ( locManager != null ) {

            if ( graph == null ) {
                findViewsById();
            }

            GpsStatus gpsStatus = locManager.getGpsStatus(null);
            if (gpsStatus != null) {
                Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();

                int maxInView = 0;
                for (GpsSatellite satellite : satellites) {
                    maxInView++;
                }

                int inFix = 0;
                int totalColumns = maxInView;

                values = new DataPoint[totalColumns];

                //String labels[] = new String[totalColumns];
                //staticLabelsFormatter.setHorizontalLabels(labels);
                graph.getViewport().setMaxX(totalColumns);

                int i = 0;
                for (GpsSatellite satellite : satellites) {

                    //Log.e("test", "prn:" + String.valueOf(satellite.getPrn()));
                    values[i] = new DataPoint(i, satellite.getSnr());
                    //series.appendData(new DataPoint(i, satellite.getSnr()), false, totalColumns);
                    i++;

                    if (satellite.getSnr() > maxValue) {
                        maxValue = satellite.getSnr();
                        graph.getViewport().setMaxY(maxValue);
                    }

                    if (satellite.usedInFix()) {
                        inFix += 1;
                    }
                }

                location_inFix.setText(String.format(Locale.US, "%d %s", inFix, getResources().getString(R.string.in_fix)));
                location_inView.setText(String.format(Locale.US, "%d %s", maxInView, getResources().getString(R.string.in_view)));

                if (inFix >= 3) {
                    location_inFix.setTextColor(getResources().getColor(R.color.green));
                } else {
                    location_inFix.setTextColor(getResources().getColor(R.color.red));
                }
            }
        }

        return values;
    }

    @SuppressLint("HandlerLeak")
    private Handler locationHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();

            if (bundle != null) {

                if (bundle.getBoolean("satellite_info", false)) {
                    if (locManager != null) //GPS only
                    {
                        //GPS 32 satellites numbered from 1-32
                        //GLONASS 24 satellites numbered from 65-92
                        //Galileo 30 satellites numbered from Exx

                        //if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        //    return;
                        //}

                        if ( viewsAreVisible ) {
                            series.resetData(generateData());
                        }
                    }

                } else {

                    //double bearing = 0;
                    double lat = bundle.getDouble("lat", 0);
                    double lng = bundle.getDouble("lng", 0);
                    double alt = bundle.getDouble("alt", 0);
                    String accuracy = bundle.getString("accuracy", "");
                    boolean saveNow = bundle.getBoolean("save", false);

                    if ( lastLocation == null ) {
                        lastLocation = new myLocation();
                    }
                    lastLocation.setLat(lat);
                    lastLocation.setLon(lng);
                    lastLocation.setAlt(alt);
                    lastLocation.setAccuracy(accuracy);
                    lastLocation.setId(System.currentTimeMillis());

                    if (useDegrees) {
                        location_lat.setText(LocationConverter.LocationLatToStr(lastLocation.getLat(), useDegrees));
                        location_lng.setText(LocationConverter.LocationLonToStr(lastLocation.getLon(), useDegrees));
                    } else {
                        location_lat.setText(String.format("%s %s", LocationConverter.LocationLatToStr(lastLocation.getLat(), useDegrees), getString(R.string.location_lat)));
                        location_lng.setText(String.format("%s %s", LocationConverter.LocationLonToStr(lastLocation.getLon(), useDegrees), getString(R.string.location_lon)));
                    }
                    if (alt == 0) {
                        location_alt.setText("");
                    } else {
                        location_alt.setText(String.format("%s%s %s", String.format(Locale.US, "%d", Math.round(alt)), getString(R.string.suffix_meters), getString(R.string.altitude)));
                    }

                    if (accuracy.isEmpty()) {
                        location_accuracy.setText("");
                    } else {
                        location_accuracy.setText(String.format("%s%s %s", accuracy, getString(R.string.suffix_meters), getString(R.string.accuracy)));
                    }
                    /*
                    if ( bundle.getBoolean("has_bearing", false) ) {
                        bearing = bundle.getDouble("bearing", 0);
                    }*/

                    if (saveNow) {
                        SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
                        SharedPreferences.Editor esp = sp.edit();
                        esp.putFloat(PREF_LAST_LOCATION_LAT, (float) lat);
                        esp.putFloat(PREF_LAST_LOCATION_LNG, (float) lng);
                        esp.putFloat(PREF_LAST_LOCATION_ALT, (float) alt);
                        esp.apply();
                    }

                    if (map != null) {
                        GeoPoint loc = new GeoPoint(lat, lng);
                        map.getController().animateTo(loc);
                    }
                }
            }
        }
    };

    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        if (map != null) {
            map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
        }

        if (mCompassOverlay != null) {
            mCompassOverlay.enableCompass();
        }

        if (mLocationOverlay != null) {
            mLocationOverlay.enableFollowLocation();
            mLocationOverlay.enableMyLocation();
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            boolean permission_ok = true;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                        (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                        (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                        (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                    Log.e(getClass().getSimpleName(), "Missing permissions");
                    permission_ok = false;
                }
            }

            if (permission_ok){
                Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                if (location != null && map != null) {
                    mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
                    map.getOverlays().add(mLocationOverlay);
                }

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                if (locManager == null) {
                    locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    locListener = new MyLocationListener();
                }

                locManager.removeUpdates(locListener);
                locManager.removeGpsStatusListener(this);
                //locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_MIN_TIME, GPS_MIN_DISTANCE, locListener);

                if (locManager.getAllProviders().contains(LocationManager.GPS_PROVIDER) &&
                        locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_MIN_TIME, GPS_MIN_DISTANCE, locListener);
                    locManager.addGpsStatusListener(this);
                } else {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.gps_is_off), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
        //Configuration.getInstance().save(this, prefs);

        if (mLocationOverlay != null) {
            mLocationOverlay.disableFollowLocation();
            mLocationOverlay.disableMyLocation();
        }

        if (mCompassOverlay != null) {
            mCompassOverlay.disableCompass();
        }

        if (map != null) {
            map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
        }

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (locManager != null) {
            locManager.removeUpdates(locListener);
            locManager.removeGpsStatusListener(this);
        }
    }

    public void onBtnBack(View view) {
        finish();
    }

    public void onBtnShare(View view) {

        if ( lastLocation == null ) {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.waiting_gps), Toast.LENGTH_SHORT).show();
        } else {
            DoShare();
        }
    }

    public void onBtnSave(View view) {

        if ( lastLocation == null ) {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.waiting_gps), Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(this,R.style.AlertDialogCustom));
        final EditText edittext = new EditText(this);

        String date = App.instance().formatDate(this, new Date(), true);

        lastLocation.setId(System.currentTimeMillis());
        edittext.setText(date);
        edittext.selectAll();

        alert.setTitle(getResources().getString(R.string.save_location));
        alert.setMessage(getResources().getString(R.string.location_title));
        alert.setView(edittext);

        alert.setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                lastLocation.setCategory(0);
                lastLocation.setAddress("");
                lastLocation.setNote("");
                lastLocation.setTitle(edittext.getText().toString());
                SaveLocation();

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);
                }
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);
                }
            }
        });

        alert.show();
        edittext.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    void SaveLocation() {
        SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
        String pref_saved_locations = sp.getString(PREF_SAVED_LOCATIONS, "");

        savedLocations saved_locations = null;

        try {
            saved_locations = new Gson().fromJson(pref_saved_locations, savedLocations.class);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error loading old data");
        }

        if ( saved_locations == null ) {
            saved_locations = new savedLocations();
            saved_locations.mySavedlocations = new ArrayList<myLocation>();
        }

        saved_locations.mySavedlocations.add(lastLocation);

        String json = new Gson().toJson(saved_locations);

        SharedPreferences.Editor esp = sp.edit();
        esp.putString(PREF_SAVED_LOCATIONS, json);

        esp.commit();
    }

    public void onBtnList(View view) {

        startActivity(new Intent(MainActivity.this, SavedPlaces.class));

    }

    public void onSos(View view) {
        if ( lastLocation == null ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
            builder.setTitle(getString(R.string.waiting_gps));
            builder.setMessage(getString(R.string.no_gps_continue));
            builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    DoSos();

                }
            });
            builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.show();
        } else {
            DoSos();
        }
    }

    private void DoSos() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(getString(R.string.warning));
        builder.setMessage(getString(R.string.warning_msg));
        builder.setPositiveButton(getResources().getString(R.string.app_name), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                DoSosNow("6977805905");

            }
        });
        builder.setNegativeButton(getResources().getString(R.string.friends), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                DoSosNow("");

            }
        });
        builder.show();
    }

    private void DoSosNow(final String sendTo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.final_info));
        builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                String sms_body = "";

                SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
                String username = sp.getString(PREF_USERNAME, "");
                if (!username.isEmpty()) {
                    sms_body = username + ", ";
                }

                if (lastLocation != null) {
                    sms_body = sms_body + getString(R.string.coordinates) + ": " +
                            LocationConverter.LocationLatToStr(lastLocation.getLat(), false) + ", " +
                            LocationConverter.LocationLonToStr(lastLocation.getLon(), false) + ", " +
                            String.format("%s: %s", getString(R.string.altitude),
                                    (lastLocation.getAlt() != 0) ?
                                            String.format(Locale.US, "%d%s", Math.round(lastLocation.getAlt()), getString(R.string.suffix_meters)) :
                                            getString(R.string.unknown)) + ", " +
                            getResources().getString(R.string.problem);
                } else {
                    sms_body = sms_body + getResources().getString(R.string.waiting_gps) + " " +
                            getResources().getString(R.string.my_location) + ": ";
                }

                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setData(Uri.parse("sms:"));
                smsIntent.putExtra("address", sendTo);
                smsIntent.putExtra("sms_body",sms_body);
                startActivity(smsIntent);

            }
        });
        builder.show();
    }

    public void onHideInfo(View view) {
        viewsAreVisible = !viewsAreVisible;

        SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
        SharedPreferences.Editor esp = sp.edit();
        esp.putBoolean(PREF_VIEWS_VISIBLE, viewsAreVisible);
        esp.apply();

        if ( graph == null ) {
            findViewsById();
        }

        graph.setVisibility(viewsAreVisible ? View.VISIBLE : View.GONE);
        footer.setVisibility(viewsAreVisible ? View.VISIBLE : View.GONE);
        btnHideInfo.setRotation(viewsAreVisible ? -90 : 90);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)btnHideInfo.getLayoutParams();
        if ( viewsAreVisible ) {
            if (series != null) {
                series.resetData(generateData());
            }

            params.addRule(RelativeLayout.ABOVE, R.id.footer);
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            btnHideInfo.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams)openMapsLogo.getLayoutParams();
            params.addRule(RelativeLayout.ABOVE, R.id.footer);
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            openMapsLogo.setLayoutParams(params);
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.removeRule(RelativeLayout.ABOVE);
            btnHideInfo.setLayoutParams(params);

            params = (RelativeLayout.LayoutParams)openMapsLogo.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.removeRule(RelativeLayout.ABOVE);
            openMapsLogo.setLayoutParams(params);
        }
    }

    @SuppressLint("SimpleDateFormat")
    public class MyLocationListener implements LocationListener {


        @Override
        @SuppressLint("MissingPermission")
        public void onLocationChanged(Location loc) {

            if (loc != null) {

                String provider = loc.getProvider();

                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putDouble("lat", loc.getLatitude());
                bundle.putDouble("lng", loc.getLongitude());
                //bundle.putBoolean("save", true);
                bundle.putString("provider", provider);

                if (loc.hasAccuracy()) {
                    bundle.putString("accuracy", String.format("%d", Math.round(loc.getAccuracy())));
                } else {
                    bundle.putString("accuracy", "");
                }

                if (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {


                    if (loc.hasAltitude()) {
                        bundle.putDouble("alt", loc.getAltitude());
                    }
                    if (loc.hasBearing()) {
                        bundle.putBoolean("has_bearing", true);
                        bundle.putDouble("bearing", loc.getBearing());
                    } else {
                        bundle.putBoolean("has_bearing", false);
                    }

                }

                msg.setData(bundle);
                locationHandler.sendMessage(msg);

                /*
                if (pr.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) //) && ( GPSHasSignal ) )
                {
                    NoSpeed++;
                    if ( NoSpeed > 3)
                    {
                        LastSpeed = 0;
                    }
                    alt = 0;
                } else
                {
                    float s;


                    if ( loc.hasSpeed() )
                    {
                        s = loc.getSpeed();
                        LastSpeed = s;
                        NoSpeed = 0;
                    } else
                    {
                        s = LastSpeed;

                        NoSpeed++;
                        if ( NoSpeed > 3)
                        {
                            LastSpeed = 0;
                        }
                    }

                    if ( loc.hasAltitude() )
                    {
                        alt = loc.getAltitude();
                    }
                }*/
            }
        }


        @Override
        public void onProviderDisabled(String provider) {
            Log.i(TAG, provider + " Disabled");
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, provider + " Enabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

            /*
            if ( provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER))
            {
                switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    Log.i(TAG, provider + " = Status Changed: Out of Service");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i(TAG, provider + " = Status Changed: Temporarily Unavailable");
                    break;
                case LocationProvider.AVAILABLE:
                    Log.i(TAG, provider + " = Status Changed: Available");
                    break;
                }
            }

            if ( provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER))
            {
                switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    Log.i(TAG, provider + " = Status Changed: Out of Service");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i(TAG, provider + " = Status Changed: Temporarily Unavailable");
                    break;
                case LocationProvider.AVAILABLE:
                    Log.i(TAG, provider + " = Status Changed: Available");
                    break;
                }
            }
            /* This is called when the GPS status alters */
            /*switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                Toast.makeText(getBaseContext(), provider + " = Status Changed: Out of Service", Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Toast.makeText(getBaseContext(), provider + " = Status Changed: Temporarily Unavailable", Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.AVAILABLE:
                Toast.makeText(getBaseContext(), provider + " = Status Changed: Available", Toast.LENGTH_SHORT).show();
                break;
            }*/

            //ShowProvider();


        }
    }


    @Override
    @SuppressLint("MissingPermission")
    public void onGpsStatusChanged(int event) {

        //Green indicates that a satellite is currently being used to determine the location of your device.
        //Yellow means that information from the satellite in question is available but it is not being used to determine your location.
        //Blue means that approximate data is available while
        //Grey tells us that data from that satellite is not available.

        //GPS consists of up to 32
        //GLONASS has full global coverage with 24 satellites.

        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putBoolean("satellite_info", true);
        msg.setData(bundle);
        locationHandler.sendMessage(msg);


    }
}
