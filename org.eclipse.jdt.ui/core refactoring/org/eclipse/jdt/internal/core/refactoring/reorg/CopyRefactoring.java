/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.reorg;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.changes.CopyCompilationUnitChange;
import org.eclipse.jdt.internal.core.refactoring.changes.CopyPackageChange;
import org.eclipse.jdt.internal.core.refactoring.changes.CopyResourceChange;

public class CopyRefactoring extends ReorgRefactoring {

	public CopyRefactoring(List elements){
		super(elements);
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Copy elements";
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public final RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}
	
	/* non java-doc
	 * @see ReorgRefactoring#isValidDestinationForCusAndFiles(Object)
	 */
	boolean isValidDestinationForCusAndFiles(Object dest) throws JavaModelException {
		return canCopyCusAndFiles(dest);
	}
	
	//-----
	
	IChange createChange(ICompilationUnit cu) throws JavaModelException{
		Object dest= getDestinationForCusAndFiles(getDestination());
		if (dest instanceof IPackageFragment)
			return new CopyCompilationUnitChange(cu, (IPackageFragment)dest);
		Assert.isTrue(dest instanceof IContainer);//this should be checked before - in preconditions
		return new CopyResourceChange(getResource(cu), (IContainer)dest);
	}
	
	IChange createChange(IPackageFragment pack) throws JavaModelException{
		return new CopyPackageChange(pack, getDestinationForPackages(getDestination()));
	}
	
	IChange createChange(IResource res) throws JavaModelException{
		return new CopyResourceChange(res, getDestinationForResources(getDestination()));
	}}

