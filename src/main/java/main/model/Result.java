package main.model;

public class Result {

    private boolean result;

    private String error;

    public Result(boolean result) {
        this.result = result;
    }

    public Result(String error) {
        this.error = error;
    }

    public Result(boolean result, String error) {
        this.result = result;
        this.error = error;
    }


    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
