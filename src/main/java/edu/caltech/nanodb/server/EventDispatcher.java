package edu.caltech.nanodb.server;


import edu.caltech.nanodb.commands.Command;
import edu.caltech.nanodb.relations.TableInfo;
import edu.caltech.nanodb.relations.Tuple;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;


/**
 * The event dispatcher notifies listeners when specific events occur within
 * the database, related to command execution and row processing operations.
 * It is used, for example, to implement index updates and other such tasks.
 */
public class EventDispatcher {

    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(EventDispatcher.class);


    /**
     * A list of listeners that are registered to receive command event
     * notifications.
     */
    private ArrayList<CommandEventListener> commandEventListeners =
        new ArrayList<>();


    /**
     * A list of listeners that are registered to receive row event
     * notifications.
     */
    private ArrayList<RowEventListener> rowEventListeners = new ArrayList<>();


    /**
     * Add another command-event listener to the event dispatcher.  Note that
     * if the same component is added twice, it will receive two notifications
     * for each event.
     *
     * @param listener the command-event listener
     */
    public void addCommandEventListener(CommandEventListener listener) {
        commandEventListeners.add(listener);
    }


    /**
     * Add another row-event listener to the event dispatcher.  Note that if
     * the same component is added twice, it will receive two notifications
     * for each event.
     *
     * @param listener the row-event listener
     */
    public void addRowEventListener(RowEventListener listener) {
        rowEventListeners.add(listener);
    }


    /**
     *
     * @param cmd the command that is about to be executed
     */
    public void fireBeforeCommandExecuted(Command cmd) {
        for (CommandEventListener cel : commandEventListeners)
            cel.beforeCommandExecuted(cmd);
    }


    /**
     *
     * @param cmd the command that was just executed
     */
    public void fireAfterCommandExecuted(Command cmd) {
        for (CommandEventListener cel : commandEventListeners)
            cel.afterCommandExecuted(cmd);
    }


    public void fireBeforeRowInserted(TableInfo tblFileInfo,
                                      Tuple newValues) {
        logger.debug("Firing beforeRowInserted");
        for (RowEventListener rel : rowEventListeners)
            rel.beforeRowInserted(tblFileInfo, newValues);
    }


    public void fireAfterRowInserted(TableInfo tblFileInfo,
                                     Tuple newTuple) {
        logger.debug("Firing afterRowInserted");
        for (RowEventListener rel : rowEventListeners)
            rel.afterRowInserted(tblFileInfo, newTuple);
    }


    public void fireBeforeRowUpdated(TableInfo tblFileInfo, Tuple oldTuple,
                                     Tuple newValues) {
        logger.debug("Firing beforeRowUpdated");
        for (RowEventListener rel : rowEventListeners)
            rel.beforeRowUpdated(tblFileInfo, oldTuple, newValues);
    }


    public void fireAfterRowUpdated(TableInfo tblFileInfo, Tuple oldValues,
                                    Tuple newTuple) {
        logger.debug("Firing afterRowUpdated");
        for (RowEventListener rel : rowEventListeners)
            rel.afterRowUpdated(tblFileInfo, oldValues, newTuple);
    }


    public void fireBeforeRowDeleted(TableInfo tblFileInfo,
                                     Tuple oldTuple) {
        logger.debug("Firing beforeRowDeleted");
        for (RowEventListener rel : rowEventListeners)
            rel.beforeRowDeleted(tblFileInfo, oldTuple);
    }


    public void fireAfterRowDeleted(TableInfo tblFileInfo,
                                    Tuple oldValues) {
        logger.debug("Firing afterRowDeleted");
        for (RowEventListener rel : rowEventListeners)
            rel.afterRowDeleted(tblFileInfo, oldValues);
    }
}
