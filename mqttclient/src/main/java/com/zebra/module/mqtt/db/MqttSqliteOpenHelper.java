package com.zebra.module.mqtt.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.zebra.module.mqtt.client.StoreMqttConnectOptions;
import com.zebra.module.mqtt.job.CycleJob;
import com.zebra.module.mqtt.job.CycleJobGroup;

import java.sql.SQLException;

/**
 * 配置保存,用于后台任务
 *
 * @author xining
 * @date 2019/3/4
 */
public class MqttSqliteOpenHelper extends OrmLiteSqliteOpenHelper {
    public static final String DB_NAME = "zebra_mqtt.db";
    public static final int VERSION = 18;
    public static MqttSqliteOpenHelper helper;

    public MqttSqliteOpenHelper(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion) {
        super(context, databaseName, factory, databaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, StoreMqttConnectOptions.class);
            TableUtils.createTableIfNotExists(connectionSource, CycleJob.class);
            TableUtils.createTableIfNotExists(connectionSource, CycleJobGroup.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {

    }

    public static MqttSqliteOpenHelper getHelper(Context context) {
        if (helper == null) {
            synchronized (MqttSqliteOpenHelper.class) {
                if (helper == null) {
                    helper = new MqttSqliteOpenHelper(context, DB_NAME, null, VERSION);
                }
            }
        }
        if (!helper.isOpen()) {
            releaseHelper();
            return getHelper(context);
        }
        return helper;
    }

    public static synchronized void releaseHelper() {
        if (helper != null) {
            helper.close();
            helper = null;
        }
    }
}
