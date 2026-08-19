package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Statement;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

public class ChangeRecordSignatureProcessor extends RefactoringProcessor implements IDelegateUpdating{

	IType fType;

	ASTNode fNode;

	ClassInstanceCreation fClassInstanceCreation;

	private List<ParameterInfo> fParameterInfos;

	private int fVisibility;

	public ChangeRecordSignatureProcessor(IType type, ASTNode node) {
		this.fType = type;
		this.fNode = node;
		this.fClassInstanceCreation= resolveClassInstanceCreation(node);
		this.fVisibility= JdtFlags.getVisibilityCode(this.fClassInstanceCreation.getType().resolveBinding());
		if (node != null) {
			this.fParameterInfos = getTypeParameters(this.fClassInstanceCreation);
		}
	}

	private List<ParameterInfo> getTypeParameters(ClassInstanceCreation cic) {
		IMethodBinding mbinding = cic.resolveConstructorBinding();
		if (mbinding == null) mbinding = cic.resolveConstructorBinding();
		ITypeBinding[] parametersTypes = mbinding.getParameterTypes();
		String[] parametersNames = mbinding.getParameterNames();
		parametersNames.clone();
		parametersTypes.clone();
		List<ParameterInfo> result= new ArrayList<>(parametersTypes.length);
		for (int i= 0; i < parametersTypes.length; i++) {
			ParameterInfo parameterInfo;
			//We don't have  var args for record parameters so we don't need to check them.
			parameterInfo= new ParameterInfo(parametersTypes[i].getName(), parametersNames[i], i);
			result.add(parameterInfo);
		}
		return result;
	}

	private ClassInstanceCreation resolveClassInstanceCreation(ASTNode node) {
		if (node instanceof ClassInstanceCreation) return (ClassInstanceCreation)node;
		else {
			if(node == null || node instanceof Statement || node instanceof CompilationUnit || node instanceof BodyDeclaration ) {
				return null;
			}
		}
		return resolveClassInstanceCreation(node.getParent());
	}

	@Override
	public boolean canEnableDelegateUpdating() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getDelegateUpdating() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDelegateUpdatingTitle(boolean plural) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getDeprecateDelegates() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDelegateUpdating(boolean updating) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDeprecateDelegates(boolean deprecate) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object[] getElements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProcessorName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		pm.beginTask("", 2); //$NON-NLS-1$
		RefactoringStatus result= Checks.checkIfCuBroken(fType);
		if (result.hasFatalError()) {
			return result;
		}
		pm.worked(1);
		if (fClassInstanceCreation == null) {
			return null;
		}
		pm.worked(2);
		return result;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

}
