package com.crazymen.freenote.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条记录（内存模型，映射到 Room 表 notes / note_photos）。
 * mood 为心情标签（可空），tags 为普通标签，photos 为配图文件名（按顺序）。
 */
public class Note {

    public long id;
    public String title = "";
    public String body = "";
    public long createdAt;
    public String mood;
    public List<String> tags = new ArrayList<>();
    /** 配图文件名列表，仅存文件名，真实文件在应用私有目录 */
    public List<String> photos = new ArrayList<>();

    public Note() {
    }

    public boolean hasPhoto() {
        return photos != null && !photos.isEmpty();
    }

    /** 封面图（列表页只展示第一张） */
    public String coverPhoto() {
        return hasPhoto() ? photos.get(0) : null;
    }

    /** 列表里展示的标题：优先手写标题，否则取正文首行 */
    public String displayTitle() {
        if (title != null && title.trim().length() > 0) {
            return title.trim();
        }
        String first = firstLine();
        return first.length() > 18 ? first.substring(0, 18) + "…" : first;
    }

    public String firstLine() {
        if (body == null) {
            return "";
        }
        String[] lines = body.split("\n");
        for (String line : lines) {
            if (line.trim().length() > 0) {
                return line.trim();
            }
        }
        return "";
    }

    public int wordCount() {
        return body == null ? 0 : body.replaceAll("\\s", "").length();
    }
}
