/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     ChunHao Dong <m15231670380@163.com> - [Rename field] Add quick assist for field rename refactoring. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/749
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.IField;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;

public class ConvertFieldNamingConventionProposal implements IJavaCompletionProposal, ICommandAccess{

	private String replacement;

	private int offset;

	@SuppressWarnings("unused")
	private int fLength;

	private int cursorPosition;

	private int fRelevance;

	private String fCommandId;

	private Image image;

	private IField fSelectedField;

	@SuppressWarnings("unused")
	private IInvocationContext context;
	public ConvertFieldNamingConventionProposal(String replacement, int offset, int length, int cursorPosition, Image image, IField selectedField, IInvocationContext context) {
		this.replacement= replacement;
		this.offset= offset;
		this.fLength= length;
		this.cursorPosition= cursorPosition;
		this.fRelevance= IProposalRelevance.FIELD_NAMING_CONVENTION;
		this.image= image;
		this.fSelectedField= selectedField;
		this.context= context;
	}

	@Override
	public void apply(IDocument document) {
		try {
			RenameFieldProcessor processor = new RenameFieldProcessor(fSelectedField);
			processor.setNewElementName(replacement);
			processor.setUpdateReferences(true);
			processor.setRenameSetter(true);
			processor.setRenameGetter(true);
			processor.setUpdateTextualMatches(true);
			RenameRefactoring refactoring = new RenameRefactoring(processor);
			RefactoringStatus status;
			status= refactoring.checkAllConditions(new NullProgressMonitor());
			if (status.isOK()) {
				PerformRefactoringOperation operation = new PerformRefactoringOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
				operation.run(new NullProgressMonitor());
				}
		} catch (OperationCanceledException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(offset + cursorPosition, 0);
	}

	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}

	@Override
	public String getDisplayString() {
		return "Convert to Constant Naming Convention"; //$NON-NLS-1$
	}

	@Override
	public Image getImage() {
		return image;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public int getRelevance() {
		return fRelevance;
	}

	public void setRelevance(int relevance) {
		fRelevance= relevance;
	}

	@Override
	public String getCommandId() {
		return fCommandId;
	}
	public void setCommandId(String commandId) {
		fCommandId= commandId;
	}
}
