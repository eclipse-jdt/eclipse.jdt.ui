package org.eclipse.jdt.internal.core.refactoring.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class CopyRefactoring extends ReorgRefactoring {

	public CopyRefactoring(List elements){
		super(elements);
	}
	
	/**
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Copy elements";
	}
	
	/**
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
	
	public boolean isValidDestination(Object dest) throws JavaModelException{
		if (dest instanceof IJavaProject)
			return isValidDestination(ReorgUtils.getPackageFragmentRoot((IJavaProject)dest));
		
		//only packages are selected
		if (hasPackages())	
			return canCopyPackages(dest);
		
		//only resources are selected
		if (hasResources() && ! hasNonResources())	
			return canCopyResources(dest);
			
		return canCopyCusAndFiles(dest);
	}
	
	//-----
	
	IChange createChange(ICompilationUnit cu) throws JavaModelException{
		return new CopyCompilationUnitChange(cu, getDestinationForCusAndFiles(getDestination()), getNewName(cu));
	}
	
	IChange createChange(IPackageFragment pack) throws JavaModelException{
		return new CopyPackageChange(pack, getDestinationForPackages(getDestination()), getNewName(pack));
	}
	
	IChange createChange(IResource res) throws JavaModelException{
		return new CopyResourceChange(res, getDestinationForResources(getDestination()), getNewName(res));
	}
		
}

