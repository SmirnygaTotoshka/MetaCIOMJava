package ru.smirnygatotoshka.MetaCIOM.io;

import tech.tablesaw.api.Table;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public interface TableIO {
    Table readTable(String path) throws IOException;
    void writeTables(Table[] tables,String dir) throws IOException;

    default void writeFreeAnswers(String[] free_ans,String name,String dir) throws IOException{
        String path = dir + File.separator + name.replaceAll("[\\\\\\\\/:*?\\\"<>|]","") + "_free.txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        for (String a : free_ans){
            writer.write(a);
            writer.newLine();
        }
        writer.close();
    }
}
