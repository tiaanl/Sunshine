package com.fizix.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fizix.sunshine.data.WeatherContract;


public class DetailActivity extends ActionBarActivity {

    private static final int DETAIL_LOADER = 0;
    public static final String DATE_KEY = "date";
    private static final String LOCATION_KEY = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

        private static final String TAG = DetailFragment.class.getSimpleName();

        private String mForecastStr;
        private String mLocation;

        public DetailFragment() {
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            return rootView;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.detail_fragment, menu);

            MenuItem menuItem = menu.findItem(R.id.action_share);

            // Get the provider and hold onto it to set/change the share intent.
            ShareActionProvider shareActionProvider =
                    (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

            // Attach an intent to this ShareActionProvider.  You can update this at any time,
            // like when the user selects a new piece of data they might like to share.
            if (shareActionProvider != null) {
                shareActionProvider.setShareIntent(createShareForecastIntent());
            } else {
                Log.d(TAG, "Share action provider is null?");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString(LOCATION_KEY, mLocation);
            super.onSaveInstanceState(outState);
        }

        @Override
        public void onResume() {
            super.onResume();

            if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
                getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
            }
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if (savedInstanceState != null) {
                mLocation = savedInstanceState.getString(LOCATION_KEY);
            }
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        }

        private Intent createShareForecastIntent() {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, mForecastStr);
            return intent;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            String dateString = getActivity().getIntent().getStringExtra(DATE_KEY);

            String[] columns = {
                    WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
                    WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                    WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                    WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                    WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                    WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
                    WeatherContract.WeatherEntry.COLUMN_PRESSURE,
                    WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
                    WeatherContract.WeatherEntry.COLUMN_DEGREES,
                    WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
            };

            mLocation = Utility.getPreferredLocation(getActivity());
            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDateUri(
                    mLocation, dateString);

            return new CursorLoader(
                    getActivity(),
                    weatherUri,
                    columns,
                    null,
                    null,
                    null
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            if (cursor.moveToFirst()) {
                String description = cursor.getString(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC));
                String dateText = cursor.getString(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATETEXT));

                double high = cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP));
                double low = cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));

                boolean isMetric = Utility.isMetric(getActivity());

                TextView dateView = (TextView) getView().findViewById(R.id.detail_date_textview);
                TextView forecastView = (TextView) getView().findViewById(R.id.detail_forecast_textview);
                TextView highView = (TextView) getView().findViewById(R.id.detail_high_textview);
                TextView lowView = (TextView) getView().findViewById(R.id.detail_low_textview);

                dateView.setText(Utility.formatDate(dateText));
                forecastView.setText(description);
                highView.setText(Utility.formatTemperature(high, isMetric) + "\u00B0");
                lowView.setText(Utility.formatTemperature(low, isMetric) + "\u00B0");

                mForecastStr = String.format("%s - %s - %s/%s",
                        dateView.getText(),
                        forecastView.getText(),
                        highView.getText(),
                        lowView.getText()
                );

            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {

        }
    }
}
