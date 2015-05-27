package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;


public class GroupMessengerProvider extends ContentProvider
{
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        String FILENAME = (String)values.get("key");
        String value = (String)values.get("value");

        try
        {
            FileOutputStream output_stream = getContext().openFileOutput(FILENAME, Context.MODE_PRIVATE);
            output_stream.write(value.getBytes());
            output_stream.close();
        }
        catch (Exception e)
        {

        }

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder)
    {

        MatrixCursor cursor = cursor = new MatrixCursor(new String [] {"key", "value"});;
        try
        {
            InputStream input_stream = getContext().openFileInput(selection);
            BufferedReader br = new BufferedReader(new InputStreamReader(input_stream));
            String value = br.readLine();
            cursor.addRow(new String [] {selection, value});
            Log.v("value", value);
        }
        catch(Exception e)
        {

        }

        Log.v("query", selection);
        return cursor;
    }
}
