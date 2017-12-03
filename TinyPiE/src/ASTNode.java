public class ASTNode {}

//ここから演習３
class ASTUnaryExprNode extends ASTNode {
	String op;
	//String operand;
	ASTNode operand;
	ASTUnaryExprNode(String op, ASTNode operand) {
		this.op = op;
		this.operand = operand;
	}
//ここまで
	
	public String toString() {
		return "(UnExpr " +op+ " " +operand+ ")";
	}
}

class ASTBinaryExprNode extends ASTNode {
	String op;
	ASTNode lhs;
	ASTNode rhs;
	ASTBinaryExprNode(String op, ASTNode lhs, ASTNode rhs) {
		this.op = op;
		this.lhs = lhs;
		this.rhs = rhs;
	}
	@Override
	public String toString() {
		return "(BinExpr "+op+" "+lhs+" "+rhs+")";
	}
}

class ASTNumberNode extends ASTNode {
	int value;
	ASTNumberNode(int value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return "(Number "+value+")";
	}
}

class ASTVarRefNode extends ASTNode {
	String varName;
	ASTVarRefNode(String varName) {
		this.varName = varName;
	}
	@Override
	public String toString() {
		return "(VarRef "+varName+")";
	}
}