package production.ctstudio.com.watchfaceanimation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.concurrent.TimeUnit;

/**
 * Created by anguyen on 6/29/2015.
 */
public class AnimationWF extends CanvasWatchFaceService {
    protected static final String TAG = "AnimationWF";
    protected static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    public class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        protected Time mTime;
        protected Bitmap mBackgroundBitmap;
        protected Bitmap mBackgroundScaledBitmap;
        protected Paint mPaint = new Paint();

        /**
         * Handler to update the time once a second in interactive mode.
         *
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

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
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

        protected void onAmbientModeChangeUpdate(boolean inAmbientMode) {

        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mBackgroundBitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.RGB_565);
            mBackgroundBitmap.eraseColor(Color.BLACK);

            mPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            mPaint.setTextSize(20f);
            mPaint.setColor(Color.BLUE);
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
    }
}
