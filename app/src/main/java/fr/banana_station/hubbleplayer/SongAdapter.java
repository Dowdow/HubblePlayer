package fr.banana_station.hubbleplayer;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.List;

class SongAdapter extends BaseAdapter implements SectionIndexer {

    private List<Song> songList;
    private LayoutInflater layoutInflater;
    private AlphabetIndexer alphabetIndexer;


    SongAdapter(Context c, List<Song> songList, Cursor cursor) {
        this.layoutInflater = LayoutInflater.from(c);
        this.songList = songList;
        this.alphabetIndexer = new AlphabetIndexer(cursor, 1, " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Override
    public int getCount() {
        return songList.size();
    }

    @Override
    public Object getItem(int i) {
        return songList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return songList.get(i).getId();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;

        if (view == null) {
            view = layoutInflater.inflate(R.layout.song, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) view.findViewById(R.id.songTitle);
            viewHolder.artist = (TextView) view.findViewById(R.id.songArtist);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        Song song = songList.get(i);
        viewHolder.title.setText(song.getTitle());
        viewHolder.artist.setText(song.getArtist());
        view.setId(i);

        return view;
    }

    @Override
    public Object[] getSections() {
        return alphabetIndexer.getSections();
    }

    @Override
    public int getPositionForSection(int i) {
        return alphabetIndexer.getPositionForSection(i);
    }

    @Override
    public int getSectionForPosition(int i) {
        return alphabetIndexer.getSectionForPosition(i);
    }

    private static class ViewHolder {
        TextView title;
        TextView artist;
    }
}
