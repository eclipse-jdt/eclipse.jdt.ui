/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;
import org.eclipse.jdt.internal.core.refactoring.util.TextBufferChangeManager;

public class RenameFieldRefactoring extends Refactoring implements IRenameRefactoring{
	
	private String fNewName;
	private IField fField;
	
	private SearchResultGroup[] fOccurrences;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	private boolean fUpdateReferences;

	public RenameFieldRefactoring(ITextBufferChangeCreator changeCreator, IField field){
		Assert.isNotNull(changeCreator);
		Assert.isTrue(field.exists());
		fTextBufferChangeCreator= changeCreator;
		fField= field;
		fUpdateReferences= true;
	}
	
	/* non java-doc
	 * @see Refactoring#checkPreconditions(IProgressMonitor)
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= checkPreactivation();
		if (result.hasFatalError())
			return result;
		result.merge(super.checkPreconditions(pm));
		return result;
	}
		
	/* non java-doc
	 * IRenameRefactoring#setNewName
	 */
	public final void setNewName(String newName){
		Assert.isNotNull(newName);
		fNewName= newName;
	}
	
	/* non java-doc
	 * IRenameRefactoring#getCurrentName
	 */
	public final String getCurrentName(){
		return fField.getElementName();
	}
	
	/* non java-doc
	 * IRenameRefactoring#getNewName
	 */	
	public final String getNewName(){
		return fNewName;
	}
	
	/* non java-doc
	 * IRefactoring#getName
	 */
	 public String getName(){
	 	return RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.name", //$NON-NLS-1$
	 													 new String[]{fField.getElementName(), getNewName()});
	 }
	
	/* non java-doc
	 * @see IRenameRefactoring#canUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		return true;
	}

	/* non java-doc
	 * @see IRenameRefactoring#setUpdateReferences(boolean)
	 */
	public void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getUpdateReferences()
	 */	
	public boolean getUpdateReferences(){
		return fUpdateReferences;
	}
		
	// -------------- Preconditions -----------------------
	
	/* non java-doc
	 * IPreactivatedRefactoring#checkPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException {
		return checkAvailability(fField);	
	}

	/* non java-doc
	 * Refactoring#checkActivation
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		return Checks.checkIfCuBroken(fField);
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#checkNewName
	 */
	public RefactoringStatus checkNewName() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		
		result.merge(Checks.checkFieldName(getNewName()));
		
		if (isInstaceField(fField) && (! Checks.startsWithLowerCase(getNewName())))
			result.addWarning(RefactoringCoreMessages.getString("RenameFieldRefactoring.should_start_lowercase")); //$NON-NLS-1$
			
		if (Checks.isAlreadyNamed(fField, getNewName()))
			result.addFatalError(RefactoringCoreMessages.getString("RenameFieldRefactoring.another_name")); //$NON-NLS-1$
		if (fField.getDeclaringType().getField(getNewName()).exists())
			result.addError(RefactoringCoreMessages.getString("RenameFieldRefactoring.field_already_defined")); //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc
	 * Refactoring#checkInput
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 9); //$NON-NLS-1$
			pm.subTask(RefactoringCoreMessages.getString("RenameFieldRefactoring.checking")); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(Checks.checkIfCuBroken(fField));
			if (result.hasFatalError())
				return result;
			result.merge(checkNewName());
			pm.worked(1);
			result.merge(checkEnclosingHierarchy());
			pm.worked(1);
			result.merge(checkNestedHierarchy(fField.getDeclaringType()));
			pm.worked(1);
			
			if (! fUpdateReferences)
				return result;
				
			result.merge(Checks.checkAffectedResourcesAvailability(getOccurrences(new SubProgressMonitor(pm, 3))));
			result.merge(analyzeAffectedCompilationUnits(new SubProgressMonitor(pm, 3)));
			return result;
		} finally{
			pm.done();
		}
	}
	
	private static boolean isInstaceField(IField field) throws JavaModelException{
		if (field.getDeclaringType().isInterface())
			return false;
		else 
			return ! Flags.isStatic(field.getFlags());
	}
	
	private RefactoringStatus checkNestedHierarchy(IType type) throws JavaModelException {
		IType[] nestedTypes= type.getTypes();
		if (nestedTypes == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();	
		for (int i= 0; i < nestedTypes.length; i++){
			IField otherField= nestedTypes[i].getField(getNewName());
			if (otherField.exists())
				result.addWarning(RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.hiding", //$NON-NLS-1$
																			new String[]{fField.getElementName(), getNewName(), nestedTypes[i].getFullyQualifiedName()}));
			result.merge(checkNestedHierarchy(nestedTypes[i]));	
		}	
		return result;
	}
	
	private RefactoringStatus checkEnclosingHierarchy() throws JavaModelException {
		IType current= fField.getDeclaringType();
		if (Checks.isTopLevel(current))
			return null;
		RefactoringStatus result= new RefactoringStatus();
		while (current != null){
			IField otherField= current.getField(getNewName());
			if (otherField.exists())
				result.addWarning(RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.hiding2", //$NON-NLS-1$
				 															new String[]{getNewName(), current.getFullyQualifiedName(), fField.getElementName()}));
			current= current.getDeclaringType();
		}
		return result;
	}
	
	/*
	 * (non java-doc)
	 * Analyzes all compilation units in which type is referenced
	 */
	private RefactoringStatus analyzeAffectedCompilationUnits(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		fOccurrences= Checks.excludeCompilationUnits(fOccurrences, getUnsavedFiles(), result);
		if (result.hasFatalError())
			return result;
		
		result.merge(Checks.checkCompileErrorsInAffectedFiles(fOccurrences));	
			
		pm.beginTask("", fOccurrences.length); //$NON-NLS-1$
		RenameFieldASTAnalyzer analyzer= new RenameFieldASTAnalyzer(fNewName, fField);
		for (int i= 0; i < fOccurrences.length; i++){
			if (pm.isCanceled())
				throw new OperationCanceledException();
			
			analyzeCompilationUnit(pm, analyzer, fOccurrences[i], result);
		}
		return result;
	}
	
	private void analyzeCompilationUnit(IProgressMonitor pm, RenameFieldASTAnalyzer analyzer, SearchResultGroup searchResults, RefactoringStatus result)  throws JavaModelException {
		CompilationUnit cu= (CompilationUnit) (JavaCore.create(searchResults.getResource()));
		pm.subTask(RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.analyzing", cu.getElementName())); //$NON-NLS-1$
		if ((! cu.exists()) || (cu.isReadOnly()) || (!cu.isStructureKnown()))
			return;
		result.merge(analyzer.analyze(searchResults.getSearchResults(), cu));
	}
	
	private ISearchPattern createSearchPattern(){
		return SearchEngine.createSearchPattern(fField, IJavaSearchConstants.REFERENCES);
	}
	
	private IJavaSearchScope createRefactoringScope() throws JavaModelException{
		if (Flags.isPrivate(fField.getFlags()))
			return SearchEngine.createJavaSearchScope(new IResource[]{getResource(fField)});
		else
			return SearchEngine.createWorkspaceScope();	
	}
	
	private SearchResultGroup[] getOccurrences(IProgressMonitor pm) throws JavaModelException{
		pm.subTask(RefactoringCoreMessages.getString("RenameFieldRefactoring.searching"));	 //$NON-NLS-1$
		fOccurrences= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 6), createRefactoringScope(), createSearchPattern());
		return fOccurrences;
	}
	
	// ---------- Changes -----------------

	/* non java-doc
	 * IRefactoring#createChange
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("RenameFieldRefactoring.creating_change"), 1); //$NON-NLS-1$
			CompositeChange builder= new CompositeChange();
			addOccurrences(new SubProgressMonitor(pm, 1), builder);
			return builder; 
		} finally{
			pm.done();
		}
	}
	
	private void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		pm.beginTask(RefactoringCoreMessages.getString("RenameFieldRefactoring.creating_change"), 4);//$NON-NLS-1$
		TextBufferChangeManager manager= new TextBufferChangeManager(fTextBufferChangeCreator);

		addDeclarationUpdate(manager);
		pm.worked(1);	
		
		if (fUpdateReferences)
			addReferenceUpdates(manager, new SubProgressMonitor(pm, 3));
		
		//putting it together
		IChange[] changes= manager.getAllChanges();
		for (int i= 0; i < changes.length; i++){
			builder.addChange(changes[i]);
		}
	}
	
	private void addDeclarationUpdate(TextBufferChangeManager manager) throws JavaModelException{
		manager.addReplace(fField.getCompilationUnit(), "update field declaration", fField.getNameRange().getOffset(), fField.getElementName().length(), fNewName);
	}
	
	private void addReferenceUpdates(TextBufferChangeManager manager, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", fOccurrences.length);
		for (int i= 0; i < fOccurrences.length; i++){
			IJavaElement element= JavaCore.create(fOccurrences[i].getResource());
			if (!(element instanceof ICompilationUnit))
				continue;
			SearchResult[] results= fOccurrences[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				manager.addSimpleTextChange((ICompilationUnit)element, createTextChange(results[j]));
			}
			pm.worked(1);			
		}
	}
	
	private SimpleReplaceTextChange createTextChange(SearchResult searchResult) {
		return new SimpleReplaceTextChange(RefactoringCoreMessages.getString("RenameFieldRefactoring.update_reference"), searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), getNewName()){ //$NON-NLS-1$
			protected SimpleTextChange[] adjust(ITextBuffer buffer) {
				String oldText= buffer.getContent(getOffset(), getLength());
				if (oldText.startsWith("this.") && (! getText().startsWith("this."))){ //$NON-NLS-2$ //$NON-NLS-1$
					setText("this." + getText()); //$NON-NLS-1$
					setLength(getLength());
				}
				return null;
			}
		};
	}		
}