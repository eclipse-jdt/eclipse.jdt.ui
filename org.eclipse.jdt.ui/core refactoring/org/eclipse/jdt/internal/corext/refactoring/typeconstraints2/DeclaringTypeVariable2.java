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

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * A DeclaringTypeVariable is a ConstraintVariable which stands for
 * the declaring type of a member.
 */
public class DeclaringTypeVariable2 extends TypeConstraintVariable2 {

	private String fBindingKey;

	protected DeclaringTypeVariable2(ITypeBinding declaringTypeBinding, IBinding memberBinding) {
		super(declaringTypeBinding);
		fBindingKey= memberBinding.getKey();
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
