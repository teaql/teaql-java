package io.teaql.core.spi;

/**
 * SPI interface for providing custom logger implementations.
 * Implementations can integrate with slf4j, log4j2, or other logging frameworks.
 */
public interface LoggerProvider {
    
    /**
     * Create a logger for the specified category.
     * @param category the logger category (typically class name)
     * @return a logger instance
     */
    InternalLogger getLogger(String category);
    
    /**
     * Create a logger for the specified class.
     * @param clazz the class
     * @return a logger instance
     */
    default InternalLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getSimpleName());
    }
}
