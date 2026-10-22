package com.crazymen.freenote.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

/** 记录与配图的数据访问 */
@Dao
public interface NoteDao {

    // ------------------------------------------------------------ 查询

    @Transaction
    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    List<NoteWithPhotos> all();

    @Transaction
    @Query("SELECT * FROM notes WHERE created_at >= :from AND created_at < :to ORDER BY created_at DESC")
    List<NoteWithPhotos> between(long from, long to);

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    NoteWithPhotos byId(long id);

    /** 关键词搜索：标题 / 正文 / 标签，忽略顺序，按时间倒序 */
    @Transaction
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :q || '%' ESCAPE '\\'"
            + " OR body LIKE '%' || :q || '%' ESCAPE '\\'"
            + " OR tags_csv LIKE '%' || :q || '%' ESCAPE '\\'"
            + " ORDER BY created_at DESC")
    List<NoteWithPhotos> search(String q);

    @Query("SELECT COUNT(*) FROM notes")
    int count();

    @Query("SELECT body FROM notes")
    List<String> allBodies();

    @Query("SELECT body FROM notes WHERE created_at >= :from")
    List<String> bodiesSince(long from);

    @Query("SELECT created_at FROM notes")
    List<Long> allCreatedAt();

    @Query("SELECT created_at FROM notes WHERE created_at >= :from")
    List<Long> createdAtSince(long from);

    @Query("SELECT created_at FROM notes WHERE created_at >= :from AND created_at < :to")
    List<Long> createdAtBetween(long from, long to);

    @Query("SELECT tags_csv FROM notes WHERE created_at >= :since")
    List<String> tagsSince(long since);

    @Query("SELECT mood FROM notes WHERE created_at >= :since AND mood IS NOT NULL AND mood <> ''")
    List<String> moodsSince(long since);

    // ------------------------------------------------------------ 写入

    @Insert
    void insert(NoteEntity note);

    @Update
    int update(NoteEntity note);

    @Query("DELETE FROM notes WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM notes")
    void clear();

    // ------------------------------------------------------------ 配图

    @Insert
    void insertPhotos(List<NotePhotoEntity> photos);

    @Query("SELECT file_name FROM note_photos WHERE note_id = :noteId ORDER BY position")
    List<String> photoFilesOf(long noteId);

    @Query("DELETE FROM note_photos WHERE note_id = :noteId")
    void deletePhotosOf(long noteId);

    @Query("SELECT file_name FROM note_photos")
    List<String> allPhotoFiles();
}
