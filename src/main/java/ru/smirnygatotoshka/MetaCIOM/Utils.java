package ru.smirnygatotoshka.MetaCIOM;

import tech.tablesaw.api.StringColumn;
import tech.tablesaw.columns.Column;

public class Utils {
    public static StringColumn toStringColumn(Column column){
        StringColumn newColumn = StringColumn.create(column.name());
        for (int i = 0;i < column.size();i++)
            newColumn.append(column.get(i).toString());
        return newColumn;
    }

    public static int[] getRange(int start,int finish){
        int[] indexes = new int[finish - start + 1];
        for (int i = start, j = 0; i <= finish; i++,j++) {
            indexes[j] = i;
        }
        return indexes;
    }

}
