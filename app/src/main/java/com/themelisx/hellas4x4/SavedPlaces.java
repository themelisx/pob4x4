package com.themelisx.hellas4x4;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.themelisx.hellas4x4.App.GoogleMapsInfo;
import com.themelisx.hellas4x4.App.Point;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SavedPlaces extends Activity {

    //private InterstitialAd mInterstitialAd;

    final String PREF_SAVED_LOCATIONS = "PREF_SAVED_LOCATIONS";
    final String PREF_USE_DEGREES = "PREF_USE_DEGREES";
    int selectedItem = -1;

    class savedLocations {
        ArrayList<myLocation> mySavedlocations = null;
    }

    savedLocations saved_locations = null;

    private static final int REQUEST_GPS = 111;
    ProgressBar loading;

    private ListView mListView;
    private ItemsAdapter mAdapter;
    private LayoutInflater mInflater;

    //private ImageView mButtonMaps;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onPause() {
        super.onPause();

        //showAds();
    }

    /*
    private void showAds() {

        if ( mInterstitialAd != null && mInterstitialAd.isLoaded() ) {
            mInterstitialAd.show();
        }
    }

    private void LoadAd() {
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-8218376207658297/1109262266");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.d("Ads", "onAdLoaded");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.d("Ads", "onAdFailedToLoad:"+ String.valueOf(errorCode));
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when the ad is displayed.
                //Log.e("Ads", "onAdOpened");
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
                //Log.e("Ads", "onAdLeftApplication");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the interstitial ad is closed.
                //Log.e("Ads", "onAdClosed");
            }
        });
    }
    */

    /*
    public void showMenu (View view, final int position)
    {
        PopupMenu menu = new PopupMenu (new ContextThemeWrapper(SavedPlaces.this,R.style.AlertDialogCustom), view);
        menu.setOnMenuItemClickListener (new PopupMenu.OnMenuItemClickListener ()
        {
            @Override
            public boolean onMenuItemClick (MenuItem item)
            {
                int id = item.getItemId();
                switch (id)
                {
                    case R.id.item_show_map:
                        showItemOnMap(position);
                        break;
                    case R.id.item_edit:
                        final myLocation location = (myLocation) mAdapter.getItem(position);

                        AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(SavedPlaces.this,R.style.AlertDialogCustom));
                        final EditText edittext = new EditText(SavedPlaces.this);

                        edittext.setText(location.getTitle());
                        edittext.selectAll();

                        alert.setTitle(getResources().getString(R.string.save_location));
                        alert.setMessage(getResources().getString(R.string.location_title));
                        alert.setView(edittext);

                        alert.setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                location.setCategory(0);
                                location.setAddress("");
                                location.setNote("");
                                location.setTitle(edittext.getText().toString());
                                //UpdateLocation();

                            }
                        });

                        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                            }
                        });

                        alert.show();
                        break;
                }
                return true;
            }
        });
        menu.inflate (R.menu.saved_list);
        menu.show();
    }*/

    void DeleteItem(long id) {

        int found = -1;
        for (int i=0; i<saved_locations.mySavedlocations.size();i++) {
            if (saved_locations.mySavedlocations.get(i).getId() == id) {
                found = i;
                break;
            }
        }

        if ( found != -1) {
            AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(SavedPlaces.this,R.style.AlertDialogCustom));

            alert.setTitle(saved_locations.mySavedlocations.get(found).getTitle());
            alert.setMessage(getResources().getString(R.string.confirm_delete));

            final int finalFound = found;
            alert.setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    saved_locations.mySavedlocations.remove(finalFound);

                    String json = new Gson().toJson(saved_locations);
                    SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
                    SharedPreferences.Editor esp = sp.edit();
                    esp.putString(PREF_SAVED_LOCATIONS, json);
                    esp.commit();

                    updateGUI();

                }
            });

            alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });

            alert.show();

        }
    }

    void updateLocation(myLocation location) {
        SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);

        for (int i=0; i<saved_locations.mySavedlocations.size();i++) {
            if (saved_locations.mySavedlocations.get(i).getId() == location.getId()) {
                saved_locations.mySavedlocations.set(i, location);
                break;
            }
        }

        String json = new Gson().toJson(saved_locations);

        SharedPreferences.Editor esp = sp.edit();
        esp.putString(PREF_SAVED_LOCATIONS, json);
        esp.commit();

        updateGUI();

    }

    private void ShareMyData(myLocation location, boolean useDegrees)
    {
        if ( location == null ) {
            return;
        }

        final Intent intent = new Intent(Intent.ACTION_SEND);

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.getDefault());

        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, location.getTitle() );
        intent.putExtra(Intent.EXTRA_TEXT,
                getResources().getString(R.string.saved_places) + ": " + location.getTitle() + "\n" +
                    getResources().getString(R.string.date_time) + ": " +
                    dateFormat.format(location.getId()) + "\n" +
                    getResources().getString(R.string.location_lat) + ": " + LocationConverter.LocationLatToStr(location.getLat(), useDegrees) + "\n" +
                    getResources().getString(R.string.location_lon) + ": " + LocationConverter.LocationLonToStr(location.getLon(), useDegrees) + "\n" +
                    String.format("%s: %s", getString(R.string.altitude),
                            (location.getAlt() != 0) ?
                                    String.format(Locale.US, "%d%s", Math.round(location.getAlt()), getString(R.string.suffix_meters)) :
                                    getString(R.string.unknown)) + "\n" +
                    //getResources().getString(R.string.address_found) + ": " + note_edit_adr.getText().toString() + "\n" +
                    //getResources().getString(R.string.notes) + ": " + lastLocation.getNote() + "\n\n" +
                    "https://maps.google.com/maps?q=" + LocationConverter.LocationLatToStr(location.getLat(), false) + "+" + LocationConverter.LocationLonToStr(location.getLon(), false)
        );

        startActivity(Intent.createChooser(intent, getString(R.string.loading)));

        //finish();
    }

    @SuppressLint("StaticFieldLeak")
    void DoShare(final myLocation location, final boolean useDegrees)
    {
        new AsyncTask<Integer, Integer, Boolean>()
        {
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(SavedPlaces.this, "", getResources().getString(R.string.loading));
            }

            @Override
            protected Boolean doInBackground(Integer... params)
            {
                if (params == null) {
                    return false;
                }
                try {
                    ShareMyData(location, useDegrees);
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

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        unregisterForContextMenu(mListView);
        if (selectedItem == -1) { return true; }
        int position = selectedItem;

        SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
        boolean useDegrees = sp.getBoolean(PREF_USE_DEGREES, false);

        final myLocation location = (myLocation) mAdapter.getItem(position);

        int id = item.getItemId();
        switch (id)
        {
            case R.id.item_show_map:
                showItemOnMap(position);
                break;
            case R.id.item_navigate:
                App.instance().navigateTo(this, location.getTitle(), location.getLat(), location.getLon());
                break;
            case R.id.item_delete:
                DeleteItem(location.getId());
                break;
            case R.id.item_share:
                DoShare(location, useDegrees);
                break;
            case R.id.item_edit:
                AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(SavedPlaces.this,R.style.AlertDialogCustom));
                final EditText edittext = new EditText(SavedPlaces.this);

                edittext.setText(location.getTitle());
                edittext.selectAll();

                //alert.setTitle(getResources().getString(R.string.location_title));
                alert.setMessage(getResources().getString(R.string.location_title));
                alert.setView(edittext);

                alert.setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        location.setCategory(0);
                        location.setAddress("");
                        location.setNote("");
                        location.setTitle(edittext.getText().toString());
                        updateLocation(location);

                    }
                });

                alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });

                alert.show();
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.saved_list, menu);

        //menu.setHeaderTitle("Select The Action");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.instance().init(this);//=== must be before "setContentView"

        if (!getResources().getBoolean(R.bool.isTablet)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.activity_saved_places);

        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new ItemsAdapter();
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                //showMenu(v, position);
                selectedItem = position;
                registerForContextMenu(mListView);
                openContextMenu( v );
            }
        });

        ImageView btn_back = (ImageView) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        /*
        mButtonMaps = (ImageView) findViewById(R.id.buttonMap);
        mButtonMaps.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showMap();
            }
        });
        */

        SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
        String pref_saved_locations = sp.getString(PREF_SAVED_LOCATIONS, "");

        try {
            saved_locations = new Gson().fromJson(pref_saved_locations, savedLocations.class);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error loading old data");
        }

        //TODO:change this
        //MobileAds.initialize(this, "ca-app-pub-8218376207658297~7359400878");
        //LoadAd();

        updateGUI();
        //reInitFocus();

    }

    private void showItemOnMap(int position){
        if (saved_locations == null || saved_locations.mySavedlocations == null || saved_locations.mySavedlocations.isEmpty())
            return;

        List<Point> points = new ArrayList<Point>();
        myLocation item = saved_locations.mySavedlocations.get(position);
        Point p = new Point();
        p.latitude = item.getLat();
        p.longitude = item.getLon();
        p.name = item.getTitle();
        p.resIconID = R.mipmap.ic_launcher;
        p.snippet = item.getAddress();
        points.add(p);

        GoogleMapsInfo gmi = new GoogleMapsInfo();
        gmi.points = points;
        gmi.resTitleIcon = R.mipmap.ic_launcher;

        App.instance().showMap(gmi);
    }

    /*
    private void showMap(){
        if (saved_locations == null || saved_locations.mySavedlocations == null || saved_locations.mySavedlocations.isEmpty())
            return;
        List<Point> points = new ArrayList<Point>();
        for (int i = 0; i < saved_locations.mySavedlocations.size(); i++){
            myLocation item = saved_locations.mySavedlocations.get(i);
            Point p = new Point();
            p.latitude = item.getLat();
            p.longitude = item.getLon();
            p.name = item.getTitle();
            p.resIconID = R.mipmap.ic_launcher;
            p.snippet = item.getAddress();
            points.add(p);
        }
        GoogleMapsInfo gmi = new GoogleMapsInfo();
        gmi.points = points;
        gmi.resTitleIcon = R.mipmap.ic_launcher;
        gmi.resTitleText = R.string.saved_places;

        App.instance().showMap(gmi);
    }*/

    private void updateGUI(){

        if (saved_locations == null || mAdapter == null || mAdapter.getCount() == 0){
            mListView.setVisibility(View.INVISIBLE);
            //mButtonMaps.setVisibility(View.INVISIBLE);
        }
        else {
            //mButtonMaps.setVisibility(View.VISIBLE);
            if ( saved_locations.mySavedlocations != null && saved_locations.mySavedlocations.size() > 1) {
                Collections.sort(saved_locations.mySavedlocations, new Comparator<myLocation>() {
                    @Override
                    public int compare(myLocation c1, myLocation c2) {
                        return c1.getTitle().compareTo(c2.getTitle());
                    }
                });
            }

            mListView.setVisibility(View.VISIBLE);
        }

        mAdapter.notifyDataSetChanged();
        mListView.invalidateViews();
    }

    //====================================================================================
    //====================================================================================
    //=== CustomAdapter
    //====================================================================================
    //====================================================================================

    private class ItemsAdapter extends BaseAdapter {

        //=== BaseAdapter implementation ======================
        @Override
        public int getCount() {
            if (saved_locations != null && saved_locations.mySavedlocations != null)
                return saved_locations.mySavedlocations.size();
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (saved_locations != null && saved_locations.mySavedlocations != null && position < saved_locations.mySavedlocations.size())
                return saved_locations.mySavedlocations.get(position);
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            myLocation item = (myLocation) getItem(position);
            if (rowView == null) {
                mInflater = getLayoutInflater();
                rowView = mInflater.inflate(R.layout.listitem_places, null);
            }
            if (item == null)
                return rowView;

            //TextView textViewDistance = rowView.findViewById(R.id.textViewDistance);
            TextView textViewTitle = rowView.findViewById(R.id.textViewTitle);
            TextView textViewLat = rowView.findViewById(R.id.textViewLat);
            TextView textViewLon = rowView.findViewById(R.id.textViewLon);
            TextView textViewDescription = rowView.findViewById(R.id.textViewDescription);

            textViewTitle.setText(item.getTitle());
            //textViewDistance.setText(App.instance().formatDistance(item.distance));

            SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
            if (sp.getBoolean(PREF_USE_DEGREES, false)) {
                textViewLat.setText(LocationConverter.LocationLatToStr(item.getLat(), true));
                textViewLon.setText(LocationConverter.LocationLonToStr(item.getLon(), true));
            } else {
                textViewLat.setText(String.format("%s %s", LocationConverter.LocationLatToStr(item.getLat(), false), getString(R.string.location_lat)));
                textViewLon.setText(String.format("%s %s", LocationConverter.LocationLonToStr(item.getLon(), false), getString(R.string.location_lon)));
            }
            /*
            textViewLat.setText(String.format(Locale.US, "%s: %s / %s",
                    getResources().getString(R.string.coordinates),
                    LocationConverter.LocationLatToStr(item.getLat(), useDegrees),
                    LocationConverter.LocationLatToStr(item.getLon(), useDegrees)));
                    */

            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.getDefault());
            textViewDescription.setText(dateFormat.format(item.getId()));
            /*
            textViewDescription.setText(String.format("%s: %s", getString(R.string.altitude),
                    (item.getAlt() != 0) ?
                            String.format(Locale.US, "%d", Math.round(item.getAlt())) :
                            getString(R.string.unknown)));
                            */
            //textViewAddress.setText(item.getAddress());
            //textViewDescription.setText(item.getNote());

            /*
            if ( item.getNote().trim().length() > 0 ) {
                textViewAddress.setMaxLines(1);
            } else {
                textViewAddress.setMaxLines(2);
            }
            */

            return rowView;
        }



    }
}
