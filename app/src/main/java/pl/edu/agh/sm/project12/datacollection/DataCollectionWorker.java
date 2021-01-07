package pl.edu.agh.sm.project12.datacollection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import pl.edu.agh.sm.project12.MainActivity;
import pl.edu.agh.sm.project12.battery.BatteryConsumptionListener;
import pl.edu.agh.sm.project12.battery.BatteryConsumptionMonitor;
import pl.edu.agh.sm.project12.cloudocr.TextRecognitionCloudOcr;
import pl.edu.agh.sm.project12.ocr.TextRecognitionOcr;

import static android.content.Context.BATTERY_SERVICE;

public class DataCollectionWorker extends Worker {
    private static final String TAG = DataCollectionWorker.class.getSimpleName();

    public static final String KEY_ITERATIONS = "iterations";
    public static final String KEY_PROGRESS = "progress";
    public static final String KEY_NAME = "name";
    public static final String KEY_IMAGES_DIR = "images_directory";
    public static final String KEY_CLOUD = "useCloud";

    private static final String[] CSV_HEADERS = {"image", "width", "height", "duration", "energy", "image_size"};
    private static final String CSV_EXT = ".csv";

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
        String fileName = inputData.getString(KEY_NAME) + CSV_EXT;
        boolean isCloud = inputData.getBoolean(KEY_CLOUD, false);
        String imagesDirPath = inputData.getString(KEY_IMAGES_DIR);

        File appFilesDir = getApplicationContext().getFilesDir();
        Log.i(TAG, "File directory: " + appFilesDir.getAbsolutePath());
        File imagesDir = new File(imagesDirPath);
        Log.i(TAG, "Starting data collection, " + iterations + " iterations");

        /*
         * Save csv to file.
         * File will be located under /data/data/pl.edu.agh.sm.project12
         * <p>
         * Example file content:
         * <p>
         * image,width,height,duration,energy,image_size
         * 2131165312,522,512,456000000,3.134947822E-5,756396
         * <p>
         * image - resource id
         * width and height - pixels
         * duration - nano sec
         * energy - mAh
         * image_size - bytes
         */
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(new File(appFilesDir, fileName).getAbsolutePath()));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))
        ) {
            int imgCounter = 0;
            for (File file : imagesDir.listFiles()) {
                Log.i(TAG, "file: " + file.getName());
                for (int i = 0; i < iterations; ++i) {
                    Log.i(TAG, "Data collection, iteration " + i);
                    List<String> record = performIteration(file, isCloud);
                    if (!record.isEmpty()) {
                        csvPrinter.printRecord(record);
                        csvPrinter.flush();
                    }
                }
                setProgressAsync(new Data.Builder().putInt(KEY_PROGRESS, ++imgCounter).build());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Result.success();
    }

    private final TextRecognitionOcr recognizer = new TextRecognitionOcr();
    private final TextRecognitionCloudOcr cloudRecognizer = new TextRecognitionCloudOcr(MainActivity.accessToken);

    private List<String> performIteration(File image, boolean useCloud) {
        List<String> results = new ArrayList<>(4);

        BatteryManager batteryManager = (BatteryManager) getApplicationContext().getSystemService(BATTERY_SERVICE);
        Duration samplingPeriod = Duration.ofMillis(50);
        BatteryConsumptionMonitor batteryConsumptionMonitor = new BatteryConsumptionMonitor(batteryManager, samplingPeriod);
        batteryConsumptionMonitor.setListener(new BatteryConsumptionListener() {
            @Override
            public void onSample(int currentMicro, long deltaNanos, double mAhSoFar) {

            }

            @Override
            public void onStop(double mAhTotal) {
                results.add(Double.toString(mAhTotal));
            }
        });
        batteryConsumptionMonitor.start();

        Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
        if (bitmap == null) {
            Log.w(TAG, "Couldn't decode file: " + image.getName() + " Ignoring...");
            return results;
        }
        results.add(image.getName());
        results.add(Integer.toString(bitmap.getWidth()));
        results.add(Integer.toString(bitmap.getHeight()));

        try {
            InputImage inputImage = InputImage.fromBitmap(bitmap, Surface.ROTATION_0);

            Instant start = Instant.now();

            CountDownLatch latch = new CountDownLatch(1);
            if (useCloud) {
                cloudRecognizer.performCloudVisionRequest(bitmap);
                Instant finish = Instant.now();
                Duration between = Duration.between(start, finish);
                Log.i(TAG, "Elapsed time: " + between.toString());
                results.add(Integer.toString(between.getNano()));
                latch.countDown();
            } else {
                Task<Text> process = recognizer.process(inputImage);

                process.addOnSuccessListener(visionText -> {
                    Instant finish = Instant.now();
                    Duration between = Duration.between(start, finish);
                    Log.i(TAG, "Elapsed time: " + between.toString());
                    results.add(Integer.toString(between.getNano()));
                    latch.countDown();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error while OCRing");
                    latch.countDown();
                });
            }
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            batteryConsumptionMonitor.stop();
        }

        // in bytes
        results.add("" + image.length());

        return results;
    }
}
