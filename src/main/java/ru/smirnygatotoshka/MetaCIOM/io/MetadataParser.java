package ru.smirnygatotoshka.MetaCIOM.io;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.smirnygatotoshka.MetaCIOM.Data;
import tech.tablesaw.api.Table;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;

public class MetadataParser {

    private String pathToMetadata;
    private boolean local;

    public MetadataParser(String pathToMetadata) throws IllegalArgumentException{
        URL urlToMetadata = null;
        try {
            urlToMetadata = new URL(pathToMetadata);
            if (urlToMetadata.getProtocol().contentEquals("https") &&
                    urlToMetadata.getAuthority().contains("google.com")) {
                this.local = false;
            }
            else {
                throw new IllegalArgumentException("It`s not google link.");
            }
        }
        catch (MalformedURLException e) {
            this.local = true;
        }
        finally {
            this.pathToMetadata = pathToMetadata;
        }
    }

    public Data parse() throws IOException, GeneralSecurityException {
        String json = "";
        if (local)
            json = getJSONString(pathToMetadata);
        else{
            ByteArrayOutputStream file = GoogleDriveIO.readFile(pathToMetadata,"text/json");
            json = file.toString("utf-8");
        }
        JSONObject metadata = new JSONObject(json);
        String pathToTable = metadata.getString("input_table");
        Table table = local ? ExcelIO.read(pathToTable,0) : GoogleDriveIO.getSpreadsheet(pathToTable,0);
        String pathToOutputDirectory = metadata.getString("output_dir");
        boolean saveFreeAnswers = metadata.getBoolean("save_free_answers");
        boolean toGoogle = metadata.getBoolean("to_google");//TODO - remove field. detect by urls
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
