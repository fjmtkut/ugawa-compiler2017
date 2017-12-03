import java.io.IOException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import parser.TinyPiSLexer;
import parser.TinyPiSParser;

public class Compiler extends CompilerBase {
	void compileExpr(ASTNode ndx, Environment env) {
		if (ndx instanceof ASTBinaryExprNode) {
			ASTBinaryExprNode nd = (ASTBinaryExprNode) ndx;
			compileExpr(nd.lhs, env);
			emitPUSH(REG_R1);
			emitRR("mov", REG_R1, REG_DST);
			compileExpr(nd.rhs, env);
			if (nd.op.equals("|"))
				emitRR("orr", REG_DST, REG_R1);
			else if (nd.op.equals("&"))
				emitRR("and", REG_DST, REG_R1);
			else if (nd.op.equals("+"))
				emitRRR("add", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("-"))
				emitRRR("sub", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("*"))
				emitRRR("mul", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("/"))
				emitRRR("udiv", REG_DST, REG_R1, REG_DST);
			else
				throw new Error("Unknwon operator: "+nd.op);
			emitPOP(REG_R1);
		} else if (ndx instanceof ASTUnaryExprNode) {
			ASTUnaryExprNode nd = (ASTUnaryExprNode) ndx;
			if (nd.op.equals("~")) {
				compileExpr(nd.operand, env);
				emitRR("mvn", REG_DST, REG_DST);
			} else  {
				compileExpr(nd.operand, env);
				emitRR("mvn", REG_DST, REG_DST);
				emitRRI("add", REG_DST, REG_DST, 1);
			}
		} else if (ndx instanceof ASTNumberNode) {
			ASTNumberNode nd = (ASTNumberNode) ndx;
			emitLDC(REG_DST, nd.value);
		} else if (ndx instanceof ASTVarRefNode) {
			ASTVarRefNode nd = (ASTVarRefNode) ndx;
			Variable var = env.lookup(nd.varName);
			if (var == null)
				throw new Error("Undefined variable: "+nd.varName);
			if (var instanceof GlobalVariable) {
				GlobalVariable globalVar = (GlobalVariable) var;
				emitLDC(REG_DST, globalVar.getLabel());
				emitLDR(REG_DST, REG_DST, 0);
			} else
				throw new Error("Not a global variable: "+nd.varName);
		} else 
			throw new Error("Unknown expression: "+ndx);
	}
	
	void compileStmt(ASTNode ndx, Environment env) {
		if (ndx instanceof ASTCompoundStmtNode) {
//ここから
			ASTCompoundStmtNode nd = (ASTCompoundStmtNode) ndx;
			for (int i = 0; i < nd.stmts.size(); i++){
				compileStmt(nd.stmts.get(i), env);
			}
//ここまで
		} else if (ndx instanceof ASTAssignStmtNode) {
			ASTAssignStmtNode nd = (ASTAssignStmtNode) ndx;
			Variable var = env.lookup(nd.var);
			if (var == null)
				throw new Error("undefined variable: "+nd.var);
			compileExpr(nd.expr, env);
			if (var instanceof GlobalVariable) {
				GlobalVariable globalVar = (GlobalVariable) var;
				emitLDC(REG_R1, globalVar.getLabel());
				emitSTR(REG_DST, REG_R1, 0);
			} else
				throw new Error("Not a global variable: "+nd.var);
		} else if (ndx instanceof ASTIfStmtNode) {
			ASTIfStmtNode nd = (ASTIfStmtNode) ndx;
			String elseLabel = freshLabel();
			String endLabel = freshLabel();
			compileExpr(nd.cond, env);
			emitRI("cmp", REG_DST, 0);
			emitJMP("beq", elseLabel);
			compileStmt(nd.thenClause, env);
			emitJMP("b", endLabel);
			emitLabel(elseLabel);
			compileStmt(nd.elseClause, env);
			emitLabel(endLabel);
		} else if (ndx instanceof ASTWhileStmtNode) {
//ここから
			ASTWhileStmtNode nd = (ASTWhileStmtNode) ndx;
			compileExpr(nd.cond, env);
			String loop = freshLabel();
			String endloop = freshLabel();
			emitLabel(loop);
			emitRI("cmp", REG_DST, 0);
			emitJMP("beq", endloop);
			compileStmt(nd.stmt, env);
			emitJMP("b", loop);
			emitLabel(endloop);
//ここまで
//ここから演習13
		} else if (ndx instanceof ASTPrintStmtNode) {
			ASTPrintStmtNode nd = (ASTPrintStmtNode) ndx;
			compileExpr(nd.expr, env);
			emitCALL("print_func");
//ここまで
		} else
			throw new Error("Unknown expression: "+ndx);
	}
	
	void compile(ASTNode ast) {
		Environment env = new Environment();
		ASTProgNode prog = (ASTProgNode) ast;
		System.out.println("\t.section .data");
		System.out.println("\t@ 大域変数の定義");
		for (String varName: prog.varDecls) {
			if (env.lookup(varName) != null)
				throw new Error("Variable redefined: "+varName);
			GlobalVariable v = addGlobalVariable(env, varName);
			emitLabel(v.getLabel());
			System.out.println("\t.word 0");
		}
		if (env.lookup("answer") == null) {
			GlobalVariable v = addGlobalVariable(env, "answer");
			emitLabel(v.getLabel());
			System.out.println("\t.word 0");
		}
		System.out.println("buf:\t.space 8\n\t.byte 10");
		System.out.println("\t.section .text");
		System.out.println("\t.global _start");
//ここから演習13
		System.out.println("print_func:");
		String print_loop = freshLabel();
		emitPUSH(REG_LR);
		emitPUSH(REG_DST);
		emitPUSH(REG_R1);
		emitPUSH(REG_R2);
		emitPUSH(REG_R3);
		emitPUSH(REG_R7);
		emitRRI("sub", "sp", "sp", 4);
		emitLDC(REG_R1, "buf");
		emitRRI("add", REG_R1, REG_R1, 8);
		emitRI("mov", REG_R2, 4);
		emitLabel(print_loop);
		emitRRI("sub", REG_R1, REG_R1, 1);
		emitRI("mov", REG_R7, 32);
		emitRRR("sub", REG_R7, REG_R7, REG_R2);
		System.out.println("\tmov r3, r0, lsl r7");
		System.out.println("\tmov r3, r3, lsr #28");
		emitRI("cmp", REG_R3, 9);
		emitRRI("addls", REG_R3, REG_R3, 48);
		emitRRI("addhi", REG_R3, REG_R3, 55);
		emitRR("strb", REG_R3, "[r1]");
		emitRRI("add", REG_R2, REG_R2, 4);
		emitRI("cmp", REG_R2, 32);
		System.out.println("\tbls " + print_loop);
		emitRI("mov", REG_R7, 4);
		emitRI("mov", REG_DST, 1);
		emitLDC(REG_R1, "buf");
		emitRI("mov", REG_R2, 9);
		emitI("swi", 0);
		emitRRI("add", "sp", "sp", 4);
		emitPOP(REG_R7);
		emitPOP(REG_R3);
		emitPOP(REG_R2);
		emitPOP(REG_R1);
		emitPOP(REG_DST);
		emitPOP(REG_LR);
		emitRET();
//ここまで
		System.out.println("_start:");
		System.out.println("\t@ 式をコンパイルした命令列");
		compileStmt(prog.stmt, env);
		System.out.println("\t@ EXITシステムコール");
		GlobalVariable v = (GlobalVariable) env.lookup("answer");
		emitLDC(REG_DST, v.getLabel());  //変数answerの値をr0 (終了コード)に入れる
		emitLDR("r0", REG_DST, 0);
		emitRI("mov", "r7", 1);
		// EXITのシステムコール番号
		emitI("swi", 0);
	}

	public static void main(String[] args) throws IOException {
		ANTLRInputStream input = new ANTLRInputStream(System.in);
		TinyPiSLexer lexer = new TinyPiSLexer(input);
		CommonTokenStream token = new CommonTokenStream(lexer);
		TinyPiSParser parser = new TinyPiSParser(token);
		ParseTree tree = parser.prog();
		ASTGenerator astgen = new ASTGenerator();
		ASTNode ast = astgen.translate(tree);
		Compiler compiler = new Compiler();
		compiler.compile(ast);
	}
}
