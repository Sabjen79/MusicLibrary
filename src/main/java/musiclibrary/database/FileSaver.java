package musiclibrary.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import musiclibrary.ui.DatabaseTable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileSaver {
    public static void saveImage(String path, BufferedImage img) {
        if(!path.toLowerCase().endsWith(".jpg")) path += ".jpg";

        File file = new File(path);

        try {
            ImageIO.write(img, "jpg", file);
            openFile(path);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void saveFile(String path, int option, JTable table) {
        if(option == 0) {
            String filename = path.substring(path.lastIndexOf("\\") + 1);

            DatabaseConnection.getINSTANCE().saveToFile(path.substring(0, path.lastIndexOf("\\")), filename);
            openFile(path);
            return;
        }

        File file = new File(path);

        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) { throw new RuntimeException(e); }
        }

        try {
            FileWriter myWriter = new FileWriter(path, false);
            myWriter.write((option == 1) ? getXML(table) : getJSON(table));
            myWriter.flush();
            myWriter.close();

            openFile(path);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void openFile(String path) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try { Desktop.getDesktop().open(new File(path)); }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    private static String getXML(JTable t) {
        StringBuilder str = new StringBuilder();
        str.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        str.append("<root>\n");

        for(var item : SongTableObject.create(t)) {
            str.append("   <row>\n")
                    .append("      <song>").append(item.song).append("</song>\n")
                    .append("      <artists>").append(item.artists).append("</artists>\n")
                    .append("      <album>").append(item.album).append("</album>\n")
                    .append("      <genres>").append((item.genres == null) ? "" : item.genres).append("</genres>\n")
               .append("   </row>\n");
        }

        str.append("</root>\n");


        return str.toString();
    }

    private static String getJSON(JTable t) {
        String str = "";

        SongTableObject[] array = SongTableObject.create(t);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        str = gson.toJson(array);
        System.out.println(str);
        return str;
    }
}
