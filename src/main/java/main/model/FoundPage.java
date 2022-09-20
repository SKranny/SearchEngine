package main.model;

public class FoundPage {
    private String uri;
    private String title;
    private float relevancy;
    private String snippet;

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public float getRelevancy() {
        return relevancy;
    }

    public void setRelevancy(float relevancy) {
        this.relevancy = relevancy;
    }
}
