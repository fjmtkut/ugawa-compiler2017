import org.antlr.v4.runtime.tree.ParseTree;

import parser.TinyPiEParser.AddExprContext;
import parser.TinyPiEParser.ExprContext;
import parser.TinyPiEParser.LiteralExprContext;
import parser.TinyPiEParser.MulExprContext;
import parser.TinyPiEParser.ParenExprContext;
import parser.TinyPiEParser.VarExprContext;
// ここから
import parser.TinyPiEParser.AndExprContext;
import parser.TinyPiEParser.OrExprContext;
import parser.TinyPiEParser.NotExprContext;
// ここまで

public class ASTGenerator {	
	ASTNode translateExpr(ParseTree ctxx) {
		if (ctxx instanceof ExprContext) {
			ExprContext ctx = (ExprContext) ctxx;
			return translateExpr(ctx.orExpr());
// ここから演習２
		} else if (ctxx instanceof OrExprContext) {
			OrExprContext ctx = (OrExprContext) ctxx;
			if (ctx.orExpr() == null)
				return translateExpr(ctx.andExpr());
			ASTNode lhs = translateExpr(ctx.orExpr());
			ASTNode rhs = translateExpr(ctx.andExpr());
			return new ASTBinaryExprNode(ctx.OROP().getText(), lhs, rhs);
		} else if (ctxx instanceof AndExprContext) {
			AndExprContext ctx = (AndExprContext) ctxx;
			if (ctx.andExpr() == null)
				return translateExpr(ctx.addExpr());
			ASTNode lhs = translateExpr(ctx.andExpr());
			ASTNode rhs = translateExpr(ctx.addExpr());
			return new ASTBinaryExprNode(ctx.ANDOP().getText(), lhs, rhs);
// ここまで
		} else if (ctxx instanceof AddExprContext) {
			AddExprContext ctx = (AddExprContext) ctxx;
			if (ctx.addExpr() == null)
				return translateExpr(ctx.mulExpr());
			ASTNode lhs = translateExpr(ctx.addExpr());
			ASTNode rhs = translateExpr(ctx.mulExpr());
//ここから変更点
			if (ctx.ADDOP() != null)
				return new ASTBinaryExprNode(ctx.ADDOP().getText(), lhs, rhs);
			else if (ctx.SUBOP() != null)
				return new ASTBinaryExprNode(ctx.SUBOP().getText(), lhs, rhs);
//ここまで
		} else if (ctxx instanceof MulExprContext) {
			MulExprContext ctx = (MulExprContext) ctxx;
			if (ctx.mulExpr() == null)
				return translateExpr(ctx.unaryExpr());
			ASTNode lhs = translateExpr(ctx.mulExpr());
			ASTNode rhs = translateExpr(ctx.unaryExpr());
			return new ASTBinaryExprNode(ctx.MULOP().getText(), lhs, rhs);
// ここから演習３
		} else if (ctxx instanceof NotExprContext) {
			NotExprContext ctx = (NotExprContext) ctxx;
			ASTNode operand = translateExpr(ctx.unaryExpr());;
			if (ctx.SUBOP() != null) {
				return new ASTUnaryExprNode(ctx.SUBOP().getText(), operand);
			} else if (ctx.NOTOP() != null){
				return new ASTUnaryExprNode(ctx.NOTOP().getText(), operand);
			}
// ここまで
		} else if (ctxx instanceof LiteralExprContext) {
			LiteralExprContext ctx = (LiteralExprContext) ctxx;
			int value = Integer.parseInt(ctx.VALUE().getText());
			return new ASTNumberNode(value);
		} else if (ctxx instanceof VarExprContext) {
			VarExprContext ctx = (VarExprContext) ctxx;
			String varName = ctx.IDENTIFIER().getText();
			return new ASTVarRefNode(varName);
		} else if (ctxx instanceof ParenExprContext) {
			ParenExprContext ctx = (ParenExprContext) ctxx;
			return translateExpr(ctx.expr());
		}
		throw new Error("Unknown parse tree node: "+ctxx.getText());		
	}
	ASTNode translate(ParseTree ctxx) {
		return translateExpr(ctxx);
	}
}
