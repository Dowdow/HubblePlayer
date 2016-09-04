package fr.banana_station.hubbleplayer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ProximityHandler implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor proximity;

    private boolean starting = false;
    private boolean close = false;

    public ProximityHandler(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        this.proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    public void start() {
        starting = true;
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        starting = false;
        sensorManager.unregisterListener(this, proximity);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float cm = sensorEvent.values[0];
            //System.out.println(cm);
            if (cm == 0 && !close) {
                close  = true;
            }
            if (cm >= 1 && close) {
                proximityListener.onProximityDetected();
                close = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public boolean isStarting() {
        return starting;
    }

    private ProximityListener proximityListener;

    public void setProximityListener(ProximityListener proximityListener) {
        this.proximityListener = proximityListener;
    }

    public interface ProximityListener {
        void onProximityDetected();
    }
}
