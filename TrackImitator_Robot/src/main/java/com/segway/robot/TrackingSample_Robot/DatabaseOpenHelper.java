package com.segway.robot.TrackingSample_Robot;

import android.content.Context;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Created by LCabrera on 24/10/2017.
 */

public class DatabaseOpenHelper extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "loomoRemote";
    private static final int DATABASE_VERSION = 1;

    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

}