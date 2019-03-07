package com.zebra.module.mqtt.job;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.DatabaseTable;
import com.zebra.module.mqtt.db.MqttSqliteOpenHelper;
import com.zebra.module.mqtt.log.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 周期任务集合
 *
 * @author xining
 * @date 2019/3/4
 */
@DatabaseTable(tableName = "cycle_job_group")
public class CycleJobGroup {
    public static final String TAG = CycleJobGroup.class.getSimpleName();
    /**
     * 全局唯一
     */
    @DatabaseField(id = true)
    public String uniqueId;
    /**
     * 优先级
     */
    @DatabaseField
    public int priority = 1;

    /**
     * 任务延迟时间
     */
    @DatabaseField
    public long delaySeconds = 5;

    /**
     * 任务集合,只读
     */
    @ForeignCollectionField(eager = true)
    public ForeignCollection<CycleJob> jobsForQuery;

    /***
     * 任务集合，写
     */
    public List<CycleJob> jobsForWrite;

    public static CycleJobGroup getCycleJobGroup(String uniqueId, int priority, long delaySeconds) {
        CycleJobGroup jobGroup = new CycleJobGroup();
        jobGroup.uniqueId = uniqueId;
        jobGroup.priority = priority;
        jobGroup.delaySeconds = delaySeconds;
        return jobGroup;
    }

    /**
     * 按priority优先级排序
     * @param context
     * @return
     */
    public static List<CycleJobGroup> queryCycleJobGroupOrderByPriority(Context context) {
        List<CycleJobGroup> result = null;
        try {
            Dao<CycleJobGroup, String> dao = MqttSqliteOpenHelper.getHelper(context).getDao(CycleJobGroup.class);
            QueryBuilder queryBuilder = dao.queryBuilder();
            queryBuilder.orderBy("priority", true);
            result = queryBuilder.query();
        } catch (SQLException e) {
            Logger.e(TAG, "queryCycleJobGroupOrderByPriority", e);
        }
        return result;
    }

    public void addJob(CycleJob cycleJob) {
        cycleJob.cycleJobGroup = this;
        if (jobsForWrite == null) {
            jobsForWrite = new ArrayList<>();
        }
        jobsForWrite.add(cycleJob);
    }

    public void createOrUpdate(Context context) throws SQLException {
        Dao<CycleJobGroup, String> daoCycleJobGroup = MqttSqliteOpenHelper.getHelper(context).getDao(CycleJobGroup.class);
        daoCycleJobGroup.createOrUpdate(this);
    }
}
