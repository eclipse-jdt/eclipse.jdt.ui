/*******************************************************************************
 * Copyright (c) 2025 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer using github copilot - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

/**
 * Quick Assist proposal to remove invalid enum names from @EnumSource annotation.
 *
 * @since 3.15
 */
public class RemoveInvalidEnumNamesProposal implements IJavaCompletionProposal {

	private final IInvocationContext fContext;
	private final MethodDeclaration fMethodDecl;

	public RemoveInvalidEnumNamesProposal(IInvocationContext context, MethodDeclaration methodDecl) {
		fContext = context;
		fMethodDecl = methodDecl;
	}

	@Override
	public void apply(IDocument document) {
		try {
			ICompilationUnit cu = fContext.getCompilationUnit();
			if (cu == null) {
				return;
			}

			// Find the method in the compilation unit
			IType primaryType = cu.findPrimaryType();
			if (primaryType == null) {
				return;
			}

			IMethod method = findTestMethod(primaryType, fMethodDecl.getName().getIdentifier());
			if (method == null) {
				return;
			}

			// Remove invalid enum names
			EnumSourceValidator.removeInvalidEnumNames(method);
		} catch (Exception e) {
			JUnitPlugin.log(e);
		}
	}

	private IMethod findTestMethod(IType type, String methodName) throws JavaModelException {
		IMethod[] methods = type.getMethods();
		for (IMethod method : methods) {
			if (method.getElementName().equals(methodName)) {
				return method;
			}
		}
		return null;
	}

	@Override
	public String getAdditionalProposalInfo() {
		return JUnitMessages.JUnitQuickAssistProcessor_remove_invalid_enum_names_info;
	}

	@Override
	public String getDisplayString() {
		return JUnitMessages.JUnitQuickAssistProcessor_remove_invalid_enum_names_description;
	}

	@Override
	public Image getImage() {
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_ANNOTATION);
	}

	@Override
	public Point getSelection(IDocument document) {
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public int getRelevance() {
		return 10;
	}
}
