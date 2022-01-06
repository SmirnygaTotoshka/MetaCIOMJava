package ru.smirnygatotoshka.MetaCIOM;

import org.json.JSONObject;

public class QuestionFactory {

    public enum TypeQuestions{
        SIMPLE,
        SIMPLE_MULT,
        MATRIX,
        MATRIX_MULT,
        FREE
    }
    public Question build(Metadata metadata,JSONObject object){
        String t = object.getString("type").toUpperCase();
        TypeQuestions type = TypeQuestions.valueOf(t);
        switch (type){
            case SIMPLE:
                return new SimpleSingleChoiceQuestion().build(metadata, object);
            case SIMPLE_MULT:
                return new SimpleMultipleChoiceQuestion().build(metadata, object);
            case FREE:
                return new FreeAnsweredQuestion().build(metadata, object);
            case MATRIX:
                return new MatrixSingleChoiceQuestion().build(metadata, object);
            case MATRIX_MULT:
                return new MatrixMultipleChoiceQuestion().build(metadata, object);
            default:
                throw new IllegalArgumentException("Неизвестный тип вопроса");

        }
    }
}
