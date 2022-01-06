package ru.smirnygatotoshka.MetaCIOM.io;

import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.io.csv.CsvWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvIO implements TableIO{

    @Override
    public Table readTable(String path) throws IOException {
        File file = new File(path);
        CsvReadOptions options = CsvReadOptions.builder(file).separator(';').header(true).build();
        return Table.read().csv(options);
    }

    @Override
    public void writeTables(Table[] tables,String dir) throws IOException  {
        CsvWriter writer = new CsvWriter();
        for (Table t : tables){
            String filename = t.name().replaceAll("[\\\\\\\\/:*?\\\"<>|]","") + ".csv";
            String path = dir + File.separator + filename;
            CsvWriteOptions options = CsvWriteOptions.builder(path).separator(';').header(true).build();
            writer.write(t, options);
        }
    }

}
