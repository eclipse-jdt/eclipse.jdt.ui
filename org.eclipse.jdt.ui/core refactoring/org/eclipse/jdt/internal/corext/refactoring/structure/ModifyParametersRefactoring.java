package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;

public class ModifyParametersRefactoring extends Refactoring implements IReferenceUpdatingRefactoring{
	
	private RenameParametersRefactoring fRenameParameters;
	private ReorderParametersRefactoring fReorderParameters;
	private TextChangeManager fChangeManager;

	public ModifyParametersRefactoring(IMethod method){
		fRenameParameters= new RenameParametersRefactoring(method);
		fReorderParameters= new ReorderParametersRefactoring(method);
	}

	public String getName() {
		return RefactoringCoreMessages.getString("ModifyParamatersRefactoring.modify_Parameters"); //$NON-NLS-1$
	}
	
	public String[] getNewParameterOrder() {
		return fReorderParameters.getNewParameterOrder();
	}
	
	public int[] getParamaterPermutation() {
		return fReorderParameters.getParamaterPermutation();
	}
	
	public int getNewParameterPosition(String name){
		return fReorderParameters.getNewParameterPosition(name);
	}
	
	public void setNewParameterOrder(String[] newParameterOrder) {
		fReorderParameters.setNewParameterOrder(newParameterOrder);
	}
	
	public void setNewNames(Map renamings) {
		fRenameParameters.setNewNames(renamings);
	}

	public Map getNewNames() throws JavaModelException {
		return fRenameParameters.getNewNames();
	}

	public RefactoringStatus checkNewNames() throws JavaModelException {
		return fRenameParameters.checkNewNames();
	}

	public boolean canEnableUpdateReferences() {
		return fRenameParameters.canEnableUpdateReferences();
	}

	public void setUpdateReferences(boolean update) {
		fRenameParameters.setUpdateReferences(update);
	}

	public boolean getUpdateReferences() {
		return fRenameParameters.getUpdateReferences();
	}

	public RefactoringStatus checkPreactivation() throws JavaModelException{
		//XXX disable for constructors  - broken. see bug 23585
		if (fReorderParameters.getMethod().isConstructor())
			return RefactoringStatus.createFatalErrorStatus("This refactoring is not implemented for constructors");

		//it is enabled if any of the 2 is enabled
		RefactoringStatus result= fRenameParameters.checkPreactivation();
		if (result.isOK())
			return result;
		result.merge(fReorderParameters.checkPreactivation());
		return result;
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			RefactoringStatus result= fRenameParameters.checkActivation(new SubProgressMonitor(pm, 1));
			if (! result.isOK())
				return result;
			result.merge(fReorderParameters.checkActivation(new SubProgressMonitor(pm, 1)));
			return result;
		} finally{
			pm.done();
		}
	}

	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("ModifyParamatersRefactoring.checking_preconditions"), 2); //$NON-NLS-1$
			if (fRenameParameters.isInputSameAsInitial() && fReorderParameters.isInputSameAsInitial())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ModifyParamatersRefactoring.no_changes")); //$NON-NLS-1$
			
			RefactoringStatus result;
			if (! fRenameParameters.isInputSameAsInitial()){
				result= fRenameParameters.checkInput(new SubProgressMonitor(pm, 1));
				if (! result.isOK())
					return result;
				if (! fReorderParameters.isInputSameAsInitial())	
					result.merge(fReorderParameters.checkInput(new SubProgressMonitor(pm, 1)));	
			} else {
				 result= fReorderParameters.checkInput(new SubProgressMonitor(pm, 1));		
			}	 
			
			if (result.hasFatalError())
				return result;
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}

	public IMethod getMethod() {
		return fRenameParameters.getMethod();
	}
	
	public String getMethodSignaturePreview() throws JavaModelException{
		StringBuffer buff= new StringBuffer();

		if (! getMethod().isConstructor())
			buff.append(getReturnTypeString());

		buff.append(getMethod().getElementName())
			.append(Signature.C_PARAM_START)
			.append(getMethodParameters())
			.append(Signature.C_PARAM_END);
		return buff.toString();
	}

	private String getReturnTypeString() throws IllegalArgumentException, JavaModelException {
		StringBuffer buff= new StringBuffer();
		String returnType = Signature.getReturnType(getMethod().getSignature());
		if (returnType.length() != 0) {
			buff.append(Signature.toString(returnType))
				  .append(' ');
		}
		return buff.toString();
	}

	private String getMethodParameters() throws JavaModelException {
		StringBuffer buff= new StringBuffer();
		String[] parameterNames= getNewParameterNames(getMethod().getParameterNames(), fRenameParameters.getNewNames());
		String[] parameterTypes= getMethod().getParameterTypes();
		int[] permutation= fReorderParameters.getParamaterPermutation();
		for (int i= 0; i < parameterTypes.length; i++) {
			if (i != 0)
				buff.append(", ");  //$NON-NLS-1$
			int newI= permutation[i];
			buff.append(Signature.toString(parameterTypes[newI]));
			buff.append(" "); //$NON-NLS-1$
			buff.append(parameterNames[newI]);
		}
		return buff.toString();
	}
		
	private static String[] getNewParameterNames(String[] oldNames, Map oldToNewMap){
		String[] result= new String[oldNames.length];
		for (int i= 0; i < result.length; i++) {
			result[i]= (String)oldToNewMap.get(oldNames[i]);
		}
		return result;
	}

	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	//--  changes ----
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("ModifyParamatersRefactoring.preparing_preview"), 2); //$NON-NLS-1$
			return new CompositeChange(RefactoringCoreMessages.getString("ModifyParamatersRefactoring.restructure_parameters"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
		}	
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm) throws JavaModelException {
		TextChangeManager manager= new TextChangeManager();
		//the sequence here is critical 
		if (! fReorderParameters.isInputSameAsInitial())	
			fReorderParameters.createChange(new SubProgressMonitor(pm, 1), manager);
		if (! fRenameParameters.isInputSameAsInitial())
			fRenameParameters.createChange(new SubProgressMonitor(pm, 1), manager);
		return manager;
	}
	
}
