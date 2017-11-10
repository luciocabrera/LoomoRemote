package com.segway.robot.TrackingSample_Phone;

/**
 * Created by LCabrera on 24/10/2017.
 */
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseAccess extends SQLiteOpenHelper {
    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase database;
    private static DatabaseAccess instance;
    private static final String DATABASE_NAME = "loomoRemote";
    private static final int DATABASE_VERSION = 1;
    /**
     * Private constructor to avoid object creation from outside classes.
     *
     * @param context
     */
    public DatabaseAccess(Context context) {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
        this.openHelper = new DatabaseOpenHelper(context);
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //sqLiteDatabase.execSQL(CREATE_QUERY);
        Log.e("DATABASE OPERATION", "Table create...");
    }

    /**
     * Return a singleton instance of DatabaseAccess.
     *
     * @param context the Context
     * @return the instance of DabaseAccess
     */
    public static DatabaseAccess getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseAccess(context);
        }
        return instance;
    }

    /**
     * Open the database connection.
     */
    public void open() {
        // this.database = openHelper.getWritableDatabase();
        this.database = openHelper.getReadableDatabase();
    }

    /**
     * Close the database connection.
     */
    public void close() {
        if (database != null) {
            this.database.close();
        }
    }

    /**
     * Read all quotes from the database.
     *
     * @return a List of contacts
     */
    public List<String> getNames() {
        List<String> list = new ArrayList<>();
       // Cursor cursor = database.rawQuery("SELECT name, greeting FROM contacts", null);
        String[] projections = {"name"};
        Cursor cursor = database.query("contacts",projections,null,null,null,null,null);


        if(cursor.moveToFirst())
        {
            do {
                String dataContact = cursor.getString(0);

                //list.add(cursor.getString(0));
                list.add(dataContact);

            }while (cursor.moveToNext());
        }
        //}
        //    cursor.moveToFirst();
        //    while (!cursor.isAfterLast()) {
        //        list.add(cursor.getString(0));
        //        cursor.moveToNext();
        //    }
        //   cursor.close();
        return list;
    }

    /**
     * Read the BLOB data as byte[]
     *
     * @param name name of the contact
     * @return image as byte[]
     */
    public byte[] getImage(String name) {
        byte[] data = null;
        Cursor cursor = database.rawQuery("SELECT picture FROM contacts WHERE name = ?", new String[]{name});
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            data = cursor.getBlob(0);
            break;  // Assumption: name is unique
        }
        cursor.close();
        return data;
    }
}