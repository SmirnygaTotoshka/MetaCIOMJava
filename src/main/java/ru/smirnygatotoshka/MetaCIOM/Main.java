package ru.smirnygatotoshka.MetaCIOM;

import me.tongfei.progressbar.ProgressBar;
import ru.smirnygatotoshka.MetaCIOM.io.MetadataParser;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        printInfo();
        System.out.println("Введите путь/URL до документа с информацией об опросе\n");
        String inputPath = parseUserInput();
        if (inputPath.contains("google.com")){
            //TODO
            System.err.println("TODO");
        }
        else {
            File cfg = new File(inputPath);
            if (cfg.exists()){
                MetadataParser parser = new MetadataParser(inputPath);
                Data data;

                try {
                    data = parser.parse();
                    Question[] questions = data.getQuestions();
                    DataCleaner cleaner = new DataCleaner();
                    ArrayList<Table> results = new ArrayList<>();
                    ProgressBar pb = new ProgressBar("Обработка опроса:", questions.length);
                    for (Question q : questions){
                        Table[] cleanTables = cleaner.clean(q);
                        for (Table t : cleanTables) {
                            Table res = q.calculate(t);
                            results.add(res);
                        }
                        q.save(results);
                        pb.step();
                        results.clear();
                    }
                    pb.close();
                }
                catch (IOException e) {
                    System.err.println("Cannot parse metadata " + inputPath);
                    e.printStackTrace();
                    return;
                }
            }
            else {
                System.err.println("Metadata not exist");
            }
        }
    }

    public static void printInfo(){
        System.out.println("Программа для обработки опросов\n" +
                "Написана для ЦИОМ РНИМУ им. Н.И. Пирогова.\n" +
                "Автор: агент 036(STotoshka), 2021 год.\n");
    }
    public static String parseUserInput() {
        // Using Scanner for Getting Input from User
        Scanner in = new Scanner(System.in);
        return in.nextLine().trim();
    }
}
