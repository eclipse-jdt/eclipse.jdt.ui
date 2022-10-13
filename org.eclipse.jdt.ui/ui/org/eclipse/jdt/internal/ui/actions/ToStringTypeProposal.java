/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.actions.GenerateToStringAction;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;


public class ToStringTypeProposal extends ChangeCorrectionProposal { // public for tests

	private IType fType;

	public ToStringTypeProposal(int relevance, IType type) {
		super(getDescription(type), null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		fType= type;
	}

	public IType getType() {
		return fType;
	}

	private static String getDescription(IType type) {
		return Messages.format(CorrectionMessages.AddToString_createtostringfortype_description, BasicElementLabels.getJavaElementName(type.getElementName()));
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension5#getAdditionalProposalInfo(org.eclipse.core.runtime.IProgressMonitor)
	 * @since 3.5
	 */
	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		return CorrectionMessages.AddToString_additional_info;
	}

	@Override
	public void apply(IDocument document) {
		Display.getDefault().syncExec(() -> {
			try {
				IStructuredSelection selection= new StructuredSelection(fType);
				IWorkbenchSite site= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getSite();
				new GenerateToStringAction(site).run(selection);
			} catch (NullPointerException e) {
				// do nothing
			}
		});
	}
}

