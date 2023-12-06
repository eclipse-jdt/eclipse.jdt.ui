/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genady Beryozkin <eclipse@genady.org> - [misc] Display values for constant fields in the Javadoc view - https://bugs.eclipse.org/bugs/show_bug.cgi?id=204914
 *     Brock Janiczak <brockj@tpg.com.au> - [implementation] Streams not being closed in Javadoc views - https://bugs.eclipse.org/bugs/show_bug.cgi?id=214854
 *     Benjamin Muskalla <bmuskalla@innoopract.com> - [javadoc view] NPE on enumerations - https://bugs.eclipse.org/bugs/show_bug.cgi?id=223586
 *     Stephan Herrmann - Contribution for Bug 403917 - [1.8] Render TYPE_USE annotations in Javadoc hover/view
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.infoviews;

import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.internal.text.html.BrowserInput;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModularClassFile;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.OpenAttachedJavadocAction;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.SimpleSelectionProvider;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover.FallbackInformationPresenter;
import org.eclipse.jdt.internal.ui.text.javadoc.JavadocContentAccess2;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLinkedLabelComposer;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;


/**
 * View which shows Javadoc for a given Java element.
 *
 * FIXME: As of 3.0 selectAll() and getSelection() is not working
 *			see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
 *
 * @since 3.0
 */
public class JavadocView extends AbstractInfoView {

	/**
	 * Implementation of a {@link BrowserInput} using
	 * a {@link IJavaElement} as input.
	 *
	 * @since 3.4
	 */
	private static final class JavaElementBrowserInput extends BrowserInput {

		private final IJavaElement fInput;

		public JavaElementBrowserInput(BrowserInput previous, IJavaElement inputElement) {
			super(previous);
			Assert.isNotNull(inputElement);
			fInput= inputElement;
		}

		@Override
		public Object getInputElement() {
			return fInput;
		}

		@Override
		public String getInputName() {
			return fInput.getElementName();
		}
	}

	/**
	 * Implementation of a {@link BrowserInput} using an
	 * {@link URL} as input.
	 *
	 * @since 3.4
	 */
	private static class URLBrowserInput extends BrowserInput {

		private final URL fURL;

		public URLBrowserInput(BrowserInput previous, URL url) {
			super(previous);
			Assert.isNotNull(url);
			fURL= url;
		}

		@Override
		public Object getInputElement() {
			return fURL;
		}

		@Override
		public String getInputName() {
			return fURL.toExternalForm();
		}
	}

	/**
	 * Action to go forward in the history.
	 *
	 * @since 3.4
	 */
	private final class ForthAction extends Action {

		public ForthAction() {
			setText(InfoViewMessages.JavadocView_action_forward_name);
			ISharedImages images= PlatformUI.getWorkbench().getSharedImages();
			setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
			setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD_DISABLED));

			update();
		}

		public void update() {
			if (fCurrent != null && fCurrent.getNext() != null) {
				BrowserInput element= fCurrent.getNext();
				setToolTipText(Messages.format(InfoViewMessages.JavadocView_action_forward_enabledTooltip, BasicElementLabels.getJavaElementName(element.getInputName())));
				setEnabled(true);
			} else {
				setToolTipText(InfoViewMessages.JavadocView_action_forward_disabledTooltip);
				setEnabled(false);
			}
		}

		@Override
		public void run() {
			setInput(fCurrent.getNext());
		}

	}

	/**
	 * Action to go backwards in the history.
	 *
	 * @since 3.4
	 */
	private final class BackAction extends Action {

		public BackAction() {
			setText(InfoViewMessages.JavadocView_action_back_name);
			ISharedImages images= PlatformUI.getWorkbench().getSharedImages();
			setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_BACK));
			setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_BACK_DISABLED));

			update();
		}

		private void update() {
			if (fCurrent != null && fCurrent.getPrevious() != null) {
				BrowserInput element= fCurrent.getPrevious();
				setToolTipText(Messages.format(InfoViewMessages.JavadocView_action_back_enabledTooltip, BasicElementLabels.getJavaElementName(element.getInputName())));
				setEnabled(true);
			} else {
				setToolTipText(InfoViewMessages.JavadocView_action_back_disabledTooltip);
				setEnabled(false);
			}
		}

		@Override
		public void run() {
			setInput(fCurrent.getPrevious());
		}
	}

	/**
	 * Action to open the selection in an external browser. If the selection is a java element its
	 * corresponding javadoc is shown if possible. If it is an URL the URL's content is shown.
	 *
	 * The action is disabled if the selection cannot be opened.
	 *
	 * @since 3.6
	 */
	private static class OpenInBrowserAction extends OpenAttachedJavadocAction {

		/**
		 * Create a new ShowExternalJavadocAction
		 *
		 * @param site the site
		 */
		public OpenInBrowserAction(IWorkbenchSite site) {
			super(site);
		}

		@Override
		public void selectionChanged(IStructuredSelection structuredSelection) {
			super.selectionChanged(structuredSelection);
			Object element= structuredSelection.getFirstElement();
			if (element instanceof URL) {
				setText(InfoViewMessages.OpenInBrowserAction_url_label);
				setToolTipText(InfoViewMessages.OpenInBrowserAction_url_toolTip);
			} else {
				setText(ActionMessages.OpenAttachedJavadocAction_label);
				setToolTipText(ActionMessages.OpenAttachedJavadocAction_tooltip);
			}
		}

		@Override
		public void run(IStructuredSelection selection) {
			if (!canEnableFor(selection))
				return;

			Object element= selection.getFirstElement();
			if (element instanceof IJavaElement)
				super.run(selection);
			else
				open((URL)element);

		}

		@Override
		protected boolean canEnableFor(IStructuredSelection selection) {
			if (selection.size() != 1)
				return false;

			Object element= selection.getFirstElement();
			return element instanceof URL || super.canEnableFor(selection);
		}

	}


	/**
	 * Preference key for the preference whether to show a dialog
	 * when the SWT Browser widget is not available.
	 * @since 3.0
	 */
	private static final String DO_NOT_WARN_PREFERENCE_KEY= "JavadocView.error.doNotWarn"; //$NON-NLS-1$

	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=73558
	private static final boolean WARNING_DIALOG_ENABLED= false;

	/** The HTML widget. */
	private Browser fBrowser;
	/** The text widget. */
	private StyledText fText;
	/** The information presenter. */
	private HTMLTextPresenter fPresenter;
	/** The text presentation. */
	private final TextPresentation fPresentation= new TextPresentation();
	/** The select all action */
	private SelectAllAction fSelectAllAction;
	/** The style sheet (css) */
	private static String fgStyleSheet;
	/**
	 * <code>true</code> once the style sheet has been loaded.
	 * @since 3.3
	 */
	private static boolean fgStyleSheetLoaded= false;

	/** The Browser widget */
	private boolean fIsUsingBrowserWidget;

	private RGB fBackgroundColorRGB;
	private RGB fForegroundColorRGB;
	/**
	 * The font listener.
	 * @since 3.3
	 */
	private IPropertyChangeListener fFontListener;

	/**
	 * Holds original Javadoc input string.
	 * @since 3.4
	 */
	private String fOriginalInput;

	/**
	 * The current input element if any
	 * @since 3.4
	 */
	private BrowserInput fCurrent;

	/**
	 * Action to go back in the link history.
	 * @since 3.4
	 */
	private BackAction fBackAction;

	/**
	 * Action to go forth in the link history.
	 * @since 3.4
	 */
	private ForthAction fForthAction;

	/**
	 * Action to open the attached Javadoc.
	 * @since 3.4
	 */
	private OpenInBrowserAction fOpenBrowserAction;

	/**
	 * A selection provider providing the current
	 * Java element input of this view as selection.
	 * @since 3.4
	 */
	private ISelectionProvider fInputSelectionProvider;

	/**
	 * The Javadoc view's select all action.
	 */
	private class SelectAllAction extends Action {

		/** The control. */
		private final Control fControl;
		/** The selection provider. */
		private final SelectionProvider fSelectionProvider;

		/**
		 * Creates the action.
		 *
		 * @param control the widget
		 * @param selectionProvider the selection provider
		 */
		public SelectAllAction(Control control, SelectionProvider selectionProvider) {
			super("selectAll"); //$NON-NLS-1$

			Assert.isNotNull(control);
			Assert.isNotNull(selectionProvider);
			fControl= control;
			fSelectionProvider= selectionProvider;

			// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
			setEnabled(!fIsUsingBrowserWidget);

			setText(InfoViewMessages.SelectAllAction_label);
			setToolTipText(InfoViewMessages.SelectAllAction_tooltip);
			setDescription(InfoViewMessages.SelectAllAction_description);

			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAbstractTextEditorHelpContextIds.SELECT_ALL_ACTION);
		}

		/**
		 * Selects all in the view.
		 */
		@Override
		public void run() {
			if (fControl instanceof StyledText)
				((StyledText)fControl).selectAll();
			else {
				// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
//				((Browser)fControl).selectAll();
				if (fSelectionProvider != null)
					fSelectionProvider.fireSelectionChanged();
			}
		}
	}

	/**
	 * The Javadoc view's selection provider.
	 */
	private static class SelectionProvider implements ISelectionProvider {

		/** The selection changed listeners. */
		private final ListenerList<ISelectionChangedListener> fListeners= new ListenerList<>(ListenerList.IDENTITY);
		/** The widget. */
		private final Control fControl;

		/**
		 * Creates a new selection provider.
		 *
		 * @param control	the widget
		 */
		public SelectionProvider(Control control) {
			Assert.isNotNull(control);
			fControl= control;
			if (fControl instanceof StyledText styledText) {
				styledText.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						fireSelectionChanged();
					}
				});
			} else {
				// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
//				((Browser)fControl).addSelectionListener(new SelectionAdapter() {
//					public void widgetSelected(SelectionEvent e) {
//						fireSelectionChanged();
//					}
//				});
			}
		}

		/**
		 * Sends a selection changed event to all listeners.
		 */
		public void fireSelectionChanged() {
			ISelection selection= getSelection();
			SelectionChangedEvent event= new SelectionChangedEvent(this, selection);
			for (ISelectionChangedListener selectionChangedListener : fListeners) {
				selectionChangedListener.selectionChanged(event);
			}
		}

		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			fListeners.add(listener);
		}

		@Override
		public ISelection getSelection() {
			if (fControl instanceof StyledText styledText) {
				IDocument document= new Document(styledText.getSelectionText());
				return new TextSelection(document, 0, document.getLength());
			} else {
				// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
				return StructuredSelection.EMPTY;
			}
		}

		@Override
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			fListeners.remove(listener);
		}

		@Override
		public void setSelection(ISelection selection) {
			// not supported
		}
	}

	@Override
	protected void internalCreatePartControl(Composite parent) {
		try {
			fBrowser= new Browser(parent, SWT.NONE);
			fBrowser.setJavascriptEnabled(false);
			fIsUsingBrowserWidget= true;
			addLinkListener(fBrowser);
			fBrowser.addOpenWindowListener(event -> event.required= true);

		} catch (SWTError er) {

			/* The Browser widget throws an SWTError if it fails to
			 * instantiate properly. Application code should catch
			 * this SWTError and disable any feature requiring the
			 * Browser widget.
			 * Platform requirements for the SWT Browser widget are available
			 * from the SWT FAQ web site.
			 */

			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			boolean doNotWarn= store.getBoolean(DO_NOT_WARN_PREFERENCE_KEY);
			if (WARNING_DIALOG_ENABLED) {
				if (!doNotWarn) {
					String title= InfoViewMessages.JavadocView_error_noBrowser_title;
					String message= InfoViewMessages.JavadocView_error_noBrowser_message;
					String toggleMessage= InfoViewMessages.JavadocView_error_noBrowser_doNotWarn;
					MessageDialogWithToggle dialog= MessageDialogWithToggle.openError(parent.getShell(), title, message, toggleMessage, false, null, null);
					if (dialog.getReturnCode() == Window.OK)
						store.setValue(DO_NOT_WARN_PREFERENCE_KEY, dialog.getToggleState());
				}
			}

			fIsUsingBrowserWidget= false;
		}

		if (!fIsUsingBrowserWidget) {
			fText= new StyledText(parent, SWT.V_SCROLL | SWT.H_SCROLL);
			fText.setEditable(false);
			fPresenter= new FallbackInformationPresenter();

			fText.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					doSetInput(fOriginalInput);
				}
			});
		}

		initStyleSheet();
		listenForFontChanges();
		getViewSite().setSelectionProvider(new SelectionProvider(getControl()));
	}

	/**
	 * Registers a listener for the Java editor font.
	 *
	 * @since 3.3
	 */
	private void listenForFontChanges() {
		fFontListener= event -> {
			if (PreferenceConstants.APPEARANCE_JAVADOC_FONT.equals(event.getProperty())) {
				fgStyleSheetLoaded= false;
				// trigger reloading, but make sure other listeners have already run, so that
				// the style sheet gets reloaded only once.
				final Display display= getSite().getPage().getWorkbenchWindow().getWorkbench().getDisplay();
				if (!display.isDisposed()) {
					display.asyncExec(() -> {
						if (!display.isDisposed()) {
							initStyleSheet();
							refresh();
						}
					});
				}
			}
		};
		JFaceResources.getFontRegistry().addListener(fFontListener);
	}

	private static void initStyleSheet() {
		if (fgStyleSheetLoaded)
			return;
		fgStyleSheetLoaded= true;
		fgStyleSheet= JavadocHover.loadStyleSheet("/JavadocViewStyleSheet.css"); //$NON-NLS-1$
	}

	@Override
	protected void createActions() {
		super.createActions();
		fSelectAllAction= new SelectAllAction(getControl(), (SelectionProvider) getSelectionProvider());

		fBackAction= new BackAction();
		fBackAction.setActionDefinitionId(IWorkbenchCommandConstants.NAVIGATE_BACK);
		fForthAction= new ForthAction();
		fForthAction.setActionDefinitionId(IWorkbenchCommandConstants.NAVIGATE_FORWARD);

		fInputSelectionProvider= new SimpleSelectionProvider();
		fOpenBrowserAction= new OpenInBrowserAction(getSite());
		fOpenBrowserAction.setSpecialSelectionProvider(fInputSelectionProvider);
		fOpenBrowserAction.setImageDescriptor(JavaPluginImages.DESC_ELCL_OPEN_BROWSER);
		fOpenBrowserAction.setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_OPEN_BROWSER);
		fOpenBrowserAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_ATTACHED_JAVADOC);
		fInputSelectionProvider.addSelectionChangedListener(fOpenBrowserAction);

		IJavaElement input= getOrignalInput();
		StructuredSelection selection;
		if (input != null) {
			selection= new StructuredSelection(input);
		} else {
			selection= new StructuredSelection();
		}
		fInputSelectionProvider.setSelection(selection);
	}

	@Override
	protected void fillActionBars(final IActionBars actionBars) {
		super.fillActionBars(actionBars);

		actionBars.setGlobalActionHandler(ActionFactory.BACK.getId(), fBackAction);
		actionBars.setGlobalActionHandler(ActionFactory.FORWARD.getId(), fForthAction);

		fInputSelectionProvider.addSelectionChangedListener(event -> actionBars.setGlobalActionHandler(JdtActionConstants.OPEN_ATTACHED_JAVA_DOC, fOpenBrowserAction));

	}

	@Override
	protected void fillToolBar(IToolBarManager tbm) {
		tbm.add(fBackAction);
		tbm.add(fForthAction);
		tbm.add(new Separator());

		super.fillToolBar(tbm);
		tbm.add(fOpenBrowserAction);
	}

	@Override
	public void menuAboutToShow(IMenuManager menu) {
		super.menuAboutToShow(menu);

		menu.appendToGroup(IContextMenuConstants.GROUP_GOTO, fBackAction);
		menu.appendToGroup(IContextMenuConstants.GROUP_GOTO, fForthAction);

		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, fOpenBrowserAction);
	}

	@Override
	protected IAction getSelectAllAction() {
		// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
		if (fIsUsingBrowserWidget)
			return null;

		return fSelectAllAction;
	}

	@Override
	protected IAction getCopyToClipboardAction() {
		// FIXME: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=63022
		if (fIsUsingBrowserWidget)
			return null;

		return super.getCopyToClipboardAction();
	}

	@Override
	protected void setForeground(Color color) {
		getControl().setForeground(color);
		fForegroundColorRGB= color.getRGB();
		refresh();
	}

	@Override
	protected void setBackground(Color color) {
		getControl().setBackground(color);
		fBackgroundColorRGB= color.getRGB();
		refresh();
	}

	/**
	 * Refreshes the view.
	 *
	 * @since 3.3
	 */
	private void refresh() {
		doSetInput(computeInput(getOrignalInput()));
	}

	@Override
	protected String getBackgroundColorKey() {
		return "org.eclipse.jdt.ui.Javadoc.backgroundColor";		 //$NON-NLS-1$
	}

	@Override
	protected String getForegroundColorKey() {
		return "org.eclipse.jdt.ui.Javadoc.foregroundColor";		 //$NON-NLS-1$
	}

	@Override
	protected void internalDispose() {
		fText= null;
		fBrowser= null;
		if (fFontListener != null) {
			JFaceResources.getFontRegistry().removeListener(fFontListener);
			fFontListener= null;
		}

		if (fOpenBrowserAction != null) {
			fInputSelectionProvider.removeSelectionChangedListener(fOpenBrowserAction);
			fOpenBrowserAction= null;
		}
	}

	@Override
	public void setFocus() {
		getControl().setFocus();
	}

	@Override
	protected Object computeInput(Object input) {
		if (getControl() == null || ! (input instanceof IJavaElement))
			return null;

		IWorkbenchPart part= null;
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page= window.getActivePage();
			if (page != null) {
				part= page.getActivePart();
			}
		}

		ISelection selection= null;
		if (part != null) {
			IWorkbenchPartSite site= part.getSite();
			if (site != null) {
				ISelectionProvider provider= site.getSelectionProvider();
				if (provider != null) {
					selection= provider.getSelection();
				}
			}
		}

		return computeInput(part, selection, (IJavaElement) input, new NullProgressMonitor());
	}

	@Override
	protected Object computeInput(IWorkbenchPart part, ISelection selection, IJavaElement input, IProgressMonitor monitor) {
		if (getControl() == null || input == null)
			return null;

		String javadocHtml;

		switch (input.getElementType()) {
			case IJavaElement.COMPILATION_UNIT:
				try {
					IType[] types= ((ICompilationUnit) input).getTypes();
					if (types.length == 0 && JavaModelUtil.isPackageInfo((ICompilationUnit) input)) {
						javadocHtml= getJavadocHtml(new IJavaElement[] { input.getParent() }, part, selection, monitor);
					} else {
						javadocHtml= getJavadocHtml(types, part, selection, monitor);
					}
				} catch (JavaModelException ex) {
					javadocHtml= null;
				}
				break;
			case IJavaElement.CLASS_FILE:
				if (JavaModelUtil.PACKAGE_INFO_CLASS.equals(input.getElementName())) {
					javadocHtml= getJavadocHtml(new IJavaElement[] { input.getParent() }, part, selection, monitor);
				} else if (input instanceof IModularClassFile modularClassFile) {
					try {
						javadocHtml= getJavadocHtml(new IJavaElement[] { modularClassFile.getModule() }, part, selection, monitor);
					} catch (JavaModelException e) {
						return null;
					}
				} else {
					javadocHtml= getJavadocHtml(new IJavaElement[] { ((IOrdinaryClassFile) input).getType() }, part, selection, monitor);
				}
				break;
			default:
				javadocHtml= getJavadocHtml(new IJavaElement[] { input }, part, selection, monitor);
		}

		return javadocHtml;
	}

	@Override
	protected String computeDescription(IWorkbenchPart part, ISelection selection, IJavaElement inputElement, IProgressMonitor monitor) {
		return ""; //$NON-NLS-1$
	}

	/**
	 * Set input to the given input.
	 *
	 * @param input the input for the view
	 * @since 3.4
	 */
	public void setInput(BrowserInput input) {
		fCurrent= input;

		Object inputElement= input.getInputElement();
		if (inputElement instanceof IJavaElement javaElement) {
			setInput(javaElement);
		} else if (inputElement instanceof URL url) {
			fBrowser.setUrl(url.toExternalForm());

			if (fInputSelectionProvider != null)
				fInputSelectionProvider.setSelection(new StructuredSelection(inputElement));
		}

		fForthAction.update();
		fBackAction.update();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param input a String containing the HTML to be shown in the view, or <code>null</code>
	 */
	@Override
	protected void doSetInput(Object input) {
		String javadocHtml;
		if (input instanceof String s) {
			javadocHtml= s;
		} else {
			StringBuilder buffer= new StringBuilder();
			HTMLPrinter.insertPageProlog(buffer, 0, fForegroundColorRGB, fBackgroundColorRGB, fgStyleSheet);
			HTMLPrinter.addPageEpilog(buffer);
			javadocHtml= buffer.toString();
		}
		fOriginalInput= javadocHtml;

		if (fInputSelectionProvider != null) {
			IJavaElement inputElement= getOrignalInput();
			StructuredSelection selection= inputElement == null ? StructuredSelection.EMPTY : new StructuredSelection(inputElement);
			fInputSelectionProvider.setSelection(selection);
		}

		if (fOpenBrowserAction != null)
			fOpenBrowserAction.setEnabled(input != null);

		if (fIsUsingBrowserWidget) {
			if (javadocHtml != null && javadocHtml.length() > 0) {
				boolean RTL= (getSite().getShell().getStyle() & SWT.RIGHT_TO_LEFT) != 0;
				if (RTL) {
					StringBuilder buffer= new StringBuilder(javadocHtml);
					HTMLPrinter.insertStyles(buffer, new String[] { "direction:rtl" } ); //$NON-NLS-1$
					javadocHtml= buffer.toString();
				}
			}
			fBrowser.setText(javadocHtml);
		} else {
			fPresentation.clear();
			Rectangle size=  fText.getClientArea();

			try {
				javadocHtml= fPresenter.updatePresentation(fText, javadocHtml, fPresentation, size.width, size.height);
			} catch (IllegalArgumentException ex) {
				// the javadoc might no longer be valid
				return;
			}
			fText.setText(javadocHtml);
			TextPresentation.applyTextPresentation(fPresentation, fText);
		}
	}

	/**
	 * Returns the Javadoc of the Java element in HTML format.
	 *
	 * @param result the Java elements for which to get the Javadoc
	 * @param activePart the active part if any
	 * @param selection the selection of the active site if any
	 * @param monitor a monitor to report progress to
	 * @return a string with the Javadoc in HTML format, or <code>null</code> if none
	 */
	private String getJavadocHtml(IJavaElement[] result, IWorkbenchPart activePart, ISelection selection, IProgressMonitor monitor) {
		StringBuilder buffer= new StringBuilder();
		int nResults= result.length;

		if (nResults == 0)
			return null;

		String base= null;
		if (nResults > 1) {
			for (IJavaElement r : result) {
				HTMLPrinter.startBulletList(buffer);
				IJavaElement curr= r;
				if (curr instanceof IMember || curr instanceof IPackageFragment || curr instanceof IPackageDeclaration || curr.getElementType() == IJavaElement.LOCAL_VARIABLE) {
					HTMLPrinter.addBullet(buffer, getInfoText(curr, null, null, false));
					HTMLPrinter.endBulletList(buffer);
				}
			}
		} else {
			IJavaElement curr= result[0];
			if (curr instanceof IPackageDeclaration || curr instanceof IPackageFragment) {
				HTMLPrinter.addSmallHeader(buffer, getInfoText(curr, null, null, true));
				buffer.append("<br>"); //$NON-NLS-1$
				Reader reader= null;
				String content= null;
				try {
					if (curr instanceof IPackageDeclaration packageDecl) {
						try {
							ISourceRange nameRange= packageDecl.getNameRange();
							if (SourceRange.isAvailable(nameRange)) {
								ITypeRoot typeRoot= (ITypeRoot) packageDecl.getParent();
								Region hoverRegion= new Region(nameRange.getOffset(), nameRange.getLength());
								JavadocHover.addAnnotations(buffer, typeRoot.getParent(), typeRoot, hoverRegion);
							}
						} catch (JavaModelException e) {
							// no annotations this time...
						}

						content= JavadocContentAccess2.getHTMLContent((IPackageDeclaration) curr);
					} else if (curr instanceof IPackageFragment packageFragm) {
						JavadocHover.addAnnotations(buffer, curr, null, null);
						content= JavadocContentAccess2.getHTMLContent(packageFragm);
					}
				} catch (CoreException e) {
					reader= new StringReader(JavaDocLocations.handleFailedJavadocFetch(e));
				}
				IPackageFragmentRoot root= (IPackageFragmentRoot) curr.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				try {
					boolean isBinary= root.getKind() == IPackageFragmentRoot.K_BINARY;
					if (content != null) {
						base= JavadocContentAccess2.extractBaseURL(content);
						if (base == null) {
							base= JavaDocLocations.getBaseURL(curr, isBinary);
						}
						reader= new StringReader(content);
					} else if (reader == null) {
						String explanationForMissingJavadoc= JavaDocLocations.getExplanationForMissingJavadoc(curr, root);
						if (explanationForMissingJavadoc != null) {
							reader= new StringReader(explanationForMissingJavadoc);
						}
					}
				} catch (JavaModelException e) {
					reader= new StringReader(InfoViewMessages.JavadocView_error_gettingJavadoc);
					JavaPlugin.log(e);
				}
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, reader);
				}
			} else if (curr instanceof IMember || curr instanceof ILocalVariable || curr instanceof ITypeParameter) {
				final IJavaElement element= curr;

				ITypeRoot typeRoot= null;
				Region hoverRegion= null;
				try {
					ISourceRange nameRange= ((ISourceReference) curr).getNameRange();
					if (SourceRange.isAvailable(nameRange)) {
						if (element instanceof ILocalVariable localVar) {
							typeRoot= localVar.getTypeRoot();
						} else if (element instanceof ITypeParameter typeParam) {
							typeRoot= typeParam.getTypeRoot();
						} else {
							typeRoot= ((IMember) curr).getTypeRoot();
						}
						hoverRegion= new Region(nameRange.getOffset(), nameRange.getLength());
					}
				} catch (JavaModelException e) {
					// no annotations this time...
				}

				String constantValue= null;
				if (element instanceof IField field) {
					constantValue= computeFieldConstant(activePart, selection, field, monitor);
					if (constantValue != null)
						constantValue= HTMLPrinter.convertToHTMLContentWithWhitespace(constantValue);
				}

				String defaultValue= null;
				if (element instanceof IMethod method) {
					try {
						defaultValue= JavadocHover.getAnnotationMemberDefaultValue(method, typeRoot, hoverRegion);
						if (defaultValue != null) {
							defaultValue= HTMLPrinter.convertToHTMLContentWithWhitespace(defaultValue);
						}
					} catch (JavaModelException e) {
						// no default value
					}
				}

				HTMLPrinter.addSmallHeader(buffer, getInfoText(element, constantValue, defaultValue, true));

				if (typeRoot != null && hoverRegion != null) {
					buffer.append("<br>"); //$NON-NLS-1$
					JavadocHover.addAnnotations(buffer, curr, typeRoot, hoverRegion);
				}

				Reader reader= null;
				try {
					String content= JavadocContentAccess2.getHTMLContent(element, true);
					IPackageFragmentRoot root= (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					if (content != null) {
						IMember member;
						if (element instanceof ILocalVariable localVar) {
							member= localVar.getDeclaringMember();
						} else if (element instanceof ITypeParameter typeParam) {
							member= typeParam.getDeclaringMember();
						} else {
							member= (IMember) element;
						}
						base= JavadocContentAccess2.extractBaseURL(content);
						if (base == null) {
							base= JavaDocLocations.getBaseURL(member, member.isBinary());
						}
						reader= new StringReader(content);
					} else {
						String explanationForMissingJavadoc= JavaDocLocations.getExplanationForMissingJavadoc(element, root);
						if (explanationForMissingJavadoc != null) {
							reader= new StringReader(explanationForMissingJavadoc);
						}
					}
				} catch (CoreException ex) {
					reader= new StringReader(JavaDocLocations.handleFailedJavadocFetch(ex));
				}
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, reader);
				}

			}
		}

		if (buffer.length() == 0)
			return null;

		HTMLPrinter.insertPageProlog(buffer, 0, fForegroundColorRGB, fBackgroundColorRGB, fgStyleSheet);
		if (base != null) {
			int endHeadIdx= buffer.indexOf("</head>"); //$NON-NLS-1$
			buffer.insert(endHeadIdx, "\n<base href='" + base + "'>\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}


	/**
	 * Gets the label for the given member.
	 *
	 * @param member the Java member
	 * @param constantValue the constant value if any
	 * @param defaultValue the default value of the annotation type member, if any
	 * @param allowImage <code>true</code> if the Java element image should be shown
	 * @return a string containing the member's label
	 */
	private String getInfoText(IJavaElement member, String constantValue, String defaultValue, boolean allowImage) {
		long flags= JavadocHover.getHeaderFlags(member);
		IBinding binding= JavadocHover.getHoverBinding(member, null);
		StringBuffer label;
		if (binding != null) {
			label= new StringBuffer();
			// setting haveSource to false lets the JavadocView *always* show qualified type names,
			// would need to track the source of our input to distinguish classfile/compilationUnit:
			boolean haveSource= false;
			new BindingLinkedLabelComposer(member, label, haveSource).appendBindingLabel(binding, flags);
		} else {
			label= new StringBuffer(JavaElementLinks.getElementLabel(member, flags));
		}
		if (member.getElementType() == IJavaElement.FIELD && constantValue != null) {
			label.append(constantValue);
		} else if (member.getElementType() == IJavaElement.METHOD && defaultValue != null) {
			label.append(JavadocHover.CONSTANT_VALUE_SEPARATOR);
			label.append(defaultValue);
		}
		return JavadocHover.getImageAndLabel(member, allowImage, label.toString());
	}

	@Override
	protected boolean isIgnoringNewInput(IJavaElement je, IWorkbenchPart part, ISelection selection) {
		if (fCurrent != null && fCurrent.getInputElement() instanceof URL)
			return false;

		if (super.isIgnoringNewInput(je, part, selection)
				&& part instanceof ITextEditor editor
				&& selection instanceof ITextSelection textSel) {

			IDocumentProvider docProvider= editor.getDocumentProvider();
			if (docProvider == null)
				return false;

			IDocument document= docProvider.getDocument(editor.getEditorInput());
			if (!(document instanceof IDocumentExtension3))
				return false;

			try {
				int offset= textSel.getOffset();
				String partition= ((IDocumentExtension3)document).getContentType(IJavaPartitions.JAVA_PARTITIONING, offset, false);
				return !IJavaPartitions.JAVA_DOC.equals(partition);
			} catch (BadPartitioningException | BadLocationException ex) {
				return false;
			}

		}
		return false;
	}

	@Override
	protected IJavaElement findSelectedJavaElement(IWorkbenchPart part, ISelection selection) {
		IJavaElement element= super.findSelectedJavaElement(part, selection);
		try {
			//update the Javadoc view when package.html is selected in project explorer view
			if (element == null && selection instanceof IStructuredSelection sSel) {
				Object selectedElement= sSel.getFirstElement();
				if (selectedElement instanceof IFile selectedFile) {
					if (JavaModelUtil.PACKAGE_HTML.equals(selectedFile.getName())) {
						element= JavaCore.create(selectedFile.getParent());
					}
				} else if (selectedElement instanceof IJarEntryResource jarEntryResource) {
					if (JavaModelUtil.PACKAGE_HTML.equals(jarEntryResource.getName())) {
						Object parent= jarEntryResource.getParent();
						if (parent instanceof IJavaElement javaEl) {
							element= javaEl;
						}
					}

				}
			}

			if (element == null && selection instanceof ITextSelection textSelection) {
				if (part instanceof AbstractDecoratedTextEditor editor) {
					IDocumentProvider documentProvider= editor.getDocumentProvider();
					if (documentProvider != null) {
						IEditorInput editorInput= editor.getEditorInput();
						IDocument document= documentProvider.getDocument(editorInput);

						if (document != null) {
							ITypedRegion typedRegion= TextUtilities.getPartition(document, IJavaPartitions.JAVA_PARTITIONING, textSelection.getOffset(), false);
							if (IJavaPartitions.JAVA_DOC.equals(typedRegion.getType())){
								element= TextSelectionConverter.getElementAtOffset((JavaEditor) part, textSelection);
							}
							else if (editorInput instanceof IFileEditorInput fei) {
								IFile file= fei.getFile();
								//update the Javadoc view when the content of the package.html is modified in the editor
								if (JavaModelUtil.PACKAGE_HTML.equals(file.getName())) {
									element= JavaCore.create(file.getParent());
								}
							}
						}
					}
				}
			}

		} catch (JavaModelException | BadLocationException e) {
			return null;
		}
		return element;
	}

	@Override
	protected Control getControl() {
		if (fIsUsingBrowserWidget)
			return fBrowser;
		else
			return fText;
	}

	@Override
	protected String getHelpContextId() {
		return IJavaHelpContextIds.JAVADOC_VIEW;
	}

	/**
	 * Compute the textual representation of a 'static' 'final' field's constant initializer value.
	 *
	 * @param activePart the part that triggered the computation, or <code>null</code>
	 * @param selection the selection that references the field, or <code>null</code>
	 * @param resolvedField the field whose constant value will be computed
	 * @param monitor the progress monitor
	 *
	 * @return the textual representation of the constant, or <code>null</code> if the
	 *   field is not a constant field, the initializer value could not be computed, or
	 *   the progress monitor was cancelled
	 * @since 3.4
	 */
	private String computeFieldConstant(IWorkbenchPart activePart, ISelection selection, IField resolvedField, IProgressMonitor monitor) {

		if (!JavadocHover.isStaticFinal(resolvedField))
			return null;

		Object constantValue;

		if (selection instanceof ITextSelection textSel && activePart instanceof JavaEditor editor) {
			ITypeRoot activeType= JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
			constantValue= getConstantValueFromActiveEditor(activeType, resolvedField, textSel, monitor);
			if (constantValue == null) // fall back - e.g. when selection is inside Javadoc of the element
				constantValue= computeFieldConstantFromTypeAST(resolvedField, monitor);
		} else {
			constantValue= computeFieldConstantFromTypeAST(resolvedField, monitor);
		}

		if (constantValue != null)
			return JavadocHover.CONSTANT_VALUE_SEPARATOR + formatCompilerConstantValue(constantValue);

		return null;
	}

	/**
	 * Retrieve a constant initializer value of a field by (AST) parsing field's type.
	 *
	 * @param constantField the constant field
	 * @param monitor the progress monitor or null
	 * @return the constant value of the field, or <code>null</code> if it could not be computed
	 *   (or if the progress was cancelled).
	 * @since 3.4
	 */
	public static Object computeFieldConstantFromTypeAST(IField constantField, IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled())
			return null;

		CompilationUnit ast= SharedASTProviderCore.getAST(constantField.getTypeRoot(), SharedASTProviderCore.WAIT_NO, monitor);
		if (ast != null) {
			try {
				if (constantField.isEnumConstant())
					return null;

				VariableDeclarationFragment fieldDecl= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(constantField, ast);
				if (fieldDecl == null)
					return null;
				Expression initializer= fieldDecl.getInitializer();
				if (initializer == null)
					return null;
				return initializer.resolveConstantExpressionValue();
			} catch (JavaModelException e) {
				// ignore the exception and try the next method
			}
		}

		if (monitor != null && monitor.isCanceled())
			return null;

		ASTParser p= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		p.setProject(constantField.getJavaProject());
		IBinding[] createBindings;
		try {
			createBindings= p.createBindings(new IJavaElement[] { constantField }, monitor);
		} catch (OperationCanceledException e) {
			return null;
		}

		if (createBindings[0] instanceof IVariableBinding variableBinding)
			return variableBinding.getConstantValue();

		return null;
	}

	/**
	 * Returns the constant value for a field that is referenced by the currently active type. This
	 * method does may not run in the main UI thread.
	 *
	 * @param activeType the type that is currently active
	 * @param field the field that is being referenced (usually not declared in
	 *            <code>activeType</code>)
	 * @param selection the region in <code>activeType</code> that contains the field reference
	 * @param monitor a progress monitor
	 *
	 * @return the constant value for the given field or <code>null</code> if none
	 * @since 3.4
	 */
	private static Object getConstantValueFromActiveEditor(ITypeRoot activeType, IField field, ITextSelection selection, IProgressMonitor monitor) {
		CompilationUnit unit= SharedASTProviderCore.getAST(activeType, SharedASTProviderCore.WAIT_ACTIVE_ONLY, monitor);
		if (unit == null)
			return null;

		ASTNode node= NodeFinder.perform(unit, selection.getOffset(), selection.getLength());
		return JavadocHover.getVariableBindingConstValue(node, field);
	}

	/**
	 * Returns the string representation of the given constant value.
	 *
	 * @param constantValue the constant value
	 * @return the string representation of the given constant value.
	 * @since 3.4
	 */
	private static String formatCompilerConstantValue(Object constantValue) {
		if (constantValue instanceof String stringConstant) {
			StringBuilder result= new StringBuilder();
			result.append('"');
			if (stringConstant.length() > 80) {
				result.append(stringConstant.substring(0, 80));
				result.append(JavaElementLabels.ELLIPSIS_STRING);
			} else {
				result.append(stringConstant);
			}
			result.append('"');
			return result.toString();

		} else {
			return JavadocHover.getHexConstantValue(constantValue);
		}
	}


	/**
	 * see also org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover.addLinkListener(BrowserInformationControl)
	 *
	 * Add link listener to the given browser
	 * @param browser the browser to add a listener to
	 * @since 3.4
	 */
	private void addLinkListener(Browser browser) {
		browser.addLocationListener(JavaElementLinks.createLocationListener(new JavaElementLinks.ILinkHandler() {

			@Override
			public void handleDeclarationLink(IJavaElement target) {
				try {
					JavadocHover.openDeclaration(target);
				} catch (PartInitException | JavaModelException e) {
					JavaPlugin.log(e);
				}
			}

			@Override
			public boolean handleExternalLink(final URL url, Display display) {
				if (fCurrent == null ||
						!(fCurrent.getInputElement() instanceof URL)
						|| !url.toExternalForm().equals(((URL) fCurrent.getInputElement()).toExternalForm())) {
					fCurrent= new URLBrowserInput(fCurrent, url);

					if (fBackAction != null) {
						fBackAction.update();
						fForthAction.update();
					}

					if (fInputSelectionProvider != null)
						fInputSelectionProvider.setSelection(new StructuredSelection(url));
				}

				return false;
			}

			@Override
			public void handleInlineJavadocLink(IJavaElement target) {
				JavaElementBrowserInput newInput= new JavaElementBrowserInput(fCurrent, target);
				JavadocView.this.setInput(newInput);
			}

			@Override
			public void handleJavadocViewLink(IJavaElement target) {
				handleInlineJavadocLink(target);
			}

			@Override
			public void handleTextSet() {
				IJavaElement input= getOrignalInput();
				if (input == null)
					return;

				if (fCurrent == null || !fCurrent.getInputElement().equals(input)) {
					fCurrent= new JavaElementBrowserInput(null, input);

					if (fBackAction != null) {
						fBackAction.update();
						fForthAction.update();
					}
				}
			}
		}));
	}

}
