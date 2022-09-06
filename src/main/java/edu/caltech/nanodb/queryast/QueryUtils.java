package edu.caltech.nanodb.queryast;


import edu.caltech.nanodb.expressions.ColumnName;
import edu.caltech.nanodb.expressions.ColumnValue;
import edu.caltech.nanodb.expressions.Expression;


public class QueryUtils {

    /**
     * Given a table name and an optional <tt>WHERE</tt> predicate, this
     * method constructs a query AST for the SQL
     * "<tt>SELECT * FROM table [WHERE expr]</tt>".
     *
     * @param tableName the name of the table to select from
     * @param whereExpr the predicate to use for selecting rows, or
     *        {@code null} if no predicate should be applied
     *
     * @return a query AST for selecting the rows from the table
     */
    public static SelectClause makeSelectStar(String tableName,
                                              Expression whereExpr) {
        SelectClause selectClause = new SelectClause();

        ColumnName colName = new ColumnName(null); // wildcard
        ColumnValue colValue = new ColumnValue(colName);
        selectClause.addSelectValue(new SelectValue(colValue));

        FromClause fromClause = new FromClause(tableName, null);
        selectClause.setFromClause(fromClause);

        if (whereExpr != null)
            selectClause.setWhereExpr(whereExpr);

        return selectClause;
    }


    /**
     * Given a table name, this method constructs a query AST for the SQL
     * "<tt>SELECT * FROM table</tt>".
     *
     * @param tableName the name of the table to select from
     * @return a query AST for selecting all rows from the table
     */
    public static SelectClause makeSelectStar(String tableName) {
        return makeSelectStar(tableName, null);
    }
}
