package com.bcf.watchface.bcfwearcore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by annguyenquocduy on 12/27/14.
 */
public class WatchFaceUtility {
    private static final String TAG = WatchFaceUtility.class.getSimpleName();
    private static final String[] MONTHS_NAME_LIST = {"JAN", "Feb", "Mar", "Apr", "May", "June",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] WEEK_DAY_LIST = {"Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday", "Sunday"};

    /**
     * Callback interface to perform an action with the current config {@link com.google.android.gms.wearable.DataMap} for
     * {@link BaseWatchFaceService}.
     */
    public interface FetchConfigDataMapCallback {
        /**
         * Callback invoked with the current config {@link com.google.android.gms.wearable.DataMap} for
         * {@link BaseWatchFaceService}.
         */
        void onConfigDataMapFetched(DataMap config);
    }

    /**
     * Asynchronously fetches the current config {@link com.google.android.gms.wearable.DataMap}
     * for {@link BaseWatchFaceService}
     * and passes it to the given callback.
     * <p/>
     * If the current config {@link com.google.android.gms.wearable.DataItem} doesn't exist,
     * it isn't created and the callback
     * receives an empty DataMap.
     */
    public static void fetchConfigDataMap(final GoogleApiClient client,
                                          final FetchConfigDataMapCallback callback, final String path) {
        Wearable.NodeApi.getLocalNode(client).setResultCallback(
                new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                        String localNode = getLocalNodeResult.getNode().getId();
                        Uri uri = new Uri.Builder()
                                .scheme("wear")
                                .path(path)
                                .authority(localNode)
                                .build();
                        Wearable.DataApi.getDataItem(client, uri)
                                .setResultCallback(new DataItemResultCallback(callback));
                    }
                }
        );
    }

    /**
     * Overwrites the current config {@link DataItem}'s {@link DataMap} with {@code newConfig}.
     * If the config DataItem doesn't exist, it's created.
     */
    public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig, String path) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        DataMap configToPut = putDataMapRequest.getDataMap();
        configToPut.putAll(newConfig);
        Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d(TAG, "putDataItem result status: " + dataItemResult.getStatus());
                    }
                });
    }

    /**
     * Overwrites (or sets, if not present) the keys in the current config {@link DataItem} with
     * the ones appearing in the given {@link DataMap}. If the config DataItem doesn't exist,
     * it's created.
     * <p/>
     * It is allowed that only some of the keys used in the config DataItem appear in
     * {@code configKeysToOverwrite}. The rest of the keys remains unmodified in this case.
     */
    public static void overwriteKeysInConfigDataMap(final GoogleApiClient googleApiClient,
                                                    final DataMap configKeysToOverwrite, final String path) {

        fetchConfigDataMap(googleApiClient,
                new FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap currentConfig) {
                        DataMap overwrittenConfig = new DataMap();
                        overwrittenConfig.putAll(currentConfig);
                        overwrittenConfig.putAll(configKeysToOverwrite);
                        putConfigDataItem(googleApiClient, overwrittenConfig, path);
                    }
                }, path
        );
    }

    private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult> {

        private final FetchConfigDataMapCallback mCallback;

        public DataItemResultCallback(FetchConfigDataMapCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onResult(DataApi.DataItemResult dataItemResult) {
            if (dataItemResult.getStatus().isSuccess()) {
                if (dataItemResult.getDataItem() != null) {
                    DataItem configDataItem = dataItemResult.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                    DataMap config = dataMapItem.getDataMap();
                    mCallback.onConfigDataMapFetched(config);
                } else {
                    mCallback.onConfigDataMapFetched(new DataMap());
                }
            }
        }
    }

    /**
     * General Utility
     */

    public static Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
        Paint paint = new Paint();
        paint.setColor(defaultInteractiveColor);
        paint.setTypeface(typeface);
        paint.setAntiAlias(true);
        return paint;
    }

    public static int convertTo12Hour(int hour) {
        int result = hour % 12;
        return (result == 0) ? 12 : result;
    }

    public static String formatTwoDigitNumber(int hour) {
        return String.format("%02d", hour);
    }

    public static String getAMPMFromHour(int hour) {
        if (hour >= 12)
            return "PM";
        return "AM";
    }

    public static String getMonthNameShortForm(int monthIndex) {
        return MONTHS_NAME_LIST[monthIndex];
    }

    public static String getWeekDayName(int weekDayIndex) {
        return WEEK_DAY_LIST[weekDayIndex];
    }

    public static File setBitmapToStorage(Context context, Bitmap bitmap, String filePath) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File f = new File(context.getExternalFilesDir(null), filePath);
        try {
            if (f.exists())
                f.delete();
            f.createNewFile();
            // write the bytes in file
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            fo.close();
            bytes.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f;
    }

    public static Bitmap getBitmapFromUrl(Context context, String filePath) {
        File f = new File(context.getExternalFilesDir(null), filePath);
        if (f.exists()) {
            return BitmapFactory.decodeFile(f.getPath());
        }
        return null;
    }
}
