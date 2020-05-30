package ru.liboskat.graphql.security.utils;

/**
 * Класс со вспомогательными методами для строк
 */
public class StringUtils {
    /**
     * @param string строка
     * @return является ли строка null или пустой
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
