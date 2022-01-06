package ru.smirnygatotoshka.MetaCIOM.io;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class ExcelIO implements TableIO {

    @Override
    public Table readTable(String path) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(path);
        XSSFSheet sheet = workbook.getSheetAt(0);
        ArrayList<StringColumn> cols = new ArrayList<>();//iterating
        int num_cols = sheet.getRow(sheet.getFirstRowNum()).getPhysicalNumberOfCells();// over excel file
        for (int i = sheet.getFirstRowNum(); i < sheet.getPhysicalNumberOfRows(); i++) {
            XSSFRow row = sheet.getRow(i);
            if (row == null) {
               continue;
            }
            else {
                for (int j = 0; j < num_cols; j++) {
                    XSSFCell cell = row.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = getCellValue(cell);
                    if (i == sheet.getFirstRowNum()) {
                        cols.add(StringColumn.create(val));
                    } else {
                        cols.get(j).append(val);
                    }
                }
            }
        }
        String name = path.substring(path.lastIndexOf(File.separator) + 1, path.lastIndexOf('.'));
        Table table = Table.create(name);
        for (StringColumn c : cols)
            table.addColumns(c);
        return table;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:    //field that represents string cell type
                return cell.getStringCellValue();
            case _NONE:
                return "";
            case NUMERIC:    //field that represents number cell type
                return String.valueOf((int) cell.getNumericCellValue());
            case FORMULA:
                return String.valueOf(cell.getNumericCellValue());
            case BLANK:
                return "0";
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case ERROR:
                return "#ОШИБ";
            default:
                return "";
        }
    }

    @Override
    public void writeTables(Table[] tables, String dir) throws IOException {
        String path = dir + File.separator + tables[0].name().replaceAll("[\\\\\\\\/:*?\\\"<>|]", "") + ".xlsx";
        XSSFWorkbook workbook = new XSSFWorkbook();
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setShrinkToFit(true);
        for (int i = 0;i < tables.length;i++) {
            String suf = tables[i].name().length() <= 15 ? tables[i].name() : tables[i].name().substring(0,15);
            String name = (i+1) + "_" + suf;
            XSSFSheet sheet = workbook.createSheet(name);
            ArrayList<String> header = (ArrayList<String>) tables[i].columnNames();
            //write header
            Row h = sheet.createRow(0);
            int cellnum = 0;
            for (String colname : header) {
                Cell cell = h.createCell(cellnum++);
                cell.setCellValue(colname);
            }
            //write table body
            for (int rownum = 0; rownum < tables[i].rowCount(); rownum++) {
                Row r = sheet.createRow(rownum + 1);
                for (cellnum = 0; cellnum < tables[i].columnCount(); cellnum++) {
                    Cell cell = r.createCell(cellnum);
                    cell.setCellStyle(style);
                    cell.setCellValue(tables[i].get(rownum, cellnum).toString());
                }
            }
            FileOutputStream out = new FileOutputStream(path);
            workbook.write(out);
            out.close();
        }
        workbook.close();
    }
}