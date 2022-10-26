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
 * Represents the super-types of a set of array types. Special because this set
 * always includes Object.
 */
public class ArraySuperTypeSet extends ArrayTypeSet {
	public ArraySuperTypeSet(TypeSet s) {
		super(s.getTypeSetEnvironment());
		if (s instanceof SuperTypesOfSingleton || s instanceof SuperTypesSet)
			fElemTypeSet= s.lowerBound(); // optimization: array-super(super(s)) == array-super(s)
		else
			fElemTypeSet= s;
	}

	@Override
	public TType anyMember() {
		return getJavaLangObject();
	}

	@Override
	public boolean contains(TType t) {
		if (t.equals(getJavaLangObject())) return true;
		if (!(t instanceof ArrayType))
			return false;

		ArrayType at= (ArrayType) t;
		TType atElemType= at.getComponentType();

		if (fElemTypeSet.contains(atElemType)) // try to avoid enumeration
			return true;

		for(Iterator<TType> iter= fElemTypeSet.iterator(); iter.hasNext(); ) {
			TType elemType= iter.next();

			if (TTypes.canAssignTo(elemType, atElemType))
				return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(TypeSet s) {
		if (s instanceof ArraySuperTypeSet) {
			ArraySuperTypeSet ats= (ArraySuperTypeSet) s;

			return fElemTypeSet.containsAll(ats.fElemTypeSet);
		} else if (s instanceof ArrayTypeSet) {
			ArrayTypeSet ats= (ArrayTypeSet) s;

			return fElemTypeSet.containsAll(ats.fElemTypeSet);
		} else
			return enumerate().containsAll(s);
	}

	@Override
	protected TypeSet specialCasesIntersectedWith(TypeSet s2) {
		if (s2 instanceof ArraySuperTypeSet) {
			ArraySuperTypeSet ats2= (ArraySuperTypeSet) s2;

			if (ats2.fElemTypeSet.isUniverse())
				return new ArraySuperTypeSet(fElemTypeSet);
		} else if (s2 instanceof ArrayTypeSet) {
			ArrayTypeSet ats2= (ArrayTypeSet) s2;

			if (ats2.fElemTypeSet.isUniverse())
				return new ArrayTypeSet(fElemTypeSet); // intersection doesn't include Object, which is in 'this'
		}
		return super.specialCasesIntersectedWith(s2);
	}

	private EnumeratedTypeSet fEnumCache= null;

	@Override
	public EnumeratedTypeSet enumerate() {
		if (fEnumCache == null) {
			fEnumCache= new EnumeratedTypeSet(getTypeSetEnvironment());
			TypeSet elemSupers= fElemTypeSet.superTypes();

			for(Iterator<TType> iter= elemSupers.iterator(); iter.hasNext(); ) {
				TType elemSuper= iter.next();

				fEnumCache.add(TTypes.createArrayType(elemSuper, 1));
			}

			fEnumCache.add(getJavaLangObject());
			fEnumCache.initComplete();
		}
		return fEnumCache;
	}

	@Override
	public boolean hasUniqueUpperBound() {
		return true; // Object is the unique upper bound of any set of array types
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	public boolean isUniverse() {
		return false;
	}

	@Override
	public Iterator<TType> iterator() {
		return enumerate().iterator();
	}

	@Override
	public TypeSet makeClone() {
		return new ArraySuperTypeSet(fElemTypeSet);
	}

	@Override
	public TypeSet superTypes() {
		return makeClone();
	}

	@Override
	public TType uniqueUpperBound() {
		return getJavaLangObject();
	}

	@Override
	public TypeSet upperBound() {
		return new SingletonTypeSet(getJavaLangObject(), getTypeSetEnvironment());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj instanceof ArraySuperTypeSet) {
			ArraySuperTypeSet other= (ArraySuperTypeSet) obj;

			return fElemTypeSet.equals(other.fElemTypeSet);
		}
		return false;
	}

	@Override
	public TypeSet subTypes() {
		return getTypeSetEnvironment().getUniverseTypeSet();
	}

	@Override
	public String toString() {
		return "{" + fID + ": array-super(" + fElemTypeSet + ")}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
