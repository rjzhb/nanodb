package edu.caltech.nanodb.relations;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.caltech.nanodb.expressions.ColumnName;
import edu.caltech.nanodb.storage.TableException;


/**
 * <p>
 * A schema is an ordered collection of column names and associated types.
 * </p>
 * <p>
 * Many different entities in the database code can have schema associated
 * with them.  Both tables and tuples have schemas, for obvious reasons.
 * <tt>SELECT</tt> and <tt>FROM</tt> clauses also have schemas, used by the
 * database engine to verify the semantics of database queries.  Finally,
 * relational algebra plan nodes also have schemas, which specify the kinds of
 * tuples that they generate.
 * </p>
 */
public class Schema implements Serializable, Iterable<ColumnInfo> {

    //========================================================================
    // STATIC HELPER CLASSES

    /**
     * This helper class is used for the internal hashed column structure, so
     * that we can do fast lookups based on table names and column names.
     */
    private static class IndexedColumnInfo implements Serializable {
        /** The index in the schema that the column appears at. */
        public int colIndex;

        /** The details of the column at the stored index. */
        public ColumnInfo colInfo;

        /** Stores the specified index and column-info value. */
        IndexedColumnInfo(int colIndex, ColumnInfo colInfo) {
            if (colInfo == null)
                throw new NullPointerException("colInfo cannot be null");

            if (colIndex < 0) {
                throw new IllegalArgumentException("colIndex must be >= 0; got " +
                    colIndex);
            }

            this.colIndex = colIndex;
            this.colInfo = colInfo;
        }
    }


    //========================================================================
    // INSTANCE FIELDS


    /**
     * The collection of the column-info objects describing the columns in the
     * schema.
     */
    private ArrayList<ColumnInfo> columnInfos = new ArrayList<>();


    /**
     * A mapping that provides fast lookups for columns based on table name and
     * column name.  The outer hash-map has table names for keys; "no table" is
     * indicated with a <code>null</code> key, which {@link java.util.HashMap}
     * supports.  The inner hash-map has column names for keys, and maps to
     * column information objects.
     */
    private HashMap<String, HashMap<String, IndexedColumnInfo>>
        colsHashedByTable = new HashMap<>();


    /**
     * A mapping that provides fast lookups for columns based only on column
     * name.  Because multiple columns could have the same column name (but
     * different table names) in a single schema, the values in the mapping are
     * lists.
     */
    private HashMap<String, ArrayList<IndexedColumnInfo>> colsHashedByColumn =
        new HashMap<>();


    /**
     * A set recording the indexes of the columns have NOT NULL constraints
     * on them.
     */
    private HashSet<Integer> notNullCols = new HashSet<>();


    /**
     * A collection of all candidate-key objects specifying the sets of
     * columns that comprise candidate keys on this table.  One of these keys
     * may be specified as the primary key.
     */
    private ArrayList<KeyColumnRefs> candidateKeys = new ArrayList<>();


    /**
     * A collection of foreign-key objects specifying other tables that this
     * table references.
     */
    private ArrayList<ForeignKeyColumnRefs> foreignKeys = new ArrayList<>();


    /** A set of the tables that reference this table. */
    private HashSet<String> referencingTables = new HashSet<>();


    /**
     * This collection provides easy access to all the indexes defined on this
     * table, including those for candidate keys and the primary key.
     */
    private HashMap<String, IndexColumnRefs> indexes = new HashMap<>();


    //========================================================================
    // INSTANCE METHODS


    /** Construct an empty schema. */
    public Schema() {
    }


    /**
     * Construct a schema whose columns are taken from the specified iterable.
     * Note that this does not work with arrays of column-info objects.
     */
    public Schema(Iterable<ColumnInfo> colInfos) throws SchemaNameException {
        for (ColumnInfo i : colInfos)
            addColumnInfo(i);
    }


    /**
     * Construct a schema containing the specified array of column-info
     * objects.
     */
    public Schema(ColumnInfo... colInfos) throws SchemaNameException {
        for (ColumnInfo i : colInfos)
            addColumnInfo(i);
    }


    /**
     * <p>
     * Create a schema that is the concatenation of one or more other schemas.
     * Schemas are copied in the order they are given.  If a column name
     * appears multiple times in the input, an exception will be generated.
     * </p>
     * <p>
     * Keys will not be copied by this constructor.
     * </p>
     *
     * @param schemas one or more schema objects to copy into this schema
     */
    public Schema(Schema... schemas) {
        this();
        for (Schema s : schemas)
            append(s);
    }


    /**
     * Returns the number of columns in the schema.
     *
     * @return the number of columns in the schema.
     */
    public int numColumns() {
        return columnInfos.size();
    }


    /**
     * Returns the <tt>ColumnInfo</tt> object describing the column at the
     * specified index.  Column indexes are numbered from 0.
     *
     * @param i the index to retrieve the column-info for
     *
     * @return the <tt>ColumnInfo</tt> object describing the name and type of
     *         the column
     */
    public ColumnInfo getColumnInfo(int i) {
        return columnInfos.get(i);
    }


    /**
     * Returns an unmodifiable list of all the columns in the schema
     *
     * @return an unmodifiable list of all the columns in the schema
     */
    public List<ColumnInfo> getColumnInfos() {
        return Collections.unmodifiableList(columnInfos);
    }


    /**
     * Constructs and returns a list of {@link ColumnInfo} objects for the
     * columns at the specified indexes.  This method can be useful with
     * {@link ColumnRefs} objects,to retrieve the columns referenced by a key.
     *
     * @param colIndexes an array of zero-based column indexes to retrieve
     * @return a list of {@code ColumnInfo} objects for the specified columns
     */
    public ArrayList<ColumnInfo> getColumnInfos(int[] colIndexes) {
        ArrayList<ColumnInfo> result = new ArrayList<>(colIndexes.length);

        for (int colIndex : colIndexes)
            result.add(getColumnInfo(colIndex));

        return result;
    }


    /**
     * Provides support for iteration over the columns in the schema.
     *
     * @return an iterator over the columns in this schema.
     */
    @Override
    public Iterator<ColumnInfo> iterator() {
        return Collections.unmodifiableList(columnInfos).iterator();
    }


    /**
     * Add a column to the schema.
     *
     * @param colInfo the name and type of the column being added to the
     *        schema
     * @return the zero-based index of the column in the schema
     *
     * @throws IllegalArgumentException if {@code colInfo} is {@code null}
     */
    public int addColumnInfo(ColumnInfo colInfo) {
        if (colInfo == null)
            throw new NullPointerException("colInfo cannot be null");

        String colName = colInfo.getName();
        String tblName = colInfo.getTableName();

        // Check the hashed-columns structure to see if this column already
        // appears in the schema.

        HashMap<String, IndexedColumnInfo> colMap = colsHashedByTable.get(tblName);
        if (colMap != null && colMap.containsKey(colName)) {
            throw new SchemaNameException("Specified column " + colInfo +
            " is a duplicate of an existing column.");
        }

        int colIndex = columnInfos.size();
        columnInfos.add(colInfo);

        IndexedColumnInfo indexedColInfo = new IndexedColumnInfo(colIndex, colInfo);

        // Update the hashed-columns structures for fast/easy lookup.

        if (colMap == null) {
            colMap = new HashMap<>();
            colsHashedByTable.put(tblName, colMap);
        }
        colMap.put(colName, indexedColInfo);

        ArrayList<IndexedColumnInfo> colList =
            colsHashedByColumn.computeIfAbsent(colName, k -> new ArrayList<>());
        colList.add(indexedColInfo);

        // Finally, return the index.

        return colIndex;
    }


    /**
     * Append another schema to this schema.
     *
     * @throws SchemaNameException if any of the input column-info objects
     *         overlap the names of columns already in the schema.
     */
    public void append(Schema s) throws SchemaNameException {
        for (ColumnInfo colInfo : s)
             addColumnInfo(colInfo);
    }


    /**
     * Returns a set containing all table names that appear in this schema.
     * Note that this may include {@code null} if there are columns with no
     * table name specified!
     */
    public Set<String> getTableNames() {
        return Collections.unmodifiableSet(colsHashedByTable.keySet());
    }


    /**
     * This helper method returns the names of all tables that appear in both
     * this schema and the specified schema.  Note that not all columns of a
     * given table must be present for the table to be included in the result;
     * there just has to be at least one column from the table in both schemas
     * for it to be included in the result.
     *
     * @param s the other schema to compare this schema to
     * @return a set containing the names of all tables that appear in both
     *         schemas
     */
    public Set<String> getCommonTableNames(Schema s) {
        HashSet<String> shared = new HashSet<>(colsHashedByTable.keySet());

        // If there are columns without a table name, null will be in the set.
        // Remove it, since that's a bit confusing.
        shared.remove(null);

        shared.retainAll(s.getTableNames());
        return shared;
    }


    /**
     * Returns a set containing all column names that appear in this schema.
     * Note that a column-name may be used by multiple columns, if it is
     * associated with multiple table names in this schema.
     *
     * @return a set containing all column names that appear in this schema.
     */
    public Set<String> getColumnNames() {
        return Collections.unmodifiableSet(colsHashedByColumn.keySet());
    }


    /**
     * Returns the names of columns that are common between this schema and the
     * specified schema.  This kind of operation is mainly used for resolving
     * <tt>NATURAL</tt> joins.
     *
     * @param s the schema to compare to this schema
     * @return a set of the common column names
     */
    public Set<String> getCommonColumnNames(Schema s) {
        HashSet<String> shared = new HashSet<>(colsHashedByColumn.keySet());
        shared.retainAll(s.getColumnNames());
        return shared;
    }


    /**
     * Returns the number of columns that have the specified column name.  Note
     * that multiple columns can have the same column name but different table
     * names.
     *
     * @param colName the column name to return the count for
     * @return the number of columns with the specified column name
     */
    public int numColumnsWithName(String colName) {
        ArrayList<IndexedColumnInfo> list = colsHashedByColumn.get(colName);
        if (list != null)
            return list.size();

        return 0;
    }


    /**
     * This helper method returns true if this schema contains any columns with
     * the same column name but different table names.  If so, the schema is not
     * valid for use on one side of a <tt>NATURAL</tt> join.
     *
     * @return true if the schema has multiple columns with the same column name
     *         but different table names, or false otherwise.
     */
    public boolean hasMultipleColumnsWithSameName() {
        for (String cName : colsHashedByColumn.keySet()) {
            if (colsHashedByColumn.get(cName).size() > 1)
                return true;
        }
        return false;
    }


    /**
     * Presuming that exactly one column has the specified name, this method
     * returns the column information for that column name.
     *
     * @param colName the name of the column to retrieve the information for
     *
     * @return the column information for the specified column
     *
     * @throws SchemaNameException if the specified column name doesn't appear
     *         in this schema, or if it appears multiple times
     */
    public ColumnInfo getColumnInfo(String colName) {
        ArrayList<IndexedColumnInfo> list = colsHashedByColumn.get(colName);
        if (list == null || list.size() == 0)
            throw new SchemaNameException("No columns with name " + colName);

        if (list.size() > 1)
            throw new SchemaNameException("Multiple columns with name " + colName);

        return list.get(0).colInfo;
    }


    /**
     * This method iterates through all columns in the schema, setting them
     * to all have the specified table name.  An exception will be thrown if
     * the result would be an invalid schema with duplicate column names; in
     * this case, the schema object will remain unchanged.
     *
     * @throws SchemaNameException if the schema contains columns with the
     *         same column name but different table names.  In this case,
     *         resetting the table name will produce an invalid schema with
     *         ambiguous column names.
     *
     * @design (donnie) At present, this method does this by replacing each
     *         {@link ColumnInfo} object with a new object with updated
     *         information.  This is because {@code ColumnInfo} is immutable.
     */
    public void setTableName(String tableName) throws SchemaNameException {
        // First, verify that overriding the table names will not produce
        // multiple ambiguous column names.
        ArrayList<String> duplicateNames = null;

        for (Map.Entry<String, ArrayList<IndexedColumnInfo>> entry :
             colsHashedByColumn.entrySet()) {

            if (entry.getValue().size() > 1) {
                if (duplicateNames == null)
                    duplicateNames = new ArrayList<>();

                duplicateNames.add(entry.getKey());
            }
        }

        if (duplicateNames != null) {
            throw new SchemaNameException("Overriding table-name to \"" +
                tableName + "\" would produce ambiguous columns:  " +
                duplicateNames);
        }

        // If we get here, we know that we can safely override the table name for
        // all columns.

        ArrayList<ColumnInfo> oldColInfos = columnInfos;

        columnInfos = new ArrayList<>();
        colsHashedByTable = new HashMap<>();
        colsHashedByColumn = new HashMap<>();

        // Iterate over the columns in the same order as they were in originally.
        // For each one, override the table name, then use addColumnInfo() to
        // properly update the internal hash structure.

        for (ColumnInfo colInfo : oldColInfos) {
            ColumnInfo newColInfo =
                new ColumnInfo(colInfo.getName(), tableName, colInfo.getType());

            addColumnInfo(newColInfo);
        }
    }


    /**
     * Finds the index of the specified column in this schema, or returns -1
     * if the schema contains no column of the specified name.  The
     * column-name object is not required to specify a table name; however, if
     * the table name is unspecified and the column name is ambiguous then an
     * exception will be thrown.
     *
     * @param colName column-name object to use for looking up the column in
     *        the schema
     *
     * @return the zero-based index of the column, or -1 if the schema does
     *         not contain a column of the specified name.
     *
     * @throws IllegalArgumentException if {@code colName} is {@code null}
     * @throws SchemaNameException if {@code colName} doesn't specify a table
     *         name, and multiple columns have the specified column name
     */
    public int getColumnIndex(ColumnName colName) {
        if (colName == null)
            throw new IllegalArgumentException("colInfo cannot be null");

        if (colName.isColumnWildcard())
            throw new IllegalArgumentException("colName cannot be a wildcard");

        return getColumnIndex(colName.getTableName(), colName.getColumnName());
    }


    /**
     * Finds the index of the specified column in this schema, or returns -1
     * if the schema contains no column of the specified name.  The
     * column-info object is not required to specify a table name; however, if
     * the table name is unspecified and the column name is ambiguous then an
     * exception will be thrown.
     *
     * @param colInfo column-info object to use for looking up the column in
     *        the schema
     *
     * @return the zero-based index of the column, or -1 if the schema does
     *         not contain a column of the specified name.
     *
     * @throws IllegalArgumentException if {@code colInfo} is {@code null}
     * @throws SchemaNameException if {@code colInfo} doesn't specify a table
     *         name, and multiple columns have the specified column name
     */
    public int getColumnIndex(ColumnInfo colInfo) {
        if (colInfo == null)
            throw new IllegalArgumentException("colInfo cannot be null");

        return getColumnIndex(colInfo.getTableName(), colInfo.getName());
    }


    /**
     * Finds the index of the specified column in this schema, or returns -1
     * if the schema contains no column of the specified name.  The table name
     * is unspecified; if the column name is ambiguous then an exception will
     * be thrown.
     *
     * @param colName the column name to look up
     *
     * @return the zero-based index of the column, or -1 if the schema does
     *         not contain a column of the specified name.
     *
     * @throws IllegalArgumentException if {@code colName} is {@code null}
     * @throws SchemaNameException if multiple columns have the specified
     *         column name
     */
    public int getColumnIndex(String colName) {
        return getColumnIndex(null, colName);
    }


    /**
     * Finds the index of the specified column in this schema, or returns -1
     * if the schema contains no column of the specified name.  The table name
     * may be specified or it may be {@code null}; if {@code null} and the
     * column name is ambiguous then an exception will be thrown.
     *
     * @param tblName the table name, or {@code null} if the table name is not
     *        known or unspecified
     * @param colName the column name to look up
     *
     * @return the zero-based index of the column, or -1 if the schema does
     *         not contain a column of the specified name.
     *
     * @throws IllegalArgumentException if {@code colName} is {@code null}
     * @throws SchemaNameException if {@code tblName} is {@code null} and
     *         multiple columns have the specified column name
     */
    public int getColumnIndex(String tblName, String colName) {
        if (colName == null)
            throw new IllegalArgumentException("colName cannot be null");

        ArrayList<IndexedColumnInfo> colList = colsHashedByColumn.get(colName);

        if (colList == null)
            return -1;

        if (tblName == null) {
            if (colList.size() > 1) {
                throw new SchemaNameException("Column name \"" + colName +
                    "\" is ambiguous in this schema.");
            }

            return colList.get(0).colIndex;
        }
        else {
            // Table-name is specified.

            for (IndexedColumnInfo c : colList) {
                if (tblName.equals(c.colInfo.getTableName()))
                    return c.colIndex;
            }
        }

        return -1;
    }


    /**
     * Given a list of column names, this method returns an array containing
     * the indexes of the specified columns.
     *
     * @param columnNames a list of column names in the schema
     *
     * @return an array containing the indexes of the columns specified in the
     *         input
     *
     * @throws SchemaNameException if a column name is specified multiple
     *         times in the input list, or if a column name doesn't appear in
     *         the schema
     */
    public int[] getColumnIndexes(List<String> columnNames) {
        int[] result = new int[columnNames.size()];
        HashSet<String> s = new HashSet<>();

        int i = 0;
        for (String colName : columnNames) {
            if (!s.add(colName)) {
                throw new SchemaNameException(String.format(
                    "Column %s was specified multiple times", colName));
            }

            result[i] = getColumnIndex(colName);
            if (result[i] == -1) {
                throw new SchemaNameException(String.format(
                    "Schema doesn't contain a column named %s", colName));
            }

            i++;
        }

        return result;
    }


    /**
     * Given a (possibly wildcard) column-name, this method returns the collection
     * of all columns that match the specified column name.  The collection is a
     * mapping from integer indexes (the keys) to <code>ColumnInfo</code> objects
     * from the schema.
     * <p>
     * Any valid column-name object will work, so all of these options are
     * available:
     * <ul>
     *   <li><b>No table, only a column name</b> - to resolve an unqualified
     *     column name, e.g. in an expression or predicate</li>
     *   <li><b>A table and column name</b> - to check whether the schema contains
     *     such a column</li>
     *   <li><b>A wildcard without a table name</b> - to retrieve all columns in
     *     the schema</li>
     *   <li><b>A wildcard with a table name</b> - to retrieve all columns
     *     associated with a particular table name</li>
     * </ul>
     */
    public SortedMap<Integer, ColumnInfo> findColumns(ColumnName colName) {

        TreeMap<Integer, ColumnInfo> found = new TreeMap<>();

        if (colName.isColumnWildcard()) {
            // Some kind of wildcard column-name object.

            if (!colName.isTableSpecified()) {
                // Wildcard with no table name:  *
                // Add all columns in the schema to the result.

                for (int i = 0; i < columnInfos.size(); i++)
                    found.put(i, columnInfos.get(i));
            }
            else {
                // Wildcard with a table name:  tbl.*
                // Find the table info and add its columns to the result.

                HashMap<String, IndexedColumnInfo> tableCols =
                    colsHashedByTable.get(colName.getTableName());

                if (tableCols != null) {
                    for (IndexedColumnInfo indexedColInfo: tableCols.values())
                        found.put(indexedColInfo.colIndex, indexedColInfo.colInfo);
                }
            }
        }
        else {
            // A non-wildcard column-name object.

            if (!colName.isTableSpecified()) {
                // Column name with no table name:  col
                // Look up the list of column-info objects grouped by column name.

                ArrayList<IndexedColumnInfo> colList =
                    colsHashedByColumn.get(colName.getColumnName());

                if (colList != null) {
                    for (IndexedColumnInfo indexedColInfo : colList)
                        found.put(indexedColInfo.colIndex, indexedColInfo.colInfo);
                }
            }
            else {
                // Column name with a table name:  tbl.col
                // Find the table info and see if it has the specified column.

                HashMap<String, IndexedColumnInfo> tableCols =
                    colsHashedByTable.get(colName.getTableName());

                if (tableCols != null) {
                    IndexedColumnInfo indexedColInfo =
                        tableCols.get(colName.getColumnName());
                    if (indexedColInfo != null)
                        found.put(indexedColInfo.colIndex, indexedColInfo.colInfo);
                }
            }
        }

        return found;
    }


    /**
     * Adds a column with given index to list of NOT NULL constrained columns.
     *
     * @param colIndex the integer index of the column to NOT NULL constrain.
     *
     * @return true if the column previous was NULLable, or false if the
     *         column already had a NOT NULL constraint on it before this call.
     */
    public boolean addNotNull(int colIndex) {
        if (colIndex < 0 || colIndex >= numColumns()) {
            throw new IllegalArgumentException("Column index must be between" +
                " 0 and " + numColumns() + "; got " + colIndex + " instead.");
        }
        return notNullCols.add(colIndex);
    }


    /**
     * Removes a column with given index from the list of NOT NULL constrained
     * columns.
     *
     * @param colIndex the integer index of the column to remove the NOT NULL
     *        constraint from.
     */
    public boolean removeNotNull(int colIndex) {
        if (colIndex < 0 || colIndex >= numColumns()) {
            throw new IllegalArgumentException("Column index must be between" +
                " 0 and " + numColumns() + "; got " + colIndex + " instead.");
        }
        return notNullCols.remove(colIndex);
    }


    public boolean isNullable(int colIndex) {
        return !notNullCols.contains(colIndex);
    }


    /**
     * Returns a set of columns that have a NOT NULL constraint, specified by
     * the indexes of the columns in the table schema.
     *
     * @return set of integers - indexes of columns with NOT NULL constraint
     */
    public Set<Integer> getNotNull() {
        return Collections.unmodifiableSet(notNullCols);
    }


    /**
     * Returns the primary key on this table, or {@code null} if there is no
     * primary key.
     *
     * @return the primary key on this table, or {@code null} if there is no
     *         primary key.
     */
    public KeyColumnRefs getPrimaryKey() {
        for (KeyColumnRefs ck : candidateKeys) {
            if (ck.getConstraintType() == TableConstraintType.PRIMARY_KEY)
                return ck;
        }

        return null;
    }


    /**
     * Adds another candidate key to the schema.
     *
     * @param ck the candidate key to add to the schema.
     *
     * @throws IllegalArgumentException if {@code ck} is {@code null}, or if
     *         {@code ck} is a primary key and the schema already contains a
     *         primary key.
     */
    public void addCandidateKey(KeyColumnRefs ck) {
        if (ck == null)
            throw new IllegalArgumentException("ck cannot be null");

        // Make sure we don't have more than one primary key.
        if (ck.getConstraintType() == TableConstraintType.PRIMARY_KEY &&
            getPrimaryKey() != null) {
            throw new TableException("Schema already has a primary key");
        }

        candidateKeys.add(ck);
    }


    /**
     * Returns a count of how many candidate keys are present on the schema.
     *
     * @return a count of how many candidate keys are present on the schema.
     */
    public int numCandidateKeys() {
        return candidateKeys.size();
    }


    /**
     * Returns an unmodifiable list of candidate keys present on the schema.
     *
     * @return an unmodifiable list of candidate keys present on the schema.
     */
    public List<KeyColumnRefs> getCandidateKeys() {
        return Collections.unmodifiableList(candidateKeys);
    }


    /**
     * Returns {@code true} if this schema has a candidate key on the set of
     * columns specified in the argument.  The columns are specified by an
     * array of zero-based indexes.
     *
     * @param colIndexes the set of columns to check against this table
     *        to see if it's a candidate key
     *
     * @return {@code true} if this table has a candidate key on the
     *         specified columns; {@code false} otherwise
     */
    public boolean hasKeyOnColumns(int[] colIndexes) {
        return (getKeyOnColumns(colIndexes) != null);
    }


    /**
     * Returns {@code true} if this schema has a candidate key on the set of
     * columns specified in the argument.  The columns are specified by a
     * {@code ColumnRefs} object.
     *
     * @param colRefs the set of columns to check against this table to see if
     *        it's a candidate key
     *
     * @return {@code true} if this table has a candidate key on the
     *         specified columns; {@code false} otherwise
     */
    public boolean hasKeyOnColumns(ColumnRefs colRefs) {
        return hasKeyOnColumns(colRefs.getCols());
    }


    /**
     * Returns any candidate-key from this schema that has the specified set
     * of columns.  Note that the key's columns may be in a different order
     * than those specified in the argument.
     *
     * @param colIndexes the set of columns to check against this table
     *        to see if it's a candidate key
     *
     * @return a candidate key on the specified columns, or {@code null}
     *         if the schema contains no key on the specified columns
     */
    public KeyColumnRefs getKeyOnColumns(int[] colIndexes) {
        for (KeyColumnRefs ck : candidateKeys)
            if (ck.hasSameColumns(colIndexes))
                return ck;

        return null;
    }


    /**
     * Returns any candidate-key from this schema that has the specified set
     * of columns.  Note that the key's columns may be in a different order
     * than those specified in the argument.
     *
     * @param colRefs the set of columns to check against this table to see if
     *        it's a candidate key
     *
     * @return a candidate key on the specified columns, or {@code null}
     *         if the schema contains no key on the specified columns
     */
    public KeyColumnRefs getKeyOnColumns(ColumnRefs colRefs) {
        return getKeyOnColumns(colRefs.getCols());
    }


    /**
     * Returns all candidate-keys from this schema that have the specified set
     * of columns.  Note that keys may specify columns in a different order
     * than those specified in the argument.  If there are no keys on the
     * specified columns, this method will return an empty list.
     *
     * @param colIndexes the set of columns to check against this table
     *        to see if it's a candidate key
     *
     * @return a list of candidate keys on the specified columns
     */
    public List<KeyColumnRefs> getAllKeysOnColumns(int[] colIndexes) {
        ArrayList<KeyColumnRefs> keys = new ArrayList<>();

        for (KeyColumnRefs ck : candidateKeys) {
            if (ck.hasSameColumns(colIndexes))
                keys.add(ck);
        }

        return keys;
    }


    /**
     * Returns all candidate-keys from this schema that have the specified set
     * of columns.  Note that keys may specify columns in a different order
     * than those specified in the argument.  If there are no keys on the
     * specified columns, this method will return an empty list.
     *
     * @param colRefs the set of columns to check against this table
     *        to see if it's a candidate key
     *
     * @return a list of candidate keys on the specified columns
     */
    public List<KeyColumnRefs> getAllKeysOnColumns(ColumnRefs colRefs) {
        return getAllKeysOnColumns(colRefs.getCols());
    }


    /**
     * Adds the specified table to the set of tables that reference this
     * table.
     *
     * @param referencingTableName the name of the table that references this
     *        table
     * @return {@code true} if the table wasn't previously in this table's set
     *         of referencing tables, or {@code false} if the table was
     *         already in the set of referencing tables
     */
    public boolean addReferencingTable(String referencingTableName) {
        return referencingTables.add(referencingTableName);
    }


    public void removeReferencingTable(String referencingTableName) {
        referencingTables.remove(referencingTableName);
    }


    public void addIndex(IndexColumnRefs index) {
        if (index == null)
            throw new IllegalArgumentException("index cannot be null");

        String indexName = index.getIndexName();

        if (indexes.containsKey(indexName)) {
            throw new TableException("Schema already has an index named " +
                indexName);
        }

        indexes.put(indexName, index);
    }


/*
    public void dropIndex(String idxName) {
        if (idxName == null)
            throw new IllegalArgumentException("drop index must specify an index name");

        if (!indexes.containsKey(idxName))
            throw new IllegalArgumentException("table does not have this index to drop");

        if (primaryKey != null && primaryKey.getIndexName().equals(idxName))
            primaryKey = null;
        Iterator<KeyColumnRefs> it = candidateKeys.iterator();
        while (it.hasNext()) {
            if (it.next().getIndexName().equals(idxName))
                it.remove();
        }
        /* Foreign keys should never be able to be dropped this way * /

        indexes.remove(idxName);
    }
*/

    public void addForeignKey(ForeignKeyColumnRefs fk) {
        foreignKeys.add(fk);
    }


    public int numForeignKeys() {
        return foreignKeys.size();
    }


    public List<ForeignKeyColumnRefs> getForeignKeys() {
        return Collections.unmodifiableList(foreignKeys);
    }


    public Set<String> getReferencingTables() {
        return Collections.unmodifiableSet(referencingTables);
    }


    public Collection<IndexColumnRefs> getIndexes() {
        return Collections.unmodifiableCollection(indexes.values());
    }


    public IndexColumnRefs getIndex(String indexName) {
        return indexes.get(indexName);
    }


    /*
     * Given a set of column names, this method returns the names of all
     * indexes built on these columns.
     *
     * @param columnNames the names of columns to test for
     * @return a set of index names built on the specified columns
     *
    public Set<String> getIndexNames(List<String> columnNames) {
        int[] colIndexes = getColumnIndexes(columnNames);
        ColumnRefs index = new ColumnRefs(colIndexes);

        Set<String> indexNames = new HashSet<>();
        for (Map.Entry<String, ColumnRefs> entry : indexes.entrySet()) {
            if (index.hasSameColumns(entry.getValue()))
                indexNames.add(entry.getKey());
        }

        return indexNames;
    }
*/

    public Set<String> getIndexNames() {
        return new HashSet<>(indexes.keySet());
    }


    @Override
    public String toString() {
        return "Schema[cols=" + columnInfos.toString() + "]";
    }
}
