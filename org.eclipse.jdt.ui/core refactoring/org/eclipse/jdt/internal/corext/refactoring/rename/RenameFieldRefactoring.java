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
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
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
import org.eclipse.jdt.internal.corext.refactoring.util.*;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class RenameFieldRefactoring extends Refactoring implements IRenameRefactoring, IReferenceUpdatingRefactoring, ITextUpdatingRefactoring{
	
	private IField fField;
	private String fNewName;
	private SearchResultGroup[] fOccurrences;
	private boolean fUpdateReferences;
	
	private boolean fUpdateJavaDoc;
	private boolean fUpdateComments;
	private boolean fUpdateStrings;
	
	private final CodeGenerationSettings fSettings;
	private final String[] fPrefixes;
	private final String[] fSuffixes;
	
	private boolean fRenameGetter;
	private boolean fRenameSetter;
	
	public RenameFieldRefactoring(IField field, CodeGenerationSettings settings, String[] prefixes, String[] suffixes){
		Assert.isTrue(field.exists());
		fField= field;
		fNewName= fField.getElementName();
		fUpdateReferences= true;
		fUpdateJavaDoc= false;
		fUpdateComments= false;
		fUpdateStrings= false;
		
		Assert.isNotNull(settings);
		fSettings= settings;
		fPrefixes= prefixes;
		fSuffixes= suffixes;
		fRenameGetter= false;
		fRenameSetter= false;
	}
	
	public Object getNewElement(){
		return fField.getDeclaringType().getField(fNewName);
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
	
	//-- getter/setter
	
	public boolean canEnableGetterRenaming() throws JavaModelException{
		if (fField.getDeclaringType().isInterface())
			return false;
		return getGetter() != null;	
	}
	
	public boolean canEnableSetterRenaming() throws JavaModelException{
		if (fField.getDeclaringType().isInterface())
			return false;
		return getGetter() != null;	
	}
	
	public boolean getRenameGetter() {
		return fRenameGetter;
	}

	public void setRenameGetter(boolean renameGetter) {
		fRenameGetter= renameGetter;
	}

	public boolean getRenameSetter() {
		return fRenameSetter;
	}

	public void setRenameSetter(boolean renameSetter) {
		fRenameSetter= renameSetter;
	}
	
	public IMethod getGetter() throws JavaModelException{
		return GetterSetterUtil.getGetter(fField, fSettings, fPrefixes, fSuffixes);
	}
	
	public IMethod getSetter() throws JavaModelException{
		return GetterSetterUtil.getSetter(fField, fSettings, fPrefixes, fSuffixes);
	}

	public String getNewGetterName(){
		return GetterSetterUtil.getGetterName(fNewName, fSettings, fPrefixes, fSuffixes);
	}
	
	public String getNewSetterName(){
		return GetterSetterUtil.getSetterName(fNewName, fSettings, fPrefixes, fSuffixes);
	}
	
	//----------
	
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
			pm.beginTask("", 11); //$NON-NLS-1$
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
				
			if (getGetter() != null && fRenameGetter)
				result.merge(checkAccessor(new SubProgressMonitor(pm, 1), getGetter(), getNewGetterName()));
				
			if (getSetter() != null && fRenameSetter)
				result.merge(checkAccessor(new SubProgressMonitor(pm, 1), getSetter(), getNewSetterName()));
				
			return result;
		} finally{
			pm.done();
		}
	}
	
	private RefactoringStatus checkAccessor(IProgressMonitor pm, IMethod existingAccessor, String newAccessorName) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAccessorDeclarations(pm, existingAccessor));
		result.merge(checkNewAccessor(existingAccessor, newAccessorName));
		return result;
	}
	
	private RefactoringStatus checkNewAccessor(IMethod existingAccessor, String newAccessorName) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IMethod accessor= JavaModelUtil.findMethod(newAccessorName, existingAccessor.getParameterTypes(), false, fField.getDeclaringType());
		if (accessor == null || !accessor.exists())
			return null;
			
		String msg= "Method \'" + JavaElementUtil.createMethodSignature(accessor) + "\' already exists in \'" 
					+ fField.getDeclaringType().getFullyQualifiedName() + "\'";
		result.addError(msg, JavaSourceContext.create(accessor));
		return result;
	}
	
	private RefactoringStatus checkAccessorDeclarations(IProgressMonitor pm, IMethod existingAccessor) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		ISearchPattern pattern= SearchEngine.createSearchPattern(existingAccessor, IJavaSearchConstants.DECLARATIONS);
		IJavaSearchScope scope= SearchEngine.createHierarchyScope(fField.getDeclaringType());
		SearchResultGroup[] groupDeclarations= RefactoringSearchEngine.search(pm, scope, pattern);
		Assert.isTrue(groupDeclarations.length > 0);
		if (groupDeclarations.length != 1){
			result.addError("Method \'" + JavaElementUtil.createMethodSignature(existingAccessor) + "\' is overridden or overrides another method.");
		} else {
			SearchResultGroup group= groupDeclarations[0];
			Assert.isTrue(group.getSearchResults().length > 0);
			if (group.getSearchResults().length != 1)
				result.addError("Method \'" + JavaElementUtil.createMethodSignature(existingAccessor) + "\' is overridden or overrides another method.");
		}	
		return result;
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
			pm.beginTask(RefactoringCoreMessages.getString("RenameFieldRefactoring.creating_change"), 3); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();

			addOccurrences(new SubProgressMonitor(pm, 1), manager);

			if (getRenameGetter())
				addGetterOccurrences(new SubProgressMonitor(pm, 1), manager);
			else
				pm.worked(1);
					
			if (getSetter() != null && fRenameSetter)
				addSetterOccurrences(new SubProgressMonitor(pm, 1), manager);
			else
				pm.worked(1);
				
			return new CompositeChange("Rename Field", manager.getAllChanges());
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}
	
	private void addGetterOccurrences(IProgressMonitor pm, TextChangeManager manager) throws CoreException{
		addAccessorOccurrences(pm, manager, getGetter(), "Update getter occurrence", getNewGetterName());
	}
	
	private void addSetterOccurrences(IProgressMonitor pm, TextChangeManager manager) throws CoreException{
		addAccessorOccurrences(pm, manager, getSetter(), "Update setter occurrence", getNewSetterName());
	}

	private static void addAccessorOccurrences(IProgressMonitor pm, TextChangeManager manager, IMethod accessor, String editName, String newAccessorName) throws CoreException {
		Assert.isTrue(accessor.exists());
		
		IJavaSearchScope scope= RefactoringScopeFactory.create(accessor);
		ISearchPattern pattern= SearchEngine.createSearchPattern(accessor, IJavaSearchConstants.ALL_OCCURRENCES);
		SearchResultGroup[] groupedResults= RefactoringSearchEngine.search(pm, scope, pattern);
		
		for (int i= 0; i < groupedResults.length; i++) {
			IJavaElement element= JavaCore.create(groupedResults[i].getResource());
			if (!(element instanceof ICompilationUnit))
				continue;
			SearchResult[] results= groupedResults[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				SearchResult searchResult= results[j];
				ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)element);
				TextEdit edit= new UpdateMethodReferenceEdit(searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), newAccessorName, accessor.getElementName());
				manager.get(cu).addTextEdit(editName, edit);
			}
		}
	}
	
	private void addTextMatches(TextChangeManager manager, IProgressMonitor pm) throws JavaModelException{
		TextMatchFinder.findTextMatches(pm, createRefactoringScope(), this, manager);
	}	
	
	private void addOccurrences(IProgressMonitor pm, TextChangeManager manager) throws CoreException{
		pm.beginTask(RefactoringCoreMessages.getString("RenameFieldRefactoring.creating_change"), 5);//$NON-NLS-1$

		addDeclarationUpdate(manager);
		pm.worked(1);	
		
		if (fUpdateReferences)
			addReferenceUpdates(manager, new SubProgressMonitor(pm, 3));
		
		pm.subTask("searching for text matches...");
		addTextMatches(manager, new SubProgressMonitor(pm, 1));
	}
	
	private void addDeclarationUpdate(TextChangeManager manager) throws CoreException{
		TextEdit textEdit= SimpleTextEdit.createReplace(fField.getNameRange().getOffset(), fField.getElementName().length(), fNewName);
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fField.getCompilationUnit());
		manager.get(cu).addTextEdit("Update field declaration", textEdit);
	}
	
	private void addReferenceUpdates(TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", fOccurrences.length);
		String editName= "Update field reference";
		for (int i= 0; i < fOccurrences.length; i++){
			IJavaElement element= JavaCore.create(fOccurrences[i].getResource());
			if (!(element instanceof ICompilationUnit))
				continue;
			SearchResult[] results= fOccurrences[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)element);
				manager.get(cu).addTextEdit(editName, createTextChange(results[j]));
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