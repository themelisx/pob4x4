package com.themelisx.hellas4x4;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

public class SettingsActivity extends PreferenceActivity {

    int checkedItem;
    int checkedItem2;

    final String PREF_USE_DEGREES = "PREF_USE_DEGREES";
    final String PREF_USERNAME = "PREF_USERNAME";


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.app_name);

        /*
        ActionBar actionBar = getActionBar();
        if ( actionBar != null )
        {
            actionBar.setTitle(getResources().getString(R.string.app_name));
            //actionBar.setSubtitle(getResources().getString(R.string.menu_settings));
            actionBar.setDisplayHomeAsUpEnabled(true);
            getOverflowMenu();
        }*/

        addPreferencesFromResource(R.xml.settings);

        Preference btn_back = (Preference) findPreference("btn_back");
        btn_back.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                finish();
                return false;
            }
        });

        /*
        Preference my_app_version = (Preference) findPreference("my_version");
        my_app_version.setTitle(getResources().getString(R.string.app_name));
        my_app_version.setSummary("v." + getMyVersion());
        */

        Preference select_language = (Preference) findPreference("select_language");
        String locale = LocaleHelper.getLanguage(this);
        select_language.setSummary("");
        for (int i=0; i<App.lang.length; i++) {
            if (App.lang[i].equalsIgnoreCase(locale)) {
                checkedItem = i;
                select_language.setSummary(App.languages[i]);
                break;
            }
        }
        select_language.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
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
                        LocaleHelper.setLocale(SettingsActivity.this, App.lang[checkedItem]);

                        finish();
                        startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                    }
                });
                builder.setNegativeButton(getResources().getString(android.R.string.cancel), null);

                AlertDialog dialog = builder.create();
                dialog.show();
                return false;
            }
        });

        Preference select_format = (Preference) findPreference("select_format");
        SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
        if (sp.getBoolean(PREF_USE_DEGREES, false)) {
            checkedItem2 = 1;
        } else {
            checkedItem2 = 0;
        }
        select_format.setSummary(App.format[checkedItem2]);
        select_format.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(getResources().getString(R.string.select_format));

                builder.setSingleChoiceItems(App.format, checkedItem2, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkedItem2 = which;
                    }
                });

                builder.setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.sharedPreferencesFilename), Context.MODE_PRIVATE);
                        SharedPreferences.Editor esp = sp.edit();
                        esp.putBoolean(PREF_USE_DEGREES, checkedItem2 == 1);
                        esp.commit();

                        finish();
                        startActivity(new Intent(SettingsActivity.this, MainActivity.class));

                    }
                });
                builder.setNegativeButton(getResources().getString(android.R.string.cancel), null);

                AlertDialog dialog = builder.create();
                dialog.show();
                return false;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public String getMyVersion() {
        String ret = "";
        PackageInfo packageInfo;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ret = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void getOverflowMenu() {

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
