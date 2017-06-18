package edu.umich.verdict.relation.expr;

import edu.umich.verdict.VerdictContext;
import edu.umich.verdict.exceptions.VerdictException;

public class StarExpr extends Expr {

	public StarExpr() {}

	@Override
	public String toString() {
		return "*";
	}

	@Override
	public <T> T accept(ExprVisitor<T> v) {
		return v.call(this);
	}

}
