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

public class SuperTypesSet extends TypeSet {
	private TypeSet fLowerBounds;

	SuperTypesSet(TType subType, TypeSetEnvironment typeSetEnvironment) {
		super(typeSetEnvironment);
		fLowerBounds= new SingletonTypeSet(subType, typeSetEnvironment);
	}

	SuperTypesSet(TypeSet subTypes, TypeSetEnvironment typeSetEnvironment) {
		super(typeSetEnvironment);
		fLowerBounds= subTypes;
	}

	@Override
	public boolean isUniverse() {
		return fLowerBounds.isUniverse();
	}

	@Override
	public TypeSet makeClone() {
		return this; //new SuperTypesSet(fLowerBounds.makeClone(), getTypeSetEnvironment());
	}

	@Override
	public TypeSet upperBound() {
		return new SingletonTypeSet(getTypeSetEnvironment().getJavaLangObject(), getTypeSetEnvironment());
	}

	@Override
	public TypeSet lowerBound() {
		// Ask the operand for its lower-bound, in case it's something like an
		// EnumeratedTypeSet, which may have things in it other than the lower
		// bound...
		return fLowerBounds.lowerBound();
	}

	@Override
	protected TypeSet specialCasesIntersectedWith(TypeSet s2) {
		if (fLowerBounds.equals(s2))
			return s2; // xsect(superTypes(A),A) = A
		if (s2 instanceof SuperTypesSet) {
			SuperTypesSet st2= (SuperTypesSet) s2;

			if (fLowerBounds.isSingleton() && st2.fLowerBounds.isSingleton()) {
				TType t1= this.fLowerBounds.anyMember();
				TType t2= st2.fLowerBounds.anyMember();

				if (TTypes.canAssignTo(t1, t2))
					return new SuperTypesSet(st2.fLowerBounds, getTypeSetEnvironment());
			} else if (fLowerBounds instanceof SubTypesSet) {
				// xsect(superTypes(subTypes(A)), superTypes(A)) = superTypes(A)
				SubTypesSet myLowerSubTypes= (SubTypesSet) fLowerBounds;

				if (myLowerSubTypes.upperBound().equals(st2.upperBound()))
					return st2;
			}
		}
		if (s2 instanceof SuperTypesOfSingleton) {
			SuperTypesOfSingleton st2= (SuperTypesOfSingleton) s2;

			if (fLowerBounds.isSingleton()) {
				TType t1= this.fLowerBounds.anyMember();
				TType t2= st2.uniqueLowerBound();

				if (TTypes.canAssignTo(t1, t2))
					return getTypeSetEnvironment().createSuperTypesOfSingleton(t2);
			} else if (fLowerBounds instanceof SubTypesOfSingleton) {
				// xsect(superTypes(subTypes(A)), superTypes(A)) = superTypes(A)
				SubTypesOfSingleton myLowerSubTypes= (SubTypesOfSingleton) fLowerBounds;

				if (myLowerSubTypes.uniqueUpperBound().equals(st2.uniqueUpperBound()))
					return st2;
			}
		}
		if (s2 instanceof SubTypesSet) {
			SubTypesSet st2= (SubTypesSet) s2;
			if (fLowerBounds.equals(st2.upperBound()))
				return fLowerBounds;

			if (fLowerBounds instanceof TypeSetIntersection) {
				// (intersect (superTypes (intersect (subTypes A) B))
				//            (subTypes A)) =>
				// (intersect (subTypes A) (superTypes B))
				TypeSetIntersection lbXSect= (TypeSetIntersection) fLowerBounds;
				TypeSet xsectLeft= lbXSect.getLHS();
				TypeSet xsectRight= lbXSect.getRHS();

				if (xsectLeft.equals(st2.upperBound()))
					return new TypeSetIntersection(s2, new SuperTypesSet(xsectRight, getTypeSetEnvironment()));
			}
		}
		return null;
	}

	@Override
	public TypeSet superTypes() {
		return this; // makeClone();
	}

	@Override
	public boolean isEmpty() {
		return fLowerBounds.isEmpty();
	}

	@Override
	public boolean contains(TType t) {
		if (fEnumCache != null) return fEnumCache.contains(t);

		if (t.equals(getJavaLangObject()))
			return true;
		if (fLowerBounds.contains(t))
			return true;

		// Find the "lower frontier", i.e. the lower bound, and see whether
		// the given type is a supertype of any of those.
		for(Iterator<TType> lbIter= fLowerBounds /*.lowerBound() */.iterator(); lbIter.hasNext(); ) {
			TType lb= lbIter.next();

			if (TTypes.canAssignTo(lb, t))
				return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(TypeSet s) {
		if (fEnumCache != null) return fEnumCache.containsAll(s);

		if (!isUniverse() && s.isUniverse()) // this is more general than just SuperTypesSet; probably belongs in TypeSet
			return false;
		if (equals(s))
			return true;
		if (fLowerBounds.containsAll(s))
			return true;

		// Make sure all elements of s are contained in this set
		for(Iterator<TType> sIter= s.iterator(); sIter.hasNext(); ) {
			TType t= sIter.next();
			boolean found= false;

			// Scan the "lower frontier", i.e. the lower bound set, and see whether
			// 't' is a supertype of any of those.
			for(Iterator<TType> lbIter= fLowerBounds /*.lowerBound()*/.iterator(); lbIter.hasNext(); ) {
				TType lb= lbIter.next();

				if (TTypes.canAssignTo(lb, t)) {
					found= true;
					break;
				}
			}
			if (!found) return false;
		}
		return true;
	}

	@Override
	public boolean isSingleton() {
		if (fEnumCache != null) return fEnumCache.isSingleton();

		return fLowerBounds.isSingleton() && (fLowerBounds.anyMember() == getJavaLangObject());
	}

	@Override
	public TType anyMember() {
		return fLowerBounds.anyMember();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof SuperTypesSet) {
			SuperTypesSet other= (SuperTypesSet) o;
			return other.fLowerBounds.equals(fLowerBounds);
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
		return fLowerBounds.hashCode();
	}

	@Override
	public Iterator<TType> iterator() {
		return enumerate().iterator();
	}

	@Override
	public String toString() {
		return "<" + fID + ": superTypes(" + fLowerBounds + ")>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public boolean hasUniqueLowerBound() {
		return fLowerBounds.isSingleton();
	}

	@Override
	public boolean hasUniqueUpperBound() {
		return false;
	}

	@Override
	public TType uniqueLowerBound() {
		return fLowerBounds.isSingleton() ? fLowerBounds.anyMember() : null;
	}

	@Override
	public TType uniqueUpperBound() {
		return null;
	}

	private EnumeratedTypeSet fEnumCache= null;

	@Override
	public EnumeratedTypeSet enumerate() {
		if (fEnumCache == null) {
			fEnumCache= new EnumeratedTypeSet(getTypeSetEnvironment());
			boolean anyLBIsIntfOrArray= false;

			for(Iterator<TType> iter= fLowerBounds.iterator(); iter.hasNext(); ) {
				TType lb= iter.next();

				if (lb instanceof ArrayType) {
					ArrayType at= (ArrayType) lb;
					int numDims= at.getDimensions();
					for(Iterator<TType> elemSuperIter=TTypes.getAllSuperTypesIterator(at.getElementType()); elemSuperIter.hasNext(); )
						fEnumCache.add(TTypes.createArrayType(elemSuperIter.next(), numDims));
					anyLBIsIntfOrArray= true;
				} else {
					for (Iterator<TType> iterator= TTypes.getAllSuperTypesIterator(lb); iterator.hasNext(); )
					fEnumCache.fMembers.add(iterator.next());
				}
				fEnumCache.add(lb);
			}
			if (anyLBIsIntfOrArray) fEnumCache.add(getJavaLangObject());
			//fEnumCache.initComplete();
		}
		return fEnumCache;
	}
}
