package main.model;

import javax.persistence.*;

@Entity
public class Search_index {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private int page_id;

    private int lemma_id;

    private float rating;

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getPage_id() {
        return page_id;
    }

    public void setPage_id(int page_id) {
        this.page_id = page_id;
    }

    public int getLemma_id() {
        return lemma_id;
    }

    public void setLemma_id(int lemma_id) {
        this.lemma_id = lemma_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
