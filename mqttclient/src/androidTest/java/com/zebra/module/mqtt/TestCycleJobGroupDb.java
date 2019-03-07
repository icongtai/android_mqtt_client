package com.zebra.module.mqtt;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.zebra.module.mqtt.client.StoreMqttConnectOptions;
import com.zebra.module.mqtt.db.MqttSqliteOpenHelper;
import com.zebra.module.mqtt.job.CycleJob;
import com.zebra.module.mqtt.job.CycleJobGroup;
import com.zebra.module.mqtt.job.JobManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.sql.SQLException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

/**
 * TestCycleJobGroupDb 数据库测试
 *
 * @author xining
 * @date 2019/3/6
 */
@RunWith(AndroidJUnit4.class)
public class TestCycleJobGroupDb {

    @Before
    public void setup() {
        Context appContext = ApplicationProvider.getApplicationContext();
    }

    public void cleanDb() {
        Context appContext = ApplicationProvider.getApplicationContext();
        File file = new File(appContext.getDatabasePath(MqttSqliteOpenHelper.DB_NAME).getAbsolutePath());
        file.delete();
    }

    @Test
    public void testAdd() throws SQLException {
        cleanDb();
        CycleJobGroup group = getCycleJobGroup(ZebraMqttClientTest.broker);
        Context appContext = ApplicationProvider.getApplicationContext();
        JobManager.createOrUpdateCycleJobGroupOffline(group, appContext);
        //有ZebraStoreMqttConnectOptions
        Dao<StoreMqttConnectOptions, String> daoZebraStoreMqttConnectOptions = MqttSqliteOpenHelper.getHelper(appContext).getDao(StoreMqttConnectOptions.class);
        Assert.assertTrue(daoZebraStoreMqttConnectOptions.countOf() == 1);
        //有CycleJobGroup
        Dao<CycleJobGroup, String> daoCycleJobGroup = MqttSqliteOpenHelper.getHelper(appContext).getDao(CycleJobGroup.class);
        Assert.assertTrue(daoCycleJobGroup.countOf() == 1);
        //有CycleJob
        Dao<CycleJob, String> daoCycleJob = MqttSqliteOpenHelper.getHelper(appContext).getDao(CycleJob.class);
        QueryBuilder<CycleJob, String> queryBuilder = daoCycleJob.queryBuilder();
        Assert.assertTrue(queryBuilder.countOf() == 1);
    }

    @Test
    public void testDel() throws SQLException {
        Context appContext = ApplicationProvider.getApplicationContext();
        CycleJobGroup group = getCycleJobGroup(ZebraMqttClientTest.broker);
        JobManager.deleteCycleJobGroupOffline(group, appContext);
        //有ZebraStoreMqttConnectOptions
        Dao<StoreMqttConnectOptions, String> daoZebraStoreMqttConnectOptions = MqttSqliteOpenHelper.getHelper(appContext).getDao(StoreMqttConnectOptions.class);
        Assert.assertTrue(daoZebraStoreMqttConnectOptions.countOf() == 1);
        //不存在CycleJobGroup
        Dao<CycleJobGroup, String> daoCycleJobGroup = MqttSqliteOpenHelper.getHelper(appContext).getDao(CycleJobGroup.class);
        Assert.assertTrue(daoCycleJobGroup.countOf() == 0);
        //不存在CycleJob
        Dao<CycleJob, String> daoCycleJob = MqttSqliteOpenHelper.getHelper(appContext).getDao(CycleJob.class);
        QueryBuilder<CycleJob, String> queryBuilder = daoCycleJob.queryBuilder();
        queryBuilder.where().eq("connectOptions_id", group.uniqueId);
        Assert.assertTrue(queryBuilder.countOf() == 0);
    }

    public CycleJobGroup getCycleJobGroup(String broker) {
        CycleJobGroup jobGroup = CycleJobGroup.getCycleJobGroup("DcbMqttManager_Driver", 1, 10);
        CycleJob cycleJob = CycleJob.create(EmptyMqttConsumer.class, ZebraMqttClientTest.getZebraStoreMqttConnectOptions(broker));
        jobGroup.addJob(cycleJob);
        return jobGroup;
    }
}
