/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.packages;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.NullChange;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenamePackageChange extends Change {

	private String fNewName;
	private String fOldName;
	private IPath fResourcePath;
	private IChange fUndoChange;
	
	public RenamePackageChange(IPackageFragment pack, String newName) throws JavaModelException{
		this(pack.getCorrespondingResource().getFullPath(), pack.getElementName(), newName);
		Assert.isTrue(!pack.isReadOnly(), "package must not be read-only");
	}
	
	private RenamePackageChange(IPath resourcePath, String oldName, String newName){
		Assert.isNotNull(newName, "new name");
		Assert.isNotNull(oldName, "old name");
		
		fResourcePath= resourcePath;
		fOldName= oldName;
		fNewName= newName;
	}
	
	private IResource getResource(){
		return ResourcesPlugin.getWorkspace().getRoot().findMember(fResourcePath);
	}
	
	public IJavaElement getCorrespondingJavaElement() {
		return JavaCore.create(getResource());
	}

	public String getName() {
		return "Rename package:" + fOldName + " to:" + fNewName;
	}

	public IChange getUndoChange() {
		return fUndoChange;
	}
	
	private IPath createPath(String packageName){
		return new Path(packageName.replace('.', IPath.SEPARATOR));
	}
	private IPath createNewPath(){
		IPackageFragment oldPackage= (IPackageFragment)getCorrespondingJavaElement();
		IPath oldPackageName= createPath(oldPackage.getElementName());
		IPath newPackageName= createPath(fNewName);
		return fResourcePath.removeLastSegments(oldPackageName.segmentCount()).append(newPackageName);
	}

	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("Renaming package ...", 1);
			if (isActive()){
				fUndoChange= new RenamePackageChange(createNewPath(), fNewName, fOldName);
				IPackageFragment pack= (IPackageFragment)getCorrespondingJavaElement();
				pack.rename(fNewName, false, pm);
			} else{
				fUndoChange= new NullChange();
			}
		} catch (Exception e) {
			handleException(context, e);
			fUndoChange= new NullChange();
			setActive(false);
		} finally {
			pm.done();
		}
	}
}