package org.eclipse.jdt.internal.core.refactoring.changes;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.cus.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class MoveRefactoring extends ReorgRefactoring {
	
	public MoveRefactoring(List elements){
		super(elements);
	}
	
	/**
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Move elements";
	}
	
	/**
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public final RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkReadOnlyStatus());
			return result;
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkReadOnlyStatus() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= getElements().iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (Checks.isReadOnly(each))
				result.addError("Selected element " + ReorgUtils.getName(each) + "(or one or its sub-elements) is marked as read-only.");
		}
		return result;
	}
	
	
	public boolean isValidDestination(Object dest) throws JavaModelException{
		if (dest instanceof IJavaProject)
			return isValidDestination(ReorgUtils.getPackageFragmentRoot((IJavaProject)dest));
		
		//only packages are selected
		if (hasPackages())	
			return canMovePackages(dest);
		
		//only resources are selected
		if (hasResources() && ! hasNonResources())	
			return canMoveResources(dest);
			
		return canMoveCusAndFiles(dest);
	}
	
	private boolean canMoveCusAndFiles(Object dest) throws JavaModelException{
		if (ReorgUtils.destinationIsParent(getElements(), getDestinationForCusAndFiles(dest)))
			return false;
		
		return canCopyCusAndFiles(dest);
	}
	
	private boolean canMoveResources(Object dest) throws JavaModelException{
		if (destinationIsParentForResources(getDestinationForResources(dest)))
			return false;
		return canCopyResources(dest);
	}
	
	private boolean canMovePackages(Object dest) throws JavaModelException{
		if (ReorgUtils.destinationIsParent(getElements(), getDestinationForPackages(dest)))
			return false;
		return canCopyPackages(dest);
	}
	
	private boolean destinationIsParentForResources(IContainer dest){
		if (dest == null)
			return false;
		for (Iterator iter= getElements().iterator(); iter.hasNext(); ){
			IResource resource= (IResource)iter.next();
			if (dest.equals(resource.getParent()))
				return true;
		}
		return false;
	}
	
	IChange createChange(ICompilationUnit cu) throws JavaModelException{
		return new MoveCompilationUnitChange(cu, getDestinationForCusAndFiles(getDestination()), getNewName(cu));
	}
	
	IChange createChange(IPackageFragment pack) throws JavaModelException{
		return new MovePackageChange(pack, getDestinationForPackages(getDestination()), getNewName(pack));
	}
	
	IChange createChange(IResource res) throws JavaModelException{
		return new MoveResourceChange(res, getDestinationForResources(getDestination()), getNewName(res));
	}
}

