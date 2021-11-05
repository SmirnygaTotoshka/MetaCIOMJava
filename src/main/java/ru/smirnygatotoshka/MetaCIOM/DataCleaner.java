package ru.smirnygatotoshka.MetaCIOM;

import tech.tablesaw.api.CategoricalColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.Arrays;
import java.util.List;

public class DataCleaner {

    public Table[] clean(Question question){
        if (isValid(question)) {
            if (question.getClass() == SimpleQuestion.class)
                return new Table[]{cleanSimple((SimpleQuestion) question)};
            else if (question.getClass() == MatrixQuestion.class)
                return cleanMatrix((MatrixQuestion) question);
            else
                return new Table[]{cleanFree((FreeAnswerQuestion) question)};
        }
        else throw new IllegalArgumentException("Invalid question description " + question.question);
    }

    private boolean isValid(Question question) {
        if (question.groupingWith != null) {
            if (question.groupingWith.length > 1 && question.getClass() == MatrixQuestion.class) {
                System.err.println("Too many grouping factors for " + question.question);
                return false;
            }
            if (factorInMatrix(question)) {
                System.err.println("Grouping factor(col_nums=" + Arrays.toString(question.groupingWith) + "is part of a matrix " + question.question);
                return false;
            }
        }
        return true;
    }

    private boolean factorInMatrix(Question ref) {
        Question[] questions = ref.metadata.getQuestions();
        for (Question q : questions){
            if (!q.equals(ref)){
                if (q.getClass() == MatrixQuestion.class){
                    for (int i = 0;i < ref.groupingWith.length;i++){
                        if (i >= ((MatrixQuestion) q).getStartColumn() && i <= ((MatrixQuestion) q).getFinishColumn())
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private Table cleanFree(FreeAnswerQuestion question) {
        StringColumn strCol = question.metadata.getAllTable().column(question.getColumn()).asStringColumn();
        strCol.setName(question.question);
        return Table.create(question.question,strCol).dropRowsWithMissingValues().dropDuplicateRows();
    }

    private Table[] cleanMatrix(MatrixQuestion question) {
        Table[] clean = new Table[getNumGroupingFactors(question)];
        Table matrix = question.metadata.getAllTable().selectColumns(question.getColumnNames());
        String[] levels = null;
        if (question.groupingWith.length > 0)
            levels = question.metadata.getAllTable().categoricalColumn(question.groupingWith[0]).removeMissing().unique().asStringColumn().asObjectArray();
        else
            levels = new String[]{""};
        for (int lev = 0; lev < levels.length; lev++){
            Table res = Table.create(question.question + "_" + levels[lev]);
            for (int i = 0; i < matrix.columnCount(); i++) {
                StringColumn strCol = matrix.column(i).asStringColumn();
                strCol.setName(matrix.column(i).name());
                Column col = matrix.column(i).copy();
                for (int j = 0; j < strCol.size(); j++) {
                    if (question.multiple){
                        String[] vars = strCol.getString(i).split(Data.VARIANT_SEPARATOR);
                        for (String v : vars){
                            if (!in(v, question.answersVariants)) {
                                System.err.println(question.question + " recognize bad variant.");
                                throw new IllegalArgumentException(question.question + " recognize bad variant.");
                            }
                        }
                    }
                    else {
                        if (!in(strCol.get(i), question.answersVariants)) {
                            System.err.println(question.question + " recognize bad variant.");
                            throw new IllegalArgumentException(question.question + " recognize bad variant.");
                        }
                    }
                }
                res.addColumns(col);
            }
            clean[lev] = res;
        }
        return clean;
    }

    private int getNumGroupingFactors(Question question) {
        int num = 1;
        List<String> colNames =  question.metadata.getAllTable().columnNames();
        for (int i = 0;i < question.groupingWith.length;i++){
            String colName = colNames.get(question.groupingWith[i]);
            CategoricalColumn categoricalColumn = question.metadata.getAllTable().selectColumns(colName).dropRowsWithMissingValues().categoricalColumn(colName);
            num *= categoricalColumn.countUnique();
        }
        return num;
    }

    private Table cleanSimple(SimpleQuestion question) {
        Table subset = question.metadata.getAllTable().selectColumns(question.getColumnNamesBindingWithQuestion());
        StringColumn strCol = subset.column(0).asStringColumn();
        strCol.setName(subset.column(0).name());
        FreeVariants variants = new FreeVariants(question);
        for (int i = 0;i < subset.rowCount();i++){
            if (question.multiple){
                String ans = subset.get(i,0).toString();
                String[] vars = ans.split(Data.VARIANT_SEPARATOR);
                for (String v : vars){
                    if ((!in(v, question.answersVariants) && question.hasFreeAnswer) ||
                            (!in(v, question.answersVariants) && question.hasFreeAnswer)) {
                        variants.add(v);
                        strCol.set(i,ans.replaceAll(v + Data.VARIANT_SEPARATOR,""));

                    } else if (!in(v, question.answersVariants) && !question.hasFreeAnswer) {
                        System.err.println("Has error in data/metadata, Has Free Answer? in " + question.question);
                        strCol.set(i,strCol.get(i).replaceAll(v + Data.VARIANT_SEPARATOR,""));
                    }
                }
            }
            else{
                if ((!in(strCol.get(i), question.answersVariants) && question.hasFreeAnswer) ||
                        (!in(strCol.get(i), question.answersVariants) && question.hasFreeAnswer)) {
                    variants.add(strCol.get(i));
                    strCol.setMissing(i);

                } else if (!in(strCol.get(i), question.answersVariants) && !question.hasFreeAnswer) {
                    System.err.println("Has error in data/metadata, Has Free Answer? in " + question.question);
                    strCol.setMissing(i);
                }
            }
        }
        if (question.metadata.isSaveFreeAnswers()){
            variants.save();
        }
        return subset.replaceColumn(0,strCol).dropRowsWithMissingValues();
    }


    public boolean in(String s, String[] array){
        for (int i = 0; i < array.length; i++) {
            if (array[i].trim().contentEquals(s.trim()))
                return true;
        }
        return false;
    }
    
}
