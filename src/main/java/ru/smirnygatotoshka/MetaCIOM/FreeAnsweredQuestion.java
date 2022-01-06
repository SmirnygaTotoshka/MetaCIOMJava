package ru.smirnygatotoshka.MetaCIOM;

import org.json.JSONArray;
import org.json.JSONObject;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

public class FreeAnsweredQuestion extends SimpleSingleChoiceQuestion{

    private boolean needCalculations;

    FreeAnsweredQuestion(){}

    private FreeAnsweredQuestion(boolean needCalculations,Metadata metadata, TypeStatistics[] calculate, String[] answersVariants, String question, Divider divider, int column, int[] groupingWith, boolean hasFreeAnswers) {
        super(metadata, calculate, answersVariants, question, divider,column,groupingWith,hasFreeAnswers);
        this.needCalculations = needCalculations;
    }

    @Override
    public Question build(Metadata metadata, JSONObject question) {
        boolean nCalc = question.getBoolean("calculations");
        if (nCalc) {
            String q = question.getString("question");
            Divider d = Divider.valueOf(question.getString("divider"));
            JSONArray arrayCalculations = question.getJSONArray("statistics");
            if (arrayCalculations.length() == 0) throw new IllegalArgumentException("Нельзя создать экземпляр этого класса без статистик для подсчета");
            TypeStatistics[] c = new TypeStatistics[arrayCalculations.length()];
            for (int i = 0; i < c.length; i++) {
                c[i] = TypeStatistics.valueOf(arrayCalculations.getString(i));
            }
            //exclude case absolute/procent and mean/median meet together
            if (c.length > 2) throw new IllegalArgumentException("Обнаружены несовместимые статитстики. Вместе можно считать либо частоты, либо среднее/медиану");
            else if (c.length == 2){
                if ((in(TypeStatistics.ABSOLUTE,c) || in(TypeStatistics.PROCENT,c)) && (in(TypeStatistics.MEAN,c) || in(TypeStatistics.MEDIAN,c)))
                    throw new IllegalArgumentException("Обнаружены несовместимые статитстики. Вместе можно считать либо частоты, либо среднее/медиану");
            }
            int col = question.getInt("column") - 2; //вычитаем сдвиг для удобства пользователя и удаленный столбец с датой
            if (col < 0) throw new IllegalArgumentException("Неправильный номер столбца");
            JSONArray grouping_with = question.getJSONArray("grouping_with");
            int[] gr = new int[grouping_with.length()];
            for (int i = 0; i < gr.length; i++) {
                gr[i] = grouping_with.getInt(i) - 2;
            }
            return new FreeAnsweredQuestion(nCalc, metadata, c, null, q, d, col, gr, false);
        }
        else {
            int col = question.getInt("column") - 2; //вычитаем сдвиг для удобства пользователя и удаленный столбец с датой
            if (col < 0) throw new IllegalArgumentException("Неправильный номер столбца");
            return new FreeAnsweredQuestion(nCalc, metadata, null, null, question.getString("question"), null, col, null, false);
        }
    }

    @Override
    public Table clean() {
        StringColumn col = metadata.getAllTable().column(column).asStringColumn();
        if (!needCalculations) {
            for (int i = 0; i < col.size(); i++) {
                //col.set(i,censor(col.get(i)));
                col.set(i,col.get(i));
            }
        }
        return Table.create(question,col);
    }

    @Override
    public Table calculate(Table data) {
        if (needCalculations)
            return super.calculate(data);
        else
            return clean().dropRowsWithMissingValues();
    }

    @Override
    public String createForm() {
        return super.createForm();
    }
}
