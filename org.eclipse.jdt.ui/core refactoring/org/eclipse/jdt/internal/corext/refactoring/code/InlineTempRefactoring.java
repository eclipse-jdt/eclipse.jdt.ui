/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.Iterator;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempDeclarationFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.Messages;


public class InlineTempRefactoring extends Refactoring {

	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	
	//the following fields are set after the construction
	private VariableDeclaration fTempDeclaration;
	private CompilationUnit fCompilationUnitNode;
	private int[] fReferenceOffsets;

	private InlineTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
	}
	
	public static InlineTempRefactoring create(ICompilationUnit unit, CompilationUnit node, int selectionStart, int selectionLength) {
		InlineTempRefactoring ref= new InlineTempRefactoring(unit, selectionStart, selectionLength);
		if (ref.checkIfTempSelected(node).hasFatalError())
			return null;
		return ref;
	}
	
	private RefactoringStatus checkIfTempSelected(CompilationUnit node){
		fCompilationUnitNode= node;

		fTempDeclaration= TempDeclarationFinder.findTempDeclaration(fCompilationUnitNode, fSelectionStart, fSelectionLength);

		if (fTempDeclaration == null){
			String message= RefactoringCoreMessages.InlineTempRefactoring_select_temp;
			return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCompilationUnitNode, message);
		}

		if (fTempDeclaration.getParent() instanceof FieldDeclaration)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTemRefactoring_error_message_fieldsCannotBeInlined); 
		
		return new RefactoringStatus();
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.InlineTempRefactoring_name; 
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			
			RefactoringStatus result= Checks.validateModifiesFiles(
				ResourceUtil.getFiles(new ICompilationUnit[]{fCu}),
				getValidationContext());
			if (result.hasFatalError())
				return result;
				
			if (! fCu.isStructureKnown())		
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_syntax_errors); 
								
			result.merge(checkSelection());
			if (result.hasFatalError())
				return result;
			
			result.merge(checkInitializer());	
			return result;
		} finally {
			pm.done();
		}	
	}

    private RefactoringStatus checkInitializer() {
    	switch(fTempDeclaration.getInitializer().getNodeType()){
    		case ASTNode.NULL_LITERAL:
    			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTemRefactoring_error_message_nulLiteralsCannotBeInlined); 
    		case ASTNode.ARRAY_INITIALIZER:
    			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_Array_vars_initialized); 	 
    		default:	
		        return null;
    	}
    }

	private RefactoringStatus checkSelection() {
		if (fTempDeclaration.getParent() instanceof MethodDeclaration)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_method_parameter); 
		
		if (fTempDeclaration.getParent() instanceof CatchClause)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_exceptions_declared); 
		
		if (ASTNodes.getParent(fTempDeclaration, ASTNode.FOR_STATEMENT) != null){
			ForStatement forStmt= (ForStatement)ASTNodes.getParent(fTempDeclaration, ASTNode.FOR_STATEMENT);
			for (Iterator iter= forStmt.initializers().iterator(); iter.hasNext();) {
				if (ASTNodes.isParent(fTempDeclaration, (Expression) iter.next()))
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InlineTempRefactoring_for_initializers); 
			}
		}
		
		if (fTempDeclaration.getInitializer() == null){
			String message= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_not_initialized, fTempDeclaration.getName().getIdentifier());
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
				
		return checkAssignments();
	}
	
	private RefactoringStatus checkAssignments() {
		TempAssignmentFinder assignmentFinder= new TempAssignmentFinder(fTempDeclaration);
		fCompilationUnitNode.accept(assignmentFinder);
		if (! assignmentFinder.hasAssignments())
			return new RefactoringStatus();
		int start= assignmentFinder.getFirstAssignment().getStartPosition();
		int length= assignmentFinder.getFirstAssignment().getLength();
		ISourceRange range= new SourceRange(start, length);
		RefactoringStatusContext context= JavaStatusContext.create(fCu, range);	
		String message= Messages.format(RefactoringCoreMessages.InlineTempRefactoring_assigned_more_once, fTempDeclaration.getName().getIdentifier());
		return RefactoringStatus.createFatalErrorStatus(message, context);
	}
	
	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			return new RefactoringStatus();
		} finally {
			pm.done();
		}	
	}
	
	//----- changes
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.InlineTempRefactoring_preview, 2); 
			final CompilationUnitChange result= new CompilationUnitChange(RefactoringCoreMessages.InlineTempRefactoring_inline, fCu); 
			inlineTemp(result, new SubProgressMonitor(pm, 1));
			removeTemp(result);
			return result;
		} finally {
			pm.done();	
		}	
	}

	private void inlineTemp(TextChange change, IProgressMonitor pm) throws JavaModelException {
		int[] offsets= getReferenceOffsets();
		pm.beginTask("", offsets.length); //$NON-NLS-1$
		String changeName= RefactoringCoreMessages.InlineTempRefactoring_inline_edit_name; 
		int length= fTempDeclaration.getName().getIdentifier().length();
		for(int i= 0; i < offsets.length; i++){
			int offset= offsets[i];
            String sourceToInline= getInitializerSource(needsBrackets(offset));
			TextChangeCompatibility.addTextEdit(change, changeName, new ReplaceEdit(offset, length, sourceToInline));
			pm.worked(1);	
		}
	}
	
    private boolean needsBrackets(int offset) {
		Expression initializer= fTempDeclaration.getInitializer();

		if (initializer instanceof Assignment)//for esthetic reasons
			return true;
		    	
		SimpleName inlineSite= getReferenceAtOffset(offset);
    	if (inlineSite == null)
    		return true;
    		
    	return ASTNodes.substituteMustBeParenthesized(initializer, inlineSite);
    }

	private SimpleName getReferenceAtOffset(int offset) {
    	SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(offset, fTempDeclaration.getName().getIdentifier().length()), true);
    	fCompilationUnitNode.accept(analyzer);
    	ASTNode reference= analyzer.getFirstSelectedNode();
    	if(!isReference(reference))
    		return null;
    	return (SimpleName) reference;			
	}
	
	private boolean isReference(ASTNode node) {
		if(!(node instanceof SimpleName))
			return false;
		if(!((SimpleName) node).getIdentifier().equals(fTempDeclaration.getName().getIdentifier()))
			return false;
		return true;
	}

	private void removeTemp(TextChange change) throws JavaModelException {
		//TODO: FIX ME - multi declarations
		
		if (fTempDeclaration.getParent() instanceof VariableDeclarationStatement){
			VariableDeclarationStatement vds= (VariableDeclarationStatement)fTempDeclaration.getParent();
			if (vds.fragments().size() == 1){
				removeDeclaration(change, vds.getStartPosition(), vds.getLength());
				return;
			} else {
				//TODO: FIX ME
				return;
			}
		}
		
		removeDeclaration(change, fTempDeclaration.getStartPosition(), fTempDeclaration.getLength());
	}
	
	private void removeDeclaration(TextChange change, int offset, int length)  throws JavaModelException {
		ISourceRange range= SourceRangeComputer.computeSourceRange(new SourceRange(offset, length), fCu.getSource());
		String changeName= RefactoringCoreMessages.InlineTempRefactoring_remove_edit_name; 
		TextChangeCompatibility.addTextEdit(change, changeName, new DeleteEdit(range.getOffset(), range.getLength()));
	}
	
	private String getInitializerSource(boolean brackets) throws JavaModelException{
		if (brackets)
			return '(' + getRawInitializerSource() + ')'; 
		else
			return getRawInitializerSource(); 
	}
	
	private String getRawInitializerSource() throws JavaModelException{
		int start= fTempDeclaration.getInitializer().getStartPosition();
		int length= fTempDeclaration.getInitializer().getLength();
		int end= start + length;
		return fCu.getSource().substring(start, end);
	}

	public int[] getReferenceOffsets() {
		if (fReferenceOffsets == null) {
			TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(fTempDeclaration, false);
			analyzer.perform();
			fReferenceOffsets= analyzer.getReferenceOffsets();
		}
		return fReferenceOffsets;
	}

	public VariableDeclaration getTempDeclaration() {
		return fTempDeclaration;
	}
}
