package org.eclipse.jdt.internal.debug.ui.display;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.JavaParameterListValidator;
import org.eclipse.jdt.internal.ui.text.java.ResultCollector;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.widgets.Shell;

/**
 * Java snippet completion processor.
 */
public class DisplayCompletionProcessor implements IContentAssistProcessor {
	
	private ResultCollector fCollector;
	private DisplayView fView;
	private IContextInformationValidator fValidator;
	
	public DisplayCompletionProcessor(DisplayView view) {
		fCollector= new ResultCollector();
		fView= view;
	}
	
	/**
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fCollector.getErrorMessage();
	}

	/**
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		if (fValidator == null) {
			fValidator= new JavaParameterListValidator();
		}
		return fValidator;
	}

	/**
	 * @see IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	/**
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}
	
	/**
	 * @see IContentAssistProcessor#computeProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int position) {
		try {
			IJavaStackFrame stackFrame= fView.getContext();
			if (stackFrame == null) {
				return new ICompletionProposal[0];
			}
			
			IJavaProject project= fView.getJavaProject(stackFrame);
			if (project != null) {
				ITextSelection selection= (ITextSelection)viewer.getSelectionProvider().getSelection();			
				ICompilationUnit cu= getCompilationUnit(stackFrame);
				if (cu == null) {
					return new ICompletionProposal[0];
				}
				IDocument doc = new Document(cu.getSource());
				int offset = doc.getLineOffset(stackFrame.getLineNumber());	
				configureResultCollector(project, selection, offset);	
				IWorkingCopy workingCopy= (IWorkingCopy) cu.getWorkingCopy();
				IBuffer buffer= ((ICompilationUnit)workingCopy).getBuffer();
				buffer.replace(offset, 0, fView.getContents());
				((ICompilationUnit)workingCopy).codeComplete(offset + selection.getOffset(), fCollector);
				workingCopy.destroy();
			}
		} catch (JavaModelException x) {
			Shell shell= viewer.getTextWidget().getShell();
			ErrorDialog.openError(shell,"Problems during completion", "An exception occurred during code completion", x.getStatus()); 
		} catch (DebugException de) {
			Shell shell= viewer.getTextWidget().getShell();
			ErrorDialog.openError(shell,"Problems during completion", "An exception occurred during code completion", de.getStatus()); 
		} catch (BadLocationException ble) {
			Shell shell= viewer.getTextWidget().getShell();
			IStatus status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, ble.getMessage(), ble);
			ErrorDialog.openError(shell,"Problems during completion", "An exception occurred during code completion", status); 
		}
		return fCollector.getResults();
	}
	
	/**
	 * Configures the display result collection for the current code assist session
	 */
	protected void configureResultCollector(IJavaProject project, ITextSelection selection, int editorOffset) {
		fCollector.reset(project, null);
		fCollector.setReplacementOffsetDifference(-editorOffset);	
		if (selection.getLength() != 0) {
			fCollector.setReplacementLength(selection.getLength());
		} 
	}
	
	/**
	 * Returns the compliation unit associated with this
	 * Java stack frame.  Returns <code>null</code> for a binary stack
	 * frame.
	 */
	protected ICompilationUnit getCompilationUnit(IJavaStackFrame stackFrame) {
		// Get the corresponding element.
		ILaunch launch = stackFrame.getLaunch();
		if (launch == null) {
			return null;
		}
		ISourceLocator locator= launch.getSourceLocator();
		if (locator == null) {
			return null;
		}
		Object sourceElement= locator.getSourceElement(stackFrame);
		if (sourceElement instanceof SourceType) {
			return (ICompilationUnit)((SourceType)sourceElement).getCompilationUnit();
		}
		return null;
	}
}