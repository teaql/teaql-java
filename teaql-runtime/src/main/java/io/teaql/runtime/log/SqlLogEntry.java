package io.teaql.runtime.log;

public class SqlLogEntry {
    private final String prettySql;
    private final long elapsedUs;
    private final String resultSummary;

    public SqlLogEntry(String prettySql, long elapsedUs, String resultSummary) {
        this.prettySql = prettySql;
        this.elapsedUs = elapsedUs;
        this.resultSummary = resultSummary;
    }

    public String getPrettySql() {
        return prettySql;
    }

    public long getElapsedUs() {
        return elapsedUs;
    }

    public String getResultSummary() {
        return resultSummary;
    }
}
