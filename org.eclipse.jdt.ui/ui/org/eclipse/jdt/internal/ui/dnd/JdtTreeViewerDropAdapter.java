/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dnd;

import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.viewers.AbstractTreeViewer;

public class JdtTreeViewerDropAdapter extends JdtViewerDropAdapter {

	/**
	 * The time the mouse first started hovering over the current target
	 */
	protected long hoverStart= 0;
	/**
	 * The amount of time to hover over a tree item before expanding it
	 */
	private static final long HOVER_THRESHOLD= 1500;

	public JdtTreeViewerDropAdapter(AbstractTreeViewer viewer, int style) {
		super(viewer, style);
	}
	
	public void dragOver(DropTargetEvent event) {
		//this method implements the UI behaviour that when the user hovers 
		//over an unexpanded tree item long enough, it will auto-expand.
		Object oldTarget= fTarget;
		super.dragOver(event);
		if (oldTarget != fTarget) {
			hoverStart= System.currentTimeMillis();
		} else {
			//if we've been hovering over this item awhile, expand it.
			if (hoverStart > 0 && (System.currentTimeMillis() - hoverStart) > HOVER_THRESHOLD) {
				expandSelection((TreeItem) event.item);
				hoverStart= 0;
			}
		}
	}
	
	private void expandSelection(TreeItem item) {
		if (item == null)
			return;
		Object element= item.getData();
		if (element == null)
			return;
		((AbstractTreeViewer)getViewer()).expandToLevel(element, 1);
	}
}