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
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

public abstract class TypeInfo {

	final String fName;
	final String fPackage;
	final char[][] fEnclosingNames;
	final boolean fIsInterface;
	
	public static final int UNRESOLVABLE_TYPE_INFO= 1;
	public static final int JAR_FILE_ENTRY_TYPE_INFO= 2;
	public static final int IFILE_TYPE_INFO= 3;
	
	static final char SEPARATOR= '/';
	static final char EXTENSION_SEPARATOR= '.';
	static final char PACKAGE_PART_SEPARATOR='.';
	
	protected TypeInfo(String pkg, String name, char[][] enclosingTypes, boolean isInterface) {
		fPackage= pkg;
		fName= name;
		fIsInterface= isInterface;
		fEnclosingNames= enclosingTypes;
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
	 * Returns the <tt>IJavaElement</tt> this type info stands for.
	 *  
	 * @param scope the scope used to resolve the <tt>IJavaElement</tt>.
	 * @return the <tt>IJavaElement</tt> this info stands for.
	 * @throws JavaModelException if an error occurs while access the Java
	 * model.
	 */
	protected abstract IJavaElement getJavaElement(IJavaSearchScope scope) throws JavaModelException;
	
	/**
	 * Returns the package fragment root path of this type info.
	 * 
	 * @return the package fragment root as an <tt>IPath</tt>.
	 */
	public abstract IPath getPackageFragmentRootPath();
	
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
		return fIsInterface;
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
		StringBuffer buf= new StringBuffer();
		if (fEnclosingNames != null) {
			for (int i= 0; i < fEnclosingNames.length; i++) {
				if (i != 0) {
					buf.append('.');
				}			
				buf.append(fEnclosingNames[i]);
			}
		}
		return buf.toString();
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
		IJavaElement elem = getJavaElement(scope);
		if (elem instanceof ICompilationUnit)
			return JavaModelUtil.findTypeInCompilationUnit((ICompilationUnit)elem, getTypeQualifiedName());
		else if (elem instanceof IClassFile)
			return ((IClassFile)elem).getType();
		return null;
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
}
