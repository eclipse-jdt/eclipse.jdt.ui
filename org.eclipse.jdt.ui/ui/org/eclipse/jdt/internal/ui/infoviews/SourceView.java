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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

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
public class SourceView extends AbstractInfoView implements IMenuListener {

	/** Symbolic Java editor font name. */ 
	private static final String SYMBOLIC_FONT_NAME= "org.eclipse.jdt.ui.editors.textfont"; //$NON-NLS-1$

	/**
	 * Internal property change listener for handling changes in the editor's preferences.
	 * 
	 * @since 3.0
	 */
	class PropertyChangeListener implements IPropertyChangeListener {
		/*
		 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (fViewer == null)
				return;
			
			JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
			if (textTools.affectsBehavior(event))
				fViewer.invalidateTextPresentation();
		}
	}

	/**
	 * Internal property change listener for handling workbench font changes.
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
	}

	/**
	 * The Javadoc view's select all action.
	 */
	private static class SelectAllAction extends Action {

		private TextViewer fTextViewer;

		/**
		 * Creates the action.
		 */
		public SelectAllAction(TextViewer textViewer) {
			super("selectAll"); //$NON-NLS-1$

			Assert.isNotNull(textViewer);
			fTextViewer= textViewer;

			setText(InfoViewMessages.getString("SelectAllAction.label")); //$NON-NLS-1$
			setToolTipText(InfoViewMessages.getString("SelectAllAction.tooltip")); //$NON-NLS-1$
			setDescription(InfoViewMessages.getString("SelectAllAction.description")); //$NON-NLS-1$

			WorkbenchHelp.setHelp(this, IAbstractTextEditorHelpContextIds.SELECT_ALL_ACTION);
		}

		/**
		 * Selects all in the viewer.
		 */
		public void run() {
			fTextViewer.doOperation(ITextOperationTarget.SELECT_ALL);
		}
	}

	/** This view's source viewer */
	private SourceViewer fViewer;
	/** The viewer's font properties change listener. */
	private IPropertyChangeListener fFontPropertyChangeListener= new FontPropertyChangeListener();
	/**
	 * The editor's property change listener.
	 * @since 3.0
	 */
	private IPropertyChangeListener fPropertyChangeListener= new PropertyChangeListener();
	/** The open action */
	private OpenAction fOpen;
	/** The number of removed leading comment lines. */
	private int fCommentLineCount;
	/** The select all action. */
	private SelectAllAction fSelectAllAction;
	/** Element opened by the open action. */
	private IJavaElement fLastOpenedElement;


	/*
	 * @see AbstractInfoView#internalCreatePartControl(Composite)
	 */
	protected void internalCreatePartControl(Composite parent) {
		fViewer= new JavaSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL);
		fViewer.configure(new JavaSourceViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools(), null));
		fViewer.setEditable(false);

		setViewerFont();
		JFaceResources.getFontRegistry().addListener(fFontPropertyChangeListener);
		
		JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyChangeListener);

		getViewSite().setSelectionProvider(fViewer);
	}

	/*
	 * @see AbstractInfoView#internalCreatePartControl(Composite)
	 */
	protected void createActions() {
		super.createActions();
		fSelectAllAction= new SelectAllAction(fViewer);

		// Setup OpenAction		
		fOpen= new OpenAction(getViewSite()) {

			/*
			 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#getSelection()
			 */
			public ISelection getSelection() {
				return convertToJavaElementSelection(fViewer.getSelection());
			}

			/*
			 * @see org.eclipse.jdt.ui.actions.OpenAction#run(IStructuredSelection)
			 */
			public void run(IStructuredSelection selection) {
				if (selection.isEmpty()) {
					getShell().getDisplay().beep();
					return;
				}
				super.run(selection);
			}

			/*
			 * @see org.eclipse.jdt.ui.actions.OpenAction#getElementToOpen(Object)
			 */
			public Object getElementToOpen(Object object) throws JavaModelException {
				if (object instanceof IJavaElement)
					fLastOpenedElement= (IJavaElement)object;
				else
					fLastOpenedElement= null;
				return super.getElementToOpen(object);
			}

			/*
			 * @see org.eclipse.jdt.ui.actions.OpenAction#run(Object[])
			 */
			public void run(Object[] elements) {
				stopListeningForSelectionChanges();
				super.run(elements);
				startListeningForSelectionChanges();
			}
		}; 
	}

	/*
	 * @see AbstractInfoView#fillActionBars(IActionBars)
	 */
	protected void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN, fOpen);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.SELECT_ALL, fSelectAllAction);
		
	}

	/*
	 * @see AbstractInfoView#getControl()
	 */
	protected Control getControl() {
		return fViewer.getControl();
	}

	/*
	 * @see AbstractInfoView#menuAboutToShow(IMenuManager)
	 */
	public void menuAboutToShow(IMenuManager menu) {
		super.menuAboutToShow(menu);
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, fSelectAllAction);
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpen);
	}

	/*
	 * @see AbstractInfoView#setForeground(Color)
	 */
	protected void setForeground(Color color) {
		fViewer.getTextWidget().setForeground(color);
	}

	/*
	 * @see AbstractInfoView#setBackground(Color)
	 */
	protected void setBackground(Color color) {
		fViewer.getTextWidget().setBackground(color);
	}

	/**
	 * Converts the given selection to a structured selection
	 * containing Java elements.
	 * 
	 * @param selection the selection
	 * @return a structured selection with Java elements
	 */
	private IStructuredSelection convertToJavaElementSelection(ISelection selection) {
		
		if (!(selection instanceof ITextSelection && fCurrentViewInput instanceof ISourceReference))
			return StructuredSelection.EMPTY;

		ITextSelection textSelection= (ITextSelection)selection;
	
		Object codeAssist= fCurrentViewInput.getAncestor(IJavaElement.COMPILATION_UNIT); 
		if (codeAssist == null)
			codeAssist= fCurrentViewInput.getAncestor(IJavaElement.CLASS_FILE);

		if (codeAssist instanceof ICodeAssist) {
			IJavaElement[] elements= null;
			try {
				ISourceRange range= ((ISourceReference)fCurrentViewInput).getSourceRange();
				elements= ((ICodeAssist)codeAssist).codeSelect(range.getOffset() + getOffsetInUnclippedDocument(textSelection), textSelection.getLength());
			} catch (JavaModelException e) {
				return StructuredSelection.EMPTY;
			}
			if (elements != null && elements.length > 0) {
				return new StructuredSelection(elements[0]);
			} else
				return StructuredSelection.EMPTY;
		}

		return StructuredSelection.EMPTY;
	}

	/**
	 * Computes and returns the offset in the unclipped document
	 * based on the given text selection from the clipped
	 * document.
	 * 
	 * @param textSelection
	 * @return the offest in the unclipped document or <code>-1</code> if the offset cannot be computed
	 */
	private int getOffsetInUnclippedDocument(ITextSelection textSelection) {
		IDocument unclippedDocument= null;
		try {
			unclippedDocument= new Document(((ISourceReference)fCurrentViewInput).getSource());
		} catch (JavaModelException e) {
			return -1;
		}
		IDocument clippedDoc= (IDocument)fViewer.getInput();
		try {
			IRegion unclippedLineInfo= unclippedDocument.getLineInformation(fCommentLineCount + textSelection.getStartLine());
			IRegion clippedLineInfo= clippedDoc.getLineInformation(textSelection.getStartLine());
			int removedIndentation= unclippedLineInfo.getLength() - clippedLineInfo.getLength();
			int relativeLineOffset= textSelection.getOffset() - clippedLineInfo.getOffset();
			return unclippedLineInfo.getOffset() + removedIndentation + relativeLineOffset ;
		} catch (BadLocationException ex) {
			return -1;
		}
	}
	
	/*
	 * @see AbstractInfoView#internalDispose()
	 */
	protected void internalDispose() {
		fViewer= null;
		JFaceResources.getFontRegistry().removeListener(fFontPropertyChangeListener);
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyChangeListener);
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		fViewer.getTextWidget().setFocus();
	}
	
	/*
	 * @see AbstractInfoView#computeInput(Object)
	 */
	protected Object computeInput(Object input) {

		if (fViewer == null || !(input instanceof ISourceReference))
			return null;

		ISourceReference sourceRef= (ISourceReference)input;
		
		if (fLastOpenedElement != null && input instanceof IJavaElement && ((IJavaElement)input).getHandleIdentifier().equals(fLastOpenedElement.getHandleIdentifier())) {
			fLastOpenedElement= null;
			return null;
		} else {
			fLastOpenedElement= null;
		}

		String source;
		try {
			source= sourceRef.getSource();
		} catch (JavaModelException ex) {
			return null;
		}
		
		if (source == null)
			return null;

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
		Strings.trimIndentation(sourceLines, CodeFormatterUtil.getTabWidth());

		if (!Character.isWhitespace(firstLine.charAt(0)))
			sourceLines[0]= firstLine;

		source= Strings.concatenate(sourceLines, delim);

		IDocument doc= new Document(source);
		JavaPlugin.getDefault().getJavaTextTools().setupJavaDocumentPartitioner(doc);
		return doc;
	}

	/*
	 * @see AbstractInfoView#setInput(Object)
	 */
	protected void setInput(Object input) {
		fViewer.setInput(input);
	}

	/**
	 * Removes the leading comments from the given source.
	 * 
	 * @param source the string with the source
	 * @return the source without leading comments
	 */
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
			fCommentLineCount= document.getLineOfOffset(i);
		} catch (BadLocationException e) {
			fCommentLineCount= 0;
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
}
