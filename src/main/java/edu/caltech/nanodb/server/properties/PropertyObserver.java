package edu.caltech.nanodb.server.properties;


/**
 * An interface that components can implement to be notified of changes to
 * property-values during system operation.
 */
public interface PropertyObserver {
    /**
     * This method is called on all property observers when a given property
     * is changed to a new value.
     *
     * @param propertyName the name of the property that was changed
     *
     * @param newValue the new value of the property
     */
    void propertyChanged(String propertyName, Object newValue);
}
