package com.fizix.sunshine;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.fizix.sunshine.data.WeatherContract.LocationEntry;
import com.fizix.sunshine.data.WeatherContract.WeatherEntry;
import com.fizix.sunshine.data.WeatherDbHelper;

import java.util.Map;
import java.util.Set;

public class TestDb extends AndroidTestCase {

    private static final String LOG_TAG = TestDb.class.getSimpleName();

    public void testCreateDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase dbHelper = new WeatherDbHelper(this.mContext).getWritableDatabase();
        assertEquals(true, dbHelper.isOpen());
        dbHelper.close();
    }

    public void testInsertRead() throws Throwable {
        // Test data we're going to insert into the db.

        WeatherDbHelper dbHelper = new WeatherDbHelper(this.mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues locationValues = getLocationContentValues();

        long locationRowId;
        locationRowId = db.insert(LocationEntry.TABLE_NAME, null, locationValues);

        // Make sure we got a row back.
        assertTrue(locationRowId != -1);

        Cursor locationCursor = db.query(LocationEntry.TABLE_NAME, null, null, null, null, null, null);

        if (locationCursor.moveToFirst()) {
            validateCursor(locationValues, locationCursor);
        } else {
            fail("No location values returned");
        }

        ContentValues weatherValues = createWeatherValues(locationRowId);

        long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);
        assertTrue(weatherRowId != -1);

        Cursor weatherCursor = db.query(WeatherEntry.TABLE_NAME, null, null, null, null, null, null);

        if (weatherCursor.moveToFirst()) {
            validateCursor(weatherValues, weatherCursor);
        } else {
            fail("No weather values returned");
        }

        dbHelper.close();
    }

    static ContentValues getLocationContentValues() {
        ContentValues values = new ContentValues();

        values.put(LocationEntry.COLUMN_CITY_NAME, "North Pole");
        values.put(LocationEntry.COLUMN_LOCATION_SETTING, "99705");
        values.put(LocationEntry.COLUMN_COORD_LAT, 64.772);
        values.put(LocationEntry.COLUMN_COORD_LONG, -147.355);

        return values;
    }

    static ContentValues createWeatherValues(long locationRowId) {
        ContentValues values = new ContentValues();

        values.put(WeatherEntry.COLUMN_LOC_KEY, locationRowId);
        values.put(WeatherEntry.COLUMN_DATETEXT, "20141205");
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
