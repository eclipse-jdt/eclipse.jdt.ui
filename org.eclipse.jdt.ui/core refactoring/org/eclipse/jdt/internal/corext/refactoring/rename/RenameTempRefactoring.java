/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextBufferChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

public class RenameTempRefactoring extends Refactoring implements IRenameRefactoring, IReferenceUpdatingRefactoring{
	
	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	
	//the following fields are set or modified after the construction
	private boolean fUpdateReferences;
	private String fCurrentName;
	private String fNewName;
	private CompilationUnit fCompilationUnitNode;
	private VariableDeclaration fTempDeclarationNode;
	
	public RenameTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fUpdateReferences= true;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
		fNewName= "";//the only thing we can set //$NON-NLS-1$
	}
	
	public Object getNewElement(){
		return null; //?????
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("RenameTempRefactoring.rename"); //$NON-NLS-1$
	}

	/*
	 * @see IReferenceUpdatingRefactoring#canEnableUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		return true;
	}

	/*
	 * @see IReferenceUpdatingRefactoring#getUpdateReferences()
	 */
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}

	/*
	 * @see IReferenceUpdatingRefactoring#setUpdateReferences()
	 */
	public void setUpdateReferences(boolean updateReferences) {
		fUpdateReferences= updateReferences;
	}
	
	/*
	 * @see IRenameRefactoring#setNewName
	 */
	public void setNewName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}

	/*
	 * @see IRenameRefactoring#getNewName()
	 */
	public String getNewName() {
		return fNewName;
	}

	/*
	 * @see IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return fCurrentName;
	}

	//--- preconditions 
		
	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		initAST();
		if (fTempDeclarationNode == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameTempRefactoring.must_select_local")); //$NON-NLS-1$
		if (ASTNodes.getParent(fTempDeclarationNode, MethodDeclaration.class) == null)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenameTempRefactoring.only_in_methods")); //$NON-NLS-1$
			
		initNames();			
		return new RefactoringStatus();
	}
	
	private void initAST(){
		fCompilationUnitNode= AST.parseCompilationUnit(fCu, true);
		fTempDeclarationNode= TempDeclarationFinder.findTempDeclaration(fCompilationUnitNode, fSelectionStart, fSelectionLength);
	}
	
	private void initNames(){
		fCurrentName= fTempDeclarationNode.getName().getIdentifier();
	}
	
	/*
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName(String newName) throws JavaModelException {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (! Checks.startsWithLowerCase(newName))
			result.addWarning(RefactoringCoreMessages.getString("RenameTempRefactoring.lowercase")); //$NON-NLS-1$
		return result;		
	}
		
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm)	throws JavaModelException {
		try{
			pm.beginTask("", 1);	 //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();			
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu})));
			if (result.hasFatalError())
				return result;
			
			result.merge(checkNewName(fNewName));
			if (result.hasFatalError())
				return result;
			result.merge(analyzeAST());
			return result;
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}	
	}
		
	private RefactoringStatus analyzeAST() throws JavaModelException{
		ICompilationUnit wc= null;
		try {
			RefactoringStatus result= new RefactoringStatus();
						
			TextEdit[] edits= getAllRenameEdits();
			TextChange change= new TextBufferChange(RefactoringCoreMessages.getString("RenameTempRefactoring.rename"), TextBuffer.create(fCu.getSource())); //$NON-NLS-1$
			change.setTrackPositionChanges(true);
		
			wc= RefactoringAnalyzeUtil.getWorkingCopyWithNewContent(edits, change, fCu);
			CompilationUnit newCUNode= AST.parseCompilationUnit(wc, true);
			
			result.merge(RefactoringAnalyzeUtil.analyzeIntroducedCompileErrors(change, wc, newCUNode, fCompilationUnitNode));
			if (result.hasError())
				return result;
			
			String fullKey= RefactoringAnalyzeUtil.getFullDeclarationBindingKey(edits, fCompilationUnitNode);	
			MethodDeclaration methodDeclaration= RefactoringAnalyzeUtil.getMethodDeclaration(RefactoringAnalyzeUtil.getFirstEdit(edits), change, newCUNode);
			SimpleName[] problemNodes= ProblemNodeFinder.getProblemNodes(methodDeclaration, edits, change, fullKey);
			result.merge(RefactoringAnalyzeUtil.reportProblemNodes(wc, problemNodes));
			return result;
		} catch(CoreException e) {
			throw new JavaModelException(e);
		} finally{
			if (wc != null)
				wc.destroy();
		}
	}

	private TextEdit[] getAllRenameEdits() throws JavaModelException {
		Integer[] renamingOffsets= TempOccurrenceFinder.findTempOccurrenceOffsets(fTempDeclarationNode, fUpdateReferences, true);
		Assert.isTrue(renamingOffsets.length > 0); //should be enforced by preconditions
		TextEdit[] result= new TextEdit[renamingOffsets.length];
		int length= fCurrentName.length();
		for(int i= 0; i < renamingOffsets.length; i++){
			int offset= renamingOffsets[i].intValue();
			result[i]= SimpleTextEdit.createReplace(offset, length, fNewName);
		}
		return result;
	}
	
	//--- changes 
	
	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			pm.worked(1);
			
			TextChange change= new CompilationUnitChange(RefactoringCoreMessages.getString("RenameTempRefactoring.rename"), fCu); //$NON-NLS-1$
			
			String changeName= RefactoringCoreMessages.getFormattedString("RenameTempRefactoring.changeName", //$NON-NLS-1$
						new String[]{fCurrentName, fNewName});
			TextEdit[] edits= getAllRenameEdits();
			for (int i= 0; i < edits.length; i++) {
				change.addTextEdit(changeName, edits[i]);
			}
			
			return change;
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}	
	}

}