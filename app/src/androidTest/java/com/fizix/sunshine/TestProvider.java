package com.fizix.sunshine;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.fizix.sunshine.data.WeatherContract.LocationEntry;
import com.fizix.sunshine.data.WeatherContract.WeatherEntry;

import java.util.Map;
import java.util.Set;

public class TestProvider extends AndroidTestCase {

    private static final String LOG_TAG = TestProvider.class.getSimpleName();

    private static final String TEST_CITY_NAME = "North Pole";
    private static final String TEST_LOCATION = "99705";
    private static final String TEST_DATE = "20140906";

    public void testDeleteAllRecords() {
        mContext.getContentResolver().delete(
                WeatherEntry.CONTENT_URI,
                null,
                null
        );

        mContext.getContentResolver().delete(
                LocationEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        assertEquals(cursor.getCount(), 0);
        cursor.close();

        cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        assertEquals(cursor.getCount(), 0);
        cursor.close();
    }

    @Override
    public void setUp() {
        testDeleteAllRecords();
    }

    public void testGetType() throws Throwable {
        // content://com.fizix.sunshine/weather/
        String type = mContext.getContentResolver().getType(WeatherEntry.CONTENT_URI);
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testLocation = "94074";
        // content://com.fizix.sunshine/weather/94074
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocationUri(testLocation));
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testDate = "20140906";
        // content://com.fizix.sunshine/weather/94074/20140906
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocationWithDateUri(testLocation, testDate));
        assertEquals(WeatherEntry.CONTENT_ITEM_TYPE, type);

        // content://com.fizix.sunshine/location/
        type = mContext.getContentResolver().getType(LocationEntry.CONTENT_URI);
        assertEquals(LocationEntry.CONTENT_TYPE, type);

        long testLocationId = 123;
        // content://com.fizix.sunshine/location/94074
        type = mContext.getContentResolver().getType(LocationEntry.buildLocationUri(testLocationId));
        assertEquals(LocationEntry.CONTENT_ITEM_TYPE, type);

    }

    public void testInsertRead() throws Throwable {
        ContentValues locationValues = getLocationContentValues();

        Uri locationInsertUri = mContext.getContentResolver().insert(
                LocationEntry.CONTENT_URI, locationValues);
        long locationRowId = ContentUris.parseId(locationInsertUri);

        // Make sure we got a row back.
        assertTrue(locationRowId != -1);

        Cursor locationCursor = mContext.getContentResolver().query(
                LocationEntry.buildLocationUri(locationRowId),
                null,
                null,
                null,
                null
        );

        if (locationCursor.moveToFirst()) {
            validateCursor(locationValues, locationCursor);

            ContentValues weatherValues = createWeatherValues(locationRowId);

            Uri weatherInsertUri = mContext.getContentResolver().insert(
                    WeatherEntry.CONTENT_URI, weatherValues);
            long weatherRowId = ContentUris.parseId(weatherInsertUri);

            Cursor weatherCursor = mContext.getContentResolver().query(
                    WeatherEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );

            if (weatherCursor.moveToFirst()) {
                validateCursor(weatherValues, weatherCursor);
            } else {
                fail("No weather values returned");
            }

            weatherCursor.close();

            weatherCursor = mContext.getContentResolver().query(
                    WeatherEntry.buildWeatherLocationUri(TEST_LOCATION),
                    null,
                    null,
                    null,
                    null
            );

            if (weatherCursor.moveToFirst()) {
                validateCursor(weatherValues, weatherCursor);
            } else {
                fail("No weather values returned");
            }

            weatherCursor.close();

            weatherCursor = mContext.getContentResolver().query(
                    WeatherEntry.buildWeatherLocationWithStartDateUri(TEST_LOCATION, TEST_DATE),
                    null,
                    null,
                    null,
                    null
            );

            if (weatherCursor.moveToFirst()) {
                validateCursor(weatherValues, weatherCursor);
            } else {
                fail("No weather values returned");
            }

            weatherCursor.close();

            weatherCursor = mContext.getContentResolver().query(
                    WeatherEntry.buildWeatherLocationWithDateUri(TEST_LOCATION, TEST_DATE),
                    null,
                    null,
                    null,
                    null
            );

            if (weatherCursor.moveToFirst()) {
                validateCursor(weatherValues, weatherCursor);
            } else {
                fail("No weather values returned");
            }

            weatherCursor.close();

        } else {
            fail("No location values returned");
        }
    }

    static ContentValues getLocationContentValues() {
        ContentValues values = new ContentValues();

        values.put(LocationEntry.COLUMN_CITY_NAME, TEST_CITY_NAME);
        values.put(LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION);
        values.put(LocationEntry.COLUMN_COORD_LAT, 64.772);
        values.put(LocationEntry.COLUMN_COORD_LONG, -147.355);

        return values;
    }

    static ContentValues createWeatherValues(long locationRowId) {
        ContentValues values = new ContentValues();

        values.put(WeatherEntry.COLUMN_LOC_KEY, locationRowId);
        values.put(WeatherEntry.COLUMN_DATETEXT, TEST_DATE);
        values.put(WeatherEntry.COLUMN_DEGREES, 1.1);
        values.put(WeatherEntry.COLUMN_HUMIDITY, 1.2);
        values.put(WeatherEntry.COLUMN_PRESSURE, 1.3);
        values.put(WeatherEntry.COLUMN_MAX_TEMP, 75);
        values.put(WeatherEntry.COLUMN_MIN_TEMP, 65);
        values.put(WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
        values.put(WeatherEntry.COLUMN_WIND_SPEED, 5.5);
        values.put(WeatherEntry.COLUMN_WEATHER_ID, 321);

        return values;
    }

    static private void validateCursor(ContentValues values, Cursor cursor) {
        Set<Map.Entry<String, Object>> valueSet = values.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int index = cursor.getColumnIndex(columnName);
            assertTrue(index != -1);
            String value = entry.getValue().toString();
            assertEquals(value, cursor.getString(index));
        }
    }

}
