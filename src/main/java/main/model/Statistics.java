package main.model;
import java.util.List;

public class Statistics {

    public Statistics(Total total, List<Detailed> detailed) {
        this.total = total;
        this.detailed = detailed;
    }
    private Total total;
    private List<Detailed> detailed;

    public Total getTotal() {
        return total;
    }

    public void setTotal(Total total) {
        this.total = total;
    }

    public List<Detailed> getDetailed() {
        return detailed;
    }

    public void setDetailed(List<Detailed> detailed) {
        this.detailed = detailed;
    }
}
