/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.template.contentassist;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitContext;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.template.java.JavaTemplateMessages;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * A template proposal.
 */
public class TemplateProposal implements IJavaCompletionProposal, ICompletionProposalExtension2, ICompletionProposalExtension3 {

	private final Template fTemplate;
	private final TemplateContext fContext;
	private final Image fImage;
	private IRegion fRegion;
	private int fRelevance;

	private IRegion fSelectedRegion; // initialized by apply()
	private String fDisplayString;
		
	/**
	 * Creates a template proposal with a template and its context.
	 * 
	 * @param template  the template
	 * @param context   the context in which the template was requested
	 * @param region 	the region this proposal applies to
	 * @param image     the icon of the proposal
	 */	
	public TemplateProposal(Template template, TemplateContext context, IRegion region, Image image) {
		Assert.isNotNull(template);
		Assert.isNotNull(context);
		Assert.isNotNull(region);
		
		fTemplate= template;
		fContext= context;
		fImage= image;
		fRegion= region;
		
		fDisplayString= null;
		
		if (context instanceof JavaContext) {
			switch (((JavaContext) context).getCharacterBeforeStart()) {
			// high relevance after whitespace
			case ' ':
			case '\r':
			case '\n':
			case '\t':
				fRelevance= 90;
				break;
			default:
				fRelevance= 0;
			}
		} else {
			fRelevance= 90;			
		}		
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public final void apply(IDocument document) {
		// not called anymore
	}
	
	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {

		try {
			
			fContext.setReadOnly(false);
			TemplateBuffer templateBuffer;
			try {
				templateBuffer= fContext.evaluate(fTemplate);
			} catch (TemplateException e1) {
				fSelectedRegion= fRegion;
				return;
			}
			
			int start= getReplaceOffset();
			int end= getReplaceEndOffset();
			
			// insert template string
			IDocument document= viewer.getDocument();
			String templateString= templateBuffer.getString();	
			document.replace(start, end - start, templateString);	
			
			// translate positions
			LinkedModeModel model= new LinkedModeModel();
			TemplateVariable[] variables= templateBuffer.getVariables();
			
			MultiVariableGuess guess= fContext instanceof CompilationUnitContext ? ((CompilationUnitContext) fContext).getMultiVariableGuess() : null;
		
			boolean hasPositions= false;
			for (int i= 0; i != variables.length; i++) {
				TemplateVariable variable= variables[i];

				if (variable.isUnambiguous())
					continue;
				
				LinkedPositionGroup group= new LinkedPositionGroup();
				
				int[] offsets= variable.getOffsets();
				int length= variable.getLength();
				
				LinkedPosition first;
				if (guess != null && variable instanceof MultiVariable) {
					first= new VariablePosition(document, offsets[0] + start, length, guess, (MultiVariable) variable);
					guess.addSlave((VariablePosition) first);
				} else {
					String[] values= variable.getValues();
					ICompletionProposal[] proposals= new ICompletionProposal[values.length];
					for (int j= 0; j < values.length; j++) {
						ensurePositionCategoryInstalled(document, model);
						Position pos= new Position(offsets[0] + start, length);
						document.addPosition(getCategory(), pos);
						proposals[j]= new PositionBasedCompletionProposal(values[j], pos, length);
					}
					
					if (proposals.length > 1)
						first= new ProposalPosition(document, offsets[0] + start, length, proposals);
					else
						first= new LinkedPosition(document, offsets[0] + start, length);
				}
				
				for (int j= 0; j != offsets.length; j++)
					if (j == 0)
						group.addPosition(first);
					else
						group.addPosition(new LinkedPosition(document, offsets[j] + start, length));
				
				model.addGroup(group);
				hasPositions= true;
			}
			
			if (hasPositions) {
				model.forceInstall();
				LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
				ui.setExitPosition(viewer, getCaretOffset(templateBuffer) + start, 0, Integer.MAX_VALUE);
				ui.enter();
				
				fSelectedRegion= ui.getSelectedRegion();
			} else
				fSelectedRegion= new Region(getCaretOffset(templateBuffer) + start, 0);
			
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
			openErrorDialog(viewer.getTextWidget().getShell(), e);		    
			fSelectedRegion= fRegion;
		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
			openErrorDialog(viewer.getTextWidget().getShell(), e);		    
			fSelectedRegion= fRegion;
		}

	}	
	
	/**
	 * Returns the offset of the range in the document that will be replaced by
	 * applying this template.
	 * 
	 * @return the offset of the range in the document that will be replaced by
	 *         applying this template
	 */
	private int getReplaceOffset() {
		int start;
		if (fContext instanceof DocumentTemplateContext) {
			DocumentTemplateContext docContext = (DocumentTemplateContext)fContext;
			start= docContext.getStart();
		} else {
			start= fRegion.getOffset();
		}
		return start;
	}

	/**
	 * Returns the end offset of the range in the document that will be replaced
	 * by applying this template.
	 * 
	 * @return the end offset of the range in the document that will be replaced
	 *         by applying this template
	 */
	private int getReplaceEndOffset() {
		int end;
		if (fContext instanceof DocumentTemplateContext) {
			DocumentTemplateContext docContext = (DocumentTemplateContext)fContext;
			end= docContext.getEnd();
		} else {
			end= fRegion.getOffset() + fRegion.getLength();
		}
		return end;
	}

	private void ensurePositionCategoryInstalled(final IDocument document, LinkedModeModel model) {
		if (!document.containsPositionCategory(getCategory())) {
			document.addPositionCategory(getCategory());
			final InclusivePositionUpdater updater= new InclusivePositionUpdater(getCategory());
			document.addPositionUpdater(updater);
			
			model.addLinkingListener(new ILinkedModeListener() {

				/*
				 * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.text.link.LinkedModeModel, int)
				 */
				public void left(LinkedModeModel environment, int flags) {
					try {
						document.removePositionCategory(getCategory());
					} catch (BadPositionCategoryException e) {
						// ignore
					}
					document.removePositionUpdater(updater);
				}

				public void suspend(LinkedModeModel environment) {}
				public void resume(LinkedModeModel environment, int flags) {}
			});
		}
	}

	private String getCategory() {
		return "TemplateProposalCategory_" + toString(); //$NON-NLS-1$
	}

	private int getCaretOffset(TemplateBuffer buffer) {
	
	    TemplateVariable[] variables= buffer.getVariables();
		for (int i= 0; i != variables.length; i++) {
			TemplateVariable variable= variables[i];
			if (variable.getType().equals(GlobalTemplateVariables.Cursor.NAME))
				return variable.getOffsets()[0];
		}

		return buffer.getString().length();
	}
	
	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
	    try {
		    fContext.setReadOnly(true);
			TemplateBuffer templateBuffer;
			try {
				templateBuffer= fContext.evaluate(fTemplate);
			} catch (TemplateException e1) {
				return null;
			}

			return templateBuffer.getString();

	    } catch (BadLocationException e) {
			handleException(JavaPlugin.getActiveWorkbenchShell(), new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "", e))); //$NON-NLS-1$
			return null;
		}
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		if (fDisplayString == null) {
			fDisplayString= fTemplate.getName() + JavaTemplateMessages.getString("TemplateProposal.delimiter") + fTemplate.getDescription(); //$NON-NLS-1$
		}
		return fDisplayString;
	}
	
	public void setDisplayString(String displayString) {
		fDisplayString= displayString;
	}	

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}

	private void openErrorDialog(Shell shell, Exception e) {
		MessageDialog.openError(shell, JavaTemplateMessages.getString("TemplateEvaluator.error.title"), e.getMessage()); //$NON-NLS-1$
	}

	private void handleException(Shell shell, CoreException e) {
		ExceptionHandler.handle(e, shell, JavaTemplateMessages.getString("TemplateEvaluator.error.title"), null); //$NON-NLS-1$
	}

	/*
	 * @see IJavaCompletionProposal#getRelevance()
	 */
	public int getRelevance() {
		return fRelevance;
	}

	public void setRelevance(int relevance) {
		fRelevance= relevance;
	}
	
	public Template getTemplate() {
		return fTemplate;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getInformationControlCreator()
	 */
	public IInformationControlCreator getInformationControlCreator() {
		return new TemplateInformationControlCreator();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer, boolean)
	 */
	public void selected(ITextViewer viewer, boolean smartToggle) {
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(org.eclipse.jface.text.ITextViewer)
	 */
	public void unselected(ITextViewer viewer) {
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
	 */
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		try {
			int replaceOffset= getReplaceOffset();
			int replaceEndOffset= getReplaceEndOffset();
			if (replaceEndOffset >= offset) {
				String content= document.get(replaceOffset, replaceEndOffset - replaceOffset);
				return fTemplate.getName().startsWith(content);
			}
		} catch (BadLocationException e) {
			// concurrent modification - ignore
		}
		return false;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getReplacementString()
	 */
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return fTemplate.getName();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getReplacementOffset()
	 */
	public int getPrefixCompletionStart(IDocument document, int completionOffset) {
		return getReplaceOffset();
	}
}
