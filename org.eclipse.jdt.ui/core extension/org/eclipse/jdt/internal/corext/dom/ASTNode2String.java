/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.*;

/* package */ class ASTNode2String extends ASTVisitor {

	private StringBuffer fResult;

	private ASTNode2String() {
		// no public instance
		fResult= new StringBuffer();
	}

	public static String perform(ASTNode node) {
		ASTNode2String converter= new ASTNode2String();
		node.accept(converter);
		return converter.fResult.toString();
	}
	
	public boolean visit(SimpleName node) {
		fResult.append(node.getIdentifier());
		return true;
	}
	
	public boolean visit(QualifiedName node) {
		node.getQualifier().accept(this);
		fResult.append('.');
		node.getName().accept(this);
		return false;
	}
	
	public void endVisit(ArrayType node) {
		fResult.append("[]");
	}
	
	public boolean visit(PrimitiveType node) {
		fResult.append(node.getPrimitiveTypeCode().toString());
		return true;
	}
	
	public boolean visit(SimpleType node) {
		// no special action. SimpleType has a Name.
		return true;
	}
}
