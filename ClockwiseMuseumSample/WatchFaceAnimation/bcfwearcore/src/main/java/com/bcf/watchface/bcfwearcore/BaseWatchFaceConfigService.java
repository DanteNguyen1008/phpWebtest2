package com.bcf.watchface.bcfwearcore;

import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by annguyenquocduy on 2/8/15.
 */

public abstract class BaseWatchFaceConfigService extends BaseWatchFaceService {

    protected abstract class Engine extends BaseWatchFaceService.Engine {
        private static final String TAG = "BaseWatchConfig";
        private static final boolean D = false;
        private GoogleApiClient googleApiClient;
        private MessageApi.MessageListener mMessageListener;
        private Node phoneNode = null;

        private DataApi.DataListener mDataListener = new DataApi.DataListener() {
            @Override
            public void onDataChanged(DataEventBuffer dataEvents) {
                try {
                    for (DataEvent dataEvent : dataEvents) {
                        if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                            continue;
                        }

                        DataItem dataItem = dataEvent.getDataItem();
                        if (!dataItem.getUri().getPath().equals(getPathConfig())) {
                            continue;
                        }

                        DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                        DataMap config = dataMapItem.getDataMap();
                        Log.d(TAG, "Config DataItem updated:" + config);

                        updateUiForConfigDataMap(config);
                    }
                } finally {
                    dataEvents.close();
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            googleApiClient = setUpGoogleAPI();

            if (googleApiClient != null)
                googleApiClient.connect();

            setWatchFaceStyle(new WatchFaceStyle.Builder(BaseWatchFaceConfigService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
        }

        protected void findPhoneNode() {
            PendingResult<NodeApi.GetConnectedNodesResult> pending = Wearable.NodeApi
                    .getConnectedNodes(googleApiClient);
            pending.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(NodeApi.GetConnectedNodesResult result) {
                    if (result.getNodes().size() > 0) {
                        phoneNode = result.getNodes().get(0);
                        if (D)
                            Log.d(TAG, "Found wearable: name=" + phoneNode.getDisplayName() +
                                    ", id=" + phoneNode.getId());
                    } else {
                        phoneNode = null;
                    }
                }
            });
        }

        protected void sendToPhone(String path, byte[] data,
                                   final ResultCallback<MessageApi.SendMessageResult> callback) {
            if (phoneNode != null) {
                PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi
                        .sendMessage(googleApiClient, phoneNode.getId(), path, data);
                pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult result) {
                        if (callback != null) {
                            callback.onResult(result);
                        }
                        if (!result.getStatus().isSuccess()) {
                            if (D)
                                Log.d(TAG, "ERROR: failed to send Message: " + result.getStatus());
                        }
                    }
                });
            } else {
                if (D) Log.d(TAG, "ERROR: tried to send message before device was found");
            }
        }

        protected void updateConfigDataItemAndUiOnStartup() {
            WatchFaceUtility.fetchConfigDataMap(googleApiClient,
                    new WatchFaceUtility.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            // If the DataItem hasn't been created yet or some keys are missing,
                            // use the default values.
                            setDefaultValuesForMissingConfigKeys(startupConfig);
                            WatchFaceUtility.putConfigDataItem(googleApiClient, startupConfig, getPathConfig());
                            updateUiForConfigDataMap(startupConfig);
                        }
                    }
                    , getPathConfig());
        }

        protected void addKeyIfMissing(DataMap config, String key, Object value) {
            if (!config.containsKey(key)) {
                if (value instanceof Integer)
                    config.putInt(key, Integer.parseInt((String) value));
                if (value instanceof String)
                    config.putString(key, (String) value);
                if (value instanceof Float)
                    config.putFloat(key, (Float) value);
                if (value instanceof Double)
                    config.putDouble(key, (Double) value);
                if (value instanceof Boolean)
                    config.putBoolean(key, (Boolean) value);
            }
        }

        protected GoogleApiClient getGoogleApiClient() {
            return googleApiClient;
        }

        protected MessageApi.MessageListener getMessageListener() {
            return mMessageListener;
        }

        protected DataApi.DataListener getDataListener() {
            return mDataListener;
        }


        @Override
        protected void onAmbientModeChangeUpdate(boolean inAmbientMode) {

        }

        @Override
        protected void onInterruptionChangeUpdate(boolean inMuteMode) {

        }

        @Override
        protected void setUpDebugPaint() {

        }

        protected abstract MessageApi.MessageListener setUpMessageListener();

        protected abstract GoogleApiClient setUpGoogleAPI();

        protected abstract void setDefaultValuesForMissingConfigKeys(DataMap config);

        protected abstract String getPathConfig();

        protected abstract void updateUiForConfigDataMap(final DataMap config);

    }
}
