package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.text.MessageFormat;

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

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;

public class ConvertProjectNamingConventionProposal implements IJavaCompletionProposal, ICommandAccess {
	private String replacement;

	private int offset;

	private int cursorPosition;

	private int fRelevance;

	private String fCommandId;

	private Image image;

	private IField fSelectedField;

	public ConvertProjectNamingConventionProposal(String replacement, int offset, int cursorPosition, Image image, IField selectedField) {
		this.replacement= replacement;
		this.offset= offset;
		this.cursorPosition= cursorPosition;
		this.fRelevance= IProposalRelevance.PROJECT_NAMING_CONVENTION;
		this.image= image;
		this.fSelectedField= selectedField;
	}

	@Override
	public void apply(IDocument document) {
		try {
			RenameFieldProcessor processor= new RenameFieldProcessor(fSelectedField);
			processor.setNewElementName(replacement);
			processor.setUpdateReferences(true);
			processor.setRenameSetter(true);
			processor.setRenameGetter(true);
			processor.setUpdateTextualMatches(true);
			RenameRefactoring refactoring= new RenameRefactoring(processor);
			RefactoringStatus status;
			status= refactoring.checkAllConditions(new NullProgressMonitor());
			if (status.isOK()) {
				PerformRefactoringOperation operation= new PerformRefactoringOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
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
		return MessageFormat.format(CorrectionMessages.QuickAssistProcessor_convert_project_name_description, fSelectedField.getElementName(), replacement);
	}

	@Override
	public String getDisplayString() {
		return CorrectionMessages.QuickAssistProcessor_convert_project_name;
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
