/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public class DeleteFileChange extends AbstractDeleteChange {

	private IPath fPath;
	
	public DeleteFileChange(IFile file){
		Assert.isNotNull(file, "file");  //$NON-NLS-1$
		fPath= Utils.getResourcePath(file);
	}
	
	private IFile getFile(){
		return Utils.getFile(fPath);
	}
	
	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("DeleteFileChange.1", fPath.lastSegment()); //$NON-NLS-1$
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return super.isValid(pm, false, true);
	}

	/* non java-doc
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedElement() {
		return getFile();
	}

	/* non java-doc
	 * @see DeleteChange#doDelete(IProgressMonitor)
	 */
	protected void doDelete(IProgressMonitor pm) throws CoreException {
		IFile file= getFile();
		Assert.isNotNull(file);
		Assert.isTrue(file.exists());
		try {
			file.delete(false, true, pm);
		} catch (CoreException e) {
			// the problem is that there isn't any implementation of IReorgExceptionHandler.
			// So the code got never executed. Have to check what to do here.
			
//			if (! (context.getExceptionHandler() instanceof IReorgExceptionHandler))
//				throw e;
//			IReorgExceptionHandler handler= (IReorgExceptionHandler)context.getExceptionHandler();
//			IStatus[] children= e.getStatus().getChildren();
//			if (children.length == 1 && children[0].getCode() == IResourceStatus.OUT_OF_SYNC_LOCAL){
//				if (handler.forceDeletingResourceOutOfSynch(file.getName(), e)){
//					file.delete(true, true, pm);
//					return;
//				}	else
//						return; //do not rethrow in this case
//			} else
			throw e;
		}
	}
}

