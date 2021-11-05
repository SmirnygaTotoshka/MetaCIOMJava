package ru.smirnygatotoshka.MetaCIOM;

import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.List;

public class MatrixQuestion extends SimpleQuestion {
    public int getStartColumn() {
        return startColumn;
    }

    public int getFinishColumn() {
        return finishColumn;
    }

    private final int startColumn;
    private final int finishColumn;
    private String[] categoryNames;
    private String[] columnNames;

    public MatrixQuestion(String q, String[] a, boolean multiple, boolean hasFreeAnswer, TypeCalculate[] c, int start, int finish, int[] gr,Divider divider, Data metadata) {
        super(q, a, multiple, hasFreeAnswer, c, gr, divider, metadata);
        if (c.length != 1) throw new IllegalStateException("For MatrixQuestion can calculate one TypeCalculation");
        if (hasFreeAnswer) throw new IllegalStateException("For MatrixQuestion cannot be free answers.");
        this.startColumn = start;
        this.finishColumn = finish;
        this.columnNames = getColumnsName(metadata.getAllTable(), Utils.getRange(startColumn,finishColumn));
        this.categoryNames = getCategoryNames();
    }

    @Override
    public Table calculate(Table data) {
        Table[] ansByCat = new Table[categoryNames.length];
        for (int i = 0; i < data.columnCount(); i++) {
            Table t = data.selectColumns(data.column(i)).copy().dropRowsWithMissingValues();
            columnName = columnNames[i];
            ansByCat[i] = super.calculate(t);
        }
        Table report = Table.create(question);
        if (calculate[0] == TypeCalculate.MEAN || calculate[0] == TypeCalculate.MEDIAN){
            for (int i = 0; i < ansByCat.length; i++) {
                Column c = ansByCat[i].column(0);
                c.setName(categoryNames[i]);
                report.insertColumn(i,c);
            }
        }
        else if(calculate[0] == TypeCalculate.ABSOLUTE || calculate[0] == TypeCalculate.PROCENT){
            StringColumn levels = StringColumn.create("Уровни фактора", answersVariants);
            report.insertColumn(0,levels);
            for (int i = 0; i < ansByCat.length; i++) {
                StringColumn column =  StringColumn.create(categoryNames[i]);
                for (int j = 0;j < answersVariants.length;j++) {
                    StringColumn l = Utils.toStringColumn(ansByCat[i].column(0));
                    if (l.contains(answersVariants[j])){
                        Table factors = ansByCat[i].where(l.isEqualTo(answersVariants[j]));
                        column.append(factors.getString(0,1));
                    }
                    else{
                        column.append("0");
                    }
                }
                report.insertColumn(i+1, column);
            }
        }
        return report;
    }

    public String[] getCategoryNames(){
        String[] cat = new String[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            String c = columnNames[i];
            cat[i] = c.substring(c.indexOf("[") + 1, c.indexOf("]"));
        }
        return cat;
    }

    @Override
    protected String[] getColumnNamesBindingWithQuestion() {
        String[] colNames = new String[groupingWith.length + finishColumn - startColumn + 1];
        List<String> cols = metadata.getAllTable().columnNames();
        int c = 0;
        for (int i = startColumn; i <= finishColumn;i++,c++) {
            colNames[c] = cols.get(i);
        }
        for (int i = 0; i < colNames.length;i++,c++) {
            colNames[c] = cols.get(groupingWith[i]);
        }
        return colNames;
    }

    public String[] getColumnNames() {
        return columnNames;
    }
}
