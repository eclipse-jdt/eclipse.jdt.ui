/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.link;

import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.Position;

public interface LinkedPositionListener {
	
	void exit(boolean success);
	void setCurrentPosition(Position position, int caretOffset);

	// XXX StyledText workaround
	void setReplace(IDocumentExtension.IReplace replace);
}
