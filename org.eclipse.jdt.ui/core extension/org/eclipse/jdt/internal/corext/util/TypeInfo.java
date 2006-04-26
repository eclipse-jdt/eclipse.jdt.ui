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
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;

public abstract class TypeInfo {

	public static class TypeInfoAdapter implements ITypeInfoRequestor {
		private TypeInfo fInfo;
		public void setInfo(TypeInfo info) {
			fInfo= info;
		}
		public int getModifiers() {
			return fInfo.getModifiers();
		}
		public String getTypeName() {
			return fInfo.getTypeName();
		}
		public String getPackageName() {
			return fInfo.getPackageName();
		}
		public String getEnclosingName() {
			return fInfo.getEnclosingName();
		}
	}
	
	final String fName;
	final String fPackage;
	final char[][] fEnclosingNames;
	
	int fModifiers;
	
	public static final int UNRESOLVABLE_TYPE_INFO= 1;
	public static final int JAR_FILE_ENTRY_TYPE_INFO= 2;
	public static final int IFILE_TYPE_INFO= 3;
	
	static final char SEPARATOR= '/';
	static final char EXTENSION_SEPARATOR= '.';
	static final char PACKAGE_PART_SEPARATOR='.';
	
	static final String EMPTY_STRING= ""; //$NON-NLS-1$
	
	protected TypeInfo(String pkg, String name, char[][] enclosingTypes, int modifiers) {
		fPackage= pkg;
		fName= name;
		fModifiers= modifiers;
		fEnclosingNames= enclosingTypes;
	}
	
	public int hashCode() {
		return (fPackage.hashCode() << 16) + fName.hashCode();
	}
	
	/**
	 * Returns this type info's kind encoded as an integer.
	 * 
	 * @return the type info's kind
	 */
	public abstract int getElementType();
	
	/**
	 * Returns the path reported by the <tt>ITypeNameRequestor</tt>.
	 * 
	 * @return the path of the type info
	 */
	public abstract String getPath();
	
	/**
	 * Returns the container (class file or CU) this type info is contained
	 * in.
	 *  
	 * @param scope the scope used to resolve the <tt>IJavaElement</tt>.
	 * @return the container this type info is contained in.
	 * @throws JavaModelException if an error occurs while access the Java
	 * model.
	 */
	protected abstract IJavaElement getContainer(IJavaSearchScope scope) throws JavaModelException;
	
	/**
	 * Returns the package fragment root path of this type info.
	 * 
	 * @return the package fragment root as an <tt>IPath</tt>.
	 */
	public abstract IPath getPackageFragmentRootPath();
	
	/**
	 * Returns the package fragment root name of this type info
	 */
	public abstract String getPackageFragmentRootName();
	
	/**
	 * Returns the type's modifiers
	 * 
	 * @return the type's modifiers
	 */
	public int getModifiers() {
		return fModifiers;
	}
	
	/**
	 * Sets the modifiers to the given value.
	 * 
	 * @param modifiers the new modifiers
	 */
	public void setModifiers(int modifiers) {
		fModifiers= modifiers;
	}
	
	/**
	 * Returns the type name.
	 * 
	 * @return the info's type name.
	 */
	public String getTypeName() {
		return fName;
	}
	
	/**
	 * Returns the package name.
	 * 
	 * @return the info's package name.
	 */ 
	public String getPackageName() {
		return fPackage;
	}

	/**
	 * Returns true iff the type info describes an interface.
	 */	
	public boolean isInterface() {
		return Flags.isInterface(fModifiers);
	}
	
	/**
	 * Returns true if the info is enclosed in the given scope
	 */
	public boolean isEnclosed(IJavaSearchScope scope) {
		return scope.encloses(getPath());
	}

	/**
	 * Gets the enclosing name (dot separated).
	 */
	public String getEnclosingName() {
		if (fEnclosingNames == null || fEnclosingNames.length == 0)
			return EMPTY_STRING;
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < fEnclosingNames.length; i++) {
			if (i != 0) {
				buf.append('.');
			}			
			buf.append(fEnclosingNames[i]);
		}
		return buf.toString();
	}
	
	public boolean isInnerType() {
		return fEnclosingNames != null && fEnclosingNames.length > 0;
	}
	
	/**
	 * Gets the type qualified name: Includes enclosing type names, but
	 * not package name. Identifiers are separated by dots.
	 */
	public String getTypeQualifiedName() {
		if (fEnclosingNames != null && fEnclosingNames.length > 0) {
			StringBuffer buf= new StringBuffer();
			for (int i= 0; i < fEnclosingNames.length; i++) {
				buf.append(fEnclosingNames[i]);
				buf.append('.');
			}
			buf.append(fName);
			return buf.toString();
		}
		return fName;
	}
	
	/**
	 * Gets the fully qualified type name: Includes enclosing type names and
	 * package. All identifiers are separated by dots.
	 */
	public String getFullyQualifiedName() {
		StringBuffer buf= new StringBuffer();
		if (fPackage.length() > 0) {
			buf.append(fPackage);
			buf.append('.');
		}
		if (fEnclosingNames != null) {
			for (int i= 0; i < fEnclosingNames.length; i++) {
				buf.append(fEnclosingNames[i]);
				buf.append('.');
			}
		}
		buf.append(fName);
		return buf.toString();
	}
	
	/**
	 * Gets the fully qualified type container name: Package name or
	 * enclosing type name with package name.
	 * All identifiers are separated by dots.
	 */
	public String getTypeContainerName() {
		if (fEnclosingNames != null && fEnclosingNames.length > 0) {
			StringBuffer buf= new StringBuffer();
			if (fPackage.length() > 0) {
				buf.append(fPackage);
			}
			for (int i= 0; i < fEnclosingNames.length; i++) {
				if (buf.length() > 0) {
					buf.append('.');
				}
				buf.append(fEnclosingNames[i]);
			}
			return buf.toString();
		}
		return fPackage;
	}	
	
	/**
	 * Resolves the type in a scope if was searched for.
	 * The parent project of JAR files is the first project found in scope.
	 * Returns null if the type could not be resolved
	 */	
	public IType resolveType(IJavaSearchScope scope) throws JavaModelException {
		IJavaElement elem = getContainer(scope);
		if (elem instanceof ICompilationUnit)
			return JavaModelUtil.findTypeInCompilationUnit((ICompilationUnit)elem, getTypeQualifiedName());
		else if (elem instanceof IClassFile)
			return ((IClassFile)elem).getType();
		return null;
	}

	protected boolean doEquals(TypeInfo other) {
		// Don't compare the modifiers since they aren't relevant to identify
		// a type.
		return fName.equals(other.fName) && fPackage.equals(other.fPackage) 
			&& CharOperation.equals(fEnclosingNames, other.fEnclosingNames);
	}
	
	protected static boolean equals(String s1, String s2) {
		if (s1 == null || s2 == null)
			return s1 == s2;
		return s1.equals(s2);
	}
	
	/* non java-doc
	 * debugging only
	 */		
	public String toString() {
		StringBuffer buf= new StringBuffer();
		buf.append("path= "); //$NON-NLS-1$
		buf.append(getPath());
		buf.append("; pkg= "); //$NON-NLS-1$
		buf.append(fPackage);
		buf.append("; enclosing= "); //$NON-NLS-1$
		buf.append(getEnclosingName());
		buf.append("; name= ");		 //$NON-NLS-1$
		buf.append(fName);
		return buf.toString();
	}
	
	public abstract long getContainerTimestamp();
	
	public abstract boolean isContainerDirty();
}
