package io.teaql.core.value;

public class TeaQLNotLoadedException extends IllegalStateException {
    private final String root;
    private final String accessPath;
    private final String breakPoint;

    public TeaQLNotLoadedException(String root, String accessPath, String breakPoint) {
        super("TeaQLNotLoadedError: root=" + root + " access_path=" + accessPath
                + " break_point=" + breakPoint + " — select the missing field or relation");
        this.root = root; this.accessPath = accessPath; this.breakPoint = breakPoint;
    }
    public String getRoot() { return root; }
    public String getAccessPath() { return accessPath; }
    public String getBreakPoint() { return breakPoint; }
}
