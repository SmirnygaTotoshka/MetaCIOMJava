package ru.smirnygatotoshka.MetaCIOM.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.smirnygatotoshka.MetaCIOM.Data;
import ru.smirnygatotoshka.MetaCIOM.MatrixQuestion;
import ru.smirnygatotoshka.MetaCIOM.io.ExcelIO;
import ru.smirnygatotoshka.MetaCIOM.io.MetadataParser;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;


import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class MatrixQuestionTest {
    Table data;
    MatrixQuestion testQuestion;
    Data metadata;
    @BeforeEach
    void setUp() throws IOException, GeneralSecurityException {
        metadata = new MetadataParser("C:\\Users\\SmirnygaTotoshka\\Documents\\CommonWorkspace\\Univer\\ЦИОМ\\test_metadata\\metadata_matrix.json").parse();
        data = ExcelIO.read("C:\\Users\\SmirnygaTotoshka\\Downloads\\RNIMU GOOGLE SURVEY FOR FOREIGN STUDENTS  (Ответы).xlsx",0);
    }
   /* @Test
    void testBuildingQuestion(){
        Question[] questions = metadata.getQuestions();
        assertEquals(1,questions.length);
        assertTrue(questions[0] instanceof MatrixQuestion);
    }*/
    @Test
    void calculate() {
        testQuestion = (MatrixQuestion) metadata.getQuestions()[0];
        Table t = testQuestion.calculate(data);
        System.out.println(t);
    }

    @Test
    void testProcents() throws ParseException {
        testQuestion = (MatrixQuestion) metadata.getQuestions()[0];
        Table t = testQuestion.calculate(data);
        NumberFormat instance = NumberFormat.getInstance(Locale.getDefault());
        for (int i = 1;i < t.columnCount();i++){
            double sum = 0;//TODO - округление
            StringColumn column = t.stringColumn(i);
            for (int j = 0;j < column.size();j++){
                sum += instance.parse(column.get(j)).doubleValue();

            }
            assertEquals(100,sum);
        }
    }
}