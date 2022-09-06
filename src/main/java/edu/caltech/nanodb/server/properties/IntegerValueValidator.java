package edu.caltech.nanodb.server.properties;


import java.util.function.Predicate;

import edu.caltech.nanodb.expressions.TypeConverter;


/**
 * Validates an integer property value using a predicate; if the value cannot
 * be cast to an integer or the predicate returns false, then the specified
 * error message will be reported.  The error message may use the {@code "%d"}
 * format-specifier to include the value passed to the validator.
 */
public class IntegerValueValidator implements PropertyValidator {
    /** A predicate for validating an integer value. */
    private Predicate<Integer> predicate;

    /** An error message for when an integer value is invalid. */
    private String errorMessage;


    public IntegerValueValidator(int minValue, int maxValue) {
        this( (Integer i) -> i >= minValue && i <= maxValue,
            "Value must be between " + minValue + " and " + maxValue +
            " (got %d)");
    }


    public IntegerValueValidator(Predicate<Integer> predicate,
                                 String errorMessage) {
        if (predicate == null)
            throw new IllegalArgumentException("predicate cannot be null");

        if (errorMessage == null)
            throw new IllegalArgumentException("errorMessage cannot be null");

        this.predicate = predicate;
        this.errorMessage = errorMessage;
    }


    @Override
    public Object validate(Object value) {
        if (value == null)
            throw new PropertyException("Property value cannot be NULL");

        Integer intValue = TypeConverter.getIntegerValue(value);
        if (!predicate.test(intValue))
            throw new PropertyException(String.format(errorMessage, intValue));

        return intValue;
    }
}
