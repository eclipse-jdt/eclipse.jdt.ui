/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.internal.core.refactoring;import java.util.HashMap;import org.eclipse.jdt.internal.compiler.ast.AstNode;

public class AstNodeData {

	private HashMap fData= new HashMap(10);

	public void put(AstNode node, Object value) {
		fData.put(node, value);
	}
	
	public Object get(AstNode node) {
		return fData.get(node);
	}
}
