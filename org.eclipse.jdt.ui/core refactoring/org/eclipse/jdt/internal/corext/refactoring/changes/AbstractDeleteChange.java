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
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;

abstract class AbstractDeleteChange extends Change {
	
	protected abstract void doDelete(ChangeContext context, IProgressMonitor pm) throws CoreException;
	
	/* non java-doc
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm) throws ChangeAbortException, CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("AbstractDeleteChange.deleting"), 1); //$NON-NLS-1$
			if (!isActive())
				return;
			doDelete(context, pm);
		} catch (Exception e) {
			handleException(context, e);
			setActive(false);
		} finally{
			pm.done();
		}
	}
	
	/* non java-doc
	 * @see IChange#getUndoChange()
	 */
	public final IChange getUndoChange() {
		return new NullChange();
	}
	
	/* non java-doc
	 * @see IChange#isUndoable()
	 */
	public final boolean isUndoable() {
		return false;
	}
}

