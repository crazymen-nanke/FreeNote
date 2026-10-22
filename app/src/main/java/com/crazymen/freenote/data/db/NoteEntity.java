package com.crazymen.freenote.data.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** 记录主表 */
@Entity(tableName = "notes")
public class NoteEntity {

    @PrimaryKey
    public long id;

    public String title;

    public String body;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    /** 心情标签，可空 */
    public String mood;

    /** 普通标签，逗号分隔存储 */
    @ColumnInfo(name = "tags_csv")
    public String tagsCsv;
}
