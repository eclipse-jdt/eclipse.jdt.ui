/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - created from CUCorrectionProposal
 *******************************************************************************/

package org.eclipse.jdt.core.manipulation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.internal.ui.text.correction.proposals.EditAnnotator;

/**
 * A proposal for quick fixes and quick assists that work on a single compilation unit. Either a
 * {@link TextChange text change} is directly passed in the constructor or method
 * {@link #addEdits(IDocument, TextEdit)} is overridden to provide the text edits that are applied
 * to the document when the proposal is evaluated.
 * <p>
 * The proposal takes care of the preview of the changes as proposal information.
 * </p>
 *
 * @since 1.11
 */
public class CUCorrectionProposalCore extends ChangeCorrectionProposalCore implements ICUCorrectionProposal {

	private ICompilationUnit fCompilationUnit;
	private ICUCorrectionProposal fCUProposal;

	/**
	 * Constructs a correction proposal working on a compilation unit with a given text change.
	 *
	 * @param proposal ICUCorrectionProposal to use
	 * @param name the name that is displayed in the proposal selection dialog
	 * @param cu the compilation unit to which the change can be applied
	 * @param change the change that is executed when the proposal is applied or <code>null</code>
	 *            if implementors override {@link #addEdits(IDocument, TextEdit)} to provide the
	 *            text edits or {@link #createTextChange()} to provide a text change
	 * @param relevance the relevance of this proposal
	 */
	public CUCorrectionProposalCore(ICUCorrectionProposal proposal, String name, ICompilationUnit cu, TextChange change, int relevance) {
		super(name, change, relevance);
		if (cu == null) {
			throw new IllegalArgumentException("Compilation unit must not be null"); //$NON-NLS-1$
		}
		fCompilationUnit= cu;
		fCUProposal= proposal;
	}

	/**
	 * Constructs a correction proposal working on a compilation unit with a given text change.
	 *
	 * @param name the name that is displayed in the proposal selection dialog
	 * @param cu the compilation unit to which the change can be applied
	 * @param change the change that is executed when the proposal is applied or <code>null</code>
	 *            if implementors override {@link #addEdits(IDocument, TextEdit)} to provide the
	 *            text edits or {@link #createTextChange()} to provide a text change
	 * @param relevance the relevance of this proposal
	 */
	public CUCorrectionProposalCore(String name, ICompilationUnit cu, TextChange change, int relevance) {
		super(name, change, relevance);
		if (cu == null) {
			throw new IllegalArgumentException("Compilation unit must not be null"); //$NON-NLS-1$
		}
		fCompilationUnit= cu;
		fCUProposal= this;
	}

	/**
	 * Constructs a correction proposal working on a compilation unit.
	 * <p>
	 * Users have to override {@link #addEdits(IDocument, TextEdit)} to provide the text edits or
	 * {@link #createTextChange()} to provide a text change.
	 * </p>
	 *
	 * @param name the name that is displayed in the proposal selection dialog
	 * @param cu the compilation unit on that the change works
	 * @param relevance the relevance of this proposal
	 */
	protected CUCorrectionProposalCore(String name, ICompilationUnit cu, int relevance) {
		this(name, cu, null, relevance);
	}

	/**
	 * Called when the {@link CompilationUnitChange} is initialized. Subclasses can override to add
	 * text edits to the root edit of the change. Implementors must not access the proposal, e.g.
	 * not call {@link #getChange()}.
	 * <p>
	 * The default implementation does not add any edits
	 * </p>
	 *
	 * @param document content of the underlying compilation unit. To be accessed read only.
	 * @param editRoot The root edit to add all edits to
	 * @throws CoreException can be thrown if adding the edits is failing.
	 */
	protected void addEdits(IDocument document, TextEdit editRoot) throws CoreException {
		// empty default implementation
	}

	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		StringBuffer buf= new StringBuffer();
		try {
			TextChange change= fCUProposal.getTextChange();
			change.setKeepPreviewEdits(true);
			IDocument previewDocument= change.getPreviewDocument(monitor);
			TextEdit rootEdit= change.getPreviewEdit(change.getEdit());
			EditAnnotator ea= new EditAnnotator(buf, previewDocument);
			rootEdit.accept(ea);
			ea.unchangedUntil(previewDocument.getLength()); // Final pre-existing region
		} catch (CoreException e) {
			JavaManipulationPlugin.log(e);
		}
		return buf.toString();
	}

	@Override
	public void apply() throws CoreException {
		performChange();
	}

	public TextChange getNewChange() {
		ICompilationUnit cu= getCompilationUnit();
		String name= getName();
		TextChange change;
		if (!cu.getResource().exists()) {
			String source;
			try {
				source= cu.getSource();
			} catch (JavaModelException e) {
				JavaManipulationPlugin.log(e);
				source= ""; // empty //$NON-NLS-1$
			}
			Document document= new Document(source);
			document.setInitialLineDelimiter(StubUtility.getLineDelimiterUsed(cu));
			change= new DocumentChange(name, document);
		} else {
			CompilationUnitChange cuChange = new CompilationUnitChange(name, cu);
			cuChange.setSaveMode(TextFileChange.LEAVE_DIRTY);
			change= cuChange;
		}
		TextEdit rootEdit= new MultiTextEdit();
		change.setEdit(rootEdit);
		return change;
	}

	/**
	 * Creates the text change for this proposal.
	 * This method is only called once and only when no text change has been passed in
	 * {see #CUCorrectionProposal(String, ICompilationUnit, TextChange, int, Image)}.
	 *
	 * @return the created text change
	 * @throws CoreException if the creation of the text change failed
	 */
	public TextChange createTextChange() throws CoreException {
		TextChange change = getNewChange();
		// initialize text change
		IDocument document= change.getCurrentDocument(new NullProgressMonitor());
		addEdits(document, change.getEdit());
		return change;
	}


	@Override
	protected Change createChange() throws CoreException {
		return createTextChange(); // make sure that only text changes are allowed here
	}

	/**
	 * Returns the text change that is invoked when the change is applied.
	 *
	 * @return the text change that is invoked when the change is applied
	 * @throws CoreException if accessing the change failed
	 */
	@Override
	public final TextChange getTextChange() throws CoreException {
		return (TextChange) getChange();
	}

	/**
	 * The compilation unit on which the change works.
	 *
	 * @return the compilation unit on which the change works
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * Creates a preview of the content of the compilation unit after applying the change.
	 *
	 * @return the preview of the changed compilation unit
	 * @throws CoreException if the creation of the change failed
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public String getPreviewContent() throws CoreException {
		return getTextChange().getPreviewContent(new NullProgressMonitor());
	}

	@Override
	public String toString() {
		try {
			return getPreviewContent();
		} catch (CoreException e) {
			// didn't work out
		}
		return super.toString();
	}

}
