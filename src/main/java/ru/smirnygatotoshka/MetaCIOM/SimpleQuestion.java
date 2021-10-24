package ru.smirnygatotoshka.MetaCIOM;

import tech.tablesaw.aggregate.AggregateFunction;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;

import static tech.tablesaw.aggregate.AggregateFunctions.*;

public class SimpleQuestion extends Question{
    private final int column;
    protected String columnName;

    SimpleQuestion(String question, String[] answers, boolean multiple, boolean hasFreeAnswer, TypeCalculate[] calculate, int column, int[] gr,Divider divider, Data metadata) {
        super(metadata, multiple,hasFreeAnswer,calculate,gr,answers,question,divider);
        this.column = column;
        this.columnName = metadata.getAllTable().column(column).name();

    }

    SimpleQuestion(String question, String[] answers, boolean multiple, boolean hasFreeAnswer, TypeCalculate[] calculate, int[] gr,Divider divider, Data metadata) {
        super(metadata, multiple,hasFreeAnswer,calculate,gr,answers,question,divider);
        this.column = 0;
    }

    @Override
    public Table calculate(Table data) {
        changeDivider(data);
        Table summary = Table.create(question);
        for (int i = 0; i < calculate.length; i++) {
            Table res;
            if (!multiple){
                res = calculate(data, calculate[i]);
            }
            else{
                res = calculateMultiple(data, calculate[i]);
            }
            if (i == 0){
                for (int j = 0; j < res.columnCount(); j++) {
                    summary.addColumns(res.column(j));
                }
            }
            else {
                summary.addColumns(res.column(1));
            }
        }
        return summary;
    }

    private void changeDivider(Table data){
        switch (divider){
            case RESPONDENTS:
                divider.setNumDivider(metadata.getNumberOfRespondents());
                break;
            case ANSWERED:
                divider.setNumDivider(data.rowCount());
                break;
            default:
                throw new IllegalStateException("Unknown divider");
        }
    }
//TODO - fix factors levels
    protected Table calculateMultiple(Table data, TypeCalculate calculate) {
        if (calculate == TypeCalculate.MEAN || calculate == TypeCalculate.MEDIAN)
            throw new IllegalStateException("Cant calculate mean or median for question with multiple choices.");
        else {
            Table toNotMultiple = Table.create(columnName);
            Table table = data.copy();
            toNotMultiple.addColumns(StringColumn.create(columnName));
            for (int i = 0; i < groupingWith.length; i++) {
                toNotMultiple.addColumns(StringColumn.create(metadata.getAllTable().column(groupingWith[i]).name()));
            }
            for (int j = 0; j < table.rowCount(); j++)  {
                String[] vars = table.row(j).getString(0).split(Data.VARIANT_SEPARATOR);
                for (String s : vars) {
                    toNotMultiple.column(0).appendCell(s.trim());
                    for (int i = 0; i < groupingWith.length; i++) {
                        toNotMultiple.column(i+1).appendCell(metadata.getAllTable().get(j,groupingWith[i]).toString().trim());
                    }
                }
            }
            toNotMultiple.dropRowsWithMissingValues();
            int[] gr = Utils.getRange(1, groupingWith.length);
            SimpleQuestion newQuest = new SimpleQuestion(question,answersVariants,multiple,hasFreeAnswer,this.calculate,column,gr,this.divider,metadata);
            return newQuest.calculate(toNotMultiple,calculate);
        }
    }

    private ArrayList<Column> getColumnsByIndexes(Table data, int[] idx){
        String[] names = getColumnsName(data, idx);
        ArrayList<Column> cols = new ArrayList<>();
        for (String name : names) {
            cols.add(data.column(name));
        }
        return cols;
    }

    protected Table calculate(Table data, TypeCalculate type) throws IllegalArgumentException{
        Table table = data.copy().dropRowsWithMissingValues();
        switch (type){
            case ABSOLUTE:
                if (groupingWith.length == 0){
                    return table.countBy(columnName);
                }
                else {
                    Table t = getResult(table, count);//TODO - добавить возможные сочетания факторов, но которых нет в конкретном случае
                    return t;
                }
            case PROCENT:
                Table t = calculate(data, TypeCalculate.ABSOLUTE);
                NumericColumn<Double> col = (NumericColumn<Double>) t.column(1);
                col = col.divide(divider.getNumDivider()).multiply(100);
                //col.setPrintFormatter(NumberColumnFormatter.percent(2));
                col.setName("Procent");
                col = round(col,2);
                fixProcents(col);
                t.removeColumns(1);
                t.addColumns(col);
                return t;
            case MEAN:
                return getResult(table, mean);
            case MEDIAN:
                return getResult(table, median);
            default:
                throw new IllegalArgumentException("Unknow type of calculation " + type);
        }
    }

    private Table getResult(Table table, AggregateFunction<?, ?>... functions){
        Table t;
        if (groupingWith.length == 0){
            t = table.summarize(table.column(0),functions).apply();
        }
        else {
            String[] cols = table.columnNames().toArray(new String[0]);
            t = table.summarize(table.categoricalColumn(0),functions).groupBy(cols).apply();
        }
        return t;
    }

    protected String[] getColumnNamesBindingWithQuestion(){
        String[] colNames = new String[groupingWith.length+1];
        List<String> cols = metadata.getAllTable().columnNames();
        colNames[0] = cols.get(column);
        for (int i = 1; i < colNames.length; i++) {
            colNames[i] = cols.get(groupingWith[i-1]);
        }
        return colNames;
    }

    public int getColumn() {
        return column;
    }
}
