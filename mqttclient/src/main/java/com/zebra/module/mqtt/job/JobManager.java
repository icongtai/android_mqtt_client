package com.zebra.module.mqtt.job;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.zebra.module.mqtt.client.StoreMqttConnectOptions;
import com.zebra.module.mqtt.db.MqttSqliteOpenHelper;
import com.zebra.module.mqtt.exception.ConsumerNotFoundException;
import com.zebra.module.mqtt.log.Logger;
import com.zebra.module.mqtt.service.MqttService;
import com.zebra.module.mqtt.service.connection.MqttConntion;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author xining
 * @date 2019/3/4
 */
public class JobManager {
    public static final String TAG = JobManager.class.getSimpleName();
    public MqttService mMqttService;
    public List<CycleJobGroup> jobGroupsInMem = Collections.synchronizedList(new ArrayList<CycleJobGroup>());
    private ScheduledExecutorService mExecutorService = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> futureTask;
    public Runnable jobExecute = new Runnable() {
        @Override
        public void run() {
            Logger.d(TAG, "job run......");
            try {
                if (jobGroupsInMem.size() > 0) {
                    if (!mMqttService.connectivityManager.isConnected()) {
                        //net work not connected
                        return;
                    }
                    Iterator<CycleJobGroup> it = jobGroupsInMem.iterator();
                    while (it.hasNext()) {
                        try {
                            CycleJobGroup cycleJobGroup = it.next();
                            ForeignCollection<CycleJob> jobs = cycleJobGroup.jobsForQuery;
                            if (jobs != null && jobs.size() > 0) {
                                loopJob(jobs);
                            }
                        } catch (Exception e) {
                            //CycleJobGroup
                            Logger.e(TAG, "CycleJobGroup run...... Exception end ", e);
                        }
                    }
                }
            } catch (Exception e) {
                //Task
                Logger.e(TAG, "job run...... Exception end ", e);
            }
            Logger.d(TAG, "job run...... end");
        }

        private void loopJob(ForeignCollection<CycleJob> jobs) throws SQLException {
            Iterator<CycleJob> cycleJobs = jobs.iterator();
            boolean breakGroup = false;
            while (cycleJobs.hasNext()) {
                CycleJob cycleJob = cycleJobs.next();
                cycleJob.refresh(mMqttService.getApplicationContext());
                MqttConntion conntion = mMqttService.getZebraMqttConntion(cycleJob.connectOptions.conntionUniqueId);
                IMqttConsumer mqttConsumer = cycleJob.getIMqttConsumer();
                if (mqttConsumer == null) {
                    mqttConsumer.onThrowable(new ConsumerNotFoundException(cycleJob));
                    break;
                }
                try {
                    if (conntion == null) {
                        Logger.d(TAG, "create new conntion");
                        conntion = MqttConntion.create(mMqttService, cycleJob.connectOptions.conntionUniqueId,cycleJob.connectOptions.clientId,cycleJob.connectOptions.broker);
                        mMqttService.addZebraMqttConntion(conntion);
                    }
                    Logger.d(TAG, "conntion status "+conntion.getStatus());
                    if (conntion.getStatus() == MqttConntion.Init || conntion.getStatus() == MqttConntion.UnConntioned) {
                        Logger.d(TAG, "connect");
                        conntion.connect(cycleJob.connectOptions.getMqttConnectOptions());
                    }
                } catch (MqttException e) {
                    mqttConsumer.onThrowable(e);
                    breakGroup = true;
                    Logger.e(TAG, "loop job error ", e);
                }
                if (breakGroup) {
                    break;
                }
                if (conntion != null && conntion.getStatus() == MqttConntion.Conntioned) {
                    if (mqttConsumer != null) {
                        try {
                            breakGroup = mqttConsumer.comsume(cycleJob, conntion);
                        } catch (Exception e) {
                            Logger.e(TAG, "comsume error", e);
                            breakGroup = true;
                        }
                        if (breakGroup) {
                            break;
                        }
                    }
                }
            }
        }
    };

    public JobManager(MqttService mMqttService) {
        this.mMqttService = mMqttService;
    }


    public void refresheCycleJob(Context context, String reason) {
        Logger.d(TAG, "refresheCycleJob " + reason);
        jobGroupsInMem.clear();
        restoreJobGroups(context);
        changeExecutorRate();
    }

    public static void createOrUpdateCycleJobGroupOffline(CycleJobGroup jobGroup, Context context) {
        try {
            jobGroup.createOrUpdate(context);
            for (CycleJob cycleJob : jobGroup.jobsForWrite) {
                cycleJob.createOrUpdate(context);
            }
        } catch (SQLException e) {
            Logger.e(TAG, "createOrUpdateCycleJobGroupOffline ", e);
        }
    }

    /**
     * todo 级联删除 connection_option?
     *
     * @param jobGroup
     * @param context
     */
    public static void deleteCycleJobGroupOffline(CycleJobGroup jobGroup, Context context) {
        try {
            Dao<CycleJobGroup, String> dao = MqttSqliteOpenHelper.getHelper(context).getDao(CycleJobGroup.class);
            dao.delete(jobGroup);
            CycleJob.clean(jobGroup, context);
        } catch (SQLException e) {
            Logger.e(TAG, "deleteCycleJobGroupOffline ", e);
        }
    }

    /**
     * 所有任务当中最大频率为基准
     *
     * @return
     */
    private long getMiniRate() {
        long delaySeconds = Integer.MAX_VALUE;
        if (jobGroupsInMem.size() > 0) {
            Iterator<CycleJobGroup> it = jobGroupsInMem.iterator();
            while (it.hasNext()) {
                CycleJobGroup entry = it.next();
                delaySeconds = Math.min(delaySeconds, entry.delaySeconds);
            }
        }
        return delaySeconds;
    }

    /**
     * 修改任务周期频率
     */
    private void changeExecutorRate() {
        long delay = getMiniRate();
        if (futureTask != null) {
            Logger.d(TAG, "cancel  all job");
            futureTask.cancel(true);
        }
        if (delay == Integer.MAX_VALUE) {
            Logger.d(TAG, "no job find");
            return;
        }
        Logger.d(TAG, "change job rate  to  " + delay);
        futureTask = mExecutorService.scheduleWithFixedDelay(jobExecute, 1, delay, TimeUnit.SECONDS);
    }

    /**
     * 加载离线任务
     *
     * @param context
     */
    private void restoreJobGroups(Context context) {
        List<CycleJobGroup> jobGroups = CycleJobGroup.queryCycleJobGroupOrderByPriority(context);
        if (jobGroups != null && jobGroups.size() > 0) {
            jobGroupsInMem.addAll(jobGroups);
        }
    }

    /**
     * 更新配置 一般用于密码修改
     *
     * @param options
     */
    public void updateConnectionOption(StoreMqttConnectOptions options, Context context) {
        if (options != null) {
            try {
                Logger.d(TAG, "updateConnectionOption");
                options.update(context);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
