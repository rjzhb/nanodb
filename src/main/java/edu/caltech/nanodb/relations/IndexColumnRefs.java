package edu.caltech.nanodb.relations;


/**
 * This class records that there is an index built on a particular set of
 * columns.  The name of the index is recorded, which can be used with the
 * {@link edu.caltech.nanodb.indexes.IndexManager} to retrieve specific
 * details of the index.
 * <p>
 * Note that indexes don't indicate constraint details.  All constraints are
 * indicated separately on the {@link Schema} class.  Indexes merely
 * facilitate speedy enforcement of constraints.
 */
public class IndexColumnRefs extends ColumnRefs {

    /**
     * This is the actual name for referring to the index.  It is mapped to
     * an index filename, where the index is actually stored.
     */
    private String indexName;


    public IndexColumnRefs(String indexName, int[] colIndexes) {
        super(colIndexes);

        this.indexName = indexName;
    }


    public IndexColumnRefs(String indexName, KeyColumnRefs keyColRefs) {
        this(indexName, keyColRefs.getCols());
    }


    public String getIndexName() {
        return indexName;
    }


    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
}
