import java.io.IOException;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

//import CompilerBase.GlobalVariable;
//import CompilerBase.Variable;
import parser.PiLangLexer;
import parser.PiLangParser;

public class Compiler extends CompilerBase {
	Environment globalEnv;
	
	void compileFunction(ASTFunctionNode nd) {
		Environment env = new Environment();
		String epilogueLabel = freshLabel();
		for (int i = 0; i < nd.params.size(); i++) {
			String name = nd.params.get(i);
			int offset = 4 * (i + 1);
			LocalVariable var = new LocalVariable(name, offset);
			env.push(var);
		}
		/* ここにプログラムを追加する */
		for (int j = 0; j < nd.varDecls.size(); j++) {
			String name = nd.varDecls.get(j);
			int offset = -4 * (j + 3);
			LocalVariable var = new LocalVariable(name, offset);
			env.push(var);
		}
		emitLabel(nd.name);
		System.out.println("\t@ prologue");
		emitPUSH(REG_FP);
		emitRR("mov", REG_FP, REG_SP);
		emitPUSH(REG_LR);
		emitPUSH(REG_R1);
		emitRRI("sub", REG_SP, REG_SP, nd.varDecls.size() * 4);
		for (ASTNode stmt: nd.stmts)
			compileStmt(stmt, epilogueLabel, env);
		emitRI("mov", REG_DST, 0);  // returnがなかったときの戻り値0
		emitLabel(epilogueLabel);
		System.out.println("\t@ epilogue");
		emitRRI("add", REG_SP, REG_SP, nd.varDecls.size() * 4);
		emitPOP(REG_R1);
		emitPOP(REG_LR);
		emitPOP(REG_FP);
		emitRET();
	}
	
	void compileStmt(ASTNode ndx, String epilogueLabel, Environment env) {
		/* ここを完成させる */
		if (ndx instanceof ASTCompoundStmtNode) {
			ASTCompoundStmtNode nd = (ASTCompoundStmtNode) ndx;
			for (int i = 0; i < nd.stmts.size(); i++){
				compileStmt(nd.stmts.get(i),epilogueLabel, env);
			}
		} else if (ndx instanceof ASTAssignStmtNode) {
			ASTAssignStmtNode nd = (ASTAssignStmtNode) ndx;
			Variable local = env.lookup(nd.var);
			Variable global = globalEnv.lookup(nd.var);
			if (local != null) {
				LocalVariable localVar = (LocalVariable) local;
				compileExpr(nd.expr, env);
				emitSTR(REG_DST, REG_FP, localVar.offset);
			} else if (global != null) {
				compileExpr(nd.expr, env);
				GlobalVariable globalVar = (GlobalVariable) global;
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
			compileStmt(nd.thenClause, epilogueLabel, env);
			emitJMP("b", endLabel);
			emitLabel(elseLabel);
			compileStmt(nd.elseClause, epilogueLabel, env);
			emitLabel(endLabel);
		} else if (ndx instanceof ASTWhileStmtNode) {
			ASTWhileStmtNode nd = (ASTWhileStmtNode) ndx;
			String loop = freshLabel();
			String endloop = freshLabel();
			emitLabel(loop);
			compileExpr(nd.cond, env);
			emitRI("cmp", REG_DST, 0);
			emitJMP("beq", endloop);
			compileStmt(nd.stmt, epilogueLabel, env);
			emitJMP("b", loop);
			emitLabel(endloop);
		} else if (ndx instanceof ASTPrintStmtNode) {
			ASTPrintStmtNode nd = (ASTPrintStmtNode) ndx;
			compileExpr(nd.expr, env);
			emitCALL("print_func");
		} else if (ndx instanceof ASTReturnStmtNode) {
			ASTReturnStmtNode nd = (ASTReturnStmtNode) ndx;
			compileExpr(nd.expr, env);
			emitJMP("b", epilogueLabel);
			
		} else
			throw new Error("Unknown expression: "+ndx);
	}

	void compileExpr(ASTNode ndx, Environment env) {
		/* ここを完成させる */
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
				var = globalEnv.lookup(nd.varName);
			if (var == null)
				throw new Error("Undefined variable: "+nd.varName);
			if (var instanceof GlobalVariable) {
				GlobalVariable globalVar = (GlobalVariable) var;
				emitLDC(REG_DST, globalVar.getLabel());
				emitLDR(REG_DST, REG_DST, 0);
			} else {
				LocalVariable localVar = (LocalVariable) var;
				int offset = localVar.offset;
				emitLDR(REG_DST, REG_FP, offset);
			}
		} else if (ndx instanceof ASTCallNode) {
			ASTCallNode nd = (ASTCallNode) ndx;
			int i;
			for (i = 0; i < nd.args.size(); i++){
				compileExpr(nd.args.get(nd.args.size() - 1 - i), env);
				emitPUSH(REG_DST);
			}
			emitCALL(nd.name);
			emitRRI("add", REG_SP, REG_SP, 4 * i);
		} else
			throw new Error("Unknown expression: "+ndx);
	}
	
	void compile(ASTNode ast) {
		globalEnv = new Environment();
		ASTProgNode program = (ASTProgNode) ast;

		System.out.println("\t.section .data");
		System.out.println("\t@ 大域変数の定義");		
		for (String varName: program.varDecls) {
			if (globalEnv.lookup(varName) != null)
				throw new Error("Variable redefined: "+varName);
			GlobalVariable v = addGlobalVariable(globalEnv, varName);
			emitLabel(v.getLabel());
			System.out.println("\t.word 0");
		}
		System.out.println("buf:\t.space 8\n\t.byte 10");
		System.out.println("\t.section .text");
		System.out.println("\t.global _start");
		System.out.println("print_func:");
		String print_loop = freshLabel();
		emitPUSH(REG_LR);
		emitPUSH(REG_DST);
		emitPUSH(REG_R1);
		emitPUSH(REG_R2);
		emitPUSH(REG_R3);
		emitPUSH(REG_R7);
		emitRRI("sub", REG_SP, REG_SP, 4);
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
		emitRRI("add", REG_SP, REG_SP, 4);
		emitPOP(REG_R7);
		emitPOP(REG_R3);
		emitPOP(REG_R2);
		emitPOP(REG_R1);
		emitPOP(REG_DST);
		emitPOP(REG_LR);
		emitRET();
		System.out.println("_start:");
		System.out.println("\t@ main関数を呼出す．戻り値は r0 に入る");
		emitJMP("bl", "main");
		System.out.println("\t@ EXITシステムコール");
		emitRI("mov", "r7", 1);      // EXIT のシステムコール番号
		emitI("swi", 0);
		
		/* 関数定義 */
		for (ASTFunctionNode func: program.funcDecls)
			compileFunction(func);
	}
	

	public static void main(String[] args) throws IOException {
		ANTLRInputStream input = new ANTLRInputStream(System.in);
		PiLangLexer lexer = new PiLangLexer(input);
		CommonTokenStream token = new CommonTokenStream(lexer);
		PiLangParser parser = new PiLangParser(token);
		ParseTree tree = parser.prog();
		ASTGenerator astgen = new ASTGenerator();
		ASTNode ast = astgen.translate(tree);
		Compiler compiler = new Compiler();
		compiler.compile(ast);
	}
}
