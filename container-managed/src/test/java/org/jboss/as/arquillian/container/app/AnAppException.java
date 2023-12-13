package org.jboss.as.arquillian.container.app;

import jakarta.ejb.ApplicationException;

@ApplicationException
public class AnAppException extends Exception {
    public AnAppException(String message) {
        super(message);
    }

    public AnAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
