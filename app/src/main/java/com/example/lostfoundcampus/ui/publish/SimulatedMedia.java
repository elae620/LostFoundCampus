package com.example.lostfoundcampus.ui.publish;

import androidx.annotation.DrawableRes;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.model.Category;
import com.example.lostfoundcampus.data.model.ColorSwatch;
import com.example.lostfoundcampus.data.model.ImageAnalysis;

import java.util.ArrayList;
import java.util.List;

/**
 * ÉTAPE 1 UNIQUEMENT — simule la caméra, la galerie et l'analyse d'image.
 * Étape 2 : TakePicture + FileProvider, PickMultipleVisualMedia (Photo Picker), ML Kit.
 * Les libellés d'analyse sont des données (ce que renverra ML Kit), pas des textes d'interface.
 */
final class SimulatedMedia {

    private static final int[] CAMERA_SHOTS = {
            R.drawable.sample_phone_found, R.drawable.sample_keys,
            R.drawable.sample_wallet, R.drawable.sample_student_card
    };
    private static final int[] GALLERY = {
            R.drawable.sample_iphone, R.drawable.sample_phone_case, R.drawable.sample_backpack,
            R.drawable.sample_wallet, R.drawable.sample_keys, R.drawable.sample_student_card
    };
    private static final int GALLERY_BATCH = 2;

    private static int cameraIndex = 0;
    private static int galleryIndex = 0;

    private SimulatedMedia() {
    }

    @DrawableRes
    static int capturePhoto() {
        int res = CAMERA_SHOTS[cameraIndex % CAMERA_SHOTS.length];
        cameraIndex++;
        return res;
    }

    /** Sélection multiple simulée : renvoie au plus {@code max} photos. */
    static List<Integer> pickFromGallery(int max) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < Math.min(GALLERY_BATCH, max); i++) {
            out.add(GALLERY[galleryIndex % GALLERY.length]);
            galleryIndex++;
        }
        return out;
    }

    static ImageAnalysis analyze(@DrawableRes int photo) {
        if (photo == R.drawable.sample_iphone || photo == R.drawable.sample_phone_found) {
            return new ImageAnalysis("Smartphone", 92, "Samsung", ColorSwatch.BLACK, Category.PHONES, "Smartphone Samsung");
        } else if (photo == R.drawable.sample_phone_case) {
            return new ImageAnalysis("Étui de téléphone", 81, null, ColorSwatch.BLUE, Category.PHONES, "Étui de téléphone bleu");
        } else if (photo == R.drawable.sample_keys) {
            return new ImageAnalysis("Clés", 88, null, ColorSwatch.RED, Category.KEYS, "Clés avec porte-clés rouge");
        } else if (photo == R.drawable.sample_backpack) {
            return new ImageAnalysis("Sac à dos", 90, null, ColorSwatch.BLUE, Category.BAGS, "Sac à dos bleu");
        } else if (photo == R.drawable.sample_wallet) {
            return new ImageAnalysis("Portefeuille", 85, null, ColorSwatch.BROWN, Category.WALLETS, "Portefeuille marron");
        } else if (photo == R.drawable.sample_student_card) {
            return new ImageAnalysis("Carte d’identité", 77, "UNIVERSITÉ", ColorSwatch.WHITE, Category.CARDS, "Carte étudiante");
        }
        return new ImageAnalysis("Objet", 60, null, ColorSwatch.OTHER, Category.OTHER, "Objet");
    }
}
