package com.example.lostfoundcampus.ui.publish;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import com.example.lostfoundcampus.data.model.ColorSwatch;

/** Couleur dominante d'une photo (Palette), ramenée au nuancier de l'app. À appeler hors du fil principal. */
final class ColorMatcher {

    private ColorMatcher() {
    }

    @Nullable
    static ColorSwatch dominant(Bitmap bitmap) {
        Palette palette = Palette.from(bitmap).maximumColorCount(16).generate();
        Palette.Swatch swatch = palette.getDominantSwatch();
        if (swatch == null) return null;
        return nearest(swatch.getRgb());
    }

    static ColorSwatch nearest(int rgb) {
        float[] hsv = new float[3];
        Color.colorToHSV(rgb, hsv);
        float h = hsv[0], s = hsv[1], v = hsv[2];
        if (v < 0.22f) return ColorSwatch.BLACK;
        if (s < 0.15f) return v > 0.82f ? ColorSwatch.WHITE : ColorSwatch.GREY;
        if ((h < 15 || h >= 340)) return ColorSwatch.RED;
        if (h < 45) return v < 0.7f ? ColorSwatch.BROWN : ColorSwatch.OTHER;
        if (h < 70) return ColorSwatch.OTHER;
        if (h < 170) return ColorSwatch.GREEN;
        if (h < 260) return ColorSwatch.BLUE;
        return ColorSwatch.OTHER;
    }
}
