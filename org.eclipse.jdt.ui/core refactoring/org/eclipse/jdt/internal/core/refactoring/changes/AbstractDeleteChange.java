/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.changes.*;
import org.eclipse.jdt.internal.core.refactoring.*;

abstract class AbstractDeleteChange extends Change {
	
	protected abstract void doDelete(IProgressMonitor pm) throws Exception;
	
	/* non java-doc
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		try {
			pm.beginTask("deleting", 1);
			if (!isActive())
				return;
			doDelete(new SubProgressMonitor(pm, 1));
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

