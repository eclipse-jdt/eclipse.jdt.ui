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
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedEnvironment;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedUIControl;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.GlobalVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.ui.texteditor.link.EditorHistoryUpdater;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.corext.Assert;
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
	 * @param template  the template
	 * @param context   the context in which the template was requested.
	 * @param image     the icon of the proposal.
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
			TemplateBuffer templateBuffer= fContext.evaluate(fTemplate);
			if (templateBuffer == null) {
				fSelectedRegion= fRegion;
				return;
			}
			
			int start, end;
			if (fContext instanceof DocumentTemplateContext) {
				DocumentTemplateContext docContext = (DocumentTemplateContext)fContext;
				start= docContext.getStart();
				end= docContext.getEnd();
			} else {
				start= fRegion.getOffset();
				end= start + fRegion.getLength();
			}
			
			// insert template string
			IDocument document= viewer.getDocument();
			String templateString= templateBuffer.getString();	
			document.replace(start, end - start, templateString);	
			
			// translate positions
			LinkedEnvironment env= new LinkedEnvironment();
			TemplateVariable[] variables= templateBuffer.getVariables();
			boolean hasPositions= false;
			for (int i= 0; i != variables.length; i++) {
				TemplateVariable variable= variables[i];

				if (variable.isUnambiguous())
					continue;
				
				LinkedPositionGroup group= new LinkedPositionGroup();
				
				int[] offsets= variable.getOffsets();
				int length= variable.getLength();
				
				for (int j= 0; j != offsets.length; j++)
					group.createPosition(document, offsets[j] + start, length);
				
				env.addGroup(group);
				hasPositions= true;
			}
			
			if (hasPositions) {
				env.forceInstall();
				LinkedUIControl editor= new LinkedUIControl(env, viewer);
				editor.setPositionListener(new EditorHistoryUpdater());
				editor.setExitPosition(viewer, getCaretOffset(templateBuffer) + start, 0, Integer.MAX_VALUE);
				editor.enter();
				
				fSelectedRegion= editor.getSelectedRegion();
			} else
				fSelectedRegion= new Region(getCaretOffset(templateBuffer) + start, 0);
			
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
			openErrorDialog(viewer.getTextWidget().getShell(), e);		    
			fSelectedRegion= fRegion;
		}

	}	
	
	private int getCaretOffset(TemplateBuffer buffer) {
	
	    TemplateVariable[] variables= buffer.getVariables();
		for (int i= 0; i != variables.length; i++) {
			TemplateVariable variable= variables[i];
			if (variable.getType().equals(GlobalVariables.Cursor.NAME))
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
			TemplateBuffer templateBuffer= fContext.evaluate(fTemplate);
			
			if (templateBuffer == null)
				return null;

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
		return false;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getReplacementString()
	 */
	public CharSequence getCompletionText() {
		return new String();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getReplacementOffset()
	 */
	public int getCompletionOffset() {
		return fRegion.getOffset();
	}

	/**
	 * {@inheritDoc}
	 */
	public void updateReplacementOffset(int newOffset) {
		Assert.isTrue(newOffset > 0);
		fRegion= new Region(newOffset, Math.max(0, fRegion.getLength() - (newOffset - fRegion.getOffset())));
	}

	/**
	 * {@inheritDoc}
	 */
	public String getReplacementString() {
		fContext.setReadOnly(false);
		try {
			TemplateBuffer templateBuffer= fContext.evaluate(fTemplate);
			if (templateBuffer == null)
				return new String(); // return empty string for replacement text
			else
				return templateBuffer.getString();
		} catch (BadLocationException e) {
			return new String();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void updateReplacementLength(int length) {
		Assert.isTrue(length >= 0);
		fRegion= new Region(fRegion.getOffset(), length);
	}
}
