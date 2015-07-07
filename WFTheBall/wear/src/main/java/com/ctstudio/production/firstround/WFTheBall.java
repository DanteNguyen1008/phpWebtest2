package com.ctstudio.production.firstround;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowInsets;

import com.ustwo.clockwise.WatchFace;
import com.ustwo.clockwise.WatchFaceTime;
import com.ustwo.clockwise.WatchShape;
import com.ustwo.clockwise.sample.common.ConfigurableConnectedWatchFace;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by anguyen on 6/10/2015.
 */
public class WFTheBall extends ConfigurableConnectedWatchFace {
    private static final String tag = "WFTheBall";
    /**
     * BG, Bitmap and colors
     */
    private static final int BG_BLACK = Color.BLACK;
    private static final int DEFAULT_BG_TEXT = Color.WHITE;
    private static final int DEFAULT_TIME_TEXT_SIZE = 90;
    private static final int DEFAULT_DATE_TEXT_SIZE = 15;
    private static final int DEFAULT_DEGREE_TEXT_SIZE = 18;

    private int mCurrentBackgroundColor = BG_BLACK;
    private int mCurrentTextColor = DEFAULT_BG_TEXT;
    private int mCurrentTimeTextSize = DEFAULT_TIME_TEXT_SIZE;
    private int mCurrentDateTextSize = DEFAULT_DATE_TEXT_SIZE;
    private int mCurrentDegreeTextSize = DEFAULT_DEGREE_TEXT_SIZE;

    private Bitmap mBackground;

    /**
     * Paints
     */

    private Paint mBgPaint = new Paint();

    private Paint mHourPaint = new Paint();
    private Paint mMinutePaint = new Paint();
    private Paint mDatePaint = new Paint();
    private Paint mWeatherPaint = new Paint();
    private Paint mDegreePaint = new Paint();
    private Paint mWeatherIconPaint = new Paint();

    private Paint mSecondIndicatorPaint = new Paint();


    /**
     * Center position of WF
     */

    private PointF mWFCenter = new PointF(0f, 0f);

    /**
     * Current date
     */

    private Date mDate = new Date();

    /**
     * Current time text (will be drawn on next draw cycle)
     */
    private String mTimeText = "00:00";

    /**
     * Current date text (will be drawn on next draw cycle)
     */
    private String mDateText = "";

    /*
    * Weather short description
    * */

    private String mWeatherText;

    /*
    * Degree
    * */

    private String mDegreeText;

    /*weather icon bitmap*/
    private Bitmap mWeatherIconBitmap;

    /**
     * Simpleformat date time
     */

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("MMM - dd");
    private SimpleDateFormat mTimeFormat12h = new SimpleDateFormat("h:mm");
    private SimpleDateFormat mTimeFormat24h = new SimpleDateFormat("HH:mm");

    /*
    * Dimensions
    * */

    private float mDMXHour, mDMYHour, mDMXMinute, mDMYMinute;
    private float mSecondLineEndY;
    private float mDMXDate, mDMYDate, mDMXWeather, mDMYWeather, mDMXDegree, mDMYDegree;
    private float mDMXWeatherIcon, mDMYWeatherIcon;
    private float mWeatherIconWidth, mWeatherIconHeight;

    /**
     * Scales a float dimension from a spec value to the specified screen size
     *
     * @param specValue         The spec value to scale
     * @param currentRenderSize The screen size
     * @return The scaled dimension
     */
    public static float getFloatValueFromSpec(float specValue, float currentRenderSize) {
        return (specValue / Specs.SPEC_SIZE) * currentRenderSize;
    }

    @Override
    public void onCreate() {
        super.onCreate();


        /*hour paint*/
        mHourPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        mHourPaint.setTextAlign(Paint.Align.CENTER);
        mHourPaint.setColor(mCurrentTextColor);
        mHourPaint.setTextSize(mCurrentTimeTextSize);

        /*minute paint*/
        mMinutePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        mMinutePaint.setTextAlign(Paint.Align.CENTER);
        mMinutePaint.setColor(mCurrentTextColor);
        mMinutePaint.setTextSize(mCurrentTimeTextSize);

        /*date - weather paint*/
        mDatePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        mDatePaint.setTextAlign(Paint.Align.CENTER);
        mDatePaint.setColor(mCurrentTextColor);
        mDatePaint.setTextSize(mCurrentDateTextSize);

        /*degree paint*/
        mDegreePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        mDegreePaint.setTextAlign(Paint.Align.CENTER);
        mDegreePaint.setColor(mCurrentTextColor);
        mDegreePaint.setTextSize(mCurrentDegreeTextSize);

        mBgPaint.setFilterBitmap(true);
        mBgPaint.setDither(true);

        mSecondIndicatorPaint.setStyle(Paint.Style.FILL);
        mSecondIndicatorPaint.setColor(Color.WHITE);
        mSecondIndicatorPaint.setDither(true);
        mSecondIndicatorPaint.setAntiAlias(true);
        mSecondIndicatorPaint.setStrokeWidth(8f);

        /*weather icon paint*/
        mWeatherIconPaint.setDither(true);
        mWeatherIconPaint.setAntiAlias(true);
    }

    @Override
    protected void onLayout(WatchShape watchShape, Rect rect, WindowInsets windowInsets) {
        super.onLayout(watchShape, rect, windowInsets);

        // Convert spec dimensions to current screen size
        float renderSize = Math.min(getWidth(), getHeight());

        mDMXHour = getFloatValueFromSpec(113f, renderSize);
        mDMYHour = getFloatValueFromSpec(130f, renderSize);
        mDMXMinute = getFloatValueFromSpec(113f, renderSize);
        mDMYMinute = getFloatValueFromSpec(249f, renderSize);
        mSecondLineEndY = getFloatValueFromSpec(220f, renderSize);

        mDMXWeatherIcon = getFloatValueFromSpec(255f, renderSize);
        mDMYWeatherIcon = getFloatValueFromSpec(100f, renderSize);

        mDMXDate = getFloatValueFromSpec(270f, renderSize);
        mDMYDate = getFloatValueFromSpec(155f, renderSize);
        mDMXWeather = getFloatValueFromSpec(270f, renderSize);
        mDMYWeather = getFloatValueFromSpec(175f, renderSize);
        mDMXDegree = getFloatValueFromSpec(270f, renderSize);
        mDMYDegree = getFloatValueFromSpec(195f, renderSize);

        mWeatherIconWidth = getFloatValueFromSpec(37f, renderSize);
        mWeatherIconHeight = getFloatValueFromSpec(35f, renderSize);

        /*set center for watch*/
        mWFCenter.set(getWidth() * 0.5f, getHeight() * 0.5f);

        mBackground = BitmapFactory.decodeResource(getResources(), R.drawable.black_bg);
        Bitmap originalWeatherIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.weather_cloud);
        mWeatherIconBitmap = Bitmap.createScaledBitmap(originalWeatherIconBitmap, (int) mWeatherIconWidth, (int) mWeatherIconHeight, true);


        /*UPDATE datetime string */
        updateDateAndTimeText(getTime());

        /*test*/
        mWeatherText = "Clouds".toUpperCase();
        mDegreeText = "74" + (char) 0x00B0 + "C";
    }

    @Override
    protected WatchFaceStyle getWatchFaceStyle() {
        WatchFaceStyle.Builder builder =
                new WatchFaceStyle.Builder(this)
                        .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                        .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_VISIBLE)
                        .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                        .setCardProgressMode(WatchFaceStyle.PROGRESS_MODE_NONE)
                        .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                        .setViewProtection(WatchFaceStyle.PROTECT_HOTWORD_INDICATOR | WatchFaceStyle.PROTECT_STATUS_BAR)
                        .setHotwordIndicatorGravity(Gravity.TOP | Gravity.LEFT)
                        .setStatusBarGravity(Gravity.TOP | Gravity.LEFT)
                        .setShowSystemUiTime(false);
        return builder.build();
    }

    @Override
    protected long getInteractiveModeUpdateRate() {
        return DateUtils.SECOND_IN_MILLIS;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int backgroundBitmapPositionX = -mBackground.getWidth() / 2;
        int backgroundBitmapPositionY = -mBackground.getHeight() / 2;

        //draw bg first
        canvas.save();
        /*translate to center*/
        canvas.translate(mWFCenter.x, mWFCenter.y);
        canvas.drawBitmap(mBackground, backgroundBitmapPositionX, backgroundBitmapPositionY, mBgPaint);

        canvas.restore();
        canvas.save();
        canvas.translate(0, 0);

        /*get time*/
        WatchFaceTime time = getTime();

        /*draw hour*/
        canvas.drawText(String.valueOf(time.hour), mDMXHour, mDMYHour, mHourPaint);
        /*draw minute*/
        canvas.drawText(String.valueOf(time.minute), mDMXMinute, mDMYMinute, mMinutePaint);
        /*draw date*/
        canvas.drawText(mDateText, mDMXDate, mDMYDate, mDatePaint);
        /*draw weather text*/
        canvas.drawText(mWeatherText, mDMXWeather, mDMYWeather, mDatePaint);
        /*draw degree text*/
        canvas.drawText(mDegreeText, mDMXDegree, mDMYDegree, mDegreePaint);
        /*draw weather icon*/
        canvas.drawBitmap(mWeatherIconBitmap, mDMXWeatherIcon, mDMYWeatherIcon, mWeatherIconPaint);

        float finalY = (mSecondLineEndY * time.second) / 60;
        Log.d(getClass().getSimpleName(), "second final Y " + finalY + " end of Y " + mSecondLineEndY);
        canvas.drawLine(0, mWFCenter.y, finalY, mWFCenter.y, mSecondIndicatorPaint);
        canvas.restore();
    }

    private void updateDateAndTimeText(WatchFaceTime timeStamp) {
        mDate.setTime(timeStamp.toMillis(false));

        mDateText = mDateFormat.format(mDate);
        mTimeText = is24HourFormat() ? mTimeFormat24h.format(mDate) :
                mTimeFormat12h.format(mDate);
    }


    /**
     * Called when a change to the SharedPreferences has been observed.
     * Overriding classes should detect if the SharedPreference change is part of their configuration and update
     * accordingly. They should call {@link WatchFace#invalidate()} to redraw if required.
     *
     * @param sharedPreferences SharedPreferences for watch face on wearable
     * @param key               Key for updated shared preference
     */
    @Override
    protected void onWatchFaceConfigChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(tag, "onWatchFaceConfigChanged " + key);
    }
}
