/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

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
	public final void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("AbstractDeleteChange.deleting"), 1); //$NON-NLS-1$
			if (!isActive())
				return;
			doDelete(context, new SubProgressMonitor(pm, 1));
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

