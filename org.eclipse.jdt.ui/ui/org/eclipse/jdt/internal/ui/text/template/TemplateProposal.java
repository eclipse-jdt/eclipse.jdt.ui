/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplateMessages;
import org.eclipse.jdt.internal.corext.template.TemplatePosition;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.template.java.JavaTemplateMessages;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * A template proposal.
 */
public class TemplateProposal implements IJavaCompletionProposal {

	private final Template fTemplate;
	private final TemplateContext fContext;
	private final ITextViewer fViewer;
	private final Image fImage;
	private final IRegion fRegion;

	private TemplateBuffer fTemplateBuffer;
	private String fOldText;
	private IRegion fSelectedRegion; // initialized by apply()
		
	/**
	 * Creates a template proposal with a template and its context.
	 * @param template  the template
	 * @param context   the context in which the template was requested.
	 * @param image     the icon of the proposal.
	 */	
	public TemplateProposal(Template template, TemplateContext context, IRegion region, ITextViewer viewer, Image image) {
		Assert.isNotNull(template);
		Assert.isNotNull(context);
		Assert.isNotNull(region);
		Assert.isNotNull(viewer);
		
		fTemplate= template;
		fContext= context;
		fViewer= viewer;
		fImage= image;
		fRegion= region;
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
	    try {
			Position position= new Position(fRegion.getOffset(), fRegion.getLength());
			final String category= "__template_position_" + System.currentTimeMillis(); // $NON-NLS-1$
			IPositionUpdater updater= new DefaultPositionUpdater(category);
			document.addPositionCategory(category);
			document.addPositionUpdater(updater);
			document.addPosition(position);

		    fContext.setReadOnly(false);
			fTemplateBuffer= fContext.evaluate(fTemplate);
			
			document.removePosition(position);
			document.removePositionUpdater(updater);
			document.removePositionCategory(category);
			
			int start= position.getOffset();
			int end= position.getOffset() + position.getLength();

			// insert template string
			String templateString= fTemplateBuffer.getString();	
			document.replace(start, end - start, templateString);	

			// translate positions
			LinkedPositionManager manager= new LinkedPositionManager(document);
			TemplatePosition[] variables= fTemplateBuffer.getVariables();
			for (int i= 0; i != variables.length; i++) {
				TemplatePosition variable= variables[i];

				if (variable.isResolved())
					continue;
				
				int[] offsets= variable.getOffsets();
				int length= variable.getLength();
				
				for (int j= 0; j != offsets.length; j++)
					manager.addPosition(offsets[j] + start, length);
			}
			
			LinkedPositionUI editor= new LinkedPositionUI(fViewer, manager);
			editor.setFinalCaretOffset(getCaretOffset(fTemplateBuffer) + start);
			editor.enter();

			fSelectedRegion= editor.getSelectedRegion();
			
		} catch (BadLocationException e) {
			JavaPlugin.log(e);	
			openErrorDialog(e);		    	
			
	    } catch (BadPositionCategoryException e) {    
			JavaPlugin.log(e);	
			openErrorDialog(e);		    	

	    } catch (CoreException e) {
			handleException(e);
	    }	    
	}
	
	private static int getCaretOffset(TemplateBuffer buffer) {
	    TemplatePosition[] variables= buffer.getVariables();
		for (int i= 0; i != variables.length; i++) {
			TemplatePosition variable= variables[i];
			
			if (variable.getName().equals(JavaTemplateMessages.getString("GlobalVariables.variable.name.cursor"))) //$NON-NLS-1$
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
			fTemplateBuffer= fContext.evaluate(fTemplate);

			return textToHTML(fTemplateBuffer.getString());

	    } catch (CoreException e) {
			handleException(e);		    
			return null;
	    }
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return fTemplate.getName() + TemplateMessages.getString("TemplateProposal.delimiter") + fTemplate.getDescription(); // $NON-NLS-1$ //$NON-NLS-1$
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

	private static String textToHTML(String string) {
		StringBuffer buffer= new StringBuffer(string.length());
		buffer.append("<pre>"); //$NON-NLS-1$
	
		for (int i= 0; i != string.length(); i++) {
			char ch= string.charAt(i);
			
			switch (ch) {
				case '&':
					buffer.append("&amp;"); //$NON-NLS-1$
					break;
					
				case '<':
					buffer.append("&lt;"); //$NON-NLS-1$
					break;

				case '>':
					buffer.append("&gt;"); //$NON-NLS-1$
					break;

				case '\t':
					buffer.append("    "); //$NON-NLS-1$
					break;

				case '\n':
					buffer.append("<br>"); //$NON-NLS-1$
					break;

				default:
					buffer.append(ch);
					break;
			}
		}

		buffer.append("</pre>"); //$NON-NLS-1$
		return buffer.toString();
	}

	private void openErrorDialog(Exception e) {
		Shell shell= fViewer.getTextWidget().getShell();
		MessageDialog.openError(shell, TemplateMessages.getString("TemplateEvaluator.error.title"), e.getMessage()); //$NON-NLS-1$
	}

	private void handleException(CoreException e) {
		Shell shell= fViewer.getTextWidget().getShell();
		ExceptionHandler.handle(e, shell, TemplateMessages.getString("TemplateEvaluator.error.title"), null); //$NON-NLS-1$
	}

	/*
	 * @see IJavaCompletionProposal#getRelevance()
	 */
	public int getRelevance() {

		if (fContext instanceof JavaContext) {
			JavaContext context= (JavaContext) fContext;
			switch (context.getCharacterBeforeStart()) {
			// high relevance after whitespace
			case ' ':
			case '\r':
			case '\n':
			case '\t':
				return 90;

			default:
				return 0;
			}
		} else {
			return 90;			
		}
	}

}