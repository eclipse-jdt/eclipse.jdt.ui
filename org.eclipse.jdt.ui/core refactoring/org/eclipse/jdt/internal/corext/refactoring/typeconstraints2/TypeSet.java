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

public abstract class TypeSet {
	
	protected static class Universe extends TypeSet {
		
		public TypeSet restrictedTo(TypeSet restrictionSet) {
			return restrictionSet;
		}
		
		public TypeHandle chooseSingleType() {
			return null;
		}
	}
	
	private static class SingleTypeSet extends TypeSet {

		private final TypeHandle fTypeHandle;

		public SingleTypeSet(TypeHandle typeHandle) {
			fTypeHandle= typeHandle;
		}

		public TypeSet restrictedTo(TypeSet restrictionSet) {
			if (restrictionSet instanceof Universe) {
				return this;
			} else if (restrictionSet instanceof SingleTypeSet) {
				SingleTypeSet singleTypeSet= (SingleTypeSet) restrictionSet;
				if (fTypeHandle == singleTypeSet.fTypeHandle)
					return this;
				else //TODO
					throw new IllegalStateException(fTypeHandle + " != " + singleTypeSet.fTypeHandle); //$NON-NLS-1$
			} else { //TODO
				throw new IllegalStateException(fTypeHandle + " ^ " + restrictionSet); //$NON-NLS-1$
			}
		}
		
		public TypeHandle chooseSingleType() {
			return fTypeHandle;
		}
	}
	
	private final static Universe fgUniverse= new Universe();
	
	public static TypeSet getUniverse() {
		return fgUniverse;
	}

	public static TypeSet create(TypeHandle typeHandle) {
		return new SingleTypeSet(typeHandle);
	}

	public abstract TypeSet restrictedTo(TypeSet restrictionSet);
	
	public abstract TypeHandle chooseSingleType();
}
