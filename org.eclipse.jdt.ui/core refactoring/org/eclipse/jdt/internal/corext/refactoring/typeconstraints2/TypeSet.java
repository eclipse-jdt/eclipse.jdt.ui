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

public abstract class TypeSet {
	
	protected static class Universe extends TypeSet {
		
		public TypeSet restrictedTo(TypeSet restrictionSet) {
			return restrictionSet;
		}
		
		public TType chooseSingleType() {
			return null;
		}

		public String toString() {
			return "UNIVERSE"; //$NON-NLS-1$
		}
	}
	
	private static class SingleTypeSet extends TypeSet {

		private final TType fType;

		public SingleTypeSet(TType type) {
			fType= type;
		}

		public TypeSet restrictedTo(TypeSet restrictionSet) {
			if (restrictionSet instanceof Universe) {
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
		
		private TypeSet commonLowerBound(SingleTypeSet other) {
			//TODO: see also org.eclipse.jdt.internal.compiler.lookup.Scope.lowerUpperBound(types);
			//and org.eclipse.jdt.internal.compiler.lookup.Scope.mostSpecificCommonType(types)
			//TODO: should either not collapse sets here or support multiple lower bounds
			TType msct= TTypes.mostSpecificCommonType(new TType[] {fType, other.fType});
			return new SingleTypeSet(msct);
//			throw new IllegalStateException(this + " != " + other); //$NON-NLS-1$
		}

		public TType chooseSingleType() {
			return fType;
		}

		public String toString() {
			return fType.getPrettySignature();
		}
	}
	
	private static class MultiTypeSet extends TypeSet {
		private final TType[] fTypes;
		
		public MultiTypeSet(TType[] types) {
			fTypes= types;
		}
		
		public TypeSet restrictedTo(TypeSet restrictionSet) {
			if (restrictionSet instanceof Universe) {
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
	}
	
	private final static Universe fgUniverse= new Universe();
	
	public static TypeSet getUniverse() {
		return fgUniverse;
	}

	public static TypeSet create(TType type) {
		if (type == null)
			return new Universe();
		else
			return new SingleTypeSet(type);
	}

	public abstract TypeSet restrictedTo(TypeSet restrictionSet);
	
	public abstract TType chooseSingleType();
	
	
	public abstract String toString();
}
