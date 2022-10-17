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

public class SubTypesSet extends TypeSet {
	/**
	 * The set of "base types" defining the upper bounds of this set.
	 */
	private TypeSet fUpperBounds;

	SubTypesSet(TypeSet superTypes) {
		super(superTypes.getTypeSetEnvironment());
		fUpperBounds= superTypes;
	}

	@Override
	public boolean isUniverse() {
		return fUpperBounds.isUniverse() || fUpperBounds.contains(getJavaLangObject());
	}

	@Override
	public TypeSet makeClone() {
		return this; //new SubTypesSet(fUpperBounds.makeClone());
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof SubTypesSet) {
			SubTypesSet other= (SubTypesSet) o;
			return other.fUpperBounds.equals(fUpperBounds);
//		} else if (o instanceof TypeSet) {
//			TypeSet other= (TypeSet) o;
//			if (other.isUniverse() && isUniverse())
//				return true;
//			return enumerate().equals(other.enumerate());
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return fUpperBounds.hashCode();
	}

	@Override
	protected TypeSet specialCasesIntersectedWith(TypeSet s2) {
		if (fUpperBounds.equals(s2))
			return s2; // xsect(subTypes(A),A) = A
		if (s2 instanceof SubTypesSet) {
			SubTypesSet st2= (SubTypesSet) s2;

			if (fUpperBounds.isSingleton() && st2.fUpperBounds.isSingleton()) {
				TType t1= this.fUpperBounds.anyMember();
				TType t2= st2.fUpperBounds.anyMember();

				if (TTypes.canAssignTo(t2, t1))
					return new SubTypesSet(st2.fUpperBounds);
			} else if (fUpperBounds instanceof SubTypesSet) {
				// xsect(subTypes(superTypes(A)), subTypes(A)) = subTypes(A)
				SubTypesSet myUpperSubTypes= (SubTypesSet) fUpperBounds;

				if (myUpperSubTypes.lowerBound().equals(st2.lowerBound()))
					return st2;
			}
		}
		if (s2 instanceof SubTypesOfSingleton) {
			SubTypesOfSingleton st2= (SubTypesOfSingleton) s2;

			if (fUpperBounds.isSingleton()) {
				TType t1= this.fUpperBounds.anyMember();
				TType t2= st2.uniqueUpperBound();

				if (TTypes.canAssignTo(t2, t1))
					return getTypeSetEnvironment().createSubTypesOfSingleton(t2);
			} else if (fUpperBounds instanceof SubTypesOfSingleton) {
				// xsect(subTypes(superTypes(A)), subTypes(A)) = subTypes(A)
				SubTypesOfSingleton myUpperSubTypes= (SubTypesOfSingleton) fUpperBounds;

				if (myUpperSubTypes.uniqueLowerBound().equals(st2.uniqueLowerBound()))
					return st2;
			}
		}

		if (s2 instanceof SuperTypesSet) {
			SuperTypesSet st2= (SuperTypesSet) s2;

			if (fUpperBounds.equals(st2.lowerBound()))
				return fUpperBounds;

			if (fUpperBounds instanceof TypeSetIntersection) {
				// (intersect (subTypes (intersect (superTypes A) B))
				//            (superTypes A)) =>
				// (intersect (superTypes A) (subTypes B))
				TypeSetIntersection lbXSect= (TypeSetIntersection) fUpperBounds;
				TypeSet xsectLeft= lbXSect.getLHS();
				TypeSet xsectRight= lbXSect.getRHS();

				if (xsectLeft.equals(st2.lowerBound()))
					return new TypeSetIntersection(s2, new SubTypesSet(xsectRight));
			}
		}
		return null;
	}

	@Override
	public TypeSet subTypes() {
		return this; // makeClone();
	}

	@Override
	public boolean isEmpty() {
		return fUpperBounds.isEmpty();
	}

	@Override
	public boolean contains(TType t) {
		if (fEnumCache != null) return fEnumCache.contains(t);

		if (fUpperBounds.contains(t))
			return true;

		// Find the "upper frontier", i.e. the upper bound, and see whether
		// the given type is a subtype of any of those.
		Iterator<TType> ubIter= fUpperBounds.upperBound().iterator();

		for(; ubIter.hasNext(); ) {
			TType ub= ubIter.next();

			if (TTypes.canAssignTo(t, ub))
				return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(TypeSet s) {
		if (fEnumCache != null) return fEnumCache.containsAll(s);

		if (fUpperBounds.containsAll(s))
			return true;

		// Make sure all elements of s are contained in this set
		for(Iterator<TType> sIter= s.iterator(); sIter.hasNext(); ) {
			TType t= sIter.next();
			boolean found= false;

			// Scan the "upper frontier", i.e. the upper bound set, and see whether
			// 't' is a subtype of any of those.
			for(Iterator<TType> ubIter= fUpperBounds /*.upperBound() */.iterator(); ubIter.hasNext(); ) {
				TType ub= ubIter.next();

				if (TTypes.canAssignTo(t, ub)) {
					found= true;
					break;
				}
			}
			if (!found) return false;
		}
		return true;
	}

	/**
	 * Returns the element type of the given TType, if an array type, or the
	 * given TType itself, otherwise.
	 *
	 * @param t a type
	 * @return the element type
	 */
	private TType getElementTypeOf(TType t) {
		if (t instanceof ArrayType)
			return ((ArrayType) t).getElementType();
		return t;
	}

	@Override
	public boolean isSingleton() {
		if (!fUpperBounds.isSingleton())
			return false;

		TType t= fUpperBounds.anyMember();

		return getElementTypeOf(t).getSubTypes().length == 0;
	}

	@Override
	public TType anyMember() {
		return fUpperBounds.anyMember();
	}

	@Override
	public TypeSet upperBound() {
		return fUpperBounds; // perhaps should be unmodifiable?
	}

	@Override
	public TypeSet lowerBound() {
		return enumerate().lowerBound();
	}

	@Override
	public Iterator<TType> iterator() {
		return enumerate().iterator();
	}

	@Override
	public String toString() {
		return "<" + fID + ": subTypes(" + fUpperBounds + ")>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public boolean hasUniqueLowerBound() {
		return false;
	}

	@Override
	public boolean hasUniqueUpperBound() {
		return fUpperBounds.isSingleton();
	}

	@Override
	public TType uniqueLowerBound() {
		return null;
	}

	@Override
	public TType uniqueUpperBound() {
		return fUpperBounds.isSingleton() ? fUpperBounds.anyMember() : null;
	}

	private EnumeratedTypeSet fEnumCache= null;

	@Override
	public EnumeratedTypeSet enumerate() {
		if (fEnumCache == null) {
			fEnumCache= new EnumeratedTypeSet(getTypeSetEnvironment());

			for(Iterator<TType> iter= fUpperBounds.iterator(); iter.hasNext(); ) {
				TType ub= iter.next();

				if (ub instanceof ArrayType) {
					ArrayType at= (ArrayType) ub;
					int numDims= at.getDimensions();
					for(Iterator<TType> elemSubIter=TTypes.getAllSubTypesIterator(at.getElementType()); elemSubIter.hasNext(); )
						fEnumCache.add(TTypes.createArrayType(elemSubIter.next(), numDims));
				} else {
					for (Iterator<TType> iterator= TTypes.getAllSubTypesIterator(ub); iterator.hasNext();) {
						fEnumCache.fMembers.add(iterator.next());
					}
				}
				fEnumCache.add(ub);
			}
//			fEnumCache.initComplete();
		}
		return fEnumCache;
	}
}
