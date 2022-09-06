package edu.caltech.nanodb.server.properties;


import edu.caltech.nanodb.expressions.TypeConverter;


/**
 * Validates a string property value; if the value cannot be cast to a string
 * then an error will be reported.
 */
public class StringValueValidator implements PropertyValidator {
    @Override
    public Object validate(Object value) {
        if (value == null)
            throw new PropertyException("Property value cannot be NULL");

        return TypeConverter.getStringValue(value);
    }
}
