package org.eclipse.jdt.internal.ui.text.template;

import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IMarker;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.core.ICompletionRequestor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class TemplateCollector implements ICompletionRequestor {

	// general
	private static final String FILE= "file"; //$NON-NLS-1$
	private static final String LINE= "line"; //$NON-NLS-1$
	private static final String DATE= "date"; //$NON-NLS-1$
	private static final String TIME= "time"; //$NON-NLS-1$
	private static final String USER= "user"; //$NON-NLS-1$
	
	// arrays
	private static final String ARRAY= "array"; //$NON-NLS-1$
	private static final String ARRAY_TYPE= "array_type"; //$NON-NLS-1$
	private static final String ARRAY_ELEMENT= "array_element"; //$NON-NLS-1$
	private static final String INDEX= "index"; //$NON-NLS-1$

	// collections
	private static final String COLLECTION= "collection"; //$NON-NLS-1$
	private static final String ITERATOR= "iterator"; //$NON-NLS-1$

	// methods
	private static final String RETURN_TYPE= "return_type"; //$NON-NLS-1$
	private static final String ARGUMENTS= "arguments"; //$NON-NLS-1$

	private static final String[][] fgVariables = {
		{FILE,			TemplateMessages.getString("TemplateCollector.variable.description.file")},
//		{LINE,			TemplateMessages.getString("TemplateCollector.variable.description.line")},
		{DATE,			TemplateMessages.getString("TemplateCollector.variable.description.date")},
		{TIME,			TemplateMessages.getString("TemplateCollector.variable.description.time")},
		{USER,			TemplateMessages.getString("TemplateCollector.variable.description.user")},
		{ARRAY,			TemplateMessages.getString("TemplateCollector.variable.description.array")},
		{ARRAY_TYPE,	TemplateMessages.getString("TemplateCollector.variable.description.array.type")},
		{ARRAY_ELEMENT,	TemplateMessages.getString("TemplateCollector.variable.description.array.element")},
		{INDEX,			TemplateMessages.getString("TemplateCollector.variable.description.index")},
		{COLLECTION,	TemplateMessages.getString("TemplateCollector.variable.description.collector")},
		{ITERATOR,		TemplateMessages.getString("TemplateCollector.variable.description.iterator")},
		{RETURN_TYPE,	TemplateMessages.getString("TemplateCollector.variable.description.return.type")},
		{ARGUMENTS,		TemplateMessages.getString("TemplateCollector.variable.description.arguments")}		
	};

	private static class LocalVariable {
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

	private Vector fClasses;
	private Vector fFields;
	private Vector fInterfaces;
	private Vector fKeywords;
	private Vector fLabels;
	private Vector fLocalVariables;
	private Vector fMethods;
	private Vector fMethodDeclarations;
	private Vector fModifiers;
	private Vector fPackages;
	private Vector fTypes;
	private Vector fVariableNames;

	private boolean fError;

	public TemplateCollector(ICompilationUnit unit) {
		Assert.isNotNull(unit);
		
		reset(unit);
	}
	
	public void reset(ICompilationUnit unit) {
		Assert.isNotNull(unit);		
		fUnit= unit;
		
		fClasses= new Vector();
		fFields= new Vector();
		fInterfaces= new Vector();
		fKeywords= new Vector();
		fLabels= new Vector();
		fLocalVariables= new Vector();
		fMethods= new Vector();
		fMethodDeclarations= new Vector();
		fModifiers= new Vector();
		fPackages= new Vector();
		fTypes= new Vector();
		fVariableNames= new Vector();		
		
		fError= false;
	}

	/*
	 * @see ICompletionRequestor#acceptClass(char[], char[], char[], int, int, int)
	 */
	public void acceptClass(
		char[] packageName,
		char[] className,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd) {
	}

	/*
	 * @see ICompletionRequestor#acceptError(IMarker)
	 */
	public void acceptError(IMarker marker) {
		fError= true;
	}

	/*
	 * @see ICompletionRequestor#acceptField(char[], char[], char[], char[], char[], char[], int, int, int)
	 */
	public void acceptField(
		char[] declaringTypePackageName,
		char[] declaringTypeName,
		char[] name,
		char[] typePackageName,
		char[] typeName,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd) {
	}

	/*
	 * @see ICompletionRequestor#acceptInterface(char[], char[], char[], int, int, int)
	 */
	public void acceptInterface(
		char[] packageName,
		char[] interfaceName,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd) {
	}

	/*
	 * @see ICompletionRequestor#acceptKeyword(char[], int, int)
	 */
	public void acceptKeyword(
		char[] keywordName,
		int completionStart,
		int completionEnd) {
	}

	/*
	 * @see ICompletionRequestor#acceptLabel(char[], int, int)
	 */
	public void acceptLabel(
		char[] labelName,
		int completionStart,
		int completionEnd) {
	}

	/*
	 * @see ICompletionRequestor#acceptLocalVariable(char[], char[], char[], int, int, int)
	 */
	public void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName,
		int modifiers, int completionStart,	int completionEnd)
	{
		fLocalVariables.add(new LocalVariable(
			new String(name), new String(typePackageName), new String(typeName)));
	}

	/*
	 * @see ICompletionRequestor#acceptMethod(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int)
	 */
	public void acceptMethod(
		char[] declaringTypePackageName,
		char[] declaringTypeName,
		char[] selector,
		char[][] parameterPackageNames,
		char[][] parameterTypeNames,
		char[][] parameterNames,
		char[] returnTypePackageName,
		char[] returnTypeName,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd) {
	}

	/*
	 * @see ICompletionRequestor#acceptMethodDeclaration(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int)
	 */
	public void acceptMethodDeclaration(
		char[] declaringTypePackageName,
		char[] declaringTypeName,
		char[] selector,
		char[][] parameterPackageNames,
		char[][] parameterTypeNames,
		char[][] parameterNames,
		char[] returnTypePackageName,
		char[] returnTypeName,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd) {
	}

	/*
	 * @see ICompletionRequestor#acceptModifier(char[], int, int)
	 */
	public void acceptModifier(
		char[] modifierName,
		int completionStart,
		int completionEnd) {
	}

	/*
	 * @see ICompletionRequestor#acceptPackage(char[], char[], int, int)
	 */
	public void acceptPackage(
		char[] packageName,
		char[] completionName,
		int completionStart,
		int completionEnd) {
	}

	/*
	 * @see ICompletionRequestor#acceptType(char[], char[], char[], int, int)
	 */
	public void acceptType(
		char[] packageName,
		char[] typeName,
		char[] completionName,
		int completionStart,
		int completionEnd) {
	}

	/*
	 * @see ICompletionRequestor#acceptVariableName(char[], char[], char[], char[], int, int)
	 */
	public void acceptVariableName(
		char[] typePackageName,
		char[] typeName,
		char[] name,
		char[] completionName,
		int completionStart,
		int completionEnd) {
	}
	
	/*
	 * @see ICompletionRequestor#acceptAnonymousType(char[], char[], char[][], char[][], char[][], char[], int, int, int)
	 */
	public void acceptAnonymousType(char[] superTypePackageName, char[] superTypeName, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] completionName, int modifiers, int completionStart, int completionEnd) {
	}	

	// ---

	/**
	 * Tests if the code completion process produced errors.
	 */
	public boolean hasErrors() {
		return fError;
	}
	
	/**
	 * Evaluate a variable. Returns <code>null</code> for unrecognized
	 * variables or ambiguous matches.
	 */
	public String evaluate(String variable) {
		try {
		
			// guess array
			if (variable.equals(ARRAY)) {
				LocalVariable[] localArrays= findLocalArrays();
				
				if (localArrays.length > 0)
					return localArrays[localArrays.length - 1].name;
	
			// guess array base type
			} else if (variable.equals(ARRAY_TYPE)) {
				LocalVariable[] localArrays= findLocalArrays();
				
				if (localArrays.length > 0) {
					String typeName= localArrays[localArrays.length - 1].typeName;
					return typeName.substring(0, typeName.indexOf('['));
				}
	
			// guess array element
			} else if (variable.equals(ARRAY_ELEMENT)) {
				LocalVariable[] localArrays= findLocalArrays();
				
				if (localArrays.length > 0) {
					String typeName= localArrays[localArrays.length - 1].typeName;
					String baseTypeName= typeName.substring(0, typeName.indexOf('['));
					String variableName= typeToVariable(baseTypeName);
					
					if (!existsLocalName(variableName))
						return variableName;
				}
	
			// guess collections
			} else if (variable.equals(COLLECTION)) {
				LocalVariable[] localCollections= findLocalCollections();
				
				if (localCollections.length > 0)
					return localCollections[localCollections.length - 1].name;
					
			// find non colliding index
			} else if (variable.equals(INDEX)) {
				String[] proposals= {"i", "j", "k"};  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				for (int i= 0; i != proposals.length; i++) {
					String proposal = proposals[i];
	
					if (!existsLocalName(proposal))
						return proposal;
				}
	
			// find non colliding iterator
			} else if (variable.equals(ITERATOR)) {
				String[] proposals= {"iter"}; //$NON-NLS-1$
				
				for (int i= 0; i != proposals.length; i++) {
					String proposal = proposals[i];
	
					if (!existsLocalName(proposal))
						return proposal;
				}
	
			} else if (variable.equals(RETURN_TYPE)) {
				return "void"; //$NON-NLS-1$
	
			} else if (variable.equals(ARGUMENTS)) {
				return ""; //$NON-NLS-1$

			} else if (variable.equals(FILE)) {
				if (fUnit != null)
					return fUnit.getElementName();
	
//			} else if (variable.equals(LINE)) {				
//				try {
//					int line= document.getLineOfOffset(offset) + 1;
//					return Integer.toString(line);
//
//				} catch (BadLocationException e) {
//					JavaPlugin.log(e);
//					openErrorDialog(null, e);			
//				}
				
			} else if (variable.equals(DATE)) {
				return DateFormat.getDateInstance().format(new Date());
	
			} else if (variable.equals(TIME)) {
				return DateFormat.getTimeInstance().format(new Date());

			} else if (variable.equals(USER)) {
				return System.getProperty("user.name");
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			openErrorDialog(null, e);			
		}
		
		return null;
	}

	private boolean existsLocalName(String name) {
		for (Iterator iterator = fLocalVariables.iterator(); iterator.hasNext();) {
			LocalVariable localVariable = (LocalVariable) iterator.next();

			if (localVariable.name.equals(name))
				return true;
		}

		return false;
	}

	private LocalVariable[] findLocalArrays() {
		Vector vector= new Vector();

		for (Iterator iterator= fLocalVariables.iterator(); iterator.hasNext();) {
			LocalVariable localVariable= (LocalVariable) iterator.next();

			if (isArray(localVariable.typeName))
				vector.add(localVariable);
		}

		return (LocalVariable[]) vector.toArray(new LocalVariable[vector.size()]);
	}
	
	private LocalVariable[] findLocalCollections() throws JavaModelException {
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
		IType[] types= fUnit.getTypes();

		if (types.length == 0)
			return null;
		
		return types[0].getFullyQualifiedName();		
	}	
	
	// type names must be fully qualified
	private boolean isSubclassOf(String typeName0, String typeName1) throws JavaModelException {
		if (typeName0.equals(typeName1))
			return true;

		IJavaProject project= fUnit.getJavaProject();
		IType type0= JavaModelUtil.findType(project, typeName0);
		IType type1= JavaModelUtil.findType(project, typeName1);

		ITypeHierarchy hierarchy= type0.newSupertypeHierarchy(null);
		IType[] superTypes= hierarchy.getAllSupertypes(type0);
		
		for (int i= 0; i < superTypes.length; i++)
			if (superTypes[i].equals(type1))
				return true;			
		
		return false;
	}

	private static String typeToVariable(String string) {
		Assert.isTrue(string.length() > 0);		
		char first= string.charAt(0);
		
		// base type
		if (Character.isLowerCase(first))
			return "value"; //$NON-NLS-1$

		// class or interface
		return Character.toLowerCase(first) + string.substring(1);
	}
	
	private static void openErrorDialog(Shell shell, Exception e) {
		MessageDialog.openError(shell, TemplateMessages.getString("TemplateCollector.error.title"), e.getMessage()); //$NON-NLS-1$
	}
	
	/**
	 * Returns all variables with special meaning.
	 * The size of the second dimension is two. The first value is the
	 * name of the variable, the second value is its description.
	 */
	public static String[][] getVariables() {
		return fgVariables;
	}


}

