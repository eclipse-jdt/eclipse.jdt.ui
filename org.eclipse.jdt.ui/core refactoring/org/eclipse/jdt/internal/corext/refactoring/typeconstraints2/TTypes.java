/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import java.util.Iterator;
import java.util.Stack;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;

/**
 * Note: This class contains static helper methods to deal with
 * bindings from different clusters. They will be inlined as soon
 * as the compiler loop and subtyping-related queries on ITypeBindings
 * are implemented.  
 */
public class TTypes {
	
	private static class AllSupertypesIterator implements Iterator {
		private final Stack fWorklist;
		
		public AllSupertypesIterator(TType type) {
			fWorklist= new Stack();
			pushSupertypes(type);
		}
	
		public boolean hasNext() {
			return ! fWorklist.empty();
		}
	
		public Object next() {
			TType result= (TType) fWorklist.pop();
			pushSupertypes(result);
			return result;
		}
	
		private void pushSupertypes(TType type) {
			if (! type.isJavaLangObject()) {
				TType superclass= type.getSuperclass();
				if (superclass == null) {
					if (type.isInterface())
						fWorklist.push(type.getEnvironment().getJavaLangObject());
				} else {
					fWorklist.push(superclass);
				}
				TType[] interfaces= type.getInterfaces();
				for (int i= 0; i < interfaces.length; i++)
					fWorklist.push(interfaces[i]);
			}
		}
	
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	private static class AllSubtypesIterator implements Iterator {
		private final Stack fWorklist;
		
		public AllSubtypesIterator(TType type) {
			fWorklist= new Stack();
			fWorklist.push(type);
		}
	
		public boolean hasNext() {
			return ! fWorklist.empty();
		}
	
		public Object next() {
			TType result= (TType) fWorklist.pop();
			TType[] subTypes= result.getSubTypes();
			for (int i= 0; i < subTypes.length; i++)
				fWorklist.push(subTypes[i]);
			
			return result;
		}
	
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	protected static final boolean BUG_84021= true;

	private TTypes() {
		// no instances
	}

	public static TType createArrayType(TType elementType, int dimensions) {
		throw new UnsupportedOperationException(); //TODO waiting for bug 83502
	}

	/**
	 * @return all subtypes of this type (including this type)
	 */
	public static Iterator getAllSubTypesIterator(TType type) {
		return new AllSubtypesIterator(type);
	}

	/**
	 * @return all proper supertypes of this type
	 */
	public static Iterator getAllSuperTypesIterator(TType type) {
		return new AllSupertypesIterator(type);
	}
}
