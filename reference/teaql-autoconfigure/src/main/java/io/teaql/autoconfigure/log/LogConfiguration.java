package io.teaql.autoconfigure.log;

import io.teaql.core.UserContext;
import io.teaql.core.log.Markers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.InitializingBean;

import io.teaql.core.UserContext;

public class LogConfiguration implements InitializingBean {
    public static final String TRACE_USER_ID = "TRACE_USER_ID";
    static LogConfiguration config;
    Set<String> deniedUrls = new HashSet<>();
    Set<Marker> enabledMarkers = new HashSet<>();
    Map<String, Set<Marker>> userMarkers = new HashMap<>();
    Set<String> enabledAllUsers = new HashSet<>();

    public static LogConfiguration get() {
        return config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        config = this;
        enabledMarkers.add(Markers.SQL_SELECT);
        enabledMarkers.add(Markers.SQL_UPDATE);
        enabledMarkers.add(Markers.SEARCH_REQUEST_START);
        enabledMarkers.add(Markers.SEARCH_REQUEST_END);
    }

    public void enableGlobalMarker(String name) {
        enabledMarkers.add(MarkerFactory.getMarker(name));
    }

    public void disableGlobalMarker(String name) {
        enabledMarkers.remove(MarkerFactory.getMarker(name));
    }

    public void addDeniedUrl(String url) {
        deniedUrls.add(url);
    }

    public void removeDeniedUrl(String url) {
        deniedUrls.remove(url);
    }

    public void enableUserMarker(UserContext ctx, String names) {
        String traceUserId = MDC.get(TRACE_USER_ID);
        if (traceUserId == null) {
            return;
        }
        Set<Marker> markers = this.userMarkers.get(traceUserId);
        if (markers == null) {
            markers = new HashSet<>(enabledMarkers);
            userMarkers.put(traceUserId, markers);
        }

        String[] markerNames = names.split(",");
        for (String markerName : markerNames) {
            markers.add(MarkerFactory.getMarker(markerName));
        }
    }

    public void disableUserMarker(UserContext ctx, String names) {
        String traceUserId = MDC.get(TRACE_USER_ID);
        if (traceUserId == null) {
            return;
        }
        Set<Marker> markers = this.userMarkers.get(traceUserId);
        if (markers == null) {
            markers = new HashSet<>(enabledMarkers);
            userMarkers.put(traceUserId, markers);
        }

        String[] markerNames = names.split(",");
        for (String markerName : markerNames) {
            markers.remove(MarkerFactory.getMarker(markerName));
        }
    }

    public void enableAll(UserContext ctx) {
        String traceUserId = MDC.get(TRACE_USER_ID);
        if (traceUserId == null) {
            return;
        }
        enabledAllUsers.add(traceUserId);
    }

    public void reset(UserContext ctx) {
        String traceUserId = MDC.get(TRACE_USER_ID);
        if (traceUserId == null) {
            return;
        }
        enabledAllUsers.remove(traceUserId);
        userMarkers.remove(traceUserId);
    }
}
