package org.eclipse.jdt.internal.ui.text.correction;

import java.util.HashSet;

import org.eclipse.jdt.core.CompletionRequestorAdapter;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

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
	private String[] fArguments;
	private int fKind;
	private String fName;

	private HashSet fResult;
	private HashSet fOthers;
	

	public static SimilarElement[] findSimilarElement(ICompilationUnit cu, int pos, String name, int kind) throws JavaModelException {
		return findSimilarElement(cu, pos, name, kind, null, null);
	}
	
	public static SimilarElement[] findSimilarElement(ICompilationUnit cu, int pos, String name, int kind, String[] arguments, String preferredType) throws JavaModelException {
		SimilarElementsRequestor requestor= new SimilarElementsRequestor(name, kind, arguments, preferredType);
		return requestor.process(cu, pos);
	}
	

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
	
	public SimilarElement[] process(ICompilationUnit cu, int pos) throws JavaModelException {
		try {
			IBuffer buf= cu.getBuffer();
			if (pos < buf.getLength() - 1) {
				if ((fKind & REF_TYPES) != 0) {
					pos++;
				} else {
					int prevPos= pos - 1;
					while (prevPos >= 0 && Character.isWhitespace(buf.getChar(prevPos))) {
						prevPos--;
					}
					if (prevPos >= 0 && buf.getChar(prevPos) == '(') {
						pos++;
					}
				}
			}
			
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
