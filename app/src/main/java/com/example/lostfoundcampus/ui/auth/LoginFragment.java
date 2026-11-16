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
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.lostfoundcampus.BuildConfig;
import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.FakeRepository;
import com.example.lostfoundcampus.databinding.FragmentLoginBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.util.InsetsUtil;
import com.example.lostfoundcampus.util.Validators;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding b;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentLoginBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyPadding(b.scroll, true, true, true);

        if (BuildConfig.DEBUG && savedInstanceState == null) {
            // Démo étape 1 : e-mail prérempli (mot de passe de démo : voir le bilan d'étape).
            b.etEmail.setText(FakeRepository.demoEmail());
        }

        b.btnLogin.setOnClickListener(v -> attemptLogin());
        b.etPassword.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
        b.btnForgot.setOnClickListener(v -> Nav.go(this, R.id.forgotPasswordFragment, null));
        b.btnRegister.setOnClickListener(v -> Nav.go(this, R.id.registerFragment, null));

        Nav.AfterTextChanged hideError = new Nav.AfterTextChanged(s -> b.txtError.setVisibility(View.GONE));
        b.etEmail.addTextChangedListener(hideError);
        b.etPassword.addTextChangedListener(hideError);
    }

    private void attemptLogin() {
        String email = Nav.text(b.etEmail);
        String password = b.etPassword.getText() == null ? "" : b.etPassword.getText().toString();
        if (email.isEmpty() || password.isEmpty()) {
            showError(R.string.login_error_empty);
            return;
        }
        if (!Validators.isUniversityEmail(email)) {
            showError(R.string.login_error_credentials);
            return;
        }
        setLoading(true);
        // ÉTAPE 1 : délai réseau simulé. Étape 2 : FirebaseAuth.signInWithEmailAndPassword.
        handler.postDelayed(() -> {
            if (b == null) return;
            setLoading(false);
            if (FakeRepository.get().signIn(email, password)) {
                Nav.main(this).showMain();
            } else {
                showError(R.string.login_error_credentials);
            }
        }, getResources().getInteger(R.integer.lf_fake_network_delay_ms));
    }

    private void showError(@StringRes int message) {
        b.txtError.setText(message);
        b.txtError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        b.btnLogin.setEnabled(!loading);
        b.btnLogin.setText(loading ? R.string.login_loading : R.string.login_submit);
        b.etEmail.setEnabled(!loading);
        b.etPassword.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        b = null;
    }
}
