/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.packages;

import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.Path;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.AbstractRenameChange;import org.eclipse.jdt.internal.core.refactoring.Assert;import org.eclipse.jdt.internal.core.refactoring.base.IChange;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenamePackageChange extends AbstractRenameChange {

	public RenamePackageChange(IPackageFragment pack, String newName) throws JavaModelException{
		this(pack.getCorrespondingResource().getFullPath(), pack.getElementName(), newName);
		Assert.isTrue(!pack.isReadOnly(), "package must not be read-only");
	}
	
	private RenamePackageChange(IPath resourcePath, String oldName, String newName){
		super(resourcePath, oldName, newName);
	}
		
	protected IPath createPath(String packageName){
		return new Path(packageName.replace('.', IPath.SEPARATOR));
	}
	
	protected IPath createNewPath(){
		IPackageFragment oldPackage= (IPackageFragment)getCorrespondingJavaElement();
		IPath oldPackageName= createPath(oldPackage.getElementName());
		IPath newPackageName= createPath(getNewName());
		return getResourcePath().removeLastSegments(oldPackageName.segmentCount()).append(newPackageName);
	}
	
	public String getName() {
		return "Rename package:" + getOldName() + " to:" + getNewName();
	}

	/**
	 * @see AbstractRenameChange#createUndoChange()
	 */
	protected IChange createUndoChange() {
		return new RenamePackageChange(createNewPath(), getNewName(), getOldName());
	}
}