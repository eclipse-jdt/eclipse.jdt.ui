/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.core.refactoring.code;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.core.refactoring.text.SimpleTextChange;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.env.IConstants;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.core.refactoring.TextUtilities;
import org.eclipse.jdt.internal.core.util.HackFinder;

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
public class ExtractMethodRefactoring extends Refactoring{

	private int fTabWidth;
	private ICompilationUnit fCUnit;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	private int fSelectionStart;
	private int fSelectionLength;
	private int fSelectionEnd;
	
	private StatementAnalyzer fStatementAnalyzer;
	private ExtendedBuffer fBuffer;
	private String fMethodName;
	private int fMethodFlags= IConstants.AccProtected;
	
	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	 public String getName(){
	 	return "Extract Method";
	 }

	/**
	 * Creates a new extract method refactoring.
	 *
	 * @param cu the compilation unit which is going to be modified.
	 * @param accessor a callback object to access the source this refactoring is working on.
	 */
	public ExtractMethodRefactoring(ICompilationUnit cu, ITextBufferChangeCreator creator,
			int selectionStart, int selectionLength) {
		fCUnit= cu;
		Assert.isNotNull(fCUnit);
		fTextBufferChangeCreator= creator;
		Assert.isNotNull(fTextBufferChangeCreator);
		fMethodName= "extracted";
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fSelectionEnd= fSelectionStart + fSelectionLength - 1;
	}
	
	/**
	 * Checks if the refactoring can be activated. Activation typically means, if a
	 * corresponding menu entry can be added to the UI.
	 *
	 * @param pm a progress monitor to report progress during activation checking.
	 * @return the refactoring status describing the result of the activation check.	 
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) {
		try {
			pm.beginTask("Checking text selection", 1);
			RefactoringStatus result= new RefactoringStatus();
			
			if (fSelectionStart < 0 || fSelectionLength == 0)
				return mergeTextSelectionStatus(result);
				
			try {
				fBuffer= new ExtendedBuffer(fCUnit.getBuffer());
				fStatementAnalyzer= new StatementAnalyzer(fBuffer, fSelectionStart, fSelectionLength);
				HackFinder.fixMeSoon("1GA9VPL: ITPJUI:WIN2000 - ICompilationUnit should provide method accept(Visitor)");
				((CompilationUnit)fCUnit).accept(fStatementAnalyzer);
			} catch (JavaModelException e) {
				result.addFatalError(e.getStatus().getMessage());
				return result;
			}
			
			fStatementAnalyzer.checkActivation(result);
			return result;
		} finally {
			pm.worked(1);
			pm.done();
		}
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
	
	/**
	 * Creates the actual change object.
	 *
	 * @param pm progress monitor to report progress
	 * @return the change object.
	 * @see IChange
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		if (fMethodName == null)
			return null;
			
		ITextBufferChange result= fTextBufferChangeCreator.create("Extract Method", fCUnit);
		
		
		AbstractMethodDeclaration method= fStatementAnalyzer.getEnclosingMethod();
		
		final int start= method.declarationSourceStart;
		final int end= method.declarationSourceEnd;			
		final int insertPosition= end + 1;
		
		// Inserting the new method
		result.addSimpleTextChange(new SimpleReplaceTextChange("Extracted method", insertPosition) {
			public SimpleTextChange[] adjust(ITextBuffer buffer) {
				int line= buffer.getLineOfOffset(start);
				String delimiter= buffer.getLineDelimiter(line);
				int indent= TextUtilities.getIndent(buffer.getLineContentOfOffset(start), fTabWidth);	
				setText(computeNewMethod(buffer, line, TextUtilities.createIndentString(indent), delimiter));
				return null;
			}
		});
		
		// Replacing the old statements with the new method call.
		result.addSimpleTextChange(new SimpleReplaceTextChange("Changed method", fSelectionStart, fSelectionLength, null) {
			public SimpleTextChange[] adjust(ITextBuffer buffer) {
				String delimiter= buffer.getLineDelimiter(buffer.getLineOfOffset(start));
				int indent= TextUtilities.getIndent(buffer.getLineContentOfOffset(start), fTabWidth);	
				setText(computeCall(buffer, delimiter));
				return null;
			}
		});
		
		return result;
	}
	
	private RefactoringStatus mergeTextSelectionStatus(RefactoringStatus status) {
		status.addFatalError("TextSelection doesn't mark a set of statements");
		return status;	
	}
	
	private String computeNewMethod(ITextBuffer buffer, int lineNumber, String indent, String delimiter) {
		LocalVariableAnalyzer localAnalyzer= fStatementAnalyzer.getLocalVariableAnalyzer();
		
		StringBuffer result= new StringBuffer();
		result.append(delimiter);
		if (insertNewLineAfterMethodBody(buffer, lineNumber))
			result.append(delimiter);
		result.append(indent);
		result.append("protected ");
		result.append(fStatementAnalyzer.getSignature(fMethodName));
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
		
		StringBuffer result= new StringBuffer();
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
		
		// Reformat and add to buffer
		boolean isFirstLine= true;
		for (int i= 0; i < lines.length; i++) {
			String line= lines[i];
			if (line != null) {
				if (!isFirstLine)
					result.append(delimiter);
				else
					isFirstLine= false;			
				result.append(indent);
				result.append(line);
			}
		}
		if (sourceNeedsSemicolon())
			result.append(";");
		result.append(delimiter);
			
		String returnStatement= localAnalyzer.getReturnStatement();
		if (returnStatement != null) {
			result.append(indent);
			result.append(returnStatement);
			result.append(delimiter);
		}
		return result.toString();
	}
	
	private String computeCall(ITextBuffer buffer, String delimiter) {
		String[] lines= buffer.convertIntoLines(fSelectionStart, fSelectionLength);
		int indent= TextUtilities.getIndent(lines[0], fTabWidth);
		StringBuffer result= new StringBuffer(TextUtilities.createIndentString(indent));
		LocalVariableAnalyzer localAnalyzer= fStatementAnalyzer.getLocalVariableAnalyzer();
		
		String local= localAnalyzer.getLocalDeclaration();
		if (local != null) {
			result.append(local);
			result.append(delimiter);
			String firstLine= buffer.getLineContentOfOffset(fSelectionStart);
			indent= TextUtilities.getIndent(firstLine, fTabWidth);
			result.append(TextUtilities.createIndentString(indent));
		}
		result.append(localAnalyzer.getCall(fMethodName));
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
		int start= fStatementAnalyzer.getLastSelectedStatementEnd() + 1;
		int length= fSelectionLength - (start - fSelectionStart);
		if (length <= 0)
			return true;
		if (fBuffer.indexOf(';', start, length) != -1) {
			return true;
		} else {
			return !fStatementAnalyzer.getNeedsSemicolon();
		}
	}
	
	private boolean sourceNeedsSemicolon() {
		int start= fStatementAnalyzer.getLastSelectedStatementEnd() + 1;
		int length= fSelectionLength - (start - fSelectionStart);
		if (length <= 0)
			return false;
		if (fBuffer.indexOf(';', start, length) != -1) {
			return false;
		} else {
			return fStatementAnalyzer.getNeedsSemicolon();
		}
	}	
}