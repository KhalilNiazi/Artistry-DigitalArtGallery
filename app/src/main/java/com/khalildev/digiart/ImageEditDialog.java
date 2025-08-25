/*
package com.khalildev.digiart;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.skydoves.colorpickerview.sliders.AlphaSlideBar;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import java.io.InputStream;

import ja.burhanrashid52.photoeditor.OnSaveBitmap;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.PhotoFilter;
import ja.burhanrashid52.photoeditor.TextStyleBuilder;

public class ImageEditDialog extends Dialog {

    private PhotoEditorView photoEditorView;
    private PhotoEditor photoEditor;
    private ImageButton btnClose, btnDraw, btnAddText, btnApplyFilter, btnUndo, btnRedo, btnSave;
    private Uri imageUri;
    private GridLayout filterPreviewLayout;

    private int currentBrushColor = Color.BLUE;

    public ImageEditDialog(@NonNull Context context, Uri imageUri) {
        super(context);
        this.imageUri = imageUri;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_image_edit);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        photoEditorView = findViewById(R.id.photoEditorView);
        photoEditor = new PhotoEditor.Builder(getContext(), photoEditorView)
                .setPinchTextScalable(true)
                .build();

        loadImageIntoEditor(imageUri);

        btnClose = findViewById(R.id.btnClose);
        btnDraw = findViewById(R.id.btnDraw);
        btnAddText = findViewById(R.id.btnAddText);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnSave = findViewById(R.id.btnSave);
        filterPreviewLayout = findViewById(R.id.filterPreviewLayout);

        btnClose.setOnClickListener(v -> dismiss());
        btnDraw.setOnClickListener(v -> showBrushOptions());
        btnAddText.setOnClickListener(v -> addTextToImage());
        btnApplyFilter.setOnClickListener(v -> showFilterPreviews());
        btnUndo.setOnClickListener(v -> photoEditor.undo());
        btnRedo.setOnClickListener(v -> photoEditor.redo());
        btnSave.setOnClickListener(v -> saveImage());
    }

    private void loadImageIntoEditor(Uri imageUri) {
        try {
            Bitmap bitmap = getBitmapFromUri(imageUri);
            photoEditorView.getSource().setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws Exception {
        InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
        return BitmapFactory.decodeStream(inputStream);
    }

    */
/** Brush settings with color picker *//*

    private void showBrushOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Brush Settings");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getContext().getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        // Brush size
        final SeekBar sizeSeekBar = new SeekBar(getContext());
        sizeSeekBar.setMax(100);
        sizeSeekBar.setProgress(10);
        layout.addView(sizeSeekBar);

        // Color picker
        final ColorPickerView colorPickerView = new ColorPickerView(getContext());
        colorPickerView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 500));
        layout.addView(colorPickerView);

        // Brightness & alpha sliders
        BrightnessSlideBar brightnessSlide = new BrightnessSlideBar(getContext());
        AlphaSlideBar alphaSlide = new AlphaSlideBar(getContext());
        colorPickerView.attachBrightnessSlider(brightnessSlide);
        colorPickerView.attachAlphaSlider(alphaSlide);

        layout.addView(brightnessSlide);
        layout.addView(alphaSlide);

        final int[] selectedColor = {currentBrushColor};
        colorPickerView.setColorListener((ColorEnvelopeListener) (envelope, fromUser) ->
                selectedColor[0] = envelope.getColor()
        );

        builder.setView(layout);

        builder.setPositiveButton("Apply", (dialog, which) -> {
            currentBrushColor = selectedColor[0];
            int brushSize = sizeSeekBar.getProgress();

            photoEditor.setBrushDrawingMode(true);
            photoEditor.setBrushSize(brushSize);
            photoEditor.setBrushColor(currentBrushColor);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    */
/** Add text with font + color picker *//*

    private void addTextToImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Text");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getContext().getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final EditText input = new EditText(getContext());
        input.setHint("Enter your text");
        layout.addView(input);

        final Spinner fontSpinner = new Spinner(getContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Default", "Monospace", "Serif", "Sans"}
        );
        fontSpinner.setAdapter(adapter);
        layout.addView(fontSpinner);

        final SeekBar sizeSeekBar = new SeekBar(getContext());
        sizeSeekBar.setMax(100);
        sizeSeekBar.setProgress(42);
        layout.addView(sizeSeekBar);

        // Color picker
        final ColorPickerView colorPickerView = new ColorPickerView(getContext());
        colorPickerView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 500));
        layout.addView(colorPickerView);

        BrightnessSlideBar brightnessSlide = new BrightnessSlideBar(getContext());
        AlphaSlideBar alphaSlide = new AlphaSlideBar(getContext());
        colorPickerView.attachBrightnessSlider(brightnessSlide);
        colorPickerView.attachAlphaSlider(alphaSlide);

        layout.addView(brightnessSlide);
        layout.addView(alphaSlide);

        final int[] selectedColor = {Color.WHITE};
        colorPickerView.setColorListener((ColorEnvelopeListener) (envelope, fromUser) ->
                selectedColor[0] = envelope.getColor()
        );

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(getContext(), "Text cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String fontChoice = fontSpinner.getSelectedItem().toString();
            int textSize = sizeSeekBar.getProgress();

            TextStyleBuilder styleBuilder = new TextStyleBuilder()
                    .withTextColor(selectedColor[0])
                    .withTextSize(textSize);

            Typeface typeface;
            switch (fontChoice) {
                case "Monospace": typeface = Typeface.MONOSPACE; break;
                case "Serif": typeface = Typeface.SERIF; break;
                case "Sans": typeface = Typeface.SANS_SERIF; break;
                default: typeface = Typeface.DEFAULT;
            }
            styleBuilder.withTextFont(typeface);

            photoEditor.addText(text, styleBuilder);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showFilterPreviews() {
        filterPreviewLayout.removeAllViews();
        filterPreviewLayout.setVisibility(View.VISIBLE);

        PhotoFilter[] allFilters = PhotoFilter.values();
        for (PhotoFilter filter : allFilters) {
            try {
                Bitmap bitmap = getBitmapFromUri(imageUri);
                ImageView filterPreview = new ImageView(getContext());
                filterPreview.setLayoutParams(new GridLayout.LayoutParams());
                filterPreview.setPadding(10, 10, 10, 10);
                filterPreview.setAdjustViewBounds(true);
                filterPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

                PhotoEditorView previewView = new PhotoEditorView(getContext());
                previewView.getSource().setImageBitmap(bitmap);

                PhotoEditor tempEditor = new PhotoEditor.Builder(getContext(), previewView).build();
                tempEditor.setFilterEffect(filter);

                tempEditor.saveAsBitmap(new OnSaveBitmap() {
                    @Override
                    public void onBitmapReady(@NonNull Bitmap saveBitmap) {
                        filterPreview.setImageBitmap(saveBitmap);
                        filterPreview.setContentDescription(filter.name());

                        filterPreview.setOnClickListener(v -> {
                            applyFilter(filter);
                            filterPreviewLayout.setVisibility(View.GONE);
                        });

                        filterPreviewLayout.addView(filterPreview);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void applyFilter(PhotoFilter filter) {
        photoEditor.setFilterEffect(filter);
    }

    private void saveImage() {
        photoEditor.saveAsBitmap(new OnSaveBitmap() {
            @Override
            public void onBitmapReady(@NonNull Bitmap saveBitmap) {
                Toast.makeText(getContext(), "Image saved!", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }
}
*/
