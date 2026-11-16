package com.example.lostfoundcampus.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.data.model.Announcement;
import com.example.lostfoundcampus.data.model.AnnouncementType;
import com.example.lostfoundcampus.data.model.Category;
import com.example.lostfoundcampus.data.model.User;
import com.example.lostfoundcampus.databinding.FragmentHomeBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.ui.common.UiUtils;
import com.example.lostfoundcampus.ui.detail.AnnouncementDetailFragment;
import com.example.lostfoundcampus.ui.publish.PublishFragment;
import com.example.lostfoundcampus.util.InsetsUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class HomeFragment extends Fragment {

    private static final String STATE_TAB = "tab";
    private static final String STATE_CATEGORY = "category";

    private FragmentHomeBinding b;
    private AnnouncementAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int selectedTab = 0;
    @Nullable
    private Category selectedCategory;
    /** Le premier affichage simule un chargement réseau ; les retours sur l'écran sont immédiats. */
    private boolean loadedOnce;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(STATE_TAB);
            int cat = savedInstanceState.getInt(STATE_CATEGORY, -1);
            selectedCategory = cat >= 0 ? Category.values()[cat] : null;
            loadedOnce = true;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentHomeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyTop(b.appBar);

        User me = FakeRepository.get().currentUser();
        b.txtGreeting.setText(getString(R.string.home_greeting, me.firstName()));
        UiUtils.bindAvatar(b.txtAvatar, b.imgAvatar, me);
        b.avatarButton.setOnClickListener(v -> Nav.main(this).selectTab(R.id.profileFragment));

        adapter = new AnnouncementAdapter(this::openDetail);
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.list.setAdapter(adapter);

        TabLayout.Tab tab = b.tabsType.getTabAt(selectedTab);
        if (tab != null) tab.select();
        b.tabsType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                selectedTab = t.getPosition();
                refresh();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
            }
        });

        buildCategoryChips();

        b.etSearch.addTextChangedListener(new Nav.AfterTextChanged(s -> refresh()));
        b.etSearch.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        b.fabPublish.setOnClickListener(v -> openPublish());
        b.empty.btnEmptyAction.setText(R.string.home_empty_action);
        b.empty.btnEmptyAction.setOnClickListener(v -> openPublish());

        if (!loadedOnce) {
            // ÉTAPE 1 : chargement simulé. Étape 2 : requête Firestore paginée.
            b.progress.setVisibility(View.VISIBLE);
            b.list.setVisibility(View.GONE);
            handler.postDelayed(() -> {
                if (b == null) return;
                loadedOnce = true;
                b.progress.setVisibility(View.GONE);
                refresh();
            }, getResources().getInteger(R.integer.lf_fake_network_delay_ms));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (loadedOnce) refresh();
    }

    private void buildCategoryChips() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        b.chipsCategory.removeAllViews();
        for (Category category : Category.values()) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_category_chip, b.chipsCategory, false);
            chip.setId(View.generateViewId());
            chip.setText(category.labelRes);
            chip.setTag(category);
            chip.setChecked(category == selectedCategory);
            b.chipsCategory.addView(chip);
        }
        b.chipsCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedCategory = null;
            if (!checkedIds.isEmpty()) {
                View checked = group.findViewById(checkedIds.get(0));
                if (checked != null) selectedCategory = (Category) checked.getTag();
            }
            refresh();
        });
    }

    private void refresh() {
        if (b == null || !loadedOnce) return;
        AnnouncementType type = selectedTab == 1 ? AnnouncementType.LOST
                : selectedTab == 2 ? AnnouncementType.FOUND : null;
        List<Announcement> data = FakeRepository.get().feed(type, selectedCategory, Nav.text(b.etSearch));
        adapter.submit(data);
        boolean empty = data.isEmpty();
        b.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            b.empty.imgEmpty.setImageResource(R.drawable.ic_search_off);
            b.empty.txtEmptyTitle.setText(R.string.home_empty_title);
            b.empty.txtEmptyBody.setText(R.string.home_empty_body);
            b.empty.btnEmptyAction.setVisibility(View.VISIBLE);
        }
    }

    private void openDetail(Announcement a) {
        Nav.go(this, R.id.announcementDetailFragment, AnnouncementDetailFragment.args(a.id));
    }

    private void openPublish() {
        Nav.go(this, R.id.publishFragment, PublishFragment.argsNew(AnnouncementType.LOST));
    }

    private void hideKeyboard() {
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(b.etSearch.getWindowToken(), 0);
        b.etSearch.clearFocus();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, selectedTab);
        outState.putInt(STATE_CATEGORY, selectedCategory == null ? -1 : selectedCategory.ordinal());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        b = null;
    }
}
