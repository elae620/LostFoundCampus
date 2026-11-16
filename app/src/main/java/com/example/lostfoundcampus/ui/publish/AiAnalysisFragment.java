package com.example.lostfoundcampus.ui.publish;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.databinding.FragmentAiAnalysisBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.util.InsetsUtil;

/**
 * Overlay d'analyse IA. ÉTAPE 1 : analyse simulée (~3 s) puis retour au formulaire
 * avec les suggestions. Étape 2 : étiquettes, texte et couleur via ML Kit.
 */
public class AiAnalysisFragment extends Fragment {

    private static final int[] STATUS_MESSAGES = {
            R.string.ai_status_detect, R.string.ai_status_text,
            R.string.ai_status_color, R.string.ai_status_ready
    };

    private FragmentAiAnalysisBinding b;
    private PublishViewModel vm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int statusIndex;

    private final Runnable nextStatus = new Runnable() {
        @Override
        public void run() {
            if (b == null) return;
            statusIndex = Math.min(statusIndex + 1, STATUS_MESSAGES.length - 1);
            b.txtStatus.setText(STATUS_MESSAGES[statusIndex]);
            if (statusIndex < STATUS_MESSAGES.length - 1) {
                handler.postDelayed(this, getResources().getInteger(R.integer.ai_status_interval_ms));
            }
        }
    };

    private final Runnable finish = this::onAnalysisDone;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(PublishViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentAiAnalysisBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyBottom(b.panel, false);

        int photo = vm.analysisSource();
        if (photo == 0) {
            Nav.of(this).popBackStack();
            return;
        }
        b.imgPhoto.setImageResource(photo);
        b.btnCancel.setOnClickListener(v -> cancel());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        cancel();
                    }
                });

        b.aiScan.start();
        if (b.aiScan.isReducedMotion()) {
            b.txtStatus.setText(R.string.ai_status_static);
        } else {
            statusIndex = 0;
            b.txtStatus.setText(STATUS_MESSAGES[0]);
            handler.postDelayed(nextStatus, getResources().getInteger(R.integer.ai_status_interval_ms));
        }
        handler.postDelayed(finish, getResources().getInteger(R.integer.ai_analysis_duration_ms));
    }

    private void onAnalysisDone() {
        if (b == null) return;
        int photo = vm.analysisSource();
        vm.applyAnalysis(SimulatedMedia.analyze(photo), getResources().getInteger(R.integer.lf_max_photos));
        Nav.of(this).popBackStack(R.id.publishFragment, false);
    }

    /** Annuler arrête l'analyse et revient à l'écran précédent (aperçu ou formulaire), sans rien appliquer. */
    private void cancel() {
        handler.removeCallbacksAndMessages(null);
        Nav.of(this).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (b != null) b.aiScan.stop();
        b = null;
    }
}
