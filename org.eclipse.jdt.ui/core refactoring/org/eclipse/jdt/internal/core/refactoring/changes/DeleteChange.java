package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

abstract class DeleteChange extends Change {
	
	protected abstract void doDelete(IProgressMonitor pm) throws Exception;
	
	/**
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
	
	/**
	 * @see IChange#getUndoChange()
	 */
	public final IChange getUndoChange() {
		return new NullChange();
	}
	
	/**
	 * @see IChange#isUndoable()
	 */
	public final boolean isUndoable() {
		return false;
	}
}

