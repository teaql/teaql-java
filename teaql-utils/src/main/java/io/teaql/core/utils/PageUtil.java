package io.teaql.core.utils;

public class PageUtil {

    public static int getStart(int p0, int p1) {
        int pageNo = Math.max(p0, 0);
        int pageSize = Math.max(p1, 0);
        return pageNo * pageSize;
    }

}
