package com.don.pedometer;

import org.litepal.crud.DataSupport;

/**
 * <p>
 * Description：每日步数实体
 * </p>
 * <p>
 * date 2017/10/19
 */

public class StepData extends DataSupport {

    private int id;
    private String time;
    private int step;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
}
