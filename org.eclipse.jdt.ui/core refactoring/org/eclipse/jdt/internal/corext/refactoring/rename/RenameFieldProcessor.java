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
package org.eclipse.jdt.internal.corext.refactoring.rename;
import java.util.Arrays;
import java.util.List;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

public class RenameFieldProcessor extends JavaRenameProcessor implements IReferenceUpdating, ITextUpdating {
	
	private static final String DECLARED_SUPERTYPE= RefactoringCoreMessages.getString("RenameFieldRefactoring.declared_in_supertype"); //$NON-NLS-1$
	private IField fField;
	private SearchResultGroup[] fReferences;
	private TextChangeManager fChangeManager;
	private ICompilationUnit[] fNewWorkingCopies;
	private boolean fUpdateReferences;
	
	private boolean fUpdateTextualMatches;
	
	private boolean fRenameGetter;
	private boolean fRenameSetter;

	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameFieldProcessor"; //$NON-NLS-1$
	
	public RenameFieldProcessor(IField field) {
		fField= field;
		setNewElementName(fField.getElementName());
		fUpdateReferences= true;
		fUpdateTextualMatches= false;
		
		fRenameGetter= false;
		fRenameSetter= false;
	}
	
	//---- IRefactoringProcessor --------------------------------

	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws CoreException {
		if (fField == null)
			return false;
		return Checks.isAvailable(fField);
	}
	
	 public String getProcessorName() {
		return RefactoringCoreMessages.getFormattedString(
			"RenameFieldRefactoring.name", //$NON-NLS-1$
			new String[]{fField.getElementName(), getNewElementName()});
	 }
	
	protected String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fField);
	}

	public Object[] getElements() {
		return new Object[] {fField};
	}
	
	protected void loadDerivedParticipants(List result, String[] natures, SharableParticipants shared) throws CoreException {
		if (fRenameGetter) {
			IMethod getter= getGetter();
			if (getter != null) {
				addParticipants(result, getter, getNewGetterName(), natures, shared);
			}
		}
		if (fRenameSetter) {
			IMethod setter= getSetter();
			if (setter != null) {
				addParticipants(result, setter, getNewSetterName(), natures, shared);
			}
		}
	}

	private void addParticipants(List result, IMethod method, String methodName, String[] natures, SharableParticipants shared) throws CoreException {
		RenameArguments args= new RenameArguments(methodName, getUpdateReferences());
		RenameParticipant[] participants= ParticipantManager.loadRenameParticipants(this, method, args, natures, shared);
		result.addAll(Arrays.asList(participants));
	}

	//---- IRenameProcessor -------------------------------------
	
	public final String getCurrentElementName(){
		return fField.getElementName();
	}
	
	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
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
	
	public Object getNewElement() {
		return fField.getDeclaringType().getField(getNewElementName());
	}
	
	//---- ITextUpdating2 ---------------------------------------------
	
	public boolean canEnableTextUpdating() {
		return true;
	}
	
	public boolean getUpdateTextualMatches() {
		return fUpdateTextualMatches;
	}
	
	public void setUpdateTextualMatches(boolean update) {
		fUpdateTextualMatches= update;
	}
	
	//---- IReferenceUpdating -----------------------------------

	public boolean canEnableUpdateReferences() {
		return true;
	}

	public void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}
	
	public boolean getUpdateReferences(){
		return fUpdateReferences;
	}
		
	//-- getter/setter --------------------------------------------------
	
	/**
	 * @return Error message or <code>null</code> if getter can be renamed.
	 */
	public String canEnableGetterRenaming() throws CoreException{
		if (fField.getDeclaringType().isInterface())
			return getGetter() == null ? "": null; //$NON-NLS-1$
			
		IMethod getter= getGetter();
		if (getter == null) 
			return ""; //$NON-NLS-1$
		if (MethodChecks.isVirtual(getter) && null != MethodChecks.isDeclaredInInterface(getter, new NullProgressMonitor()))
			return DECLARED_SUPERTYPE;
		if (MethodChecks.isVirtual(getter) && null != MethodChecks.overridesAnotherMethod(getter, new NullProgressMonitor()))
			return DECLARED_SUPERTYPE;
		return null;	
	}
	
	/**
	 * @return Error message or <code>null</code> if setter can be renamed.
	 */
	public String canEnableSetterRenaming() throws CoreException{
		if (fField.getDeclaringType().isInterface())
			return getSetter() == null ? "": null; //$NON-NLS-1$
			
		IMethod setter= getSetter();
		if (setter == null) 
			return "";	 //$NON-NLS-1$
		if (MethodChecks.isVirtual(setter) && null != MethodChecks.isDeclaredInInterface(setter, new NullProgressMonitor()))
			return DECLARED_SUPERTYPE;
		if (MethodChecks.isVirtual(setter) && null != MethodChecks.overridesAnotherMethod(setter, new NullProgressMonitor()))
			return DECLARED_SUPERTYPE;
		return null;	
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
	
	public IMethod getGetter() throws CoreException {
		return GetterSetterUtil.getGetter(fField);
	}
	
	public IMethod getSetter() throws CoreException {
		return GetterSetterUtil.getSetter(fField);
	}

	public String getNewGetterName() throws CoreException {
		IMethod primaryGetterCandidate= JavaModelUtil.findMethod(GetterSetterUtil.getGetterName(fField, new String[0]), new String[0], false, fField.getDeclaringType());
		if (! JavaModelUtil.isBoolean(fField) || (primaryGetterCandidate != null && primaryGetterCandidate.exists()))
			return GetterSetterUtil.getGetterName(fField.getJavaProject(), getNewElementName(), fField.getFlags(), JavaModelUtil.isBoolean(fField), null);
		//bug 30906 describes why we need to look for other alternatives here	
		return GetterSetterUtil.getGetterName(fField.getJavaProject(), getNewElementName(), fField.getFlags(), false, null);
	}

	public String getNewSetterName() throws CoreException {
		return GetterSetterUtil.getSetterName(fField.getJavaProject(), getNewElementName(), fField.getFlags(), JavaModelUtil.isBoolean(fField), null);
	}

	// -------------- Preconditions -----------------------
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException{
		IField orig= (IField)WorkingCopyUtil.getOriginal(fField);
		if (orig == null || ! orig.exists()){
			String message= RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.deleted", //$NON-NLS-1$
								fField.getCompilationUnit().getElementName());
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fField= orig;
		
		return Checks.checkIfCuBroken(fField);
	}
	
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		try{
			pm.beginTask("", 18); //$NON-NLS-1$
			pm.setTaskName(RefactoringCoreMessages.getString("RenameFieldRefactoring.checking")); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(Checks.checkIfCuBroken(fField));
			if (result.hasFatalError())
				return result;
			result.merge(checkNewElementName(getNewElementName()));
			pm.worked(1);
			result.merge(checkEnclosingHierarchy());
			pm.worked(1);
			result.merge(checkNestedHierarchy(fField.getDeclaringType()));
			pm.worked(1);
			
			fReferences= null;
			if (fUpdateReferences){
				pm.setTaskName(RefactoringCoreMessages.getString("RenameFieldRefactoring.searching"));	 //$NON-NLS-1$
				fReferences= getReferences(new SubProgressMonitor(pm, 3));
				pm.setTaskName(RefactoringCoreMessages.getString("RenameFieldRefactoring.checking")); //$NON-NLS-1$
			} else {
				pm.worked(3);
			}	
			
			if (fUpdateReferences)
				result.merge(analyzeAffectedCompilationUnits());
				
			if (getGetter() != null && fRenameGetter){
				result.merge(checkAccessor(new SubProgressMonitor(pm, 1), getGetter(), getNewGetterName()));
				result.merge(Checks.checkIfConstructorName(getGetter(), getNewGetterName(), fField.getDeclaringType().getElementName()));
			} else {
				pm.worked(1);
			}
				
			if (getSetter() != null && fRenameSetter){
				result.merge(checkAccessor(new SubProgressMonitor(pm, 1), getSetter(), getNewSetterName()));
				result.merge(Checks.checkIfConstructorName(getSetter(), getNewSetterName(), fField.getDeclaringType().getElementName()));
			} else {
				pm.worked(1);
			}
			
			result.merge(createChanges(new SubProgressMonitor(pm, 10)));
			if (result.hasFatalError())
				return result;
			
			result.merge(validateModifiesFiles());
			return result;
		} finally{
			pm.done();
		}
	}
	
	//----------
	private RefactoringStatus checkAccessor(IProgressMonitor pm, IMethod existingAccessor, String newAccessorName) throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAccessorDeclarations(pm, existingAccessor));
		result.merge(checkNewAccessor(existingAccessor, newAccessorName));
		return result;
	}
	
	private RefactoringStatus checkNewAccessor(IMethod existingAccessor, String newAccessorName) throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		IMethod accessor= JavaModelUtil.findMethod(newAccessorName, existingAccessor.getParameterTypes(), false, fField.getDeclaringType());
		if (accessor == null || !accessor.exists())
			return null;
	
		String message= RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.already_exists", //$NON-NLS-1$
				new String[]{JavaElementUtil.createMethodSignature(accessor), JavaModelUtil.getFullyQualifiedName(fField.getDeclaringType())});
		result.addError(message, JavaStatusContext.create(accessor));
		return result;
	}
	
	private RefactoringStatus checkAccessorDeclarations(IProgressMonitor pm, IMethod existingAccessor) throws CoreException{
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
	
	private static boolean isInstaceField(IField field) throws CoreException{
		if (field.getDeclaringType().isInterface())
			return false;
		else 
			return ! JdtFlags.isStatic(field);
	}
	
	private RefactoringStatus checkNestedHierarchy(IType type) throws CoreException {
		IType[] nestedTypes= type.getTypes();
		if (nestedTypes == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();	
		for (int i= 0; i < nestedTypes.length; i++){
			IField otherField= nestedTypes[i].getField(getNewElementName());
			if (otherField.exists()){
				String msg= RefactoringCoreMessages.getFormattedString(
					"RenameFieldRefactoring.hiding", //$NON-NLS-1$
					new String[]{fField.getElementName(), getNewElementName(), JavaModelUtil.getFullyQualifiedName(nestedTypes[i])});
				result.addWarning(msg, JavaStatusContext.create(otherField));
			}									
			result.merge(checkNestedHierarchy(nestedTypes[i]));	
		}	
		return result;
	}
	
	private RefactoringStatus checkEnclosingHierarchy() {
		IType current= fField.getDeclaringType();
		if (Checks.isTopLevel(current))
			return null;
		RefactoringStatus result= new RefactoringStatus();
		while (current != null){
			IField otherField= current.getField(getNewElementName());
			if (otherField.exists()){
				String msg= RefactoringCoreMessages.getFormattedString("RenameFieldRefactoring.hiding2", //$NON-NLS-1$
				 															new String[]{getNewElementName(), JavaModelUtil.getFullyQualifiedName(current)});
				result.addWarning(msg, JavaStatusContext.create(otherField));
			}									
			current= current.getDeclaringType();
		}
		return result;
	}
	
	/*
	 * (non java-doc)
	 * Analyzes all compilation units in which type is referenced
	 */
	private RefactoringStatus analyzeAffectedCompilationUnits() throws CoreException{
		RefactoringStatus result= new RefactoringStatus();
		fReferences= Checks.excludeCompilationUnits(fReferences, result);
		if (result.hasFatalError())
			return result;
		
		result.merge(Checks.checkCompileErrorsInAffectedFiles(fReferences));	
		return result;
	}
	
	private IFile[] getAllFilesToModify() {
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() {
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	private ISearchPattern createSearchPattern(){
		return SearchEngine.createSearchPattern(fField, IJavaSearchConstants.REFERENCES);
	}
	
	private IJavaSearchScope createRefactoringScope() throws CoreException{
		return RefactoringScopeFactory.create(fField);
	}
	
	private SearchResultGroup[] getReferences(IProgressMonitor pm) throws CoreException{
		return RefactoringSearchEngine.search(pm, createRefactoringScope(), createSearchPattern());
	}
	
	// ---------- Changes -----------------

	/* non java-doc
	 * IRefactoring#createChange
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try{
			return new ValidationStateChange(RefactoringCoreMessages.getString("Change.javaChanges"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
		}	
	}
	
	//----------
	
	private RefactoringStatus createChanges(IProgressMonitor pm) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.getString("RenameFieldRefactoring.checking"), 10); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		fChangeManager= new TextChangeManager(true);

		addDeclarationUpdate();
		
		if (fUpdateReferences) {
			addReferenceUpdates(new SubProgressMonitor(pm, 1));
			result.merge(analyzeRenameChanges(new SubProgressMonitor(pm, 2)));
			if (result.hasFatalError())
				return result;
		} else {
			pm.worked(3);
		}
		
		if (getGetter() != null && fRenameGetter) {
			addGetterOccurrences(new SubProgressMonitor(pm, 1));
		} else {
			pm.worked(1);
		}
					
		if (getSetter() != null && fRenameSetter) {
			addSetterOccurrences(new SubProgressMonitor(pm, 1));
		} else {
			pm.worked(1);
		}

		if (fUpdateTextualMatches) {
			addTextMatches(new SubProgressMonitor(pm, 5));
		} else {
			pm.worked(5);
		}
		pm.done();
		return result;
	}

	private void addDeclarationUpdate() throws CoreException { 
		TextEdit textEdit= new ReplaceEdit(fField.getNameRange().getOffset(), fField.getElementName().length(), getNewElementName());
		ICompilationUnit cu= fField.getCompilationUnit();
		String groupName= RefactoringCoreMessages.getString("RenameFieldRefactoring.Update_field_declaration"); //$NON-NLS-1$
		TextChangeCompatibility.addTextEdit(fChangeManager.get(cu), groupName, textEdit);
	}
	
	private void addReferenceUpdates(IProgressMonitor pm) {
		pm.beginTask("", fReferences.length); //$NON-NLS-1$
		String editName= RefactoringCoreMessages.getString("RenameFieldRefactoring.Update_field_reference"); //$NON-NLS-1$
		for (int i= 0; i < fReferences.length; i++){
			ICompilationUnit cu= fReferences[i].getCompilationUnit();
			if (cu == null)
				continue;
			SearchResult[] results= fReferences[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				TextChangeCompatibility.addTextEdit(fChangeManager.get(cu), editName, createTextChange(results[j]));
			}
			pm.worked(1);			
		}
	}
	
	private TextEdit createTextChange(SearchResult searchResult) {
		String oldName= fField.getElementName();
		int offset= searchResult.getEnd() - oldName.length();
		return new ReplaceEdit(offset, oldName.length(), getNewElementName());
	}
	
	private void addGetterOccurrences(IProgressMonitor pm) throws CoreException {
		addAccessorOccurrences(pm, getGetter(), RefactoringCoreMessages.getString("RenameFieldRefactoring.Update_getter_occurrence"), getNewGetterName()); //$NON-NLS-1$
	}
	
	private void addSetterOccurrences(IProgressMonitor pm) throws CoreException {
		addAccessorOccurrences(pm, getSetter(), RefactoringCoreMessages.getString("RenameFieldRefactoring.Update_setter_occurrence"), getNewSetterName()); //$NON-NLS-1$
	}

	private void addAccessorOccurrences(IProgressMonitor pm, IMethod accessor, String editName, String newAccessorName) throws CoreException {
		Assert.isTrue(accessor.exists());
		
		IJavaSearchScope scope= RefactoringScopeFactory.create(accessor);
		ISearchPattern pattern= SearchEngine.createSearchPattern(accessor, IJavaSearchConstants.ALL_OCCURRENCES);
		SearchResultGroup[] groupedResults= RefactoringSearchEngine.search(
			scope, pattern, new MethodOccurenceCollector(pm, accessor.getElementName()));
		
		for (int i= 0; i < groupedResults.length; i++) {
			ICompilationUnit cu= groupedResults[i].getCompilationUnit();
			if (cu == null)
				continue;
			SearchResult[] results= groupedResults[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				SearchResult searchResult= results[j];
				TextEdit edit= new ReplaceEdit(searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), newAccessorName);
				TextChangeCompatibility.addTextEdit(fChangeManager.get(cu), editName, edit);
			}
		}
	}
	
	private void addTextMatches(IProgressMonitor pm) throws CoreException {
		TextMatchUpdater.perform(pm, createRefactoringScope(), this, fChangeManager, fReferences);
	}	
	
	//----------------
	private RefactoringStatus analyzeRenameChanges(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 2); //$NON-NLS-1$
			SearchResultGroup[] oldOccurrences= getOldOccurrences(new SubProgressMonitor(pm, 1));
			SearchResultGroup[] newOccurrences= getNewOccurrences(new SubProgressMonitor(pm, 1), fChangeManager);
			RefactoringStatus result= RenameAnalyzeUtil.analyzeRenameChanges(fChangeManager, oldOccurrences, newOccurrences);
			return result;
		} finally{
			pm.done();
			if (fNewWorkingCopies != null){
				for (int i= 0; i < fNewWorkingCopies.length; i++) {
					fNewWorkingCopies[i].destroy();
				}
			}	
		}
	}

	private SearchResultGroup[] getOldOccurrences(IProgressMonitor pm) throws CoreException {
		//TODO: repeats the search for fReferences, only to get the declaration too.
		ISearchPattern oldPattern= SearchEngine.createSearchPattern(fField, IJavaSearchConstants.ALL_OCCURRENCES);
		return RefactoringSearchEngine.search(pm, createRefactoringScope(), oldPattern);
	}
	
	private SearchResultGroup[] getNewOccurrences(IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		ICompilationUnit[] compilationUnitsToModify= manager.getAllCompilationUnits();
		fNewWorkingCopies= RenameAnalyzeUtil.getNewWorkingCopies(compilationUnitsToModify, manager, new SubProgressMonitor(pm, 1));
		
		ICompilationUnit declaringCuWorkingCopy= RenameAnalyzeUtil.findWorkingCopyForCu(fNewWorkingCopies, fField.getCompilationUnit());
		if (declaringCuWorkingCopy == null)
			return new SearchResultGroup[0];
		
		IField field= getNewField(declaringCuWorkingCopy);
		if (field == null || ! field.exists())
			return new SearchResultGroup[0];
		
		ISearchPattern newPattern= SearchEngine.createSearchPattern(field, IJavaSearchConstants.ALL_OCCURRENCES);			
		return RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), createRefactoringScope(), newPattern, fNewWorkingCopies);
	}

	private IField getNewField(ICompilationUnit newWorkingCopyOfDeclaringCu) throws CoreException{
		IType[] allNewTypes= newWorkingCopyOfDeclaringCu.getAllTypes();
		String fullyTypeName= fField.getDeclaringType().getFullyQualifiedName();
		for (int i= 0; i < allNewTypes.length; i++) {
			if (allNewTypes[i].getFullyQualifiedName().equals(fullyTypeName))
				return allNewTypes[i].getField(getNewElementName());
		}
		return null;
	}
	
}
