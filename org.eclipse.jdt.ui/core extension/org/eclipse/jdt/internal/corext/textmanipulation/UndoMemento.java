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
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IDocument;

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
	
	/* package */ void execute(IDocument document) throws PerformEditException {
		for (int i= fEdits.size() - 1; i >= 0; i--) {
			((TextEdit)fEdits.get(i)).perform(document);
		}
	}
	
	/* package */ boolean canPerform(int bufferLength) {
		for (Iterator iter= fEdits.iterator(); iter.hasNext();) {
			TextEdit edit= (TextEdit)iter.next();
			if (edit.getOffset() + edit.getLength() >= bufferLength)
				return false;
		}
		return true;
	}
}

