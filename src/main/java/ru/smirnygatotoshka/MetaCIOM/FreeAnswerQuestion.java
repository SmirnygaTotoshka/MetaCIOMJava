package ru.smirnygatotoshka.MetaCIOM;

import tech.tablesaw.api.Table;

public class FreeAnswerQuestion extends Question{

    private final int column;

    public FreeAnswerQuestion(Data metadata,String q, int column) {
        super(metadata,false,true,null,null,null,q,null);
        this.column = column;
    }



    @Override
    public String getFilename() {
        String filename = question + "." + metadata.getFormat().name();
        return filename.replaceAll("[\\\\\\\\/:*?\\\"<>|]","");
    }

    @Override
    public Table calculate(Table data) {
        Table table = data.select(data.column(0));
        table.setName(data.column(0).name());
        table = table.dropRowsWithMissingValues().dropDuplicateRows();
        return table;
    }
    public int getColumn() {
        return column;
    }
}
