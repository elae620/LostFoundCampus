package com.example.lostfoundcampus.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.model.CampusLocation;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

/** Position actuelle via FusedLocationProviderClient. La permission est demandée par l'écran appelant. */
public final class LocationHelper {

    public interface Callback {
        void onLocation(@NonNull CampusLocation location);

        void onUnavailable(@StringRes int reason);
    }

    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private LocationHelper() {
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public static void current(Context context, Callback cb) {
        if (!hasPermission(context)) {
            cb.onUnavailable(R.string.location_permission_denied);
            return;
        }
        CancellationTokenSource cts = new CancellationTokenSource();
        LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) cb.onUnavailable(R.string.location_unavailable);
                    else cb.onLocation(new CampusLocation(location.getLatitude(), location.getLongitude(), null));
                })
                .addOnFailureListener(e -> cb.onUnavailable(R.string.location_unavailable));
    }
}
