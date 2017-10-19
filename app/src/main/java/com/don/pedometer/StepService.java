package com.don.pedometer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;
import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class StepService extends Service implements StepCallBack {

    private static final long SCREEN_OFF_RECEIVER_DELAY = 500l;

    //默认为30秒进行一次存储
    private static int duration = 30000;

    private BroadcastReceiver mBatInfoReceiver;

    //保存到数据库的定时器
    private TimeCount time;

    @Override
    public void onCreate() {
        super.onCreate();
        initBroadcastReceiver();
        startStep();
        startTimeCount();

        //发送当前步数的定时器
        Timer postEventTimer = new Timer(true);
        TimerTask task = new TimerTask() {
            public void run() {
                EventBus.getDefault().post(StepMode.CURRENT_SETP + "");
            }
        };
        postEventTimer.schedule(task, 0, 500);

        //LitePal初始化
        LitePal.initialize(this);
        SQLiteDatabase db = Connector.getDatabase();
    }

    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        // 屏幕灭屏广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //日期修改
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        //关机广播
        filter.addAction(Intent.ACTION_SHUTDOWN);
        // 屏幕亮屏广播
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 屏幕解锁广播
        filter.addAction(Intent.ACTION_USER_PRESENT);
        // 当长按电源键弹出“关机”对话或者锁屏时系统会发出这个广播
        // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者“关机”对话框之上，
        // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();

                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    //改为60秒一存储
                    duration = 60000;
                    //解决某些厂商的rom在锁屏后收不到sensor的回调
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            startStep();
                        }
                    };
                    new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    save();
                    //改为30秒一存储
                    duration = 30000;
                } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                    //保存一次
                    save();
                } else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                    save();
                } else if (Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {
                    //时间改变监听
                    initTodayData();
                    Step(StepMode.CURRENT_SETP);
                }
            }
        };
        registerReceiver(mBatInfoReceiver, filter);
    }

    /**
     * 开始计步，判断使用加速度传感器或者计步传感器（4.4以上有些手机有，有些手机没有）
     */
    private void startStep() {
        StepMode mode = new StepInPedometer(this, this);
        boolean isAvailable = mode.getStep();
        if (!isAvailable) {
            mode = new StepInAcceleration(this, this);
            isAvailable = mode.getStep();
            if (isAvailable) {
                Log.i("MyLog", "acceleration can execute!");
            }
        }
    }

    /**
     * 开始保存到数据库计时器
     */
    private void startTimeCount() {
        time = new TimeCount(duration, 1000);
        time.start();
    }

    @Override
    public void Step(int stepNum) {
        StepMode.CURRENT_SETP = stepNum;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initTodayData();
        return START_STICKY;
    }

    /**
     * 获取当前日期的步数
     */
    private void initTodayData() {
        //获取当天的数据，用于展示
        List<StepData> list = DataSupport.where("time=?", getTodayDate()).find(StepData.class);
        if (list == null || list.size() == 0) {
            StepMode.CURRENT_SETP = 0;
        } else if (list.size() == 1) {
            StepMode.CURRENT_SETP = list.get(0).getStep();
        }
    }

    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            //计时完成，保存数据到数据库并新开一个计数器
            time.cancel();
            save();
            startTimeCount();
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }
    }

    private void save() {
        int tempStep = StepMode.CURRENT_SETP;
        List<StepData> list = DataSupport.where("time=?", getTodayDate()).find(StepData.class);
        if (list == null || list.size() == 0) {//新增
            StepData data = new StepData();
            data.setTime(getTodayDate());
            data.setStep(tempStep);
            data.save();
        } else if (list.size() == 1) {//修改
            ContentValues values = new ContentValues();
            values.put("step", tempStep);
            DataSupport.update(StepData.class, values, list.get(0).getId());
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBatInfoReceiver);
        Intent intent = new Intent(this, StepService.class);
        startService(intent);
        super.onDestroy();
    }
}
