package io.teaql.core.i18n;

public class UnsupportedLocaleException extends IllegalArgumentException {
    private final String localeCode;

    public UnsupportedLocaleException(String localeCode) {
        super("Unsupported locale: " + localeCode);
        this.localeCode = localeCode;
    }

    public String getLocaleCode() { return localeCode; }
}
