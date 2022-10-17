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

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.ArrayType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TTypes;

/**
 * Represents the set of array types whose element types are in a given TypeSet.
 * I.e., ArrayTypeSet(S) = { x[] | x \in S }
 */
public class ArrayTypeSet extends TypeSet {
	protected TypeSet fElemTypeSet;

	protected ArrayTypeSet(TypeSetEnvironment typeSetEnvironment) {
		super(typeSetEnvironment);
	}

	public ArrayTypeSet(TypeSet s) {
		super(s.getTypeSetEnvironment());
		fElemTypeSet= s;
	}

	/**
	 * @return Returns the element TypeSet.
	 */
	public TypeSet getElemTypeSet() {
		return fElemTypeSet;
	}

	@Override
	public boolean isUniverse() {
		return false;
	}

	@Override
	public TypeSet makeClone() {
		return new ArrayTypeSet(fElemTypeSet);
	}

	@Override
	public boolean isEmpty() {
		return fElemTypeSet.isEmpty();
	}

	@Override
	public TypeSet upperBound() {
		return new ArrayTypeSet(fElemTypeSet.upperBound());
	}

	@Override
	public TypeSet lowerBound() {
		return new ArrayTypeSet(fElemTypeSet.lowerBound());
	}

	@Override
	public boolean hasUniqueLowerBound() {
		return fElemTypeSet.hasUniqueLowerBound();
	}

	@Override
	public boolean hasUniqueUpperBound() {
		return fElemTypeSet.hasUniqueUpperBound();
	}

	@Override
	public TType uniqueLowerBound() {
		return TTypes.createArrayType(fElemTypeSet.uniqueLowerBound(), 1);
	}

	@Override
	public TType uniqueUpperBound() {
		return TTypes.createArrayType(fElemTypeSet.uniqueUpperBound(), 1);
	}

	@Override
	public boolean contains(TType t) {
		if (!(t instanceof ArrayType))
			return false;
		ArrayType at= (ArrayType) t;
		return fElemTypeSet.contains(at.getComponentType());
	}

	@Override
	public boolean containsAll(TypeSet s) {
		if (s instanceof ArrayTypeSet && !(s instanceof ArraySuperTypeSet)) {
			ArrayTypeSet ats= (ArrayTypeSet) s;

			return fElemTypeSet.containsAll(ats.fElemTypeSet);
		}
		for(Iterator<TType> iter= s.iterator(); iter.hasNext(); ) {
			TType t= iter.next();
			if (!contains(t))
				return false;
		}
		return true;
	}

	@Override
	public Iterator<TType> iterator() {
		if (fEnumCache != null) return fEnumCache.iterator();

		return new Iterator<TType>() {
			Iterator<TType> fElemIter= fElemTypeSet.iterator();
			@Override
			public boolean hasNext() {
				return fElemIter.hasNext();
			}
			@Override
			public TType next() {
				return TTypes.createArrayType(fElemIter.next(), 1);
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	private EnumeratedTypeSet fEnumCache= null;

	@Override
	public EnumeratedTypeSet enumerate() {
		if (fEnumCache == null) {
			fEnumCache= new EnumeratedTypeSet(getTypeSetEnvironment());

			for(Iterator<TType> iter= fElemTypeSet.iterator(); iter.hasNext(); ) {
				TType t= iter.next();
				fEnumCache.add(TTypes.createArrayType(t, 1));
			}
			fEnumCache.initComplete();
		}
		return fEnumCache;
	}

	@Override
	public boolean isSingleton() {
		return fElemTypeSet.isSingleton();
	}

	@Override
	public TType anyMember() {
		return TTypes.createArrayType(fElemTypeSet.anyMember(), 1);
	}

	@Override
	public TypeSet superTypes() {
		return new ArraySuperTypeSet(fElemTypeSet);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj instanceof ArrayTypeSet) {
			ArrayTypeSet other= (ArrayTypeSet) obj;

			return fElemTypeSet.equals(other.fElemTypeSet);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fElemTypeSet.hashCode();
	}

	@Override
	public String toString() {
		return "{" + fID + ": array(" + fElemTypeSet + ")}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
