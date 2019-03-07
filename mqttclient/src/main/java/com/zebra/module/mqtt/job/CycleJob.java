package com.zebra.module.mqtt.job;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.DatabaseTable;
import com.zebra.module.mqtt.client.StoreMqttConnectOptions;
import com.zebra.module.mqtt.db.MqttSqliteOpenHelper;

import java.sql.SQLException;

/**
 * 周期任务
 *
 * @author xining
 * @date 2019/3/4
 */
@DatabaseTable(tableName = "cycle_job")
public class CycleJob {
    /**
     * job所属的group
     */
    @DatabaseField(foreign = true)
    public CycleJobGroup cycleJobGroup;
    //
    @DatabaseField(id = true)
    public String consumerClazz;
    /**
     * job的mqtt通道信息
     */
    @DatabaseField(foreign = true)
    public StoreMqttConnectOptions connectOptions;

    public CycleJob() {

    }

    public static void clean(CycleJobGroup jobGroup,Context context) throws SQLException {
        Dao<CycleJob, String> dao = MqttSqliteOpenHelper.getHelper(context).getDao(CycleJob.class);
        DeleteBuilder<CycleJob, String> deleteBuilder = dao.deleteBuilder();
        deleteBuilder.where().eq("cycleJobGroup_id",jobGroup.uniqueId);
        deleteBuilder.delete();
    }

    public void refresh(Context context) throws SQLException {
        connectOptions.refresh(context);
    }

    public static CycleJob create(Class<? extends IMqttConsumer> sourceClazz, StoreMqttConnectOptions options) {
        CycleJob job = new CycleJob();
        job.consumerClazz = sourceClazz.getName();
        job.connectOptions = options;
        return job;
    }

    public IMqttConsumer iJobSource;

    public IMqttConsumer getIMqttConsumer() {
        if (iJobSource == null) {
            iJobSource = createIMqttConsumer();
        }
        return iJobSource;
    }

    private IMqttConsumer createIMqttConsumer() {
        IMqttConsumer source = null;
        try {
            Class clazz = Class.forName(consumerClazz);
            Object object = clazz.newInstance();
            source = (IMqttConsumer) object;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return source;
    }

    public void createOrUpdate(Context context) throws SQLException {
        this.connectOptions.createOrUpdate(context);
        Dao<CycleJob, String> dao = MqttSqliteOpenHelper.getHelper(context).getDao(CycleJob.class);
        dao.createOrUpdate(this);
    }
}
