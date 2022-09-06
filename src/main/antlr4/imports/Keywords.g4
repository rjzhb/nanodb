grammar Keywords;

// These are the keywords that NanoDB recognizes.  As usual, the set definitely
// overlaps standard SQL, and it also includes other non-standard commands.
ADD         : [Aa][Dd][Dd] ;
ALL         : [Aa][Ll][Ll] ;
ALTER       : [Aa][Ll][Tt][Ee][Rr] ;
ANALYZE     : [Aa][Nn][Aa][Ll][Yy][Zz][Ee] ;
AND         : [Aa][Nn][Dd] ;
ANY         : [Aa][Nn][Yy] ;
AS          : [Aa][Ss] ;
ASC         : [Aa][Ss][Cc] ;
ASCENDING   : [Aa][Ss][Cc][Ee][Nn][Dd][Ii][Nn][Gg] ;
BEGIN       : [Bb][Ee][Gg][Ii][Nn] ;
BETWEEN     : [Bb][Ee][Tt][Ww][Ee][Ee][Nn] ;
BY          : [Bb][Yy] ;
CASCADE     : [Cc][Aa][Ss][Cc][Aa][Dd][Ee] ;
CHECK       : [Cc][Hh][Ee][Cc][Kk] ;
COLUMN      : [Cc][Oo][Ll][Uu][Mm][Nn] ;
COMMIT      : [Cc][Oo][Mm][Mm][Ii][Tt] ;
CONSTRAINT  : [Cc][Oo][Nn][Ss][Tt][Rr][Aa][Ii][Nn][Tt] ;
CRASH       : [Cc][Rr][Aa][Ss][Hh] ;
CREATE      : [Cc][Rr][Ee][Aa][Tt][Ee] ;
CROSS       : [Cc][Rr][Oo][Ss][Ss] ;
DEFAULT     : [Dd][Ee][Ff][Aa][Uu][Ll][Tt] ;
DELETE      : [Dd][Ee][Ll][Ee][Tt][Ee] ;
DESC        : [Dd][Ee][Ss][Cc] ;
DESCENDING  : [Dd][Ee][Ss][Cc][Ee][Nn][Dd][Ii][Nn][Gg] ;
DISTINCT    : [Dd][Ii][Ss][Tt][Ii][Nn][Cc][Tt] ;
DROP        : [Dd][Rr][Oo][Pp] ;
DUMP        : [Dd][Uu][Mm][Pp] ;
EXCEPT      : [Ee][Xx][Cc][Ee][Pp][Tt] ;
EXISTS      : [Ee][Xx][Ii][Ss][Tt][Ss] ;
EXIT        : [Ee][Xx][Ii][Tt] ;
EXPLAIN     : [Ee][Xx][Pp][Ll][Aa][Ii][Nn] ;
FALSE       : [Ff][Aa][Ll][Ss][Ee] ;
FILE        : [Ff][Ii][Ll][Ee] ;
FLUSH       : [Ff][Ll][Uu][Ss][Hh] ;
FOREIGN     : [Ff][Oo][Rr][Ee][Ii][Gg][Nn] ;
FORMAT      : [Ff][Oo][Rr][Mm][Aa][Tt] ;
FROM        : [Ff][Rr][Oo][Mm] ;
FULL        : [Ff][Uu][Ll][Ll] ;
GROUP       : [Gg][Rr][Oo][Uu][Pp] ;
HAVING      : [Hh][Aa][Vv][Ii][Nn][Gg] ;
IF          : [Ii][Ff] ;
IN          : [Ii][Nn] ;
INDEX       : [Ii][Nn][Dd][Ee][Xx] ;
INNER       : [Ii][Nn][Nn][Ee][Rr] ;
INSERT      : [Ii][Nn][Ss][Ee][Rr][Tt] ;
INTERSECT   : [Ii][Nn][Tt][Ee][Rr][Ss][Ee][Cc][Tt] ;
INTERVAL    : [Ii][Nn][Tt][Ee][Rr][Vv][Aa][Ll] ;
INTO        : [Ii][Nn][Tt][Oo] ;
IS          : [Ii][Ss] ;
JOIN        : [Jj][Oo][Ii][Nn] ;
KEY         : [Kk][Ee][Yy] ;
LEFT        : [Ll][Ee][Ff][Tt] ;
LIKE        : [Ll][Ii][Kk][Ee] ;
LIMIT       : [Ll][Ii][Mm][Ii][Tt] ;
MINUS       : [Mm][Ii][Nn][Uu][Ss] ;
NATURAL     : [Nn][Aa][Tt][Uu][Rr][Aa][Ll] ;
NOT         : [Nn][Oo][Tt] ;
NULL        : [Nn][Uu][Ll][Ll] ;
OFFSET      : [Oo][Ff][Ff][Ss][Ee][Tt] ;
ON          : [Oo][Nn] ;
OPTIMIZE    : [Oo][Pp][Tt][Ii][Mm][Ii][Zz][Ee] ;
OR          : [Oo][Rr] ;
ORDER       : [Oo][Rr][Dd][Ee][Rr] ;
OUTER       : [Oo][Uu][Tt][Ee][Rr] ;
PRIMARY     : [Pp][Rr][Ii][Mm][Aa][Rr][Yy] ;
PROPERTIES  : [Pp][Rr][Oo][Pp][Ee][Rr][Tt][Ii][Ee][Ss] ;
PROPERTY    : [Pp][Rr][Oo][Pp][Ee][Rr][Tt][Yy] ;
QUIT        : [Qq][Uu][Ii][Tt] ;
RECURSIVE   : [Rr][Ee][Cc][Uu][Rr][Ss][Ii][Vv][Ee] ;
REFERENCES  : [Rr][Ee][Ff][Ee][Rr][Ee][Nn][Cc][Ee][Ss] ;
RENAME      : [Rr][Ee][Nn][Aa][Mm][Ee] ;
RESTRICT    : [Rr][Ee][Ss][Tt][Rr][Ii][Cc][Tt] ;
RIGHT       : [Rr][Ii][Gg][Hh][Tt] ;
ROLLBACK    : [Rr][Oo][Ll][Ll][Bb][Aa][Cc][Kk] ;
SELECT      : [Ss][Ee][Ll][Ee][Cc][Tt] ;
SET         : [Ss][Ee][Tt] ;
SHOW        : [Ss][Hh][Oo][Ww] ;
SIMILAR     : [Ss][Ii][Mm][Ii][Ll][Aa][Rr] ;
SOME        : [Ss][Oo][Mm][Ee] ;
START       : [Ss][Tt][Aa][Rr][Tt] ;
STATS       : [Ss][Tt][Aa][Tt][Ss] ;
TABLE       : [Tt][Aa][Bb][Ll][Ee] ;
TABLES      : [Tt][Aa][Bb][Ll][Ee][Ss] ;
TEMPORARY   : [Tt][Ee][Mm][Pp][Oo][Rr][Aa][Rr][Yy] ;
TO          : [Tt][Oo] ;
TRANSACTION : [Tt][Rr][Aa][Nn][Ss][Aa][Cc][Tt][Ii][Oo][Nn] ;
TRUE        : [Tt][Rr][Uu][Ee] ;
TYPE        : [Tt][Yy][Pp][Ee] ;
UNION       : [Uu][Nn][Ii][Oo][Nn] ;
UNIQUE      : [Uu][Nn][Ii][Qq][Uu][Ee] ;
UNKNOWN     : [Uu][Nn][Kk][Nn][Oo][Ww][Nn] ;
UPDATE      : [Uu][Pp][Dd][Aa][Tt][Ee] ;
USING       : [Uu][Ss][Ii][Nn][Gg] ;
VALUES      : [Vv][Aa][Ll][Uu][Ee][Ss] ;
VERBOSE     : [Vv][Ee][Rr][Bb][Oo][Ss][Ee] ;
VERIFY      : [Vv][Ee][Rr][Ii][Ff][Yy] ;
VIEW        : [Vv][Ii][Ee][Ww] ;
WHERE       : [Ww][Hh][Ee][Rr][Ee] ;
WITH        : [Ww][Ii][Tt][Hh] ;
WORK        : [Ww][Oo][Rr][Kk] ;

// These tokens are for type-recognition.  A number of these types have
// additional syntax for specifying length or precision, which is why we have
// parser rules for them.  The type-system is not extensible by database users.
// Note also that not all of these types are supported by NanoDB; they are
// primarily included to reserve the keywords.

TYPE_BIGINT     : [Bb][Ii][Gg][Ii][Nn][Tt] ;
TYPE_BLOB       : [Bb][Ll][Oo][Bb] ;
TYPE_CHAR       : [Cc][Hh][Aa][Rr] ;      // char(length)
TYPE_CHARACTER  : [Cc][Hh][Aa][Rr][Aa][Cc][Tt][Ee][Rr] ; // character(length)
TYPE_DATE       : [Dd][Aa][Tt][Ee] ;
TYPE_DATETIME   : [Dd][Aa][Tt][Ee][Tt][Ii][Mm][Ee] ;
TYPE_DECIMAL    : [Dd][Ee][Cc][Ii][Mm][Aa][Ll] ;   // decimal, decimal(prec), decimal(prec, scale)
TYPE_FLOAT      : [Ff][Ll][Oo][Aa][Tt] ;     // float, float(prec)
TYPE_DOUBLE     : [Dd][Oo][Uu][Bb][Ll][Ee] ;    // double
TYPE_INT        : [Ii][Nn][Tt] ;
TYPE_INTEGER    : [Ii][Nn][Tt][Ee][Gg][Ee][Rr] ;
TYPE_NUMERIC    : [Nn][Uu][Mm][Ee][Rr][Ii][Cc] ;   // numeric, numeric(prec), numeric(prec, scale)
TYPE_SMALLINT   : [Ss][Mm][Aa][Ll][Ll][Ii][Nn][Tt] ;
TYPE_TEXT       : [Tt][Ee][Xx][Tt] ;
TYPE_TIME       : [Tt][Ii][Mm][Ee] ;
TYPE_TIMESTAMP  : [Tt][Ii][Mm][Ee][Ss][Tt][Aa][Mm][Pp] ;
TYPE_TINYINT    : [Tt][Ii][Nn][Yy][Ii][Nn][Tt] ;
TYPE_VARCHAR    : [Vv][Aa][Rr][Cc][Hh][Aa][Rr] ;   // varchar(length)
TYPE_VARYING    : [Vv][Aa][Rr][Yy][Ii][Nn][Gg] ;   // character varying(length)
