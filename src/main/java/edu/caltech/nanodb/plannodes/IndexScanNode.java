//~ Include file if #full.

package edu.caltech.nanodb.plannodes;


import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.expressions.OrderByExpression;
import edu.caltech.nanodb.expressions.TupleLiteral;
import edu.caltech.nanodb.indexes.IndexInfo;
import edu.caltech.nanodb.indexes.IndexManager;
import edu.caltech.nanodb.indexes.IndexScanEndpoints;
import edu.caltech.nanodb.queryeval.PlanCost;
import edu.caltech.nanodb.queryeval.TableStats;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.Tuple;
import edu.caltech.nanodb.storage.FilePointer;
import edu.caltech.nanodb.storage.HashedTupleFile;
import edu.caltech.nanodb.storage.SequentialTupleFile;
import edu.caltech.nanodb.storage.TupleFile;


/**
 * A select plan-node that uses an index to access the tuples in a tuple file,
 * checking the optional predicate against the values in the index.
 */
public class IndexScanNode extends PlanNode {

    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(IndexScanNode.class);


    /** The index-info for the index being scanned. */
    private IndexInfo indexInfo;


    /** The index being used for the index scan. */
    private TupleFile indexTupleFile;


    /**
     * The table that the index is built against.  This is used to resolve
     * the index-tuples into the table-file tuples.
     */
    private TupleFile tableTupleFile;


    /**
     * This is the value to use when looking up the first tuple in the index.
     * Note that this may not be the first tuple that this node returns.  For
     * example, if the predicate is "<tt>a > 5</tt>" then the initial lookup
     * value will be "<tt>a == 5</tt>", but that lookup may not return a row
     * that satisfies the predicate.  This is the purpose of
     * {@link #startPredicate}.
     */
    private TupleLiteral startLookupValue;


    /**
     * This is the predicate that the first tuple in the index-scan must
     * satisfy.  Note that all tuples must also satisfy the
     * {@link #endPredicate} predicate.  Note also that this may be
     * {@code null} if the index-scan must start with the first tuple in the
     * index.
     */
    private Expression startPredicate;


    /**
     * This is the predicate that all tuples in the index-scan must satisfy.
     * It is named "endPredicate" because when it becomes false, the node has
     * reached the end of the index-scan.  Note that this may be {@code null}
     * if the index-scan must start with the first tuple in the index.
     */
    private Expression endPredicate;


    /**
     * The current tuple from the index that is being used.  Note that this is
     * not what {@link #getNextTuple} returns; rather, it's the index-record
     * used to look up that tuple.
     */
    private Tuple currentIndexTuple;


    /**
     * The index must have one column named "<tt>#TUPLE_PTR</tt>"; this is the
     * column-index of that column in the index's schema.
     */
    private int idxTuplePtr;


    /** True if we have finished scanning or pulling tuples from children. */
    private boolean done;


    /**
     * This field allows the index-scan node to mark a particular tuple in the
     * tuple-stream and then rewind to that point in the tuple-stream.
     */
    private FilePointer markedTuple;


    private boolean jumpToMarkedTuple;


    /**
     * Construct an index scan node that performs an equality-based lookup on
     * an index.
     *
     * @param indexInfo the information about the index being used
     */
    public IndexScanNode(IndexInfo indexInfo,
                         IndexScanEndpoints indexEndpoints) {
        super();

        if (indexInfo == null)
            throw new IllegalArgumentException("indexInfo cannot be null");

        if (indexEndpoints == null)
            throw new IllegalArgumentException("indexEndpoints cannot be null");

        // Store the index endpoints, and a TupleLiteral of where to start
        // looking in the index.

        startPredicate = indexEndpoints.getStartPredicate();
        endPredicate = indexEndpoints.getEndPredicate();
        startLookupValue =
            new TupleLiteral(indexEndpoints.getStartValues().toArray());

        // Pull out the tuple-file for the index, as well as the tuple-file
        // for the table that the index references.

        this.indexInfo = indexInfo;
        indexTupleFile = indexInfo.getTupleFile();
        tableTupleFile = indexInfo.getTableInfo().getTupleFile();

        // Figure out which column in the index is the tuple-pointer column.

        Schema idxSchema = indexTupleFile.getSchema();
        idxTuplePtr = idxSchema.getColumnIndex(IndexManager.COLNAME_TUPLEPTR);
        if (idxTuplePtr == -1) {
            throw new IllegalArgumentException("Index must have a column " +
                "named " + IndexManager.COLNAME_TUPLEPTR);
        }
    }


    /**
     * Returns true if the passed-in object is a <tt>FileScanNode</tt> with
     * the same predicate and table.
     *
     * @param obj the object to check for equality
     *
     * @return true if the passed-in object is equal to this object; false
     *         otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IndexScanNode) {
            IndexScanNode other = (IndexScanNode) obj;
            // We don't include the table-info or the index-info since each
            // table or index is in its own tuple file.
            return indexTupleFile.equals(other.indexTupleFile) &&
                Objects.equals(startPredicate, other.startPredicate) &&
                Objects.equals(endPredicate, other.endPredicate) &&
                Objects.equals(startLookupValue, other.startLookupValue);
        }

        return false;
    }


    /**
     * Computes the hashcode of a PlanNode.  This method is used to see if two
     * plan nodes CAN be equal.
     **/
    public int hashCode() {
        int hash = 7;
        // We don't include the index-info since each index is in its own
        // tuple file.
        hash = 31 * hash + indexTupleFile.hashCode();
        hash = 31 * hash + Objects.hashCode(startPredicate);
        hash = 31 * hash + Objects.hashCode(endPredicate);
        hash = 31 * hash + Objects.hashCode(startLookupValue);
        return hash;
    }


    /**
     * Creates a copy of this simple filter node node and its subtree.  This
     * method is used by {@link PlanNode#duplicate} to copy a plan tree.
     */
    @Override
    protected PlanNode clone() throws CloneNotSupportedException {
        IndexScanNode node = (IndexScanNode) super.clone();

        // TODO:  Should we clone these?
        node.indexInfo = indexInfo;

        // The tuple file doesn't need to be copied since it's immutable.
        node.indexTupleFile = indexTupleFile;

        return node;
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("IndexScan[");
        buf.append("index:  ").append(indexInfo.getTableName());
        buf.append('.').append(indexInfo.getIndexName());

        buf.append(", startLookup:  ").append(startLookupValue);
        buf.append(", startPred:  ").append(startPredicate);
        buf.append(", endPred:  ").append(endPredicate);

        buf.append("]");

        return buf.toString();
    }


    /**
     * Currently we will always say that the file-scan node produces unsorted
     * results.  In actuality, a file scan's results will be sorted if the table
     * file uses a sequential format, but currently we don't have any sequential
     * file formats.
     */
    public List<OrderByExpression> resultsOrderedBy() {
        return null;
    }


    /** This node supports marking. */
    public boolean supportsMarking() {
        return true;
    }


    protected void prepareSchema() {
        // Grab the schema from the table.
        schema = indexTupleFile.getSchema();
    }


    // Inherit javadocs from base class.
    public void prepare() {
        // Grab the schema and statistics from the table file.

        schema = tableTupleFile.getSchema();

        // TODO:  We should also update the table statistics based on what the
        //        index scan is going to do, but that's too complicated, so
        //        we'll leave them unchanged for now.
        TableStats tableStats = tableTupleFile.getStats();
        stats = tableStats.getAllColumnStats();

        // TODO:  Cost the plan node
        cost = null;
    }


    @Override
    public void initialize() {
        super.initialize();

        currentIndexTuple = null;
        done = false;

        // Reset our marking state.
        markedTuple = null;
        jumpToMarkedTuple = false;
    }


    @Override
    public Tuple getNextTuple() {
        if (done)
            return null;

        if (jumpToMarkedTuple) {
            logger.debug("Resuming at previously marked tuple.");
            currentIndexTuple = indexTupleFile.getTuple(markedTuple);
            jumpToMarkedTuple = false;
        }
        else if (currentIndexTuple == null) {
            // Navigate to the first tuple.
            currentIndexTuple = findFirstIndexTuple();
        }
        else {
            // Go ahead and navigate to the next tuple.
            currentIndexTuple = findNextIndexTuple(currentIndexTuple);
            if (currentIndexTuple == null)
                done = true;
        }

        Tuple tableTuple = null;
        if (currentIndexTuple != null) {
            // Now, look up the table tuple based on the index tuple's
            // file-pointer.
            FilePointer tuplePtr =
                (FilePointer) currentIndexTuple.getColumnValue(idxTuplePtr);
            currentIndexTuple.unpin();
            tableTuple = tableTupleFile.getTuple(tuplePtr);
        }
        return tableTuple;
    }


    /**
     * This method finds the starting tuple in the index, based on the
     * starting predicate and the starting value, as determined from the
     * query predicate.  The index-tuple is also verified against the ending
     * predicate, just in case the two predicates are mutually exclusive.
     *
     * @return the first tuple in the index that satisfies the starting
     *         criteria, or {@code null} if no tuples in the index satisfy
     *         the criteria.
     */
    private Tuple findFirstIndexTuple() {
        Tuple tup;

        // Navigate to first index-tuple based on start-value to lookup
        if (indexTupleFile instanceof SequentialTupleFile) {
            SequentialTupleFile seqTupFile =
                (SequentialTupleFile) indexTupleFile;
            tup = seqTupFile.findFirstTupleEquals(startLookupValue);
        }
        else if (indexTupleFile instanceof HashedTupleFile) {
            HashedTupleFile hashTupFile =
                (HashedTupleFile) indexTupleFile;
            tup = hashTupFile.findFirstTupleEquals(startLookupValue);
        }
        else {
            throw new IllegalStateException("indexTupleFile is neither a " +
                "SequentialTupleFile or a HashedTupleFile (got " +
                indexTupleFile.getClass().getName() + ")");
        }

        // Initial lookup returned null.  No tuples.
        if (tup == null)
            return null;

        // While our start-predicate isn't true, advance the current index
        // tuple.
        while (true) {
            environment.clear();
            environment.addTuple(indexInfo.getSchema(), tup);
            if (startPredicate.evaluatePredicate(environment))
                break;

            tup.unpin();
            tup = indexTupleFile.getNextTuple(tup);
        }

        // Also check the ending predicate.  This is only necessary if the
        // start- and end-points overlap, and therefore the plan-node actually
        // should output nothing.  Presumably this will be caught in the code
        // that runs before this plan-node.
        if (!endPredicate.evaluatePredicate(environment)) {
            tup.unpin();
            tup = null;
        }

        // Now we are at the proper starting point in the index tuple sequence
        return tup;
    }


    /**
     * Given the "current" tuple in the index, this method finds the next
     * tuple in the index, ensuring that it also satisfies the ending
     * predicate.
     *
     * @param tuple the "current" tuple in the index
     *
     * @return the next tuple in the index that follows the specified tuple,
     *         or {@code null} if no more tuples in the index satisfy the
     *         criteria.
     */
    private Tuple findNextIndexTuple(Tuple tuple) {
        // Get the next tuple from the index file.  If it still satisfies the
        // ending predicate then return the tuple.
        Tuple tup = indexTupleFile.getNextTuple(tuple);
        if (tup != null) {
            environment.clear();
            environment.addTuple(indexInfo.getSchema(), tup);
            if (!endPredicate.evaluatePredicate(environment)) {
                tup.unpin();
                tup = null;
            }
        }

        return tup;
    }


    public void cleanUp() {
        // Nothing to do!
    }


    public void markCurrentPosition() {
        if (currentIndexTuple == null)
            throw new IllegalStateException("There is no current tuple!");

        logger.debug("Marking current position in tuple-stream.");
        markedTuple = currentIndexTuple.getExternalReference();
    }


    public void resetToLastMark() {
        if (markedTuple == null)
            throw new IllegalStateException("There is no last-marked tuple!");

        logger.debug("Resetting to previously marked position in tuple-stream.");
        jumpToMarkedTuple = true;
    }
}
