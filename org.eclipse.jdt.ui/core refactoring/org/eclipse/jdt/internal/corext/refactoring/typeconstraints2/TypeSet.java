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

import java.util.ArrayList;

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
				ITypeBinding restrictionTypeBinding= ((SingleTypeSet) restrictionSet).fTypeBinding;
				if (fTypeBinding == restrictionTypeBinding)
					return this;
				else if (TypeBindings.canAssign(fTypeBinding, restrictionTypeBinding))
					return restrictionSet; //e.g. fTypeBinding==Integer, restrictionTypeBinding==Number
				else if (TypeBindings.canAssign(restrictionTypeBinding, fTypeBinding))
					return this; //e.g. fTypeBinding==Number, restrictionTypeBinding==Integer
				else
					return new MultiTypeSet(new ITypeBinding[] {fTypeBinding, restrictionTypeBinding});
//					return commonLowerBound(restrictionSingleTypeSet);
			} else if (restrictionSet instanceof MultiTypeSet) {
				ITypeBinding[] restrictionTypeBindings= ((MultiTypeSet) restrictionSet).fTypeBindings;
				int count= restrictionTypeBindings.length;
				for (int i= 0; i < count; i++) {
					if (! TypeBindings.canAssign(fTypeBinding, restrictionTypeBindings[i])) {
						//e.g. fTypeBinding==ArrayList, restrictionTypeBindings=={List, LinkedList}
						ITypeBinding[] newTypeBindings= new ITypeBinding[count + 1];
						System.arraycopy(restrictionTypeBindings, 0, newTypeBindings, 0, count);
						newTypeBindings[count]= fTypeBinding;
						return new MultiTypeSet(newTypeBindings);
					}
				}
				return restrictionSet; //e.g. fTypeBinding==ArrayList, restrictionTypeBindings=={List, RandomAccess}
			} else 	{ //TODO
				throw new IllegalStateException(fTypeBinding.getQualifiedName() + " ^ " + restrictionSet); //$NON-NLS-1$
			}
		}
		
		private TypeSet commonLowerBound(SingleTypeSet other) {
			//TODO: see also org.eclipse.jdt.internal.compiler.lookup.Scope.lowerUpperBound(types);
			//and org.eclipse.jdt.internal.compiler.lookup.Scope.mostSpecificCommonType(types)
			//TODO: should either not collapse sets here or support multiple lower bounds
			ITypeBinding msct= TypeBindings.mostSpecificCommonType(new ITypeBinding[] {fTypeBinding, other.fTypeBinding});
			return new SingleTypeSet(msct);
//			throw new IllegalStateException(this + " != " + other); //$NON-NLS-1$
		}

		public ITypeBinding chooseSingleType() {
			return fTypeBinding;
		}

		public String toString() {
			return fTypeBinding.getQualifiedName();
		}
	}
	
	private static class MultiTypeSet extends TypeSet {
		private final ITypeBinding[] fTypeBindings;
		
		public MultiTypeSet(ITypeBinding[] typeBindings) {
			fTypeBindings= typeBindings;
		}
		
		public TypeSet restrictedTo(TypeSet restrictionSet) {
			if (restrictionSet instanceof Universe) {
				return this;
			} else if (restrictionSet instanceof SingleTypeSet) {
				ITypeBinding restrictionTypeBinding= ((SingleTypeSet) restrictionSet).fTypeBinding;
				boolean foundTarget= false;
				int count= fTypeBindings.length;
				for (int i= 0; i < count; i++) {
					if (TypeBindings.canAssign(restrictionTypeBinding, fTypeBindings[i])) {
						//e.g. fTypeBindings=={Number, Something}, restrictionTypeBinding==Integer
						foundTarget= true;
						break;
					}
				}
				if (foundTarget)
					return this; 
				
				//e.g. fTypeBindings=={Integer, Runnable}, restrictionTypeBinding==Number
				ITypeBinding[] newTypeBindings= new ITypeBinding[count + 1];
				System.arraycopy(fTypeBindings, 0, newTypeBindings, 0, count);
				newTypeBindings[count]= restrictionTypeBinding;
				return new MultiTypeSet(newTypeBindings);
				
			} else if (restrictionSet instanceof MultiTypeSet) {
				if (this == restrictionSet)
					return this;
				ITypeBinding[] restrictionTypeBindings= ((MultiTypeSet) restrictionSet).fTypeBindings;
				ArrayList toAdd= new ArrayList();
				for (int i= 0; i < restrictionTypeBindings.length; i++) {
					ITypeBinding restrictionType= restrictionTypeBindings[i];
					boolean foundTarget= false;
					for (int j= 0; j < fTypeBindings.length; j++) {
						ITypeBinding type= fTypeBindings[j];
						if (TypeBindings.canAssign(restrictionType, type)) {
							//e.g. fTypeBindings=={Number, Something}, restrictionType==Integer
							foundTarget= true;
							break;
						}
					}
					if (! foundTarget)
						toAdd.add(restrictionType); //e.g. fTypeBindings=={Runnable, String}, restrictionType==Integer
				}
				if (toAdd.size() == 0)
					return this;
				
				ITypeBinding[] newTypeBindings= new ITypeBinding[fTypeBindings.length + toAdd.size()];
				System.arraycopy(fTypeBindings, 0, newTypeBindings, 0, fTypeBindings.length);
				for (int i= 0; i < toAdd.size(); i++)
					newTypeBindings[i + fTypeBindings.length]= (ITypeBinding) toAdd.get(i);
				
				return new MultiTypeSet(newTypeBindings);
			} else 	{ //TODO
				throw new IllegalStateException(toString() + " ^ " + restrictionSet); //$NON-NLS-1$
			}
		}

		public ITypeBinding chooseSingleType() {
			//TODO: Need to count possible removable casts for each choice to decide for best choice.
			return TypeBindings.mostSpecificCommonType(fTypeBindings);
		}

		public String toString() {
			ArrayList names= new ArrayList(fTypeBindings.length);
			for (int i= 0; i < fTypeBindings.length; i++) {
				names.add(fTypeBindings[i].getQualifiedName());
			}
			return names.toString();
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
