package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * Navigator-specific action which switches the view's input
 * to be the currently selected container.
 * Enabled only when the current selection is a single container.
 */
/*
 * COPIED and MODIFIED from org.eclipse.ui.views.navigator
 */
/* package */ class ZoomInAction extends FrameAction {
	private PackageExplorerPart fExplorerPart;
public ZoomInAction(PackageExplorerPart explorer, FrameList frameList) {
	super(frameList);
	fExplorerPart= explorer;
	setText("Go Into");
	setToolTipText("Go Into");
	update();
}
protected Object getContainer() {
	TreeViewer viewer= fExplorerPart.getViewer();
	IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
	if (sel.size() == 1) {
		Object o = sel.getFirstElement();
		if (viewer.isExpandable(o)) {
			return o;
		}
	}
	return null;
}
/**
 * Calls <code>gotoFrame</code> on the frame list with a frame
 * representing the currently selected container.
 */
public void run() {
	Object container = getContainer();
	if (container != null) {
		TreeViewer viewer = fExplorerPart.getViewer();
		Frame frame = new PackageFrame(fExplorerPart, container, viewer.getSelection(), viewer.getExpandedElements());
		getFrameList().gotoFrame(frame);
	}
}
protected void updateEnabledState() {
	setEnabled(getContainer() != null);
}
}
