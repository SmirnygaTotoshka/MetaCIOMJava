package ru.smirnygatotoshka.MetaCIOM.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.smirnygatotoshka.MetaCIOM.Data;
import ru.smirnygatotoshka.MetaCIOM.Question;
import ru.smirnygatotoshka.MetaCIOM.SimpleQuestion;
import ru.smirnygatotoshka.MetaCIOM.io.MetadataParser;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

class SimpleQuestionTest {
    Table data;
    SimpleQuestion testQuestion;
    Data metadata;

    @BeforeEach
    void setUp() throws IOException, GeneralSecurityException {
        data = Table.read().csv("C:\\Users\\SmirnygaTotoshka\\Downloads\\Опрос коррупция.csv");
        metadata = new MetadataParser("C:\\Users\\SmirnygaTotoshka\\Documents\\CommonWorkspace\\Univer\\ЦИОМ\\test_metadata\\metadata.json").parse();
    }

    @Test
    void testBuildingQuestion(){
        Question[] questions = metadata.getQuestions();
        assertEquals(1,questions.length);
    }
    @Test
    void printData(){
        System.out.println(data);
        System.out.println(data.row(0));
        assertTrue(true);
    }
    @Test
    void testCalculate(){
        Question[] questions = metadata.getQuestions();
        Table res = questions[0].calculate(data);
        System.out.println(res);
        assertTrue(true);
    }

    @Test
    void testProcent(){
        Question[] questions = metadata.getQuestions();
        Table res = questions[0].calculate(data);
        Table t = res.summarize(res.column(2).name(), AggregateFunctions.sum).apply();
        assertEquals(100.0,t.get(0,0));
    }


    @Test
    void testRead(){
        assertEquals(8,data.columnCount());
    }
}