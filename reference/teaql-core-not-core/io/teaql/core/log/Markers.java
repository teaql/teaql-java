package io.teaql.core.log;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public interface Markers {
    Marker SEARCH_REQUEST_START = MarkerFactory.getMarker("SEARCH_REQUEST_START");
    Marker SEARCH_REQUEST_END = MarkerFactory.getMarker("SEARCH_REQUEST_END");
    Marker SQL_SELECT = MarkerFactory.getMarker("SQL_SELECT");
    Marker SQL_UPDATE = MarkerFactory.getMarker("SQL_UPDATE");
    Marker HTTP_REQUEST = MarkerFactory.getMarker("HTTP_REQUEST");
    Marker HTTP_SHORT_REQUEST = MarkerFactory.getMarker("HTTP_SHORT_REQUEST");
    Marker HTTP_RESPONSE = MarkerFactory.getMarker("HTTP_RESPONSE");
    Marker HTTP_SHORT_RESPONSE = MarkerFactory.getMarker("HTTP_SHORT_RESPONSE");
}
