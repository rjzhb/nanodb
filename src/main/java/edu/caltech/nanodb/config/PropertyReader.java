package edu.caltech.nanodb.config;


import java.util.Properties;


/**
 * This class provides helper operations for reading typed properties out of
 * Java {@link java.util.Properties} files, or out of the Java environment.
 */
public class PropertyReader {

    /**
     * Some properties can be a number plus a scale, such as "1024m" or "1G".
     * This helper function pulls the scale indicator off of the end of the
     * string and computes a scale from it.
     *
     * Note that this method does not flag invalid scale values!
     *
     * @param str a string that may or may not end with a scale
     * @return the scale indicated by the string
     */
    private static int getScale(String str) {
        int scale = 1;

        if (str == null || str.length() == 0)
            return scale;

        char modifierChar = str.charAt(str.length() - 1);
        if (modifierChar == 'k' || modifierChar == 'K')
            scale = 1024;
        else if (modifierChar == 'm' || modifierChar == 'M')
            scale = 1024 * 1024;
        else if (modifierChar == 'g' || modifierChar == 'G')
            scale = 1024 * 1024 * 1024;

        return scale;
    }


    /**
     * Retrieves an integer-valued property, possibly with a scale modifier,
     * from the specified collection of properties.
     *
     * @param properties the collection of properties to read from
     * @param propertyName the name of the property
     * @param defaultValue the default value for the property, if absent
     *
     * @return the integer property value, or default if the property is absent
     */
    public static int getIntProperty(Properties properties,
                                     String propertyName,
                                     int defaultValue) {
        int value = defaultValue;

        String str = properties.getProperty(propertyName);
        if (str != null) {
            str = str.trim().toLowerCase();
            if (str.length() > 1) {
                int scale = getScale(str);
                if (scale != 1)
                    str = str.substring(0, str.length() - 1);

                value = Integer.parseInt(str);
                value *= scale;
            }
        }

        return value;
    }


    /**
     * Retrieves a long-integer-valued property, possibly with a scale
     * modifier, from the specified collection of properties.
     *
     * @param properties the collection of properties to read from
     * @param propertyName the name of the property
     * @param defaultValue the default value for the property, if absent
     *
     * @return the long property value, or default if the property is absent
     */
    public static long getLongProperty(Properties properties,
                                       String propertyName,
                                       long defaultValue) {
        long value = defaultValue;

        String str = properties.getProperty(propertyName);
        if (str != null) {
            str = str.trim().toLowerCase();
            if (str.length() > 1) {
                long scale = getScale(str);
                if (scale != 1)
                    str = str.substring(0, str.length() - 1);

                value = Long.parseLong(str);
                value *= scale;
            }
        }

        return value;
    }


    /**
     * Retrieves an integer-valued property, possibly with a scale modifier,
     * from the system properties.
     *
     * @param propertyName the name of the property
     * @param defaultValue the default value for the property, if absent
     *
     * @return the integer property value, or default if the property is absent
     */
    public int getSystemIntProperty(String propertyName, int defaultValue) {
        return getIntProperty(System.getProperties(), propertyName,
            defaultValue);
    }


    /**
     * Retrieves a long-integer-valued property, possibly with a scale
     * modifier, from the system properties.
     *
     * @param propertyName the name of the property
     * @param defaultValue the default value for the property, if absent
     *
     * @return the long property value, or default if the property is absent
     */
    public long getSystemLongProperty(String propertyName, long defaultValue) {
        return getLongProperty(System.getProperties(), propertyName,
            defaultValue);
    }
}
