package ru.smirnygatotoshka.MetaCIOM;

import org.json.JSONObject;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Базовый класс, описывающий бизнес-логику обработки/создания опроса
 *
 * @author SmirnygaTotoshka
 * */
public abstract class Question {

    Question() {

    }

    /**
     * Описывает перечень статистик, которые могут быть посчитаны для вопроса
     * Absolute - абсолютная частота
     * Procent - относительная частота
     * Mean - среднее
     * Median - медиана
     * */
    protected enum TypeStatistics {
        ABSOLUTE,
        PROCENT,
        MEAN,
        MEDIAN
    }

    /**
     * Сигнализирует на что делить при подсчете относительной частоты
     * RESPONDENTS - делить на общее число репондентов (общее число строк в общей таблице)
     * ANSWERED - делить на число, ответивших на этот вопрос
     * */
    protected enum Divider{
        RESPONDENTS,

        ANSWERED;
        private int numDivider;

        public int getNumDivider() {
            return numDivider;
        }

        public void setNumDivider(int numDivider) {
            this.numDivider = numDivider;
        }
    }
    /**
     * Переменная, содержащая в себе общие сведения об опросе
     * @see Metadata
     * */
    protected Metadata metadata;

    /**
     * @see TypeStatistics
     * */
    protected TypeStatistics[] statistics;
    /**
     * Возможные варианты ответа на вопрос. Пустой массив, если вопрос с свободным ответом
     * */
    protected String[] answersVariants;
    /**
     * Формулировка вопроса
     * */
    protected String question;

    /**
     * @see Divider
     */
    protected Divider divider;

    protected boolean hasFreeAnswers;



    protected Question(Metadata metadata, TypeStatistics[] calculate, String[] answersVariants, String question, Divider divider,boolean hasFreeAnswers) {
        this.metadata = metadata;
        this.statistics = calculate;
        this.answersVariants = answersVariants;
        this.question = question;
        this.divider = divider;
        this.hasFreeAnswers = hasFreeAnswers;
    }

    /**
     * Создает экземпляр объекта из его мета-описания на языке JSON
     * */
    public abstract Question build(Metadata metadata, JSONObject question);

    /**
     * Подготавливает необходимые данные к расчёту статистик
     * */
    public abstract Table clean();

    /**
     * Рассчитывает необходимые статистики по данному вопросу
     * */
    public abstract Table calculate(Table data);

    /**
     * Возвращает свободные ответы, если они имеются
     * */
    public abstract String[] getFreeAnswers();

    /**
     * Создает строку с REST API запросом для создания поля в гугл-форме с данным вопросом
     * */
    public abstract String createForm();


    /**
     * Округляет до нужного числа позиций <b>стобец</b>
     * */
    protected DoubleColumn round(NumericColumn<Double> column, int places) {
        DoubleColumn newColumn = DoubleColumn.create(column.name() + "[rounded]", column.size());
        for (int i = 0; i < column.size(); ++i) {
            newColumn.set(i, round(column.getDouble(i), places));
        }

        return newColumn;
    }
    /**
     * Округляет до нужного числа позиций <b>одно число</b>
     * */
    private Double round(Double num, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(num));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Проверяет, является ли сумма относительных частот в столбце равной 100%.
     * Если нет, исправляет погрешность округления путем прибавления к минимальному значению излишка максимального
     * */
    protected DoubleColumn fixProcents(DoubleColumn column) {
        if (column.sum() == 100)
            return column;
        else {
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
