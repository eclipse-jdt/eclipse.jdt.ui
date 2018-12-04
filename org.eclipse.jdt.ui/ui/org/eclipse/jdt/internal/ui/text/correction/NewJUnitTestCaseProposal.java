/*******************************************************************************
 * Copyright (c) 2018 Red Hat.
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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Arrays;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;

import org.eclipse.jdt.internal.ui.wizards.NewTypeDropDownAction;
import org.eclipse.jdt.internal.ui.wizards.NewTypeDropDownAction.OpenTypeWizardAction;

public class NewJUnitTestCaseProposal extends ChangeCorrectionProposal {

	private static final String JUNIT_NEW_TESTCASE_ID= "org.eclipse.jdt.junit.wizards.NewTestCaseCreationWizard"; //$NON-NLS-1$
	private CompilationUnit fCompilationUnit;

	public NewJUnitTestCaseProposal(String name, Change change, int relevance, Image image, CompilationUnit cu) {
		super(name, change, relevance, image);
		this.fCompilationUnit= cu;
	}

	@Override
	public void apply(IDocument document) {
		OpenTypeWizardAction[] actions= NewTypeDropDownAction.getActionFromDescriptors();
		OpenTypeWizardAction junitTestCaseAction= Arrays.asList(actions).stream().filter(a ->
		JUNIT_NEW_TESTCASE_ID.equals(a.getId())).findFirst().get();
		junitTestCaseAction.setSelection(new StructuredSelection(fCompilationUnit));
		junitTestCaseAction.run();
	}

}
