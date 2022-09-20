package main.model;

import main.model.Page;

public class ExtendedPage {
    private Page page;

    private float absoluteRelevancy;

    private float relativeRelevance;

    public ExtendedPage(Page page, float relevancy) {
        this.page = page;
        this.absoluteRelevancy = relevancy;
    }

    public float getRelativeRelevance() {
        return relativeRelevance;
    }

    public void setRelativeRelevance(float relativeRelevance) {
        this.relativeRelevance = relativeRelevance;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public float getAbsoluteRelevancy() {
        return absoluteRelevancy;
    }

    public void setAbsoluteRelevancy(float absoluteRelevancy) {
        this.absoluteRelevancy = absoluteRelevancy;
    }
}
