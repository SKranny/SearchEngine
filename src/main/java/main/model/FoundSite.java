package main.model;

import java.util.List;

public class FoundSite {
    private boolean result;
    private long count;
    private List<Data> data;
    private String error;

    public FoundSite(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public FoundSite(boolean result, long count, List<Data> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public List<Data> getData() {
        return data;
    }

    public void setData(List<Data> data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
