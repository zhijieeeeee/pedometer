package com.don.pedometer;

import android.content.Context;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * 计步模式分为 加速度传感器 google内置计步器
 * <p/>
 * Created by base on 2016/8/17.
 */
public abstract class StepMode implements SensorEventListener {
    private Context context;
    public StepCallBack stepCallBack;
    public SensorManager sensorManager;
    public static int CURRENT_SETP = 0;
    public boolean isAvailable = false;

    public StepMode(Context context, StepCallBack stepCallBack) {
        this.context = context;
        this.stepCallBack = stepCallBack;
    }

    public boolean getStep() {
        prepareSensorManager();
        registerSensor();
        return isAvailable;
    }

    protected abstract void registerSensor();

    private void prepareSensorManager() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
        }
        sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
    }


}
