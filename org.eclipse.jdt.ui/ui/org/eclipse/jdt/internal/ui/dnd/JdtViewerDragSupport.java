/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.dnd;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.internal.ui.packageview.FileTransferDragAdapter;
import org.eclipse.jdt.internal.ui.packageview.SelectionTransferDragAdapter;

public class JdtViewerDragSupport {

	private final StructuredViewer fViewer;
	private JdtViewerDragAdapter fDragAdapter;
	private boolean fStarted;

	public JdtViewerDragSupport(StructuredViewer viewer) {
		fViewer= viewer;

		fDragAdapter= new JdtViewerDragAdapter(fViewer);
		fDragAdapter.addDragSourceListener(new SelectionTransferDragAdapter(fViewer));
		fDragAdapter.addDragSourceListener(new EditorInputTransferDragAdapter(viewer));
		fDragAdapter.addDragSourceListener(new ResourceTransferDragAdapter(fViewer));
		fDragAdapter.addDragSourceListener(new FileTransferDragAdapter(fViewer));

		fStarted= false;
	}

	public void start() {
		Assert.isLegal(!fStarted);

		int ops= DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;

		Transfer[] transfers= new Transfer[] {
			LocalSelectionTransfer.getTransfer(),
			ResourceTransfer.getInstance(),
			FileTransfer.getInstance()};

		fViewer.addDragSupport(ops, transfers, fDragAdapter);

		fStarted= true;
	}

}
