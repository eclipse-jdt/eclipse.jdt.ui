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
package org.eclipse.jdt.internal.corext.textmanipulation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * This class encapsulates the reverse change of a number of <code>TextEdit</code>s
 * executed on a <code>TextBufferEditor</code>
 */
public final class UndoMemento {

	private List fEdits; 

	/* package */ UndoMemento() {
		fEdits= new ArrayList(5);
	}
	
	/* package */ void add(SimpleTextEdit edit) {
		fEdits.add(edit);
	}
	
	/* package */ void execute(TextBuffer buffer, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", fEdits.size()); //$NON-NLS-1$
		for (int i= fEdits.size() - 1; i >= 0; i--) {
			((TextEdit)fEdits.get(i)).perform(buffer);
			pm.worked(1);
		}
	}
	
	/* package */ void executed(IProgressMonitor pm) {
		pm.beginTask("", fEdits.size()); //$NON-NLS-1$
		for (int i= fEdits.size() - 1; i >= 0; i--) {
			((TextEdit)fEdits.get(i)).performed();
			pm.worked(1);
		}
	}
	
	/* package */ IStatus checkEdits(int bufferLength) {
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, TextManipulationMessages.getString("UndoMemento.is_valid"), null); //$NON-NLS-1$
	}
}

