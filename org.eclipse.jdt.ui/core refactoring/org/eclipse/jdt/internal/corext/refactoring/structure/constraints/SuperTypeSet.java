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
package org.eclipse.jdt.internal.corext.refactoring.structure.constraints;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet;

/**
 * Optimized type sets for supertype constraint problems.
 */
public abstract class SuperTypeSet implements ITypeSet {

	/** Implementation of an empty set */
	private static class SuperTypeEmptySet extends SuperTypeSet {

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#chooseSingleType()
		 */
		public final TType chooseSingleType() {
			return null;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#isEmpty()
		 */
		public final boolean isEmpty() {
			return true;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#restrictedTo(org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet)
		 */
		public final ITypeSet restrictedTo(final ITypeSet set) {
			return this;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		public final String toString() {
			return "EMPTY"; //$NON-NLS-1$
		}
	}

	/** Implementation of a singleton */
	private static class SuperTypeSingletonSet extends SuperTypeSet {

		/** The type */
		private final TType fType;

		/**
		 * Creates a new super type singleton set.
		 * 
		 * @param type the type
		 */
		private SuperTypeSingletonSet(final TType type) {
			fType= type;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#chooseSingleType()
		 */
		public final TType chooseSingleType() {
			return fType;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#isEmpty()
		 */
		public final boolean isEmpty() {
			return false;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#restrictedTo(org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet)
		 */
		public final ITypeSet restrictedTo(final ITypeSet set) {
			if (set instanceof SuperTypeUniverse) {
				return this;
			} else if (set instanceof SuperTypeSingletonSet) {
				if (this == set)
					return this;
				final SuperTypeSingletonSet singleton= (SuperTypeSingletonSet) set;
				if (fType.canAssignTo(singleton.fType))
					return this;
				return SuperTypeSet.getEmpty();
			} else if (set instanceof SuperTypeTuple) {
				final SuperTypeTuple tuple= (SuperTypeTuple) set;
				if (fType.canAssignTo(tuple.fSuperType))
					return this;
				return SuperTypeSet.createTypeSet(tuple.fSubType);
			} else if (set instanceof SuperTypeEmptySet) {
				return set;
			} else
				Assert.isTrue(false);
			return null;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		public final String toString() {
			return fType.getPrettySignature();
		}
	}

	/** Implementation of a tuple */
	private static class SuperTypeTuple extends SuperTypeSet {

		/** The other type */
		private final TType fSubType;

		/** The super type */
		private final TType fSuperType;

		/**
		 * Creates a new super type tuple.
		 * 
		 * @param subType the sub type
		 * @param superType the super type
		 */
		private SuperTypeTuple(final TType subType, final TType superType) {
			fSubType= subType;
			fSuperType= superType;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#chooseSingleType()
		 */
		public final TType chooseSingleType() {
			return fSuperType;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#isEmpty()
		 */
		public final boolean isEmpty() {
			return false;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#restrictedTo(org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet)
		 */
		public final ITypeSet restrictedTo(final ITypeSet set) {
			if (set instanceof SuperTypeUniverse) {
				return this;
			} else if (set instanceof SuperTypeSingletonSet) {
				final SuperTypeSingletonSet singleton= (SuperTypeSingletonSet) set;
				if (fSubType.canAssignTo(singleton.fType) && fSuperType.canAssignTo(singleton.fType))
					return this;
				return SuperTypeSet.createTypeSet(fSubType);
			} else if (set instanceof SuperTypeTuple) {
				return this;
			} else if (set instanceof SuperTypeEmptySet) {
				return set;
			} else
				Assert.isTrue(false);
			return null;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		public final String toString() {
			return "[" + fSubType.getPrettySignature() + ", " + fSuperType.getPrettySignature() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/** Implementation of the type universe */
	private static class SuperTypeUniverse extends SuperTypeSet {

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#chooseSingleType()
		 */
		public final TType chooseSingleType() {
			return null;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#isEmpty()
		 */
		public final boolean isEmpty() {
			return false;
		}

		/*
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet#restrictedTo(org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeSet)
		 */
		public final ITypeSet restrictedTo(final ITypeSet set) {
			return set;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		public final String toString() {
			return "UNIVERSE"; //$NON-NLS-1$
		}
	}

	/** The empty set */
	private static final ITypeSet fgEmpty= new SuperTypeEmptySet();

	/** The universe */
	private static final ITypeSet fgUniverse= new SuperTypeUniverse();

	/**
	 * Creates a new type set.
	 * 
	 * @param type the type to contain, or <code>null</code>
	 * @return the type set, or the universe if <code>type</code> is <code>null</code>
	 */
	public static ITypeSet createTypeSet(final TType type) {
		if (type == null)
			return fgUniverse;
		return new SuperTypeSingletonSet(type);
	}

	/**
	 * Creates a new type set.
	 * 
	 * @param subType the sub type
	 * @param superType the super type
	 * @return the type set, or the universe if <code>type</code> is <code>null</code>
	 */
	public static ITypeSet createTypeSet(final TType subType, final TType superType) {
		if (subType == null || superType == null)
			return fgUniverse;
		return new SuperTypeTuple(subType, superType);
	}

	/**
	 * Returns the empty set.
	 * 
	 * @return the empty set
	 */
	public static ITypeSet getEmpty() {
		return fgEmpty;
	}

	/**
	 * Returns the universe set.
	 * 
	 * @return the universe set
	 */
	public static ITypeSet getUniverse() {
		return fgUniverse;
	}
}