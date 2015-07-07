package com.bcf.watchface.bcfwearcore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by annguyenquocduy on 12/26/14.
 */
public abstract class BaseWatchFaceService extends CanvasWatchFaceService {
    protected static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final String TAG = BaseWatchFaceService.class.getSimpleName();

    @Override
    public abstract Engine onCreateEngine();

    protected abstract class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;
        protected boolean IS_DEBUG_DRAW = false;

        protected boolean mMute;
        protected Time mTime;
        protected Bitmap mBackgroundBitmap;
        protected Bitmap mBackgroundScaledBitmap;
        protected boolean mRegisteredTimeZoneReceiver = false;
        protected Paint debugPaint;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        /**
         * Handler to update the time once a second in interactive mode.
         */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        protected final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            mTime = new Time();
            setUpDebugPaint();
        }


        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        protected void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            if (mLowBitAmbient) {
                onAmbientModeChangeUpdate(inAmbientMode);
            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }


        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                onInterruptionChangeUpdate(inMuteMode);
                invalidate();
            }
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }
        }


        /**
         * Draw background and set time to current time.
         * */
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();

            if(mBackgroundBitmap != null) {
                // Draw the background, scaled to fit.
                if (mBackgroundScaledBitmap == null
                        || mBackgroundScaledBitmap.getWidth() != width
                        || mBackgroundScaledBitmap.getHeight() != height) {
                    mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                            width, height, true /* filter */);
                }
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            }
        }

        protected void drawDebugLines(Canvas canvas, float centerX, float centerY, float width,
                                      float height) {
            if(IS_DEBUG_DRAW && debugPaint != null) {
                /*draw indicator lines to center*/
                canvas.drawLine(0, centerY, width, centerY, debugPaint);
                canvas.drawLine(centerX, 0, centerX, height, debugPaint);
            }
        }

        protected void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            BaseWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        protected void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            BaseWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        protected abstract void onAmbientModeChangeUpdate(boolean inAmbientMode);

        protected abstract void onInterruptionChangeUpdate(boolean inMuteMode);

        protected abstract void setUpDebugPaint();


    }

}
