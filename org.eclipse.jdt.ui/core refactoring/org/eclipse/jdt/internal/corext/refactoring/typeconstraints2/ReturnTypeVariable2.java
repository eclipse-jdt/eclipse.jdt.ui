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

/**
 * A ReturnTypeVariable is a ConstraintVariable which stands for
 * the return type of a method.
 */

public class ReturnTypeVariable2 extends ConstraintVariable2 {

	protected ReturnTypeVariable2(TypeHandle returnTypeHandle) {
		super(returnTypeHandle);
		// TODO Auto-generated constructor stub
	}

	protected int getHash() {
		// TODO Auto-generated method stub
		return 0;
	}

	protected boolean isSameAs(ConstraintVariable2 other) {
		// TODO Auto-generated method stub
		return false;
	}

}
