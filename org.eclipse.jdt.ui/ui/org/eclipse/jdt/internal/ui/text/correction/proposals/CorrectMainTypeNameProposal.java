/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

/**
 * Renames the primary type to be compatible with the name of the compilation unit.
 * All constructors and local references to the type are renamed as well.
 */
public class CorrectMainTypeNameProposal extends ASTRewriteCorrectionProposal {

	/**
	 * Constructor for CorrectTypeNameProposal.
	 * @param cu the compilation unit
	 * @param context the invocation context
	 * @param oldTypeName the old type name
	 * @param newTypeName the new type name
	 * @param relevance the relevance
	 */
	public CorrectMainTypeNameProposal(ICompilationUnit cu, IInvocationContext context, String oldTypeName, String newTypeName, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		setDelegate(new CorrectMainTypeNameProposalCore("", cu, null, context, oldTypeName, newTypeName, relevance)); //$NON-NLS-1$
		setDisplayName(Messages.format(CorrectionMessages.ReorgCorrectionsSubProcessor_renametype_description, BasicElementLabels.getJavaElementName(newTypeName)));
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}

}
