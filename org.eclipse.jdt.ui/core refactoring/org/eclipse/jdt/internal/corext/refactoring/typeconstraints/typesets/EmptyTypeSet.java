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
 *   Robert M. Fuhrer (rfuhrer@watson.ibm.com), IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets;

import java.util.Iterator;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;

public class EmptyTypeSet extends TypeSet {

	EmptyTypeSet(TypeSetEnvironment typeSetEnvironment) {
		super(typeSetEnvironment);
	}

	@Override
	public boolean isUniverse() {
		return false;
	}

	@Override
	public TypeSet makeClone() {
		return this;
	}

	@Override
	protected TypeSet specialCasesIntersectedWith(TypeSet s2) {
		return this;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public TypeSet upperBound() {
		return this;
	}

	@Override
	public TypeSet lowerBound() {
		return this;
	}

	@Override
	public boolean hasUniqueLowerBound() {
		return false;
	}

	@Override
	public boolean hasUniqueUpperBound() {
		return false;
	}

	@Override
	public TType uniqueLowerBound() {
		return null;
	}

	@Override
	public TType uniqueUpperBound() {
		return null;
	}

	@Override
	public boolean contains(TType t) {
		return false;
	}

	@Override
	public boolean containsAll(TypeSet s) {
		return false;
	}

	@Override
	public Iterator<TType> iterator() {
		return new Iterator<TType>() {
			@Override
			public void remove() {
				//do nothing
			}
			@Override
			public boolean hasNext() {
				return false;
			}
			@Override
			public TType next() {
				return null;
			}
		};
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	public TType anyMember() {
		return null;
	}

	@Override
	public String toString() {
		return "{ }"; //$NON-NLS-1$
	}

	@Override
	public EnumeratedTypeSet enumerate() {
		return new EnumeratedTypeSet(getTypeSetEnvironment());
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof EmptyTypeSet;
	}

	@Override
	public int hashCode() {
		return 42;
	}
}
