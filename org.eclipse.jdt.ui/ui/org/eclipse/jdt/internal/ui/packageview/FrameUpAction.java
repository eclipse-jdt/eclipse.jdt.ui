/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Navigator-specific action which switches the view's input
 * to be the parent of the current input.
 * Enabled only when the current input has a parent.
 */
/* 
 * COPIED and MODIFIED from org.eclipse.ui.views.navigator
 */
/* package */ class FrameUpAction extends FrameAction {
	private PackageExplorerPart fPackageExplorer;
public FrameUpAction(PackageExplorerPart explorer, FrameList frameList) {
	super(frameList);
	fPackageExplorer= explorer;
	setText("Up One Level");
	setToolTipText("Up");
	update();
}
protected Object getParent() {
	Object input= fPackageExplorer.getViewer().getInput();
	/*
	 * don't use the Adapter but ask viewer's ContentProvider
	 */
	/*if (input instanceof IAdaptable) {
		IAdaptable adaptable= (IAdaptable) input;
		IWorkbenchAdapter adapter = (IWorkbenchAdapter) adaptable.getAdapter(IWorkbenchAdapter.class);
		if (adapter != null) {
			return adapter.getParent(input);
		}
	}
	return null;
	*/
	ITreeContentProvider provider= (ITreeContentProvider)fPackageExplorer.getViewer().getContentProvider();
	return provider.getParent(input);
}
/**
 * Calls <code>gotoFrame</code> on the frame list with a frame
 * representing the parent of the current input.
 */
public void run() {
	Object parent = getParent();
	if (parent != null) {
		TreeViewer viewer= fPackageExplorer.getViewer();
		// include current input in expanded set
		Object[] expanded = viewer.getExpandedElements();
		int len = expanded.length;
		System.arraycopy(expanded, 0, expanded = new Object[len+1], 0, len);
		expanded[len]= viewer.getInput();
		Frame frame= new PackageFrame(fPackageExplorer, parent, viewer.getSelection(), expanded);
		getFrameList().gotoFrame(frame);
	}
}
protected void updateEnabledState() {
	setEnabled(getParent() != null);
}
protected void updateToolTip() {
	Object parent = getParent();
	if (parent != null) {
		String text = fPackageExplorer.getToolTipText(parent);
		if (text != null && text.length() > 0) {
			setToolTipText("Up to " + text);
			return;
		}
	}
	setToolTipText("Up");
}
}
