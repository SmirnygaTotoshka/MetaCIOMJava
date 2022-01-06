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
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GoogleDriveIO implements TableIO{

    private static final String APPLICATION_NAME = "CIOM-MetaCIOM";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SHEET_SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final Collection<String> DOCS_SCOPES = Collections.singletonList(DocsScopes.DOCUMENTS);
    private static final Collection<String> DRIVE_SCOPES = Collections.singletonList(DriveScopes.DRIVE);


    @Override
    public Table readTable(String path) throws IOException {
        if (!path.contains("docs.google.com/spreadsheets/")) throw new IOException("Not spreadsheet url");
        final NetHttpTransport HTTP_TRANSPORT;
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new IOException("GeneralSecurityException when reading");
        }
        String spreadsheetId = getIdFromURL(path);
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT,CREDENTIALS_FILE_PATH,SHEET_SCOPES))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String sheet = service.spreadsheets()
                .get(spreadsheetId)
                .execute()
                .getSheets()
                .get(0)
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

    @Override
    public void writeTables(Table[] tables, String dir) throws IOException {
        if (!dir.contains("google.com")) throw new IOException("Not spreadsheet url");
        final NetHttpTransport HTTP_TRANSPORT;
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new IOException("GeneralSecurityException when reading");
        }
        String dirID = getIdFromURL(dir);
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT,CREDENTIALS_FILE_PATH,SHEET_SCOPES))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String title = tables[0].name();
        Spreadsheet spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties()
                        .setTitle(title));
        spreadsheet = service.spreadsheets().create(spreadsheet)
                .setFields("spreadsheetId")
                .execute();


        List<ValueRange> ranges = new ArrayList<>();
        List<AddSheetRequest> requests = new ArrayList<>();

        for (int t = 0;t < tables.length;t++){
            List<Column<?>> cols = tables[t].columns();
            List<List<Object>> values = new ArrayList<>();
            for (int i = 0; i < cols.size(); i++) {
                StringColumn c = cols.get(i).asStringColumn();
                values.add(new ArrayList<>());
                values.get(i).add(c.name());
                for (int j = 0; j < c.size(); j++) {
                    values.get(i).add(c.get(j));
                }
            }
            String range = "Лист" + (t+1);
            AddSheetRequest addSheetRequest = new AddSheetRequest().set("sheetId",range);
            requests.add(addSheetRequest);
            BatchUpdateSpreadsheetRequest requestBody = new BatchUpdateSpreadsheetRequest();
           /* requestBody.setRequests(requests);
           // service.spreadsheets().batchUpdate(spreadsheet.getSpreadsheetId(),addSheetRequest);
            ValueRange r = new ValueRange()
                    .setValues(values)
                    .setRange(range)
                    .setMajorDimension("COLUMNS");
            BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                    .setValueInputOption("RAW")
                    .setData(r).set;

            AppendValuesResponse response = service.spreadsheets().values()
                    .append(spreadsheet.getSpreadsheetId(),range,r)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();
            System.out.println(response.getTableRange());*/
        }
    }

    @Override
    public void writeFreeAnswers(String[] free_ans, String name, String dir) throws IOException {
        if (!name.contains("docs.google.com/spreadsheets/")) throw new IOException("Not spreadsheet url");
        final NetHttpTransport HTTP_TRANSPORT;
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new IOException("GeneralSecurityException when reading");
        }
        String id = getIdFromURL(name);
        Drive drive_service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT, CREDENTIALS_FILE_PATH,DRIVE_SCOPES))
                .setApplicationName(APPLICATION_NAME)
                .build();
        String folderId = getIdFromURL(dir);
        File fileMetadata = new File();
        fileMetadata.setName(name.replaceAll("[\\\\\\\\/:*?\\\"<>|]",""));
        fileMetadata.setParents(Collections.singletonList(folderId));
        fileMetadata.setMimeType("application/vnd.google-apps.document");
        java.io.File filePath = new java.io.File("/tmp.txt");
        FileContent mediaContent = new FileContent("text/plain", filePath);
        File file = drive_service.files().create(fileMetadata, mediaContent)
                .setFields("id, parents")
                .execute();
        filePath.delete();
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT,final String cred_path,final Collection<String> scopes) throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveIO.class.getResourceAsStream(cred_path);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + cred_path);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

//
//    public static Table getSpreadsheet(String url, int num_sheet) throws GeneralSecurityException, IOException {
//        if (!url.contains("docs.google.com/spreadsheets/")) throw new IOException("Not spreadsheet url");
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        String spreadsheetId = getIdFromURL(url);
//        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//
//        String sheet = service.spreadsheets()
//                .get(spreadsheetId)
//                .execute()
//                .getSheets()
//                .get(num_sheet)
//                .getProperties()
//                .getTitle();
//        ValueRange spreadsheet = service.spreadsheets()
//                .values()
//                .get(spreadsheetId, sheet)
//                .setMajorDimension("COLUMNS")
//                .execute();
//
//        Table table = Table.create(sheet);
//        for (List col : spreadsheet.getValues()){
//            ArrayList<String> tbl_column = new ArrayList<>();
//            StringColumn.create(col.get(0).toString());
//            for (int i = 1; i < col.size(); i++) {
//                tbl_column.add(col.get(i).toString());
//            }
//            appendRowsIfNeeded(spreadsheet.getValues(), tbl_column);
//            table = table.addColumns(StringColumn.create(col.get(0).toString(),tbl_column));
//            tbl_column.clear();
//        }
//        return table;
//    }
//
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
//
//    /**
//     * Prints the names and majors of students in a sample spreadsheet:
//     * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
//     */
//    public static void main(String... args) throws IOException, GeneralSecurityException {
//
//        //Read google sheet
//       /* //FileUtils.deleteDirectory(new java.io.File("tokens"));
//        Table tbl = getSpreadsheet("https://docs.google.com/spreadsheets/d/19IOlIf4CksGlZSQGE7BKfihEaOPZBoc_BlU1l5XurBo/edit?usp=sharing",0);
//        assert tbl != null;
//        System.out.println(tbl);*/
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//        String[] animals = {"bear", "cat", "giraffe"};
//        double[] cuteness = {90.1, 84.3, 99.7};
//
//        Table test_write =
//                Table.create("Cute Animals")
//                        .addColumns(
//                                StringColumn.create("Animal types", animals),
//                                DoubleColumn.create("rating", cuteness));
//        uploadTable("https://drive.google.com/drive/folders/18jSj1ZR9nkp08grePiijv0ptl3lqojDq",
//                test_write,null);
//        System.out.println("Successful Meow");
//        /*// Print the names and IDs for up to 10 files.
//        FileList result = service.files().list()
//                .setQ("\"18jSj1ZR9nkp08grePiijv0ptl3lqojDq\" in parents")
//                .setPageSize(10)
//                .setFields("nextPageToken, files(id, name)")
//                .execute();
//        List<File> files = result.getFiles();
//        if (files == null || files.isEmpty()) {
//            System.out.println("No files found.");
//        } else {
//            System.out.println("Files:");
//            for (File file : files) {
//                System.out.printf("%s (%s)\n", file.getName(), file.getId());
//            }
//        }*/
//    }
//
//    public static void uploadTable(String outFolderUrl, Table table, String outFilename) throws IOException, GeneralSecurityException {
//        String temp_name = "tmp_excel.xlsx";
//        String out = outFilename == null ? table.name() : outFilename;
//        ExcelIO.write(table, temp_name,false);
//        uploadFile(outFolderUrl,temp_name, out, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.google-apps.spreadsheet");
//        Files.delete(Paths.get(temp_name));
//    }
//
//    public static ByteArrayOutputStream readFile(String url,String mime) throws IOException, GeneralSecurityException {
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//        String fileId = getIdFromURL(url);
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        service.files().get(fileId)
//                .executeMediaAndDownloadTo(outputStream);
//        return outputStream;
//    }
//
//    public static void uploadFile(String outFolderUrl, String inFilename, String outFilename, String local_mime,String target_mime) throws GeneralSecurityException, IOException {
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//        String folderId = getIdFromURL(outFolderUrl);
//        File fileMetadata = new File();
//        fileMetadata.setName(outFilename);
//        fileMetadata.setParents(Collections.singletonList(folderId));
//        if (target_mime != null)
//            fileMetadata.setMimeType(target_mime);
//        java.io.File filePath = new java.io.File(inFilename);
//        FileContent mediaContent = new FileContent(local_mime, filePath);
//        File file = service.files().create(fileMetadata, mediaContent)
//                .setFields("id, parents")
//                .execute();
//        System.out.println("File ID: " + file.getId());
//    }
//
    private static String getIdFromURL(String urlToSheet) throws MalformedURLException {
        URL url = new URL(urlToSheet);
        String path = url.getPath();
        String[] folders = path.split("/");
        return folders[3];
    }
//

}
