/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.*;

public class ASTNode2String extends ASTVisitor {

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
	
	public boolean visit(SimpleName name) {
		fResult.append(name.getIdentifier());
		return true;
	}
	
	public boolean visit(QualifiedName name) {
		name.getQualifier().accept(this);
		fResult.append('.');
		name.getName().accept(this);
		return false;
	}
}
