// antlr4 -package parser -o antlr-generated  -no-listener parser/PiLang.g4
grammar PiLang;

prog: varDecls funcDecl*
	;

funcDecl: 'function' IDENTIFIER '(' params ')' '{' varDecls stmt* '}'
    ;

params:   /* no parameter */
    | IDENTIFIER (',' IDENTIFIER)*
    ;

varDecls: ('var' IDENTIFIER ';')*
    ;

stmt: '{' stmt* '}'							# compoundStmt
	| IDENTIFIER '=' expr ';'				# assignStmt
	| 'if' '(' expr ')' stmt 'else' stmt	# ifStmt
	| 'while' '(' expr ')' stmt				# whileStmt
	| 'print' expr ';'						# printStmt
	| 'return' expr ';'						# returnStmt
	;

expr: orExpr
      ;

orExpr: orExpr OROP andExpr
	| andExpr
	;
	
andExpr: andExpr ANDOP addExpr
	| addExpr
	;

addExpr: addExpr (ADDOP|SUBOP) mulExpr
	| mulExpr
	;

mulExpr: mulExpr MULOP unaryExpr
	| unaryExpr
	;

unaryExpr: (NOTOP|SUBOP) unaryExpr	# notExpr
	| VALUE			# literalExpr
	| IDENTIFIER			# varExpr
	| '(' expr ')'			# parenExpr
	| IDENTIFIER '(' args ')' # callExpr
	;

args: /* no arguments */
	| expr ( ',' expr )*
	;

ADDOP: '+';
SUBOP: '-';
MULOP: '*'|'/';
ANDOP: '&';
OROP:	'|';
NOTOP:	'~';

IDENTIFIER: '_'[a-zA-Z0-9_]*|[a-zA-Z][a-zA-Z0-9_]*;
VALUE: [1-9][0-9]*|[0];
WS: [ \t\r\n] -> skip;
