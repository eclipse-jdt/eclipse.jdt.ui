/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.HashMap;

import org.eclipse.jdt.internal.compiler.ast.AstNode;

public class AstNodeData {

	private HashMap fData= new HashMap(10);

	public void put(AstNode node, Object value) {
		fData.put(node, value);
	}
	
	public Object get(AstNode node) {
		return fData.get(node);
	}
	
	public Object remove(AstNode node) {
		return fData.remove(node);
	}
}
