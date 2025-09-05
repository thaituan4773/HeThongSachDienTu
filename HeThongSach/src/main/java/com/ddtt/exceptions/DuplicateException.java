package com.ddtt.exceptions;

import java.sql.SQLException;
import org.jooq.exception.DataAccessException;

public class DuplicateException extends RuntimeException {

    public DuplicateException(String message) {
        super(message);
    }
    
    public static boolean isDuplicate(DataAccessException e) {
        Throwable cause = e.getCause();
        if (cause instanceof SQLException sqlEx) {
            return "23505".equals(sqlEx.getSQLState()); // unique_violation
        }
        return false;
    }
}
