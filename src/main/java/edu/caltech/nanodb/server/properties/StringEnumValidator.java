package edu.caltech.nanodb.server.properties;


import edu.caltech.nanodb.expressions.TypeConverter;


/**
 * Validates a string property value against a collection of valid values; if
 * the value cannot be cast to a string or the value doesn't appear in the
 * collection of valid values, then an error will be reported.
 */
public class StringEnumValidator implements PropertyValidator {
    /** An array of valid values for the string enumeration. */
    private String[] validValues;


    public StringEnumValidator(String[] validValues) {
        if (validValues == null)
            throw new IllegalArgumentException("validValues cannot be null");

        if (validValues.length == 0) {
            throw new IllegalArgumentException(
                "validValues must contain at least one value");
        }

        this.validValues = validValues;
    }

    @Override
    public Object validate(Object value) {
        if (value == null)
            throw new PropertyException("Property value cannot be NULL");

        String strValue = TypeConverter.getStringValue(value);

        boolean found = false;
        for (String v : validValues) {
            if (strValue.equals(v))
                found = true;
        }

        if (!found) {
            StringBuilder buf = new StringBuilder();
            boolean first = true;

            for (String v : validValues) {
                if (first)
                    first = false;
                else
                    buf.append(", ");

                buf.append(v);
            }

            throw new PropertyException(String.format(
                "Value \"%s\" is unrecognized.  Valid values are:  %s",
                strValue, buf.toString()));
        }

        return strValue;
    }
}
