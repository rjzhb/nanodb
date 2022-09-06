grammar NanoSQL;

import Keywords, Lexer;


// A series of one or more commands.  (Not sure if I should allow this to
// produce 0 commands as well. -Donnie)
commands:
      command+ ;

// All commands are required to end with a semicolon.  If we don't have any
// terminator then we don't know when an interactive command is actually
// completely entered.  For example, "SELECT * FROM foo" would parse correctly
// even though the user meant to type "... WHERE b > 5".  By requiring the
// semicolon we avoid this issue.
command:
        commandNoSemicolon ';' ;

// Separate this rule out from the "command" rule so that the visitor
// implementation doesn't have to do anything fancy.
commandNoSemicolon:
        createTableStmt
      | createIndexStmt
      | dropTableStmt
      | dropIndexStmt
      | selectStmt
      | insertStmt
      | updateStmt
      | deleteStmt
      | explainStmt
      | beginTxnStmt
      | commitTxnStmt
      | rollbackTxnStmt
      | analyzeStmt
      | exitStmt
      | crashStmt
      | dumpTableStmt
      | dumpIndexStmt
      | flushStmt
      | verifyStmt
      | optimizeStmt
      | showTableStatsStmt
      | showTablesStmt
      | showPropsStmt
      | setPropStmt
      | showSystemStatsStmt
      ;


//============================================================================
// CREATE TABLE
//
// The rules for CREATE TABLE commands are somewhat involved since they have
// many components and different kinds of constraints that can be specified.
//

createTableStmt:
        CREATE (TEMPORARY)? TABLE (IF NOT EXISTS)? tableName=IDENT '('
        ( tableColDecl | tableConstraint )
        ( ',' ( tableColDecl | tableConstraint ) )*
        ')'
        cmdProperties?
        ;

tableColDecl:
        columnName=IDENT columnType columnConstraint* ;

columnType:
        ( TYPE_INT | TYPE_INTEGER )                 # ColTypeInt
      | TYPE_BIGINT                                 # ColTypeBigInt
      | (TYPE_DECIMAL | TYPE_NUMERIC)
            ('(' precision=INT_LITERAL (',' scale=INT_LITERAL)? ')')?               # ColTypeDecimal
      | TYPE_FLOAT                                  # ColTypeFloat
      | TYPE_DOUBLE                                 # ColTypeDouble
      | (TYPE_CHAR | TYPE_CHARACTER) '(' length=INT_LITERAL ')'                     # ColTypeChar
      | (TYPE_VARCHAR | (TYPE_CHARACTER TYPE_VARYING)) '(' length=INT_LITERAL ')'   # ColTypeVarChar
      | TYPE_DATE                                   # ColTypeDate
      | TYPE_DATETIME                               # ColTypeDateTime
      | TYPE_TIME                                   # ColTypeTime
      | TYPE_TIMESTAMP                              # ColTypeTimestamp
      ;

// Table columns can have a number of constraints, which may optionally be
// named.  Note that column-constraints and table-constraints can be quite
// different, even though they are represented with the same Java class in
// the implementation.
//
// The rule is written with the repeated components so that we can use the
// nifty alternative-naming feature of Antlr4.
columnConstraint:
          (CONSTRAINT constraintName=IDENT)? NOT NULL                   # ColConstraintNotNull
        | (CONSTRAINT constraintName=IDENT)? UNIQUE                     # ColConstraintUnique
        | (CONSTRAINT constraintName=IDENT)? PRIMARY KEY                # ColConstraintPrimaryKey
        | (CONSTRAINT constraintName=IDENT)?
              REFERENCES refTableName=IDENT ('(' refColumnName=IDENT ')')?
              (ON DELETE delOpt=cascadeOption)?
              (ON UPDATE updOpt=cascadeOption)?                         # ColConstraintForeignKey
        ;

// Table columns can have a number of constraints, which may optionally be
// named.  Note that column-constraints and table-constraints can be quite
// different, even though they are represented with the same Java class in
// the implementation.
tableConstraint:
          (CONSTRAINT constraintName=IDENT)? UNIQUE
              '(' columnName+=IDENT (',' columnName+=IDENT)* ')'        # TblConstraintUnique
        | (CONSTRAINT constraintName=IDENT)? PRIMARY KEY
              '(' columnName+=IDENT (',' columnName+=IDENT)* ')'        # TblConstraintPrimaryKey
        | (CONSTRAINT constraintName=IDENT)? FOREIGN KEY
              '(' columnName+=IDENT (',' columnName+=IDENT)* ')'
              REFERENCES refTableName=IDENT ('(' refColumnName+=IDENT (',' refColumnName+=IDENT)* ')')?
              (ON DELETE delOpt=cascadeOption)?
              (ON UPDATE updOpt=cascadeOption)?                         # TblConstraintForeignKey
        ;


cascadeOption:
          RESTRICT  # CascadeOptRestrict
        | CASCADE   # CascadeOptCascade
        | SET NULL  # CascadeOptSetNull
        ;

//============================================================================
// DROP TABLE ...
//

dropTableStmt:
        DROP TABLE (IF EXISTS)? tableName=IDENT ;


//============================================================================
// CREATE INDEX ...
//

createIndexStmt:
        CREATE UNIQUE? INDEX (IF NOT EXISTS)?
        indexName=IDENT ON tableName=IDENT
        '(' columnName+=IDENT (',' columnName+=IDENT)* ')'
        cmdProperties?
        ;


//============================================================================
// DROP INDEX ...
//

dropIndexStmt:
        DROP INDEX (IF EXISTS)? indexName=IDENT ON tableName=IDENT ;


//============================================================================
// Table utility operations:
//   SHOW TABLES
//   ANALYZE table [, table ...]
//   OPTIMIZE table [, table ...]
//   VERIFY table [, table ...]
//   DUMP TABLE table ...
//   DUMP INDEX table.index ...
//

showTablesStmt:
        SHOW TABLES ;

analyzeStmt:
        ANALYZE IDENT ( ',' IDENT )* ;

optimizeStmt:
        OPTIMIZE IDENT ( ',' IDENT )* ;

verifyStmt:
        VERIFY IDENT ( ',' IDENT )* ;

dumpTableStmt:
        DUMP TABLE tableName=IDENT
        ( TO FILE fileName=STRING_LITERAL )?
        ( FORMAT format=STRING_LITERAL )?
        ;

dumpIndexStmt:
        DUMP INDEX tableName=IDENT '.' indexName=IDENT
        ( TO FILE fileName=STRING_LITERAL )?
        ( FORMAT format=STRING_LITERAL )?
        ;

showTableStatsStmt:
        SHOW TABLE tableName=IDENT STATS ;


//============================================================================
// Transaction processing statements
//   START TRANSACTION / BEGIN WORK
//   COMMIT WORK
//   ROLLBACK WORK
//

beginTxnStmt:
        START TRANSACTION | BEGIN WORK? ;

commitTxnStmt:
        COMMIT WORK? ;

rollbackTxnStmt:
        ROLLBACK WORK? ;


//============================================================================
// Other utility statements
//   SHOW PROPERTIES
//   SET PROPERTY '...' = ...
//   SHOW '...' STATS
//   FLUSH
//   CRASH
//   EXIT (or QUIT)
//

showPropsStmt:
        SHOW PROPERTIES (LIKE pattern=STRING_LITERAL)? ;

setPropStmt:
        SET PROPERTY name=STRING_LITERAL '=' expression ;

showSystemStatsStmt:
        SHOW name=STRING_LITERAL STATS ;

flushStmt:
        FLUSH ;

crashStmt:
        CRASH INT_LITERAL? ;

exitStmt:
        ( EXIT | QUIT ) ;


//============================================================================
// SELECT
//

// NOTE:  This approach to supporting the WITH clause is completely broken.
//        The SELECT statements combined with set-operations need to have no
//        WITH or ORDER BY clauses.  The WITH and ORDER BY clauses should
//        be applied after set-operations.  Nested subqueries can use these
//        clauses if they are parenthesized.
//
//        Oh, and INSERT / UPDATE / DELETE clauses can also specify WITH
//        clauses...
//
// selectStmt:
//          (withClause? selectStmtNoWith)
//        | selectStmt (UNION | EXCEPT | INTERSECT) ALL? selectStmt
//             orderByClause? limitOffsetClause?
//         | '(' selectStmt ')'
//         ;
//
// withClause:
//         WITH RECURSIVE? commonTableExpression (',' commonTableExpression)* ;
//
// commonTableExpression:
//         IDENT AS selectStmtNoWith ;
//
// selectStmtNoWith:

selectStmt:
        SELECT (ALL | DISTINCT)? selectValue (',' selectValue)*
        ( FROM fromExpr )?
        ( WHERE wherePred=expression )?
        ( GROUP BY groupExpr+=expression (',' groupExpr+=expression)*
          (HAVING havingPred=expression)? )?
        ( ORDER BY orderByExpr (',' orderByExpr)* )?
        ( LIMIT limit=INT_LITERAL)?
        ( OFFSET offset=INT_LITERAL)?
        ;

selectValue:
          expression (AS? alias=IDENT)? ;

joinType:
          INNER         # JoinTypeInner
        | LEFT OUTER?   # JoinTypeLeftOuter
        | RIGHT OUTER?  # JoinTypeRightOuter
        | FULL OUTER?   # JoinTypeFullOuter
        ;

fromExpr:
          fromExpr CROSS JOIN fromExpr                                  # FromCrossJoin
        | fromExpr NATURAL joinType? JOIN fromExpr                      # FromNaturalJoin
        | fromExpr joinType? JOIN fromExpr ON expression                # FromJoinOn
        | fromExpr joinType? JOIN fromExpr
              USING '(' columnName+=IDENT (',' columnName+=IDENT)* ')'  # FromJoinUsing
        | fromExpr ',' fromExpr                                         # FromImplicitCrossJoin
        | tableName=IDENT (AS? alias=IDENT)?                            # FromTable
        | functionCall (AS? alias=IDENT)?                               # FromTableFunction
        | '(' selectStmt ')' AS? alias=IDENT                            # FromNestedSelect
        | '(' fromExpr ')'                                              # FromParens
        ;

orderByExpr:
        expression (ASC | ASCENDING | DESC | DESCENDING)? ;


insertStmt:
        INSERT INTO tableName=IDENT
        ( '(' (columnName+=IDENT (',' columnName+=IDENT)*)? ')' )?  // Optional column-list
        ( VALUES expressionList | selectStmt )
        ;


updateStmt:
        UPDATE tableName=IDENT
        SET columnName+=IDENT '=' expression (',' columnName+=IDENT '=' expression)*
        (WHERE predicate=expression)?
        ;


deleteStmt:
        DELETE FROM tableName=IDENT (WHERE expression)? ;


explainStmt:
          EXPLAIN selectStmt    # ExplainSelect
        | EXPLAIN insertStmt    # ExplainInsert
        | EXPLAIN updateStmt    # ExplainUpdate
        | EXPLAIN deleteStmt    # ExplainDelete
        ;

//============================================================================
// Some commands take properties to specify e.g. page-size for tables, type of
// storage format, etc.
//

cmdProperties:
        PROPERTIES '(' name+=IDENT '=' literalValue
                  (',' name+=IDENT '=' literalValue)* ')' ;

//============================================================================
// All kinds of expressions!
//

// The general expression parsing rule.  ANTLR4 doesn't explicitly specify
// operator precedence; the order implicitly specifies precedence order from
// high to low.
//
// Also, we would really like to be able to factor out different kinds of
// expressions into subrules (e.g. subquery operations, or compare operations)
// but ANTLR4 can only handle left-recursive rules if they are within a single
// rule, not spaning multiple mutually left-recursive rules.  Therefore, we
// can either have a simple grammar that throws everything into one bucket, or
// we can have a very large complicated grammar that separates things into
// different categories.  (The previous grammar had nearly 300 lines for
// expressions alone; this is well under 50 lines.)
expression:
        literalValue                                        # ExprLiteral
      | columnRef                                           # ExprColumnRef
      | functionCall                                        # ExprFunctionCall
      | op=('+' | '-') expression                           # ExprUnarySign
      | expression op=('*' | '/' | '%') expression          # ExprMul
      | expression op=('+' | '-') expression                # ExprAdd
      | expression op=( '<' | '<=' | '>' | '>=' | '=' | '==' | '!=' | '<>' ) expression     # ExprCompare
      | expression IS NOT? NULL                             # ExprIsNull
      | expression NOT? BETWEEN expression AND expression   # ExprBetween
      | expression NOT? LIKE expression                     # ExprLike
      | expression NOT? SIMILAR TO expression               # ExprSimilarTo
      | expression NOT? IN expressionList                   # ExprOneColInValues
      | expression NOT? IN '(' selectStmt ')'               # ExprOneColInSubquery
      | expressionList NOT? IN '(' selectStmt ')'           # ExprMultiColInSubquery
      | EXISTS '(' selectStmt ')'                           # ExprExists
      | NOT expression                                      # ExprNot
      | expression AND expression                           # ExprAnd
      | expression OR expression                            # ExprOr
      | '(' selectStmt ')'                                  # ExprScalarSubquery
      | '(' expression ')'                                  # ExprParen
      ;

literalValue:
        NULL                        # LiteralNull
      | TRUE                        # LiteralTrue
      | FALSE                       # LiteralFalse
      | INT_LITERAL                 # LiteralInteger
      | DECIMAL_LITERAL             # LiteralDecimal
      | STRING_LITERAL              # LiteralString
      | INTERVAL STRING_LITERAL     # LiteralInterval
      ;

// To keep the parsing simple, we just have four separate subrules for column
// references.
columnRef:
        tableName=IDENT '.' columnName=IDENT    # ColRefTable
      | columnName=IDENT                        # ColRefNoTable
      | tableName=IDENT '.' '*'                 # ColRefWildcardTable
      | '*'                                     # ColRefWildcardNoTable
      ;

// A list of zero or more expressions, wrapped with parentheses.  This is
// useful for function calls, INSERT statements, IN clauses, etc.
//
expressionList: '(' (expression (',' expression)*)? ')' ;

// Note that we have a "DISTINCT" keyword in this parse rule since it is used
// to parse aggregate functions, and aggregates support this syntax.
// If a "DISTINCT" modifier is inappropriate for a given kind of function,
// the code should detect this and complain about it.
functionCall:
        functionName=IDENT '(' (DISTINCT? expression (',' expression)*)? ')' ;
