package com.themelisx.hellas4x4;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.library.BuildConfig;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

public class MapActivity extends Activity {

	LocationManager locationManager;
	private static final double DEF_ZOOM = 19.0;
	//private List<Point> mPoints;
	MapView map = null;
	App.GoogleMapsInfo gmInfo = null;

	CompassOverlay mCompassOverlay;
	MyLocationNewOverlay mLocationOverlay = null;


	double minLat = 90.0;
	double maxLat = -90.0;
	double minLong = 180.0;
	double maxLong = -180.0;


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

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(LocaleHelper.onAttach(base));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//TODO:Keep screen on

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		setContentView(R.layout.activity_map);

		Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

		gmInfo = App.instance().getGoogleMapsInfo();

		TextView textViewTitle = findViewById(R.id.textViewTitle);

		if (gmInfo.points != null && gmInfo.points.size() > 0) {
			textViewTitle.setText(gmInfo.points.get(0).name);
		} else {
			textViewTitle.setText(getResources().getString(R.string.saved_places));
		}

		//ImageView mapIcon = findViewById(R.id.mapIcon);
		//mapIcon.setImageResource(gmInfo.resTitleIcon);

		map = findViewById(R.id.map);
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
				}
			}
		}

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


				if (point.latitude < minLat) { minLat = point.latitude; }
				if (point.latitude > maxLat) { maxLat = point.latitude; }
				if (point.longitude < minLong) { minLong = point.longitude; }
				if (point.longitude > maxLong) { maxLong = point.longitude; }

			}
		}

		ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
				new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
					@Override
					public boolean onItemSingleTapUp(final int index, final OverlayItem item) {

						new AlertDialog.Builder(new ContextThemeWrapper(MapActivity.this, R.style.AlertDialogCustom))
								.setTitle(item.getTitle())
								.setMessage(item.getPoint().getLatitude() + "\n" + item.getPoint().getLongitude())
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

						App.instance().navigateTo(MapActivity.this, item.getTitle(), item.getPoint().getLatitude(), item.getPoint().getLongitude());
						return false;
					}
				}, this);

		mOverlay.setFocusItemsOnTap(false);
		map.getOverlays().add(mOverlay);



		if ( gmInfo.points.size() > 0 ) {
			if (gmInfo.points.size() > 1) {

				mapController.setZoom(DEF_ZOOM - 4);
				//zoomToBoundingBox();

			} else {
				mapController.setZoom(DEF_ZOOM);

				if (mLocationOverlay != null) {
					final GeoPoint here = mLocationOverlay.getMyLocation();
					if (here != null) {

						Looper.prepare();
						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								map.getController().animateTo(here);
							}
						}, 100);


						/*if (gmInfo.points.size() == 1) {
							DrawRoute(here, new GeoPoint(gmInfo.points.get(0).latitude, gmInfo.points.get(0).longitude));
						}*/
					}
				}
			}

			mapController.setCenter(new GeoPoint(gmInfo.points.get(0).latitude, gmInfo.points.get(0).longitude));

			if (mLocationOverlay != null) {
				mLocationOverlay.runOnFirstFix(new Runnable() {
					public void run() {
						final GeoPoint here = mLocationOverlay.getMyLocation();
						if ( here != null ) {
							Looper.prepare();
							new Handler().postDelayed(new Runnable() {
								@Override
								public void run() {
									map.getController().animateTo(here);
								}
							}, 100);
							//map.getController().animateTo(here);
							/*if (gmInfo.points.size() == 1) {
								DrawRoute(here, new GeoPoint(gmInfo.points.get(0).latitude, gmInfo.points.get(0).longitude));
							} else*/ {
								if (here.getLatitude() < minLat) { minLat = here.getLatitude(); }
								if (here.getLatitude() > maxLat) { maxLat = here.getLatitude(); }
								if (here.getLongitude() < minLong) { minLong = here.getLongitude(); }
								if (here.getLongitude() > maxLong) { maxLong = here.getLongitude(); }

								zoomToBoundingBox();
							}
						}

					}
				});

			}

		}


		map.invalidate();
	}


	void zoomToBoundingBox() {
		new Thread(new Runnable()
		{
			public void run()
			{
				final BoundingBox mBoundingBox = new BoundingBox(maxLat, maxLong, minLat, minLong);

				runOnUiThread(new Runnable()
				{
					public void run()
					{
						map.zoomToBoundingBox(mBoundingBox, false);
						map.getController().setZoom(map.getZoomLevelDouble() - 0.2);
						//mapController.setCenter(new GeoPoint((minLat + maxLat) / 2.0, (minLong + maxLong) / 2.0));
						map.invalidate();
					}
				});
			}
		}).start();
	}

	void DrawRoute(final GeoPoint startPoint, final GeoPoint endPoint) {

		new Thread(new Runnable()
		{
			public void run()
			{
				RoadManager roadManager = new OSRMRoadManager(MapActivity.this);
				ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
				waypoints.add(startPoint);
				waypoints.add(endPoint);
				Road road = null; // = roadManager.getRoad(waypoints);
				try
				{
					road = roadManager.getRoad(waypoints);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				if (road != null) {

					final Road finalRoad = road;
					runOnUiThread(new Runnable() {
						public void run() {
							if (finalRoad.mStatus != Road.STATUS_OK) {
								Log.e("OSRM", "Error:" + String.valueOf(finalRoad.mStatus));
							} else {
								Polyline roadOverlay = RoadManager.buildRoadOverlay(finalRoad);
								map.getOverlays().add(roadOverlay);
								//TODO:finalRoad.mLength = distance ?

							}
							map.zoomToBoundingBox(finalRoad.mBoundingBox, false);
							map.getController().setZoom(map.getZoomLevelDouble() - 0.2);
							map.invalidate();
						}
					});
				}
			}
		}).start();
	}

	public void onResume() {
		super.onResume();

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		//this will refresh the osmdroid configuration on resuming.
		//if you make changes to the configuration, use
		//SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
		if ( map != null ) {
			map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
		}
		if (mCompassOverlay != null) {
			mCompassOverlay.enableCompass();
		}

		if (mLocationOverlay != null) {
			mLocationOverlay.enableFollowLocation();
			mLocationOverlay.enableMyLocation();
		}
	}

	public void onPause(){
		super.onPause();

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		//this will refresh the osmdroid configuration on resuming.
		//if you make changes to the configuration, use
		//SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//Configuration.getInstance().save(this, prefs);

		if (mLocationOverlay != null) {
			mLocationOverlay.disableFollowLocation();
			mLocationOverlay.disableMyLocation();
		}
		if (mCompassOverlay != null) {
			mCompassOverlay.disableCompass();
		}

		if ( map != null ) {
			map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
		}
	}

	public void onBtnBack(View view) {
		finish();
	}

	public void onBtnNavigate(View view) {

		if (gmInfo != null && gmInfo.points != null && gmInfo.points.size() > 0) {

			App.instance().navigateTo(this, gmInfo.points.get(0).name, gmInfo.points.get(0).latitude, gmInfo.points.get(0).longitude);
		}
	}
}
