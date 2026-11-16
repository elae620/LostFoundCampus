package com.example.lostfoundcampus.ui.profile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.data.model.User;
import com.example.lostfoundcampus.databinding.FragmentEditProfileBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.ui.common.UiUtils;
import com.example.lostfoundcampus.util.InsetsUtil;
import com.example.lostfoundcampus.util.Validators;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Modifier mon profil (accessible uniquement depuis son propre profil) :
 * nom, photo (galerie) et mot de passe (actuel, nouveau, confirmation).
 */
public class EditProfileFragment extends Fragment {

    private static final String STATE_PHOTO = "pending_photo";

    private FragmentEditProfileBinding b;
    private final Handler handler = new Handler(Looper.getMainLooper());
    @DrawableRes
    private int pendingPhoto;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pendingPhoto = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_PHOTO)
                : FakeRepository.get().currentUser().photoRes;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentEditProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyPadding(b.scroll, true, true, true);
        b.header.txtTitle.setText(R.string.edit_profile_title);
        b.header.btnBack.setOnClickListener(v -> Nav.back(this));

        User me = FakeRepository.get().currentUser();
        if (savedInstanceState == null) b.etName.setText(me.fullName);
        b.txtEmail.setText(me.universityEmail);
        renderAvatar();

        b.btnChangePhoto.setOnClickListener(v -> {
            // ÉTAPE 1 : choix galerie simulé. Étape 2 : PickVisualMedia + envoi Storage.
            pendingPhoto = R.drawable.sample_avatar;
            renderAvatar();
            snack(R.string.edit_profile_photo_changed);
        });
        b.btnSaveProfile.setOnClickListener(v -> saveProfile());
        b.btnChangePassword.setOnClickListener(v -> changePassword());

        for (TextInputLayout til : new TextInputLayout[]{b.tilName, b.tilCurrent, b.tilNew, b.tilConfirm}) {
            Nav.clearErrorOnEdit(til);
        }
    }

    private void renderAvatar() {
        User me = FakeRepository.get().currentUser();
        if (pendingPhoto != 0) {
            b.avatar.imgPhoto.setImageResource(pendingPhoto);
            b.avatar.imgPhoto.setVisibility(View.VISIBLE);
            b.avatar.txtInitials.setVisibility(View.INVISIBLE);
        } else {
            b.avatar.imgPhoto.setVisibility(View.GONE);
            b.avatar.txtInitials.setVisibility(View.VISIBLE);
            b.avatar.txtInitials.setText(UiUtils.initials(me.fullName));
        }
    }

    private void saveProfile() {
        String name = Nav.text(b.etName);
        if (Validators.isBlank(name)) {
            b.tilName.setError(getString(R.string.error_name_required));
            b.tilName.requestFocus();
            return;
        }
        setBusy(b.btnSaveProfile, true);
        // ÉTAPE 1 : mise à jour locale. Étape 2 : Firestore (users/{uid}) + Storage pour la photo.
        handler.postDelayed(() -> {
            if (b == null) return;
            User me = FakeRepository.get().currentUser();
            me.fullName = name;
            me.photoRes = pendingPhoto;
            setBusy(b.btnSaveProfile, false);
            renderAvatar();
            snack(R.string.edit_profile_saved);
        }, getResources().getInteger(R.integer.lf_fake_network_delay_ms));
    }

    private void changePassword() {
        String current = raw(b.tilCurrent);
        String next = raw(b.tilNew);
        String confirm = raw(b.tilConfirm);
        TextInputLayout firstError = null;

        if (current.isEmpty()) {
            b.tilCurrent.setError(getString(R.string.error_password_current_required));
            firstError = b.tilCurrent;
        } else if (!FakeRepository.get().checkPassword(current)) {
            // Étape 2 : ré-authentification Firebase (EmailAuthProvider) avant updatePassword.
            b.tilCurrent.setError(getString(R.string.error_password_current));
            firstError = b.tilCurrent;
        }
        if (!Validators.isPasswordLongEnough(requireContext(), next)) {
            b.tilNew.setError(getString(R.string.error_password_short));
            if (firstError == null) firstError = b.tilNew;
        }
        if (!next.equals(confirm)) {
            b.tilConfirm.setError(getString(R.string.error_password_mismatch));
            if (firstError == null) firstError = b.tilConfirm;
        }
        if (firstError != null) {
            firstError.requestFocus();
            return;
        }

        setBusy(b.btnChangePassword, true);
        handler.postDelayed(() -> {
            if (b == null) return;
            FakeRepository.get().changePassword(next);
            b.etCurrent.setText("");
            b.etNew.setText("");
            b.etConfirm.setText("");
            setBusy(b.btnChangePassword, false);
            snack(R.string.edit_profile_password_saved);
        }, getResources().getInteger(R.integer.lf_fake_network_delay_ms));
    }

    private static String raw(TextInputLayout til) {
        return til.getEditText() == null || til.getEditText().getText() == null
                ? "" : til.getEditText().getText().toString();
    }

    private void setBusy(View button, boolean busy) {
        button.setEnabled(!busy);
    }

    private void snack(int message) {
        Snackbar.make(b.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PHOTO, pendingPhoto);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        b = null;
    }
}
