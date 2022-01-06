package ru.smirnygatotoshka.MetaCIOM;

import ru.smirnygatotoshka.MetaCIOM.io.TableIO;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    private static Metadata metadata;

    public static void main(String[] args) {
        try {
            printInfo();
            String inputPath = parseUserInput();
            metadata = Metadata.parse(inputPath);
            if (metadata.getMode() == Metadata.Mode.CALCULATE) {
                QuestionFactory factory = new QuestionFactory();
                Table[] result = new Table[metadata.getDescQuestions().length()];
                TableIO io = metadata.getTableIO();
                for (int i = 0; i < result.length; i++) {
                    Question question = factory.build(metadata, metadata.getDescQuestions().getJSONObject(i));
                    Table data = question.clean();
                    result[i] = question.calculate(data);
                    if (metadata.isSaveFreeAnswers() && question.hasFreeAnswers) {
                        io.writeFreeAnswers(question.getFreeAnswers(), question.question, metadata.getPathToOutputDirectory());
                    }
                    System.out.println(question.question + "_" + i);
                }
                io.writeTables(result, metadata.getPathToOutputDirectory());
            }
        }
        catch (IOException e){
            System.err.println("Cannot read/write the table");
            e.printStackTrace();
        }
    }

    public static void printInfo(){
        System.out.println("Программа для обработки опросов\n" +
                "Написана для ЦИОМ РНИМУ им. Н.И. Пирогова.\n" +
                "Автор: агент 036(STotoshka), 2021 год.\n");
        System.out.println("Введите путь/URL до документа с информацией об опросе\n");
    }
    public static String parseUserInput() {
        // Using Scanner for Getting Input from User
        Scanner in = new Scanner(System.in);
        return in.nextLine().trim();
    }
}
