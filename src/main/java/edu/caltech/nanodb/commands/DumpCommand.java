package edu.caltech.nanodb.commands;


import java.io.FileNotFoundException;
import java.io.PrintStream;

import edu.caltech.nanodb.plannodes.PlanNode;
import edu.caltech.nanodb.queryeval.EvalStats;
import edu.caltech.nanodb.queryeval.QueryEvaluator;
import edu.caltech.nanodb.queryeval.TupleProcessor;
import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.Tuple;
import edu.caltech.nanodb.server.NanoDBServer;


/**
 * An abstract base-class that holds the common implementation of the various
 * kinds of dump commands.
 */
public abstract class DumpCommand extends Command {

    /** A tuple processor implementation used to dump each tuple. */
    protected static class TupleExporter implements TupleProcessor {
        private PrintStream dumpOut;


        /**
         * Initialize the tuple-exporter object with the details it needs to
         * print out tuples from the specified table.
         */
        public TupleExporter(PrintStream dumpOut) {
            this.dumpOut = dumpOut;
        }


        /** The exporter can output the schema to the dump file. */
        public void setSchema(Schema schema) {
            dumpOut.print("{");

            boolean first = true;
            for (ColumnInfo colInfo : schema) {
                if (first)
                    first = false;
                else
                    dumpOut.print(",");

                String colName = colInfo.getName();
                String tblName = colInfo.getTableName();

                // TODO:  To only print out table-names when the column-name
                //        is ambiguous by itself, uncomment the first part and
                //        then comment out the next part.

                // Only print out the table name if there are multiple columns
                // with this column name.
                // if (schema.numColumnsWithName(colName) > 1 && tblName != null)
                //     out.print(tblName + '.');

                // If table name is specified, always print it out.
                if (tblName != null)
                    dumpOut.print(tblName + '.');

                dumpOut.print(colName);

                dumpOut.print(":");
                dumpOut.print(colInfo.getType());
            }
            dumpOut.println("}");
        }

        /** This implementation simply prints out each tuple it is handed. */
        public void process(Tuple tuple) {
            dumpOut.print("[");
            boolean first = true;
            for (int i = 0; i < tuple.getColumnCount(); i++) {
                if (first)
                    first = false;
                else
                    dumpOut.print(", ");

                Object val = tuple.getColumnValue(i);
                if (val instanceof String)
                    dumpOut.printf("\"%s\"", val);
                else
                    dumpOut.print(val);
            }
            dumpOut.println("]");
        }

        public void finish() {
            // Not used
        }
    }


    /** The path and filename to dump the table data to, if desired. */
    protected String fileName;


    /** The data format to use when dumping the table data. */
    protected String format;


    protected DumpCommand(String fileName, String format) {
        super(Command.Type.UTILITY);

        this.fileName = fileName;
        this.format = format;
    }


    public String getFilename() {
        return fileName;
    }


    public String getFormat() {
        return format;
    }


    @Override
    public void execute(NanoDBServer server) throws ExecutionException {
        // Figure out where the dumped data should go.
        PrintStream dumpOut = out;
        if (fileName != null) {
            try {
                dumpOut = new PrintStream(fileName);
            }
            catch (FileNotFoundException e) {
                throw new ExecutionException(e);
            }
        }

        // Dump the table.
        PlanNode dumpPlan = prepareDumpPlan(server);
        TupleExporter exporter = new TupleExporter(dumpOut);
        EvalStats stats = QueryEvaluator.executePlan(dumpPlan, exporter);

        if (fileName != null)
            dumpOut.close();

        // Print out the evaluation statistics.
        out.printf("Dumped %d rows in %f sec.%n",
            stats.getRowsProduced(), stats.getElapsedTimeSecs());
    }


    protected abstract PlanNode prepareDumpPlan(NanoDBServer server);
}
