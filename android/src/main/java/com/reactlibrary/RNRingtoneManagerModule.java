
package com.reactlibrary;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Promise;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RNRingtoneManagerModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private static final String TYPE_ALARM_KEY = "TYPE_ALARM";
    private static final String TYPE_ALL_KEY = "TYPE_ALL";
    private static final String TYPE_NOTIFICATION_KEY = "TYPE_NOTIFICATION";
    private static final String TYPE_RINGTONE_KEY = "TYPE_RINGTONE";

    final static class SettingsKeys {
        public static final String URI = "uri";
        public static final String TITLE = "title";
        public static final String ARTIST = "artist";
        public static final String SIZE = "size";
        public static final String MIME_TYPE = "mimeType";
        public static final String DURATION = "duration";
        public static final String RINGTONE_TYPE = "ringtoneType";
    }

    public RNRingtoneManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RingtoneManager";
    }
//
//    @ReactMethod
//    public void getRingtones() {
//        getRingtones(RingtoneManager.TYPE_ALL);
//    }

    @ReactMethod
    public void getRingtones() {
        verifyStoragePermissions(getCurrentActivity());
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1){
                out.write(buffer, 0, read);
            }
        }   catch(Exception exp){

        }
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public boolean verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return false;
        }
        return true;
    }

    private boolean checkSystemWritePermission() {
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(this.reactContext);
            Log.d("TAG", "Can Write Settings: " + retVal);
            if(retVal){
                ///Permission granted by the user
                retVal = true;
            }else{
                //permission not granted navigate to permission screen
                openAndroidPermissionsMenu();
                retVal = false;
            }
        }
        return retVal;
    }

    private void openAndroidPermissionsMenu() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + this.reactContext.getPackageName()));
        getCurrentActivity().startActivity(intent);
    }

    @ReactMethod
    public void createRingtone(ReadableMap settings, Promise promise) throws IOException {
        String uriStr = settings.getString(SettingsKeys.URI);
        Log.i("createRingtone =====>", uriStr);

        if (!checkSystemWritePermission() || !verifyStoragePermissions(getCurrentActivity())) {
            Log.i("createRingtone =====>", "NEED_PERMISSIONS");
            promise.resolve("NEED_PERMISSIONS");
            return;
        }

        File ringtone = new File(uriStr);
        Log.i("createRingtone =====>", ringtone.getAbsolutePath());

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, ringtone.getAbsolutePath());
        values.put(MediaStore.MediaColumns.TITLE, settings.getString(SettingsKeys.TITLE));
        values.put(MediaStore.MediaColumns.SIZE, settings.getInt(SettingsKeys.SIZE));
        values.put(MediaStore.MediaColumns.MIME_TYPE, settings.getString(SettingsKeys.MIME_TYPE));
        values.put(MediaStore.Audio.Media.ARTIST, settings.getString(SettingsKeys.ARTIST));
        values.put(MediaStore.Audio.Media.DURATION, settings.getInt(SettingsKeys.DURATION));
        int ringtoneType = settings.getInt(SettingsKeys.RINGTONE_TYPE);
        values.put(MediaStore.Audio.Media.IS_RINGTONE, isRingtoneType(ringtoneType, RingtoneManager.TYPE_RINGTONE));
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, isRingtoneType(ringtoneType, RingtoneManager.TYPE_NOTIFICATION));
        values.put(MediaStore.Audio.Media.IS_ALARM, isRingtoneType(ringtoneType, RingtoneManager.TYPE_ALARM));
        values.put(MediaStore.Audio.Media.IS_MUSIC, false);

        Log.i("createRingtone =====>", String.valueOf(ringtone.exists()));

        if (ringtone.exists() && getCurrentActivity() != null) {
            ContentResolver contentResolver = getCurrentActivity().getContentResolver();
            Uri uri = MediaStore.Audio.Media.getContentUriForPath(ringtone.getAbsolutePath());
            Uri newUri = contentResolver.insert(uri, values);

            RingtoneManager.setActualDefaultRingtoneUri(
                    getCurrentActivity(),
                    ringtoneType,
                    newUri
            );
            Log.i("createRingtone =====>", "SUCESS");
            promise.resolve("SUCESS");
        }
        promise.resolve("ERROR");
    }

    @ReactMethod
    public void setRingtone(String uri) {

    }

    @ReactMethod
    public void pickRingtone() {

    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(TYPE_ALARM_KEY, RingtoneManager.TYPE_ALARM);
        constants.put(TYPE_ALL_KEY, RingtoneManager.TYPE_ALL);
        constants.put(TYPE_NOTIFICATION_KEY, RingtoneManager.TYPE_NOTIFICATION);
        constants.put(TYPE_RINGTONE_KEY, RingtoneManager.TYPE_RINGTONE);
        return constants;
    }

    /**
     * Returns true when the given ringtone type matches the ringtone to compare.
     * Will default to true if the given ringtone type is RingtoneManager.TYPE_ALL.
     * @param ringtoneType ringtone type given
     * @param ringtoneTypeToCompare ringtone type to compare to
     * @return true if the type matches or is TYPE_ALL
     */
    private boolean isRingtoneType(int ringtoneType, int ringtoneTypeToCompare) {
        return ringtoneTypeToCompare == ringtoneType || RingtoneManager.TYPE_ALL == ringtoneType;
    }
}
