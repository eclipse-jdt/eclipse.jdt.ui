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
package org.eclipse.jdt.internal.corext.template.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jdt.core.CompletionRequestorAdapter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

/**
 * A completion requestor to collect informations on local variables.
 * This class is used for guessing variable names like arrays, collections, etc.
 */
class CompilationUnitCompletion extends CompletionRequestorAdapter {

	static class LocalVariable {
		String name;
		String typePackageName;
		String typeName;
		
		LocalVariable(String name, String typePackageName, String typeName) {
			this.name= name;
			this.typePackageName= typePackageName;
			this.typeName= typeName;
		}
	}

	private ICompilationUnit fUnit;

	private List fLocalVariables= new ArrayList();
	private Map fTypes= new HashMap();


	private boolean fError;

	/**
	 * Creates a compilation unit completion.
	 * 
	 * @param unit the compilation unit, may be <code>null</code>.
	 */
	public CompilationUnitCompletion(ICompilationUnit unit) {
		reset(unit);
	}
	
	/**
	 * Resets the completion requestor.
	 * 
	 * @param unit the compilation unit, may be <code>null</code>.
	 */
	public void reset(ICompilationUnit unit) {
		fUnit= unit;
		
		fLocalVariables.clear();
		fTypes.clear();
		
		fError= false;
	}

	/*
	 * @see ICompletionRequestor#acceptError(IProblem)
	 */
	public void acceptError(IProblem error) {
		fError= true;
	}


	/*
	 * @see ICodeCompletionRequestor#acceptLocalVariable
	 */
	public void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName,
		int modifiers, int completionStart,	int completionEnd, int relevance)
	{
		fLocalVariables.add(new LocalVariable(
			new String(name), new String(typePackageName), new String(typeName)));
	}


	// ---

	/**
	 * Tests if the code completion process produced errors.
	 */
	public boolean hasErrors() {
		return fError;
	}
	

	boolean existsLocalName(String name) {
		for (Iterator iterator = fLocalVariables.iterator(); iterator.hasNext();) {
			LocalVariable localVariable = (LocalVariable) iterator.next();

			if (localVariable.name.equals(name))
				return true;
		}

		return false;
	}
	
	String[] getLocalVariableNames() {
		String[] res= new String[fLocalVariables.size()];
		int i= 0;
		for (Iterator iterator = fLocalVariables.iterator(); iterator.hasNext();) {
			LocalVariable localVariable = (LocalVariable) iterator.next();
			res[i++]= localVariable.name;
		}		
		return res;
	}	

	LocalVariable[] findLocalArrays() {
		Vector vector= new Vector();

		for (Iterator iterator= fLocalVariables.iterator(); iterator.hasNext();) {
			LocalVariable localVariable= (LocalVariable) iterator.next();

			if (isArray(localVariable.typeName))
				vector.add(localVariable);
		}

		return (LocalVariable[]) vector.toArray(new LocalVariable[vector.size()]);
	}
	
	LocalVariable[] findLocalCollections() throws JavaModelException {
		Vector vector= new Vector();

		for (Iterator iterator= fLocalVariables.iterator(); iterator.hasNext();) {
			LocalVariable localVariable= (LocalVariable) iterator.next();

			String typeName= qualify(localVariable.typeName);
			
			if (typeName == null)
				continue;
						
			if (isSubclassOf(typeName, "java.util.Collection")) //$NON-NLS-1$			
				vector.add(localVariable);
		}

		return (LocalVariable[]) vector.toArray(new LocalVariable[vector.size()]);
	}

	String simplifyTypeName(String qualifiedName) {
		return (String) fTypes.get(qualifiedName);	
	}

	private static boolean isArray(String type) {
		return type.endsWith("[]"); //$NON-NLS-1$
	}
	
	// returns fully qualified name if successful
	private String qualify(String typeName) throws JavaModelException {
		if (fUnit == null)
			return null;

		IType[] types= fUnit.getTypes();

		if (types.length == 0)
			return null;
		
		String[][] resolvedTypeNames= types[0].resolveType(typeName);

		if (resolvedTypeNames == null)
			return null;
			
		return resolvedTypeNames[0][0] + '.' + resolvedTypeNames[0][1];
	}	
	
	// type names must be fully qualified
	private boolean isSubclassOf(String typeName0, String typeName1) throws JavaModelException {
		if (typeName0.equals(typeName1))
			return true;

		if (fUnit == null)
			return false;

		IJavaProject project= fUnit.getJavaProject();

		IType type0= project.findType(typeName0);
		if (type0 == null)
			return false;

		IType type1= project.findType(typeName1);
		if (type1 == null)
			return false;

		ITypeHierarchy hierarchy= type0.newSupertypeHierarchy(null);
		IType[] superTypes= hierarchy.getAllSupertypes(type0);
		
		for (int i= 0; i < superTypes.length; i++)
			if (superTypes[i].equals(type1))
				return true;			
		
		return false;
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptClass(char[], char[], char[], int, int, int, int)
	 */
	public void acceptClass(char[] packageName, char[] className, char[] completionName, int modifiers,
		int completionStart, int completionEnd, int relevance)
	{
		final String qualifiedName= createQualifiedTypeName(packageName, className);
		fTypes.put(qualifiedName, String.valueOf(completionName));
	}

	/*
	 * @see org.eclipse.jdt.core.ICompletionRequestor#acceptInterface(char[], char[], char[], int, int, int, int)
	 */
	public void acceptInterface(char[] packageName, char[] interfaceName, char[] completionName,
		int modifiers, int completionStart, int completionEnd, int relevance)
	{
		final String qualifiedName= createQualifiedTypeName(packageName, interfaceName);
		fTypes.put(qualifiedName, String.valueOf(completionName));
	}

	private static String createQualifiedTypeName(char[] packageName, char[] className) {
		StringBuffer buffer= new StringBuffer();

		if (packageName.length != 0) {
			buffer.append(packageName);
			buffer.append('.');
		}
		buffer.append(className);
		
		return buffer.toString();
	}

}

