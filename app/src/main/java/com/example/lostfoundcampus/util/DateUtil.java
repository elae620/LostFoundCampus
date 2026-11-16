package com.example.lostfoundcampus.util;

import android.content.Context;

import com.example.lostfoundcampus.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Formats de date en français. Les motifs ne sont pas des textes visibles. */
public final class DateUtil {

    private static final Locale FR = Locale.FRANCE;

    private DateUtil() {
    }

    /** Nombre de jours calendaires entre la date et aujourd'hui (0 = aujourd'hui). */
    public static int daysAgo(long time) {
        Calendar then = startOfDay(time);
        Calendar today = startOfDay(System.currentTimeMillis());
        long diff = today.getTimeInMillis() - then.getTimeInMillis();
        return (int) Math.round(diff / (24.0 * 60 * 60 * 1000));
    }

    /** « Aujourd'hui », « Hier », « Il y a 3 j », puis « 9 nov. ». */
    public static String relativeShort(Context c, long time) {
        int d = daysAgo(time);
        if (d <= 0) return c.getString(R.string.date_today);
        if (d == 1) return c.getString(R.string.date_yesterday);
        if (d < 7) return c.getString(R.string.date_days_ago, d);
        return new SimpleDateFormat("d MMM", FR).format(new Date(time));
    }

    /** « Publiée il y a 2 jours ». */
    public static String published(Context c, long time) {
        int d = daysAgo(time);
        if (d <= 0) return c.getString(R.string.published_today);
        if (d == 1) return c.getString(R.string.published_yesterday);
        return c.getString(R.string.published_days_ago, d);
    }

    /** « 9 novembre 2026 ». */
    public static String longDate(long time) {
        return new SimpleDateFormat("d MMMM yyyy", FR).format(new Date(time));
    }

    /** « 9 nov. 2026 ». */
    public static String shortDate(long time) {
        return new SimpleDateFormat("d MMM yyyy", FR).format(new Date(time));
    }

    /** « 10:42 ». */
    public static String time(long time) {
        return new SimpleDateFormat("HH:mm", FR).format(new Date(time));
    }

    /** Heure du jour, « Hier », ou jour abrégé (« Lun. »). */
    public static String conversationTime(Context c, long time) {
        int d = daysAgo(time);
        if (d <= 0) return time(time);
        if (d == 1) return c.getString(R.string.time_yesterday);
        if (d < 7) return capitalize(new SimpleDateFormat("EEE", FR).format(new Date(time)));
        return new SimpleDateFormat("d MMM", FR).format(new Date(time));
    }

    private static Calendar startOfDay(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(FR) + s.substring(1);
    }
}
