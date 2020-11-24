package com.example.project_12_sm_optimizing_energy_consumption.ocr;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Surface;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import androidx.core.content.res.ResourcesCompat;

public class TextRecognitionOcr {

    private TextRecognizer recognizer = TextRecognition.getClient();

    public Task<Text> process(InputImage image) {
        return recognizer.process(image);
    }

    public InputImage getInputImage(Resources resources, int id) {
        Drawable d = ResourcesCompat.getDrawable(resources, id, null);
        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
        return InputImage.fromBitmap(bitmap, Surface.ROTATION_0);
    }

}
