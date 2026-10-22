package com.crazymen.freenote.data.db;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/** 记录 + 配图的组合查询结果 */
public class NoteWithPhotos {

    @Embedded
    public NoteEntity note;

    @Relation(parentColumn = "id", entityColumn = "note_id")
    public List<NotePhotoEntity> photos;
}
