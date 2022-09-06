package edu.caltech.nanodb.server.properties;


public interface PropertyValidator {
    Object validate(Object value);
}
