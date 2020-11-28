package pl.edu.agh.sm.project12.datacollection;

import android.content.Context;
import android.os.BatteryManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import pl.edu.agh.sm.project12.R;
import pl.edu.agh.sm.project12.battery.BatteryConsumptionMonitor;
import pl.edu.agh.sm.project12.ocr.FaceDetectionOcr;
import pl.edu.agh.sm.project12.ocr.TextRecognitionOcr;

import static android.content.Context.BATTERY_SERVICE;

public class DataCollectionWorker extends Worker {
    private static final String TAG = DataCollectionWorker.class.getSimpleName();

    public static final String KEY_ITERATIONS = "iterations";
    public static final String KEY_PROGRESS = "progress";

    private final WorkerParameters workerParams;

    public DataCollectionWorker(Context appContext, WorkerParameters workerParams) {
        super(appContext, workerParams);
        this.workerParams = workerParams;
        setProgressAsync(new Data.Builder().putInt(KEY_PROGRESS, 0).build());
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = workerParams.getInputData();

        int iterations = inputData.getInt(KEY_ITERATIONS, 0);

        Log.i(TAG, "Starting data collection, " + iterations + " iterations");
        for (int i = 0; i < iterations; ++i) {
            Log.i(TAG, "Data collection, iteration " + i);
            performIteration();
            setProgressAsync(new Data.Builder().putInt(KEY_PROGRESS, i).build());
        }
        return Result.success();
    }

    private final TextRecognitionOcr recognizer = new TextRecognitionOcr();

    private void performIteration() {
        BatteryManager batteryManager = (BatteryManager) getApplicationContext().getSystemService(BATTERY_SERVICE);
        Duration samplingPeriod = Duration.ofMillis(10);
        BatteryConsumptionMonitor batteryConsumptionMonitor = new BatteryConsumptionMonitor(batteryManager, samplingPeriod);
        batteryConsumptionMonitor.start();
        try {
            InputImage inputImage = recognizer.getInputImage(
                    getApplicationContext().getResources(), R.drawable.germany);

            Instant start = Instant.now();

            CountDownLatch latch = new CountDownLatch(1);
            Task<Text> process = recognizer.process(inputImage);

            process.addOnSuccessListener(visionText -> {
                Instant finish = Instant.now();
                Duration between = Duration.between(start, finish);
                Log.i(TAG, "Elapsed time: " + between.toString());
                latch.countDown();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error while OCRing");
                latch.countDown();
            });
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            batteryConsumptionMonitor.stop();
        }
    }
}
