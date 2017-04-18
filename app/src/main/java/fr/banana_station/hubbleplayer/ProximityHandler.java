package fr.banana_station.hubbleplayer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

class ProximityHandler implements SensorEventListener {

    /**
     * The sensor manager
     */
    private SensorManager sensorManager;
    /**
     * The proximity sensor
     */
    private Sensor proximity;

    /**
     * Acquisition state boolean
     */
    private boolean starting = false;
    /**
     * Boolean that describe if the user hand was close to the phone before
     */
    private boolean close = false;
    /**
     * Time since the hand of the user is close to the proximity sensor
     */
    private long time = 0;

    /**
     * @param sensorManager SensorManager
     */
    ProximityHandler(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        this.proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    /**
     * Start the sensor acquisition
     */
    void start() {
        starting = true;
        boolean supported = sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
        if (!supported) {
            proximityListener.onProximityNotSupported();
        }
    }

    /**
     * Stop the sensor acquisition
     */
    void stop() {
        starting = false;
        sensorManager.unregisterListener(this, proximity);
    }

    /**
     * Callback when sensor datas acquired
     *
     * @param sensorEvent SensorEvent
     */
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

    /**
     * Interface used to emit events in activities
     */
    interface ProximityListener {
        void onProximityDetected();

        void onLongProximityDetected();

        void onProximityNotSupported();
    }
}
