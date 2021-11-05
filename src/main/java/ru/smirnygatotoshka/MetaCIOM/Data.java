package ru.smirnygatotoshka.MetaCIOM;

import org.json.JSONArray;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;

public class Data {



    public enum OutputFormats {
        csv,
        xlsx,
    }

    private Table allTable;
    private final String pathToOutputDirectory;
    private final boolean saveFreeAnswers;
    private final boolean toGoogle;
    private final Question[] questions;
    private final OutputFormats format;
    private final int numberOfRespondents;

    public static final String VARIANT_SEPARATOR = ";";

    public int getNumberOfRespondents() {
        return numberOfRespondents;
    }

    public Data(Table allTable, String pathToOutputDirectory, boolean saveFreeAnswers, boolean toGoogle, JSONArray array, OutputFormats format) {
        this.allTable = allTable;
        this.allTable.removeColumns(0);//delete column with fill date
        this.allTable.dropDuplicateRows();
        this.numberOfRespondents = this.allTable.rowCount();//TODO
        this.pathToOutputDirectory = pathToOutputDirectory;
        this.saveFreeAnswers = saveFreeAnswers;
        this.toGoogle = toGoogle;
        this.questions = new Question[array.length()];
        this.format = format;
        for (int i = 0; i < questions.length; i++) {
            this.questions[i] = Question.buildQuestion(array.getJSONObject(i),this);
        }

    }

    public String getPathToOutputDirectory() {
        return pathToOutputDirectory;
    }

    public boolean isSaveFreeAnswers() {
        return saveFreeAnswers;
    }

    public Question[] getQuestions() {
        return questions;
    }

    public OutputFormats getFormat() {
        return format;
    }

    public Table getAllTable() {
        return allTable;
    }

    public boolean isToGoogle() {
        return toGoogle;
    }

    public List<SimpleQuestion> getQuestionsByColumn(int[] groupingWith) {
        ArrayList<SimpleQuestion> grouping = new ArrayList<>();
        for (int i = 0; i < groupingWith.length; i++) {
            for (Question q :questions){
                if (q.getClass() == SimpleQuestion.class){
                    if (((SimpleQuestion) q).getColumn() == groupingWith[i])
                        grouping.add((SimpleQuestion) q);
                }
                else{
                    if ((q.getClass() == FreeAnswerQuestion.class && ((FreeAnswerQuestion) q).getColumn() == groupingWith[i]) ||
                            (q.getClass() == MatrixQuestion.class && ((MatrixQuestion) q).getStartColumn() >= groupingWith[i] &&  ((MatrixQuestion) q).getFinishColumn() <= groupingWith[i]))
                    throw new IllegalArgumentException("Cannot grouping by matrix or free question");
                }
            }
        }
        return grouping;
    }
}
