package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;
public class ConvertProjectNamingConventionProposal implements IJavaCompletionProposal, ICommandAccess{
	private String replacement;

	private int offset;

	@SuppressWarnings("unused")
	private int fLength;

	private int cursorPosition;

	@SuppressWarnings("unused")
	private int fRelevance;

	private String fCommandId;

	private Image image;

	private String fSelectedField;

	private IInvocationContext context;
	public ConvertProjectNamingConventionProposal(String replacement, int offset, int length, int cursorPosition, Image image, String selectedField, IInvocationContext context) {
		this.replacement= replacement;
		this.offset= offset;
		this.fLength= length;
		this.cursorPosition= cursorPosition;
		this.fRelevance= IProposalRelevance.PROJECT_NAMING_CONVENTION;
		this.image= image;
		this.fSelectedField= selectedField;
		this.context= context;
	}

	@Override
	public void apply(IDocument document) {
		 try {
			    ICompilationUnit cu= context.getCompilationUnit();
			    IType[] types = cu.getTypes();
			    for(IType type:types) {
			    	IField[] fields = type.getFields();
			    	for(IField field:fields) {
			    		if(field.getElementName().equals(fSelectedField)) {
			    			RenameFieldProcessor processor = new RenameFieldProcessor(field);
			    			processor.setNewElementName(replacement);
			    			processor.setUpdateReferences(true);
			    			processor.setRenameSetter(true);
			    			processor.setRenameGetter(true);
			    			processor.setUpdateTextualMatches(true);
			  	            RenameRefactoring refactoring = new RenameRefactoring(processor);
			  	            RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());
			  	            if (status.isOK()) {
			  	                PerformRefactoringOperation operation = new PerformRefactoringOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
			  	                operation.run(new NullProgressMonitor());
			  	            }
			    		}
			    	}
			    }
          } catch (Exception e) {
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRelevance() {
		// TODO Auto-generated method stub
		return 10;
	}

	public void setRelevance(int relevance) {
		fRelevance= relevance;
	}

	@Override
	public String getCommandId() {
		// TODO Auto-generated method stub
		return fCommandId;
	}
	public void setCommandId(String commandId) {
		fCommandId= commandId;
	}
}
