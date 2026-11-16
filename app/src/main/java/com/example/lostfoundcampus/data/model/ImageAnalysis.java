package com.example.lostfoundcampus.data.model;

import androidx.annotation.Nullable;

/**
 * Résultat d'analyse d'une photo : étiquette, texte visible et couleur dominante.
 * Étape 1 : valeurs simulées. Étape 2 : ML Kit.
 */
public class ImageAnalysis {

    public final String label;
    public final int confidence;
    @Nullable
    public final String detectedText;
    public final ColorSwatch dominantColor;
    public final Category suggestedCategory;
    public final String suggestedTitle;
    public boolean userConfirmed;

    public ImageAnalysis(String label, int confidence, @Nullable String detectedText,
                         ColorSwatch dominantColor, Category suggestedCategory, String suggestedTitle) {
        this.label = label;
        this.confidence = confidence;
        this.detectedText = detectedText;
        this.dominantColor = dominantColor;
        this.suggestedCategory = suggestedCategory;
        this.suggestedTitle = suggestedTitle;
    }

    public boolean hasText() {
        return detectedText != null && !detectedText.isEmpty();
    }
}
