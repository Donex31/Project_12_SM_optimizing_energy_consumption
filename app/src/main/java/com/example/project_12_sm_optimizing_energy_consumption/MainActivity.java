package com.example.project_12_sm_optimizing_energy_consumption;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.project_12_sm_optimizing_energy_consumption.ocr.FaceDetectionOcr;
import com.example.project_12_sm_optimizing_energy_consumption.ocr.TextRecognitionOcr;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.text.Text;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private TextRecognitionOcr recognizer = new TextRecognitionOcr();
    private FaceDetectionOcr detector = new FaceDetectionOcr();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
    }

    public void testConnection(View view) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://optimizingenergyconsumption.appspot.com/";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> textView.setText("Response is: " + response),
                error -> textView.setText("That didn't work!"));
        queue.add(stringRequest);
    }

    public void recognizeText(View view) {
        textView.setText("Recognition started...");

        InputImage inputImage = recognizer.getInputImage(getResources(), R.drawable.germany);

        Instant start = Instant.now();

        Task<Text> process = recognizer.process(inputImage);

        process.addOnSuccessListener(visionText -> {
            Instant finish = Instant.now();
            Duration between = Duration.between(start, finish);
            textView.setText("Elapsed time: " + between.toString());
        }).addOnFailureListener(e -> {
            textView.setText("Error");
        });
    }

    public void detectFaces(View view) {
        textView.setText("Detection started...");

        InputImage inputImage = detector.getInputImage(getResources(), R.drawable.passat);

        Instant start = Instant.now();

        Task<List<Face>> process = detector.process(inputImage);

        process.addOnSuccessListener(visionText -> {
            Instant finish = Instant.now();
            Duration between = Duration.between(start, finish);
            textView.setText("Elapsed time: " + between.toString());
        }).addOnFailureListener(e -> {
            textView.setText("Error");
        });
    }


}