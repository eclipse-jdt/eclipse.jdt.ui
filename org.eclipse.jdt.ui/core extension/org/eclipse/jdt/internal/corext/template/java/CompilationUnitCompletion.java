/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import java.util.Iterator;
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

	private Vector fLocalVariables;


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
		
		fLocalVariables= new Vector();
		
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

	private LocalVariable[] findLocalIntegers() {
		Vector vector= new Vector();

		for (Iterator iterator= fLocalVariables.iterator(); iterator.hasNext();) {
			LocalVariable localVariable= (LocalVariable) iterator.next();

			if (localVariable.typeName.equals("int")) //$NON-NLS-1$
				vector.add(localVariable);
		}

		return (LocalVariable[]) vector.toArray(new LocalVariable[vector.size()]);
	}

	private LocalVariable[] findLocalIterator() throws JavaModelException {
		Vector vector= new Vector();

		for (Iterator iterator= fLocalVariables.iterator(); iterator.hasNext();) {
			LocalVariable localVariable= (LocalVariable) iterator.next();

			String typeName= qualify(localVariable.typeName);			

			if (typeName == null)
				continue;

			if (isSubclassOf(typeName, "java.util.Iterator")) //$NON-NLS-1$
				vector.add(localVariable);
		}

		return (LocalVariable[]) vector.toArray(new LocalVariable[vector.size()]);
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
		IType type1= project.findType(typeName1);

		ITypeHierarchy hierarchy= type0.newSupertypeHierarchy(null);
		IType[] superTypes= hierarchy.getAllSupertypes(type0);
		
		for (int i= 0; i < superTypes.length; i++)
			if (superTypes[i].equals(type1))
				return true;			
		
		return false;
	}

}

