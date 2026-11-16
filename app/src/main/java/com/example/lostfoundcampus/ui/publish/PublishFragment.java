package com.example.lostfoundcampus.ui.publish;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavOptions;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.data.model.Announcement;
import com.example.lostfoundcampus.data.model.AnnouncementType;
import com.example.lostfoundcampus.data.model.CampusLocation;
import com.example.lostfoundcampus.data.model.Category;
import com.example.lostfoundcampus.data.model.ColorSwatch;
import com.example.lostfoundcampus.data.model.ImageAnalysis;
import com.example.lostfoundcampus.databinding.FragmentPublishBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.ui.detail.AnnouncementDetailFragment;
import com.example.lostfoundcampus.ui.map.LocationPickerFragment;
import com.example.lostfoundcampus.ui.map.MiniMapController;
import com.example.lostfoundcampus.util.DateUtil;
import com.example.lostfoundcampus.util.InsetsUtil;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.TimeZone;

/** Publier une annonce : onglets « J'ai perdu » (galerie) et « J'ai trouvé » (caméra), mode édition. */
public class PublishFragment extends Fragment {

    private static final String ARG_TYPE = "type";
    private static final String ARG_EDIT_ID = "edit_id";
    private static final String TAG_DATE_PICKER = "date_picker";

    private FragmentPublishBinding b;
    private PublishViewModel vm;
    private PhotoThumbAdapter thumbs;
    private MiniMapController miniMap;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean binding;

    public static Bundle argsNew(AnnouncementType type) {
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type.name());
        return args;
    }

    public static Bundle argsEdit(String announcementId) {
        Bundle args = new Bundle();
        args.putString(ARG_EDIT_ID, announcementId);
        return args;
    }

    private int maxPhotos() {
        return getResources().getInteger(R.integer.lf_max_photos);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(PublishViewModel.class);
        if (savedInstanceState == null) {
            Bundle args = getArguments();
            Announcement toEdit = args == null ? null
                    : FakeRepository.get().announcement(args.getString(ARG_EDIT_ID));
            if (toEdit != null) {
                vm.loadForEdit(toEdit);
            } else {
                String type = args == null ? null : args.getString(ARG_TYPE);
                vm.reset(type == null ? AnnouncementType.LOST : AnnouncementType.valueOf(type));
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentPublishBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyTop(b.scroll);
        InsetsUtil.applyBottom(b.bottomBar, true);

        b.header.txtTitle.setText(vm.isEditing() ? R.string.publish_edit_title : R.string.publish_title);
        b.header.btnBack.setOnClickListener(v -> Nav.back(this));
        b.tabsType.setVisibility(vm.isEditing() ? View.GONE : View.VISIBLE);

        setUpTabs();
        setUpPhotos();
        setUpFields();
        miniMap = new MiniMapController(b.miniMap, this::openLocationPicker);
        b.btnAnalyze.setOnClickListener(v -> Nav.go(this, R.id.aiAnalysisFragment, null));
        b.btnPublish.setText(vm.isEditing() ? R.string.publish_save : R.string.publish_submit);
        b.btnPublish.setOnClickListener(v -> submit());

        observeLocationResult();
        render();
    }

    // ------------------------------------------------------------------ Onglets

    private void setUpTabs() {
        TabLayout.Tab tab = b.tabsType.getTabAt(vm.type == AnnouncementType.FOUND ? 1 : 0);
        if (tab != null) tab.select();
        b.tabsType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                vm.setType(t.getPosition() == 1 ? AnnouncementType.FOUND : AnnouncementType.LOST);
                b.txtPhotoError.setVisibility(View.GONE);
                render();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
            }
        });
    }

    // ------------------------------------------------------------------ Photos

    private void setUpPhotos() {
        thumbs = new PhotoThumbAdapter(new PhotoThumbAdapter.Listener() {
            @Override
            public void onRemove(int index) {
                vm.removePhoto(index);
                renderPhotos();
            }

            @Override
            public void onAdd() {
                addPhotos();
            }
        });
        b.rvPhotos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        b.rvPhotos.setAdapter(thumbs);
        b.btnAddPhotos.setOnClickListener(v -> addPhotos());
    }

    private void addPhotos() {
        int remaining = vm.remainingSlots(maxPhotos());
        if (remaining == 0) return;
        b.txtPhotoError.setVisibility(View.GONE);
        if (vm.type == AnnouncementType.FOUND) {
            // « J'ai trouvé » : caméra uniquement. ÉTAPE 1 : prise de vue simulée.
            vm.pendingPhoto = SimulatedMedia.capturePhoto();
            Nav.go(this, R.id.photoPreviewFragment, null);
        } else {
            // « J'ai perdu » : galerie, sélection multiple. ÉTAPE 1 : sélection simulée.
            vm.addPhotos(SimulatedMedia.pickFromGallery(remaining), maxPhotos());
            renderPhotos();
        }
    }

    private void renderPhotos() {
        boolean found = vm.type == AnnouncementType.FOUND;
        int count = vm.photos.size();
        int max = maxPhotos();
        boolean full = count >= max;

        thumbs.submit(vm.photos, count > 0 && !full, found);
        b.rvPhotos.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        b.txtPhotoCount.setText(getString(R.string.publish_photos_count, count, max));
        if (full) {
            b.txtPhotosHint.setText(R.string.publish_photos_max);
        } else {
            b.txtPhotosHint.setText(found ? R.string.publish_photos_empty_found : R.string.publish_photos_empty_lost);
        }
        b.btnAddPhotos.setText(found ? R.string.publish_take_photo : R.string.publish_pick_gallery);
        b.btnAddPhotos.setIconResource(found ? R.drawable.ic_camera : R.drawable.ic_gallery);
        b.btnAddPhotos.setVisibility(count == 0 ? View.VISIBLE : View.GONE);

        b.groupAnalyze.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        boolean analysed = vm.analysis != null;
        b.txtAnalysisStatus.setText(analysed ? R.string.publish_analysis_done : R.string.publish_analysis_none);
        b.btnAnalyze.setVisibility(analysed ? View.GONE : View.VISIBLE);
    }

    // ------------------------------------------------------------------ Analyse

    private void renderAnalysis() {
        ImageAnalysis a = vm.analysis;
        b.cardAnalysis.setVisibility(a == null ? View.GONE : View.VISIBLE);
        if (a == null) return;
        b.rowLabel.txtRowLabel.setText(R.string.analysis_label);
        b.rowLabel.txtRowValue.setText(getString(R.string.analysis_label_value, a.label, a.confidence));
        b.rowText.txtRowLabel.setText(R.string.analysis_text);
        b.rowText.txtRowValue.setText(a.hasText() ? a.detectedText : getString(R.string.analysis_text_none));
        b.rowColor.txtRowLabel.setText(R.string.analysis_color);
        b.rowColor.txtRowValue.setText(a.dominantColor.labelRes);
        b.rowColor.swatch.setVisibility(View.VISIBLE);
        ViewCompat.setBackgroundTintList(b.rowColor.swatch, ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), a.dominantColor.colorRes)));
    }

    // ------------------------------------------------------------------ Champs

    private void setUpFields() {
        String[] categories = new String[Category.values().length];
        for (int i = 0; i < categories.length; i++) categories[i] = getString(Category.values()[i].labelRes);
        b.actCategory.setSimpleItems(categories);
        b.actCategory.setOnItemClickListener((parent, v, position, id) -> {
            vm.category = Category.values()[position];
            b.tilCategory.setError(null);
            renderAiHelpers();
        });

        String[] colors = new String[ColorSwatch.values().length];
        for (int i = 0; i < colors.length; i++) colors[i] = getString(ColorSwatch.values()[i].labelRes);
        b.actColor.setSimpleItems(colors);
        b.actColor.setOnItemClickListener((parent, v, position, id) -> {
            vm.color = ColorSwatch.values()[position];
            renderAiHelpers();
        });

        b.etTitle.addTextChangedListener(new Nav.AfterTextChanged(s -> {
            if (binding) return;
            vm.title = s.toString();
            b.tilTitle.setError(null);
            renderAiHelpers();
        }));
        b.etDescription.addTextChangedListener(new Nav.AfterTextChanged(s -> {
            if (!binding) vm.description = s.toString();
        }));

        b.etDate.setOnClickListener(v -> openDatePicker());
        b.tilDate.setEndIconOnClickListener(v -> openDatePicker());
    }

    /** « Suggestion IA : à vérifier » tant que la valeur proposée n'a pas été modifiée. */
    private void renderAiHelpers() {
        String hint = getString(R.string.ai_suggestion_chip);
        b.tilTitle.setHelperText(vm.titleIsAiSuggestion() ? hint : null);
        b.tilCategory.setHelperText(vm.categoryIsAiSuggestion() ? hint : null);
        b.tilColor.setHelperText(vm.colorIsAiSuggestion() ? hint : null);
    }

    private void renderFields() {
        binding = true;
        if (!Nav.text(b.etTitle).equals(vm.title.trim())) b.etTitle.setText(vm.title);
        if (!Nav.text(b.etDescription).equals(vm.description.trim())) b.etDescription.setText(vm.description);
        b.actCategory.setText(vm.category == null ? "" : getString(vm.category.labelRes), false);
        b.actColor.setText(vm.color == null ? "" : getString(vm.color.labelRes), false);
        b.txtDateLabel.setText(vm.type == AnnouncementType.FOUND ? R.string.label_date_found : R.string.label_date_lost);
        b.etDate.setText(vm.date == null ? "" : DateUtil.longDate(vm.date));
        binding = false;
        renderAiHelpers();
    }

    private void openDatePicker() {
        if (getChildFragmentManager().findFragmentByTag(TAG_DATE_PICKER) != null) return;
        long selection = vm.date != null ? toUtcMidnight(vm.date) : MaterialDatePicker.todayInUtcMilliseconds();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(vm.type == AnnouncementType.FOUND
                        ? R.string.date_picker_title_found : R.string.date_picker_title_lost)
                .setSelection(selection)
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now()).build())
                .build();
        picker.addOnPositiveButtonClickListener(utc -> {
            vm.date = fromUtcMidnight(utc);
            b.tilDate.setError(null);
            renderFields();
        });
        picker.show(getChildFragmentManager(), TAG_DATE_PICKER);
    }

    /** Le sélecteur Material travaille en minuit UTC ; on convertit vers le jour local. */
    private static long fromUtcMidnight(long utc) {
        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.setTimeInMillis(utc);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH), 12, 0);
        return local.getTimeInMillis();
    }

    private static long toUtcMidnight(long local) {
        Calendar l = Calendar.getInstance();
        l.setTimeInMillis(local);
        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.clear();
        u.set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH));
        return u.getTimeInMillis();
    }

    // ------------------------------------------------------------------ Lieu

    private void openLocationPicker() {
        Nav.go(this, R.id.locationPickerFragment, LocationPickerFragment.args(vm.location));
    }

    private void observeLocationResult() {
        NavBackStackEntry entry = Nav.of(this).getCurrentBackStackEntry();
        if (entry == null) return;
        entry.getSavedStateHandle().<CampusLocation>getLiveData(LocationPickerFragment.RESULT_KEY)
                .observe(getViewLifecycleOwner(), picked -> {
                    if (picked == null) return;
                    vm.setLocation(picked);
                    entry.getSavedStateHandle().remove(LocationPickerFragment.RESULT_KEY);
                    b.txtLocationError.setVisibility(View.GONE);
                    renderLocation();
                });
    }

    private void renderLocation() {
        String override = vm.locationCaptured && vm.location != null && !vm.location.hasPrecision()
                ? getString(R.string.map_captured_position) : null;
        miniMap.show(vm.location, FakeRepository.get().campusCenter(), override);
    }

    // ------------------------------------------------------------------ Rendu global

    private void render() {
        renderPhotos();
        renderAnalysis();
        renderFields();
        renderLocation();
    }

    // ------------------------------------------------------------------ Publication

    private void submit() {
        vm.title = Nav.text(b.etTitle);
        vm.description = Nav.text(b.etDescription);
        View firstError = null;

        if (vm.photos.isEmpty()) {
            b.txtPhotoError.setText(R.string.error_photo_required);
            b.txtPhotoError.setVisibility(View.VISIBLE);
            firstError = b.cardPhotos;
        }
        if (vm.title.isEmpty()) {
            b.tilTitle.setError(getString(R.string.error_item_name));
            if (firstError == null) firstError = b.tilTitle;
        }
        if (vm.category == null) {
            b.tilCategory.setError(getString(R.string.error_category));
            if (firstError == null) firstError = b.tilCategory;
        }
        if (vm.location == null) {
            b.txtLocationError.setVisibility(View.VISIBLE);
            if (firstError == null) firstError = b.miniMap.getRoot();
        }
        if (vm.date == null) {
            b.tilDate.setError(getString(R.string.error_date));
            if (firstError == null) firstError = b.tilDate;
        }
        if (firstError != null) {
            final View target = firstError;
            b.scroll.post(() -> b.scroll.smoothScrollTo(0, Math.max(0, target.getTop() - b.content.getPaddingTop())));
            Snackbar.make(b.getRoot(), R.string.error_form_summary, Snackbar.LENGTH_SHORT)
                    .setAnchorView(b.bottomBar).show();
            return;
        }

        b.btnPublish.setEnabled(false);
        b.btnPublish.setText(R.string.publish_loading);
        final boolean editing = vm.isEditing();
        // ÉTAPE 1 : enregistrement local. Étape 2 : compression, envoi Storage, document Firestore.
        handler.postDelayed(() -> {
            if (b == null) return;
            String id = vm.save(FakeRepository.get());
            NavOptions options = new NavOptions.Builder().setPopUpTo(R.id.publishFragment, true).build();
            Nav.of(this).navigate(R.id.announcementDetailFragment, AnnouncementDetailFragment.args(id), options);
            Nav.main(this).showMessage(editing ? R.string.publish_updated : R.string.publish_success);
        }, getResources().getInteger(R.integer.lf_fake_network_delay_ms));
    }

    @Override
    public void onResume() {
        super.onResume();
        miniMap.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        miniMap.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        miniMap.onDestroy();
        b = null;
    }
}
