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

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;

public abstract class TypeSet implements ITypeSet {
	
	private static class TypeUniverse extends TypeSet {
		
		public ITypeSet restrictedTo(ITypeSet restrictionSet) {
			return restrictionSet;
		}
		
		public TType chooseSingleType() {
			return null;
		}

		public String toString() {
			return "UNIVERSE"; //$NON-NLS-1$
		}

		public boolean isEmpty() {
			return false;
		}
	}
	
	private static class SingleTypeSet extends TypeSet {

		private final TType fType;

		public SingleTypeSet(TType type) {
			fType= type;
		}

		public ITypeSet restrictedTo(ITypeSet restrictionSet) {
			if (restrictionSet instanceof TypeUniverse) {
				return this;
			} else if (restrictionSet instanceof SingleTypeSet) {
				TType restrictionType= ((SingleTypeSet)restrictionSet).fType;
				if (fType == restrictionType)
					return this;
				else if (fType.canAssignTo(restrictionType))
					return restrictionSet; //e.g. fTypeBinding==Integer, restrictionTypeBinding==Number
				else if (restrictionType.canAssignTo(fType))
					return this; //e.g. fTypeBinding==Number, restrictionTypeBinding==Integer
				else
					return new MultiTypeSet(new TType[] {fType, restrictionType});
//					return commonLowerBound(restrictionSingleTypeSet);
			} else if (restrictionSet instanceof MultiTypeSet) {
				TType[] restrictionTypes= ((MultiTypeSet) restrictionSet).fTypes;
				int count= restrictionTypes.length;
				for (int i= 0; i < count; i++) {
					if (! fType.canAssignTo(restrictionTypes[i])) {
						//e.g. fTypeBinding==ArrayList, restrictionTypeBindings=={List, LinkedList}
						TType[] newTypes= new TType[count + 1];
						System.arraycopy(restrictionTypes, 0, newTypes, 0, count);
						newTypes[count]= fType;
						return new MultiTypeSet(newTypes);
					}
				}
				return restrictionSet; //e.g. fTypeBinding==ArrayList, restrictionTypeBindings=={List, RandomAccess}
			} else 	{ //TODO
				throw new IllegalStateException(fType.getPrettySignature() + " ^ " + restrictionSet); //$NON-NLS-1$
			}
		}
		
		public TType chooseSingleType() {
			return fType;
		}

		public String toString() {
			return fType.getPrettySignature();
		}

		public boolean isEmpty() {
			return false;
		}
	}
	
	private static class EmptyTypeSet extends TypeSet {

		public String toString() {
			return "EMPTY"; //$NON-NLS-1$
		}

		public TType chooseSingleType() {
			return null;
		}

		public ITypeSet restrictedTo(ITypeSet restrictionSet) {
			return this;
		}

		public boolean isEmpty() {
			return true;
		}
	}

	private static class MultiTypeSet extends TypeSet {
		private final TType[] fTypes;
		
		public MultiTypeSet(TType[] types) {
			fTypes= types;
		}
		
		public ITypeSet restrictedTo(ITypeSet restrictionSet) {
			if (restrictionSet instanceof TypeUniverse) {
				return this;
			} else if (restrictionSet instanceof SingleTypeSet) {
				TType restrictionType= ((SingleTypeSet) restrictionSet).fType;
				boolean foundTarget= false;
				int count= fTypes.length;
				for (int i= 0; i < count; i++) {
					if (restrictionType.canAssignTo(fTypes[i])) {
						//e.g. fTypeBindings=={Number, Something}, restrictionTypeBinding==Integer
						foundTarget= true;
						break;
					}
				}
				if (foundTarget)
					return this; 
				
				//e.g. fTypeBindings=={Integer, Runnable}, restrictionTypeBinding==Number
				TType[] newTypes= new TType[count + 1];
				System.arraycopy(fTypes, 0, newTypes, 0, count);
				newTypes[count]= restrictionType;
				return new MultiTypeSet(newTypes);
				
			} else if (restrictionSet instanceof MultiTypeSet) {
				if (this == restrictionSet)
					return this;
				TType[] restrictionTypes= ((MultiTypeSet) restrictionSet).fTypes;
				ArrayList toAdd= new ArrayList();
				for (int i= 0; i < restrictionTypes.length; i++) {
					TType restrictionType= restrictionTypes[i];
					boolean foundTarget= false;
					for (int j= 0; j < fTypes.length; j++) {
						TType type= fTypes[j];
						if (restrictionType.canAssignTo(type)) {
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
				
				TType[] newTypes= new TType[fTypes.length + toAdd.size()];
				System.arraycopy(fTypes, 0, newTypes, 0, fTypes.length);
				for (int i= 0; i < toAdd.size(); i++)
					newTypes[i + fTypes.length]= (TType) toAdd.get(i);
				
				return new MultiTypeSet(newTypes);
			} else 	{ //TODO
				throw new IllegalStateException(toString() + " ^ " + restrictionSet); //$NON-NLS-1$
			}
		}

		public TType chooseSingleType() {
			//TODO: Need to count possible removable casts for each choice to decide for best choice.
			return TTypes.mostSpecificCommonType(fTypes);
		}

		public String toString() {
			ArrayList names= new ArrayList(fTypes.length);
			for (int i= 0; i < fTypes.length; i++) {
				names.add(fTypes[i].getPrettySignature());
			}
			return names.toString();
		}

		public boolean isEmpty() {
			return fTypes.length == 0;
		}
	}

	private final static EmptyTypeSet fgEmpty= new EmptyTypeSet();

	private final static TypeUniverse fgUniverse= new TypeUniverse();

	public static ITypeSet getEmptySet() {
		return fgEmpty;
	}

	public static ITypeSet getTypeUniverse() {
		return fgUniverse;
	}

	public static ITypeSet create(TType type) {
		if (type == null)
			return fgUniverse;
		else
			return new SingleTypeSet(type);
	}

	public static ITypeSet create(TType[] types) {
		if (types == null)
			return fgUniverse;
		else
			return new MultiTypeSet(types);
	}
}