/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.core.refactoring.code;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.core.refactoring.Assert;

/* package */ class Selection {
	
	public int start;		// inclusive
	public int end;		// inclusive
	
	public Selection(int s, int length) {
		start= s;
		end= s + length - 1;
		Assert.isTrue(start <= end);
	}
	
	// enclosed* methods do a open interval check.
	
	public boolean enclosedBy(AstNode node) {
		return node.sourceStart < start && end < node.sourceEnd;		
	}
	
	public boolean enclosedBy(int sourceStart, int sourceEnd) {
		return sourceStart < start && end < sourceEnd;
	}
	
	// cover* methods do a closed interval check.
	
	public boolean covers(AstNode node) {
		return start <= node.sourceStart && node.sourceEnd <= end;
	}
	
	public boolean covers(int position) {
		return start <= position && position <= end;
	}
	
	public boolean covers(int sourceStart, int sourceEnd) {
		return start <= sourceStart && sourceEnd <= end;
	}
	
	public boolean coveredBy(AstNode node) {
		return coveredBy(node.sourceStart, node.sourceEnd);
	}
	
	public boolean coveredBy(int sourceStart, int sourceEnd) {
		return sourceStart <= start && end <= sourceEnd;
	}
	
	public boolean endsIn(int sourceStart, int sourceEnd) {
		return sourceStart <= end && end < sourceEnd;
	}
	
	public boolean intersects(int sourceStart, int sourceEnd) {
		return sourceStart < start && start <= sourceEnd && sourceEnd <= end ||
		       start <= sourceStart && sourceStart <= end && end < sourceEnd;
	}
	
	public boolean intersects(AstNode node) {
		return intersects(node.sourceStart, node.sourceEnd);
	}
}