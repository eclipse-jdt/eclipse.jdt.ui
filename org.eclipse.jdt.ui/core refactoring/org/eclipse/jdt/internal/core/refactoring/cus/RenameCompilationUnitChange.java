/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.cus;import org.eclipse.core.runtime.IPath;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.Assert;import org.eclipse.jdt.internal.core.refactoring.DebugUtils;import org.eclipse.jdt.internal.core.refactoring.base.IChange;import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.*;
public class RenameCompilationUnitChange extends AbstractRenameChange {

	public RenameCompilationUnitChange(ICompilationUnit cu, String newName) throws JavaModelException{
		this(Refactoring.getResource(cu).getFullPath(), cu.getElementName(), newName);
		Assert.isTrue(!cu.isReadOnly(), "cu must not be read-only");
	}
	
	private RenameCompilationUnitChange(IPath resourcePath, String oldName, String newName){
		super(resourcePath, oldName, newName);
	}
	
	protected IPath createNewPath() throws JavaModelException{
		if (getResourcePath().getFileExtension() != null)
			return getResourcePath().removeFileExtension().removeLastSegments(1).append(getNewName());
		else	
			return getResourcePath().removeLastSegments(1).append(getNewName());
	}
	
	public String getName() {
		return "Rename compilation unit:" + getOldName() + " to:" + getNewName();
	}
	
	/**	 * @see AbstractRenameChange#createUndoChange()	 */	protected IChange createUndoChange() throws JavaModelException{		return new RenameCompilationUnitChange(createNewPath(), getNewName(), getOldName());	}}
