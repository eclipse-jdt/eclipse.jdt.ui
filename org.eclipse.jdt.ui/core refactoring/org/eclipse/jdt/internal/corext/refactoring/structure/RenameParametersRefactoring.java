/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextBufferChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.ProblemNodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IMultiRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

class RenameParametersRefactoring extends Refactoring implements IMultiRenameRefactoring, IReferenceUpdatingRefactoring{

	private Map fRenamings;
	private boolean fUpdateReferences;
	private IMethod fMethod;
	
	public RenameParametersRefactoring(IMethod method){
		Assert.isNotNull(method);
		fMethod= method;
		setOldParameterNames();
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
	 * @see IRefactoring#getName
	 */
	public String getName(){
		return RefactoringCoreMessages.getString("RenameParametersRefactoring.rename_parameters"); //$NON-NLS-1$
	}
	
	public IMethod getMethod(){
		return fMethod;
	}
	
	/*
	 * @see IReferenceUpdatingRefactoring#canEnableUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		return true;
	}
	
	/*
	 *@see  IReferenceUpdatingRefactoring#setUpdateReferences
	 */
	public void setUpdateReferences(boolean update){
		fUpdateReferences= update;
	}

	/*
	 * @see  IReferenceUpdatingRefactoring#getUpdateReferences
	 */	
	public boolean getUpdateReferences(){
		return fUpdateReferences;
	}
	
	//-- preconditions
	
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkAvailability(fMethod));
		if (fRenamings == null || fRenamings.isEmpty())
			result.addFatalError(RefactoringCoreMessages.getString("RenameParametersRefactoring.no_parameters"));  //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc 
	 * @see Refactoring#getActivation
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		IMethod orig= (IMethod)WorkingCopyUtil.getOriginal(fMethod);
		if (orig == null || ! orig.exists()){
			String message= RefactoringCoreMessages.getFormattedString("RenameParametersRefactoring.deleted", fMethod.getCompilationUnit().getElementName());//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fMethod= orig;
		
		return Checks.checkIfCuBroken(fMethod);
	}

	public RefactoringStatus checkNewNames(){
		if (fRenamings == null || fRenamings.isEmpty())
			return new RefactoringStatus();
		RefactoringStatus result= new RefactoringStatus();
		if (result.isOK())
			result.merge(checkForDuplicateNames());
		if (result.isOK())	
			result.merge(checkAllNames());
		return result;
	}
	
	public boolean isInputSameAsInitial(){
		return ! isAnythingRenamed();
	}
	
	/* non java-doc 
	 * @see Refactoring#getInput
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		try{
			RefactoringStatus result= new RefactoringStatus();
			pm.beginTask("", 10); //$NON-NLS-1$
			result.merge(Checks.checkIfCuBroken(fMethod));
			if (result.hasFatalError())
				return result;
			pm.subTask(RefactoringCoreMessages.getString("RenameParametersRefactoring.checking")); //$NON-NLS-1$
			result.merge(checkNewNames());
			if (result.hasFatalError())
				return result;
			pm.worked(3);
			if (fUpdateReferences && mustAnalyzeAst()) 
				result.merge(analyzeAst()); 
			pm.worked(7);
			return result;
		} finally{
			pm.done();
		}	
	}

	/*
	 * @see IMultiRenameRefactoring#setNewNames(Map)
	 */
	public void setNewNames(Map renamings) {
		Assert.isNotNull(renamings);
		fRenamings= renamings;
	}

	/*
	 * @see IMultiRenameRefactoring#getNewNames()
	 */
	public Map getNewNames() {
		return fRenamings;
	}
	
	private void setOldParameterNames(){
		if (fMethod.isBinary()) 
			return;
		try{
			String[] oldNames= fMethod.getParameterNames();
			fRenamings= new HashMap();
			for (int i= 0; i <oldNames.length; i++) {
				fRenamings.put(oldNames[i], oldNames[i]);
			}
		} catch (JavaModelException e){
			//ok to ignore - if this method does not exist, then the refactoring will not
			//be activated anyway
		}	
	}
	
	private boolean isAnythingRenamed(){
		for (Iterator iterator = fRenamings.keySet().iterator(); iterator.hasNext();) {
			String oldName = (String) iterator.next();
			if (! getNewName(oldName).equals(oldName))
				return true;	
		}
		return false;
	}
	
	private String getNewName(String oldName){
		if (fRenamings.containsKey(oldName))
			return (String)fRenamings.get(oldName);
		else
			return oldName;	
	}
	
	private RefactoringStatus checkForDuplicateNames(){
		RefactoringStatus result= new RefactoringStatus();
		Set found= new HashSet();
		Set doubled= new HashSet();
		for (Iterator iterator = fRenamings.keySet().iterator(); iterator.hasNext();) {
			String oldName= (String) iterator.next();
			String newName= getNewName(oldName);
			if (found.contains(newName) && !doubled.contains(newName)){
				result.addFatalError(RefactoringCoreMessages.getFormattedString("RenameParametersRefactoring.duplicate_name", newName));//$NON-NLS-1$	
				doubled.add(newName);
			} else {
				found.add(newName);
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAllNames(){
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iterator = fRenamings.keySet().iterator(); iterator.hasNext();) {
			String oldName = (String) iterator.next();
			String newName= getNewName(oldName);
			result.merge(Checks.checkFieldName(newName));	
			if (! Checks.startsWithLowerCase(newName))
				result.addWarning(RefactoringCoreMessages.getString("RenameParametersRefactoring.should_start_lowercase")); //$NON-NLS-1$
		}
		return result;			
	}
	
	private boolean mustAnalyzeAst() throws JavaModelException{
		if (JdtFlags.isAbstract(fMethod))
			return false;
		else if (JdtFlags.isNative(fMethod))
			return false;
		else if (fMethod.getDeclaringType().isInterface())
			return false;
		else 
			return true;
	}

	private ICompilationUnit getCu() {
		return fMethod.getCompilationUnit();
	}
	
	private RefactoringStatus analyzeAst() throws JavaModelException{		
		ICompilationUnit wc= null;
		try {
			RefactoringStatus result= new RefactoringStatus();
						
			Map map= getEditMapping();
			TextEdit[] allEdits= getAllEdits(map);
			
			CompilationUnit compliationUnitNode= AST.parseCompilationUnit(getCu(), true);
			TextChange change= new TextBufferChange(RefactoringCoreMessages.getString("RenameParametersRefactoring.rename_Paremeters"), TextBuffer.create(getCu().getSource())); //$NON-NLS-1$
			change.setTrackPositionChanges(true);
		
			wc= RefactoringAnalyzeUtil.getWorkingCopyWithNewContent(allEdits, change, getCu());
			CompilationUnit newCUNode= AST.parseCompilationUnit(wc, true);
			
			result.merge(RefactoringAnalyzeUtil.analyzeIntroducedCompileErrors(change, wc, newCUNode, compliationUnitNode));
			if (result.hasError())
				return result;

			String[] oldNames= getRenamedParameterNames();				
			for (int i= 0; i < oldNames.length; i++) {
				TextEdit[] paramRenameEdits= (TextEdit[])map.get(oldNames[i]);
				String fullKey= RefactoringAnalyzeUtil.getFullDeclarationBindingKey(paramRenameEdits, compliationUnitNode);
				MethodDeclaration methodNode= RefactoringAnalyzeUtil.getMethodDeclaration(allEdits[0], change, newCUNode);
				SimpleName[] problemNodes= ProblemNodeFinder.getProblemNodes(methodNode, paramRenameEdits, change, fullKey);
				result.merge(RefactoringAnalyzeUtil.reportProblemNodes(wc, problemNodes));
			}
			return result;
		} catch(CoreException e) {
			throw new JavaModelException(e);
		} finally{
			if (wc != null)
				wc.destroy();
		}
	}

	//String -> TextEdit[]
	private Map getEditMapping() throws JavaModelException {
		String[] oldNames= getRenamedParameterNames();	
		Map map= new HashMap();
		for (int i= 0; i < oldNames.length; i++) {
			TextEdit[] paramRenameEdits= getParameterRenameEdits(oldNames[i]);
			map.put(oldNames[i], paramRenameEdits);
		}
		return map;
	}
	
	//String -> TextEdit[]
	private static TextEdit[] getAllEdits(Map map){
		Collection result= new ArrayList();
		for (Iterator iter= map.keySet().iterator(); iter.hasNext();) {
			TextEdit[] array= (TextEdit[])map.get(iter.next());
			result.addAll(Arrays.asList(array));	
		}
		return (TextEdit[]) result.toArray(new TextEdit[result.size()]);
	}
	
	//-------- changes ----

	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		TextChangeManager manager= new TextChangeManager();
		createChange(pm, manager);
		return new CompositeChange(RefactoringCoreMessages.getString("RenameParametersRefactoring.rename_Parameters"), manager.getAllChanges()); //$NON-NLS-1$
	}

	public void createChange(IProgressMonitor pm, TextChangeManager manager) throws JavaModelException {
		try{
			TextChange change= manager.get(WorkingCopyUtil.getWorkingCopyIfExists(fMethod.getCompilationUnit()));
			TextEdit[] edits= getAllRenameEdits();
			pm.beginTask(RefactoringCoreMessages.getString("RenameParametersRefactoring.preview"), edits.length);  //$NON-NLS-1$
			for (int i= 0; i < edits.length; i++) {
				change.addTextEdit(RefactoringCoreMessages.getString("RenameParametersRefactoring.rename_method_parameter"), edits[i]); //$NON-NLS-1$
			}
		}catch (CoreException e)	{
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}	
	}
	
	private TextEdit[] getAllRenameEdits() throws JavaModelException {
		Collection edits= new ArrayList();
		String[] renamed= getRenamedParameterNames();
		for (int i= 0; i < renamed.length; i++) {
			edits.addAll(Arrays.asList(getParameterRenameEdits(renamed[i])));
		}
		return (TextEdit[]) edits.toArray(new TextEdit[edits.size()]);
	}
		
	private String[] getRenamedParameterNames(){
		Set result= new HashSet();
		for (Iterator iterator = fRenamings.keySet().iterator(); iterator.hasNext();) {
			String oldName = (String) iterator.next();
			String newName= getNewName(oldName);
			if (! oldName.equals(newName))
				result.add(oldName);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	private TextEdit[] getParameterRenameEdits(String oldParameterName) throws JavaModelException{
		Collection edits= new ArrayList(); 
		int[] offsets= ParameterOffsetFinder.findOffsets(fMethod, oldParameterName, fUpdateReferences);
		Assert.isTrue(offsets.length > 0); //at least the method declaration
		for (int i= 0; i < offsets.length; i++){
			edits.add(getParameterRenameEdit(oldParameterName, offsets[i]));
		};
		return (TextEdit[]) edits.toArray(new TextEdit[edits.size()]);
	}	
	
	private TextEdit getParameterRenameEdit(String oldParameterName, int occurrenceOffset){
		return SimpleTextEdit.createReplace(occurrenceOffset, oldParameterName.length(), getNewName(oldParameterName));
	}
	
}