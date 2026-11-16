package com.example.lostfoundcampus.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.data.model.CampusLocation;
import com.example.lostfoundcampus.databinding.FragmentLocationPickerBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.util.InsetsUtil;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

/**
 * Sélection d'un point réel sur la carte : toucher la carte ou faire glisser le marqueur.
 * Le résultat (CampusLocation) est renvoyé à l'écran précédent via son SavedStateHandle.
 */
public class LocationPickerFragment extends Fragment {

    public static final String ARG_INITIAL = "initial_location";
    public static final String RESULT_KEY = "picked_location";

    private FragmentLocationPickerBinding b;
    @Nullable
    private MapView map;
    @Nullable
    private Marker marker;
    private GeoPoint current;

    public static Bundle args(@Nullable CampusLocation initial) {
        Bundle args = new Bundle();
        if (initial != null) args.putSerializable(ARG_INITIAL, initial);
        return args;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentLocationPickerBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyTop(b.topBar);
        InsetsUtil.applyBottom(b.sheet, true);

        CampusLocation initial = null;
        if (getArguments() != null) {
            initial = (CampusLocation) getArguments().getSerializable(ARG_INITIAL);
        }
        CampusLocation start = initial != null ? initial : FakeRepository.get().campusCenter();
        current = MapHelper.toGeoPoint(start);
        if (savedInstanceState == null && initial != null && initial.hasPrecision()) {
            b.etPrecision.setText(initial.precision);
        }

        map = MapHelper.createMap(b.mapContainer, true);
        if (map == null) {
            b.fallback.setVisibility(View.VISIBLE);
            b.btnMyLocation.setVisibility(View.GONE);
            b.txtInstructions.setVisibility(View.INVISIBLE);
            b.txtAttribution.setVisibility(View.GONE);
        } else {
            MapHelper.center(map, start, getResources().getInteger(R.integer.lf_map_zoom_picker));
            // La couche d'événements passe sous le marqueur pour qu'il reste déplaçable.
            map.getOverlays().add(0, new MapEventsOverlay(new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    moveMarker(p);
                    return true;
                }

                @Override
                public boolean longPressHelper(GeoPoint p) {
                    moveMarker(p);
                    return true;
                }
            }));
            marker = MapHelper.addMarker(map, current, true);
            marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                @Override
                public void onMarkerDrag(Marker m) {
                }

                @Override
                public void onMarkerDragEnd(Marker m) {
                    current = m.getPosition();
                }

                @Override
                public void onMarkerDragStart(Marker m) {
                }
            });
        }

        b.btnBack.setOnClickListener(v -> Nav.back(this));
        b.btnMyLocation.setOnClickListener(v -> useMyPosition());
        b.btnConfirm.setOnClickListener(v -> confirm());
    }

    private void moveMarker(GeoPoint p) {
        current = p;
        if (marker != null && map != null) {
            marker.setPosition(p);
            map.invalidate();
        }
    }

    /** ÉTAPE 1 : position d'exemple fixe. Étape 2 : FusedLocationProviderClient + permission. */
    private void useMyPosition() {
        CampusLocation here = FakeRepository.get().campusCenter();
        moveMarker(MapHelper.toGeoPoint(here));
        if (map != null) map.getController().animateTo(current);
        Snackbar.make(b.getRoot(), R.string.picker_position_simulated, Snackbar.LENGTH_SHORT)
                .setAnchorView(b.sheet).show();
    }

    private void confirm() {
        String precision = Nav.text(b.etPrecision);
        CampusLocation result = new CampusLocation(current.getLatitude(), current.getLongitude(),
                precision.isEmpty() ? null : precision);
        NavController nav = Nav.of(this);
        NavBackStackEntry previous = nav.getPreviousBackStackEntry();
        if (previous != null) previous.getSavedStateHandle().set(RESULT_KEY, result);
        nav.popBackStack();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (map != null) map.onDetach();
        map = null;
        marker = null;
        b = null;
    }
}
