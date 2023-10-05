import java.util.*;

public class TasksBean {
    List<Map<String, String>> comparisonDatabases;
    List<String> queries;
    List<String> skipHeader;

    public List<Map<String, String>> getComparisonDatabases() {
        return comparisonDatabases;
    }

    public void setComparisonDatabases(List<Map<String, String>> comparisonDatabases) {
        this.comparisonDatabases = comparisonDatabases;
    }

    public List<String> getQueries() {
        return queries;
    }

    public void setQueries(List<String> queries) {
        this.queries = queries;
    }

    public List<String> getSkipHeader() {
        return skipHeader;
    }

    public void setSkipHeader(List<String> skipHeader) {
        this.skipHeader = skipHeader;
    }


}
