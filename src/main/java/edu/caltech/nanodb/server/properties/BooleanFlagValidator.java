package edu.caltech.nanodb.server.properties;


/**
 * This validator handles properties that are Boolean flags.  These properties
 * can be set using a string (yes/no, on/off, true/false) or an integer (1/0),
 * as well as a Boolean value.  A {@code null} value is not permitted.
 */
public class BooleanFlagValidator implements PropertyValidator {
    @Override
    public Object validate(Object value) {
        if (value == null)
            throw new PropertyException("Property value cannot be NULL");

        if (value instanceof Boolean)
            return value;  // Yay boolean!

        if (value instanceof String) {
            String s = ((String) value).trim().toLowerCase();

            if ("off".equals(s) || "false".equals(s) || "no".equals(s)) {
                value = Boolean.FALSE;
            }
            else if ("on".equals(s) || "true".equals(s) || "yes".equals(s)) {
                value = Boolean.TRUE;
            }
            else {
                throw new PropertyException("Unrecognized string value \"" +
                    value + "\" for Boolean property.  Accepted values are " +
                    "on/off, true/false, yes/no (case insensitive).");
            }
        }
        else if (value instanceof Number) {
            int n = ((Number) value).intValue();

            if (n == 0) {
                value = Boolean.FALSE;
            }
            else if (n == 1) {
                value = Boolean.TRUE;
            }
            else {
                throw new PropertyException("Unrecognized int value " + n +
                    " for Boolean property.  Accepted values are 1/0.");
            }
        }
        else {
            throw new PropertyException("Unrecognized value-type " +
                value.getClass().getName() + " for Boolean property.");
        }

        return value;
    }
}
