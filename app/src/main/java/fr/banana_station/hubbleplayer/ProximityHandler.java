package fr.banana_station.hubbleplayer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

class ProximityHandler implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor proximity;

    private boolean starting = false;
    private boolean close = false;
    private long time = 0;

    ProximityHandler(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        this.proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    void start() {
        starting = true;
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    void stop() {
        starting = false;
        sensorManager.unregisterListener(this, proximity);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float cm = sensorEvent.values[0];
            if (cm == 0 && !close) {
                time = System.currentTimeMillis();
                close = true;
            }
            if (cm >= 1 && close) {
                if (System.currentTimeMillis() - time > 1500) {
                    proximityListener.onLongProximityDetected();
                } else {
                    proximityListener.onProximityDetected();
                }
                close = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    boolean isStarting() {
        return starting;
    }

    private ProximityListener proximityListener;

    void setProximityListener(ProximityListener proximityListener) {
        this.proximityListener = proximityListener;
    }

    interface ProximityListener {
        void onProximityDetected();

        void onLongProximityDetected();
    }
}
