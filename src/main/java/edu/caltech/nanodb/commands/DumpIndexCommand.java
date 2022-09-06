package edu.caltech.nanodb.commands;


import edu.caltech.nanodb.indexes.IndexInfo;
import edu.caltech.nanodb.indexes.IndexManager;
import edu.caltech.nanodb.plannodes.FileScanNode;
import edu.caltech.nanodb.plannodes.PlanNode;
import edu.caltech.nanodb.relations.TableInfo;
import edu.caltech.nanodb.server.NanoDBServer;
import edu.caltech.nanodb.storage.TableManager;


/**
 * <p>
 * This command object represents a <tt>DUMP INDEX</tt> command issued against
 * the database.  <tt>DUMP INDEX</tt> commands are pretty simple, having a
 * single form:   <tt>DUMP INDEX ... ON TABLE ... [TO FILE ...] [FORMAT ...]</tt>.
 * </p>
 */
public class DumpIndexCommand extends DumpCommand {

    /** The name of the index to dump. */
    private String indexName;


    /** The name of the table containing the index to dump. */
    private String tableName;


    public DumpIndexCommand(String indexName, String tableName,
                            String fileName, String format) {
        super(fileName, format);

        if (indexName == null)
            throw new IllegalArgumentException("indexName cannot be null");

        if (tableName == null)
            throw new IllegalArgumentException("tableName cannot be null");

        this.indexName = indexName;
        this.tableName = tableName;

    }


    /**
     * Get the name of the table containing the index to be dumped.
     *
     * @return the name of the table containing the index to dump
     */
    public String getTableName() {
        return tableName;
    }


    /**
     * Get the name of the index to be dumped.
     *
     * @return the name of the index to dump
     */
    public String getIndexName() {
        return indexName;
    }


    @Override
    protected PlanNode prepareDumpPlan(NanoDBServer server) {
        // Open the table so we can open the index.
        TableManager tblMgr = server.getStorageManager().getTableManager();
        TableInfo tblInfo = tblMgr.openTable(tableName);

        // Open the index.
        IndexManager idxMgr = server.getStorageManager().getIndexManager();
        IndexInfo idxInfo = idxMgr.openIndex(tblInfo, indexName);

        // Make a file-scan plan-node over the index.
        PlanNode plan = new FileScanNode(idxInfo, null);
        plan.prepare();

        // Done!
        return plan;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("DumpTableCommand[table=");
        sb.append(tableName);

        if (fileName != null) {
            sb.append(", filename=\"");
            sb.append(fileName);
            sb.append("\"");
        }

        if (format != null) {
            sb.append(", format=");
            sb.append(format);
        }

        sb.append(']');

        return sb.toString();
    }

}
