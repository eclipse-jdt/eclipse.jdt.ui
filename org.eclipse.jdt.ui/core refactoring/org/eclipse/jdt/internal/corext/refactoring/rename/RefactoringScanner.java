/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.Assert;

public class RefactoringScanner {

	private boolean fAnalyzeJavaDoc;
	private boolean fAnalyzeComments;
	private boolean fAnalyzeStrings;
	
	private String fPattern;
	
	private Set fJavaDocResults;
	private Set fCommentResults;
	private Set fStringResults;
	
	public RefactoringScanner(){
		this(true, true, true);
	}
	
	public RefactoringScanner(boolean analyzeJavaDoc, boolean analyzeComments, boolean analyzeStrings){
		fAnalyzeComments= analyzeComments;
		fAnalyzeJavaDoc= analyzeJavaDoc;
		fAnalyzeStrings= analyzeStrings;
	}
	
	public void scan(ICompilationUnit cu)	throws JavaModelException {
		try{
			scan(cu.getBuffer().getCharacters());		
		} catch (InvalidInputException e){
			//ignore
		}	
	}
	
	public void scan(String text) throws JavaModelException {
		try{
			scan(text.toCharArray());
		} catch (InvalidInputException e){
			//ignore
		}	
	}

	private void scan(char[] content)	throws JavaModelException, InvalidInputException {
		fJavaDocResults= new HashSet();
		fCommentResults= new HashSet();
		fStringResults= new HashSet();
		
		//Scanner scanner = new Scanner(true, true);
		IScanner scanner= ToolFactory.createScanner(true, true, false, true);
		scanner.setSource(content);
		int token = scanner.getNextToken();

		while (token != ITerminalSymbols.TokenNameEOF) {
			switch (token) {
				case ITerminalSymbols.TokenNameStringLiteral :
					if (!fAnalyzeStrings)
						break;
					parseCurrentToken(fStringResults, scanner);	
					break;
				case ITerminalSymbols.TokenNameCOMMENT_JAVADOC :
					if (!fAnalyzeJavaDoc)
						break;
					parseCurrentToken(fJavaDocResults, scanner);	
					break;
				case ITerminalSymbols.TokenNameCOMMENT_LINE :
					if (!fAnalyzeComments)
						break;
					parseCurrentToken(fCommentResults, scanner);	
					break;
				case ITerminalSymbols.TokenNameCOMMENT_BLOCK :
					if (!fAnalyzeComments)
						break;
					parseCurrentToken(fCommentResults, scanner);	
					break;
			}
			token = scanner.getNextToken();
		}
	}

	private static boolean isWholeWord(String value, int from, int to){
		if (from > 0) {
			char ch= value.charAt(from-1);
			if (Character.isLetterOrDigit(ch) || ch == '_') {
				return false;
			}
		}
		if (to < value.length()) {
			char ch= value.charAt(to);
			if (Character.isLetterOrDigit(ch) || ch == '_' ) {
				return false;
			}
		}
		return true;
	}
	
	private void parseCurrentToken(Set result, final IScanner scanner) throws  InvalidInputException {
		String value = new String(scanner.getCurrentTokenSource());
		int start= scanner.getCurrentTokenStartPosition();
		int index= value.indexOf(fPattern);
		while (index != -1) {
			if (isWholeWord(value, index, index + fPattern.length()))			
				result.add(new Integer(start + index));
			index= value.indexOf(fPattern, index + 1);
		}
	}

	public boolean getAnalyzeJavaDoc() {
		return fAnalyzeJavaDoc;
	}

	public void setAnalyzeJavaDoc(boolean analyzeJavaDoc) {
		fAnalyzeJavaDoc = analyzeJavaDoc;
	}

	public boolean getAnalyzeComments() {
		return fAnalyzeComments;
	}

	public void setAnalyzeComments(boolean analyzeComments) {
		fAnalyzeComments= analyzeComments;
	}

	public boolean getAnalyzeStrings() {
		return fAnalyzeStrings;
	}

	public void setAnalyzeStrings(boolean analyzeStrings) {
		fAnalyzeStrings = analyzeStrings;
	}

	public String getPattern() {
		return fPattern;
	}

	public void setPattern(String pattern) {
		Assert.isNotNull(pattern);
		fPattern = pattern;
	}

	public Set getJavaDocResults() {
		return fJavaDocResults;
	}

	public Set getCommentResults() {
		return fCommentResults;
	}

	public Set getStringResults() {
		return fStringResults;
	}
}

