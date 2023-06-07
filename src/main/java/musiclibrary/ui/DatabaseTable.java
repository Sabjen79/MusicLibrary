package musiclibrary.ui;

import musiclibrary.database.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.sql.ResultSet;

public class DatabaseTable extends AbstractTableModel {
    private static final String[] columns = {"Song", "Artists", "Album", "Genres"};

    private String[][] table;

    public DatabaseTable() {
        table = new String[getRowCount()][getColumnCount()];

        table = DatabaseConnection.getINSTANCE().getAllSongs();
    }

    @Override
    public int getRowCount() {
        return DatabaseConnection.getINSTANCE().getSongCount();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int col) {
        return columns[col];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return table[rowIndex][columnIndex+1];
    }

    @Override
    public Class getColumnClass(int column) {
        return getValueAt(0, column).getClass();
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        table[row][col] = (String) value;
        fireTableCellUpdated(row, col);
    }

    public String[][] getTable() {
        return table;
    }
}


