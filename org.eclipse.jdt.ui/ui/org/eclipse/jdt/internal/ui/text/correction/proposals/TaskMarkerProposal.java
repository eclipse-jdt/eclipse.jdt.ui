/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;

public class TaskMarkerProposal extends CUCorrectionProposal {
	public TaskMarkerProposal(ICompilationUnit cu, IProblemLocationCore location, int relevance) {
		super("", cu, relevance, null); //$NON-NLS-1$
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		setDelegate(new TaskMarkerProposalCore(cu, location, relevance));
	}
}