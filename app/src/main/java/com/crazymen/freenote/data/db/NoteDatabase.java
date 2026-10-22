package com.crazymen.freenote.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** 应用数据库。数据量小、写入频率低，允许主线程访问以保持界面同步逻辑简单。 */
@Database(
        entities = {NoteEntity.class, NotePhotoEntity.class},
        version = 1,
        exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {

    private static final String DB_NAME = "freenote.db";

    private static volatile NoteDatabase instance;

    public abstract NoteDao noteDao();

    public static NoteDatabase get(Context context) {
        if (instance == null) {
            synchronized (NoteDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    NoteDatabase.class,
                                    DB_NAME)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }

    /** 测试专用：关闭连接并丢弃单例，让下一个用例拿到全新数据库 */
    public static synchronized void resetForTest() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
