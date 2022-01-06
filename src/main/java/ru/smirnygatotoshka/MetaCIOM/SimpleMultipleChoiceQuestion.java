package ru.smirnygatotoshka.MetaCIOM;

import org.json.JSONObject;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;

public class SimpleMultipleChoiceQuestion extends SimpleSingleChoiceQuestion{
    private String separator;

    SimpleMultipleChoiceQuestion(){

    }
    private SimpleMultipleChoiceQuestion(String separator,Metadata metadata, TypeStatistics[] calculate, String[] answersVariants, String question, Divider divider, int column, int[] groupingWith, boolean hasFreeAnswers) {
        super(metadata, calculate, answersVariants, question, divider,column,groupingWith,hasFreeAnswers);
        this.separator = separator;
    }

    @Override
    public Question build(Metadata metadata, JSONObject question) {
        SimpleSingleChoiceQuestion s = (SimpleSingleChoiceQuestion) super.build(metadata, question);
        String sep = question.getString("separator");
        return new SimpleMultipleChoiceQuestion(sep,metadata,s.statistics,s.answersVariants,s.question,s.divider,s.column,s.groupingWith,s.hasFreeAnswers);
    }

    @Override
    public Table clean() {
        Table subset = metadata.getAllTable().selectColumns(getNeededColumnsIndexes());
        StringColumn col = subset.column(subset.columnCount() - 1).asStringColumn();
        for (int i = 0; i < col.size(); i++) {
            String value = col.get(i);
            if (value.isEmpty())
                continue;
            else {
                String[] vars = value.split(separator);
                boolean meet_undef = false;
                String cleanStr = "";
                for (String v : vars) {
                    if (!in(v.trim(), answersVariants) && hasFreeAnswers && !meet_undef) {
                        cleanStr += UNDEFINED_VALUE + separator;
                        meet_undef = true;
                    }
                    if (!in(v.trim(), answersVariants) && !hasFreeAnswers) {
                        throw new IllegalStateException("Нет такого варианта ответа. Проверьте метадату/данные. Value = " + value);
                    }
                    if (in(v.trim(),answersVariants)){
                        cleanStr += v.trim() + separator;
                    }
                }
                col.set(i,cleanStr);
            }
        }
        return subset.replaceColumn(subset.columnCount()-1,col).dropRowsWithMissingValues();
    }

    @Override
    public Table calculate(Table data) {
        if (in(TypeStatistics.MEAN, statistics) || in(TypeStatistics.MEDIAN,statistics))
            throw new IllegalStateException("Cant calculate mean or median for question with multiple choices.");
        else
            return super.calculate(data);
    }

    @Override
    public String createForm() {
        return super.createForm();
    }

    @Override
    protected Table calculate(Table data, TypeStatistics type) throws IllegalArgumentException {
        Table toNotMultiple = Table.create(question);
        for (int i = 0; i < groupingWith.length; i++) {
            toNotMultiple.addColumns(StringColumn.create(metadata.getAllTable().column(groupingWith[i]).name()));
        }
        toNotMultiple.addColumns(StringColumn.create(question));

        int rows = data.rowCount();
        for (int j = 0; j < rows; j++)  {
            String[] vars = data.get(j, data.columnCount()-1).toString().trim().split(separator);
            for (String s : vars) {
                for (int i = 0; i < groupingWith.length; i++) {
                    toNotMultiple.column(i).appendCell(metadata.getAllTable().get(j,groupingWith[i]).toString().trim());
                }
                toNotMultiple.column(toNotMultiple.columnCount()-1).appendCell(s.trim());
            }
        }
        toNotMultiple.dropRowsWithMissingValues();
        return super.calculate(toNotMultiple, type);
    }

    @Override
    public String[] getFreeAnswers() {
        if (!hasFreeAnswers)
            return new String[0];
        else {
            StringColumn col = metadata.getAllTable().column(column).asStringColumn();
            ArrayList<String> ans = new ArrayList<>();
            for (int i = 0; i < col.size(); i++) {
                String[] vars = col.get(i).trim().split(separator);
                for (String v : vars) {
                    if (!in(v.trim(), answersVariants))
                        //ans.add(censor(v.trim()));
                        ans.add(v.trim());
                }
            }
            return ans.toArray(new String[0]);
        }
    }
}
