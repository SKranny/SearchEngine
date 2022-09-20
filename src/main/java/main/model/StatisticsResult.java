package main.model;

public class StatisticsResult {
    public StatisticsResult(boolean result, Statistics statistics) {
        this.result = result;
        this.statistics = statistics;
    }

    private boolean result;
    private Statistics statistics;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }
}
