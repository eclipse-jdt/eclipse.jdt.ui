/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.ui.views.framelist.TreeFrame;
import org.eclipse.ui.views.framelist.TreeViewerFrameSource;

class PackagesFrameSource extends TreeViewerFrameSource {
	private PackageExplorerPart fPackagesExplorer;
	
	PackagesFrameSource(PackageExplorerPart explorer) {
		super(explorer.getViewer());
		fPackagesExplorer= explorer;
	}

	protected TreeFrame createFrame(Object input) {
		TreeFrame frame = super.createFrame(input);
		frame.setToolTipText(fPackagesExplorer.getToolTipText(input));
		return frame;
	}

	/**
	 * Also updates the title of the packages explorer
	 */
	protected void frameChanged(TreeFrame frame) {
		super.frameChanged(frame);
		fPackagesExplorer.updateTitle();
	}

}
