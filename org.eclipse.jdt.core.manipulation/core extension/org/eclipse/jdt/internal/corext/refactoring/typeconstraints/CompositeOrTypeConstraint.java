/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.Assert;


public class CompositeOrTypeConstraint implements ITypeConstraint{

	private final ITypeConstraint[] fConstraints;

	/* package */ CompositeOrTypeConstraint(ITypeConstraint[] constraints){
		Assert.isNotNull(constraints);
		fConstraints= sort(getCopy(constraints));
	}

	private static ITypeConstraint[] getCopy(ITypeConstraint[] constraints) {
		List<ITypeConstraint> l= Arrays.asList(constraints);
		return l.toArray(new ITypeConstraint[l.size()]);
	}

	private static ITypeConstraint[] sort(ITypeConstraint[] constraints) {
		//TODO bogus to sort by toString - will have to come up with something better
		Arrays.sort(constraints, Comparator.comparing(ITypeConstraint::toString).reversed());
		return constraints;
	}

	@Override
	public String toResolvedString() {
		StringBuilder buff= new StringBuilder();
		for (int i= 0; i < fConstraints.length; i++) {
			ITypeConstraint constraint= fConstraints[i];
			if (i > 0)
				buff.append(" or ");			 //$NON-NLS-1$
			buff.append(constraint.toResolvedString());
		}
		return buff.toString();
	}

	@Override
	public boolean isSimpleTypeConstraint() {
		return false;
	}

	@Override
	public String toString() {
		StringBuilder buff= new StringBuilder();
		for (int i= 0; i < fConstraints.length; i++) {
			ITypeConstraint constraint= fConstraints[i];
			if (i > 0)
				buff.append(" or ");			 //$NON-NLS-1$
			buff.append(constraint.toString());
		}
		return buff.toString();
	}

	public ITypeConstraint[] getConstraints() {
		return fConstraints;
	}
}
