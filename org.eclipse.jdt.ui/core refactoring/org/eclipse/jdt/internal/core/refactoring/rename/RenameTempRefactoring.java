/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.SourceRange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.util.AST;
import org.eclipse.jdt.internal.core.refactoring.util.SelectionAnalyzer;

public class RenameTempRefactoring extends Refactoring implements IRenameRefactoring, IReferenceUpdatingRefactoring{
	
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	private boolean fUpdateReferences;
	private int fSelectionStart;
	private int fSelectionLength;
	private int fSelectionEnd;
	private ICompilationUnit fCu;
	private String fCurrentName;
	private String fNewName;
	private LocalDeclaration fTempDeclaration;
	private AST fAST;
	private Map fAlreadyUsedNames;//String -> ISourceRange
	
	public RenameTempRefactoring(ICompilationUnit cu, ITextBufferChangeCreator changeCreator, int selectionStart, int selectionLength) {
		Assert.isNotNull(changeCreator);
		fTextBufferChangeCreator= changeCreator;
		fUpdateReferences= true;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fSelectionEnd= fSelectionStart + fSelectionLength - 1;
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
		fUpdateReferences = updateReferences;
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

	/*
	 * @see IRenameRefactoring#checkNewName()
	 */
	public RefactoringStatus checkNewName() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkFieldName(fNewName));
		if (! Checks.startsWithLowerCase(fNewName))
			result.addWarning("By convention, all names of  local variables start with lowercase letters.");
		if (fAlreadyUsedNames.containsKey(fNewName))
			result.addError("Name '" + fNewName + "' is already used.", Refactoring.getResource(fCu), (ISourceRange)fAlreadyUsedNames.get(fNewName));
		return result;		
	}

	//--- preconditions 
		
	/* non java-doc
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm)	throws JavaModelException {
		
		if (fSelectionStart < 0 || fSelectionLength == 0)
			return RefactoringStatus.createFatalErrorStatus("A local variable declaration or reference must be selected to activate this refactoring");
		
		if (!fCu.isStructureKnown())
			return RefactoringStatus.createFatalErrorStatus("Syntax errors");
			
		if (!(fCu instanceof CompilationUnit))
			return RefactoringStatus.createFatalErrorStatus("Internal Error");
	
		return checkSelection();	
	}
	
	private RefactoringStatus checkSelection()throws JavaModelException {
		SelectionAnalyzer analyzer= createSelectionValidator();
		fAST= new AST(fCu);
		fAST.accept(analyzer.getParentTracker());
		
		RefactoringStatus errorStatus= RefactoringStatus.createFatalErrorStatus("A local variable declaration or reference must be selected to activate this refactoring");
		AstNode[] selected= analyzer.getSelectedNodes();
		if (selected == null || selected.length != 1){
			return errorStatus;
		} else if (selected[0] instanceof LocalDeclaration && (!(selected[0] instanceof Argument))){
			initializeTempDeclaration((LocalDeclaration)selected[0]);
			return new RefactoringStatus();
		} else if (selected[0] instanceof NameReference){
			NameReference reference= (NameReference)selected[0];
			if (reference.binding instanceof LocalVariableBinding){
				LocalVariableBinding localBinding= (LocalVariableBinding)reference.binding;
				if (! localBinding.isArgument){
					initializeTempDeclaration(localBinding.declaration);
					return new RefactoringStatus();
				}	
			}
		}
		return errorStatus;		
	}
	
	private void initializeTempDeclaration(LocalDeclaration localDeclaration){
		fTempDeclaration= localDeclaration;
		fAlreadyUsedNames= getLocalNames(fTempDeclaration);
		fCurrentName= fTempDeclaration.name();		
	}
	
	private static Map getLocalNames(LocalDeclaration localDeclaration){
		return getLocalNames(localDeclaration.binding.declaringScope);
	}
	
	private static Map getLocalNames(BlockScope scope){
		if (scope.locals == null)
			return new HashMap(0);
		Map result= new HashMap();
		LocalVariableBinding[] locals= scope.locals;
		for (int i= 0; i< locals.length; i++){
			if (locals[i] == null)
				continue;
			if (locals[i].declaration == null)	
				continue;
			int offset= locals[i].declaration.sourceStart();
			int length= locals[i].declaration.sourceEnd() - locals[i].declaration.sourceStart() + 1; 
			result.put(new String(locals[i].name), new SourceRange(offset, length));
		}	
		
		if (scope.subscopes == null)
			return result;	
		
		for (int i= 0; i < scope.subscopes.length; i++){
			Scope subScope= scope.subscopes[i];
			if (subScope instanceof BlockScope)
				result.putAll(getLocalNames((BlockScope)subScope));
		}	
		return result;	
	}
	
	private SelectionAnalyzer createSelectionValidator() 	throws JavaModelException {
		return new SelectionAnalyzer(fCu.getBuffer(), fSelectionStart, fSelectionLength);
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm)	throws JavaModelException {
		try{
			pm.beginTask("", 1);
		
			if (Arrays.asList(getUnsavedFiles()).contains(Refactoring.getResource(fCu)))
					return RefactoringStatus.createFatalErrorStatus("Compilation unit '" + fCu.getElementName() + "'  must be saved before this refactoring.");
			
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkNewName());
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
			ITextBufferChange builder= fTextBufferChangeCreator.create("Rename local variables", fCu);
			Integer[] renamingOffsets= getOccurrenceOffsets();
			pm.worked(1);
			
			IProgressMonitor subPm= new SubProgressMonitor(pm, 1);
			subPm.beginTask("", renamingOffsets.length);
			Assert.isTrue(renamingOffsets.length > 0); //should be enforced by preconditions
			String changeName= "Rename local variable:'" + fCurrentName + "' to: '" + fNewName + "'.";
			int length= fCurrentName.length();
			for(int i= 0; i < renamingOffsets.length; i++){
				int offset= renamingOffsets[i].intValue();
				builder.addReplace(changeName, offset, length, fNewName);
			}
			return builder;
		} finally{
			pm.done();
		}	
	}
	
	private Integer[] getOccurrenceOffsets() throws JavaModelException{
		RenameTempOccurrenceOffsetFinder offsetFinder= new RenameTempOccurrenceOffsetFinder(fTempDeclaration, fUpdateReferences);
		fAST.accept(offsetFinder);
		return offsetFinder.getOccurrenceOffsets();
	}
}