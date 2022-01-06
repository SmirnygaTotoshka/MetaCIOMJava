package ru.smirnygatotoshka.MetaCIOM;

import javafx.scene.control.Tab;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.smirnygatotoshka.MetaCIOM.io.CsvIO;
import ru.smirnygatotoshka.MetaCIOM.io.ExcelIO;
import ru.smirnygatotoshka.MetaCIOM.io.GoogleDriveIO;
import ru.smirnygatotoshka.MetaCIOM.io.TableIO;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Metadata {



    public enum OutputFormats {
        csv,
        xlsx,
        google
    }

    public enum Mode{
        CREATE,
        CALCULATE
    }

    private Mode mode;
    private String pathToInputFile;
    private String pathToOutputDirectory;
    private boolean saveFreeAnswers;
    private OutputFormats format;
    private Table allTable;
    private JSONArray descQuestions;

    private Metadata(Mode mode, String pathToInputFile, String pathToOutputDirectory, boolean saveFreeAnswers, OutputFormats format, Table allTable,JSONArray a) {
        this.mode = mode;
        this.pathToInputFile = pathToInputFile;
        this.pathToOutputDirectory = pathToOutputDirectory;
        this.saveFreeAnswers = saveFreeAnswers;
        this.format = format;
        this.allTable = allTable;
        this.allTable.removeColumns(0);//delete column with fill date
        this.allTable.dropDuplicateRows();
        this.descQuestions = a;
    }

    public static Metadata parse(String pathToJSON) throws IOException {
        JSONObject metadata = new JSONObject(getJSONString(pathToJSON));
        Mode m = Mode.valueOf(metadata.getString("mode").toUpperCase());
        String input = metadata.getString("input");
        String output = metadata.getString("output");
        boolean saveFreeAnswers = metadata.getBoolean("save_free_answers");
        OutputFormats format = OutputFormats.valueOf(metadata.getString("output_format").toLowerCase());
        Table tbl = getTableIO(input).readTable(input);
        JSONArray q = metadata.getJSONArray("questions");
        return new Metadata(m,input,output,saveFreeAnswers,format,tbl,q);
    }

    public static TableIO getTableIO(String path){
        try {
            URL url = new URL(path);
            if (url.getProtocol().contentEquals("https") &&
                    url.getAuthority().contains("google.com"))
                return new GoogleDriveIO();
            else
                throw new IllegalArgumentException("Предложенная ссылка не принадлежит Google.");
        }
        catch (MalformedURLException e) {
            String ext = path.substring(path.lastIndexOf('.'));
            if (ext.contentEquals(".csv"))
                return new CsvIO();
            else if (ext.contentEquals(".xlsx"))
                return new ExcelIO();
            else throw new IllegalArgumentException("Неизвестный тип файла с таблицей. " +
                        "Умею работать с csv,xlsx и гугл-таблицами.");
        }
    }

    public TableIO getTableIO(){
        switch (format){
            case csv:
                return new CsvIO();
            case xlsx:
                return new ExcelIO();
            case google:
                try {
                    URL url = new URL(pathToOutputDirectory);
                    if (url.getProtocol().contentEquals("https") &&
                            url.getAuthority().contains("google.com"))
                        return new GoogleDriveIO();
                    else
                        throw new IllegalArgumentException("Предложенная ссылка не принадлежит Google.");
                }
                catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Нельзя создать гугл-таблицу вне гугл-диска.");
                }
            default:
                throw new IllegalArgumentException("Неизвестный выходной формат");
        }
    }

    public int getNumberOfRespondents() {
        return allTable.rowCount();
    }

    public Mode getMode() {
        return mode;
    }

    public String getPathToInputFile() {
        return pathToInputFile;
    }

    public String getPathToOutputDirectory() {
        return pathToOutputDirectory;
    }

    public boolean isSaveFreeAnswers() {
        return saveFreeAnswers;
    }

    public OutputFormats getFormat() {
        return format;
    }

    public Table getAllTable() {
        return allTable;
    }

    public JSONArray getDescQuestions() {
        return descQuestions;
    }

    private static String getJSONString(String pathToJSON){
        StringBuilder stringBuilder = new StringBuilder();
        // открываем соедиение к указанному URL
        // помощью конструкции try-with-resources
        try (BufferedReader in = new BufferedReader(new FileReader(pathToJSON))) {
            String inputLine;
            // построчно считываем результат в объект StringBuilder
            while ((inputLine = in.readLine()) != null) {
                stringBuilder.append(inputLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
}
