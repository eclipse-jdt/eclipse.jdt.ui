/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;

import org.eclipse.jdt.internal.compiler.IAbstractSyntaxTreeVisitor;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.env.IConstants;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.ASTParentTrackingAdapter;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.core.refactoring.TextUtilities;

/**
 * Extracts a method in a compilation unit based on a text selection range.
 * [ToDo:
 *   - Exceptions         (waiting for 1GBWRMY)
 *   - break statements   (waiting for 1GBWRMY)
 * ]
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class ExtractMethodRefactoring extends Refactoring {

	private ICompilationUnit fCUnit;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	private int fSelectionStart;
	private int fSelectionLength;
	private int fSelectionEnd;
	private boolean fAsymetricAssignment;
	private int fTabWidth;
	private boolean fCallOnDecalrationLine= true;
	
	private StatementAnalyzer fStatementAnalyzer;
	private ExtendedBuffer fBuffer;
	private String fVisibility;
	private String fMethodName;
	private int fMethodFlags= IConstants.AccProtected;
	
	private static final String DEFAULT_VISIBILITY= "default";
	
	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	 public String getName() {
	 	return "Extract Method " + fMethodName + " in " + fCUnit.getElementName();
	 }

	/**
	 * Creates a new extract method refactoring.
	 *
	 * @param cu the compilation unit which is going to be modified.
	 * @param accessor a callback object to access the source this refactoring is working on.
	 */
	public ExtractMethodRefactoring(ICompilationUnit cu, ITextBufferChangeCreator creator,
			int selectionStart, int selectionLength, boolean asymetricAssignment, int tabWidth) {
		fCUnit= cu;
		Assert.isNotNull(fCUnit);
		fTextBufferChangeCreator= creator;
		Assert.isNotNull(fTextBufferChangeCreator);
		fVisibility= "protected";
		fMethodName= "extracted";
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fSelectionEnd= fSelectionStart + fSelectionLength - 1;
		fAsymetricAssignment= asymetricAssignment;
		fTabWidth= tabWidth;
	}
	
	/**
	 * Checks if the refactoring can be activated. Activation typically means, if a
	 * corresponding menu entry can be added to the UI.
	 *
	 * @param pm a progress monitor to report progress during activation checking.
	 * @return the refactoring status describing the result of the activation check.	 
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask("Checking text selection", 1);
			RefactoringStatus result= new RefactoringStatus();
			
			if (fSelectionStart < 0 || fSelectionLength == 0)
				return mergeTextSelectionStatus(result);
			
			if (!(fCUnit instanceof CompilationUnit)) {
				result.addFatalError("Internal error: compilation unit has wrong type.");
				return result;
			}
			fBuffer= new ExtendedBuffer(fCUnit.getBuffer());
			((CompilationUnit)fCUnit).accept(createVisitor());
			
			fStatementAnalyzer.checkActivation(result);
			return result;
		} finally {
			pm.worked(1);
			pm.done();
		}
	}

	private IAbstractSyntaxTreeVisitor createVisitor() {
		fStatementAnalyzer= new StatementAnalyzer(fBuffer, fSelectionStart, fSelectionLength, fAsymetricAssignment);
		ASTParentTrackingAdapter result= new ASTParentTrackingAdapter(fStatementAnalyzer);
		fStatementAnalyzer.setParentTracker(result);
		return result;
	}
	
	/**
	 * Sets the method name to be used for the extracted method.
	 *
	 * @param name the new method name.
	 */	
	public void setMethodName(String name) {
		fMethodName= name;
	}
	
	/**
	 * Returns the method name to be used for the extracted method.
	 * @return the method name to be used for the extracted method.
	 */
	public String getMethodName() {
		return fMethodName;
	} 
	
	/**
	 * Sets the visibility of the new method.
	 * 
	 * @param visibility the visibility of the new method. Valid values are
	 *  "public", "protected", "default", and "private"
	 */
	public void setVisibility(String visibility) {
		fVisibility= visibility;
	}
	
	/**
	 * Checks if the refactoring can work on the values provided by the refactoring
	 * client. The client defined value for the extract method refactoring is the 
	 * new method name.
	 */
	public RefactoringStatus checkMethodName() {
		return Checks.checkMethodName(fMethodName);
	}
	
	
	/* (non-Javadoc)
	 * Method declared in Refactoring
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= null;
		AbstractMethodDeclaration node= fStatementAnalyzer.getEnclosingMethod();
		if (node != null) {
			pm.beginTask("Checking new method name", 4);
			pm.subTask("");
		
			result= Checks.checkMethodName(fMethodName);
			pm.worked(1);
		
			IMethod method= (IMethod)fCUnit.getElementAt(node.sourceStart);
			IType type= method.getDeclaringType();
			LocalVariableAnalyzer localAnalyzer= fStatementAnalyzer.getLocalVariableAnalyzer();
			String[] params= localAnalyzer.getParameterTypes();
		
			result.merge(Checks.checkMethodInType(type, fMethodName, params, false));
			pm.worked(1);
		
			result.merge(Checks.checkMethodInHierarchy(new SubProgressMonitor(pm, 1), 
				type, fMethodName, params, false, fMethodFlags));	
			pm.worked(1);
		
			pm.done();
		} else {
			result= new RefactoringStatus();
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		if (fMethodName == null)
			return null;
		
		adjustSelection();
			
		AbstractMethodDeclaration method= fStatementAnalyzer.getEnclosingMethod();
		String sourceMethodName= new String(method.selector);
		
		ITextBufferChange result= fTextBufferChangeCreator.create("extract method " +
			fMethodName + " from method " + sourceMethodName, fCUnit);
		
		
		final int methodStart= method.declarationSourceStart;
		final int methodEnd= method.declarationSourceEnd;			
		final int insertPosition= methodEnd + 1;
		
		// Inserting the new method
		result.addSimpleTextChange(new SimpleReplaceTextChange("add new method " + fMethodName, insertPosition) {
			public SimpleTextChange[] adjust(ITextBuffer buffer) {
				int startLine= buffer.getLineOfOffset(methodStart);
				int endLine= buffer.getLineOfOffset(methodEnd);
				String delimiter= buffer.getLineDelimiter(startLine);
				int indent= TextUtilities.getIndent(buffer.getLineContentOfOffset(methodStart), fTabWidth);	
				setText(computeNewMethod(buffer, endLine, TextUtilities.createIndentString(indent), delimiter));
				return null;
			}
		});
		
		// Replacing the old statements with the new method call.
		result.addSimpleTextChange(new SimpleReplaceTextChange("substitute statement(s) with call to " + fMethodName, fSelectionStart, fSelectionLength, null) {
			public SimpleTextChange[] adjust(ITextBuffer buffer) {
				String delimiter= buffer.getLineDelimiter(buffer.getLineOfOffset(methodStart));
				setText(computeCall(buffer, delimiter));
				return null;
			}
		});
		
		return result;
	}
	
	/**
	 * Returns the signature of the new method.
	 * 
	 * @return the signature of the extracted method
	 */
	public String getSignature() {
		return getSignature(fMethodName);
	}
	
	/**
	 * Returns the signature of the new method.
	 * 
	 * @param the method name used for the new method
	 * @return the signature of the extracted method
	 */
	public String getSignature(String name) {
		String s= "";
		if (!DEFAULT_VISIBILITY.equals(fVisibility))
			s= fVisibility;
			
		return s + " " + fStatementAnalyzer.getSignature(name);
	}
	
	//---- Helper methods ------------------------------------------------------------------------
	
	private void adjustSelection() {
		int newEnd= fStatementAnalyzer.getAdjustedSelectionEnd();
		if (newEnd != -1) {
			if (newEnd > fSelectionStart) {
				fSelectionEnd= newEnd;
				fSelectionLength= fSelectionEnd - fSelectionStart + 1;
			} else {
				fSelectionLength= 0;
				fSelectionEnd= fSelectionStart - 1;
			}
		}
	}
	
	private RefactoringStatus mergeTextSelectionStatus(RefactoringStatus status) {
		status.addFatalError("Selection doesn't mark a set of statements. Only statements from a method body be extracted.");
		return status;	
	}
	
	private String computeNewMethod(ITextBuffer buffer, int lineNumber, String indent, String delimiter) {
		LocalVariableAnalyzer localAnalyzer= fStatementAnalyzer.getLocalVariableAnalyzer();
		
		StringBuffer result= new StringBuffer();
		result.append(delimiter);
		if (insertNewLineAfterMethodBody(buffer, lineNumber))
			result.append(delimiter);
		result.append(indent);
		result.append(getSignature());
		result.append(" {");
		result.append(delimiter);
		result.append(computeSource(buffer, indent + '\t', delimiter));
		result.append(indent);
		result.append("}");
		return result.toString();
	}
	
	private boolean insertNewLineAfterMethodBody(ITextBuffer buffer, int lineNumber) {
		String line= buffer.getLineContent(lineNumber + 1);
		if (line != null) {
			return TextUtilities.containsOnlyWhiteSpaces(line);
		}
		return true;
	}
	
	private String computeSource(ITextBuffer buffer, String indent, String delimiter) {
		LocalVariableAnalyzer localAnalyzer= fStatementAnalyzer.getLocalVariableAnalyzer();
		final String EMPTY_LINE= "";
		
		String[] lines= buffer.convertIntoLines(fSelectionStart, fSelectionLength);
		
		// Format the first line with the right indent.
		String firstLine= buffer.getLineContentOfOffset(fSelectionStart);
		int firstLineIndent= TextUtilities.getIndent(firstLine, fTabWidth);
		if (lines.length > 0)
			lines[0]= TextUtilities.createIndentString(firstLineIndent) + TextUtilities.removeLeadingIndents(lines[0], fTabWidth);
		
		// Compute the minimal indent.	
		int minIndent= Integer.MAX_VALUE;
		for (int i= 0; i < lines.length; i++) {
			String line= lines[i];
			if (line.length() == 0 && i + 1 == lines.length) {
				lines[i]= null;
			} else if (!TextUtilities.containsOnlyWhiteSpaces(lines[i])) {
				minIndent= Math.min(TextUtilities.getIndent(lines[i], fTabWidth), minIndent);
			} else {
				lines[i]= EMPTY_LINE;
			}	
			
		}
		
		// Remove the indent.
		if (minIndent > 0) {
			for (int i= 0; i < lines.length; i++) {
				String line= lines[i];
				if (line != null && line != EMPTY_LINE)
					lines[i]= TextUtilities.removeIndent(minIndent, line, fTabWidth);
			}
		}
		
		StringBuffer result= new StringBuffer();
		String localReturnValueDeclaration= localAnalyzer.getLocalReturnValueDeclaration();
		addLine(result, indent, localReturnValueDeclaration, delimiter);
		
		boolean isExpressionExtracting= localAnalyzer.getExpressionReturnType() != null;
		if (isExpressionExtracting) {
			result.append(indent);
			result.append("return ");
		}
		
		// Reformat and add to buffer
		boolean isFirstLine= true;
		for (int i= 0; i < lines.length; i++) {
			String line= lines[i];
			if (line != null) {
				if (!isFirstLine) {
					result.append(delimiter);
					result.append(indent);
				} else {
					isFirstLine= false;
					if (!isExpressionExtracting)
						result.append(indent);
				}
				result.append(line);
			}
		}
		if (sourceNeedsSemicolon())
			result.append(";");
		result.append(delimiter);
		
		if (!isExpressionExtracting) {
			String returnStatement= localAnalyzer.getReturnStatement();
			addLine(result, indent, returnStatement, delimiter);
		}
		return result.toString();
	}
	
	private void addLine(StringBuffer buffer, String indent, String line, String delimiter) {
		if (line == null)
			return;
		buffer.append(indent);
		buffer.append(line);
		buffer.append(delimiter);
	}
	
	private String computeCall(ITextBuffer buffer, String delimiter) {
		String[] lines= buffer.convertIntoLines(fSelectionStart, fSelectionLength);
		int indent= TextUtilities.getIndent(lines[0], fTabWidth);
		StringBuffer result= new StringBuffer(TextUtilities.createIndentString(indent));
		LocalVariableAnalyzer localAnalyzer= fStatementAnalyzer.getLocalVariableAnalyzer();
		
		String local= localAnalyzer.getLocalDeclaration();
		if (local != null) {
			result.append(local);
			if (!fCallOnDecalrationLine) {
				result.append(";");
				result.append(delimiter);
				String firstLine= buffer.getLineContentOfOffset(fSelectionStart);
				indent= TextUtilities.getIndent(firstLine, fTabWidth);
				result.append(TextUtilities.createIndentString(indent));
			}
		}
		result.append(localAnalyzer.getCall(fMethodName, fCallOnDecalrationLine));
		if (callNeedsSemicolon())
			result.append(";");
		if (endsSelectionWithLineDelimiter(lines))
			result.append(delimiter);
		return result.toString();
	}

	private boolean endsSelectionWithLineDelimiter(String[] lines) {
		return lines[lines.length - 1].length() == 0;
	}
	
	
	private boolean callNeedsSemicolon() {
		// We are extracting an expression.	
		if (fStatementAnalyzer.getLocalVariableAnalyzer().getExpressionReturnType() != null)
			return false;
		
		int start= fStatementAnalyzer.getLastSelectedStatementEnd() + 1;
		int length= fSelectionLength - (start - fSelectionStart);
		
		if (length < 0) {
			// Check if we have adjusted the selection end for the return statement.
			if (fStatementAnalyzer.getAdjustedSelectionEnd() == -1)
				return false;
			else
				return true;
		}
		
		// Does the source have a semicolon	
		if (fBuffer.indexOf(';', start, length) != -1)
			return true;
		
		return !fStatementAnalyzer.getNeedsSemicolon();
	}
	
	private boolean sourceNeedsSemicolon() {
		return !callNeedsSemicolon();
	}	
}