/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.refactoring.AbstractRefactoringASTAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IMultiRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;

public class RenameParametersRefactoring extends Refactoring implements IMultiRenameRefactoring, IReferenceUpdatingRefactoring{

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
	
	/* non java-doc 
	 * @see IPreactivatedRefactoring#getPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(fMethod));
		if (fRenamings == null || fRenamings.isEmpty())
			result.addFatalError(RefactoringCoreMessages.getString("RenameParametersRefactoring.no_parameters"));  //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc 
	 * @see Refactoring#getActivation
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		return Checks.checkIfCuBroken(fMethod);
	}

	public RefactoringStatus checkNewNames(){
		if (fRenamings == null || fRenamings.isEmpty())
			return new RefactoringStatus();
		RefactoringStatus result= new RefactoringStatus();
		if (!anythingRenamed())
			result.addFatalError(RefactoringCoreMessages.getString("RenameParametersRefactoring.no_change")); //$NON-NLS-1$
		if (result.isOK())
			result.merge(checkForDuplicateNames());
		if (result.isOK())	
			result.merge(checkAllNames());
		return result;
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
			if (result.hasFatalError())
				return result;	
			pm.subTask(RefactoringCoreMessages.getString("RenameParametersRefactoring.checking")); //$NON-NLS-1$
			result.merge(checkNewNames());
			pm.worked(3);
			/*
			 * only one resource is affected - no need to check its availability
			 * (done in MethodRefactoring::checkActivation)
			 */
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
	
	private boolean anythingRenamed(){
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
				result.addError(RefactoringCoreMessages.getFormattedString("RenameParametersRefactoring.duplicate_name", newName));//$NON-NLS-1$	
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
		int flags= fMethod.getFlags();
		if (Flags.isAbstract(flags))
			return false;
		else if (Flags.isNative(flags))
			return false;
		else if (fMethod.getDeclaringType().isInterface())
			return false;
		else 
			return true;
	}
	
	private RefactoringStatus analyzeAst() throws JavaModelException{		
		AbstractRefactoringASTAnalyzer analyzer= new RenameParameterASTAnalyzer(fMethod, fRenamings);
		return analyzer.analyze(fMethod.getCompilationUnit());
	}
	
	//-------- changes ----
	
	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		try{
			String[] renamed= getRenamedParameterNames();
			pm.beginTask(RefactoringCoreMessages.getString("RenameParametersRefactoring.creating_change"), renamed.length); //$NON-NLS-1$
			String name= RefactoringCoreMessages.getString("RenameParametersRefactoring.rename_method_parameters");
			TextChange change= new CompilationUnitChange(name, fMethod.getCompilationUnit());
			
			for (int i = 0; i < renamed.length; i++) {
				addParameterRenaming(renamed[i], change);
				pm.worked(1);
			}
			return change;
		}catch (CoreException e)	{
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}	
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
	
	private void addParameterRenaming(String oldParameterName, TextChange change) throws JavaModelException{
		int[] offsets= findParameterOccurrenceOffsets(oldParameterName);
		Assert.isTrue(offsets.length > 0); //at least the method declaration
		for (int i= 0; i < offsets.length; i++){
			addParameterRenameChange(oldParameterName, offsets[i], change);
		};
	}	
	
	private int[] findParameterOccurrenceOffsets(String oldParameterName) throws JavaModelException{
		return ParameterOffsetFinder.findOffsets(fMethod, oldParameterName, fUpdateReferences);
	}
	
	private void addParameterRenameChange(String oldParameterName, int occurrenceOffset, TextChange change){
		String name=  RefactoringCoreMessages.getString("RenameParametersRefactoring.update_reference");//$NON-NLS-1$
		change.addTextEdit(name, SimpleTextEdit.createReplace(occurrenceOffset, oldParameterName.length(), getNewName(oldParameterName)));
	}
}