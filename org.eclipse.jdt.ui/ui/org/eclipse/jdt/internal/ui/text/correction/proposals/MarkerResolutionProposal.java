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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IMarker;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;

public class MarkerResolutionProposal implements IJavaCompletionProposal {

	private IMarkerResolution fResolution;
	private IMarker fMarker;

	/**
	 * Constructor for MarkerResolutionProposal.
	 * @param resolution the marker resolution
	 * @param marker the marker
	 */
	public MarkerResolutionProposal(IMarkerResolution resolution, IMarker marker) {
		fResolution= resolution;
		fMarker= marker;
	}

	@Override
	public void apply(IDocument document) {
		fResolution.run(fMarker);
	}

	@Override
	public String getAdditionalProposalInfo() {
		if (fResolution instanceof IMarkerResolution2) {
			return ((IMarkerResolution2) fResolution).getDescription();
		}
		if (fResolution instanceof IJavaCompletionProposal) {
			return ((IJavaCompletionProposal) fResolution).getAdditionalProposalInfo();
		}
		try {
			String problemDesc= (String) fMarker.getAttribute(IMarker.MESSAGE);
			return Messages.format(CorrectionMessages.MarkerResolutionProposal_additionaldesc, problemDesc);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public String getDisplayString() {
		return fResolution.getLabel();
	}

	@Override
	public Image getImage() {
		if (fResolution instanceof IMarkerResolution2) {
			return ((IMarkerResolution2) fResolution).getImage();
		}
		if (fResolution instanceof IJavaCompletionProposal) {
			return ((IJavaCompletionProposal) fResolution).getImage();
		}
		return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
	}

	@Override
	public int getRelevance() {
		if (fResolution instanceof IJavaCompletionProposal) {
			return ((IJavaCompletionProposal) fResolution).getRelevance();
		}
		return IProposalRelevance.MARKER_RESOLUTION;
	}

	@Override
	public Point getSelection(IDocument document) {
		if (fResolution instanceof IJavaCompletionProposal) {
			return ((IJavaCompletionProposal) fResolution).getSelection(document);
		}
		return null;
	}

}
