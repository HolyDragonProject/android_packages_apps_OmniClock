/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnirom.deskclock;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * This widget display an analogic clock with two hands for hours and
 * minutes.
 */
public class AnalogClock extends View {
    private Time mCalendar;
    private boolean mAttached;
    private final Handler mHandler = new Handler();
    private float mSeconds;
    private float mMinutes;
    private float mHour;
    private final Context mContext;
    private String mTimeZoneId;
    private boolean mNoSeconds;
    private Paint mCirclePaint;
    private Paint mRemaingCirclePaint;
    private boolean mIsWorldClock;
    private float mCircleStrokeWidth;
    private Paint mBgPaint;
    private Paint mHourPaint;
    private Paint mMinutePaint;
    private Paint mSecondsPaint;
    private Paint mCenterDotPaint;
    private float mHandEndLength;
    private boolean mShowAlarm = false;
    private Paint mAlarmPaint;

    public AnalogClock(Context context) {
        this(context, null);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClock(Context context, AttributeSet attrs,
                       int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        Resources r = mContext.getResources();

        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setColor(r.getColor(R.color.primary));

        mRemaingCirclePaint = new Paint();
        mRemaingCirclePaint.setAntiAlias(true);
        mRemaingCirclePaint.setStyle(Paint.Style.STROKE);
        mRemaingCirclePaint.setColor(r.getColor(R.color.accent));

        mBgPaint = new Paint();
        mBgPaint.setAntiAlias(true);
        mBgPaint.setStyle(Paint.Style.FILL);
        mBgPaint.setColor(r.getColor(R.color.analog_clock_bg_color));

        mHourPaint = new Paint();
        mHourPaint.setAntiAlias(true);
        mHourPaint.setStyle(Paint.Style.STROKE);
        mHourPaint.setColor(r.getColor(R.color.analog_clock_hour_hand_color));

        mMinutePaint = new Paint();
        mMinutePaint.setAntiAlias(true);
        mMinutePaint.setStyle(Paint.Style.STROKE);
        mMinutePaint.setColor(r.getColor(R.color.analog_clock_minute_hand_color));

        mSecondsPaint = new Paint();
        mSecondsPaint.setAntiAlias(true);
        mSecondsPaint.setStyle(Paint.Style.STROKE);
        mSecondsPaint.setColor(r.getColor(R.color.analog_clock_seconds_hand_color));

        mCenterDotPaint = new Paint();
        mCenterDotPaint.setAntiAlias(true);
        mCenterDotPaint.setStyle(Paint.Style.FILL);
        mCenterDotPaint.setColor(r.getColor(R.color.accent));

        mAlarmPaint = new Paint();
        mAlarmPaint.setAntiAlias(true);
        mAlarmPaint.setStyle(Paint.Style.STROKE);
        mAlarmPaint.setColor(r.getColor(R.color.analog_clock_alarm_color));

        setWorldClock(false);
        mCalendar = new Time();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            getContext().registerReceiver(mIntentReceiver, filter, null, mHandler);
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = new Time();

        // Make sure we update to the current time
        onTimeChanged();

        // tick the seconds
        post(mClockTick);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            removeCallbacks(mClockTick);
            mAttached = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int availableWidth = getWidth();
        int availableHeight = getHeight();

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        float radius = availableHeight / 2 - mCircleStrokeWidth;
        RectF arcRect = new RectF();
        arcRect.top = y - radius;
        arcRect.bottom = y + radius;
        arcRect.left =  x - radius;
        arcRect.right = x + radius;
        canvas.drawArc(arcRect, 0, 360, true, mBgPaint);
        canvas.drawArc(arcRect, 0, 360, false, mCirclePaint);
        float minuteStartAngle = mMinutes / 60.0f * 360.0f;
        if (minuteStartAngle < 90) {
            canvas.drawArc(arcRect, 270f + minuteStartAngle, 90f - minuteStartAngle, false, mRemaingCirclePaint);
            canvas.drawArc(arcRect, 0f, 270f, false, mRemaingCirclePaint);
        } else {
            canvas.drawArc(arcRect, minuteStartAngle - 90f, 360f - minuteStartAngle, false, mRemaingCirclePaint);
        }

        if (mShowAlarm && !mIsWorldClock) {
            long nextAlamMilis = Utils.getNextAlarmInMillis(mContext);
            if (nextAlamMilis != -1) {
                Calendar alarmTime = Calendar.getInstance();
                alarmTime.setTimeInMillis(nextAlamMilis);
                Calendar twelveHoursFromNow = Calendar.getInstance();
                twelveHoursFromNow.setTime(new Date());
                twelveHoursFromNow.add(Calendar.HOUR_OF_DAY, 12);
                if (alarmTime.before(twelveHoursFromNow)) {
                    float hour = alarmTime.get(Calendar.HOUR_OF_DAY);
                    float minute = alarmTime.get(Calendar.MINUTE);
                    hour = hour + minute / 60.0f;
                    float angle = hour / 12.0f * 360.0f - 90;
                    RectF arcRectInset = new RectF(arcRect);
                    arcRectInset.inset(mCircleStrokeWidth, mCircleStrokeWidth);
                    canvas.drawArc(arcRectInset, angle - 4, 4, false, mAlarmPaint);
                }
            }
        }
        drawHand(canvas, mHourPaint, x, y, radius * 0.70f, mHour / 12.0f * 360.0f - 90);
        drawHand(canvas, mMinutePaint, x, y, radius + mCircleStrokeWidth / 2, mMinutes / 60.0f * 360.0f - 90);
        if (!mNoSeconds) {
            drawHand(canvas, mSecondsPaint, x, y, radius + mCircleStrokeWidth / 2, mSeconds / 60.0f * 360.0f - 90);
        }
        canvas.drawCircle(x, y, mHourPaint.getStrokeWidth(), mCenterDotPaint);
    }

    private void drawHand(Canvas canvas, Paint mHandPaint, int x, int y, float length, float angle) {
        canvas.save();
        canvas.rotate(angle, x, y);
        canvas.drawLine(x, y, x + length, y, mHandPaint);
        canvas.drawLine(x, y, x - mHandEndLength, y, mHandPaint);
        canvas.restore();
    }

    private void onTimeChanged() {
        mCalendar.setToNow();

        if (mTimeZoneId != null) {
            mCalendar.switchTimezone(mTimeZoneId);
        }

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;
        int second = mCalendar.second;

        mSeconds = second;
        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;

        updateContentDescription(mCalendar);
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }
            onTimeChanged();
            invalidate();
        }
    };

    private final Runnable mClockTick = new Runnable () {

        @Override
        public void run() {
            onTimeChanged();
            invalidate();
            AnalogClock.this.postDelayed(mClockTick, 1000);
        }
    };

    private void updateContentDescription(Time time) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        String contentDescription = DateUtils.formatDateTime(mContext,
                time.toMillis(false), flags);
        setContentDescription(contentDescription);
    }

    public void setTimeZone(String id) {
        mTimeZoneId = id;
        onTimeChanged();
    }

    public void enableSeconds(boolean enable) {
        mNoSeconds = !enable;
    }

    public void setWorldClock(boolean world) {
        mIsWorldClock = world;
        mCircleStrokeWidth = mIsWorldClock ? getResources().getDimension(R.dimen.world_clock_circle_size) :
                getResources().getDimension(R.dimen.main_clock_circle_size);
        mCirclePaint.setStrokeWidth(mCircleStrokeWidth);
        mRemaingCirclePaint.setStrokeWidth(mCircleStrokeWidth);

        mHourPaint.setStrokeWidth(mIsWorldClock ?
                getResources().getDimensionPixelSize(R.dimen.world_clock_hour_hand_width) :
                getResources().getDimensionPixelSize(R.dimen.main_clock_hour_hand_width));

        mMinutePaint.setStrokeWidth(mIsWorldClock ?
                getResources().getDimensionPixelSize(R.dimen.world_clock_minute_hand_width) :
                getResources().getDimensionPixelSize(R.dimen.main_clock_minute_hand_width));

        mSecondsPaint.setStrokeWidth(mIsWorldClock ?
                getResources().getDimensionPixelSize(R.dimen.world_clock_seconds_hand_width) :
                getResources().getDimensionPixelSize(R.dimen.main_clock_seconds_hand_width));

        mAlarmPaint.setStrokeWidth(mCircleStrokeWidth);

        mHandEndLength = mIsWorldClock ? 0 :
                getResources().getDimensionPixelSize(R.dimen.main_clock_hand_end_length);
    }
}

