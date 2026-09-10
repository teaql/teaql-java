package io.teaql.core.spi;

/**
 * Internal logger interface for TeaQL framework logging.
 * Replaces System.out.println with a pluggable logging abstraction.
 */
public interface InternalLogger {
    
    enum Level {
        DEBUG, INFO, WARN, ERROR
    }
    
    /**
     * Log a message at the specified level.
     * @param level the log level
     * @param message the message to log
     */
    void log(Level level, String message);
    
    /**
     * Log a message with exception at the specified level.
     * @param level the log level
     * @param message the message to log
     * @param t the exception
     */
    void log(Level level, String message, Throwable t);
    
    /**
     * Check if the specified level is enabled.
     * @param level the log level
     * @return true if enabled
     */
    boolean isLevelEnabled(Level level);
    
    // Convenience methods
    default void debug(String message) {
        if (isLevelEnabled(Level.DEBUG)) log(Level.DEBUG, message);
    }
    
    default void info(String message) {
        if (isLevelEnabled(Level.INFO)) log(Level.INFO, message);
    }
    
    default void warn(String message) {
        if (isLevelEnabled(Level.WARN)) log(Level.WARN, message);
    }
    
    default void warn(String message, Throwable t) {
        if (isLevelEnabled(Level.WARN)) log(Level.WARN, message, t);
    }
    
    default void error(String message) {
        if (isLevelEnabled(Level.ERROR)) log(Level.ERROR, message);
    }
    
    default void error(String message, Throwable t) {
        if (isLevelEnabled(Level.ERROR)) log(Level.ERROR, message, t);
    }
    
    /**
     * Create a logger for the specified category.
     * @param category the logger category (typically class name)
     * @return a new logger instance
     */
    static InternalLogger getLogger(String category) {
        return new InternalLogger() {
            @Override
            public void log(Level level, String message) {
                System.out.println("[" + level + "] [" + category + "] " + message);
            }
            
            @Override
            public void log(Level level, String message, Throwable t) {
                System.out.println("[" + level + "] [" + category + "] " + message);
                if (t != null) {
                    t.printStackTrace(System.out);
                }
            }
            
            @Override
            public boolean isLevelEnabled(Level level) {
                return true; // Default: all levels enabled
            }
        };
    }
    
    /**
     * Create a logger for the specified class.
     * @param clazz the class
     * @return a new logger instance
     */
    static InternalLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getSimpleName());
    }
}
