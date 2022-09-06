package edu.caltech.nanodb.storage;


import edu.caltech.nanodb.relations.Schema;


/**
 * This interface defines the operations that can be performed on
 * {@link TupleFile}s, but that are at a higher level of implementation than
 * the tuple file itself.  Examples of such operations are creating a new
 * tuple file on disk, deleting a tuple file from the disk, and so forth.
 */
public interface TupleFileManager {
    /**
     * Returns the "short name" used to refer to this storage-file type.  This
     * is the name used in e.g. <tt>CREATE TABLE</tt> commands to specify that
     * a table-file should use a particular storage-file type.
     *
     * @return the "short name" used to refer to this storage-file type.
     */
    String getShortName();


    /**
     * Returns the {@link DBFileType} value used to indicate this storage
     * format in data files.
     *
     * @return the {@code DBFileType} value used to indicate this storage
     *         format in data files.
     */
    DBFileType getDBFileType();


    /**
     * Initialize the specified {@code DBFile} to be a new empty table
     * with the specified schema.
     *
     * @param dbFile the file to use for the new tuple-file
     * @param schema the schema of the table data to store in the file
     * @return a {@code TupleFile} object that allows tuple operations to be
     *         performed against the {@link DBFile}.
     */
    TupleFile createTupleFile(DBFile dbFile, Schema schema);


    /**
     * Open the specified {@code DBFile} as a {@link TupleFile} of a specific
     * type, containing tuples of data conforming to a specific schema.
     *
     * @param dbFile the tuple-file to open
     * @return a {@code TupleFile} object that allows tuple operations to be
     *         performed against the {@link DBFile}.
     */
    TupleFile openTupleFile(DBFile dbFile);


    /**
     * Writes the metadata (schema and stats information) for the tuple-file
     * back into the tuple-file.  The schema and statistics are represented
     * as separate objects ({@link Schema} and
     * {@link edu.caltech.nanodb.queryeval.TableStats} objects, specifically),
     * so of course they need to be serialized into a binary representation
     * for storage when they change.
     *
     * @param tupleFile the tuple-file whose metadata should be persisted
     */
    void saveMetadata(TupleFile tupleFile);


    /**
     * Delete the specified tuple-file.
     *
     * @param tupleFile the tuple-file to delete
     */
    void deleteTupleFile(TupleFile tupleFile);
}
