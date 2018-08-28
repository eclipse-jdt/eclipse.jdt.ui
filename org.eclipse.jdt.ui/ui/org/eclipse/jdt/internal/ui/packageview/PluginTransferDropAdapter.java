/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.part.PluginDropAdapter;
import org.eclipse.ui.part.PluginTransfer;

public class PluginTransferDropAdapter extends PluginDropAdapter implements TransferDropTargetListener {

	public PluginTransferDropAdapter(StructuredViewer viewer) {
		super(viewer);
		setFeedbackEnabled(false);
	}

	@Override
	public Transfer getTransfer() {
		return PluginTransfer.getInstance();
	}

	@Override
	public boolean isEnabled(DropTargetEvent event) {
		return PluginTransfer.getInstance().isSupportedType(event.currentDataType);
	}

}
