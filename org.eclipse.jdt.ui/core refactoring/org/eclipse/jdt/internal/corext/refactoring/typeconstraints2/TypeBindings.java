/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.internal.corext.dom.Bindings;

/**
 * Note: This class contains static helper methods to deal with
 * bindings from different clusters. They will be inlined as soon
 * as the compiler loop and subtyping-related queries on ITypeBindings
 * are implemented.  
 */
public class TypeBindings {
	
	private TypeBindings() {
		// no instances
	}
	
	//TODO: inline with b1 == b2 when compiler loop is used
	public static boolean equals(IBinding b1, IBinding b2) {
		return Bindings.equals(b1, b2);
	}
	
	public static IBinding getStringBinding(ASTNode node) {
		return node.getAST().resolveWellKnownType("java.lang.String"); //$NON-NLS-1$
	}
}
