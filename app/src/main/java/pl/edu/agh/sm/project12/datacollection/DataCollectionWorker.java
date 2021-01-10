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
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import pl.edu.agh.sm.project12.MainActivity;
import pl.edu.agh.sm.project12.battery.BatteryConsumptionListener;
import pl.edu.agh.sm.project12.battery.BatteryConsumptionMonitor;
import pl.edu.agh.sm.project12.cloudocr.TextRecognitionCloudOcr;
import pl.edu.agh.sm.project12.ocr.TextRecognitionOcr;

import static android.content.Context.BATTERY_SERVICE;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;


public class DataCollectionWorker extends Worker {
    private static final String TAG = DataCollectionWorker.class.getSimpleName();

    public static final String KEY_ITERATIONS = "iterations";
    public static final String KEY_PROGRESS = "progress";
    public static final String KEY_NAME = "name";
    public static final String KEY_IMAGES_DIR = "images_directory";
    public static final String KEY_CLOUD = "useCloud";
    public static final String KEY_PROCESSING_METHOD = "processingMethod";

    private static final String[] CSV_HEADERS = {
            "image", // file name
            "width", // image width (px)
            "height", // image height (px)
            "duration", // duration (nanos)
            "energy", // battery consumption (mAh)
            "image_size", // image size (bytes)
            "cloud", // whether it's cloud (boolean)
    };
    private static final String CSV_EXT = ".csv";

    private final WorkerParameters workerParams;
    private int iterationCounter = 0;

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
        int processingMethod = inputData.getInt(KEY_PROCESSING_METHOD, 0);
        String imagesDirPath = inputData.getString(KEY_IMAGES_DIR);

        File appFilesDir = getApplicationContext().getFilesDir();
        Log.i(TAG, "File directory: " + appFilesDir.getAbsolutePath());
        File imagesDir = new File(imagesDirPath);
        Log.i(TAG, "Starting data collection, " + iterations + " iterations");

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(new File(appFilesDir, fileName).getAbsolutePath()));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {
            iterationCounter = 0;
            for (File file : imagesDir.listFiles()) {
                if (processingMethod == 0) {
                    collectDataForFile(iterations, false, csvPrinter, file);
                    collectDataForFile(iterations, true, csvPrinter, file);
                } else if (processingMethod == 1) {
                    collectDataForFile(iterations, getRandomBoolean(), csvPrinter, file);
                } else if (processingMethod == 2) {
                    collectDataForFile(iterations, getNeuralNetworkChoice(file), csvPrinter, file);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Result.success();
    }

    private void collectDataForFile(int iterations, boolean isCloud, CSVPrinter csvPrinter, File file) throws IOException {
        Log.i(TAG, "file: " + file.getName() + ", cloud: " + isCloud);
        for (int i = 0; i < iterations; ++i) {
            Log.i(TAG, "Data collection, iteration " + i);
            List<String> record = performIteration(file, isCloud);
            if (!record.isEmpty()) {
                csvPrinter.printRecord(record);
                csvPrinter.flush();
            }
            setProgressAsync(new Data.Builder().putInt(KEY_PROGRESS, ++iterationCounter).build());
        }
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

            if (useCloud) {
                CountDownLatch latch = new CountDownLatch(1);
                cloudRecognizer.performCloudVisionRequest(bitmap, latch::countDown);
                latch.await();
            } else {
                Task<Text> process = recognizer.process(inputImage);
                CountDownLatch latch = new CountDownLatch(1);
                process.addOnSuccessListener(visionText -> {
                    latch.countDown();
                }).addOnFailureListener(e -> {
                    latch.countDown();
                });
                latch.await();
            }

            Instant finish = Instant.now();
            Duration between = Duration.between(start, finish);
            Log.i(TAG, "Elapsed time: " + between.toString());
            results.add(Integer.toString(between.getNano()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            batteryConsumptionMonitor.stop();
        }

        // in bytes
        results.add("" + image.length());
        results.add("" + useCloud);

        return results;
    }

    private boolean getRandomBoolean() {
        Random random = new Random();
        return random.nextBoolean();
    }

    private boolean getNeuralNetworkChoice(File file){

        Log.w(TAG, "Run Neural Network for: " + file.getName());

        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        String width = Integer.toString(bitmap.getWidth());
        String height = Integer.toString(bitmap.getHeight());
        String image_size = Long.toString(file.length());


        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("https://us-central1-optimizingenergyconsumption.cloudfunctions.net/startInCloudPredictor");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("width", width));
        params.add(new BasicNameValuePair("height", height));
        params.add(new BasicNameValuePair("image_size", image_size));
        params.add(new BasicNameValuePair("wifi", "1.0"));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity respEntity = response.getEntity();

            if (respEntity != null) {
                String content =  EntityUtils.toString(respEntity);
                return content.equals("true");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
