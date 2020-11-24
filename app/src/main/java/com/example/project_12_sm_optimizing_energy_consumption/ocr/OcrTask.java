package com.example.project_12_sm_optimizing_energy_consumption.ocr;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Surface;

import com.google.mlkit.vision.common.InputImage;

import androidx.core.content.res.ResourcesCompat;

public abstract class OcrTask {
    public InputImage getInputImage(Resources resources, int id) {
        Drawable d = ResourcesCompat.getDrawable(resources, id, null);
        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
        return InputImage.fromBitmap(bitmap, Surface.ROTATION_0);
    }


}
