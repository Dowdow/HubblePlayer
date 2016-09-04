package fr.banana_station.hubbleplayer;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class SongFinder {

    private ContentResolver contentResolver;
    private Cursor cursor;

    public SongFinder(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public List<Song> find() {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        final String[] projection = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST};
        final String sortOrder = MediaStore.Audio.AudioColumns.TITLE + " COLLATE LOCALIZED ASC";

        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        cursor = contentResolver.query(uri, projection, selection, null, sortOrder);

        List<Song> songList = new ArrayList<>();

        if (cursor != null) {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                songList.add(new Song(cursor.getLong(0),cursor.getString(1),cursor.getString(2)));
                cursor.moveToNext();
            }
        }

        return songList;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public void closeCursor() {
        if (cursor != null) {
            cursor.close();
        }
    }

}