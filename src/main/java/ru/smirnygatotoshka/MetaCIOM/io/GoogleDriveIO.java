package ru.smirnygatotoshka.MetaCIOM.io;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleDriveIO {
    private static final String APPLICATION_NAME = "CIOM-MetaCIOM";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.DRIVE);
    

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveIO.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static Table getSpreadsheet(String url, int num_sheet) throws GeneralSecurityException, IOException {
        if (!url.contains("docs.google.com/spreadsheets/")) throw new IOException("Not spreadsheet url");
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        String spreadsheetId = getIdFromURL(url);
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String sheet = service.spreadsheets()
                .get(spreadsheetId)
                .execute()
                .getSheets()
                .get(num_sheet)
                .getProperties()
                .getTitle();
        ValueRange spreadsheet = service.spreadsheets()
                .values()
                .get(spreadsheetId, sheet)
                .setMajorDimension("COLUMNS")
                .execute();

        Table table = Table.create(sheet);
        for (List col : spreadsheet.getValues()){
            ArrayList<String> tbl_column = new ArrayList<>();
            StringColumn.create(col.get(0).toString());
            for (int i = 1; i < col.size(); i++) {
                tbl_column.add(col.get(i).toString());
            }
            appendRowsIfNeeded(spreadsheet.getValues(), tbl_column);
            table = table.addColumns(StringColumn.create(col.get(0).toString(),tbl_column));
            tbl_column.clear();
        }
        return table;
    }

    private static void appendRowsIfNeeded(List<List<Object>> values, ArrayList tbl_column) {
        int num_rows = getNumRows(values);
        assert (num_rows >= tbl_column.size());
        if (tbl_column.size() < num_rows){
            for (int i = tbl_column.size(); i < num_rows; i++) {
                tbl_column.add("");
            }
        }
    }

    private static int getNumRows(List<List<Object>> values) {
        int max = values.get(0).size()-1;
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).size()-1 > max)
                max = values.get(i).size()-1;
        }
        return max;
    }

    public static void uploadSpreadsheet(String path, Table table){

    }
    /**
     * Prints the names and majors of students in a sample spreadsheet:
     * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
     */
    public static void main(String... args) throws IOException, GeneralSecurityException {

        //Read google sheet
       /* //FileUtils.deleteDirectory(new java.io.File("tokens"));
        Table tbl = getSpreadsheet("https://docs.google.com/spreadsheets/d/19IOlIf4CksGlZSQGE7BKfihEaOPZBoc_BlU1l5XurBo/edit?usp=sharing",0);
        assert tbl != null;
        System.out.println(tbl);*/
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        createFile("https://drive.google.com/drive/folders/18jSj1ZR9nkp08grePiijv0ptl3lqojDq",
                "D:\\DataSpellWorkspace\\COVID.ipynb",
                "COVID.ipynb",
                "text/json");
        System.out.println("Successful Meow");
        /*// Print the names and IDs for up to 10 files.
        FileList result = service.files().list()
                .setQ("\"18jSj1ZR9nkp08grePiijv0ptl3lqojDq\" in parents")
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }*/
    }

    public static void createFile(String outFolderUrl,String inFilename,String outFilename, String type) throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        String folderId = getIdFromURL(outFolderUrl);
        File fileMetadata = new File();
        fileMetadata.setName(outFilename);
        fileMetadata.setParents(Collections.singletonList(folderId));
        java.io.File filePath = new java.io.File(inFilename);
        FileContent mediaContent = new FileContent(type, filePath);
        File file = service.files().create(fileMetadata, mediaContent)
                .setFields("id, parents")
                .execute();
        System.out.println("File ID: " + file.getId());
    }

    private static String getIdFromURL(String urlToSheet) throws MalformedURLException {
        URL url = new URL(urlToSheet);
        String path = url.getPath();
        String[] folders = path.split("/");
        return folders[3];
    }
}
