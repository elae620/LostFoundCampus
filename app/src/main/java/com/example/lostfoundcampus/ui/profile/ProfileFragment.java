package com.example.lostfoundcampus.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.data.model.Announcement;
import com.example.lostfoundcampus.data.model.AnnouncementType;
import com.example.lostfoundcampus.data.model.User;
import com.example.lostfoundcampus.databinding.FragmentProfileBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.ui.common.UiUtils;
import com.example.lostfoundcampus.ui.detail.AnnouncementDetailFragment;
import com.example.lostfoundcampus.ui.publish.PublishFragment;
import com.example.lostfoundcampus.util.InsetsUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding b;
    private MyAnnouncementAdapter adapter;
    private FakeRepository.ProfileFilter filter = FakeRepository.ProfileFilter.ALL;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyTop(b.scroll);

        adapter = new MyAnnouncementAdapter(new MyAnnouncementAdapter.Listener() {
            @Override
            public void onOpen(Announcement a) {
                Nav.go(ProfileFragment.this, R.id.announcementDetailFragment, AnnouncementDetailFragment.args(a.id));
            }

            @Override
            public void onEdit(Announcement a) {
                Nav.go(ProfileFragment.this, R.id.publishFragment, PublishFragment.argsEdit(a.id));
            }

            @Override
            public void onMarkReturned(Announcement a) {
                confirmReturned(a);
            }
        });
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.list.setAdapter(adapter);

        b.chipsFilter.setOnCheckedStateChangeListener((group, ids) -> {
            int id = ids.isEmpty() ? R.id.chipAll : ids.get(0);
            if (id == R.id.chipLost) filter = FakeRepository.ProfileFilter.LOST;
            else if (id == R.id.chipFound) filter = FakeRepository.ProfileFilter.FOUND;
            else if (id == R.id.chipReturned) filter = FakeRepository.ProfileFilter.RETURNED;
            else filter = FakeRepository.ProfileFilter.ALL;
            renderList();
        });

        b.btnEditProfile.setOnClickListener(v -> Nav.go(this, R.id.editProfileFragment, null));
        b.btnLogout.setOnClickListener(v -> confirmLogout());
        b.empty.btnEmptyAction.setText(R.string.profile_empty_action);
        b.empty.btnEmptyAction.setOnClickListener(v ->
                Nav.go(this, R.id.publishFragment, PublishFragment.argsNew(AnnouncementType.LOST)));
    }

    @Override
    public void onResume() {
        super.onResume();
        renderHeader();
        renderList();
    }

    private void renderHeader() {
        User me = FakeRepository.get().currentUser();
        UiUtils.bindAvatar(b.avatar.txtInitials, b.avatar.imgPhoto, me);
        b.txtName.setText(me.fullName);
        b.txtEmail.setText(me.universityEmail);
    }

    private void renderList() {
        if (b == null) return;
        List<Announcement> data = FakeRepository.get().myAnnouncements(filter);
        adapter.submit(data);
        boolean empty = data.isEmpty();
        b.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            b.empty.imgEmpty.setImageResource(R.drawable.ic_inbox);
            b.empty.txtEmptyTitle.setText(R.string.profile_empty_title);
            b.empty.txtEmptyBody.setText(R.string.profile_empty_body);
            b.empty.btnEmptyAction.setVisibility(View.VISIBLE);
        }
    }

    private void confirmReturned(Announcement a) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.returned_dialog_title)
                .setMessage(getString(R.string.returned_dialog_body, a.title))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_confirm, (d, w) -> {
                    // ÉTAPE 1 : statut local. Étape 2 : mise à jour Firestore (auteur uniquement).
                    FakeRepository.get().markReturned(a.id);
                    renderList();
                    Nav.main(this).showMessage(R.string.returned_done);
                })
                .show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_dialog_title)
                .setMessage(R.string.logout_dialog_body)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.profile_logout, (d, w) -> {
                    FakeRepository.get().signOut();
                    Nav.main(this).showAuth();
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
