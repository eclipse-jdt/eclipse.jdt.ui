/*******************************************************************************
 * Copyright (c) 2022 Red Hat.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.Arrays;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;

import org.eclipse.jdt.internal.ui.wizards.NewTypeDropDownAction;
import org.eclipse.jdt.internal.ui.wizards.NewTypeDropDownAction.OpenTypeWizardAction;

public class NewInterfaceImplementationProposal extends ChangeCorrectionProposal {
	private static final String NEW_INTERFACE_CREATION_ID= "org.eclipse.jdt.ui.wizards.NewInterfaceCreationWizard"; //$NON-NLS-1$
	private ICompilationUnit fCompilationUnit;

	public NewInterfaceImplementationProposal(String name, Change change, int relevance, Image image, ICompilationUnit cu) {
		super(name, change, relevance, image);
		this.fCompilationUnit= cu;
	}

	@Override
	public void apply(IDocument document) {
		OpenTypeWizardAction[] actions= NewTypeDropDownAction.getActionFromDescriptors();
		OpenTypeWizardAction classCreationAction= Arrays.asList(actions).stream().filter(a ->
		NEW_INTERFACE_CREATION_ID.equals(a.getId())).findFirst().get();
		IType cuType= fCompilationUnit.findPrimaryType();
		if (cuType != null) {
			// Passing the CU type ensures wizard is pre-loaded with inheritance data
			classCreationAction.setSelection(new StructuredSelection(cuType));
		}
		classCreationAction.run();
	}
}
