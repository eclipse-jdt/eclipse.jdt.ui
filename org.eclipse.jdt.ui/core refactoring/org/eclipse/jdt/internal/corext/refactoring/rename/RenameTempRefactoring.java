/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.code.TempNameUtil;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;

public class RenameTempRefactoring extends Refactoring implements IRenameRefactoring, IReferenceUpdatingRefactoring{
	
	private final int fSelectionStart;
	private final int fSelectionLength;
	private final ICompilationUnit fCu;
	
	//the following fields are set or modified after the construction
	private boolean fUpdateReferences;
	private String fCurrentName;
	private String fNewName;
	private LocalDeclaration fTempDeclaration;
	private AST fAST;
	private Map fAlreadyUsedNames;//String -> ISourceRange
	
	public RenameTempRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fUpdateReferences= true;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= cu;
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Rename local variable";
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
	public RefactoringStatus checkActivation(IProgressMonitor pm)	throws JavaModelException {
		
		if (fSelectionStart < 0)
			return RefactoringStatus.createFatalErrorStatus("A local variable declaration or reference must be selected to activate this refactoring");
		
		if (!fCu.isStructureKnown())
			return RefactoringStatus.createFatalErrorStatus("Syntax errors");
			
		if (!(fCu instanceof CompilationUnit))
			return RefactoringStatus.createFatalErrorStatus("Internal Error");

		initializeAst();
	
		return checkSelection();	
	}

	/*
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName(String newName) throws JavaModelException {
		RefactoringStatus result= Checks.checkFieldName(newName);
		if (! Checks.startsWithLowerCase(newName))
			result.addWarning("By convention, all names of local variables start with lowercase letters.");
		if (fAlreadyUsedNames.containsKey(newName))
			result.addError("Name '" + newName + "' is already used.", JavaSourceContext.create(fCu, (ISourceRange)fAlreadyUsedNames.get(newName)));
		return result;		
	}
	
	private RefactoringStatus checkSelection() throws JavaModelException {
				
		if (fAST.hasProblems()){
			RefactoringStatus compileErrors= Checks.checkCompileErrors(fAST, fCu);
			if (compileErrors.hasFatalError())
				return compileErrors;
		}		
		
		LocalDeclaration local= TempDeclarationFinder.findTempDeclaration(fAST, fCu, fSelectionStart, fSelectionLength);
		if (local == null)
			return RefactoringStatus.createFatalErrorStatus("A local variable declaration or reference must be selected to activate this refactoring");

		initializeTempDeclaration(local);
		return new RefactoringStatus();
	}

	private void initializeAst() throws JavaModelException {
		fAST= new AST(fCu);
	}
	
	private void initializeTempDeclaration(LocalDeclaration localDeclaration){
		fTempDeclaration= localDeclaration;
		fAlreadyUsedNames= TempNameUtil.getLocalNameMap(fTempDeclaration.binding.declaringScope);
		fCurrentName= fTempDeclaration.name();		
	}
			
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm)	throws JavaModelException {
		try{
			pm.beginTask("", 1);	
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkNewName(fNewName));
			result.merge(analyzeAST());
			return result;
		} finally{
			pm.done();
		}	
	}
		
	private RefactoringStatus analyzeAST() throws JavaModelException{		
		RenameTempASTAnalyzer analyzer= new RenameTempASTAnalyzer(fTempDeclaration, fNewName, fUpdateReferences);
		analyzer.setCU(fCu);
		fAST.accept(analyzer);
		return analyzer.getStatus();
	}
	
	//--- changes 
	
	/* non java-doc
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2);
			Integer[] renamingOffsets= getOccurrenceOffsets();
			pm.worked(1);
			
			TextChange change= new CompilationUnitChange("Rename Local Variable", fCu);
			IProgressMonitor subPm= new SubProgressMonitor(pm, 1);
			subPm.beginTask("", renamingOffsets.length);
			Assert.isTrue(renamingOffsets.length > 0); //should be enforced by preconditions
			String changeName= "Rename local variable:'" + fCurrentName + "' to: '" + fNewName + "'.";
			int length= fCurrentName.length();
			for(int i= 0; i < renamingOffsets.length; i++){
				int offset= renamingOffsets[i].intValue();
				change.addTextEdit(changeName, SimpleTextEdit.createReplace(offset, length, fNewName));
			}
			
			return change;
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}	
	}
	
	private Integer[] getOccurrenceOffsets() throws JavaModelException{
		TempOccurrenceFinder offsetFinder= new TempOccurrenceFinder(fTempDeclaration, fUpdateReferences, true);
		fAST.accept(offsetFinder);
		return offsetFinder.getOccurrenceOffsets();
	}
}