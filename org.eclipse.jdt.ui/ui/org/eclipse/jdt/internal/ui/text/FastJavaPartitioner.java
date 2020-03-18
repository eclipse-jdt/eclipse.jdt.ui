/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text;

import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;

public class FastJavaPartitioner extends FastPartitioner {


	private boolean fIsPreviewEnabled= false;

	public FastJavaPartitioner(IPartitionTokenScanner scanner, String[] legalContentTypes) {
		super(scanner, legalContentTypes);
	}

	@Override
	protected void initialize() {
		super.initialize();
		fIsPreviewEnabled= isEnablePreviewsAllowed();
	}

	public void resetPositionCache() {
		clearPositionCache();
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent e) {
		super.documentAboutToBeChanged(e);
		if (hasPreviewEnabledValueChanged()) {
			clearManagingPositionCategory();
			connect(fDocument, false);
		}
	}

	@Override
	public ITypedRegion[] computePartitioning(int offset, int length, boolean includeZeroLengthPartitions) {
		if (hasPreviewEnabledValueChanged()) {
			clearManagingPositionCategory();
			connect(fDocument, false);
		}
		return super.computePartitioning(offset, length, includeZeroLengthPartitions);
	}

	public void cleanAndReConnectDocumentIfNecessary() {
		if (hasPreviewEnabledValueChanged()) {
			clearManagingPositionCategory();
			connect(fDocument, false);
		}
	}

	public boolean hasPreviewEnabledValueChanged() {
		boolean previewEnabledFlagChanged= false;
		boolean enablePreviewsAllowed= isEnablePreviewsAllowed();
		if (enablePreviewsAllowed != fIsPreviewEnabled) {
			previewEnabledFlagChanged= true;
		}
		return previewEnabledFlagChanged;
	}

	private boolean isEnablePreviewsAllowed() {
		boolean isEnablePreviewsAllowed= false;
		if (fScanner instanceof FastJavaPartitionScanner) {
			isEnablePreviewsAllowed= ((FastJavaPartitionScanner) fScanner).isEnablePreviewsAllowed();
		} else {
			isEnablePreviewsAllowed= false;
		}
		return isEnablePreviewsAllowed;
	}

	private void clearManagingPositionCategory() {
		String[] categories= getManagingPositionCategories();
		for (String category : categories) {
			try {
				fDocument.removePositionCategory(category);
			} catch (BadPositionCategoryException e) {
				// do nothing
			}
		}
		clearPositionCache();
	}
}
