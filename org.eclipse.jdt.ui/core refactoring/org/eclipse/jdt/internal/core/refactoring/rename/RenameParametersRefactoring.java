/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.AbstractRefactoringASTAnalyzer;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;

public class RenameParametersRefactoring extends Refactoring implements IPreactivatedRefactoring{
	private String[] fNewParameterNames;
	private String[] fOldParameterNames;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	private boolean fUpdateReferences;
	private IMethod fMethod;
	
	public RenameParametersRefactoring(ITextBufferChangeCreator changeCreator, IMethod method){
		Assert.isNotNull(method);
		Assert.isNotNull(changeCreator);
		fMethod= method;
		setOldParameterNames();
		fTextBufferChangeCreator= changeCreator;
		fUpdateReferences= true;
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
	
	/**
	 * same as IRenameRefactoring#setUpdateReferences
	 */
	public void setUpdateReferences(boolean update){
		fUpdateReferences= update;
	}

	/**
	 * same as IRenameRefactoring#getUpdateReferences
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
		if (fOldParameterNames == null || fOldParameterNames.length == 0)
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
		if (fNewParameterNames == null || fNewParameterNames.length == 0)
			return new RefactoringStatus();
		RefactoringStatus result= new RefactoringStatus();
		if (fOldParameterNames.length != fNewParameterNames.length)
			result.addFatalError(RefactoringCoreMessages.getString("RenameParametersRefactoring.number_of_parameters")); //$NON-NLS-1$
		if (!anythingRenamed())
			result.addError(RefactoringCoreMessages.getString("RenameParametersRefactoring.no_change")); //$NON-NLS-1$
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
			if (Arrays.asList(getUnsavedFiles()).contains(Refactoring.getResource(fMethod)))
				result.addFatalError(RefactoringCoreMessages.getString("RenameParametersRefactoring.not_saved"));		 //$NON-NLS-1$
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
	
	private void setOldParameterNames(){
		if (fMethod.isBinary()) 
			return;
		try{
			fOldParameterNames= fMethod.getParameterNames();
		} catch (JavaModelException e){
			//ok to ignore - if this method does not exist, then the refactoring will not
			//be activated anyway
		}	
	}
	private void checkParameterNames(String[] newParameterNames) {
		Assert.isNotNull(fOldParameterNames, RefactoringCoreMessages.getString("RenameParametersRefactoring.assert.names_null")); //$NON-NLS-1$
		Assert.isTrue(newParameterNames.length > 0, RefactoringCoreMessages.getString("RenameParametersRefactoring.assert.one_parameter")); //$NON-NLS-1$
		Assert.isTrue(fOldParameterNames.length == newParameterNames.length, RefactoringCoreMessages.getString("RenameParametersRefactoring.assert.same_number"));	 //$NON-NLS-1$
		for (int i= 0; i < newParameterNames.length; i++){
			Assert.isNotNull(newParameterNames[i], RefactoringCoreMessages.getString("RenameParametersRefactoring.assert.name_null") + i); //$NON-NLS-1$
		}	
	}
	
	public void setNewParameterNames(String[] newParameterNames){
		checkParameterNames(newParameterNames);
		fNewParameterNames= newParameterNames;
	}
	
	private boolean anythingRenamed(){
		for (int i= 0; i < fNewParameterNames.length; i++){
			if (! fNewParameterNames[i].equals(fOldParameterNames[i]))
				return true;
		}
		return false;
	}
	
	private static String[] getSortedCopy(String[] array){
		//should we use arrayCopy?
		String[] copy= (String[])array.clone();
		Arrays.sort(copy);
		return copy;
	}
	
	private RefactoringStatus checkForDuplicateNames(){
		String[] sorted= getSortedCopy(fNewParameterNames);
		String last= null;
		for (int i= 0; i < sorted.length; i++){
			if (sorted[i].equals(last))
				return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getFormattedString("RenameParametersRefactoring.duplicate_name", last));//$NON-NLS-1$

			last= sorted[i];
		}
		return null;
	}
	
	private RefactoringStatus checkAllNames(){
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fNewParameterNames.length; i++){
			result.merge(Checks.checkFieldName(fNewParameterNames[i]));
			if (! Checks.startsWithLowerCase(fNewParameterNames[i]))
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
		AbstractRefactoringASTAnalyzer analyzer= new RenameParameterASTAnalyzer(fMethod, fNewParameterNames);
		return analyzer.analyze(fMethod.getCompilationUnit());
	}
	
	//-------- changes ----
	
	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		try{
			List renamed= getRenamedParameterIndices(fOldParameterNames, fNewParameterNames);
			pm.beginTask(RefactoringCoreMessages.getString("RenameParametersRefactoring.creating_change"), renamed.size()); //$NON-NLS-1$
			ITextBufferChange builder= fTextBufferChangeCreator.create(RefactoringCoreMessages.getString("RenameParametersRefactoring.rename_method_parameters"), fMethod.getCompilationUnit()); //$NON-NLS-1$
			for (Iterator iter= renamed.iterator(); iter.hasNext() ;){
				addParameterRenaming(((Integer)iter.next()).intValue(), builder);
				pm.worked(1);
			}
			return builder;
		} finally{
			pm.done();
		}	
	}
	
	private static List getRenamedParameterIndices(String[] oldNames, String[] newNames){
		List l= new ArrayList(oldNames.length);
		for (int i= 0; i < oldNames.length; i++){
			if (! oldNames[i].equals(newNames[i]))
				l.add(new Integer(i));
		}
		return l;
	}
	
	private void addParameterRenaming(int parameterIndex, ITextBufferChange builder) throws JavaModelException{
		int[] offsets= findParameterOccurrenceOffsets(parameterIndex);
		Assert.isTrue(offsets.length > 0); //at least the method declaration
		for (int i= 0; i < offsets.length; i++){
			addParameterRenameChange(parameterIndex, offsets[i], builder);
		};
	}
	
	private int[] findParameterOccurrenceOffsets(int parameterIndex) throws JavaModelException{
		return new ParameterOffsetFinder(fMethod, parameterIndex).findOffsets(fUpdateReferences);
	}
	
	private void addParameterRenameChange(int parameterIndex, int occurrenceOffset, ITextBufferChange builder){
		int length= fOldParameterNames[parameterIndex].length();
		String newName= fNewParameterNames[parameterIndex];
		String oldName= fOldParameterNames[parameterIndex];
		builder.addReplace(RefactoringCoreMessages.getString("RenameParametersRefactoring.update_reference"), occurrenceOffset, length, newName); //$NON-NLS-1$
	}
}