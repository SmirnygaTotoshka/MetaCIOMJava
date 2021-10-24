package ru.smirnygatotoshka.MetaCIOM;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.smirnygatotoshka.MetaCIOM.io.ExcelIO;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.DataFrameWriter;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
public abstract class Question {

    protected enum TypeCalculate {
        ABSOLUTE,
        PROCENT,
        MEAN,
        MEDIAN
    }

    protected enum Divider{
        RESPONDENTS,//делить на общее число репондентов
        ANSWERED;//делить на число ответивших на этот вопрос
        private int numDivider;

        public int getNumDivider() {
            return numDivider;
        }

        public void setNumDivider(int numDivider) {
            this.numDivider = numDivider;
        }
    }

    private enum TypeQuestions {
        SINGLE,
        MULTIPLE,
        MATRIX_SINGLE,
        MATRIX_MULTIPLE,
        FREE
    }


    protected Data metadata;
    protected boolean multiple;
    protected boolean hasFreeAnswer;
    protected TypeCalculate[] calculate;
    protected int[] groupingWith;
    protected String[] answersVariants;
    protected String question;
    protected Divider divider;

    protected Question(Data metadata, boolean multiple, boolean hasFreeAnswer, TypeCalculate[] calculate, int[] groupingWith, String[] answersVariants, String question,Divider divider) {
        this.metadata = metadata;
        this.multiple = multiple;
        this.hasFreeAnswer = hasFreeAnswer;
        this.calculate = calculate;
        this.groupingWith = groupingWith;
        this.answersVariants = answersVariants;
        this.question = question;
        this.divider = divider;
    }

    public abstract Table calculate(Table data);
    public void save(ArrayList<Table> results) {
        for (Table t : results){
            save(t, true);
        }
    }
    public void save(Table table, boolean append){
        if (metadata.isToGoogle())
            saveToGoogle(table, append);
        else
            saveToLocal(table,append);
    }

    private void saveToGoogle(Table table,boolean append){
        //TODO save to google drive, and other formats
    }

    private void saveToLocal(Table table,boolean append) {
        try {
            String path = getOutputPath();
            DataFrameWriter writer = table.write();
            switch (metadata.getFormat()) {
                case xlsx:
                    ExcelIO.write(table, path, append);
                    break;
                case csv:
                    if (append) throw new IOException("CSV doesnt support more than 1 table in a 1 file");
                    writer.csv(path);
                default:
                    if (append) throw new IOException("TXT doesnt support more than 1 table in a 1 file");
                    writer.toFile(path);
            }
        } catch (IOException e) {
            System.err.println("Cannot save the question " + question);
            e.printStackTrace();
        }
    }

    private String getOutputPath() {
        if (metadata.isToGoogle()){
            //TODO
            return null;
        }
        else {
            return metadata.getPathToOutputDirectory() + File.separator + getFilename();
        }
    }

    public String getFilename() {
        String calcs = "";
        for (int i = 0; i < calculate.length; i++) {
            calcs += calculate[i].name() + "_";
        }
        String filename = question + "_" + calcs + "." + metadata.getFormat().name();
        return filename.replaceAll("[\\\\\\\\/:*?\\\"<>|]","");
    }


    public static Question buildQuestion(JSONObject question, Data metadata) throws IllegalArgumentException {
        String type = question.getString("type");
        String q = question.getString("question");
        Divider d = Divider.valueOf(question.getString("divider"));

        JSONArray arrayAnswers = question.getJSONArray("answers");
        String[] a = new String[arrayAnswers.length()];
        for (int i = 0; i < a.length; i++) {
            a[i] = arrayAnswers.get(i).toString();
        }

        JSONArray arrayCalculations = question.getJSONArray("calculate");
        TypeCalculate[] c = new TypeCalculate[arrayCalculations.length()];
        for (int i = 0; i < c.length; i++) {
            c[i] = TypeCalculate.valueOf(arrayCalculations.getString(i));
        }

        JSONArray grouping_with = question.getJSONArray("grouping_with");
        int[] gr = new int[grouping_with.length()];
        for (int i = 0; i < gr.length; i++) {
            gr[i] = grouping_with.getInt(i)-2;
        }

        boolean hasFreeAnswer = question.getBoolean("has_free_answers");

        if (type.contentEquals(TypeQuestions.SINGLE.name())) {
            int column = question.getInt("column")-2;
            return new SimpleQuestion(q, a, false, hasFreeAnswer,c, column, gr,d, metadata);
        } else if (type.contentEquals(TypeQuestions.MULTIPLE.name())) {
            int column = question.getInt("column")-2;
            return new SimpleQuestion(q, a, true, hasFreeAnswer,c, column, gr,d, metadata);
        } else if (type.contentEquals(TypeQuestions.MATRIX_SINGLE.name())) {
            int start = question.getInt("start")-2;
            int finish = question.getInt("finish")-2;
            return new MatrixQuestion(q, a, false, hasFreeAnswer,c, start, finish, gr,d, metadata);
        } else if (type.contentEquals(TypeQuestions.MATRIX_MULTIPLE.name())) {
            int start = question.getInt("start")-2;
            int finish = question.getInt("finish")-2;
            return new MatrixQuestion(q, a, true, hasFreeAnswer,c, start, finish, gr,d, metadata);
        } else if (type.contentEquals(TypeQuestions.FREE.name())) {
            int column = question.getInt("column")-2;
            return new FreeAnswerQuestion(metadata, q, column);
        } else throw new IllegalArgumentException("Неизвестный тип вопроса.");
    }

    public String getQuestion() {
        return question;
    }

    protected String[] getColumnsName(Table data, int[] cols_indexes) {
        ArrayList<String> names = (ArrayList<String>) data.columnNames();
        String[] n = new String[cols_indexes.length];
        for (int j = 0; j < cols_indexes.length; j++) {
            n[j] = names.get(cols_indexes[j]);
        }
        return n;
    }

    protected DoubleColumn round(NumericColumn<Double> column, int places) {
        DoubleColumn newColumn = DoubleColumn.create(column.name() + "[rounded]", column.size());
        for (int i = 0; i < column.size(); ++i) {
            newColumn.set(i, round(column.getDouble(i), places));
        }

        return newColumn;
    }

    protected Double round(Double num, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(num));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    protected NumericColumn<Double> fixProcents(NumericColumn<Double> column) {
        if (column.sum() == 100)
            return column;
        else {//TODO
            double delta = round(column.sum() - 100, 2);
            double changed = round(column.max() - delta, 2);
            int imax = 0;
            for (int i = 0; i < column.size(); i++) {
                if (column.getDouble(i) > column.getDouble(imax))
                    imax = i;
            }
            column.set(imax, changed);
            return column;
        }
    }

}
