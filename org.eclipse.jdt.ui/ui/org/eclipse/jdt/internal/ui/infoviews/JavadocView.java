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

import java.io.Reader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocAccess;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.HTMLPrinter;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * View which shows Javadoc for a given Java element.
 * 
 * @since 3.0
 */
public class JavadocView extends AbstractInfoView {

	/** Flags used to render a label in the text widget. */
	private static final int LABEL_FLAGS=  JavaElementLabels.ALL_FULLY_QUALIFIED
		| JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_EXCEPTIONS 
		| JavaElementLabels.F_PRE_TYPE_SIGNATURE;

	/** The styled text widget. */
	private StyledText fText;
	/** The information presenter. */
	private DefaultInformationControl.IInformationPresenter fPresenter;
	/** The text presentation. */
	private TextPresentation fPresentation= new TextPresentation();
	/** The select all action */
	private SelectAllAction fSelectAllAction;

	/**
	 * The Javadoc view's select all action.
	 */
	private static class SelectAllAction extends Action {

		/** The styled text widget. */
		private StyledText fStyledText;
		/** The selection provider. */
		private SelectionProvider fSelectionProvider;

		/**
		 * Creates the action.
		 */
		public SelectAllAction(StyledText styledText, SelectionProvider selectionProvider) {
			super("selectAll"); //$NON-NLS-1$

			Assert.isNotNull(styledText);
			Assert.isNotNull(selectionProvider);
			fStyledText= styledText;
			fSelectionProvider= selectionProvider;

			setText(InfoViewMessages.getString("SelectAllAction.label")); //$NON-NLS-1$
			setToolTipText(InfoViewMessages.getString("SelectAllAction.tooltip")); //$NON-NLS-1$
			setDescription(InfoViewMessages.getString("SelectAllAction.description")); //$NON-NLS-1$

			WorkbenchHelp.setHelp(this, IAbstractTextEditorHelpContextIds.SELECT_ALL_ACTION);
		}

		/**
		 * Selects all in the view.
		 */
		public void run() {
			fStyledText.selectAll();
			fSelectionProvider.fireSelectionChanged();
		}
	}

	/**
	 * The Javadoc view's selection provider.
	 */
	private static class SelectionProvider implements ISelectionProvider {

		/** The selection changed listeners. */
		private ListenerList fListeners= new ListenerList();
		/** The styled text widget. */
		private StyledText fStyledText;

		/**
		 * Creates a new selection provider.
		 * 
		 * @param styledText	the styled text widget
		 */
		public SelectionProvider(StyledText styledText) {
			Assert.isNotNull(styledText);
			fStyledText= styledText;
			
			fStyledText.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fireSelectionChanged();
				}
			});
		}
		
		/**
		 * Sends a selection changed event to all listeners.
		 */
		public void fireSelectionChanged() {
			ISelection selection= getSelection();
			SelectionChangedEvent event= new SelectionChangedEvent(this, selection);
			Object[] selectionChangedListeners= fListeners.getListeners();
			for (int i= 0; i < selectionChangedListeners.length; i++)
				((ISelectionChangedListener)selectionChangedListeners[i]).selectionChanged(event);
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			fListeners.add(listener);
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
		 */
		public ISelection getSelection() {
			IDocument document= new Document(fStyledText.getSelectionText());
			return new TextSelection(document, 0, document.getLength());
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			fListeners.remove(listener);
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
		 */
		public void setSelection(ISelection selection) {
			// not supported
		}
	}

	/*
	 * @see AbstractInfoView#internalCreatePartControl(Composite)
	 */
	protected void internalCreatePartControl(Composite parent) {
		fText= new StyledText(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		fText.setEditable(false);
		fPresenter= new HTMLTextPresenter(false);
		getViewSite().setSelectionProvider(new SelectionProvider(fText));
	}
	
	/*
	 * @see AbstractInfoView#createActions()
	 */
	protected void createActions() {
		super.createActions();
		fSelectAllAction= new SelectAllAction(fText, (SelectionProvider)getSelectionProvider());
	}

	/*
 	 * @see AbstractInfoView#setForeground(Color)
 	 */
	protected void setForeground(Color color) {
		fText.setForeground(color);
	}

	/*
	 * @see AbstractInfoView#setBackground(Color)
	 */
	protected void setBackground(Color color) {
		fText.setBackground(color);
	}

	/*
	 * @see AbstractInfoView#internalDispose()
	 */
	protected void internalDispose() {
		fText= null;
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		fText.setFocus();
	}

	/*
	 * @see AbstractInfoView#computeInput(Object)
	 */
	protected Object computeInput(Object input) {
		if (fText == null || ! (input instanceof IJavaElement))
			return null;

		IJavaElement je= (IJavaElement)input;
		String javadocHtml= null;
		if (je.getElementType() == IJavaElement.COMPILATION_UNIT) {
			try {
				javadocHtml= getJavadocHtml(((ICompilationUnit)je).getTypes());
			} catch (JavaModelException ex) {
				return null;
			}
		} else
			javadocHtml= getJavadocHtml(new IJavaElement[] { je });

		return javadocHtml;
	}

	/*
	 * @see AbstractInfoView#setInput(Object)
	 */
	protected void setInput(Object input) {
		String javadocHtml= (String)input;
		
		fPresentation.clear();
		Point size= fText.getSize();
		try {
			javadocHtml= fPresenter.updatePresentation(getSite().getShell().getDisplay(), javadocHtml, fPresentation, size.x, size.y);
		} catch (IllegalArgumentException ex) {
			// the javadoc might no longer be valid
			return;
		}
		fText.setText(javadocHtml);
		TextPresentation.applyTextPresentation(fPresentation, fText);
	}

	/**
	 * Returns the Javadoc in HTML format.
	 * 
	 * @param result the Java elements for which to get the Javadoc
	 * @return a string with the Javadoc in HTML format. 
	 */
	private String getJavadocHtml(IJavaElement[] result) {
		StringBuffer buffer= new StringBuffer();
		int nResults= result.length;
		
		if (nResults > 1) {
			
			for (int i= 0; i < result.length; i++) {
				HTMLPrinter.startBulletList(buffer);
				IJavaElement curr= result[i];
				if (curr instanceof IMember)
					HTMLPrinter.addBullet(buffer, getInfoText((IMember) curr));
				HTMLPrinter.endBulletList(buffer);
			}
			
		} else {
			
			IJavaElement curr= result[0];
			if (curr instanceof IMember) {
				IMember member= (IMember) curr;
//				HTMLPrinter.addSmallHeader(buffer, getInfoText(member));
				Reader reader;
				try {
					reader= JavaDocAccess.getJavaDoc(member, true);
				} catch (JavaModelException ex) {
					return null;
				}
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, new JavaDoc2HTMLTextReader(reader));
				}
			}
		}
		
		if (buffer.length() > 0) {
			HTMLPrinter.insertPageProlog(buffer, 0);
			HTMLPrinter.addPageEpilog(buffer);
			return buffer.toString();
		}
		
		return null;
	}

	/**
	 * Gets the label for the given member.
	 * 
	 * @param member the Java member
	 * @return a string containing the member's label
	 */
	private String getInfoText(IMember member) {
		return JavaElementLabels.getElementLabel(member, LABEL_FLAGS);
	}

	/*
	 * @see AbstractInfoView#isUpdatingIfSameInput() 
	 */
	protected boolean isIngoringEqualInput() {
		return false;
	}

	/*
	 * @see AbstractInfoView#findSelectedJavaElement(IWorkbenchPart) 
	 */
	protected IJavaElement findSelectedJavaElement(IWorkbenchPart part, ISelection selection) {
		IJavaElement element;
		try {
			element= super.findSelectedJavaElement(part, selection);
		
			if (element == null && part instanceof JavaEditor && selection instanceof ITextSelection) {
			
				JavaEditor editor= (JavaEditor)part;
				ITextSelection textSelection= (ITextSelection)selection;

				IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
				if (document == null)
					return null;
				
				ITypedRegion typedRegion= document.getPartition(textSelection.getOffset());
				if (IJavaPartitions.JAVA_DOC.equals(typedRegion.getType()))
					return TextSelectionConverter.getElementAtOffset((JavaEditor)part, textSelection);
				else
					return null;
			} else
				return element;
		} catch (JavaModelException e) {
			return null;
		} catch (BadLocationException e) {
			return null;
		}
	}

	/*
	 * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	public void menuAboutToShow(IMenuManager menu) {
		super.menuAboutToShow(menu);
		menu.add(fSelectAllAction);
	}
	
	/*
	 * @see AbstractInfoView#fillActionBars(IActionBars)
	 */
	protected void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.SELECT_ALL, fSelectAllAction);
		
	}

	/*
	 * @see AbstractInfoView#getControl()
	 */
	protected Control getControl() {
		return fText;
	}
}
