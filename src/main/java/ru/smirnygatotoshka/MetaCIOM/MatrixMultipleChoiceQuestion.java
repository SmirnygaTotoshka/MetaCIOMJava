package ru.smirnygatotoshka.MetaCIOM;

import org.json.JSONObject;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

public class MatrixMultipleChoiceQuestion extends MatrixSingleChoiceQuestion{

    private String separator;

    MatrixMultipleChoiceQuestion(){}

    private MatrixMultipleChoiceQuestion(String separator,Metadata metadata, TypeStatistics[] calculate, String[] answersVariants, String question, Divider divider, int[] groupingWith, int startColumn, int finishColumn) {
        super(metadata, calculate, answersVariants, question, divider, groupingWith, startColumn,finishColumn);
        this.separator = separator;
    }

    @Override
    public Question build(Metadata metadata, JSONObject question) {
        MatrixSingleChoiceQuestion s = (MatrixSingleChoiceQuestion) super.build(metadata, question);
        String sep = question.getString("separator");
        return new MatrixMultipleChoiceQuestion(sep,s.metadata,s.statistics,s.answersVariants,s.question,s.divider,s.groupingWith,s.startColumn,s.finishColumn);
    }

    @Override
    public Table clean() {
        return super.clean();
    }

    @Override
    public Table calculate(Table data) {
        String[] categoryNames = getCategoriesNames();
        Table[] ansByCat = new Table[categoryNames.length];
        for (int i = 0; i < data.columnCount(); i++) {
            Table t = data.selectColumns(data.column(i)).copy();
            Table toNotMultiple = Table.create(question);
            toNotMultiple.addColumns(StringColumn.create(question));
            for (int j = 0; j < t.rowCount(); j++)  {
                String[] vars = t.get(j, 0).toString().trim().split(separator);
                for (String s : vars) {
                    toNotMultiple.column(0).appendCell(s.trim());
                }
            }
            toNotMultiple.dropRowsWithMissingValues();
            ansByCat[i] = super.calculate(toNotMultiple);
        }
        return super.formReport(ansByCat);
    }

    @Override
    public String createForm() {
        return super.createForm();
    }
}
