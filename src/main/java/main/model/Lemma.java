package main.model;

import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;

@Entity
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String lemma;

    private int frequency;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}
