package ru.smirnygatotoshka.MetaCIOM;

import tech.tablesaw.api.Table;

public class FreeAnswerQuestion extends SimpleQuestion{

    private final int column;
    private boolean need_calc;
    public FreeAnswerQuestion(Data metadata,String q, int column,boolean calculate) {
        //TODO
        super(q,null,false,false,new TypeCalculate[]{Question.TypeCalculate.ABSOLUTE},column,null,Divider.RESPONDENTS,metadata);
        this.column = column;
        this.need_calc = calculate;
    }



    @Override
    public String getFilename() {
        String filename = question + "." + metadata.getFormat().name();
        return filename.replaceAll("[\\\\\\\\/:*?\\\"<>|]","");
    }

    @Override
    public Table calculate(Table data) {
        Table table = data.selectColumns(data.column(0));
        table.setName(data.column(0).name());
        if (need_calc){
            return super.calculate(table,this.calculate[0]);
        }
        else {
            table = table.dropRowsWithMissingValues().dropDuplicateRows();
            return table;
        }
    }
    public int getColumn() {
        return column;
    }
}
