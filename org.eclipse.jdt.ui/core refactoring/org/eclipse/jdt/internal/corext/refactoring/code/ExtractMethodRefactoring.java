/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.IAbstractSyntaxTreeVisitor;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.env.IConstants;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.TextUtilities;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTParentTrackingAdapter;

/**
 * Extracts a method in a compilation unit based on a text selection range.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class ExtractMethodRefactoring extends Refactoring {

	private ICompilationUnit fCUnit;
	private int fSelectionStart;
	private int fSelectionLength;
	private int fSelectionEnd;
	private String fAssignment;
	private int fTabWidth;
	private boolean fCallOnDecalrationLine= true;
	
	private ExtractMethodAnalyzer fAnalyzer;
	private ExtendedBuffer fBuffer;
	private String fVisibility;
	private String fMethodName;
	private int fMethodFlags= IConstants.AccProtected;

	private static final String EMPTY= "";
	private static final String BLANK= " ";
	private static final String RETURN= "return";
	private static final String RETURN_BLANK= "return ";
	private static final String SEMICOLON= ";";
	private static final String COMMA_BLANK= ", ";
	private static final String STATIC= "static";
	
	private class InsertNewMethod extends SimpleTextEdit {
		private int fMethodStart;
		private int fMethodEnd;
		public InsertNewMethod(int offset, int methodStart, int methodEnd) {
			super(offset, 0, "");
			fMethodStart= methodStart;
			fMethodEnd= methodEnd;
		}
		public void connect(TextBufferEditor editor) {
			TextBuffer buffer= editor.getTextBuffer();
			int startLine= buffer.getLineOfOffset(fMethodStart);
			int endLine= buffer.getLineOfOffset(fMethodEnd);
			String delimiter= buffer.getLineDelimiter(startLine);
			int indent= TextUtilities.getIndent(buffer.getLineContentOfOffset(fMethodStart), fTabWidth);	
			setText(computeNewMethod(buffer, endLine, TextUtilities.createIndentString(indent), delimiter));
		}
		public TextEdit copy() {
			return new InsertNewMethod(getTextRange().getOffset(), fMethodStart, fMethodEnd);
		}
	}
	
	private class ReplaceCall extends SimpleTextEdit {
		private int fMethodStart;
		public ReplaceCall(int offset, int length, int methodStart) {
			super(offset, length, "");
			fMethodStart= methodStart;
		}
		public void connect(TextBufferEditor editor) {
			TextBuffer buffer= editor.getTextBuffer();
			String delimiter= buffer.getLineDelimiter(buffer.getLineOfOffset(fMethodStart));
			setText(computeCall(buffer, delimiter));
		}
		public TextEdit copy() {
			TextRange range= getTextRange();
			return new ReplaceCall(range.getOffset(), range.getLength(), fMethodStart);
		}
	}
	
	/**
	 * Creates a new extract method refactoring.
	 *
	 * @param cu the compilation unit which is going to be modified.
	 * @param accessor a callback object to access the source this refactoring is working on.
	 */
	public ExtractMethodRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength, boolean asymetricAssignment, int tabWidth) {
		fCUnit= cu;
		Assert.isNotNull(fCUnit);
		fVisibility= "protected"; //$NON-NLS-1$
		fMethodName= "extracted"; //$NON-NLS-1$
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fSelectionEnd= fSelectionStart + fSelectionLength - 1;
		if (asymetricAssignment)
			fAssignment= "= ";
		else
			fAssignment= " = ";
		fTabWidth= tabWidth;
	}
	
	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	 public String getName() {
	 	return RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.name", new String[]{fMethodName, fCUnit.getElementName()}); //$NON-NLS-1$
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
			pm.beginTask(RefactoringCoreMessages.getString("ExtractMethodRefactoring.checking_selection"), 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			
			if (fSelectionStart < 0 || fSelectionLength == 0)
				return mergeTextSelectionStatus(result);
			
			if (!(fCUnit instanceof CompilationUnit)) {
				result.addFatalError(RefactoringCoreMessages.getString("ExtractMethodRefactoring.Internal_error")); //$NON-NLS-1$
				return result;
			}
			if (!fCUnit.isStructureKnown()) {
				result.addFatalError(RefactoringCoreMessages.getString("ExtractMethodRefactoring.Syntax_errors")); //$NON-NLS-1$
				return result;
			}
			fBuffer= new ExtendedBuffer(fCUnit.getBuffer());
			((CompilationUnit)fCUnit).accept(createVisitor());
			
			fAnalyzer.checkActivation(result);
			return result;
		} finally {
			pm.worked(1);
			pm.done();
		}
	}


	private IAbstractSyntaxTreeVisitor createVisitor() {
		fAnalyzer= new ExtractMethodAnalyzer(fBuffer, fSelectionStart, fSelectionLength, true);
		ASTParentTrackingAdapter result= new ASTParentTrackingAdapter(fAnalyzer);
		fAnalyzer.setParentTracker(result);
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
		AbstractMethodDeclaration node= fAnalyzer.getEnclosingMethod();
		if (node != null) {
			pm.beginTask(RefactoringCoreMessages.getString("ExtractMethodRefactoring.checking_new_name"), 2); //$NON-NLS-1$
			pm.subTask(EMPTY);
		
			result= Checks.checkMethodName(fMethodName);
			pm.worked(1);
			
			fAnalyzer.checkInput(result, fMethodName, fCUnit.getJavaProject());
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
		
		AbstractMethodDeclaration method= fAnalyzer.getEnclosingMethod();
		String sourceMethodName= new String(method.selector);
		
		CompilationUnitChange result= null;
		try {
			result= new CompilationUnitChange(
				RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.change_name", new String[]{fMethodName, sourceMethodName}),  //$NON-NLS-1$
				fCUnit);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		
		final int methodStart= method.declarationSourceStart;
		final int methodEnd= method.declarationSourceEnd;			
		final int insertPosition= methodEnd + 1;
		
		// Inserting the new method
		result.addTextEdit(RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.add_method", fMethodName), //$NON-NLS-1$
			new InsertNewMethod(insertPosition, methodStart, methodEnd));
		
		// Replacing the old statements with the new method call.
		result.addTextEdit(RefactoringCoreMessages.getFormattedString("ExtractMethodRefactoring.substitute_with_call", fMethodName), //$NON-NLS-1$
			new ReplaceCall(fSelectionStart, fSelectionLength, methodStart));
			
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
	 * @param methodName the method name used for the new method
	 * @return the signature of the extracted method
	 */
	public String getSignature(String methodName) {
		StringBuffer buffer= new StringBuffer(fVisibility);		
		if (fVisibility.length() > 0)
			buffer.append(BLANK);
			
		if ((fAnalyzer.getEnclosingMethod().modifiers & AstNode.AccStatic) != 0) {
			buffer.append(STATIC);
			buffer.append(BLANK);
		}
		
		TypeReference returnType= fAnalyzer.getReturnType();
		if (returnType != null) {
			buffer.append(returnType.toStringExpression(0));
			buffer.append(BLANK);
		}

		buffer.append(methodName);

		appendArguments(buffer);
		appendThrownExceptions(buffer);		
		return buffer.toString();
	}
	
	//---- Helper methods ------------------------------------------------------------------------
	
	private RefactoringStatus mergeTextSelectionStatus(RefactoringStatus status) {
		status.addFatalError(RefactoringCoreMessages.getString("ExtractMethodRefactoring.no_set_of_statements")); //$NON-NLS-1$
		return status;	
	}
	
	private String computeNewMethod(TextBuffer buffer, int lineNumber, String indent, String delimiter) {
		StringBuffer result= new StringBuffer();
		result.append(delimiter);
		if (insertNewLineAfterMethodBody(buffer, lineNumber))
			result.append(delimiter);
		result.append(indent);
		result.append(getSignature());
		result.append(" {"); //$NON-NLS-1$
		result.append(delimiter);
		result.append(computeSource(buffer, indent + '\t', delimiter));
		result.append(indent);
		result.append("}"); //$NON-NLS-1$
		return result.toString();
	}
	
	private boolean insertNewLineAfterMethodBody(TextBuffer buffer, int lineNumber) {
		String line= buffer.getLineContent(lineNumber + 1);
		if (line != null) {
			return TextUtilities.containsOnlyWhiteSpaces(line);
		}
		return true;
	}
	
	private String computeSource(TextBuffer buffer, String indent, String delimiter) {
		final String EMPTY_LINE= EMPTY;
		
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
		
		// Locals that are not passed as an arguments since the extracted method only
		// writes to them
		LocalVariableBinding[] methodLocals= fAnalyzer.getMethodLocals();
		for (int i= 0; i < methodLocals.length; i++) {
			if (methodLocals[i] != null)
				appendLocalDeclaration(result, indent, methodLocals[i], delimiter);
		}
		
		// We extract an expression
		boolean extractsExpression= fAnalyzer.extractsExpression();
		if (extractsExpression) {
			result.append(indent);
			if (fAnalyzer.getExpressionTypeBinding() != BaseTypeBinding.VoidBinding)
				result.append(RETURN_BLANK); //$NON-NLS-1$
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
					if (!extractsExpression)
						result.append(indent);
				}
				result.append(line);
			}
		}
		if (sourceNeedsSemicolon())
			result.append(SEMICOLON);
		result.append(delimiter);
		
		LocalVariableBinding returnValue= fAnalyzer.getReturnValue();
		if (returnValue != null) {
			result.append(indent);
			result.append(RETURN_BLANK);
			result.append(returnValue.readableName());
			result.append(SEMICOLON);
			result.append(delimiter);
		}
		return result.toString();
	}
	
	private String computeCall(TextBuffer buffer, String delimiter) {
		String[] lines= buffer.convertIntoLines(fSelectionStart, fSelectionLength);
		String firstLineIndent= TextUtilities.createIndentString(
			TextUtilities.getIndent(buffer.getLineContentOfOffset(fSelectionStart), fTabWidth));
		StringBuffer result= new StringBuffer(TextUtilities.createIndentString(TextUtilities.getIndent(lines[0], fTabWidth)));
		
		
		LocalVariableBinding[] locals= fAnalyzer.getCallerLocals();
		for (int i= 0; i < locals.length; i++) {
			appendLocalDeclaration(result, locals[i]);
			result.append(SEMICOLON);
			result.append(delimiter);
			result.append(firstLineIndent);
		}
				
		int returnKind= fAnalyzer.getReturnKind();
		switch (returnKind) {
			case ExtractMethodAnalyzer.ACCESS_TO_LOCAL:
				LocalVariableBinding binding= fAnalyzer.getReturnLocal();
				if (binding != null) {
					appendLocalDeclaration(result, binding);
					result.append(fAssignment);
				} else {
					binding= fAnalyzer.getReturnValue();
					result.append(binding.readableName());
					result.append(fAssignment);
				}
				break;
			case ExtractMethodAnalyzer.RETURN_STATEMENT_VALUE:
				// We return a value. So the code must look like "return extracted();"
				result.append(RETURN_BLANK);
				break;
		}
		
		LocalVariableBinding[] arguments= fAnalyzer.getArguments();
		result.append(fMethodName);
		result.append("("); //$NON-NLS-1$
		for (int i= 0; i < arguments.length; i++) {
			if (arguments[i] == null)
				continue;
			if (i > 0)
				result.append(COMMA_BLANK);
			result.append(arguments[i].readableName());
		}		
		result.append(")"); //$NON-NLS-1$
						
		if (callNeedsSemicolon())
			result.append(SEMICOLON);
			
		// We have a void return statement. The code looks like
		// extracted();
		// return;	
		if (returnKind == ExtractMethodAnalyzer.RETURN_STATEMENT_VOID) {
			result.append(delimiter);
			result.append(firstLineIndent);
			result.append(RETURN);
			result.append(SEMICOLON);
		}	
		if (endsSelectionWithLineDelimiter(lines))
			result.append(delimiter);
			
		return result.toString();
	}


	private boolean endsSelectionWithLineDelimiter(String[] lines) {
		return lines[lines.length - 1].length() == 0;
	}
	
	
	private boolean callNeedsSemicolon() {
		if (selectionIncludesSemicolon())
			return true;
	
		if (fAnalyzer.isExpressionSelected()) {
			return false;	
		} else {
			return !fAnalyzer.getNeedsSemicolon();				
		}		
	}
	
	private boolean sourceNeedsSemicolon() {
		return !callNeedsSemicolon();
	}	
	
	private boolean selectionIncludesSemicolon() {
		int start= fAnalyzer.getEndOfLastSelectedNode() + 1;
		int length= fSelectionLength - (start - fSelectionStart);
		
		if (length <= 0)
			return false;
		
		return fBuffer.indexOf(Scanner.TokenNameSEMICOLON, start, start + length - 1) != -1;
	}
	
	private void appendArguments(StringBuffer buffer) {
		buffer.append('(');
		LocalVariableBinding[] arguments= fAnalyzer.getArguments();
		for (int i= 0; i < arguments.length; i++) {
			LocalVariableBinding argument= arguments[i];
			if (argument == null)
				continue;
				
			if (i > 0)
				buffer.append(COMMA_BLANK);
			appendLocalDeclaration(buffer, argument);
		}
		buffer.append(')');
	}
	
	private void appendThrownExceptions(StringBuffer buffer) {
		buffer.append(fAnalyzer.fExceptionAnalyzer.getThrowSignature());
	}
	
	private void appendLocalDeclaration(StringBuffer buffer, LocalVariableBinding local) {
		LocalDeclaration declaration= local.declaration;
		buffer.append(declaration.modifiersString(declaration.modifiers));
		buffer.append(declaration.type.toStringExpression(0));
		buffer.append(BLANK);
		buffer.append(local.readableName());
	}
	
	private void appendLocalDeclaration(StringBuffer buffer, String indent, LocalVariableBinding local, String delimiter) {
		buffer.append(indent);
		appendLocalDeclaration(buffer, local);
		buffer.append(SEMICOLON);
		buffer.append(delimiter);
	}	
}