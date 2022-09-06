grammar Literals;

//============================================================================
// Literals
//   We keep literals pretty simple in NanoDB.  Values can be single-quoted
//   strings, or integer values (converted to Integer/Long/BigInteger as
//   necessary for precision), or decimal values (converted to BigDecimal,
//   but also castable to float or double as necessary).
//

IDENT:
        [A-Za-z_] [A-Za-z0-9_]* ;

INT_LITERAL:
        [0-9]+ ;

DECIMAL_LITERAL:
        [0-9]+ '.' [0-9]* | '.' [0-9]+ ;

STRING_LITERAL:
        '\'' .*? '\'' ;

//============================================================================
// Stuff We Ignore
//

// Define whitespace rule, toss it out
WS:
        [ \t\r\n]+ -> skip ;

// Toss out comments as well

COMMENT:
        '/*' .*? '*/' -> skip ;

LINE_COMMENT:
        '--' .*? '\n' -> skip ;
