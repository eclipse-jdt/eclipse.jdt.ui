/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.textmanipulation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class encapsulates the reverse change of a number of <code>TextEdit</code>s
 * executed on a <code>TextBufferEditor</code>
 */
public final class UndoMemento {

	/* package */ int fMode;
	/* package */ List fEdits; 

	/* package */ UndoMemento(int mode) {
		fMode= mode;
		fEdits= new ArrayList(10);
	}
	
	/* package */ void add(TextEdit edit) {
		fEdits.add(edit);
	}	
}

