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
package org.eclipse.jdt.internal.ui.infoviews;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.JavaCodeReader;

/**
 * View which shows source for a given Java element.
 * 
 * @since 3.0
 */
public class SourceView extends AbstractInfoView {

	private IJavaElement fSelectedElement;
	private int fCommentLines;
	private static final String SYMBOLIC_FONT_NAME= "org.eclipse.jdt.ui.editors.textfont"; //$NON-NLS-1$

	/**
	 * Internal property change listener for handling workbench font changes.
	 * @since 2.1
	 */
	class FontPropertyChangeListener implements IPropertyChangeListener {
		/*
		 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (fViewer == null)
				return;
				
			String property= event.getProperty();
			
			if (SYMBOLIC_FONT_NAME.equals(property))
				setViewerFont();
		}
	};

	/** This view's source viewer */
	private SourceViewer fViewer;
	/** The viewer's font properties change listener. */
	private IPropertyChangeListener fFontPropertyChangeListener= new FontPropertyChangeListener();
	/** The open action */
	private OpenAction fOpen;


	protected void internalCreatePartControl(Composite parent) {
		fViewer= new JavaSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL) {
			public ISelection getSelection() {
				return convertToJavaElementSelection(super.getSelection());
			}
		};
		fViewer.configure(new JavaSourceViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools(), null));
		fViewer.setEditable(false);

		setViewerFont();
		JFaceResources.getFontRegistry().addListener(fFontPropertyChangeListener);

		// Setup OpenAction		
		fOpen= new OpenAction(getViewSite());
		fOpen.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
		getViewSite().getActionBars().setGlobalActionHandler(JdtActionConstants.OPEN, fOpen);
		
		getViewSite().setSelectionProvider(fViewer);
	}

	protected void setForeground(Color color) {
		fViewer.getTextWidget().setForeground(color);
	}

	protected void setBackground(Color color) {
		fViewer.getTextWidget().setBackground(color);
	}

	public ISelection convertToJavaElementSelection(ISelection selection) {
		
		fSelectedElement= null;
		
		if (!(selection instanceof ITextSelection && fCurrentInput instanceof ISourceReference))
			return selection;

		ITextSelection textSelection= (ITextSelection)selection;
	
		Object codeAssist= fCurrentInput.getAncestor(IJavaElement.COMPILATION_UNIT); 
		if (codeAssist == null)
			codeAssist= fCurrentInput.getAncestor(IJavaElement.CLASS_FILE);

		if (codeAssist instanceof ICodeAssist) {
			IJavaElement[] elements= null;
			try {
				ISourceRange range= ((ISourceReference)fCurrentInput).getSourceRange();

				IDocument originalDoc= new Document(((ISourceReference)fCurrentInput).getSource());
				IDocument inputDoc= (IDocument)fViewer.getInput();
				int origOffset= 0;
				try {
					IRegion origLineInfo= originalDoc.getLineInformation(fCommentLines + textSelection.getStartLine());
					IRegion inputLineInfo= inputDoc.getLineInformation(textSelection.getStartLine());
					origOffset= origLineInfo.getOffset() + origLineInfo.getLength() - inputLineInfo.getLength() - inputLineInfo.getOffset() + textSelection.getOffset();
				} catch (BadLocationException e1) {
					return StructuredSelection.EMPTY;
				}

				elements= ((ICodeAssist)codeAssist).codeSelect(range.getOffset() + origOffset, textSelection.getLength());
			} catch (JavaModelException e) {
				return StructuredSelection.EMPTY;
			}
			if (elements != null && elements.length > 0) {
				fSelectedElement= elements[0];
				return new StructuredSelection(elements[0]);
			} else
				return StructuredSelection.EMPTY;
		}

		return StructuredSelection.EMPTY;
	}

	
	/*
	 * @see IWorkbenchPart#dispose()
	 */
	protected void internalDispose() {
		fViewer= null;
		JFaceResources.getFontRegistry().removeListener(fFontPropertyChangeListener);
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		fViewer.getTextWidget().setFocus();
	}
	
	protected boolean setInput(Object input) {

		if (fViewer == null || !(input instanceof ISourceReference))
			return false;

		if (fSelectedElement != null && fSelectedElement.equals(input))
			return false;

		fSelectedElement= null;

		String source;
		try {
			source= ((ISourceReference)input).getSource();
		} catch (JavaModelException ex) {
			return false;
		}
		
		if (source == null)
			return false;

		source= removeLeadingComments(source);
		String delim= null;

		try {
			if (input instanceof IJavaElement)
			delim= StubUtility.getLineDelimiterUsed((IJavaElement)input);
		} catch (JavaModelException e) {
			delim= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		String[] sourceLines= Strings.convertIntoLines(source);
		String firstLine= sourceLines[0];
		if (!Character.isWhitespace(firstLine.charAt(0)))
			sourceLines[0]= ""; //$NON-NLS-1$
		CodeFormatterUtil.removeIndentation(sourceLines);

		if (!Character.isWhitespace(firstLine.charAt(0)))
			sourceLines[0]= firstLine;

		source= Strings.concatenate(sourceLines, delim);

		IDocument doc= new Document(source);
		IDocumentPartitioner dp= JavaPlugin.getDefault().getJavaTextTools().createDocumentPartitioner();
		if (dp != null) {
			doc.setDocumentPartitioner(dp);
			dp.connect(doc);
		}
		fViewer.setInput(doc);
		
		return true;
	}

	private String removeLeadingComments(String source) {
		JavaCodeReader reader= new JavaCodeReader();
		IDocument document= new Document(source);
		int i;
		try {
			reader.configureForwardReader(document, 0, document.getLength(), true, false);
			int c= reader.read();
			while (c != -1 && (c == '\r' || c == '\n' || c == '\t')) {
				c= reader.read();
			}
			i= reader.getOffset();
			reader.close();
		} catch (IOException ex) {
			i= 0;
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException ex) {
				JavaPlugin.log(ex);
			}
		}

		try {
			fCommentLines= document.getLineOfOffset(i);
		} catch (BadLocationException e) {
			fCommentLines= 0;
		}

		if (i < 0)
			return source;

		return source.substring(i);
	}

	/**
	 * Sets the font for this viewer sustaining selection and scroll position.
	 */
	private void setViewerFont() {
		Font font= JFaceResources.getFont(SYMBOLIC_FONT_NAME);

		if (fViewer.getDocument() != null) {

			Point selection= fViewer.getSelectedRange();
			int topIndex= fViewer.getTopIndex();
			
			StyledText styledText= fViewer.getTextWidget();
			Control parent= styledText;
			if (fViewer instanceof ITextViewerExtension) {
				ITextViewerExtension extension= (ITextViewerExtension) fViewer;
				parent= extension.getControl();
			}
			
			parent.setRedraw(false);
			
			styledText.setFont(font);
			
			fViewer.setSelectedRange(selection.x , selection.y);
			fViewer.setTopIndex(topIndex);
			
			if (parent instanceof Composite) {
				Composite composite= (Composite) parent;
				composite.layout(true);
			}
			
			parent.setRedraw(true);
			
			
		} else {
			StyledText styledText= fViewer.getTextWidget();
			styledText.setFont(font);
		}	
	}

	/*
	 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		if (fViewer != null)
			fViewer.addSelectionChangedListener(listener);
	}

	/*
	 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		if (fViewer != null)
			fViewer.removeSelectionChangedListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
	 */
	public void setSelection(ISelection selection) {
		// XXX Auto-generated method stub
		
	}
}
