/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

public class TypeRef {

	private char[] fName;
	private char[] fPackage;
	private char[][] fEnclosingNames;
	private boolean fIsInterface;
	private String fPath;

	public TypeRef(char[] pkg, char[] name, char[][] enclosingTypes, String path, boolean isInterface) {
		fPath= path;
		fPackage= pkg;
		fName= name;
		fIsInterface= isInterface;
		fEnclosingNames= enclosingTypes;		
	}
	
	/**
	 * Gets the type name
	 */
	public String getTypeName() {
		return new String(fName);
	}
	 
	/**
	 * Gets the package name
	 */
	public String getPackageName() {
		return new String(fPackage);
	}

	/**
	 * Returns true if the type ref describes an interface
	 */	
	public boolean isInterface() {
		return fIsInterface;
	}

	/**
	 * Gets the enlosing name (dot separated)
	 */
	public String getEnclosingName() {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < fEnclosingNames.length; i++) {
			if (i != 0) {
				buf.append('.');
			}			
			buf.append(fEnclosingNames[i]);
		}
		return buf.toString();
	}	
	
	/**
	 * Gets the type qualified name: Includes enclosing type names, but
	 * not package name. Identifiers are separated by dots.
	 */
	public String getTypeQualifiedName() {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < fEnclosingNames.length; i++) {
			buf.append(fEnclosingNames[i]);
			buf.append('.');
		}
		buf.append(fName);
		return buf.toString();
	}
	
	/**
	 * Gets the fully qualified type name: Includes enclosing type names and
	 * package. All identifiers are separated by dots.
	 */
	public String getFullyQualifiedName() {
		StringBuffer buf= new StringBuffer();
		if (fPackage.length > 0) {
			buf.append(fPackage);
			buf.append('.');
		}
		for (int i= 0; i < fEnclosingNames.length; i++) {
			buf.append(fEnclosingNames[i]);
			buf.append('.');
		}
		buf.append(fName);
		return buf.toString();
	}
	
	/**
	 * Gets the fully qualified type container name: Package name or
	 * enclosing type name with package name
	 * package. All identifiers are separated by dots.
	 */
	public String getTypeContainerName() {
		StringBuffer buf= new StringBuffer();
		if (fPackage.length > 0) {
			buf.append(fPackage);
		}
		for (int i= 0; i < fEnclosingNames.length; i++) {
			buf.append('.');
			buf.append(fEnclosingNames[i]);
		}
		return buf.toString();		
	}	

	/**
	 * Gets the name of they type as it is used for the class file:
	 * Enclosing type name and type name, separated with '$'
	 */	
	public String getVMName() {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < fEnclosingNames.length; i++) {
			buf.append(fEnclosingNames[i]);
			buf.append('$');
		}
		buf.append(fName);
		return buf.toString();
	}
	
	/**
	 * Contructs the package fragment root name from the type ref path
	 */	
	public IPath getPackageFragmentRootPath() {
		int index= fPath.indexOf('|');
		if (index > 0) {
			return new Path(fPath.substring(0, index));
		} else {
			int removeSegments= 1; // the file name
			int packNameLen= fPackage.length;
			if (packNameLen > 0) {
				removeSegments++;
				for (int i= 0; i < packNameLen; i++) {
					if (fPackage[i] == '.') {
						removeSegments++;
					}
				}
			}
			return (new Path(fPath)).removeLastSegments(removeSegments);
		}
	}	
	
	/**
	 * Resolves the type in a scope if was searched for.
	 * The parent project of JAR files is the first project found in scope.
	 * Returns null if the type could not be resolved
	 */	
	public IType resolveType(IJavaSearchScope scope) throws JavaModelException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IJavaElement elem;
		int index= fPath.indexOf('|');		
		if (index > 0) {
			String jarName= fPath.substring(0, index);
			IPath elementPath= new Path(fPath.substring(index + 1));			
			elem= findJarInScope(root, scope, jarName, elementPath);
		} else {
			elem= findInFile(root);
		}
		if (elem instanceof ICompilationUnit) {
			return JavaModelUtility.findTypeInCompilationUnit((ICompilationUnit)elem, getTypeQualifiedName());
		} else if (elem instanceof IClassFile) {
			return ((IClassFile)elem).getType();
		}
		return null;
	}
	

	private IJavaElement findJarInScope(IWorkspaceRoot workspaceRoot, IJavaSearchScope scope, String jarPath, IPath elementPath) throws JavaModelException {
		IJavaModel jmodel= JavaCore.create(workspaceRoot);
		IPath[] enclosedPaths= scope.enclosingProjectsAndJars();
		for (int i= 0; i < enclosedPaths.length; i++) {
			IPath curr= enclosedPaths[i];
			if (curr.segmentCount() == 1) {
				IJavaProject jproject= jmodel.getJavaProject(curr.segment(0));
				IPackageFragmentRoot root= jproject.getPackageFragmentRoot(jarPath);
				if (root.exists()) {
					return jproject.findElement(elementPath);
				}
			}
		}
		return null;
	}	
	
	private IJavaElement findInFile(IWorkspaceRoot root) throws JavaModelException {
		IResource res= root.findMember(new Path(fPath));
		return JavaCore.create(res);
	}	
			
	public String toString() {
		StringBuffer buf= new StringBuffer();
		buf.append("path= ");
		buf.append(fPath);
		buf.append("; pkg= ");
		buf.append(fPackage);
		buf.append("; enclosing= ");
		buf.append(getEnclosingName());
		buf.append("; name= ");		
		buf.append(fName);
		return buf.toString();
	}	
	

}