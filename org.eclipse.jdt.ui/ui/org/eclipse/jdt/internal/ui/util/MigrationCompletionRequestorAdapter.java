package org.eclipse.jdt.internal.ui.util;

import org.eclipse.jdt.core.CompletionRequestorAdapter;
import org.eclipse.jdt.core.compiler.IProblem;

public class MigrationCompletionRequestorAdapter extends CompletionRequestorAdapter {

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptAnonymousType(char[], char[], char[][], char[][], char[][], char[], int, int, int)
	 */
	public void acceptAnonymousType(char[] thisTypePackageName, char[] thisTypeName, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] completionName, int modifiers, int completionStart, int completionEnd) {
		this.acceptAnonymousType(thisTypePackageName, thisTypeName, parameterPackageNames, parameterTypeNames, parameterNames, completionName, modifiers, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptClass(char[], char[], char[], int, int, int)
	 */
	public void acceptClass(char[] packageName, char[] className, char[] completionName, int modifiers, int completionStart, int completionEnd) {
		this.acceptClass(packageName, className, completionName, modifiers, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptError(IProblem)
	 */
	public void acceptError(IProblem error) {
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptField(char[], char[], char[], char[], char[], char[], int, int, int)
	 */
	public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] name, char[] typePackageName, char[] typeName, char[] completionName, int modifiers, int completionStart, int completionEnd) {
		this.acceptField(declaringTypePackageName, declaringTypeName, name, typePackageName, typeName, completionName, modifiers, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptInterface(char[], char[], char[], int, int, int)
	 */
	public void acceptInterface(char[] packageName, char[] interfaceName, char[] completionName, int modifiers, int completionStart, int completionEnd) {
		this.acceptInterface(packageName, interfaceName, completionName, modifiers, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptKeyword(char[], int, int)
	 */
	public void acceptKeyword(char[] keywordName, int completionStart, int completionEnd) {
		this.acceptKeyword(keywordName, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptLabel(char[], int, int)
	 */
	public void acceptLabel(char[] labelName, int completionStart, int completionEnd) {
		this.acceptLabel(labelName, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptLocalVariable(char[], char[], char[], int, int, int)
	 */
	public void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName, int modifiers, int completionStart, int completionEnd) {
		this.acceptLocalVariable(name, typePackageName, typeName, modifiers, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptMethod(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int)
	 */
	public void acceptMethod(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers, int completionStart, int completionEnd) {
		this.acceptMethod(declaringTypePackageName, declaringTypeName, selector, parameterPackageNames, parameterTypeNames, parameterNames, returnTypePackageName, returnTypeName, completionName, modifiers, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptMethodDeclaration(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int)
	 */
	public void acceptMethodDeclaration(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers, int completionStart, int completionEnd) {
		this.acceptMethodDeclaration(declaringTypePackageName, declaringTypeName, selector, parameterPackageNames, parameterTypeNames, parameterNames, returnTypePackageName, returnTypeName, completionName, modifiers, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptModifier(char[], int, int)
	 */
	public void acceptModifier(char[] modifierName, int completionStart, int completionEnd) {
		this.acceptModifier(modifierName, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptPackage(char[], char[], int, int)
	 */
	public void acceptPackage(char[] packageName, char[] completionName, int completionStart, int completionEnd) {
		this.acceptPackage(packageName, completionName, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptType(char[], char[], char[], int, int)
	 */
	public void acceptType(char[] packageName, char[] typeName, char[] completionName, int completionStart, int completionEnd) {
		this.acceptType(packageName, typeName, completionName, completionStart, completionEnd, 0);
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptVariableName(char[], char[], char[], char[], int, int)
	 */
	public void acceptVariableName(char[] typePackageName, char[] typeName, char[] name, char[] completionName, int completionStart, int completionEnd) {
		this.acceptVariableName(typePackageName, typeName, name, completionName, completionStart, completionEnd, 0);
	}


	// new methods

	/*
	 * @see ICompletionRequestor#acceptAnonymousType(char[], char[], char[][], char[][], char[][], char[], int, int, int)
	 */
	public void acceptAnonymousType(
		char[] superTypePackageName,
		char[] superTypeName,
		char[][] parameterPackageNames,
		char[][] parameterTypeNames,
		char[][] parameterNames,
		char[] completionName,
		int modifiers,
		int completionStart,
		int completionEnd,
		int relevance) {
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
		int completionEnd,
		int relevance) {
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
		int completionEnd,
		int relevance) {
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
		int completionEnd,
		int relevance) {
	}

	/*
	 * @see ICompletionRequestor#acceptKeyword(char[], int, int)
	 */
	public void acceptKeyword(
		char[] keywordName,
		int completionStart,
		int completionEnd,
		int relevance) {
	}

	/*
	 * @see ICompletionRequestor#acceptLabel(char[], int, int)
	 */
	public void acceptLabel(
		char[] labelName,
		int completionStart,
		int completionEnd,
		int relevance) {
	}

	/*
	 * @see ICompletionRequestor#acceptLocalVariable(char[], char[], char[], int, int, int)
	 */
	public void acceptLocalVariable(
		char[] name,
		char[] typePackageName,
		char[] typeName,
		int modifiers,
		int completionStart,
		int completionEnd,
		int relevance) {
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
		int completionEnd,
		int relevance) {
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
		int completionEnd,
		int relevance) {
	}

	/*
	 * @see ICompletionRequestor#acceptModifier(char[], int, int)
	 */
	public void acceptModifier(
		char[] modifierName,
		int completionStart,
		int completionEnd,
		int relevance) {
	}

	/*
	 * @see ICompletionRequestor#acceptPackage(char[], char[], int, int)
	 */
	public void acceptPackage(
		char[] packageName,
		char[] completionName,
		int completionStart,
		int completionEnd,
		int relevance) {
	}

	/*
	 * @see ICompletionRequestor#acceptType(char[], char[], char[], int, int)
	 */
	public void acceptType(
		char[] packageName,
		char[] typeName,
		char[] completionName,
		int completionStart,
		int completionEnd,
		int relevance) {
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
		int completionEnd,
		int relevance) {
	}


}
