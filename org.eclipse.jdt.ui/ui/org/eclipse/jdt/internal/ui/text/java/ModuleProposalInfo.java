/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

public class ModuleProposalInfo extends ProposalInfo {

	private boolean fJavaElementResolved= false;

	private final IJavaProject fJavaProject;

	private final CompletionProposal fProposal;

	ModuleProposalInfo(IJavaProject project, CompletionProposal proposal) {
		Assert.isNotNull(project);
		Assert.isNotNull(proposal);
		fJavaProject= project;
		fProposal= proposal;
	}

	@Override
	public IJavaElement getJavaElement() throws JavaModelException {
		if (!fJavaElementResolved) {
			fJavaElementResolved= true;
			fElement= resolveModule();
		}
		return fElement;
	}

	/**
	 * Resolves to an IModuleDescription.
	 *
	 * @return the <code>IModuleDescription</code> or <code>null</code> if no Java element can be found
	 * @throws JavaModelException thrown if the given path is <code>null</code> or absolute
	 */
	private IJavaElement resolveModule() throws JavaModelException {
		char[] signature= fProposal.getDeclarationSignature();
		if (signature != null) {
			String typeName= String.valueOf(signature);
			return fJavaProject.findModule(typeName, null);
		}
		return null;
	}

}
