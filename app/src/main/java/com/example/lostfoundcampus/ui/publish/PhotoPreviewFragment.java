package com.example.lostfoundcampus.ui.publish;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.databinding.FragmentPhotoPreviewBinding;
import com.example.lostfoundcampus.ui.common.Nav;
import com.example.lostfoundcampus.util.InsetsUtil;

/** Aperçu plein cadre de la photo prise : « Analyser avec l'IA » ou « Reprendre la photo ». */
public class PhotoPreviewFragment extends Fragment {

    private FragmentPhotoPreviewBinding b;
    private PublishViewModel vm;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(PublishViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentPhotoPreviewBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InsetsUtil.applyTop(b.topBar);
        InsetsUtil.applyBottom(b.actions, false);

        if (vm.pendingPhoto == null) {
            // Rien à prévisualiser (ex. retour après une restauration) : on revient au formulaire.
            Nav.of(this).popBackStack();
            return;
        }
        b.imgPhoto.setImageResource(vm.pendingPhoto);

        b.btnClose.setOnClickListener(v -> {
            vm.pendingPhoto = null;
            Nav.of(this).popBackStack();
        });
        b.btnAnalyze.setOnClickListener(v -> Nav.go(this, R.id.aiAnalysisFragment, null));
        b.btnRetake.setOnClickListener(v -> {
            // ÉTAPE 1 : nouvelle prise de vue simulée. Étape 2 : relance TakePicture.
            vm.pendingPhoto = SimulatedMedia.capturePhoto();
            b.imgPhoto.setImageResource(vm.pendingPhoto);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
