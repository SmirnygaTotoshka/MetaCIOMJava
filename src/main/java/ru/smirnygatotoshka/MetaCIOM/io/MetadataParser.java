package ru.smirnygatotoshka.MetaCIOM.io;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.smirnygatotoshka.MetaCIOM.Data;
import tech.tablesaw.api.Table;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MetadataParser {

    private String pathToMetadata;

    public MetadataParser(String pathToMetadata) {
       this.pathToMetadata = pathToMetadata;
    }

    public Data parse() throws IOException {
        JSONObject metadata = new JSONObject(getJSONString(pathToMetadata));
        String pathToTable = metadata.getString("input_table");
        Table table = ExcelIO.read(pathToTable,0);
        String pathToOutputDirectory = metadata.getString("output_dir");
        boolean saveFreeAnswers = metadata.getBoolean("save_free_answers");
        boolean toGoogle = metadata.getBoolean("to_google");
        Data.OutputFormats format = Data.OutputFormats.valueOf(metadata.getString("output_format"));
        JSONArray array = metadata.getJSONArray("questions");
        return new Data(table, pathToOutputDirectory,saveFreeAnswers,toGoogle, array, format);
    }


    private String getJSONString(String pathToJSON){
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
