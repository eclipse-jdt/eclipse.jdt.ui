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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.HashSet;

import org.eclipse.jdt.core.CompletionRequestorAdapter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.TypeFilter;

public class SimilarElementsRequestor extends CompletionRequestorAdapter {
	
	public static final int CLASSES= 1 << 1;
	public static final int INTERFACES= 1 << 2;
	public static final int PRIMITIVETYPES= 1 << 3;
	public static final int VOIDTYPE= 1 << 4;
	public static final int REF_TYPES= CLASSES | INTERFACES;
	public static final int ALL_TYPES= PRIMITIVETYPES | REF_TYPES;
	public static final int METHODS= 1 << 5;
	public static final int FIELDS= 1 << 6;
	public static final int LOCALS= 1 << 7;
	public static final int VARIABLES= FIELDS | LOCALS;

	private static final String[] PRIM_TYPES= { "boolean", "byte", "char", "short", "int", "long", "float", "double" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

	private String fPreferredType;
	private int fNumberOfArguments;
	private int fKind;
	private String fName;

	private HashSet fResult;
	private HashSet fOthers;

	public static SimilarElement[] findSimilarElement(ICompilationUnit cu, Name name, int kind) throws JavaModelException {
		int pos= name.getStartPosition();
		int nArguments= -1;
		
		String identifier= ASTNodes.getSimpleNameIdentifier(name);
		String returnType= null;
		ICompilationUnit preparedCU= null;
		
		if ((kind & REF_TYPES) != 0) {
			if (name.isQualifiedName()) {
				pos= ((QualifiedName) name).getName().getStartPosition();
			} else {
				pos= name.getStartPosition() + 1; // first letter must be included, other
			}
			Javadoc javadoc=  (Javadoc) ASTNodes.getParent(name, ASTNode.JAVADOC);
			if (javadoc != null) {
				preparedCU= createPreparedCU(cu, javadoc, name.getStartPosition());
				cu= preparedCU;
			}
			
		} else {	
			if (name.getParent().getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation invocation= (MethodInvocation) name.getParent();
				if (name.equals(invocation.getName())) {
					if ((kind & METHODS) != 0) {
						nArguments= invocation.arguments().size();
					}
				} else if (invocation.arguments().contains(name)) {
					pos= invocation.getStartPosition(); // workaround for code assist
					// foo(| code assist here returns only method declaration
				}
			}
			ITypeBinding binding= ASTResolving.guessBindingForReference(name);
			if (binding != null) {
				returnType= binding.getName();
			}
		}

		try {
			SimilarElementsRequestor requestor= new SimilarElementsRequestor(identifier, kind, nArguments, returnType);
			return requestor.process(cu, pos);		
		} finally {
			if (preparedCU != null) {
				preparedCU.discardWorkingCopy();
			}
		}
	}
	
	private static ICompilationUnit createPreparedCU(ICompilationUnit cu, Javadoc comment, int wordStart) throws JavaModelException {
		int startpos= comment.getStartPosition();
		boolean isTopLevel= comment.getParent().getParent() instanceof CompilationUnit;
		char[] content= (char[]) cu.getBuffer().getCharacters().clone();
		if (isTopLevel && (wordStart + 6 < content.length)) {
			content[startpos++]= 'i'; content[startpos++]= 'm'; content[startpos++]= 'p';
			content[startpos++]= 'o'; content[startpos++]= 'r'; content[startpos++]= 't';
		}		
		if (wordStart < content.length) {
			for (int i= startpos; i < wordStart; i++) {
				content[i]= ' ';
			}
		}

		/*
		 * Explicitly create a new non-shared working copy.
		 */
		ICompilationUnit newCU= cu.getWorkingCopy(null);
		newCU.getBuffer().setContents(content);
		return newCU;
	}
	

	/**
	 * Constructor for SimilarElementsRequestor.
	 */
	private SimilarElementsRequestor(String name, int kind, int nArguments, String preferredType) {
		super();
		fName= name;
		fKind= kind;
		fNumberOfArguments= nArguments;
		fPreferredType= preferredType;
		
		fResult= new HashSet();
		fOthers= new HashSet();	
	}
	
	private void addResult(SimilarElement elem) {
		fResult.add(elem);
	}
	
	private SimilarElement[] process(ICompilationUnit cu, int pos) throws JavaModelException {
		try {
			cu.codeComplete(pos, this);
			processKeywords();
			
			if (fResult.size() == 0) {
				if (fOthers.size() < 6) {
					fResult= fOthers;
				}
			}
			return (SimilarElement[]) fResult.toArray(new SimilarElement[fResult.size()]);
		} finally {
			fResult.clear();
			fOthers.clear();
		}
	}

	/**
	 * Method addPrimitiveTypes.
	 */
	private void processKeywords() {
		if ((fKind & PRIMITIVETYPES) != 0) {
			for (int i= 0; i < PRIM_TYPES.length; i++) {
				if (NameMatcher.isSimilarName(fName, PRIM_TYPES[i])) {
					addResult(new SimilarElement(PRIMITIVETYPES, PRIM_TYPES[i], 50));
				}			
			}
		}
		if ((fKind & VOIDTYPE) != 0) {
			String voidType= "void"; //$NON-NLS-1$
			if (NameMatcher.isSimilarName(fName, voidType)) {
				addResult(new SimilarElement(PRIMITIVETYPES, voidType, 50));
			}
		}
	}
	
	private void addType(int kind, char[] packageName, char[] typeName, char[] completionName, int relevance) {
		if (TypeFilter.isFiltered(packageName)) {
			return;
		}
		
		StringBuffer buf= new StringBuffer();
		if (packageName.length > 0) {
			buf.append(packageName);
			buf.append('.');
		}
		buf.append(typeName);
		SimilarElement elem= new SimilarElement(kind, buf.toString(), relevance);

		if (NameMatcher.isSimilarName(fName, new String(typeName))) {
			addResult(elem);
		}
		//addOther(elem);
	}
	
	private void addVariable(int kind, char[] name, char[] typePackageName, char[] typeName, int relevance) {
		if (TypeFilter.isFiltered(typePackageName)) {
			return;
		}
		
		String variableName= new String(name);
		if (NameMatcher.isSimilarName(fName, variableName)) {
			addResult(new SimilarElement(kind, variableName, 1));
		} else if (fPreferredType != null) {
			if (fPreferredType.equals(JavaModelUtil.concatenateName(typePackageName, typeName))) {
				addResult(new SimilarElement(kind, variableName, 0));
			}
		}
		
	}	
	
	/*
	 * @see ICompletionRequestor#acceptClass(char[], char[], char[], int, int, int)
	 */
	public void acceptClass(char[] packageName, char[] className, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		if ((fKind & CLASSES) != 0) {
			addType(CLASSES, packageName, className, completionName, relevance);
		}
	}

	/*
	 * @see ICompletionRequestor#acceptInterface(char[], char[], char[], int, int, int)
	 */
	public void acceptInterface(char[] packageName, char[] interfaceName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		if ((fKind & INTERFACES) != 0) {
			addType(INTERFACES, packageName, interfaceName, completionName, relevance);
		}
	}
	
	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptField(char[], char[], char[], char[], char[], char[], int, int, int, int)
	 */
	public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] name, char[] typePackageName, char[] typeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		if ((fKind & FIELDS) != 0) {
			addVariable(FIELDS, name, typePackageName, typeName, relevance);
		}
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptLocalVariable(char[], char[], char[], int, int, int)
	 */
	public void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName, int modifiers, int completionStart, int completionEnd, int relevance) {
		if ((fKind & VARIABLES) != 0) {
			addVariable(VARIABLES, name,  typePackageName, typeName, relevance);
		}
	}
	
	/*
	 * @see ICompletionRequestor#acceptMethod(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int, int)
	 */
	public void acceptMethod(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		if ((fKind & METHODS) != 0) {
			if (TypeFilter.isFiltered(declaringTypePackageName)) {
				return;
			}
			
			String methodName= new String(selector);
			if (fName.equals(methodName)) {
				String[] paramTypes= new String[parameterTypeNames.length];
				for (int i= 0; i < paramTypes.length; i++) {
					paramTypes[i]= JavaModelUtil.concatenateName(parameterPackageNames[i], parameterTypeNames[i]);
				}
				addResult(new SimilarElement(METHODS, methodName, paramTypes, relevance));
			} else if ((fNumberOfArguments == -1 || fNumberOfArguments == parameterTypeNames.length)) {
				int similarity= NameMatcher.getSimilarity(fName, methodName);
				if (similarity >= 0) {
					SimilarElement elem= new SimilarElement(METHODS, methodName, null, relevance + similarity);
					addResult(elem);
				}
			}
		}
	}	
			

}
