/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.JavaPlugin;
/**
 * Class used to handle exceptions occurring during reorg actions.
 */
class ReorgExceptionHandler implements IChangeExceptionHandler{

	private MultiStatus fStatus;

	ReorgExceptionHandler() {
		String id = JavaPlugin.getDefault().getDescriptor().getUniqueIdentifier();
		fStatus = new MultiStatus(id, IStatus.OK, "Status", null);
	}

	public void handle(ChangeContext context, IChange change, Exception e) {
		if (e instanceof RuntimeException)
			throw (RuntimeException) e;
		if (e instanceof CoreException)
			fStatus.merge(((CoreException) e).getStatus());
	}
	
	MultiStatus getStatus(){
		return fStatus;
	}	
}
