package com.crazymen.freenote.data.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 记录配图表：一条记录可以有多张图。
 * 只保存文件名，真实图片经压缩后存放在应用私有目录（见 PhotoStore）。
 * 记录删除时，配图随外键级联删除。
 */
@Entity(
        tableName = "note_photos",
        foreignKeys = @ForeignKey(
                entity = NoteEntity.class,
                parentColumns = "id",
                childColumns = "note_id",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("note_id"))
public class NotePhotoEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "note_id")
    public long noteId;

    @ColumnInfo(name = "file_name")
    public String fileName;

    /** 同一条记录内的展示顺序，从 0 开始 */
    public int position;
}
