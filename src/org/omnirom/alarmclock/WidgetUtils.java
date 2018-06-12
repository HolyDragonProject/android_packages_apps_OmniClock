/*
 * Copyright (C) 2012 The Android Open Source Project
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

package org.omnirom.alarmclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.RemoteViews;

import org.omnirom.deskclock.AlarmUtils;
import org.omnirom.deskclock.Utils;
import org.omnirom.deskclock.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class WidgetUtils {
    static final String TAG = "WidgetUtils";

    // Decide if to show the list of world clock.
    // Check to see if the widget size is big enough, if it is return true.
    public static boolean showList(Context context, int id) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        if (widgetManager == null) {
            // no manager to make the calculation, show the list anyway
            return true;
        }
        Bundle options = widgetManager.getAppWidgetOptions(id);
        if (options == null) {
            // no data to make the calculation, show the list anyway
            return true;
        }
        Resources res = context.getResources();
        String whichHeight = res.getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT
                ? AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                : AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
        int height = options.getInt(whichHeight);
        if (height == 0) {
            // no data to make the calculation, show the list anyway
            return true;
        }
        float density = res.getDisplayMetrics().density;
        // Estimate height of date text box
        float lblBox = 1.35f * res.getDimension(R.dimen.label_font_size);
        float neededSize = res.getDimension(R.dimen.digital_widget_list_min_fixed_height) +
                2 * lblBox +
                res.getDimension(R.dimen.digital_widget_list_min_scaled_height);
        return ((density * height) > neededSize);
    }

    /***
     * Set the format of the time on the clock accrding to the locale
     *
     * @param clock        - view to format
     * @param amPmFontSize - size of am/pm label, zero size means no am/om label
     * @param clockId      - id of TextClock view as defined in the clock's layout.
     */
    public static void setTimeFormat(RemoteViews clock, int amPmFontSize, int clockId, int secondsSize) {
        if (clock != null) {
            // Set the best format for 12 hours mode according to the locale
            clock.setCharSequence(clockId, "setFormat12Hour", Utils.get12ModeFormat(amPmFontSize, secondsSize));
            // Set the best format for 24 hours mode according to the locale
            clock.setCharSequence(clockId, "setFormat24Hour", Utils.get24ModeFormat(secondsSize));
        }
    }

    public static boolean isShowingAlarm(Context context, int id, boolean defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(CustomAppWidgetConfigure.KEY_SHOW_ALARM + "_" + id, defaultValue);
    }

    public static boolean isShowingDate(Context context, int id, boolean defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(CustomAppWidgetConfigure.KEY_SHOW_DATE + "_" + id, defaultValue);
    }

    public static boolean isClockShadow(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(CustomAppWidgetConfigure.KEY_CLOCK_SHADOW + "_" + id, true);
    }

    public static boolean isShowingWorldClock(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(CustomAppWidgetConfigure.KEY_WORLD_CLOCKS + "_" + id, true);
    }

    public static boolean isShowingNumbers(Context context, int id, boolean defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(CustomAnalogAppWidgetConfigure.KEY_SHOW_NUMBERS + "_" + id, defaultValue);
    }

    public static boolean isShowingTicks(Context context, int id, boolean defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(CustomAnalogAppWidgetConfigure.KEY_SHOW_TICKS + "_" + id, defaultValue);
    }

    public static boolean isShowing24hours(Context context, int id, boolean defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(CustomAnalogAppWidgetConfigure.KEY_24H_MODE + "_" + id, defaultValue);
    }

    public static Bitmap createTextBitmap(final String text, final Typeface typeface, final float textSizePixels, final int textColor, boolean shadow, float letterSpacing) {
        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePixels);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        if (shadow) {
            textPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
        }
        if (letterSpacing != -1) {
            textPaint.setLetterSpacing(letterSpacing);
        }
        int textHeight = (int) (textPaint.descent() - textPaint.ascent());
        int textOffset = (int) ((textHeight / 2) - textPaint.descent());
        Bitmap myBitmap = Bitmap.createBitmap((int) textPaint.measureText(text), (int) textSizePixels, Bitmap.Config.ARGB_8888);
        Canvas myCanvas = new Canvas(myBitmap);
        myCanvas.drawText(text, myBitmap.getWidth() / 2, myBitmap.getHeight() / 2 + textOffset, textPaint);
        return myBitmap;
    }

    public static Bitmap createTimeBitmap(String timeFormat, final Typeface typeface, final float textSizePixels, final int textColor,
                                          boolean shadow, float letterSpacing, boolean showAmPm) {
        if (showAmPm) {
            // remove any a
            timeFormat = timeFormat.replaceAll("a", "").trim();
        }
        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, Locale.getDefault());
        String currTime = sdf.format(new Date());
        String amPmString = "";

        if (showAmPm) {
            SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.getDefault());
            amPmString = amPmFormat.format(new Date());
        }
        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePixels);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.LEFT);
        if (shadow) {
            textPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
        }
        if (letterSpacing != -1) {
            textPaint.setLetterSpacing(letterSpacing);
        }

        final TextPaint smallTextPaint = new TextPaint();
        smallTextPaint.setTypeface(typeface);
        smallTextPaint.setTextSize(textSizePixels / 3);
        smallTextPaint.setAntiAlias(true);
        smallTextPaint.setSubpixelText(true);
        smallTextPaint.setColor(textColor);
        smallTextPaint.setTextAlign(Paint.Align.LEFT);
        if (shadow) {
            smallTextPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
        }
        if (letterSpacing != -1) {
            smallTextPaint.setLetterSpacing(letterSpacing);
        }

        int textHeight = (int) (textPaint.descent() - textPaint.ascent());
        int textOffset = (int) ((textHeight / 2) - textPaint.descent());
        float timeStringSize = textPaint.measureText(currTime);
        float timeStringSizeTotal = timeStringSize;
        float amPmStringSize = smallTextPaint.measureText(amPmString);
        int totalWidth = (int) (timeStringSizeTotal + amPmStringSize);
        int startOffset = (int) (timeStringSizeTotal - timeStringSize);
        Bitmap myBitmap = Bitmap.createBitmap(totalWidth, (int) textSizePixels, Bitmap.Config.ARGB_8888);
        Canvas myCanvas = new Canvas(myBitmap);
        myCanvas.drawText(currTime, startOffset, myBitmap.getHeight() / 2 + textOffset, textPaint);

        if (showAmPm) {
            myCanvas.drawText(amPmString, startOffset + timeStringSize, myBitmap.getHeight() / 2 + textOffset, smallTextPaint);
        }
        return myBitmap;
    }

    public static Typeface getClockFont(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String font = prefs.getString(CustomAppWidgetConfigure.KEY_CLOCK_FONT + "_" + id, null);
        if (font != null) {
            try {
                return Typeface.createFromFile(font);
            } catch (Exception e) {
            }
        }
        return Typeface.create("sans-serif-light", Typeface.NORMAL);
    }

    public static int getClockColor(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(CustomAppWidgetConfigure.KEY_CLOCK_COLOR + "_" + id, Color.WHITE);
    }

    public static int getAnalogBgColor(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(CustomAnalogAppWidgetConfigure.KEY_BG_COLOR + "_" + id, context.getResources().getColor(R.color.analog_clock_bg_color));
    }

    public static int getAnalogBorderColor(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(CustomAnalogAppWidgetConfigure.KEY_BORDER_COLOR + "_" + id, context.getResources().getColor(R.color.primary));
    }
    public static int getAnalogHourColor(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(CustomAnalogAppWidgetConfigure.KEY_HOUR_COLOR + "_" + id, context.getResources().getColor(R.color.analog_clock_hour_hand_color));
    }
    public static int getAnalogMinuteColor(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(CustomAnalogAppWidgetConfigure.KEY_MINUTE_COLOR + "_" + id, context.getResources().getColor(R.color.analog_clock_minute_hand_color));
    }
    public static int getAnalogTextColor(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(CustomAnalogAppWidgetConfigure.KEY_TEXT_COLOR + "_" + id, context.getResources().getColor(R.color.analog_clock_text_color));
    }
    public static int getAnalogAccentColor(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(CustomAnalogAppWidgetConfigure.KEY_ACCENT_COLOR + "_" + id, context.getResources().getColor(R.color.accent));
    }

    public static Bitmap createAnalogClockBitmap(Context context, boolean showAlarm, boolean showDate, boolean showNumbers, boolean showTicks,
                                                 boolean show24hours, int bgColor, int borderColor, int hourColor, int minuteColor, int textColor,
                                                 int accentColor) {
        Resources r = context.getResources();

        Calendar calendar = new GregorianCalendar();
        float hours = calendar.get(Calendar.HOUR_OF_DAY);
        float minutes = calendar.get(Calendar.MINUTE);
        hours = hours + minutes / 60.0f;

        Paint circlePaint = new Paint();
        circlePaint.setAntiAlias(true);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setColor(borderColor);

        Paint remaingCirclePaint = new Paint();
        remaingCirclePaint.setAntiAlias(true);
        remaingCirclePaint.setStyle(Paint.Style.STROKE);
        remaingCirclePaint.setColor(accentColor);

        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(bgColor);

        Paint hourPaint = new Paint();
        hourPaint.setAntiAlias(true);
        hourPaint.setStrokeCap(Paint.Cap.ROUND);
        hourPaint.setStyle(Paint.Style.STROKE);
        hourPaint.setColor(hourColor);

        Paint minutePaint = new Paint();
        minutePaint.setAntiAlias(true);
        minutePaint.setStrokeCap(Paint.Cap.ROUND);
        minutePaint.setStyle(Paint.Style.STROKE);
        minutePaint.setColor(minuteColor);

        Paint centerDotPaint = new Paint();
        centerDotPaint.setAntiAlias(true);
        centerDotPaint.setStyle(Paint.Style.FILL);
        centerDotPaint.setColor(accentColor);

        Paint tickPaint = new Paint();
        tickPaint.setAntiAlias(true);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setColor(textColor);

        Typeface typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL);

        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);

        final TextPaint textPaintSmall = new TextPaint();
        textPaintSmall.setTypeface(typeface);
        textPaintSmall.setAntiAlias(true);
        textPaintSmall.setSubpixelText(true);
        textPaintSmall.setColor(textColor);
        textPaintSmall.setTextAlign(Paint.Align.CENTER);

        float textSizePixels = r.getDimension(R.dimen.analog_widget_font_size);
        textPaint.setTextSize(textSizePixels);
        textPaintSmall.setTextSize(textSizePixels / 2f);

        final int circleStrokeWidth = r.getDimensionPixelSize(R.dimen.widget_clock_circle_size);
        final int handEndLength = 0;
        final int width = r.getDimensionPixelSize(R.dimen.custom_analog_widget_size);
        float textInset = textSizePixels;
        float textInsetTop = 1.5f * textSizePixels;

        circlePaint.setStrokeWidth(circleStrokeWidth);
        remaingCirclePaint.setStrokeWidth(circleStrokeWidth);
        hourPaint.setStrokeWidth(r.getDimensionPixelSize(R.dimen.widget_clock_hour_hand_width));
        minutePaint.setStrokeWidth(r.getDimensionPixelSize(R.dimen.widget_clock_minute_hand_width));
        tickPaint.setStrokeWidth(r.getDimensionPixelSize(R.dimen.widget_clock_tick_width));

        Bitmap myBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(myBitmap);

        int availableWidth = width;
        int availableHeight = width;

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        float radius = availableHeight / 2 - circleStrokeWidth;
        RectF arcRect = new RectF();
        arcRect.top = y - radius;
        arcRect.bottom = y + radius;
        arcRect.left = x - radius;
        arcRect.right = x + radius;

        int tickLength = (int) (radius / 12.0);
        int numberInset = 0;

        if (showTicks) {
            numberInset = (int) (radius / 5.0);
        } else {
            numberInset = (int) (radius / 6.0);
        }

        if (showNumbers) {
            textInset += numberInset;
            textInsetTop += numberInset;
        }

        canvas.drawArc(arcRect, 0, 360, true, bgPaint);

        int i = 1;
        for (int angle = 0; angle < 360; angle += show24hours ? 15 : 30) {
            if (showTicks) {
                drawHourTick(canvas, radius, x, y, angle, tickLength, tickPaint);
            }
            if (showNumbers) {
                drawNumeral(canvas, availableWidth, availableHeight, radius, i, textPaint, textPaintSmall, numberInset, show24hours);
            }
            i++;
        }

        canvas.drawArc(arcRect, 0, 360, false, circlePaint);
        float minuteStartAngle = minutes / 60.0f * 360.0f;
        if (minuteStartAngle < 90) {
            canvas.drawArc(arcRect, 270f + minuteStartAngle, 90f - minuteStartAngle, false, remaingCirclePaint);
            canvas.drawArc(arcRect, 0f, 270f, false, remaingCirclePaint);
        } else {
            canvas.drawArc(arcRect, minuteStartAngle - 90f, 360f - minuteStartAngle, false, remaingCirclePaint);
        }

        if (showDate) {
            CharSequence dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                    context.getString(R.string.abbrev_wday_month_day_no_year));
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat.toString(), Locale.getDefault());
            String currDate = sdf.format(new Date());

            Path path = new Path();
            RectF arcRectText = new RectF(arcRect);
            arcRectText.inset(textInsetTop, textInsetTop);
            path.addArc(arcRectText, 180f, 180f);
            canvas.drawTextOnPath(currDate, path, 0, 0, textPaint);
        }
        if (showAlarm) {
            long nextAlamMilis = Utils.getNextAlarmInMillis(context);
            if (nextAlamMilis != -1) {
                calendar.setTimeInMillis(nextAlamMilis);
                String alarmTimeString = AlarmUtils.getFormattedTime(context, calendar);
                Path path = new Path();
                RectF arcRectText = new RectF(arcRect);
                arcRectText.inset(textInset, textInset);
                path.addArc(arcRectText, 180f, -180f);
                canvas.drawTextOnPath(alarmTimeString, path, 0, 0, textPaint);
            }
        }
        drawHand(canvas, hourPaint, x, y, radius * 0.70f, hours / (show24hours ? 24.0f : 12.0f) * 360.0f - 90, handEndLength);
        drawHand(canvas, minutePaint, x, y, radius, minutes / 60.0f * 360.0f - 90, handEndLength);
        canvas.drawCircle(x, y, minutePaint.getStrokeWidth(), centerDotPaint);

        return myBitmap;
    }

    private static void drawHand(Canvas canvas, Paint mHandPaint, int x, int y, float length, float angle, int mHandEndLength) {
        canvas.save();
        canvas.rotate(angle, x, y);
        canvas.drawLine(x, y, x + length, y, mHandPaint);
        canvas.drawLine(x, y, x - mHandEndLength, y, mHandPaint);
        canvas.restore();
    }

    private static void drawHourTick(Canvas canvas, float radius, int x, int y, float angle, int tickLength, Paint tickPaint) {
        canvas.save();
        canvas.rotate(angle, x, y);
        canvas.drawLine(x + radius - tickLength, y, x + radius, y, tickPaint);
        canvas.restore();
    }

    private static void drawNumeral(Canvas canvas, int width, int height, float radius, int number, Paint textPaint, Paint textPaintSmall, int numberInset, boolean show24hours) {

        String tmp = String.valueOf(number == 24 ? 0 : number);
        Rect rect = new Rect();
        textPaint.getTextBounds(tmp, 0, tmp.length(), rect);
        double angle = show24hours ? (Math.PI / 12 * (number - 6)) : (Math.PI / 6 * (number - 3));
        int x = (int) (width / 2 + Math.cos(angle) * (radius - numberInset));
        int y = (int) (height / 2 + Math.sin(angle) * (radius - numberInset) + rect.height() / 2);
        canvas.drawText(tmp, x, y, show24hours ? (number % 2 == 0 ? textPaint : textPaintSmall) : textPaint);
    }

    private static void drawTextOnCanvas(Canvas canvas, float xPos, float yPos, final String text,
                                  final Typeface typeface, final float textSizePixels, final int textColor,
                                  boolean shadow, float letterSpacing) {
        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePixels);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setColor(textColor);
        textPaint.setTextAlign(Paint.Align.CENTER);

        if (shadow) {
            textPaint.setShadowLayer(5.0f, 0.0f, 0.0f, Color.BLACK);
        }
        if (letterSpacing != -1) {
            textPaint.setLetterSpacing(letterSpacing);
        }

        int textHeight = (int) (textPaint.descent() - textPaint.ascent());
        int textOffset = (int) ((textHeight / 2) - textPaint.descent());
        canvas.drawText(text, xPos, yPos + textOffset, textPaint);
    }

    public static Bitmap createDataAlarmBitmap(final Context context, final Typeface typeface,
                                               final float textSizePixels, final int textColor,
                                               boolean shadow, float letterSpacing, boolean showDate,
                                               boolean showAlarm) {

        String nextAlarm = Utils.getNextAlarm(context);
        boolean hasAlarm = !TextUtils.isEmpty(nextAlarm);
        if (hasAlarm) {
            nextAlarm = nextAlarm.toUpperCase();
        }

        CharSequence dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                context.getString((showAlarm && hasAlarm) ? R.string.abbrev_wday_month_day_no_year :
                        R.string.full_wday_month_day_no_year));

        String currDate = "";

        if (showDate) {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat.toString(), Locale.getDefault());
            currDate = sdf.format(new Date()).toUpperCase();
        }

        final TextPaint textPaint = new TextPaint();
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePixels);
        if (letterSpacing != -1) {
            textPaint.setLetterSpacing(letterSpacing);
        }
        Drawable d = context.getDrawable(R.drawable.ic_alarm);

        float separatorWidth = textPaint.measureText(" ");

        float dateWidth = showDate ? textPaint.measureText(currDate) + 2 * separatorWidth  : 0;
        float alarmWidth = (showAlarm && hasAlarm) ? textPaint.measureText(nextAlarm) + 3 * separatorWidth + d.getIntrinsicWidth() : 0;
        float totalWidth = dateWidth + alarmWidth;
        float totalHeight = Math.max(d.getIntrinsicHeight(), textSizePixels);

        if (totalWidth != 0 && totalHeight != 0) {
            Bitmap myBitmap = Bitmap.createBitmap((int) totalWidth, (int) totalHeight, Bitmap.Config.ARGB_8888);
            Canvas myCanvas = new Canvas(myBitmap);
            if (showDate) {
                drawTextOnCanvas(myCanvas, dateWidth / 2, myBitmap.getHeight() / 2, currDate, typeface, textSizePixels, textColor, shadow, letterSpacing);
            }

            if (hasAlarm && showAlarm) {
                ColorFilter filter = new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_IN);
                d.mutate().setColorFilter(filter);
                d.setBounds((int) (dateWidth + separatorWidth), 0,
                        (int) (dateWidth + separatorWidth + d.getIntrinsicWidth() - 10), d.getIntrinsicHeight() - 10);
                d.draw(myCanvas);
                drawTextOnCanvas(myCanvas, dateWidth + 2 * separatorWidth + alarmWidth / 2,
                        myBitmap.getHeight() / 2, nextAlarm, typeface, textSizePixels, textColor, shadow,
                        letterSpacing);
            }
            return myBitmap;
        }
        return null;
    }
}
