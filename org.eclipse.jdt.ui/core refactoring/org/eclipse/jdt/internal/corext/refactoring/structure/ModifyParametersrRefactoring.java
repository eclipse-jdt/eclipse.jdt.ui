package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IMultiRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;

public class ModifyParametersrRefactoring extends Refactoring implements IMultiRenameRefactoring, IReferenceUpdatingRefactoring{
	
	private RenameParametersRefactoring fRenameParameters;
	private ReorderParametersRefactoring fReorderParameters;

	public ModifyParametersrRefactoring(IMethod method){
		fRenameParameters= new RenameParametersRefactoring(method);
		fReorderParameters= new ReorderParametersRefactoring(method);
	}

	public String getName() {
		return "Restructure Parameters";
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
		//it is enabled if any of the 2 is enabled
		RefactoringStatus result= fRenameParameters.checkPreactivation();
		if (result.isOK())
			return result;
		result.merge(fReorderParameters.checkPreactivation());
		return result;
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2);
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
			pm.beginTask("'Checking preconditions", 2);
			if (fRenameParameters.isInputSameAsInitial() && fReorderParameters.isInputSameAsInitial())
				return RefactoringStatus.createFatalErrorStatus("No parameters were renamed or reordered.");
			
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
			
			return result;
		} finally{
			pm.done();
		}
	}

	public IMethod getMethod() {
		return fRenameParameters.getMethod();
	}
	
	public String getMethodSignaturePreview() throws JavaModelException{
		StringBuffer buff= new StringBuffer();
		buff.append(getReturnTypeString())
			.append(getMethod().getElementName())
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
				buff.append(", "); 
			int newI= permutation[i];
			buff.append(Signature.toString(parameterTypes[newI]));
			buff.append(" ");
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

	//--  changes ----
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("Preparing preview", 2);
			TextChangeManager manager= new TextChangeManager();
			if (! fRenameParameters.isInputSameAsInitial())
				fRenameParameters.createChange(new SubProgressMonitor(pm, 1), manager);
			if (! fReorderParameters.isInputSameAsInitial())	
				fReorderParameters.createChange(new SubProgressMonitor(pm, 1), manager);
			return new CompositeChange("Restructure parameters", manager.getAllChanges());
		} finally{
			pm.done();
		}	
	}	
}
