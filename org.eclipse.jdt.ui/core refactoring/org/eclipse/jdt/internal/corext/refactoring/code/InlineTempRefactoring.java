/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
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
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempDeclarationFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceFinder;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;


public class InlineTempRefactoring extends Refactoring {

	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	
	//the following fields are set after the construction
	private VariableDeclaration fTempDeclaration;
	private CompilationUnit fCompilationUnitNode;

	private InlineTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
	}
	
	public static boolean isAvailable(ILocalVariable variable) throws JavaModelException {
		// work around for https://bugs.eclipse.org/bugs/show_bug.cgi?id=48420
		return Checks.isAvailable(variable);
	}
	
	public static InlineTempRefactoring create(ICompilationUnit unit, int selectionStart, int selectionLength) {
		InlineTempRefactoring ref= new InlineTempRefactoring(unit, selectionStart, selectionLength);
		if (ref.checkIfTempSelected().hasFatalError())
			return null;
		return ref;
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("InlineTempRefactoring.name"); //$NON-NLS-1$
	}
	
	public String getTempName(){
		return fTempDeclaration.getName().getIdentifier();
	}
	
	public int getOccurences() {
		try {
			return getOccurrenceOffsets().length;
		} catch (JavaModelException e) {
			return 1;
		}
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			
			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu}));
			if (result.hasFatalError())
				return result;
				
			if (! fCu.isStructureKnown())		
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTempRefactoring.syntax_errors")); //$NON-NLS-1$
								
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
    			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTemRefactoring.error.message.nulLiteralsCannotBeInlined")); //$NON-NLS-1$
    		case ASTNode.ARRAY_INITIALIZER:
    			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTempRefactoring.Array_vars_initialized")); 	 //$NON-NLS-1$
    		default:	
		        return null;
    	}
    }

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			return new RefactoringStatus();
		} finally {
			pm.done();
		}	
	}

	private void initializeAST() {
		fCompilationUnitNode= AST.parseCompilationUnit(fCu, true);
	}
	
	private RefactoringStatus checkIfTempSelected(){
		initializeAST();

		fTempDeclaration= TempDeclarationFinder.findTempDeclaration(fCompilationUnitNode, fSelectionStart, fSelectionLength);

		if (fTempDeclaration == null){
			String message= RefactoringCoreMessages.getString("InlineTempRefactoring.select_temp");//$NON-NLS-1$
			return CodeRefactoringUtil.checkMethodSyntaxErrors(fSelectionStart, fSelectionLength, fCompilationUnitNode, message);
		}

		if (fTempDeclaration.getParent() instanceof FieldDeclaration)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTemRefactoring.error.message.fieldsCannotBeInlined")); //$NON-NLS-1$
		
		return new RefactoringStatus();
	}
	
	private RefactoringStatus checkSelection() throws JavaModelException {
		RefactoringStatus rs= checkIfTempSelected();
		if (rs != null && rs.hasFatalError())
			return rs;

		if (fTempDeclaration.getParent() instanceof MethodDeclaration)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTempRefactoring.method_parameter")); //$NON-NLS-1$
		
		if (fTempDeclaration.getParent() instanceof CatchClause)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTempRefactoring.exceptions_declared")); //$NON-NLS-1$
		
		if (ASTNodes.getParent(fTempDeclaration, ASTNode.FOR_STATEMENT) != null){
			ForStatement forStmt= (ForStatement)ASTNodes.getParent(fTempDeclaration, ASTNode.FOR_STATEMENT);
			for (Iterator iter= forStmt.initializers().iterator(); iter.hasNext();) {
				if (ASTNodes.isParent(fTempDeclaration, (Expression) iter.next()))
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("InlineTempRefactoring.for_initializers")); //$NON-NLS-1$
			}
		}
		
		if (fTempDeclaration.getInitializer() == null){
			String message= RefactoringCoreMessages.getFormattedString("InlineTempRefactoring.not_initialized", getTempName());//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
				
		return checkAssignments();
	}
	
	private RefactoringStatus checkAssignments() throws JavaModelException {
		TempAssignmentFinder assignmentFinder= new TempAssignmentFinder(fTempDeclaration);
		fCompilationUnitNode.accept(assignmentFinder);
		if (! assignmentFinder.hasAssignments())
			return new RefactoringStatus();
		int start= assignmentFinder.getFirstAssignment().getStartPosition();
		int length= assignmentFinder.getFirstAssignment().getLength();
		ISourceRange range= new SourceRange(start, length);
		RefactoringStatusContext context= JavaStatusContext.create(fCu, range);	
		String message= RefactoringCoreMessages.getFormattedString("InlineTempRefactoring.assigned_more_once", getTempName());//$NON-NLS-1$
		return RefactoringStatus.createFatalErrorStatus(message, context);
	}
	
	//----- changes
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("InlineTempRefactoring.preview"), 2); //$NON-NLS-1$
			CompilationUnitChange change= new CompilationUnitChange(RefactoringCoreMessages.getString("InlineTempRefactoring.inline"), fCu); //$NON-NLS-1$
			inlineTemp(change, new SubProgressMonitor(pm, 1));
			removeTemp(change);
			return change;
		} finally {
			pm.done();	
		}	
	}

	private void inlineTemp(TextChange change, IProgressMonitor pm) throws JavaModelException {
		Integer[] offsets= getOccurrenceOffsets();
		pm.beginTask("", offsets.length); //$NON-NLS-1$
		String changeName= RefactoringCoreMessages.getString("InlineTempRefactoring.inline_edit_name") + getTempName(); //$NON-NLS-1$
		int length= getTempName().length();
		for(int i= 0; i < offsets.length; i++){
			int offset= offsets[i].intValue();
            String sourceToInline= getInitializerSource(needsBrackets(offset));
			change.addTextEdit(changeName, new ReplaceEdit(offset, length, sourceToInline));
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
    	SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(offset, getTempName().length()), true);
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
		//FIX ME - multi declarations
		
		if (fTempDeclaration.getParent() instanceof VariableDeclarationStatement){
			VariableDeclarationStatement vds= (VariableDeclarationStatement)fTempDeclaration.getParent();
			if (vds.fragments().size() == 1){
				removeDeclaration(change, vds.getStartPosition(), vds.getLength());
				return;
			} else {
				//FIX ME
				return;
			}
		}
		
		removeDeclaration(change, fTempDeclaration.getStartPosition(), fTempDeclaration.getLength());
	}
	
	private void removeDeclaration(TextChange change, int offset, int length)  throws JavaModelException {
		ISourceRange range= SourceRangeComputer.computeSourceRange(new SourceRange(offset, length), fCu.getSource());
		String changeName= RefactoringCoreMessages.getString("InlineTempRefactoring.remove_edit_name") + getTempName();  //$NON-NLS-1$
		change.addTextEdit(changeName, new DeleteEdit(range.getOffset(), range.getLength()));
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
	
	private Integer[] getOccurrenceOffsets() throws JavaModelException {
		return TempOccurrenceFinder.findTempOccurrenceOffsets(fTempDeclaration, true, false);
	}	
	
	}
