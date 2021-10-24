package ru.smirnygatotoshka.MetaCIOM;

import java.io.*;
import java.util.ArrayList;

public class FreeVariants{
    private ArrayList<String> variants;
    private Question question;
    private String path;

    public FreeVariants(SimpleQuestion question) {
        variants = new ArrayList<>();
        this.question = question;
        if (question.metadata.isToGoogle()){
            //TODO
        }
        else {
            path = question.metadata.getPathToOutputDirectory() + File.separator + question.question.replaceAll("[\\\\\\\\/:*?\\\"<>|]","") + "_freeAns.txt";
        }
    }

    public void add(String var){
        variants.add(var);
    }

    public void save(){
        if (variants.size() == 0)
            System.out.println(question.question + " не имеет свободных ответов.\n");
        else{
            if (question.metadata.isToGoogle()){
                saveToGoogle();
            }
            else {
                saveToLocal();
            }
        }
    }

    private void saveToGoogle() {
        //TODO mat filter
    }

    private void saveToLocal() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            for (String line : variants){
                writer.write(line);
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            System.err.println("Can`t save free variants for " + question.question);
            e.printStackTrace();
        }
    }
}
