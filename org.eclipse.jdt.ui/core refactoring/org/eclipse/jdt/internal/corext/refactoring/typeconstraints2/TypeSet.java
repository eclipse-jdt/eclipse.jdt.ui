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

import org.eclipse.jdt.core.dom.ITypeBinding;

public abstract class TypeSet {
	
	protected static class Universe extends TypeSet {
		
		public TypeSet restrictedTo(TypeSet restrictionSet) {
			return restrictionSet;
		}
		
		public ITypeBinding chooseSingleType() {
			return null;
		}

		public String toString() {
			return "UNIVERSE"; //$NON-NLS-1$
		}
	}
	
	private static class SingleTypeSet extends TypeSet {

		private final ITypeBinding fTypeBinding;

		public SingleTypeSet(ITypeBinding typeHandle) {
			fTypeBinding= typeHandle;
		}

		public TypeSet restrictedTo(TypeSet restrictionSet) {
			if (restrictionSet instanceof Universe) {
				return this;
			} else if (restrictionSet instanceof SingleTypeSet) {
				SingleTypeSet restrictionSingleTypeSet= (SingleTypeSet) restrictionSet;
				if (fTypeBinding == restrictionSingleTypeSet.fTypeBinding)
					return this;
				else if (TypeBindings.canAssign(fTypeBinding, restrictionSingleTypeSet.fTypeBinding))
					return restrictionSingleTypeSet;
				else if (TypeBindings.canAssign(restrictionSingleTypeSet.fTypeBinding, fTypeBinding))
					return this;
				else
					return commonLowerBound(restrictionSingleTypeSet);
			} else { //TODO
				throw new IllegalStateException(fTypeBinding.getQualifiedName() + " ^ " + restrictionSet); //$NON-NLS-1$
			}
		}
		
		private TypeSet commonLowerBound(SingleTypeSet other) {
			//see also org.eclipse.jdt.internal.compiler.lookup.Scope.lowerUpperBound(types);
			//and org.eclipse.jdt.internal.compiler.lookup.Scope.mostSpecificCommonType(types)
			
			throw new IllegalStateException(this + " != " + other); //$NON-NLS-1$
//			// first try superclasses:
//			ArrayList superclasses= new ArrayList();
//			ITypeBinding superclass= fTypeBinding.getSuperclass();
//			while (superclass != null) {
//				superclasses.add(superclass);
//				superclass= superclass.getSuperclass();
//			}
//			return null;
		}

		public ITypeBinding chooseSingleType() {
			return fTypeBinding;
		}

		public String toString() {
			return fTypeBinding.getQualifiedName();
		}
	}
	
	private final static Universe fgUniverse= new Universe();
	
	public static TypeSet getUniverse() {
		return fgUniverse;
	}

	public static TypeSet create(ITypeBinding typeHandle) {
		if (typeHandle == null)
			return new Universe();
		else
			return new SingleTypeSet(typeHandle);
	}

	public abstract TypeSet restrictedTo(TypeSet restrictionSet);
	
	public abstract ITypeBinding chooseSingleType();
	
	
	public abstract String toString();
}
