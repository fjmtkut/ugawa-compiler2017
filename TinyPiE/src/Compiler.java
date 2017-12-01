import java.io.IOException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import parser.TinyPiELexer;
import parser.TinyPiEParser;

public class Compiler extends CompilerBase {
	void compileExpr(ASTNode ndx, Environment env) {
		if (ndx instanceof ASTBinaryExprNode) {
			ASTBinaryExprNode nd = (ASTBinaryExprNode) ndx;
			compileExpr(nd.lhs, env);
			emitPUSH(REG_R1);
			emitRR("mov", REG_R1, REG_DST);
			compileExpr(nd.rhs, env);
//ここから演習５
			if (nd.op.equals("|"))
				emitRR("orr", REG_DST, REG_R1);
			else if (nd.op.equals("&"))
				emitRR("and", REG_DST, REG_R1);
//ここまで
			else if (nd.op.equals("+"))
				emitRRR("add", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("-"))
				emitRRR("sub", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("*"))
				emitRRR("mul", REG_DST, REG_R1, REG_DST);
			else if (nd.op.equals("/"))
				emitRRR("udiv", REG_DST, REG_R1, REG_DST);
			else
				throw new Error("Unknown operator: "+nd.op);
			emitPOP(REG_R1);
//ここから演習５
		} else if (ndx instanceof ASTUnaryExprNode) {
			ASTUnaryExprNode nd = (ASTUnaryExprNode) ndx;
			Variable var = env.lookup(nd.operand);
			if (nd.op.equals("~")) {
				if (var != null) {
					GlobalVariable globalVar = (GlobalVariable) var;
					emitLDC(REG_DST, globalVar.getLabel());
					emitLDR(REG_DST, REG_DST, 0);
					emitRR("mvn", REG_DST, REG_DST);
				} else {
					emitLDC(REG_DST, Integer.parseInt(nd.operand));
					emitRR("mvn", REG_DST, REG_DST);
				}
			} else if (nd.op.equals("-")) {
				if (var != null) {
					GlobalVariable globalVar = (GlobalVariable) var;
					emitLDC(REG_DST, globalVar.getLabel());
					emitLDR(REG_DST, REG_DST, 0);
					emitRR("mvn", REG_DST, REG_DST);
					emitRRI("add", REG_DST, REG_DST, 1);
				} else {
					emitLDC(REG_DST, 0 - Integer.parseInt(nd.operand));
				}
			}
//ここまで
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
				throw new Error("not a blobal variable: "+nd.varName);
		} else
			throw new Error("Unknown expression: "+ndx);
	}
	
	void compile(ASTNode ast) {
		Environment env = new Environment();
		GlobalVariable vx = addGlobalVariable(env, "x");
		GlobalVariable vy = addGlobalVariable(env, "y");
		GlobalVariable vz = addGlobalVariable(env, "z");
		
		System.out.println("\t.section .data");
		System.out.println("\t@ 大域変数の定義");
		emitLabel(vx.getLabel());
		System.out.println("\t.word 1");
		emitLabel(vy.getLabel());
		System.out.println("\t.word 10");
		emitLabel(vz.getLabel());
		System.out.println("\t.word -1");
		System.out.println("\t.section .text");
		System.out.println("\t.global _start");
		System.out.println("_start:");
		System.out.println("\t@ 式をコンパイルした命令列");
		compileExpr(ast, env);
		System.out.println("\t@ EXITシステムコール");
		emitRI("mov", "r7", 1);								// EXITのシステムコール番号
		emitI("swi", 0);
	}

	public static void main(String[] args) throws IOException {
		ANTLRInputStream input = new ANTLRInputStream(System.in);
		TinyPiELexer lexer = new TinyPiELexer(input);
		CommonTokenStream token = new CommonTokenStream(lexer);
		TinyPiEParser parser = new TinyPiEParser(token);
		ParseTree tree = parser.expr();
		ASTGenerator astgen = new ASTGenerator();
		ASTNode ast = astgen.translate(tree);
		Compiler compiler = new Compiler();
		compiler.compile(ast);
	}
}
