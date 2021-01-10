package pl.edu.agh.sm.project12.battery;

import android.os.BatteryManager;
import android.util.Log;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BatteryConsumptionMonitor {
    private static final String TAG = "BatteryConsumptionMonitor";
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    private final BatteryManager batteryManager;
    private final Duration samplingPeriod;

    private double mAh;
    private long lastMeasure;

    private BatteryConsumptionListener listener = null;
    private ScheduledFuture<?> scheduled;

    public BatteryConsumptionMonitor(BatteryManager batteryManager, Duration samplingPeriod) {
        super();
        this.batteryManager = batteryManager;
        this.samplingPeriod = samplingPeriod;
    }

    public void setListener(BatteryConsumptionListener listener) {
        this.listener = listener;
    }

    public synchronized void start() {
        if (scheduled != null) {
            throw new IllegalArgumentException();
        }
        lastMeasure = System.nanoTime();
        mAh = 0d;
        scheduled = executor.scheduleAtFixedRate(this::sample, 0, samplingPeriod.toNanos(), TimeUnit.NANOSECONDS);
    }

    public synchronized void stop() {
        if (scheduled == null) {
            throw new IllegalArgumentException();
        }
        scheduled.cancel(false);
        scheduled = null;

        if (listener != null) {
            listener.onStop(mAh);
        }
    }

    private void sample() {
        int currentMicro = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        long measure = System.nanoTime();
        double currentMilli = currentMicro / 1000D;
        long deltaNanos = measure - lastMeasure;
        double deltaHours = deltaNanos / (double) Duration.ofHours(1).toNanos();

        if (deltaHours < 0) {
            throw new AssertionError("Time went backwards");
        }

        lastMeasure = measure;
        mAh += currentMilli * deltaHours;

        if (listener != null) {
            listener.onSample(currentMicro, deltaNanos, mAh);
        }
    }
}
