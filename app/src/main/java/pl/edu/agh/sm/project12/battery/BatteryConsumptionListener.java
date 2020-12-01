package pl.edu.agh.sm.project12.battery;

public interface BatteryConsumptionListener {
    void onSample(int currentMicro, long deltaNanos, double mAhSoFar);

    void onStop(double mAhTotal);
}
