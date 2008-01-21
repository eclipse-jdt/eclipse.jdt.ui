/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.breadcrumb;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.texteditor.ITextEditor;


/**
 * The editor breadcrumb shows the parent chain of the active
 * editor item inside a {@link BreadcrumbViewer}.
 * 
 * <p>Clients must implement the abstract methods.</p>
 * 
 * @since 3.4
 */
public abstract class EditorBreadcrumb implements IBreadcrumb {

	private static final String ACTIVE_TAB_BG_END= "org.eclipse.ui.workbench.ACTIVE_TAB_BG_END"; //$NON-NLS-1$
	
	private ITextEditor fTextEditor;
	private ITextViewer fTextViewer;

	private BreadcrumbViewer fBreadcrumbViewer;

	private boolean fHasFocus;
	private boolean fIsActive;

	private Composite fComposite;

	private Listener fDisplayFocusListener;
	private Listener fDisplayMenuListener;
	private Listener fDisplayKeyListener;

	private IPropertyChangeListener fPropertyChangeListener;

	
	/**
	 * The editor inside which this breadcrumb is shown.
	 * 
	 * @param editor the editor
	 */
	public EditorBreadcrumb(ITextEditor editor) {
		setTextEditor(editor);
	}

	/**
	 * The active element of the editor.
	 * 
	 * @return the active element of the editor, or <b>null</b> if none.
	 */
	protected abstract Object getCurrentInput();

	/**
	 * Create and configure the viewer used to display
	 * the parent chain.
	 * 
	 * @param parent the parent composite
	 * @return the viewer
	 */
	protected abstract BreadcrumbViewer createViewer(Composite parent);

	/**
	 * Reveal the given element in the editor if possible.
	 * 
	 * @param element the element to reveal
	 * @return true if the element could be revealed
	 */
	protected abstract boolean reveal(Object element);

	/**
	 * Open the element in a new editor if possible.
	 * 
	 * @param element the element to open
	 * @return true if the element could be opened
	 */
	protected abstract boolean open(Object element);

	/**
	 * Fill the given menu manager with context menu actions for
	 * the current viewers selection.
	 * 
	 * @param manager the manager to which to add menu entries too
	 */
	protected abstract void fillContextMenu(MenuManager manager);
	
	/**
	 * The breadcrumb has been activated. Implementors must retarget
	 * the editor actions to the breadcrumb aware actions.
	 */
	protected abstract void activateBreadcrumb();

	/**
	 * The breadcrumb has been deactivated. Implementors must retarget
	 * the breadcrumb actions to the editor actions.
	 */
	protected abstract void deactivateBreadcrumb();
	
	public ISelectionProvider getSelectionProvider() {
		return fBreadcrumbViewer;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IBreadcrumb#setTextViewer(org.eclipse.jface.text.ITextViewer)
	 */
	protected void setTextViewer(ITextViewer viewer) {
		fTextViewer= viewer;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IBreadcrumb#setInput(java.lang.Object)
	 */
	public void setInput(Object element) {
		if (element == null)
			return;

		if (fBreadcrumbViewer.getInput() == element || element.equals(fBreadcrumbViewer.getInput()))
			return;
		
		if (fBreadcrumbViewer.isDropDownOpen())
			return;
		
		fBreadcrumbViewer.setInput(element);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IBreadcrumb#setFocus()
	 */
	public void activate() {
		if (fBreadcrumbViewer.getSelection().isEmpty())
			fBreadcrumbViewer.setSelection(new StructuredSelection(fBreadcrumbViewer.getInput()));
		fBreadcrumbViewer.setFocus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.IBreadcrumb#isActive()
	 */
	public boolean isActive() {
		return fIsActive;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IBreadcrumb#createContent(org.eclipse.swt.widgets.Composite)
	 */
	public Control createContent(Composite parent) {
		if (fComposite != null)
			new IllegalArgumentException("Content must only be created once."); //$NON-NLS-1$
		
		fComposite= new Composite(parent, SWT.NONE);
		GridData data= new GridData(SWT.FILL, SWT.TOP, true, false);
		fComposite.setLayoutData(data);
		GridLayout gridLayout= new GridLayout(1, false);
		gridLayout.marginWidth= 1;
		gridLayout.marginHeight= 1;
		gridLayout.verticalSpacing= 0;
		gridLayout.horizontalSpacing= 0;
		fComposite.setLayout(gridLayout);

		fDisplayFocusListener= new Listener() {
			public void handleEvent(Event event) {
				if (isBreadcrumbEvent(event)) {
					if (fHasFocus)
						return;
					
					fIsActive= true;

					focusGained();
				} else {
					if (!fHasFocus)
						return;
					
					boolean hasTextFocus= fTextViewer.getTextWidget().isFocusControl();
					if (hasTextFocus) {
						fIsActive= false;
					}
	
					focusLost();
					
					if (hasTextFocus) {
						fBreadcrumbViewer.setInput(getCurrentInput());
					}
				}
			}
		};
		Display.getCurrent().addFilter(SWT.FocusIn, fDisplayFocusListener);

		fBreadcrumbViewer= createViewer(fComposite);
		fBreadcrumbViewer.getControl().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

		fBreadcrumbViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (doReveal(event.getSelection())) {
					fTextViewer.getTextWidget().setFocus();
				} else if (doOpen(event.getSelection())) {
					fTextViewer.getTextWidget().setFocus();
					fBreadcrumbViewer.setInput(getCurrentInput());
				}
			}
		});

		fBreadcrumbViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				if (doReveal(event.getSelection())) {
					fTextViewer.getTextWidget().setFocus();
				} else if (doOpen(event.getSelection())) {
					fTextViewer.getTextWidget().setFocus();
					fBreadcrumbViewer.setInput(getCurrentInput());
				}
			}
		});
		
		fBreadcrumbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection selection= (StructuredSelection) event.getSelection();
				if (selection.size() != 1)
					return;

				Object element= selection.getFirstElement();
				if (fBreadcrumbViewer.getInput() == element || fBreadcrumbViewer.getInput().equals(element)) {
					if (doReveal(selection)) {
						fBreadcrumbViewer.setFocus();
						fBreadcrumbViewer.setInput(element);
					}
				}
			}
		});

		fPropertyChangeListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (ACTIVE_TAB_BG_END.equals(event.getProperty())) {
					if (fComposite.isFocusControl()) {
						fComposite.setBackground(JFaceResources.getColorRegistry().get(ACTIVE_TAB_BG_END));
					}
				}
			}
		};
		JFaceResources.getColorRegistry().addListener(fPropertyChangeListener);
		
		return fComposite;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IEditorViewPart#dispose()
	 */
	public void dispose() {
		if (fPropertyChangeListener != null) {
			JFaceResources.getColorRegistry().removeListener(fPropertyChangeListener);
		}
		if (fDisplayFocusListener != null) {
			Display.getDefault().removeFilter(SWT.FocusIn, fDisplayFocusListener);
		}
		deinstallDisplayListeners();

		setTextEditor(null);
	}

	/**
	 * @param selection to open
	 * @return true if selection could be opened
	 */
	private boolean doOpen(ISelection selection) {
		if (!(selection instanceof StructuredSelection))
			return false;

		StructuredSelection structuredSelection= (StructuredSelection) selection;
		if (structuredSelection.isEmpty())
			return false;

		return open(structuredSelection.getFirstElement());
	}

	/**
	 * @param selection to reveal
	 * @return true if selection could be revealed
	 */
	private boolean doReveal(ISelection selection) {
		if (!(selection instanceof StructuredSelection))
			return false;

		StructuredSelection structuredSelection= (StructuredSelection) selection;
		if (structuredSelection.isEmpty())
			return false;

		return reveal(structuredSelection.getFirstElement());
	}

	/**
	 * Focus has been transfered into the breadcrumb.
	 */
	private void focusGained() {
		fComposite.setBackground(JFaceResources.getColorRegistry().get(ACTIVE_TAB_BG_END));
		fHasFocus= true;
		
		installDisplayListeners();
		
		activateBreadcrumb();
		
		getTextEditor().getEditorSite().getActionBars().updateActionBars();
		
		getTextEditor().getSelectionProvider().setSelection(new StructuredSelection(this));
	}

	/**
	 * Focus has been revoked from the breadcrumb.
	 */
	private void focusLost() {
		fComposite.setBackground(null);
		fHasFocus= false;
		
		deinstallDisplayListeners();
		
		deactivateBreadcrumb();
		
		getTextEditor().getEditorSite().getActionBars().updateActionBars();
		
		getTextEditor().getSelectionProvider().setSelection(new StructuredSelection(this));
	}

	/**
	 * Installs all display listeners
	 */
	private void installDisplayListeners() {
		//Sanity check
		deinstallDisplayListeners();

		fDisplayMenuListener= new Listener() {
			public void handleEvent(Event event) {
				if (!isBreadcrumbEvent(event))
					return;

				MenuManager manager= new MenuManager();
				Menu menu= manager.createContextMenu(fBreadcrumbViewer.getControl());

				fillContextMenu(manager);

				if (manager.isEmpty())
					return;

				menu.setLocation(event.x + 10, event.y + 10);
				menu.setVisible(true);
				while (!menu.isDisposed() && menu.isVisible()) {
					if (!Display.getDefault().readAndDispatch())
						Display.getDefault().sleep();
				}
			}
		};
		Display.getDefault().addFilter(SWT.MenuDetect, fDisplayMenuListener);

		fDisplayKeyListener= new Listener() {
			public void handleEvent(Event event) {
				if (event.keyCode != SWT.ESC)
					return;

				if (!isBreadcrumbEvent(event))
					return;

				fTextViewer.getTextWidget().setFocus();
			}
		};
		Display.getDefault().addFilter(SWT.KeyDown, fDisplayKeyListener);
	}

	/**
	 * Removes all previously installed display listeners
	 */
	private void deinstallDisplayListeners() {
		if (fDisplayMenuListener != null) {
			Display.getDefault().removeFilter(SWT.MenuDetect, fDisplayMenuListener);
			fDisplayMenuListener= null;
		}

		if (fDisplayKeyListener != null) {
			Display.getDefault().removeFilter(SWT.KeyDown, fDisplayKeyListener);
			fDisplayKeyListener= null;
		}
	}

	/**
	 * Was the event issued inside the breadcrumb viewers control?
	 * 
	 * @param event the event to inspect
	 * @return true if event was generated by a breadcrumb child
	 */
	private boolean isBreadcrumbEvent(Event event) {
		if (fBreadcrumbViewer == null)
			return false;

		Widget item= event.widget;
		if (!(item instanceof Control))
			return false;

		return isChild((Control) item, fBreadcrumbViewer.getControl());
	}

	/**
	 * @param child the potential child
	 * @param parent the potential parent
	 * @return true if child is inside parent
	 */
	private boolean isChild(Control child, Control parent) {
		if (child == null)
			return false;

		if (child == parent)
			return true;

		return isChild(child.getParent(), parent);
	}

	/**
	 * @param textEditor the textEditor to set
	 */
	protected void setTextEditor(ITextEditor textEditor) {
		fTextEditor= textEditor;
	}

	/**
	 * @return the textEditor
	 */
	protected ITextEditor getTextEditor() {
		return fTextEditor;
	}

}
