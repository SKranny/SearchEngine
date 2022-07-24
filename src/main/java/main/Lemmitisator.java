package main;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Lemmitisator {
    private List<String> rusLemms;
    private List<String> engLemms;

    public List<String> getRusLemms() {
        return rusLemms;
    }

    public void setRusLemms(List<String> rusLemms) {
        this.rusLemms = rusLemms;
    }

    public List<String> getEngLemms() {
        return engLemms;
    }

    public void setEngLemms(List<String> engLemms) {
        this.engLemms = engLemms;
    }

    public List<String> getRusLemm(String text) throws IOException {
        LuceneMorphology russianLuceneMorphology = new RussianLuceneMorphology();
        rusLemms = new ArrayList<>();
        text = text.toLowerCase();
        String[] words = text.split("[^а-я]+");
        for (String word : words){
            word = word.toLowerCase();
            if(word.length()> 1 && !russianLuceneMorphology.getMorphInfo(word).toString().contains("ПРЕДЛ") &&
                    !russianLuceneMorphology.getMorphInfo(word).toString().contains("СОЮЗ") &&
                    !russianLuceneMorphology.getMorphInfo(word).toString().contains("МЕЖД") &&
                    !russianLuceneMorphology.getMorphInfo(word).toString().contains("ЧАСТ"))
            {
                List<String> wordBaseForms = russianLuceneMorphology.getNormalForms(word);
                wordBaseForms.forEach(s -> rusLemms.add(s));
            }
        }

        return rusLemms;
    }

    public List<String> getEngLemm(String text) throws IOException {
        LuceneMorphology englishLuceneMorphology = new EnglishLuceneMorphology();
        engLemms = new ArrayList<>();
        text = text.toLowerCase();
        String[] words = text.split("[^a-z]+");
        for (String word : words){
            word = word.toLowerCase();
            if(word.length()> 1 && !englishLuceneMorphology.getMorphInfo(word).toString().contains("ПРЕДЛ") &&
                    !englishLuceneMorphology.getMorphInfo(word).toString().contains("СОЮЗ") &&
                    !englishLuceneMorphology.getMorphInfo(word).toString().contains("МЕЖД") &&
                    !englishLuceneMorphology.getMorphInfo(word).toString().contains("ЧАСТ"))
            {
                List<String> wordBaseForms = englishLuceneMorphology.getNormalForms(word);
                wordBaseForms.forEach(s -> engLemms.add(s));
            }
        }
        return engLemms;
    }
}
