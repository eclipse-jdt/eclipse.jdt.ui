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

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;

/**
 * A ParameterizedTypeVariable2 is a ConstraintVariable which stands for
 * a unique parameterization of a generic type (without an updatable source location)
 */

public class ParameterizedTypeVariable2 extends TypeConstraintVariable2 {

	protected ParameterizedTypeVariable2(TType type) {
		super(type);
		Assert.isTrue(! type.isWildcardType());
		Assert.isTrue(! type.isTypeVariable());
	}

	// hashCode() and equals(..) not necessary (unique per construction)
	
	public String toString() {
		return getType().getName();
	}
}
