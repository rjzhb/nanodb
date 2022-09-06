package edu.caltech.nanodb.server.properties;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.caltech.nanodb.storage.DBFile;


/**
 * This is the central location where all properties exposed by the database
 * can be registered and accessed.  Various components register their
 * configurable properties with the registry, and then they can be accessed
 * and/or set from the SQL prompt.
 */
public class PropertyRegistry implements ServerProperties {

    private class PropertyDescriptor {
        /** The name of the property. */
        String name;

        /** The current value of the property. */
        Object value;

        /**
         * A flag indicating whether the property is read-only or read-write.
         */
        boolean readonly;

        /** A validator to ensure the property's values are correct. */
        PropertyValidator validator;


        PropertyDescriptor(String name, PropertyValidator validator,
                           Object initialValue, boolean readonly) {
            this.name = name;
            this.validator = validator;

            // Set readonly to false for initial write.
            this.readonly = false;
            setValue(initialValue);

            // Now, set readonly flag to what it should be.
            this.readonly = readonly;
        }


        public Object getValue() {
            return value;
        }


        public void setValue(Object newValue) {
            if (!setup && readonly) {
                throw new PropertyException("Property \"" + name +
                    "\" is read-only during normal operation, and should " +
                    "only be set at start-up.");
            }

            value = validator.validate(newValue);
        }
    }


    /**
     * A mapping of property names to values.  A thread-safe hash map is used
     * since this will be accessed and mutated from different threads.
     */
    private ConcurrentHashMap<String, PropertyDescriptor> properties =
        new ConcurrentHashMap<>();


    /**
     * A collection of property observers to be informed when property values
     * change.
     */
    private ArrayList<PropertyObserver> observers = new ArrayList<>();


    /**
     * This flag indicates whether the database is still in "start-up mode"
     * or not.  During start-up mode, all read-only properties may also be
     * modified.
     */
    private boolean setup = true;


    public PropertyRegistry() {
        initProperties();
    }


    /**
     * Initialize all of the properties that the database server knows about.
     * Some of these properties are read-write, and others are read-only and
     * are initialized right at database startup, before any other steps
     * occur.
     *
     * @review (donnie) It's not great that this configuration is in here.
     *         Someday, should probably migrate it back into
     *         {@code NanoDBServer}.
     */
    private void initProperties() {
        addProperty(PROP_BASE_DIRECTORY,
            new StringValueValidator(), DEFAULT_BASE_DIRECTORY,
            /* readonly */ true);

        addProperty(PROP_PAGECACHE_SIZE,
            new IntegerValueValidator(MIN_PAGECACHE_SIZE, MAX_PAGECACHE_SIZE),
            DEFAULT_PAGECACHE_SIZE);

        addProperty(PROP_PAGECACHE_POLICY,
            new StringEnumValidator(PAGECACHE_POLICY_VALUES),
            DEFAULT_PAGECACHE_POLICY, /* readonly */ true);

        addProperty(PROP_PAGE_SIZE,
            new IntegerValueValidator(DBFile::isValidPageSize,
                "Specified page-size %d is invalid."), DEFAULT_PAGE_SIZE);

        addProperty(PROP_ENABLE_TRANSACTIONS,
            new BooleanFlagValidator(), false, /* readonly */ true);

        addProperty(PROP_ENFORCE_KEY_CONSTRAINTS,
            new BooleanFlagValidator(), true);

        addProperty(PROP_ENABLE_INDEXES,
            new BooleanFlagValidator(), false, /* reaadonly */ true);

        addProperty(PROP_CREATE_INDEXES_ON_KEYS,
            new BooleanFlagValidator(), false);

        addProperty(PROP_PLANNER_CLASS,
            new PlannerClassValidator(), DEFAULT_PLANNER_CLASS);

        addProperty(PROP_FLUSH_AFTER_CMD, new BooleanFlagValidator(), true);
    }


    /**
     * This helper function sets the server properties based on the contents
     * of a Java {@code Properties} object.  This allows us to set NanoDB
     * properties from system properties and/or other sources of properties.
     *
     * @param properties the properties to apply to NanoDB's configuration.
     */
    public void setProperties(Properties properties) {
        if (properties == null)
            throw new IllegalArgumentException("properties cannot be null");

        for (String name : properties.stringPropertyNames()) {
            if (hasProperty(name))
                setPropertyValue(name, properties.getProperty(name));
        }
    }


    /**
     * Records that setup has been completed, and read-only properties
     * should no longer be allowed to change.
     */
    public void setupCompleted() {
        setup = false;
    }


    /**
     * Records a property-change observer on the property registry.
     *
     * @param observer the observer to receive property-change notifications
     */
    public void addObserver(PropertyObserver observer) {
        if (observer == null)
            throw new IllegalArgumentException("observer cannot be null");

        observers.add(observer);
    }


    /**
     * Add a read-only or read-write property to the registry, along with a
     * type and an initial value.
     *
     * @param name the name of the property
     * @param validator a validator for the property
     * @param initialValue an initial value for the property
     * @param readonly a flag indicating whether the property is read-only
     *        ({@code true}) or read-write ({@code false})
     */
    public void addProperty(String name, PropertyValidator validator,
                            Object initialValue, boolean readonly) {
        properties.put(name,
            new PropertyDescriptor(name, validator, initialValue, readonly));
    }


    /**
     * Add a read-write property to the registry, along with a
     * type and an initial value.
     *
     * @param name the name of the property
     * @param validator a validator for the property
     * @param initialValue an initial value for the property
     */
    public void addProperty(String name, PropertyValidator validator,
                            Object initialValue) {
        addProperty(name, validator, initialValue, false);
    }


    /**
     * Returns {@code true} if the server has a property of the specified
     * name, {@code false} otherwise.
     *
     * @param name the non-null name of the property
     * @return {@code true} if the server has a property of the specified
     *         name, {@code false} otherwise.
     */
    public boolean hasProperty(String name) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");

        return properties.containsKey(name);
    }


    /**
     * Returns an unmodifiable set of all property names.
     *
     * @return an unmodifiable set of all property names.
     */
    public Set<String> getAllPropertyNames() {
        return Collections.unmodifiableSet(properties.keySet());
    }


    public Object getPropertyValue(String name)
        throws PropertyException {

        if (name == null)
            throw new IllegalArgumentException("name cannot be null");

        if (!properties.containsKey(name)) {
            throw new PropertyException("No property named \"" +
                name + "\"");
        }

        return properties.get(name).getValue();
    }


    public void setPropertyValue(String name, Object value) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");

        if (!properties.containsKey(name)) {
            throw new PropertyException("No property named \"" +
                name + "\"");
        }

        properties.get(name).setValue(value);

        for (PropertyObserver obs : observers)
            obs.propertyChanged(name, value);
    }


    /**
     * Returns a property's value as a Boolean.  If the property's value is
     * not a Boolean then an exception is reported.
     *
     * @param name the name of the property to fetch
     *
     * @return a Boolean true or false value for the property
     */
    public boolean getBooleanProperty(String name) {
        return (Boolean) getPropertyValue(name);
    }


    /**
     * Returns a property's value as a String.  If the property's value is
     * not a String then an exception is reported.
     *
     * @param name the name of the property to fetch
     *
     * @return a String value for the property
     */
    public String getStringProperty(String name) {
        return (String) getPropertyValue(name);
    }


    /**
     * Returns a property's value as an integer.  If the property's value is
     * not an integer then an exception is reported.
     *
     * @param name the name of the property to fetch
     *
     * @return an integer value for the property
     */
    public int getIntProperty(String name) {
        return (Integer) getPropertyValue(name);
    }
}
