/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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
		pm.beginTask("", fEdits.size());
		for (int i= fEdits.size() - 1; i >= 0; i--) {
			((TextEdit)fEdits.get(i)).perform(buffer);
			pm.worked(1);
		}
	}
	
	/* package */ void executed(IProgressMonitor pm) {
		pm.beginTask("", fEdits.size());
		for (int i= fEdits.size() - 1; i >= 0; i--) {
			((TextEdit)fEdits.get(i)).performed();
			pm.worked(1);
		}
	}
	
	/* package */ IStatus checkEdits(int bufferLength) {
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "Undo memento is valid", null);
	}
}

