package org.eclipse.jdt.internal.ui.text.correction;

import java.util.HashSet;

import org.eclipse.jdt.core.CompletionRequestorAdapter;

public class SimilarElementsRequestor extends CompletionRequestorAdapter {
	
	public static final int CLASSES= 1;
	public static final int INTERFACES= 2;
	public static final int TYPES= CLASSES | INTERFACES;
	public static final int METHODS= 4;
	public static final int FIELDS= 8;
	public static final int LOCALS= 16;
	public static final int VARIABLES= FIELDS | LOCALS;

	private String fPreferredType;
	private String[] fArguments;
	private int fKind;
	private String fName;

	private HashSet fResult;
	private HashSet fOthers;

	/**
	 * Constructor for SimilarElementsRequestor.
	 */
	public SimilarElementsRequestor(String name, int kind, String[] arguments, String preferredType) {
		super();
		fName= name;
		fKind= kind;
		fArguments= arguments;
		fPreferredType= preferredType;
		
		fResult= new HashSet();
		fOthers= new HashSet();	
	}
	
	private void addResult(SimilarElement elem) {
		fResult.add(elem);
	}
	
	private void addOther(SimilarElement elem) {
		fOthers.add(elem);
	}	
	
	public HashSet getResults() {
		if (fResult.size() == 0) {
			if (fOthers.size() < 6) {
				return fOthers;
			}
		}
		return fResult;
	}
	
	private void addType(int kind, char[] packageName, char[] typeName, char[] completionName, int relevance) {
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
		addOther(elem);
	}
	
	private void addVariable(int kind, char[] name, int relevance) {
		String variableName= new String(name);
		if (NameMatcher.isSimilarName(fName, variableName)) {
			SimilarElement elem= new SimilarElement(kind, variableName, relevance);
			addResult(elem);
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
			addVariable(FIELDS, name, relevance);
		}
	}

	/* (non-Javadoc)
	 * @see ICompletionRequestor#acceptLocalVariable(char[], char[], char[], int, int, int)
	 */
	public void acceptLocalVariable(char[] name, char[] typePackageName, char[] typeName, int modifiers, int completionStart, int completionEnd, int relevance) {
		if ((fKind & VARIABLES) != 0) {
			addVariable(VARIABLES, name, relevance);
		}
	}
	
	/*
	 * @see ICompletionRequestor#acceptMethod(char[], char[], char[], char[][], char[][], char[][], char[], char[], char[], int, int, int, int)
	 */
	public void acceptMethod(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, char[][] parameterPackageNames, char[][] parameterTypeNames, char[][] parameterNames, char[] returnTypePackageName, char[] returnTypeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
		if ((fKind & METHODS) != 0) {
			String methodName= new String(selector);
			if (fArguments.length == parameterTypeNames.length) {
				int similarity= NameMatcher.getSimilarity(fName, methodName);
				if (similarity >= 0) {
					SimilarElement elem= new SimilarElement(METHODS, methodName, relevance + similarity);
					addResult(elem);
				}
			}
		}
	}	
			

}
