/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

public class JavaModelUtil {
	/**
	 * Finds a type inside the project with a given fully qualified type name.
	 * Notice the algorithm used below only works for top level types.
	 * A better implemention would use the search engine.
	 */
	public static IType findTypeInProject(IJavaProject project, String typeName) throws JavaModelException {
		String path= typeName.replace('.', '/') + ".java"; //$NON-NLS-1$
		IJavaElement element= project.findElement(new Path(path));
		if (element instanceof ICompilationUnit) {
			String simpleName= Signature.getSimpleName(typeName);
			return ((ICompilationUnit)element).getType(simpleName);
		} else if (element instanceof IClassFile) 
			return ((IClassFile)element).getType();
		return null;
	}

}
