/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;

public class MultiLocalDeclaration {
	private List fDeclarations;

	public MultiLocalDeclaration() {
		fDeclarations= new ArrayList(2);
	}
	
	public void add(LocalDeclaration declaration) {
		fDeclarations.add(declaration);
	}
	
	public boolean contains(LocalDeclaration declaration) {
		return fDeclarations.contains(declaration);
	}
	
	public LocalDeclaration[] getDeclarations() {
		return (LocalDeclaration[]) fDeclarations.toArray(new LocalDeclaration[fDeclarations.size()]);
	}
}
