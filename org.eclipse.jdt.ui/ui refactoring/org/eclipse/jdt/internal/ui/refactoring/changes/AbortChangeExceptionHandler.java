/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.changes;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChangeExceptionHandler;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A default implementation of <code>IChangeExceptionHandler</code> which
 * always aborts an change if an exception is caught.
 */
public class AbortChangeExceptionHandler implements IChangeExceptionHandler {
	
	public void handle(ChangeContext context, IChange change, Exception e) {
		JavaPlugin.log(e);
		throw new ChangeAbortException(e);
	}
}
