/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.codemanipulation.NameProposer;
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
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry.Context;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange.EditChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
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
	private SearchResultGroup[] fReferences;
	private TextChangeManager fChangeManager;
	private boolean fUpdateReferences;
	
	private boolean fUpdateJavaDoc;
	private boolean fUpdateComments;
	private boolean fUpdateStrings;
	
	private final String[] fPrefixes;
	private final String[] fSuffixes;
	
	private boolean fRenameGetter;
	private boolean fRenameSetter;
	
	public RenameFieldRefactoring(IField field, String[] prefixes, String[] suffixes){
		Assert.isTrue(field.exists());
		fField= field;
		fNewName= fField.getElementName();
		fUpdateReferences= true;
		fUpdateJavaDoc= false;
		fUpdateComments= false;
		fUpdateStrings= false;
		
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
		return getSetter() != null;	
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
		return GetterSetterUtil.getGetter(fField, fPrefixes, fSuffixes);
	}
	
	public IMethod getSetter() throws JavaModelException{
		return GetterSetterUtil.getSetter(fField, fPrefixes, fSuffixes);
	}

	public String getNewGetterName() throws JavaModelException {
		boolean isBoolean=	fField.getTypeSignature() == Signature.SIG_BOOLEAN;
		return new NameProposer(fPrefixes, fSuffixes).proposeGetterName(fNewName, isBoolean);
	}
	
	public String getNewSetterName(){
		return new NameProposer(fPrefixes, fSuffixes).proposeSetterName(fNewName);
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
		if (orig == null || ! orig.exists()){
			String message= RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.deleted", //$NON-NLS-1$
								fField.getCompilationUnit().getElementName());
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fField= orig;
		
		return Checks.checkIfCuBroken(fField);
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#checkNewName
	 */
	public RefactoringStatus checkNewName(String newName) throws JavaModelException {
		Assert.isNotNull(newName, RefactoringCoreMessages.getString("RenameFieldRefactoring.assert.new_name")); //$NON-NLS-1$
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
			pm.beginTask("", 13); //$NON-NLS-1$
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
			
			fReferences= null;
			if (fUpdateReferences)
				fReferences= getReferences(new SubProgressMonitor(pm, 3));
			else
				pm.worked(3);
			
			if (fUpdateReferences)
				result.merge(analyzeAffectedCompilationUnits());
				
			if (getGetter() != null && fRenameGetter){
				result.merge(checkAccessor(new SubProgressMonitor(pm, 1), getGetter(), getNewGetterName()));
				result.merge(Checks.checkIfConstructorName(getGetter(), getNewGetterName(), fField.getDeclaringType().getElementName()));
			}	
				
			if (getSetter() != null && fRenameSetter){
				result.merge(checkAccessor(new SubProgressMonitor(pm, 1), getSetter(), getNewSetterName()));
				result.merge(Checks.checkIfConstructorName(getSetter(), getNewSetterName(), fField.getDeclaringType().getElementName()));
			}	
			
			if (fUpdateReferences)
				result.merge(analyzeRenameChanges(new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);
			
			if (result.hasFatalError())
				return result;
			fChangeManager= createTextChangeManager(new SubProgressMonitor(pm, 5));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}
	
	//----------
	private RefactoringStatus analyzeRenameChanges(IProgressMonitor pm) throws JavaModelException{
		try {
			pm.beginTask("", 4); //$NON-NLS-1$
			
			TextChangeManager manager= createTextChangeManager(new SubProgressMonitor(pm, 1));
			SearchResultGroup[] oldOccurrences= getOldOccurrences(new SubProgressMonitor(pm, 1));
			SearchResultGroup[] newOccurrences= getNewOccurrences(new SubProgressMonitor(pm, 1), manager);
			
			RefactoringStatus result= new RefactoringStatus();
			for (int i= 0; i < oldOccurrences.length; i++) {
				SearchResultGroup searchResultGroup= oldOccurrences[i];
				SearchResult[] searchResults= searchResultGroup.getSearchResults();
				ICompilationUnit cunit= getCompilationUnit(searchResultGroup);
				if (cunit == null)
					continue;
				for (int j= 0; j < searchResults.length; j++) {
					SearchResult searchResult= searchResults[j];
					if (! existsInNewOccurrences(searchResult, newOccurrences, manager)){
						ISourceRange range= new SourceRange(searchResult.getStart(), searchResult.getEnd() - searchResult.getStart());
						Context context= JavaSourceContext.create(cunit, range); //XXX
						String message= RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.shadows", cunit.getElementName());//$NON-NLS-1$
						result.addError(message , context);
					}	
				}
			}
			return result;
		} catch(CoreException e) {
			throw new JavaModelException(e);
		}
	}

	private SearchResultGroup[] getNewOccurrences(IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		ICompilationUnit[] compilationUnitsToModify= manager.getAllCompilationUnits();
		ICompilationUnit[] newWorkingCopies= getNewWorkingCopies(compilationUnitsToModify, manager, new SubProgressMonitor(pm, 1));
		
		ICompilationUnit declaringCuWorkingCopy= findWorkingCopyForDeclaringCu(newWorkingCopies);
		if (declaringCuWorkingCopy == null)
			return new SearchResultGroup[0];
		
		IField field= getNewField(declaringCuWorkingCopy);
		if (field == null || ! field.exists())
			return new SearchResultGroup[0];
		
		ISearchPattern newPattern= SearchEngine.createSearchPattern(field, IJavaSearchConstants.ALL_OCCURRENCES);			
		return RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), createRefactoringScope(), newPattern, newWorkingCopies);
	}

	private SearchResultGroup[] getOldOccurrences(IProgressMonitor pm) throws JavaModelException {
		ISearchPattern oldPattern= SearchEngine.createSearchPattern(fField, IJavaSearchConstants.ALL_OCCURRENCES);				
		return RefactoringSearchEngine.search(pm, createRefactoringScope(), oldPattern);
	}
	
	private ICompilationUnit findWorkingCopyForDeclaringCu(ICompilationUnit[] newWorkingCopies){
		ICompilationUnit originalDeclaringCu= WorkingCopyUtil.getOriginal(fField.getCompilationUnit());
		for (int i= 0; i < newWorkingCopies.length; i++) {
			if (WorkingCopyUtil.getOriginal(newWorkingCopies[i]).equals(originalDeclaringCu))
				return newWorkingCopies[i];
		}
		return null;
	}
	
	private static ICompilationUnit[] getNewWorkingCopies(ICompilationUnit[] compilationUnitsToModify, TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", compilationUnitsToModify.length); //$NON-NLS-1$
		ICompilationUnit[] newWorkingCopies= new ICompilationUnit[compilationUnitsToModify.length];
		for (int i= 0; i < compilationUnitsToModify.length; i++) {
			ICompilationUnit cu= compilationUnitsToModify[i];
			newWorkingCopies[i]= WorkingCopyUtil.getNewWorkingCopy(cu);
			newWorkingCopies[i].getBuffer().setContents(manager.get(cu).getPreviewTextBuffer().getContent());
			newWorkingCopies[i].makeConsistent(new SubProgressMonitor(pm, 1));
		}
		return newWorkingCopies;
	}
	
	private IField getNewField(ICompilationUnit newWorkingCopyOfDeclaringCu) throws JavaModelException{
		IType[] allNewTypes= newWorkingCopyOfDeclaringCu.getAllTypes();
		String fullyTypeName= fField.getDeclaringType().getFullyQualifiedName();
		for (int i= 0; i < allNewTypes.length; i++) {
			if (allNewTypes[i].getFullyQualifiedName().equals(fullyTypeName))
				return allNewTypes[i].getField(fNewName);
		}
		return null;
	}
	
	private static boolean existsInNewOccurrences(SearchResult searchResult, SearchResultGroup[] newOccurrences, TextChangeManager manager) throws CoreException{
		SearchResultGroup newGroup= findOccurrenceGroup(searchResult.getResource(), newOccurrences);
		if (newGroup == null)
			return false;
		
		TextRange oldEditRange= getCorrespondingEditChangeRange(searchResult, manager);
		if (oldEditRange == null)
			return false;
		
		SearchResult[] newSearchResults= newGroup.getSearchResults();
		for (int i= 0; i < newSearchResults.length; i++) {
			if (createTextRange(newSearchResults[i]).equals(oldEditRange))
				return true;
		}

		return false;
	}

	private static TextRange getCorrespondingEditChangeRange(SearchResult searchResult, TextChangeManager manager) throws CoreException{
		TextChange change= getTextChange(searchResult, manager);
		if (change == null)
			return null;
		
		TextRange oldMatchRange= createTextRange(searchResult);
		EditChange[] editChanges= change.getTextEditChanges();	
		for (int i= 0; i < editChanges.length; i++) {
			if (oldMatchRange.equals(editChanges[i].getTextRange()))
				return change.getNewTextRange(editChanges[i]);
		}
		return null;
	}
	
	private static TextChange getTextChange(SearchResult searchResult, TextChangeManager manager) throws CoreException{
		ICompilationUnit cu= getCompilationUnit(searchResult);
		if (cu == null)
			return null;
		ICompilationUnit oldWorkingCopy= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		if (oldWorkingCopy == null)
			return null;
		return manager.get(oldWorkingCopy);
	}
	
	private static ICompilationUnit getCompilationUnit(SearchResultGroup searchResultGroup){
		if (searchResultGroup.getSearchResults() == null || searchResultGroup.getSearchResults().length == 0)
			return null;
		return getCompilationUnit(searchResultGroup.getSearchResults()[0]);
	}
	
	private static ICompilationUnit getCompilationUnit(SearchResult searchResult){
		IJavaElement jElement= JavaCore.create(searchResult.getResource());
		if (jElement == null || jElement.getElementType() != IJavaElement.COMPILATION_UNIT)
			return null;
		return (ICompilationUnit)jElement;
	}
	
	private static TextRange createTextRange(SearchResult searchResult) {
		return TextRange.createFromStartAndExclusiveEnd(searchResult.getStart(), searchResult.getEnd());
	}
	
	private static SearchResultGroup findOccurrenceGroup(IResource resource, SearchResultGroup[] newOccurrences){
		for (int i= 0; i < newOccurrences.length; i++) {
			if (newOccurrences[i].getResource().equals(resource))
				return newOccurrences[i];
		}
		return null;
	}
	
	//----------
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
	
		String message= RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.already_exists", //$NON-NLS-1$
				new String[]{JavaElementUtil.createMethodSignature(accessor), JavaModelUtil.getFullyQualifiedName(fField.getDeclaringType())});
		result.addError(message, JavaSourceContext.create(accessor));
		return result;
	}
	
	private RefactoringStatus checkAccessorDeclarations(IProgressMonitor pm, IMethod existingAccessor) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		ISearchPattern pattern= SearchEngine.createSearchPattern(existingAccessor, IJavaSearchConstants.DECLARATIONS);
		IJavaSearchScope scope= SearchEngine.createHierarchyScope(fField.getDeclaringType());
		SearchResultGroup[] groupDeclarations= RefactoringSearchEngine.search(pm, scope, pattern);
		Assert.isTrue(groupDeclarations.length > 0);
		if (groupDeclarations.length != 1){
			String message= RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.overridden", //$NON-NLS-1$
								JavaElementUtil.createMethodSignature(existingAccessor));
			result.addError(message);
		} else {
			SearchResultGroup group= groupDeclarations[0];
			Assert.isTrue(group.getSearchResults().length > 0);
			if (group.getSearchResults().length != 1){
				String message= RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.overridden_or_overrides", //$NON-NLS-1$
									JavaElementUtil.createMethodSignature(existingAccessor));
				result.addError(message);
			}	
		}	
		return result;
	}
	
	private static boolean isInstaceField(IField field) throws JavaModelException{
		if (field.getDeclaringType().isInterface())
			return false;
		else 
			return ! JdtFlags.isStatic(field);
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
																			new String[]{fField.getElementName(), getNewName(), JavaModelUtil.getFullyQualifiedName(nestedTypes[i])});
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
				 															new String[]{getNewName(), JavaModelUtil.getFullyQualifiedName(current)});
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
	private RefactoringStatus analyzeAffectedCompilationUnits() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		fReferences= Checks.excludeCompilationUnits(fReferences, result);
		if (result.hasFatalError())
			return result;
		
		result.merge(Checks.checkCompileErrorsInAffectedFiles(fReferences));	
		return result;
	}
	
	private IFile[] getAllFilesToModify() throws JavaModelException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	private ISearchPattern createSearchPattern(){
		return SearchEngine.createSearchPattern(fField, IJavaSearchConstants.REFERENCES);
	}
	
	private IJavaSearchScope createRefactoringScope() throws JavaModelException{
		return RefactoringScopeFactory.create(fField);
	}
	
	private SearchResultGroup[] getReferences(IProgressMonitor pm) throws JavaModelException{
		pm.subTask(RefactoringCoreMessages.getString("RenameFieldRefactoring.searching"));	 //$NON-NLS-1$
		return RefactoringSearchEngine.search(new SubProgressMonitor(pm, 6), createRefactoringScope(), createSearchPattern());
	}
	
	// ---------- Changes -----------------

	/* non java-doc
	 * IRefactoring#createChange
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			return new CompositeChange(RefactoringCoreMessages.getString("RenameFieldRefactoring.Rename_Field"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
		}	
	}
	
	private TextChangeManager createTextChangeManager(IProgressMonitor pm) throws CoreException {
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
					
			return manager;
		} finally{
			pm.done();
		}
	}
	
	private void addGetterOccurrences(IProgressMonitor pm, TextChangeManager manager) throws CoreException{
		addAccessorOccurrences(pm, manager, getGetter(), RefactoringCoreMessages.getString("RenameFieldRefactoring.Update_getter_occurrence"), getNewGetterName()); //$NON-NLS-1$
	}
	
	private void addSetterOccurrences(IProgressMonitor pm, TextChangeManager manager) throws CoreException{
		addAccessorOccurrences(pm, manager, getSetter(), RefactoringCoreMessages.getString("RenameFieldRefactoring.Update_setter_occurrence"), getNewSetterName()); //$NON-NLS-1$
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
		
		pm.subTask(RefactoringCoreMessages.getString("RenameFieldRefactoring.searching_for_text_matches")); //$NON-NLS-1$
		addTextMatches(manager, new SubProgressMonitor(pm, 1));
	}
	
	private void addDeclarationUpdate(TextChangeManager manager) throws CoreException{
		TextEdit textEdit= SimpleTextEdit.createReplace(fField.getNameRange().getOffset(), fField.getElementName().length(), fNewName);
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fField.getCompilationUnit());
		manager.get(cu).addTextEdit(RefactoringCoreMessages.getString("RenameFieldRefactoring.Update_field_declaration"), textEdit); //$NON-NLS-1$
	}
	
	private void addReferenceUpdates(TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", fReferences.length); //$NON-NLS-1$
		String editName= RefactoringCoreMessages.getString("RenameFieldRefactoring.Update_field_reference"); //$NON-NLS-1$
		for (int i= 0; i < fReferences.length; i++){
			IJavaElement element= JavaCore.create(fReferences[i].getResource());
			if (!(element instanceof ICompilationUnit))
				continue;
			SearchResult[] results= fReferences[i].getSearchResults();
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