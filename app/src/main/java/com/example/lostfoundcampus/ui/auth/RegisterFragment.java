package com.example.lostfoundcampus.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lostfoundcampus.MainActivity;
import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.databinding.FragmentRegisterBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.util.InsetsUtil;
import com.example.lostfoundcampus.util.Validators;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding b;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentRegisterBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyPadding(b.scroll, true, true, true);
        b.header.txtTitle.setText(R.string.register_toolbar);
        b.header.btnBack.setOnClickListener(v -> Nav.back(this));
        b.btnSignIn.setOnClickListener(v -> Nav.back(this));
        b.btnSubmit.setOnClickListener(v -> submit());
        b.etConfirm.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        for (TextInputLayout til : new TextInputLayout[]{b.tilName, b.tilEmail, b.tilPassword, b.tilConfirm}) {
            Nav.clearErrorOnEdit(til);
        }
    }

    private void submit() {
        String name = Nav.text(b.etName);
        String email = Nav.text(b.etEmail);
        String password = b.etPassword.getText() == null ? "" : b.etPassword.getText().toString();
        String confirm = b.etConfirm.getText() == null ? "" : b.etConfirm.getText().toString();

        TextInputLayout firstError = null;
        if (Validators.isBlank(name)) {
            b.tilName.setError(getString(R.string.error_name_required));
            firstError = b.tilName;
        }
        if (!Validators.isUniversityEmail(email)) {
            b.tilEmail.setError(getString(R.string.error_email_university));
            if (firstError == null) firstError = b.tilEmail;
        }
        if (!Validators.isPasswordLongEnough(requireContext(), password)) {
            b.tilPassword.setError(getString(R.string.error_password_short));
            if (firstError == null) firstError = b.tilPassword;
        }
        if (!password.equals(confirm)) {
            b.tilConfirm.setError(getString(R.string.error_password_mismatch));
            if (firstError == null) firstError = b.tilConfirm;
        }
        if (firstError != null) {
            firstError.requestFocus();
            return;
        }

        setLoading(true);
        // ÉTAPE 1 : création simulée. Étape 2 : FirebaseAuth.createUserWithEmailAndPassword + profil Firestore.
        handler.postDelayed(() -> {
            if (b == null) return;
            FakeRepository.get().register(name);
            MainActivity main = Nav.main(this);
            main.showMain();
            main.showMessage(R.string.register_success);
        }, getResources().getInteger(R.integer.lf_fake_network_delay_ms));
    }

    private void setLoading(boolean loading) {
        b.btnSubmit.setEnabled(!loading);
        b.btnSubmit.setText(loading ? R.string.register_loading : R.string.register_submit);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        b = null;
    }
}
