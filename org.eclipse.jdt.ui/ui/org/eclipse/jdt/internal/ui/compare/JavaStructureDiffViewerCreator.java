/*
 * (c) Copyright IBM Corp. 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.compare.*;

/**
 * A factory object for the <code>JavaStructureDiffViewer</code>.
 * This indirection is necessary because only objects with a default
 * constructor can be created via an extension point
 * (this precludes Viewers).
 */
public class JavaStructureDiffViewerCreator implements IViewerCreator {
	
	public Viewer createViewer(Composite parent, CompareConfiguration cc) {
		return new JavaStructureDiffViewer(parent, cc);
	}
}