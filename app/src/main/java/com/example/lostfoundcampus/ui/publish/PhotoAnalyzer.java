package com.example.lostfoundcampus.ui.publish;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lostfoundcampus.R;
import com.example.lostfoundcampus.data.model.Category;
import com.example.lostfoundcampus.data.model.ColorSwatch;
import com.example.lostfoundcampus.data.model.ImageAnalysis;
import com.example.lostfoundcampus.util.AppExecutors;
import com.example.lostfoundcampus.util.PhotoCompressor;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Analyse d'une photo sur l'appareil (RG03 : les résultats ne sont que des suggestions) :
 * étiquettes ML Kit, texte visible ML Kit, couleur dominante Palette.
 */
final class PhotoAnalyzer {

    interface Callback {
        void onResult(@NonNull ImageAnalysis analysis);

        void onError();
    }

    private static final float MIN_LABEL_CONFIDENCE = 0.55f;
    private static final int MAX_LABELS = 6;

    private final ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private final Context app;
    private final String[] labelKeys;
    private final String[] labelFr;
    private volatile boolean closed;

    PhotoAnalyzer(Context context) {
        app = context.getApplicationContext();
        Resources r = app.getResources();
        labelKeys = r.getStringArray(R.array.mlkit_label_keys);
        labelFr = r.getStringArray(R.array.mlkit_label_fr);
    }

    void analyze(Uri uri, Callback cb) {
        AppExecutors.io(() -> {
            Bitmap bitmap;
            ColorSwatch color;
            try {
                bitmap = PhotoCompressor.decode(app, uri, PhotoCompressor.ANALYSIS_MAX_SIDE);
                color = ColorMatcher.dominant(bitmap);
            } catch (Exception e) {
                AppExecutors.main(cb::onError);
                return;
            }
            final ColorSwatch dominant = color;
            AppExecutors.main(() -> runMlKit(bitmap, dominant, cb));
        });
    }

    private void runMlKit(Bitmap bitmap, @Nullable ColorSwatch color, Callback cb) {
        if (closed) return;
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        Task<List<ImageLabel>> labelTask = labeler.process(image);
        Task<Text> textTask = recognizer.process(image);
        Tasks.whenAllComplete(labelTask, textTask).addOnCompleteListener(done -> {
            if (closed) return;
            List<ImageLabel> labels = labelTask.isSuccessful() ? labelTask.getResult() : new ArrayList<>();
            Text text = textTask.isSuccessful() ? textTask.getResult() : null;
            if (!labelTask.isSuccessful() && !textTask.isSuccessful() && color == null) {
                cb.onError();
                return;
            }
            cb.onResult(build(labels, text, color));
        });
    }

    private ImageAnalysis build(List<ImageLabel> mlLabels, @Nullable Text text, @Nullable ColorSwatch color) {
        List<String> labels = new ArrayList<>();
        String topEnglish = null;
        int confidence = 0;
        for (ImageLabel l : mlLabels) {
            if (l.getConfidence() < MIN_LABEL_CONFIDENCE || labels.size() >= MAX_LABELS) continue;
            String key = l.getText().trim().toLowerCase(Locale.ROOT);
            if (topEnglish == null) {
                topEnglish = key;
                confidence = Math.round(l.getConfidence() * 100);
            }
            labels.add(key);
        }

        List<String> texts = new ArrayList<>();
        if (text != null) {
            for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String t = line.getText().trim();
                    if (t.length() >= 2 && t.length() <= 30 && t.matches(".*\\p{L}.*")) texts.add(t);
                    if (texts.size() >= 5) break;
                }
            }
        }

        Category category = categoryFor(labels);
        String display = topEnglish == null ? app.getString(category.labelRes) : translate(topEnglish);
        String brand = null;
        for (String t : texts) {
            if (t.matches("[\\p{L}][\\p{L}\\- ]{1,14}")) {
                brand = t;
                break;
            }
        }
        String title = brand != null && topEnglish != null ? display + " " + brand : display;
        return new ImageAnalysis(labels, display, confidence, texts, color, category, title);
    }

    /** Traduction des étiquettes ML Kit (anglais) ; à défaut, l'étiquette avec une majuscule. */
    private String translate(String english) {
        for (int i = 0; i < labelKeys.length && i < labelFr.length; i++) {
            if (labelKeys[i].equals(english)) return labelFr[i];
        }
        return english.substring(0, 1).toUpperCase(Locale.FRANCE) + english.substring(1);
    }

    /** Catégorie déduite des étiquettes (mots-clés, non affichés). */
    static Category categoryFor(List<String> labels) {
        for (String l : labels) {
            if (l.contains("phone")) return Category.PHONES;
            if (l.equals("key") || l.contains("keys") || l.contains("keychain")) return Category.KEYS;
            if (l.contains("wallet") || l.contains("purse")) return Category.WALLETS;
            if (l.contains("bag") || l.contains("backpack") || l.contains("luggage") || l.contains("suitcase")) return Category.BAGS;
            if (l.contains("card") || l.contains("passport")) return Category.CARDS;
            if (l.contains("computer") || l.contains("laptop") || l.contains("headphone") || l.contains("camera")
                    || l.contains("tablet") || l.contains("watch") || l.contains("gadget") || l.contains("television")
                    || l.contains("keyboard") || l.contains("mouse") || l.contains("electronic")) return Category.ELECTRONICS;
            if (l.contains("jacket") || l.contains("shirt") || l.contains("jeans") || l.contains("coat")
                    || l.contains("cap") || l.contains("hat") || l.contains("shoe") || l.contains("sneaker")
                    || l.contains("scarf") || l.contains("glove") || l.contains("clothing") || l.contains("sweater")
                    || l.contains("glasses") || l.contains("sunglasses")) return Category.CLOTHES;
        }
        return Category.OTHER;
    }

    void close() {
        closed = true;
        labeler.close();
        recognizer.close();
    }
}
