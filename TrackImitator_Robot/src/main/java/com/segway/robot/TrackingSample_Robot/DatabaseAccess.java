package com.segway.robot.TrackingSample_Robot;

/**
 * Created by LCabrera on 24/10/2017.
 */

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;


public class DatabaseAccess extends Activity  {
    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase database;
    private static DatabaseAccess instance;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
   }

    private void initView() {
        mTextView = (TextView) findViewById(R.id.tvHint);

    }

    private DatabaseAccess(Context context) {
        this.openHelper = new DatabaseOpenHelper(context);
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
        this.database = openHelper.getWritableDatabase();
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
     * Read all names from the contacts database.
     *
     * @return a List of contact names
     */
    public List<String> getNames() {
        List<String> list = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT name FROM contacts", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    /**
     * Read the BLOB data as byte[]
     *
     * @param key key of the contact
     * @return image as byte[]
     */
    public byte[] getImage(String key) {
        byte[] data = null;
        Cursor cursor = database.rawQuery("SELECT picture FROM contacts WHERE key = ?",new String[]{key});
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            data = cursor.getBlob(0);
            break;  // Assumption: key is unique
        }
        cursor.close();
        return data;
    }

    public String getNameContact(String key) {
        String data = null;
        Cursor cursor = database.rawQuery("SELECT name FROM contacts WHERE key = ?",new String[]{key});
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            data = cursor.getString(0);
            break;  // Assumption: key is unique
        }
        cursor.close();
        return data;
    }

    public String getDataContact(String key) {
        String greeting = null;
        String name = null;
        String question = null;
        String message = null;
        Cursor cursor = database.rawQuery("SELECT greeting, name, question, message FROM contacts WHERE key = ?",new String[]{key});
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            greeting =  cursor.getString(0);
            name =  cursor.getString(1);
            question =  cursor.getString(2);
            message =  cursor.getString(3);
            break;  // Assumption: key is unique
        }
        cursor.close();
        return  greeting + " " +  name + " " +  question ;

    }

    public String getDialogs(){

        String sentence = "";
        String response = "";
        Cursor cursor =  database.rawQuery("SELECT sentence, response FROM dialogs", null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()){
            sentence =  cursor.getString(0);
            response =  cursor.getString(1);
       }
        cursor.close();

        return response;
    }
}