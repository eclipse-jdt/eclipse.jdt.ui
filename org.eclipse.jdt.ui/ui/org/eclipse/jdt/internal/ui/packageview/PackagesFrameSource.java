/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.ui.views.framelist.TreeFrame;
import org.eclipse.ui.views.framelist.TreeViewerFrameSource;

class PackagesFrameSource extends TreeViewerFrameSource {
	private PackageExplorerPart fPackagesExplorer;

	PackagesFrameSource(PackageExplorerPart explorer) {
		super(explorer.getTreeViewer());
		fPackagesExplorer= explorer;
	}

	@Override
	protected TreeFrame createFrame(Object input) {
		TreeFrame frame = super.createFrame(input);
		frame.setName(fPackagesExplorer.getFrameName(input));
		frame.setToolTipText(fPackagesExplorer.getToolTipText(input));
		return frame;
	}
}
