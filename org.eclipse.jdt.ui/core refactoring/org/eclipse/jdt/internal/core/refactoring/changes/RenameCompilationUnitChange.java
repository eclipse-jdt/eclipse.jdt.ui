/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.changes;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.Assert;import org.eclipse.jdt.internal.core.refactoring.DebugUtils;import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.*;
import org.eclipse.jdt.internal.core.refactoring.changes.*;


public class RenameCompilationUnitChange extends AbstractJavaElementRenameChange {

	public RenameCompilationUnitChange(ICompilationUnit cu, String newName) throws JavaModelException{
		this(Refactoring.getResource(cu).getFullPath(), cu.getElementName(), newName);
		Assert.isTrue(!cu.isReadOnly(), RefactoringCoreMessages.getString("RenameCompilationUnitChange.assert.read_only")); //$NON-NLS-1$
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
		return RefactoringCoreMessages.getFormattedString("RenameCompilationUnitChange.name", new String[]{getOldName(), getNewName()}); //$NON-NLS-1$
	}
	
	/* non java-doc	 * @see AbstractRenameChange#createUndoChange()	 */	protected IChange createUndoChange() throws JavaModelException{		return new RenameCompilationUnitChange(createNewPath(), getNewName(), getOldName());	}
	
	protected void doRename(IProgressMonitor pm) throws JavaModelException {
		((ICompilationUnit)getModifiedLanguageElement()).rename(getNewName(), false, pm);
	}}
