/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.util;

import java.util.Arrays;
import java.util.List;

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


public class TypeInfo {

	private final String fName;
	private final String fPackage;
	private final char[][] fEnclosingNames;
	private final boolean fIsInterface;
	private final String fPath;

	public TypeInfo(char[] pkg, char[] name, char[][] enclosingTypes, String path, boolean isInterface) {
		this(new String(pkg), new String(name), enclosingTypes, path, isInterface);
	}
	
	public TypeInfo(String pkg, String name, char[][] enclosingTypes, String path, boolean isInterface) {
		fPath= path;
		fPackage= pkg;
		fName= name;
		fIsInterface= isInterface;
		fEnclosingNames= enclosingTypes;
	}	
	
	public String getTypeName() {
		return fName;
	}
	 
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
		return scope.encloses(fPath);
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
	 * Contructs the package fragment root name from the type ref path.
	 */	
	public IPath getPackageFragmentRootPath() {
		int index= fPath.indexOf(IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR);
		if (index > 0) {
			return new Path(fPath.substring(0, index));
		} else {
			int removeSegments= 1; // the file name
			int packNameLen= fPackage.length();
			if (packNameLen > 0) {
				removeSegments++;
				for (int i= 0; i < packNameLen; i++) {
					if (fPackage.charAt(i) == '.')
						removeSegments++;
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
		IJavaElement elem = getJavaElement(scope);
		if (elem instanceof ICompilationUnit)
			return JavaModelUtil.findTypeInCompilationUnit((ICompilationUnit)elem, getTypeQualifiedName());
		else if (elem instanceof IClassFile)
			return ((IClassFile)elem).getType();
		return null;
	}

	private IJavaElement getJavaElement(IJavaSearchScope scope) throws JavaModelException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		int index= fPath.indexOf(IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR);
		if (index > 0) 
			return findJarInScope(root, scope, fPath.substring(0, index), new Path(fPath.substring(index + 1)));
		else 
			return findInFile(root);
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
		List paths= Arrays.asList(enclosedPaths);
		IJavaProject[] projects= jmodel.getJavaProjects();
		for (int i= 0; i < projects.length; i++) {
			IJavaProject jproject= projects[i];
			if (!paths.contains(jproject.getPath())) {
				IPackageFragmentRoot root= jproject.getPackageFragmentRoot(jarPath);
				if (root.exists()) {
					return jproject.findElement(elementPath);
				}
			}
		}
		return null;
	}	
	
	private IJavaElement findInFile(IWorkspaceRoot root) throws JavaModelException {
		return JavaCore.create(root.findMember(new Path(fPath)));
	}	
		
	/* non java-doc
	 * debugging only
	 */		
	public String toString() {
		StringBuffer buf= new StringBuffer();
		buf.append("path= "); //$NON-NLS-1$
		buf.append(fPath);
		buf.append("; pkg= "); //$NON-NLS-1$
		buf.append(fPackage);
		buf.append("; enclosing= "); //$NON-NLS-1$
		buf.append(getEnclosingName());
		buf.append("; name= ");		 //$NON-NLS-1$
		buf.append(fName);
		return buf.toString();
	}	
}