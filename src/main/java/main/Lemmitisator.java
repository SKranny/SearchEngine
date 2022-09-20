package main;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Lemmitisator {
    private List<String> rusLemmas;
    private List<String> engLemmas;

    public List<String> getRusLemmas() {
        return rusLemmas;
    }

    public void setRusLemmas(List<String> rusLemmas) {
        this.rusLemmas = rusLemmas;
    }

    public List<String> getEngLemmas() {
        return engLemmas;
    }

    public void setEngLemmas(List<String> engLemmas) {
        this.engLemmas = engLemmas;
    }

    public List<String> getRusLemm(String text) throws IOException {
        LuceneMorphology russianLuceneMorphology = new RussianLuceneMorphology();
        rusLemmas = new ArrayList<>();
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
                wordBaseForms.forEach(s -> rusLemmas.add(s));
            }
        }

        return rusLemmas;
    }

    public List<String> getEngLemm(String text) throws IOException {
        LuceneMorphology englishLuceneMorphology = new EnglishLuceneMorphology();
        engLemmas = new ArrayList<>();
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
                wordBaseForms.forEach(s -> engLemmas.add(s));
            }
        }
        return engLemmas;
    }
}
