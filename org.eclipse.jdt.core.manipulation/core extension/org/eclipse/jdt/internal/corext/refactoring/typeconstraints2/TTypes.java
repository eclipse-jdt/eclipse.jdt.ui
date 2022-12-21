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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import java.util.Iterator;
import java.util.Stack;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.AbstractTypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.HierarchyType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TypeVariable;

public class TTypes {

	private static class AllSupertypesIterator implements Iterator<TType> {
		private final Stack<TType> fWorklist;

		public AllSupertypesIterator(TType type) {
			fWorklist= new Stack<>();
			pushSupertypes(type);
		}

		@Override
		public boolean hasNext() {
			return ! fWorklist.empty();
		}

		@Override
		public TType next() {
			TType result= fWorklist.pop();
			pushSupertypes(result);
			return result;
		}

		private void pushSupertypes(TType type) {
			if (type.isJavaLangObject())
				return;

			if (type.isTypeVariable() || type.isCaptureType()) {
				for (TType bound : ((AbstractTypeVariable) type).getBounds()) {
					fWorklist.push(bound.getTypeDeclaration());
				}

			} else {
				TType superclass= type.getSuperclass();
				if (superclass == null) {
					if (type.isInterface())
						fWorklist.push(type.getEnvironment().getJavaLangObject());
				} else {
					fWorklist.push(superclass.getTypeDeclaration());
				}
				for (TType intf : type.getInterfaces()) {
					fWorklist.push(intf.getTypeDeclaration());
				}
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static class AllSubtypesIterator implements Iterator<TType> {
		private final Stack<TType> fWorklist;

		public AllSubtypesIterator(TType type) {
			fWorklist= new Stack<>();
			fWorklist.push(type.getTypeDeclaration());
		}

		@Override
		public boolean hasNext() {
			return ! fWorklist.empty();
		}

		@Override
		public TType next() {
			TType result= fWorklist.pop();
			for (TType subType : result.getSubTypes()) {
				fWorklist.push(subType.getTypeDeclaration());
			}

			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private TTypes() {
		// no instances
	}

	public static TType createArrayType(TType elementType, int dimensions) {
		return elementType.getEnvironment().createArrayType(elementType, dimensions);
	}

	/**
	 * @return all subtypes of this type (including this type)
	 */
	public static Iterator<TType> getAllSubTypesIterator(TType type) {
		return new AllSubtypesIterator(type);
	}

	/**
	 * @return all proper supertypes of this type
	 */
	public static Iterator<TType> getAllSuperTypesIterator(TType type) {
		return new AllSupertypesIterator(type);
	}

	/**
	 * @param rhs
	 * @param lhs
	 * @return <code>true</code> iff an expression of type 'rhs' can be assigned to a variable of type 'lhs'.
	 * Type arguments of generic / raw / parameterized types are <b>not</b> considered.
	 */
	public static boolean canAssignTo(TType rhs, TType lhs) {
		if (rhs.isHierarchyType() && lhs.isHierarchyType()) {
			HierarchyType rhsGeneric= (HierarchyType) rhs.getTypeDeclaration();
			HierarchyType lhsGeneric= (HierarchyType) lhs.getTypeDeclaration();
			return lhs.isJavaLangObject() || rhsGeneric.equals(lhsGeneric) || rhsGeneric.isSubType(lhsGeneric);

		} else if (rhs.isTypeVariable()) {
			if (rhs.canAssignTo(lhs))
				return true;
			for (TType bound : ((TypeVariable) rhs).getBounds()) {
				if (canAssignTo(bound, lhs)) {
					return true;
				}
			}
			return lhs.isJavaLangObject();

		} else {
			return rhs.canAssignTo(lhs);
		}
	}

}
