/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Robert M. Fuhrer (rfuhrer@watson.ibm.com), IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.ArrayType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TTypes;

public class SubTypesOfSingleton extends TypeSet {
	/**
	 * The "base type" defining the upper bound of this set.
	 */
	private final TType fUpperBound;

	private static final Map/*<IType arg>*/ sCommonExprs= new LinkedHashMap();//@perf
	public static void clear() {
		sCommonExprs.clear();
	}
	
	public static SubTypesOfSingleton create(TType superType) {
		if (superType == sJavaLangObject)
			return TypeUniverseSet.create();
		if (sCommonExprs.containsKey(superType)) {
			sCommonExprHits++;
			return (SubTypesOfSingleton) sCommonExprs.get(superType);
		} else {
			SubTypesOfSingleton s= new SubTypesOfSingleton(superType);

			sCommonExprMisses++;
			sCommonExprs.put(superType, s);
			return s;
		}
	}

	protected SubTypesOfSingleton(TType t) {
		super();
		Assert.isNotNull(t);
		fUpperBound= t;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isUniverse()
	 */
	public boolean isUniverse() {
		return fUpperBound.equals(sJavaLangObject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#makeClone()
	 */
	public TypeSet makeClone() {
		return this; // new SubTypesOfSingleton(fUpperBound);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#subTypes()
	 */
	public TypeSet subTypes() {
		return this; // makeClone();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#intersectedWith(org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	public TypeSet specialCasesIntersectedWith(TypeSet other) {
		if (other.isSingleton() && other.anyMember().equals(fUpperBound))
			return other;		// xsect(subTypes(A),A) = A

		if (other instanceof SubTypesOfSingleton) {
			SubTypesOfSingleton otherSub= (SubTypesOfSingleton) other;

			if (otherSub.fUpperBound.canAssignTo(fUpperBound))
				return otherSub; // .makeClone();
			if (fUpperBound.canAssignTo(otherSub.fUpperBound))
				return this; // makeClone();
		} else if (other.hasUniqueLowerBound()) {
			TType otherLower= other.uniqueLowerBound();

			if (otherLower.equals(fUpperBound))
				return new SingletonTypeSet(fUpperBound);
			if (otherLower != fUpperBound && fUpperBound.canAssignTo(otherLower) ||
				!otherLower.canAssignTo(fUpperBound))
				return EmptyTypeSet.create();
		}
//		else if (other instanceof SubTypesSet) {
//			SubTypesSet otherSub= (SubTypesSet) other;
//			TypeSet otherUppers= otherSub.upperBound();
//
//			for(Iterator iter= otherUppers.iterator(); iter.hasNext(); ) {
//				IType t= (IType) iter.next();
//
//				if ()
//			}
//		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isEmpty()
	 */
	public boolean isEmpty() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#upperBound()
	 */
	public TypeSet upperBound() {
		return new SingletonTypeSet(fUpperBound);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#lowerBound()
	 */
	public TypeSet lowerBound() {
		EnumeratedTypeSet e= enumerate();
		return e.lowerBound();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#hasUniqueLowerBound()
	 */
	public boolean hasUniqueLowerBound() {
//		TypeSet lowerBound= lowerBound();

//		return lowerBound.isSingleton();
		return false; // fast, though perhaps inaccurate, but that's ok
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#hasUniqueUpperBound()
	 */
	public boolean hasUniqueUpperBound() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#uniqueLowerBound()
	 */
	public TType uniqueLowerBound() {
		TypeSet lowerBound= lowerBound();

		return lowerBound.anyMember();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#uniqueUpperBound()
	 */
	public TType uniqueUpperBound() {
		return fUpperBound;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#contains(org.eclipse.jdt.core.IType)
	 */
	public boolean contains(TType t) {
		if (t.equals(fUpperBound))
			return true;
		return t.canAssignTo(fUpperBound);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#containsAll(org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet)
	 */
	public boolean containsAll(TypeSet other) {
		if (isUniverse())
			return true;

		// Optimization: if other is also a SubTypeOfSingleton, just compare bounds
		if (other instanceof SubTypesOfSingleton) {
			SubTypesOfSingleton otherSub= (SubTypesOfSingleton) other;
			return otherSub.fUpperBound.canAssignTo(fUpperBound);
		}
		// Optimization: if other is a SubTypesSet, just compare each of its bounds to mine
		if (other instanceof SubTypesSet) {
			SubTypesSet otherSub= (SubTypesSet) other;
			TypeSet otherUpperBounds= otherSub.upperBound();

			for(Iterator iter= otherUpperBounds.iterator(); iter.hasNext(); ) {
				TType t= (TType) iter.next();
				if (!t.canAssignTo(fUpperBound))
					return false;
			}
			return true;
		}
		// For now, no more tricks up my sleeve; get an iterator
		for(Iterator iter= other.iterator(); iter.hasNext(); ) {
			TType t= (TType) iter.next();

			if (!t.canAssignTo(fUpperBound))
				return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#iterator()
	 */
	public Iterator iterator() {
		return enumerate().iterator();
//		return new Iterator() {
//			// First type returned is fUpperBound, then each of the subtypes, in turn
//			//
//			// If the upper bound is an array type, return the set of array types
//			// { Array(subType(elementTypeOf(fUpperBound))) }
//			private Set/*<IType>*/ subTypes= sTypeHierarchy.getAllSubtypes(getElementTypeOf(fUpperBound));
//			private Iterator/*<IType>*/ subTypeIter= subTypes.iterator();
//			private int nDims= getDimsOf(fUpperBound);
//			private int idx=-1;
//			public void remove() { /*do nothing*/}
//			public boolean hasNext() { return idx < subTypes.size(); }
//			public Object next() {
//				int i=idx++;
//				if (i < 0) return fUpperBound;
//				return makePossiblyArrayTypeFor((IType) subTypeIter.next(), nDims);
//			}
//		};
	}

	/**
	 * Returns the element type of the given IType, if an array type, or the
	 * given IType itself, otherwise.
	 */
	private TType getElementTypeOf(TType t) {
		if (t instanceof ArrayType)
			return ((ArrayType) t).getElementType();
		return t;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#isSingleton()
	 */
	public boolean isSingleton() {
		return getElementTypeOf(fUpperBound).getSubTypes().length == 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#anyMember()
	 */
	public TType anyMember() {
		return fUpperBound;
	}

	private EnumeratedTypeSet fEnumCache= null;

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet#enumerate()
	 */
	public EnumeratedTypeSet enumerate() {
		if (fEnumCache == null) {
			if (fUpperBound instanceof ArrayType) {
				ArrayType at= (ArrayType) fUpperBound;
				fEnumCache= EnumeratedTypeSet.makeArrayTypesForElements(TTypes.getAllSubTypesIterator(at.getComponentType()));
			} else
				fEnumCache= new EnumeratedTypeSet(TTypes.getAllSubTypesIterator(fUpperBound));

			fEnumCache.add(fUpperBound);
			fEnumCache.initComplete();
		}
		return fEnumCache;
	}

	public boolean equals(Object o) {
		if (!(o instanceof SubTypesOfSingleton))
			return false;
		SubTypesOfSingleton other= (SubTypesOfSingleton) o;

		return other.fUpperBound.equals(fUpperBound);
	}

	public String toString() {
		return "<" + fID + ": subTypes(" + fUpperBound.getPrettySignature() + ")>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
