/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;

public class RenameFieldRefactoring extends Refactoring implements IRenameRefactoring, IReferenceUpdatingRefactoring, ITextUpdatingRefactoring{
	
	private String fNewName;
	private IField fField;
	
	private SearchResultGroup[] fOccurrences;
	private boolean fUpdateReferences;
	
	private boolean fUpdateJavaDoc;
	private boolean fUpdateComments;
	private boolean fUpdateStrings;
	
	public RenameFieldRefactoring(IField field){
		Assert.isTrue(field.exists());
		fField= field;
		fUpdateReferences= true;
		fUpdateJavaDoc= false;
		fUpdateComments= false;
		fUpdateStrings= false;
	}
	
	/*
	 * @see ITextUpdatingRefactoring#canEnableTextUpdating()
	 */
	public boolean canEnableTextUpdating() {
		return true;
	}
	
	/*
	 * @see ITextUpdatingRefactoring#getUpdateJavaDoc()
	 */
	public boolean getUpdateJavaDoc() {
		return fUpdateJavaDoc;
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateComments()
	 */
	public boolean getUpdateComments() {
		return fUpdateComments;
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateStrings()
	 */
	public boolean getUpdateStrings() {
		return fUpdateStrings;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateJavaDoc(boolean)
	 */
	public void setUpdateJavaDoc(boolean update) {
		fUpdateJavaDoc= update;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateComments(boolean)
	 */
	public void setUpdateComments(boolean update) {
		fUpdateComments= update;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateStrings(boolean)
	 */
	public void setUpdateStrings(boolean update) {
		fUpdateStrings= update;
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
		IField orig= (IField)WorkingCopyUtil.getOriginal(fField);
		if (orig == null || ! orig.exists())
			return RefactoringStatus.createFatalErrorStatus("The selected field has been deleted from '" + fField.getCompilationUnit().getElementName()+ "'.");
		fField= orig;
		
		return Checks.checkIfCuBroken(fField);
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#checkNewName
	 */
	public RefactoringStatus checkNewName(String newName) throws JavaModelException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= Checks.checkFieldName(newName);
		
		if (isInstaceField(fField) && (! Checks.startsWithLowerCase(newName)))
			result.addWarning(RefactoringCoreMessages.getString("RenameFieldRefactoring.should_start_lowercase")); //$NON-NLS-1$
			
		if (Checks.isAlreadyNamed(fField, newName))
			result.addFatalError(RefactoringCoreMessages.getString("RenameFieldRefactoring.another_name")); //$NON-NLS-1$
		if (fField.getDeclaringType().getField(newName).exists())
			result.addFatalError(RefactoringCoreMessages.getString("RenameFieldRefactoring.field_already_defined")); //$NON-NLS-1$
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
			result.merge(checkNewName(fNewName));
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
			if (otherField.exists()){
				String msg= RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.hiding", //$NON-NLS-1$
																			new String[]{fField.getElementName(), getNewName(), nestedTypes[i].getFullyQualifiedName()});
				result.addWarning(msg, JavaSourceContext.create(otherField));
			}									
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
			if (otherField.exists()){
				String msg= RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.hiding2", //$NON-NLS-1$
				 															new String[]{getNewName(), current.getFullyQualifiedName(), fField.getElementName()});
				result.addWarning(msg, JavaSourceContext.create(otherField));
			}									
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
		fOccurrences= Checks.excludeCompilationUnits(fOccurrences, result);
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
		return RefactoringScopeFactory.create(fField);
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
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}
	
	private void addTextMatches(TextChangeManager manager, IProgressMonitor pm) throws JavaModelException{
		TextMatchFinder.findTextMatches(pm, createRefactoringScope(), this, manager);
	}	
	
	private void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws CoreException{
		pm.beginTask(RefactoringCoreMessages.getString("RenameFieldRefactoring.creating_change"), 5);//$NON-NLS-1$
		TextChangeManager manager= new TextChangeManager();

		addDeclarationUpdate(manager);
		pm.worked(1);	
		
		if (fUpdateReferences)
			addReferenceUpdates(manager, new SubProgressMonitor(pm, 3));
		
		pm.subTask("searching for text matches...");
		addTextMatches(manager, new SubProgressMonitor(pm, 1));
		
		//putting it together
		builder.addAll(manager.getAllChanges());
	}
	
	private void addDeclarationUpdate(TextChangeManager manager) throws CoreException{
		TextEdit textEdit= SimpleTextEdit.createReplace(fField.getNameRange().getOffset(), fField.getElementName().length(), fNewName);
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fField.getCompilationUnit());
		manager.get(cu).addTextEdit("update field declaration", textEdit);
	}
	
	private void addReferenceUpdates(TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", fOccurrences.length);
		for (int i= 0; i < fOccurrences.length; i++){
			IJavaElement element= JavaCore.create(fOccurrences[i].getResource());
			if (!(element instanceof ICompilationUnit))
				continue;
			SearchResult[] results= fOccurrences[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				String name= RefactoringCoreMessages.getString("RenameFieldRefactoring.update_reference");
				ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)element);
				manager.get(cu).addTextEdit(name, createTextChange(results[j]));
			}
			pm.worked(1);			
		}
	}
	
	private TextEdit createTextChange(SearchResult searchResult) {
		int offset= searchResult.getStart();
		int length= searchResult.getEnd() - searchResult.getStart();
		return new UpdateFieldReference(offset, length, fNewName, fField.getElementName());
	}

	//-----------
	private static class UpdateFieldReference extends SimpleTextEdit{

		private String fOldName;
		
		UpdateFieldReference(int offset, int length, String newName, String oldName){
			super(offset, length, newName);
			Assert.isNotNull(oldName);
			fOldName= oldName;
		}
		
		private UpdateFieldReference(TextRange range, String newName, String oldName) {
			super(range, newName);
			Assert.isNotNull(oldName);
			fOldName= oldName;
		}
	
		/* non Java-doc
		 * @see TextEdit#copy
		 */
		public TextEdit copy() {
			return new UpdateFieldReference(getTextRange().copy(), getText(), fOldName);
		}
		
		/* non java-doc
		 * @see TextEdit#connect(TextBufferEditor)
		 */
		public void connect(TextBufferEditor editor) throws CoreException {
			TextRange oldRange= getTextRange();
			int offset= oldRange.getOffset() + oldRange.getLength() - fOldName.length();
			setTextRange(new TextRange(offset, fOldName.length()));
		}
	}
}