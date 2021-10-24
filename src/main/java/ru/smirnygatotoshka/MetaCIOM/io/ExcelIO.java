package ru.smirnygatotoshka.MetaCIOM.io;

import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.*;
import java.util.ArrayList;

public class ExcelIO {
    public static Table read(String path, int sheetIndex) throws IOException {
        String pathToConverted = "";
        Table table = null;
        File converted = null;
        try {
            pathToConverted = convertXLSXToCSV(path, sheetIndex);
            converted = new File(pathToConverted);
            CsvReadOptions options = CsvReadOptions.builder(converted).separator(';').header(true).build();
            table = Table.read().csv(options);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            if (!converted.delete()) throw new IOException("Cannot delete temp file");
            return table;
        }
    }

    public static void write(Table table, String output, boolean append) throws IOException{
        //Blank workbook
        XSSFWorkbook workbook;
        File o = new File(output);
        if (append && o.exists())
            workbook = new XSSFWorkbook(output);
        else
            workbook = new XSSFWorkbook();

        XSSFSheet sheet = workbook.createSheet(table.name());

        ArrayList<String> header = (ArrayList<String>) table.columnNames();
        //write header
        Row h = sheet.createRow(0);
        int cellnum = 0;
        for (String colname : header){
            Cell cell = h.createCell(cellnum++);
            cell.setCellValue(colname);
        }
        //write table body
        for (int rownum = 0; rownum < table.rowCount();rownum++){
            Row r = sheet.createRow(rownum+1);
            for (cellnum = 0; cellnum < table.columnCount();cellnum++){
                Cell cell = r.createCell(cellnum);
                cell.setCellValue(table.getUnformatted(rownum,cellnum));
            }
        }

        FileOutputStream out = new FileOutputStream(output);
        workbook.write(out);
        out.close();
        workbook.close();
    }


    public static String convertXLSXToCSV(String xlsx_path, int sheetIndex) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(xlsx_path);
        XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
        int numRows;
        numRows = sheet.getPhysicalNumberOfRows();

        int numCols = 0; // No of columns
        int tmp = 0;

        // This trick ensures that we get the data properly even if it doesn't start from first few rows
        for(int i = 0; i < 10 || i < numRows; i++) {
            Row row = sheet.getRow(i);
            if(row != null) {
                tmp = sheet.getRow(i).getPhysicalNumberOfCells();
                if(tmp > numCols) numCols = tmp;
            }
        }
        File excel = new File(xlsx_path);
        String path = excel.getParent();
        String newName = excel.getName().substring(0,excel.getName().lastIndexOf(".xlsx")) + "_converted.csv";
        char separator = ';';
        String pathToConverted = path + File.separator + newName;
        CSVWriter writer = new CSVWriter(new FileWriter(pathToConverted),separator,'"', '"', "\n");

        for(int r = 0; r < numRows; r++) {
            Row row = sheet.getRow(r);
            if(row != null) {
                String[] values = new String[numCols];
                for(int c = 0; c < numCols; c++) {
                    Cell cell = row.getCell(c);
                    if(cell != null) {
                        DataFormatter formatter = new DataFormatter();
                        values[c] = formatter.formatCellValue(cell);
                    }
                    else {
                        values[c] = "";
                    }
                }
                writer.writeNext(values);
            }
        }
        writer.close();
        workbook.close();
        return pathToConverted;
    }

}
