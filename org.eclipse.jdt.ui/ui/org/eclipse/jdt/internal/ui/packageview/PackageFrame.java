/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;


/* package */ class PackageFrame extends Frame {
	private PackageExplorerPart fPackageExplorer;
	private Object fInput;
	private ISelection fSelection;
	private Object[] fExpandedElements;
	
public PackageFrame(PackageExplorerPart packages, Object input, ISelection selection, Object[] expandedElements) {
	fPackageExplorer= packages;
	fInput= input;
	fSelection= selection;
	fExpandedElements= expandedElements;
}

public Object[] getExpandedElements() {
	return fExpandedElements;
}
public Object getInput() {
	return fInput;
}
public ISelection getSelection() {
	return fSelection;
}
public String getToolTipText() {
	return fPackageExplorer.getToolTipText(fInput);
}
}
