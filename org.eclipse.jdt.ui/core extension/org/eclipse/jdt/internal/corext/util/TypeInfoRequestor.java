/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.util;

import java.util.Collection;

import org.eclipse.jface.util.Assert;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.ITypeNameRequestor;

public class TypeInfoRequestor implements ITypeNameRequestor {
	
	private Collection fTypesFound;
	private TypeInfoFactory fFactory;
	
	/**
	 * Constructs the TypeRefRequestor
	 * @param typesFound Will collect all TypeRef's found
	 */
	public TypeInfoRequestor(Collection typesFound) {
		Assert.isNotNull(typesFound);
		fTypesFound= typesFound;
		IJavaModel model= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		String[] projectNames;
		try {
			IJavaProject[] projects= model.getJavaProjects();
			projectNames= new String[projects.length];
			for (int i= 0; i < projects.length; i++) {
				projectNames[i]= projects[i].getElementName();
			}
		} catch (JavaModelException e) {
			projectNames= new String[0];
		}
		fFactory= new TypeInfoFactory(projectNames);
	}

	/* non java-doc
	 * @see ITypeNameRequestor#acceptInterface
	 */
	public void acceptInterface(char[] packageName, char[] typeName, char[][] enclosingTypeNames,String path) {
		fTypesFound.add(fFactory.create(packageName, typeName, enclosingTypeNames, true, path));
	}

	/* non java-doc
	 * @see ITypeNameRequestor#acceptClass
	 */	
	public void acceptClass(char[] packageName, char[] typeName, char[][] enclosingTypeNames, String path) {
		fTypesFound.add(fFactory.create(packageName, typeName, enclosingTypeNames, false, path));
	}
	
}