/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - bug fixes
 *     Brock Janiczak <brockj@tpg.com.au> - [JUnit] Add context menu action to import junit test results from package explorer - https://bugs.eclipse.org/bugs/show_bug.cgi?id=213786
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.ui.IEditorLauncher;

import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.util.ExceptionHandler;

public class JUnitViewEditorLauncher implements IEditorLauncher {

	public void open(IPath file) {
		try {
			JUnitPlugin.getActivePage().showView(TestRunnerViewPart.NAME);
			JUnitModel.importTestRunSession(file.toFile());
		} catch (CoreException e) {
			ExceptionHandler.handle(e, JUnitMessages.JUnitViewEditorLauncher_dialog_title, JUnitMessages.JUnitViewEditorLauncher_error_occurred);
		}
	}

}
