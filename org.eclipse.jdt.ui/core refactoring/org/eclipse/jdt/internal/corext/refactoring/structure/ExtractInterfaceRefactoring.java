package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public class ExtractInterfaceRefactoring extends Refactoring {

	private IType fClass;
	
	public ExtractInterfaceRefactoring(IType clazz){
		Assert.isNotNull(clazz);
		fClass= clazz;
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		return new RefactoringStatus();
	}

	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		return new RefactoringStatus();
	}

	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		return new NullChange();
	}

	public String getName() {
		return "Extract Interface";
	}
}
