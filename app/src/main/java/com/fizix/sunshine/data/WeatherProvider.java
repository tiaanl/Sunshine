package com.fizix.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class WeatherProvider extends ContentProvider {

    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;

    private static final int LOCATION = 200;
    private static final int LOCATION_ID = 201;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_LOCATION + "/#", LOCATION_ID);

        return matcher;
    }

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private WeatherDbHelper mDbHelper;
    private static SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static {
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationSettingQueryBuilder.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME + " ON " +
                        WeatherContract.WeatherEntry.TABLE_NAME + "." +
                        WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " = " +
                        WeatherContract.LocationEntry.TABLE_NAME + "." +
                        WeatherContract.LocationEntry._ID
        );
    }

    private static final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME + "." +
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";
    private static final String sLocationSettingWithStartDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME + "." +
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATETEXT + " >= ? ";
    private static final String sLocationSettingWithDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME + "." +
                    WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATETEXT + " = ? ";

    Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == null) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selection = sLocationSettingWithStartDateSelection;
            selectionArgs = new String[]{locationSetting, startDate};
        }

        return sWeatherByLocationSettingQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sLocationSettingWithDateSelection,
                new String[]{locationSetting, date},
                null,
                null,
                sortOrder
        );
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor = null;

        switch (sUriMatcher.match(uri)) {
            case WEATHER_WITH_LOCATION_AND_DATE: {
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            }

            case WEATHER_WITH_LOCATION: {
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            }

            case WEATHER: {
                retCursor = mDbHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            case LOCATION_ID: {
                retCursor = mDbHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        WeatherContract.LocationEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            case LOCATION: {
                retCursor = mDbHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;

            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;

            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;

            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;

            case LOCATION_ID:
                return WeatherContract.LocationEntry.CONTENT_ITEM_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri retUri;

        switch (match) {
            case WEATHER: {
                long id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, contentValues);
                if (id > 0)
                    retUri = WeatherContract.WeatherEntry.buildWeatherUri(id);
                else
                    throw new SQLException("Failed to insert row into " + uri);
                break;
            }

            case LOCATION: {
                long id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, contentValues);
                if (id > 0)
                    retUri = WeatherContract.LocationEntry.buildLocationUri(id);
                else
                    throw new SQLException("Failed to insert row into " + uri);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify observers that the data changed.
        getContext().getContentResolver().notifyChange(uri, null);

        return retUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted = 0;

        switch (match) {
            case WEATHER: {
                rowsDeleted = db.delete(WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }

            case LOCATION: {
                rowsDeleted = db.delete(WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (selection == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated = 0;

        switch (match) {
            case WEATHER: {
                rowsUpdated = db.update(WeatherContract.WeatherEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            }

            case LOCATION: {
                rowsUpdated = db.update(WeatherContract.LocationEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (selection == null || rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case WEATHER: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);

                return returnCount;
            }

            default:
                return super.bulkInsert(uri, values);

        }
    }
}
