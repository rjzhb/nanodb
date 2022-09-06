package edu.caltech.nanodb.server.properties;


import edu.caltech.nanodb.queryeval.Planner;


/**
 * This class validates the query-planner class name by making sure the
 * specified class actually derives from {@link Planner}, and that it may
 * be instantiated via a no-argument constructor.
 */
public class PlannerClassValidator implements PropertyValidator {

    @Override
    public Object validate(Object value) {
        if (value == null)
            throw new PropertyException("Property value cannot be NULL");

        if (value instanceof String) {
            String s = (String) value;

            // Try to instantiate the specified planner class, so we can catch
            // any bad inputs.
            try {
                Class<?> c = Class.forName(s);

                // Make sure the class actually implements the Planner
                // interface.
                if (!Planner.class.isAssignableFrom(c)) {
                    throw new PropertyException("Class \"" + s +
                        "\" does not implement the Planner interface");
                }

                // Make sure we can instantiate the class via a default
                // constructor.
                c.getDeclaredConstructor().newInstance();

                // If we got here, the value is valid.
                return value;
            }
            catch (Exception e) {
                throw new PropertyException(
                    "Couldn't instantiate Planner class \"" + s + "\"", e);
            }
        }
        else {
            throw new PropertyException(
                "Planner classname must be a string value.");
        }

    }
}
