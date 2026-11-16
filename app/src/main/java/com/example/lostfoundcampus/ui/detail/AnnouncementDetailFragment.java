package com.example.lostfoundcampus.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavBackStackEntry;
import androidx.viewpager2.widget.ViewPager2;

import com.example.lostfoundcampus.BuildConfig;
import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.data.model.Announcement;
import com.example.lostfoundcampus.data.model.AnnouncementType;
import com.example.lostfoundcampus.data.model.CampusLocation;
import com.example.lostfoundcampus.data.model.Conversation;
import com.example.lostfoundcampus.data.model.User;
import com.example.lostfoundcampus.databinding.FragmentAnnouncementDetailBinding;
import com.example.lostfoundcampus.debug.DebugFlags;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.ui.common.UiUtils;
import com.example.lostfoundcampus.ui.map.LocationPickerFragment;
import com.example.lostfoundcampus.ui.map.MapHelper;
import com.example.lostfoundcampus.ui.map.MiniMapController;
import com.example.lostfoundcampus.ui.matches.MatchesFragment;
import com.example.lostfoundcampus.ui.messages.ChatFragment;
import com.example.lostfoundcampus.util.DateUtil;
import com.example.lostfoundcampus.util.InsetsUtil;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Détail d'une annonce. « Voir les correspondances » et la modification du lieu sont réservés
 * à l'auteur ; « Contacter l'utilisateur » est masqué pour lui (RG02).
 */
public class AnnouncementDetailFragment extends Fragment {

    private static final String ARG_ID = "announcement_id";

    private FragmentAnnouncementDetailBinding b;
    private final PhotoPagerAdapter pagerAdapter = new PhotoPagerAdapter();
    @Nullable
    private TabLayoutMediator dotsMediator;
    @Nullable
    private MiniMapController miniMap;
    @Nullable
    private Announcement announcement;
    private boolean resumed;
    /** Vrai après une pause : au retour, on relit l'annonce (elle a pu être modifiée). */
    private boolean needsRefresh;

    public static Bundle args(String announcementId) {
        Bundle args = new Bundle();
        args.putString(ARG_ID, announcementId);
        return args;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Chaque détail démarre avec le comportement réel (auteur ou non).
        if (savedInstanceState == null) DebugFlags.setForcedAuthorMode(null);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentAnnouncementDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyTopMargin(b.btnBack);
        InsetsUtil.applyBottom(b.bottomBar, false);
        b.btnBack.setOnClickListener(v -> Nav.back(this));

        announcement = FakeRepository.get().announcement(getArguments() == null ? null
                : getArguments().getString(ARG_ID));
        if (announcement == null) {
            Nav.main(this).showMessage(R.string.detail_not_found);
            Nav.of(this).popBackStack();
            return;
        }

        b.pager.setAdapter(pagerAdapter);
        b.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (b == null || announcement == null) return;
                b.pager.setContentDescription(getString(R.string.cd_gallery, position + 1,
                        announcement.photoRes.size()));
            }
        });

        bindDebugSwitch();
        observeLocationResult();
        bind();
    }

    private boolean isAuthor() {
        return announcement != null
                && DebugFlags.isAuthor(FakeRepository.get().isCurrentUser(announcement.authorId));
    }

    /** DEBUG (étape 1) : interrupteur « mode auteur ». À retirer avec DebugFlags. */
    private void bindDebugSwitch() {
        if (!BuildConfig.DEBUG) return;
        b.debugAuthorCard.setVisibility(View.VISIBLE);
        b.switchAuthor.setChecked(isAuthor());
        b.switchAuthor.setOnCheckedChangeListener((button, checked) -> {
            DebugFlags.setForcedAuthorMode(checked);
            bind();
        });
    }

    private void bind() {
        Announcement a = announcement;
        if (a == null || b == null) return;
        boolean author = isAuthor();

        // Galerie : indicateur de position seulement s'il y a plusieurs photos.
        pagerAdapter.submit(a.photoRes, a.title);
        if (dotsMediator != null) dotsMediator.detach();
        dotsMediator = null;
        boolean several = a.photoRes.size() > 1;
        b.dots.setVisibility(several ? View.VISIBLE : View.GONE);
        if (several) {
            dotsMediator = new TabLayoutMediator(b.dots, b.pager, (tab, position) -> { });
            dotsMediator.attach();
        }
        b.pager.setContentDescription(getString(R.string.cd_gallery, b.pager.getCurrentItem() + 1, a.photoRes.size()));

        UiUtils.bindTypeStatusBadge(b.txtBadge, a.type, a.status);
        b.txtTitle.setText(a.title);
        b.txtMeta.setText(getString(R.string.separator_dot, DateUtil.published(requireContext(), a.createdAt),
                getString(a.category.labelRes)));
        b.txtDescription.setText(a.description);
        b.txtDescription.setVisibility(a.hasDescription() ? View.VISIBLE : View.GONE);

        b.rowColor.txtInfoLabel.setText(R.string.detail_color);
        b.rowColor.txtInfoValue.setText(a.color == null ? R.string.color_unknown : a.color.labelRes);
        b.rowLocation.txtInfoLabel.setText(R.string.detail_location);
        b.rowLocation.txtInfoValue.setText(MapHelper.label(requireContext(), a.location));
        b.rowDate.txtInfoLabel.setText(a.type == AnnouncementType.LOST ? R.string.detail_date_lost : R.string.detail_date_found);
        b.rowDate.txtInfoValue.setText(DateUtil.longDate(a.eventDate));

        // Mini-carte : modifiable par l'auteur, en lecture seule pour les autres.
        rebuildMiniMap(author);
        b.btnEditLocation.setVisibility(author ? View.VISIBLE : View.GONE);
        b.btnEditLocation.setOnClickListener(v -> openLocationPicker());

        User authorUser = FakeRepository.get().user(a.authorId);
        if (authorUser != null) {
            UiUtils.bindInitialsAvatar(b.txtAuthorAvatar, authorUser);
            b.txtAuthorName.setText(authorUser.fullName);
            b.txtAuthorSubtitle.setText(getString(R.string.detail_author_subtitle, authorUser.role));
        }
        b.cardAuthor.setVisibility(authorUser == null ? View.GONE : View.VISIBLE);

        boolean showContact = !author && a.isActive();
        boolean showMatches = author && a.isActive();
        b.btnContact.setVisibility(showContact ? View.VISIBLE : View.GONE);
        b.btnMatches.setVisibility(showMatches ? View.VISIBLE : View.GONE);
        b.bottomBar.setVisibility(showContact || showMatches ? View.VISIBLE : View.GONE);
        b.btnContact.setOnClickListener(v -> contactAuthor());
        b.btnMatches.setOnClickListener(v -> Nav.go(this, R.id.matchesFragment, MatchesFragment.args(a.id)));
    }

    private void rebuildMiniMap(boolean editable) {
        if (miniMap != null) {
            if (resumed) miniMap.onPause();
            miniMap.onDestroy();
        }
        b.miniMap.mapContainer.removeAllViews();
        miniMap = new MiniMapController(b.miniMap, editable ? this::openLocationPicker : null);
        if (announcement != null) {
            miniMap.show(announcement.location, FakeRepository.get().campusCenter(), null);
        }
        if (resumed) miniMap.onResume();
    }

    private void contactAuthor() {
        if (announcement == null) return;
        Conversation c = FakeRepository.get().openConversation(announcement.authorId, announcement.id);
        Nav.go(this, R.id.chatFragment, ChatFragment.args(c.id));
    }

    private void openLocationPicker() {
        if (announcement == null) return;
        Nav.go(this, R.id.locationPickerFragment, LocationPickerFragment.args(announcement.location));
    }

    private void observeLocationResult() {
        NavBackStackEntry entry = Nav.of(this).getCurrentBackStackEntry();
        if (entry == null) return;
        entry.getSavedStateHandle().<CampusLocation>getLiveData(LocationPickerFragment.RESULT_KEY)
                .observe(getViewLifecycleOwner(), picked -> {
                    if (picked == null || announcement == null) return;
                    entry.getSavedStateHandle().remove(LocationPickerFragment.RESULT_KEY);
                    FakeRepository.get().updateLocation(announcement.id, picked);
                    bind();
                    Nav.main(this).showMessage(R.string.location_updated);
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
        if (needsRefresh && b != null && announcement != null) {
            bind();
        } else if (miniMap != null) {
            miniMap.onResume();
        }
        needsRefresh = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        resumed = false;
        needsRefresh = true;
        if (miniMap != null) miniMap.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dotsMediator != null) dotsMediator.detach();
        dotsMediator = null;
        if (miniMap != null) miniMap.onDestroy();
        miniMap = null;
        b = null;
    }
}
