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

package org.eclipse.jdt.internal.ui.refactoring.changes;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.IChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A default implementation of <code>IChangeExceptionHandler</code> which
 * always aborts an change if an exception is caught.
 */
public class AbortChangeExceptionHandler implements IChangeExceptionHandler {
	
	public void handle(ChangeContext context, Change change, Exception e) {
		JavaPlugin.log(e);
		throw new ChangeAbortException(e);
	}
}
