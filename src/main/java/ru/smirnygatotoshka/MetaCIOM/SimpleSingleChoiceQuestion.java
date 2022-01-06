package ru.smirnygatotoshka.MetaCIOM;

import org.json.JSONArray;
import org.json.JSONObject;
import tech.tablesaw.aggregate.AggregateFunction;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.strings.StringColumnType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tech.tablesaw.aggregate.AggregateFunctions.*;

/**
 * Класс для работы с простыми, не табличными вопросами с единственным выбором варианта ответа
 * @author SmirnygaTotoshka
 * */
public class SimpleSingleChoiceQuestion extends Question{

    protected final String UNDEFINED_VALUE = "Другое";

    protected int column;
    protected int[] groupingWith;

    protected SimpleSingleChoiceQuestion(Metadata metadata, TypeStatistics[] calculate, String[] answersVariants, String question, Divider divider, int column, int[] groupingWith, boolean hasFreeAnswers) {
        super(metadata, calculate, answersVariants, question, divider,hasFreeAnswers);
        this.column = column;
        this.groupingWith = groupingWith;
    }

    public SimpleSingleChoiceQuestion() {
        super();
    }

    @Override
    public Question build(Metadata metadata, JSONObject question) {
        String q = question.getString("question");
        Divider d = Divider.valueOf(question.getString("divider"));

        //парсинг массивов
        JSONArray arrayAnswers = question.getJSONArray("answers");
        if (arrayAnswers.length() == 0) throw new IllegalArgumentException("Нельзя создать экземпляр этого класса без вариантов ответа");
        String[] a = new String[arrayAnswers.length()];
        for (int i = 0; i < a.length; i++) {
            a[i] = arrayAnswers.get(i).toString();
        }

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

        boolean has_free_answers = question.getBoolean("has_free_answers");

        return new SimpleSingleChoiceQuestion(metadata, c, a, q, d, col, gr, has_free_answers);
    }

    @Override
    public Table clean() {
        String[] idx = getNeededColumnsIndexes();
        Table subset = metadata.getAllTable().select(idx);
        StringColumn col = subset.column(subset.columnCount() - 1).asStringColumn();
        for (int i = 0; i < col.size(); i++) {
            String value = col.get(i);
            if (value.isEmpty())
                continue;
            if (!in(value, answersVariants) && hasFreeAnswers){
                col.set(i, UNDEFINED_VALUE);
            }
            if (!in(value, answersVariants) && !hasFreeAnswers){
                throw new IllegalStateException("Нет такого варианта ответа. Проверьте метадату/данные. Value = " + value);
            }
        }
        return subset.replaceColumn(subset.columnCount()-1,col).dropRowsWithMissingValues();
    }


    @Override
    public Table calculate(Table data) {
        changeDivider(data);
        Table summary = Table.create(question);
        for (int i = 0; i < statistics.length; i++) {
            Table res = calculate(data, statistics[i]);
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

    @Override
    public String createForm() {
        return null;
    }
    
    /**
     * Возвращает свободные ответы на вопрос
     * */
    public String[] getFreeAnswers(){
        if (!hasFreeAnswers)
            return new String[0];
        else {
            StringColumn col = metadata.getAllTable().column(column).asStringColumn();
            ArrayList<String> ans = new ArrayList<>();
            for (int i = 0; i < col.size(); i++) {
                if (!in(col.get(i),answersVariants))
                    //ans.add(censor(col.get(i)));
                    ans.add(col.get(i).trim());
            }
            return ans.toArray(new String[0]);
        }
    }

    /**
     * Непосредственный расчет статистики
     * */
    protected Table calculate(Table data, TypeStatistics type) throws IllegalArgumentException{
        Table table = data.copy();

        switch (type){
            case ABSOLUTE:
                if (groupingWith.length == 0){
                    String colName = table.columnNames().get(table.columnCount()-1);
                    return table.countBy(colName);
                }
                else {
                    String[] cols = table.columnNames().toArray(new String[0]);
                    return table.summarize(table.categoricalColumn(table.columnCount()-1),count).groupBy(cols).apply();
                }
            case PROCENT:
                Table t = calculate(data, TypeStatistics.ABSOLUTE);
                DoubleColumn col = t.intColumn(t.columnCount()-1).asDoubleColumn();
                col = col.divide(divider.getNumDivider()).multiply(100);
                //col.setPrintFormatter(NumberColumnFormatter.percent(2));
                col.setName("Procent");
                col = round(col,2);
                fixProcents(col);
                t.removeColumns(t.columnCount()-1);
                t.addColumns(col);
                return t;
            case MEAN:
                DoubleColumn dc = StringToDouble(table.stringColumn(table.columnCount()-1));
                if (groupingWith.length == 0){
                    return table.summarize(dc,mean).apply();
                }
                else {
                    String[] cols = table.columnNames().toArray(new String[0]);
                    return table.summarize(dc,mean).groupBy(cols).apply();
                }
            case MEDIAN:
                DoubleColumn dc1 = StringToDouble(table.stringColumn(table.columnCount()-1));
                if (groupingWith.length == 0){
                    return table.summarize(dc1,median).apply();
                }
                else {
                    String[] cols = table.columnNames().toArray(new String[0]);
                    return table.summarize(dc1,median).groupBy(cols).apply();
                }
            default:
                throw new IllegalArgumentException("Unknow type of calculation " + type);
        }
    }
    /**
     * @return Массив положений столбцов, которые связаны с этим вопросом
     * */
    protected String[] getNeededColumnsIndexes() {
        String[] idx = new String[groupingWith.length + 1];
        for (int i = 0; i < groupingWith.length; i++) {
            idx[i] = metadata.getAllTable().column(groupingWith[i]).name();
        }
        idx[groupingWith.length] = metadata.getAllTable().column(column).name();
        return idx;
    }

    private DoubleColumn StringToDouble(StringColumn col){
        DoubleColumn dc = DoubleColumn.create(col.name());
        for (String s : col){
            if (StringColumnType.valueIsMissing(s))
                dc.appendMissing();
            else
                dc.append(Double.parseDouble(s));
        }
        return dc;
    }

    /**
     * Входит ли строка в массив строк
     * */
    public boolean in(String s, String[] array){
        for (int i = 0; i < array.length; i++) {
            if (array[i].trim().contentEquals(s.trim()))
                return true;
        }
        return false;
    }

    public boolean in(Object o, Object[] array){
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(o))
                return true;
        }
        return false;
    }


    /**
     * Установление делителя
     * */
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
    /**
     * TODO - срабатывает на учеба, внеучебный
     * @author Артем Власов and SmirnygaTotoshka
     * */
   /* protected String censor(String in){
        Pattern mat = Pattern.compile("\\w*сук\\w*|\\w*[её]б\\w*|\\w*бля\\w*|\\w*наху\\w*|\\w*ху[^дж]\\w*|\\w*ху[^дж]|\\w*муд\\w*|\\w*пизд\\w*|\\w+уеть");
        Matcher matcher = mat.matcher(in.toLowerCase());
        return matcher.replaceAll("***");
    }*/
}
