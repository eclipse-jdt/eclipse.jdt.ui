/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genady Beryozkin <eclipse@genady.org> - [misc] Display values for constant fields in the Javadoc view - https://bugs.eclipse.org/bugs/show_bug.cgi?id=204914
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.infoviews;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

/**
 * Abstract class for views which show information for a given element.
 *
 * @since 3.0
 */
public abstract class AbstractInfoView extends ViewPart implements ISelectionListener, IMenuListener, IPropertyChangeListener {


	/** JavaElementLabels flags used for the title */
	private final long TITLE_FLAGS=  JavaElementLabels.ALL_FULLY_QUALIFIED
		| JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_EXCEPTIONS
		| JavaElementLabels.F_PRE_TYPE_SIGNATURE | JavaElementLabels.M_PRE_TYPE_PARAMETERS | JavaElementLabels.T_TYPE_PARAMETERS
		| JavaElementLabels.USE_RESOLVED;
	private final long LOCAL_VARIABLE_TITLE_FLAGS= TITLE_FLAGS & ~JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.F_POST_QUALIFIED;
	private final long TYPE_PARAMETER_TITLE_FLAGS= TITLE_FLAGS | JavaElementLabels.TP_POST_QUALIFIED;

	/** JavaElementLabels flags used for the tool tip text */
	private static final long TOOLTIP_LABEL_FLAGS= JavaElementLabels.DEFAULT_QUALIFIED | JavaElementLabels.ROOT_POST_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH |
			JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.M_EXCEPTIONS |
			JavaElementLabels.F_APP_TYPE_SIGNATURE | JavaElementLabels.T_TYPE_PARAMETERS;


	/*
	 * @see IPartListener2
	 */
	private IPartListener2 fPartListener= new IPartListener2() {
		public void partVisible(IWorkbenchPartReference ref) {
			if (ref.getId().equals(getSite().getId())) {
				IWorkbenchPart activePart= ref.getPage().getActivePart();
				if (activePart != null)
					selectionChanged(activePart, ref.getPage().getSelection());
				startListeningForSelectionChanges();
			}
		}
		public void partHidden(IWorkbenchPartReference ref) {
			if (ref.getId().equals(getSite().getId()))
				stopListeningForSelectionChanges();
		}
		public void partInputChanged(IWorkbenchPartReference ref) {
			if (!ref.getId().equals(getSite().getId()))
				computeAndSetInput(ref.getPart(false));
		}
		public void partActivated(IWorkbenchPartReference ref) {
		}
		public void partBroughtToTop(IWorkbenchPartReference ref) {
		}
		public void partClosed(IWorkbenchPartReference ref) {
		}
		public void partDeactivated(IWorkbenchPartReference ref) {
		}
		public void partOpened(IWorkbenchPartReference ref) {
		}
	};


	/** The current input. */
	protected IJavaElement fCurrentViewInput;
	/** The copy to clipboard action. */
	private SelectionDispatchAction fCopyToClipboardAction;
	/** The goto input action. */
	private GotoInputAction fGotoInputAction;
	/** Counts the number of background computation requests. */
	private volatile int fComputeCount;

	/**
	 * Progress monitor used to cancel pending computations.
	 * @since 3.4
	 */
	private IProgressMonitor fComputeProgressMonitor;

	/**
	 * Background color.
	 * @since 3.2
	 */
	private Color fBackgroundColor;
	private RGB fBackgroundColorRGB;

	/**
	 * True if linking with selection is enabled, false otherwise.
	 * @since 3.4
	 */
	private boolean fLinking= true;

	/**
	 * The last selected element if linking was disabled.
	 * @since 3.4
	 */
	private IJavaElement fLastSelection;

	/**
	 * Set the input of this view.
	 *
	 * @param input the input object
	 */
	abstract protected void doSetInput(Object input);

	/**
	 * Computes the input for this view based on the given element.
	 *
	 * @param element the element from which to compute the input
	 * @return	the input or <code>null</code> if the input was not computed successfully
	 */
	abstract protected Object computeInput(Object element);

	/**
	 * Computes the input for this view based on the given elements
	 *
	 * @param part the part that triggered the current element update, or <code>null</code>
	 * @param selection the new selection, or <code>null</code>
	 * @param element the new java element that will be displayed
	 * @param monitor a progress monitor
	 * @return the input or <code>null</code> if the input was not computed successfully
	 * @since 3.4
	 */
	protected Object computeInput(IWorkbenchPart part, ISelection selection, IJavaElement element, IProgressMonitor monitor) {
		return computeInput(element);
	}

	/**
	 * Create the part control.
	 *
 	 * @param parent the parent control
	 * @see IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	abstract protected void internalCreatePartControl(Composite parent);

	/**
	 * Set the view's foreground color.
	 *
	 * @param color the SWT color
	 */
	abstract protected void setForeground(Color color);

	/**
	 * Set the view's background color.
	 *
	 * @param color the SWT color
	 */
	abstract protected void setBackground(Color color);

	/**
	 * Returns the view's primary control.
	 *
	 * @return the primary control
	 */
	abstract Control getControl();

	/**
	 * Returns the context ID for the Help system
	 *
	 * @return	the string used as ID for the Help context
	 * @since 3.1
	 */
	abstract protected String getHelpContextId();

	/*
	 * @see IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public final void createPartControl(Composite parent) {
		internalCreatePartControl(parent);
		inititalizeColors();
		getSite().getWorkbenchWindow().getPartService().addPartListener(fPartListener);
		createContextMenu();
		createActions();
		fillActionBars(getViewSite().getActionBars());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());
	}

	/**
	 * Creates the actions and action groups for this view.
	 */
	protected void createActions() {
		fGotoInputAction= new GotoInputAction(this);
		fGotoInputAction.setEnabled(false);
		fCopyToClipboardAction= new CopyToClipboardAction(getViewSite());

		ISelectionProvider provider= getSelectionProvider();
		if (provider != null)
			provider.addSelectionChangedListener(fCopyToClipboardAction);
	}

	/**
	 * Creates the context menu for this view.
	 */
	protected void createContextMenu() {
		MenuManager menuManager= new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(this);
		Menu contextMenu= menuManager.createContextMenu(getControl());
		getControl().setMenu(contextMenu);
		getSite().registerContextMenu(menuManager, getSelectionProvider());
	}

	/*
	 * @see IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	public void menuAboutToShow(IMenuManager menu) {
		menu.add(new Separator(IContextMenuConstants.GROUP_GOTO));
		menu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
		menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));
		menu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));

		IAction action;

		action= getCopyToClipboardAction();
		if (action != null)
			menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, action);

		action= getSelectAllAction();
		if (action != null)
			menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, action);

		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fGotoInputAction);
	}

	protected IAction getSelectAllAction() {
		return null;
	}

	protected IAction getCopyToClipboardAction() {
		return fCopyToClipboardAction;
	}

	/**
	 * Returns the input of this view.
	 *
	 * @return input the input object or <code>null</code> if not input is set
	 */
	protected IJavaElement getInput() {
		return fCurrentViewInput;
	}

	// Helper method
	ISelectionProvider getSelectionProvider() {
		return getViewSite().getSelectionProvider();
	}

	/**
	 * Fills the actions bars.
	 * <p>
	 * Subclasses may extend.
	 *
	 * @param actionBars the action bars
	 */
	protected void fillActionBars(IActionBars actionBars) {
		IToolBarManager toolBar= actionBars.getToolBarManager();
		fillToolBar(toolBar);

		IAction action;

		action= getCopyToClipboardAction();
		if (action != null)
			actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), action);

		action= getSelectAllAction();
		if (action != null)
			actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), action);
	}

	/**
	 * Fills the tool bar.
	 * <p>
	 * Default is to do nothing.</p>
	 *
	 * @param tbm the tool bar manager
	 */
	protected void fillToolBar(IToolBarManager tbm) {
		tbm.add(fGotoInputAction);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.infoviews.AbstractInfoView#inititalizeColors()
	 * @since 3.2
	 */
	private void inititalizeColors() {
		if (getSite().getShell().isDisposed())
			return;

		Display display= getSite().getShell().getDisplay();
		if (display == null || display.isDisposed())
			return;

		setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));

		ColorRegistry registry= JFaceResources.getColorRegistry();
		registry.addListener(this);

		fBackgroundColorRGB= registry.getRGB(getBackgroundColorKey());
		Color bgColor;
		if (fBackgroundColorRGB == null) {
			bgColor= display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
			fBackgroundColorRGB= bgColor.getRGB();
		} else {
			bgColor= new Color(display, fBackgroundColorRGB);
			fBackgroundColor= bgColor;
		}

		setBackground(bgColor);
	}

	/**
	 * The preference key for the background color.
	 *
	 * @return the background color key
	 * @since 3.2
	 */
	abstract protected String getBackgroundColorKey();

	public void propertyChange(PropertyChangeEvent event) {
		if (getBackgroundColorKey().equals(event.getProperty()))
			inititalizeColors();
	}

	/**
	 * Start to listen for selection changes.
	 */
	protected void startListeningForSelectionChanges() {
		getSite().getPage().addPostSelectionListener(this);
	}

	/**
	 * Stop to listen for selection changes.
	 */
	protected void stopListeningForSelectionChanges() {
		getSite().getPage().removePostSelectionListener(this);
	}

	/**
	 * Sets whether this info view reacts to selection
	 * changes in the workbench.
	 *
	 * @param enabled if true then the input is set on selection changes
	 */
	protected void setLinkingEnabled(boolean enabled) {
		fLinking= enabled;

		if (fLinking && fLastSelection != null) {
			setInput(fLastSelection);
		}
	}

	/**
	 * Returns whether this info view reacts to selection
	 * changes in the workbench.
	 *
	 * @return true if linking with selection is enabled
	 */
	protected boolean isLinkingEnabled() {
		return fLinking;
	}

	/*
	 * @see ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part.equals(this))
			return;

		if (!fLinking) {
			IJavaElement javaElement= findSelectedJavaElement(part, selection);
			if (javaElement != null)
				fLastSelection= javaElement;
		} else {
			fLastSelection= null;
			computeAndSetInput(part);
		}
	}

	/**
	 * Tells whether the new input should be ignored
	 * if the current input is the same.
	 *
	 * @param je the new input, may be <code>null</code>
	 * @param part the workbench part
	 * @param selection the current selection from the part that provides the input
	 * @return <code>true</code> if the new input should be ignored
	 */
	protected boolean isIgnoringNewInput(IJavaElement je, IWorkbenchPart part, ISelection selection) {
		return fCurrentViewInput != null && fCurrentViewInput.equals(je) && je != null;
	}

	/**
	 * Finds and returns the Java element selected in the given part.
	 *
	 * @param part the workbench part for which to find the selected Java element
	 * @param selection the selection
	 * @return the selected Java element
	 */
	protected IJavaElement findSelectedJavaElement(IWorkbenchPart part, ISelection selection) {
		Object element;
		try {
			if (part instanceof JavaEditor && selection instanceof ITextSelection) {
				IJavaElement[] elements= TextSelectionConverter.codeResolve((JavaEditor)part, (ITextSelection)selection);
				if (elements != null && elements.length > 0)
					return elements[0];
				else
					return null;
			} else if (selection instanceof IStructuredSelection) {
				element= SelectionUtil.getSingleElement(selection);
			} else {
				return null;
			}
		} catch (JavaModelException e) {
			return null;
		}

		return findJavaElement(element);
	}

	/**
	 * Tries to get a Java element out of the given element.
	 *
	 * @param element an object
	 * @return the Java element represented by the given element or <code>null</code>
	 */
	private IJavaElement findJavaElement(Object element) {

		if (element == null)
			return null;

		IJavaElement je= null;
		if (element instanceof IAdaptable)
			je= (IJavaElement)((IAdaptable)element).getAdapter(IJavaElement.class);

		return je;
	}

	/**
	 * Finds and returns the type for the given CU.
	 *
	 * @param cu	the compilation unit
	 * @return	the type with same name as the given CU or the first type in the CU
	 */
	protected IType getTypeForCU(ICompilationUnit cu) {

		if (cu == null || !cu.exists())
			return null;

		// Use primary type if possible
		IType primaryType= cu.findPrimaryType();
		if (primaryType != null)
			return primaryType;

		// Use first top-level type
		try {
			IType[] types= cu.getTypes();
			if (types.length > 0)
				return types[0];
			else
				return null;
		} catch (JavaModelException ex) {
			return null;
		}
	}

	/*
	 * @see IWorkbenchPart#dispose()
	 */
	public final void dispose() {
		// cancel possible running computation
		fComputeCount++;
		if (fComputeProgressMonitor != null)
			fComputeProgressMonitor.setCanceled(true);

		getSite().getWorkbenchWindow().getPartService().removePartListener(fPartListener);

		ISelectionProvider provider= getSelectionProvider();
		if (provider != null)
			provider.removeSelectionChangedListener(fCopyToClipboardAction);

		JFaceResources.getColorRegistry().removeListener(this);
		fBackgroundColorRGB= null;
		if (fBackgroundColor != null) {
			fBackgroundColor.dispose();
			fBackgroundColor= null;
		}

		internalDispose();

	}

	/*
	 * @see IWorkbenchPart#dispose()
	 */
	abstract protected void internalDispose();

	/**
	 * Determines all necessary details and delegates the computation into
	 * a background thread.
	 *
	 * @param part the workbench part
	 */
	private void computeAndSetInput(final IWorkbenchPart part) {
		computeAndDoSetInput(part, null);
	}

	/**
	 * Sets the input for this view.
	 *
	 * @param element the java element
	 */
	public final void setInput(final IJavaElement element) {
		computeAndDoSetInput(null, element);
	}

	/**
	 * Determines all necessary details and delegates the computation into
	 * a background thread. One of part or element must be non-null.
	 *
	 * @param part the workbench part, or <code>null</code> if <code>element</code> not <code>null</code>
	 * @param element the java element, or <code>null</code> if <code>part</code> not <code>null</code>
	 */
	private void computeAndDoSetInput(final IWorkbenchPart part, final IJavaElement element) {
		Assert.isLegal(part != null || element != null);

		final int currentCount= ++fComputeCount;

		final ISelection selection;
		if (element != null)
			selection= null;
		else {
			ISelectionProvider provider= part.getSite().getSelectionProvider();
			if (provider == null)
				return;

			selection= provider.getSelection();
			if (selection == null || selection.isEmpty())
				return;
		}

		if (fComputeProgressMonitor != null)
			fComputeProgressMonitor.setCanceled(true);
		final IProgressMonitor computeProgressMonitor= new NullProgressMonitor();
		fComputeProgressMonitor= computeProgressMonitor;

		Thread thread= new Thread("Info view input computer") { //$NON-NLS-1$
			public void run() {
				if (currentCount != fComputeCount)
					return;

				final IJavaElement je;
				if (element != null)
					je= element;
				else {
					je= findSelectedJavaElement(part, selection);
					if (isIgnoringNewInput(je, part, selection))
						return;
				}


				// The actual computation
				final Object input= computeInput(part, selection, je, computeProgressMonitor);
				if (input == null)
					return;

				final String description= computeDescription(part, selection, je, computeProgressMonitor);

				Shell shell= getSite().getShell();
				if (shell.isDisposed())
					return;

				Display display= shell.getDisplay();
				if (display.isDisposed())
					return;

				display.asyncExec(new Runnable() {
					/*
					 * @see java.lang.Runnable#run()
					 */
					public void run() {

						if (fComputeCount != currentCount || getViewSite().getShell().isDisposed())
							return;

						fCurrentViewInput= je;
						doSetInput(input, description);

						fComputeProgressMonitor= null;
					}
				});
			}
		};

		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	/**
	 * Computes the contents description that will be displayed for the current element.
	 *
	 * @param part the part that triggered the current element update, or <code>null</code>
	 * @param selection the new selection, or <code>null</code>
	 * @param inputElement the new java element that will be displayed
	 * @param localASTMonitor a progress monitor
	 * @return the contents description for the provided <code>inputElement</code>
	 * @since 3.4
	 */
	protected String computeDescription(IWorkbenchPart part, ISelection selection, IJavaElement inputElement, IProgressMonitor localASTMonitor) {
		long flags;
		if (inputElement instanceof ILocalVariable)
			flags= LOCAL_VARIABLE_TITLE_FLAGS;
		else if (inputElement instanceof ITypeParameter)
			flags= TYPE_PARAMETER_TITLE_FLAGS;
		else
			flags= TITLE_FLAGS;

		return JavaElementLabels.getElementLabel(inputElement, flags);
	}

	private void doSetInput(Object input, String description) {
		doSetInput(input);

		fGotoInputAction.setEnabled(true);

		IJavaElement inputElement= getInput();

		setContentDescription(description);
		setTitleToolTip(JavaElementLabels.getElementLabel(inputElement, TOOLTIP_LABEL_FLAGS));
	}
}
