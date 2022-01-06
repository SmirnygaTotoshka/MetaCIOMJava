package ru.smirnygatotoshka.MetaCIOM;

import org.json.JSONArray;
import org.json.JSONObject;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

public class MatrixSingleChoiceQuestion extends SimpleSingleChoiceQuestion{

    protected int startColumn;
    protected int finishColumn;

    MatrixSingleChoiceQuestion(){}

    protected MatrixSingleChoiceQuestion(Metadata metadata, TypeStatistics[] calculate, String[] answersVariants, String question, Divider divider, int[] groupingWith, int startColumn, int finishColumn) {
        super(metadata, calculate, answersVariants, question, divider, -1, groupingWith, false);
        this.startColumn = startColumn;
        this.finishColumn = finishColumn;
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
        if (arrayCalculations.length() != 1) throw new IllegalArgumentException("Нельзя создать экземпляр этого класса без статистик для подсчета");
        TypeStatistics[] c = new TypeStatistics[1];
        c[0] = TypeStatistics.valueOf(arrayCalculations.getString(0));

        int start = question.getInt("start_column") - 2; //вычитаем сдвиг для удобства пользователя и удаленный столбец с датой
        int finish = question.getInt("finish_column") - 2;

        if (start > finish || start < 0 || finish < 0)
            throw new IllegalArgumentException("Неправильные номера столбцов");

        int[] gr = new int[0];

        return new MatrixSingleChoiceQuestion(metadata, c, a, q, d, gr, start,finish);
    }

    @Override
    public Table clean() {
        return metadata.getAllTable().select(getNeededColumnsIndexes()).dropRowsWithMissingValues();
    }

    @Override
    public Table calculate(Table data) {
        String[] categoryNames = getCategoriesNames();
        Table[] ansByCat = new Table[categoryNames.length];
        for (int i = 0; i < data.columnCount(); i++) {
            Table t = data.selectColumns(data.column(i)).copy();
            //columnName = columnNames[i];
            ansByCat[i] = super.calculate(t);
        }
        return formReport(ansByCat);
    }

    @Override
    public String createForm() {
        return super.createForm();
    }

    @Override
    protected String[] getNeededColumnsIndexes() {
        String[] idx = new String[finishColumn - startColumn + 1];
        for (int j = 0,i = startColumn; i <= finishColumn;i++,j++)
            idx[j] = metadata.getAllTable().column(i).name();
        return idx;
    }

    /**
     *
     * */
    public String[] getCategoriesNames(){
        String[] cat = new String[finishColumn - startColumn + 1];
        for (int i = 0,j = startColumn; i < cat.length; i++, j++) {
            String c = metadata.getAllTable().column(j).name();
            cat[i] = c.substring(c.indexOf("[") + 1, c.indexOf("]"));
        }
        return cat;
    }

    protected Table formReport(Table[] tables){
        String[] categoryNames = getCategoriesNames();
        Table report = Table.create(question);
        if (statistics[0] == TypeStatistics.MEAN || statistics[0] == TypeStatistics.MEDIAN){
            for (int i = 0; i < tables.length; i++) {
                Column c = tables[i].column(0);
                c.setName(categoryNames[i]);
                report.insertColumn(i,c);
            }
        }
        else if(statistics[0] == TypeStatistics.ABSOLUTE || statistics[0] == TypeStatistics.PROCENT){
            StringColumn levels = StringColumn.create("Уровни фактора", answersVariants);
            report.insertColumn(0,levels);
            for (int i = 0; i < tables.length; i++) {
                StringColumn column =  StringColumn.create(categoryNames[i]);
                for (int j = 0;j < answersVariants.length;j++) {
                    StringColumn l = tables[i].column(0).asStringColumn();
                    if (l.contains(answersVariants[j])){
                        Table factors = tables[i].where(l.isEqualTo(answersVariants[j]));
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
}
