package musiclibrary.database;

import musiclibrary.ui.DatabaseTable;

import javax.swing.*;
import javax.swing.table.TableModel;

public class SongTableObject {
    public String song, artists, album, genres;

    public SongTableObject(String song, String artists, String album, String genres) {
        this.song = song;
        this.artists = artists;
        this.album = album;
        this.genres = genres;
    }

    public static SongTableObject[] create(JTable t) {
        TableModel model = t.getModel();

        String[][] table = new String[t.getRowCount()][4];
        for(int i = 0; i < t.getRowCount(); i++) {
            for(int j = 0; j < 4; j++) {
                table[i][j] = (String) model.getValueAt(t.convertRowIndexToModel(i), j);
            }
        }
        SongTableObject[] array = new SongTableObject[table.length];

        for(int i = 0; i < array.length; i++) {
            array[i] = new SongTableObject(table[i][0], table[i][1], table[i][2], table[i][3]);
        }

        return array;
    }
}
