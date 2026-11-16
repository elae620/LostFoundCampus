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

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.databinding.FragmentForgotPasswordBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.util.InsetsUtil;
import com.example.lostfoundcampus.util.Validators;

public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding b;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyPadding(b.scroll, true, true, true);
        b.header.txtTitle.setText(R.string.forgot_toolbar);
        b.header.btnBack.setOnClickListener(v -> Nav.back(this));
        b.btnSend.setOnClickListener(v -> send());
        b.btnBackToLogin.setOnClickListener(v -> Nav.back(this));
        b.etEmail.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                send();
                return true;
            }
            return false;
        });
        Nav.clearErrorOnEdit(b.tilEmail);
    }

    private void send() {
        String email = Nav.text(b.etEmail);
        if (!Validators.isUniversityEmail(email)) {
            b.tilEmail.setError(getString(R.string.error_email_university));
            b.tilEmail.requestFocus();
            return;
        }
        b.btnSend.setEnabled(false);
        b.btnSend.setText(R.string.forgot_loading);
        // ÉTAPE 1 : envoi simulé. Étape 2 : FirebaseAuth.sendPasswordResetEmail.
        handler.postDelayed(() -> {
            if (b == null) return;
            b.txtSentBody.setText(getString(R.string.forgot_sent_body, email));
            b.groupForm.setVisibility(View.GONE);
            b.groupSent.setVisibility(View.VISIBLE);
        }, getResources().getInteger(R.integer.lf_fake_network_delay_ms));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        b = null;
    }
}
