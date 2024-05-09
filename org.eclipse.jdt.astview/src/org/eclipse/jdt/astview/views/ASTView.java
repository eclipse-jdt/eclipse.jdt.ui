/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.astview.views;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.astview.ASTViewImages;
import org.eclipse.jdt.astview.ASTViewPlugin;
import org.eclipse.jdt.astview.EditorUtility;
import org.eclipse.jdt.astview.TreeInfoCollector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.util.ASTHelper;


public class ASTView extends ViewPart implements IShowInSource, IShowInTargetList {

	static final int JLS_LATEST= AST.getJLSLatest();
	private static final int JLS23= ASTHelper.JLS23;
	private static final int JLS22= ASTHelper.JLS22;
	private static final int JLS21= ASTHelper.JLS21;
	private static final int JLS20= ASTHelper.JLS20;
	private static final int JLS19= ASTHelper.JLS19;
	private static final int JLS18= ASTHelper.JLS18;
	private static final int JLS17= ASTHelper.JLS17;
	private static final int JLS16= ASTHelper.JLS16;
	private static final int JLS15= ASTHelper.JLS15;
	private static final int JLS14= ASTHelper.JLS14;
	private static final int JLS13= ASTHelper.JLS13;
	private static final int JLS12= ASTHelper.JLS12;
	private static final int JLS11= ASTHelper.JLS11;
	private static final int JLS10= ASTHelper.JLS10;
	private static final int JLS9= ASTHelper.JLS9;
	private static final int JLS8= ASTHelper.JLS8;
	private static final int JLS4= ASTHelper.JLS4;
	private static final int JLS3= ASTHelper.JLS3;
	private static final int JLS2= ASTHelper.JLS2;

	private class ASTViewSelectionProvider implements ISelectionProvider {
		ListenerList<ISelectionChangedListener> fListeners= new ListenerList<>(ListenerList.IDENTITY);

		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			fListeners.add(listener);
		}

		@Override
		public ISelection getSelection() {
			IStructuredSelection selection= (IStructuredSelection) fViewer.getSelection();
			ArrayList<Object> externalSelection= new ArrayList<>();
			for (Iterator<?> iter= selection.iterator(); iter.hasNext();) {
				Object unwrapped= ASTView.unwrapAttribute(iter.next());
				if (unwrapped != null)
					externalSelection.add(unwrapped);
			}
			return new StructuredSelection(externalSelection);
		}

		@Override
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			fListeners.remove(listener);
		}

		@Override
		public void setSelection(ISelection selection) {
			//not supported
		}
	}

	private class ASTLevelToggle extends Action {
		private int fLevel;

		public ASTLevelToggle(String label, int level) {
			super(label, AS_RADIO_BUTTON);
			fLevel= level;
			if (level == getCurrentASTLevel()) {
				setChecked(true);
			}
		}

		public int getLevel() {
			return fLevel;
		}

		@Override
		public void run() {
			setASTLevel(fLevel, true);
		}
	}

	private class ASTInputKindAction extends Action {
		public static final int USE_PARSER= 1;
		public static final int USE_RECONCILE= 2;
		public static final int USE_CACHE= 3;
		public static final int USE_FOCAL= 4;

		private int fInputKind;

		public ASTInputKindAction(String label, int inputKind) {
			super(label, AS_RADIO_BUTTON);
			fInputKind= inputKind;
			if (inputKind == getCurrentInputKind()) {
				setChecked(true);
			}
		}

		public int getInputKind() {
			return fInputKind;
		}

		@Override
		public void run() {
			setASTInputType(fInputKind);
		}
	}


	private static class ListenerMix implements ISelectionListener, IFileBufferListener, IDocumentListener, ISelectionChangedListener, IDoubleClickListener, IPartListener2 {

		private boolean fASTViewVisible= true;
		private ASTView fView;

		public ListenerMix(ASTView view) {
			fView= view;
		}

		public void dispose() {
			fView= null;
		}

		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (fASTViewVisible) {
				fView.handleEditorPostSelectionChanged(part, selection);
			}
		}

		@Override
		public void bufferCreated(IFileBuffer buffer) {
			// not interesting
		}

		@Override
		public void bufferDisposed(IFileBuffer buffer) {
			if (buffer instanceof ITextFileBuffer) {
				fView.handleDocumentDisposed();
			}
		}

		@Override
		public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {
			// not interesting
		}

		@Override
		public void bufferContentReplaced(IFileBuffer buffer) {
			// not interesting
		}

		@Override
		public void stateChanging(IFileBuffer buffer) {
			// not interesting
		}

		@Override
		public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty) {
			// not interesting
		}

		@Override
		public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {
			// not interesting
		}

		@Override
		public void underlyingFileMoved(IFileBuffer buffer, IPath path) {
			// not interesting
		}

		@Override
		public void underlyingFileDeleted(IFileBuffer buffer) {
			// not interesting
		}

		@Override
		public void stateChangeFailed(IFileBuffer buffer) {
			// not interesting
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			// not interesting
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			fView.handleDocumentChanged();
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			fView.handleSelectionChanged(event.getSelection());
		}

		@Override
		public void doubleClick(DoubleClickEvent event) {
			fView.handleDoubleClick();
		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
			IWorkbenchPart part= partRef.getPart(false);
			if (part == fView) {
				fASTViewVisible= false;
			}
		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			IWorkbenchPart part= partRef.getPart(false);
			if (part == fView) {
				fASTViewVisible= true;
			}
		}

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			// not interesting
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
			// not interesting
		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			fView.notifyWorkbenchPartClosed(partRef);
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
			// not interesting
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			// not interesting
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
			// not interesting
		}
	}

	private static final class StatementChecker extends ASTVisitor {

		@Override
		public boolean visit(YieldStatement node) {
			try {
				if (node != null && node.isImplicit() && isInSwitchExpression(node)) {
					ASTNode parent= node.getParent();
					List<Statement> statements= null;
					if (parent instanceof Block) {
						statements= ((Block) parent).statements();
					} else if (parent instanceof SwitchExpression) {
						statements= ((SwitchExpression) parent).statements();
					}
					if (statements == null) {
						return true;
					}
					Expression exp= node.getExpression();
					if (exp == null) {
						return true;
					} else {
						int index= statements.indexOf(node);
						statements.remove(node);
						node.setExpression(null);
						ExpressionStatement exprStmt= node.getAST().newExpressionStatement(exp);
						exprStmt.setSourceRange(node.getStartPosition(), node.getLength());
						statements.add(index, exprStmt);
						exprStmt.accept(this);
						return false;
					}
				}
			} catch (UnsupportedOperationException e) {
				// do nothing
			}
			return true;
		}

		private boolean isInSwitchExpression(YieldStatement node) {
			boolean result= false;
			ASTNode parent= node;
			while (parent != null) {
				if (parent instanceof SwitchExpression) {
					result= true;
					break;
				}
				parent= parent.getParent();
			}
			return result;
		}
	}

	private final static String SETTINGS_LINK_WITH_EDITOR= "link_with_editor"; //$NON-NLS-1$
	private final static String SETTINGS_INPUT_KIND= "input_kind"; //$NON-NLS-1$
	private final static String SETTINGS_NO_BINDINGS= "create_bindings"; //$NON-NLS-1$
	private final static String SETTINGS_NO_STATEMENTS_RECOVERY= "no_statements_recovery"; //$NON-NLS-1$
	private final static String SETTINGS_NO_BINDINGS_RECOVERY= "no_bindings_recovery"; //$NON-NLS-1$
	private final static String SETTINGS_IGNORE_METHOD_BODIES= "ignore_method_bodies"; //$NON-NLS-1$
	private final static String SETTINGS_SHOW_NON_RELEVANT="show_non_relevant";//$NON-NLS-1$
	private final static String SETTINGS_JLS= "jls"; //$NON-NLS-1$

	private SashForm fSash;
	private TreeViewer fViewer;
	private ASTViewLabelProvider fASTLabelProvider;
	private TreeViewer fTray;

	private DrillDownAdapter fDrillDownAdapter;
	private Action fFocusAction;
	private Action fRefreshAction;
	private Action fCreateBindingsAction;
	private Action fStatementsRecoveryAction;
	private Action fBindingsRecoveryAction;
	private Action fIgnoreMethodBodiesAction;
	private Action fFilterNonRelevantAction;
	private Action fFindDeclaringNodeAction;
	private Action fParseBindingFromKeyAction;
	private Action fParseBindingFromElementAction;
	private Action fCollapseAction;
	private Action fExpandAction;
	private Action fClearAction;
	private TreeCopyAction fCopyAction;
	private Action fDoubleClickAction;
	private Action fLinkWithEditor;
	private Action fAddToTrayAction;
	private Action fDeleteAction;

	private ASTLevelToggle[] fASTVersionToggleActions;
	private int fCurrentASTLevel;

	private ASTInputKindAction[] fASTInputKindActions;
	private int fCurrentInputKind;

	private ITextEditor fEditor;
	private ITypeRoot fTypeRoot;
	private CompilationUnit fRoot;
	private IDocument fCurrentDocument;
	private ArrayList<Object> fTrayRoots;

	private boolean fDoLinkWithEditor;
	private boolean fCreateBindings;
	private NonRelevantFilter fNonRelevantFilter;
	private boolean fStatementsRecovery;
	private boolean fBindingsRecovery;
	private boolean fIgnoreMethodBodies;

	private Object fPreviousDouble;

	private ListenerMix fSuperListener;
	private ISelectionChangedListener fTrayUpdater;

	private IDialogSettings fDialogSettings;


	@SuppressWarnings("incomplete-switch")
	public ASTView() {
		fSuperListener= null;
		fDialogSettings= ASTViewPlugin.getDefault().getDialogSettings();
		fDoLinkWithEditor= fDialogSettings.getBoolean(SETTINGS_LINK_WITH_EDITOR);
		try {
			fCurrentInputKind= fDialogSettings.getInt(SETTINGS_INPUT_KIND);
		} catch (NumberFormatException e) {
			fCurrentInputKind= ASTInputKindAction.USE_PARSER;
		}
		fCreateBindings= !fDialogSettings.getBoolean(SETTINGS_NO_BINDINGS); // inverse so that default is to create bindings
		fStatementsRecovery= !fDialogSettings.getBoolean(SETTINGS_NO_STATEMENTS_RECOVERY); // inverse so that default is use recovery
		fBindingsRecovery= !fDialogSettings.getBoolean(SETTINGS_NO_BINDINGS_RECOVERY); // inverse so that default is use recovery
		fIgnoreMethodBodies= fDialogSettings.getBoolean(SETTINGS_IGNORE_METHOD_BODIES);
		fCurrentASTLevel= JLS_LATEST;
		try {
			int level= fDialogSettings.getInt(SETTINGS_JLS);
			switch (level) {
				case JLS2:
				case JLS3:
				case JLS4:
				case JLS8:
				case JLS9:
				case JLS10:
				case JLS11:
				case JLS12:
				case JLS13:
				case JLS14:
				case JLS15:
				case JLS16:
				case JLS17:
				case JLS18:
				case JLS19:
				case JLS20:
				case JLS21:
				case JLS22:
				case JLS23:
					fCurrentASTLevel= level;
			}
		} catch (NumberFormatException e) {
			// ignore
		}
		fNonRelevantFilter= new NonRelevantFilter();
		fNonRelevantFilter.setShowNonRelevant(fDialogSettings.getBoolean(SETTINGS_SHOW_NON_RELEVANT));
	}

	final void notifyWorkbenchPartClosed(IWorkbenchPartReference partRef) {
		if (fEditor != null && fEditor.equals(partRef.getPart(false))) {
			try {
				setInput(null);
			} catch (CoreException e) {
				// ignore
			}
		}
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.setSite(site);
		if (fSuperListener == null) {
			fSuperListener= new ListenerMix(this);

			ISelectionService service= site.getWorkbenchWindow().getSelectionService();
			service.addPostSelectionListener(fSuperListener);
			site.getPage().addPartListener(fSuperListener);
			FileBuffers.getTextFileBufferManager().addFileBufferListener(fSuperListener);
		}
	}

	public int getCurrentASTLevel() {
		return fCurrentASTLevel;
	}

	public int getCurrentInputKind() {
		return fCurrentInputKind;
	}

	public void setInput(ITextEditor editor) throws CoreException {
		if (fEditor != null) {
			uninstallModificationListener();
		}

		fEditor= null;
		fRoot= null;

		if (editor != null) {
			ITypeRoot typeRoot= EditorUtility.getJavaInput(editor);
			if (typeRoot == null) {
				throw new CoreException(getErrorStatus("Editor not showing a CU or class file", null)); //$NON-NLS-1$
			}
			fTypeRoot= typeRoot;

			ISelection selection= editor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection) {
				ITextSelection textSelection= (ITextSelection) selection;
				fRoot= internalSetInput(typeRoot, textSelection.getOffset(), textSelection.getLength());
				fEditor= editor;
			}
			installModificationListener();
		}

	}

	private CompilationUnit internalSetInput(ITypeRoot input, int offset, int length) throws CoreException {
		if (input.getBuffer() == null) {
			throw new CoreException(getErrorStatus("Input has no buffer", null)); //$NON-NLS-1$
		}

		CompilationUnit root;
		try {
			root= createAST(input, offset);
			resetView(root);
			if (root == null) {
				setContentDescription("AST could not be created."); //$NON-NLS-1$
				return null;
			}
		} catch (RuntimeException e) {
			throw new CoreException(getErrorStatus("Could not create AST:\n" + e.getMessage(), e)); //$NON-NLS-1$
		}

		try {
			ASTNode node= NodeFinder.perform(root, offset, length);
			if (node != null) {
				fViewer.getTree().setRedraw(false);
				try {
					fASTLabelProvider.setSelectedRange(node.getStartPosition(), node.getLength());
					fViewer.setSelection(new StructuredSelection(node), true);
				} finally {
					fViewer.getTree().setRedraw(true);
				}
			}
		} catch (RuntimeException e) {
			showAndLogError("Could not select node for editor selection", e); //$NON-NLS-1$
		}

		return root;
	}

	private void clearView() {
		resetView(null);
		setContentDescription("Open a Java editor and press the 'Show AST of active editor' toolbar button"); //$NON-NLS-1$
	}


	private void resetView(CompilationUnit root) {
		fViewer.setInput(root);
		fViewer.getTree().setEnabled(root != null);
		fSash.setMaximizedControl(fViewer.getTree());
		fTrayRoots= new ArrayList<>();
		if (fTray != null)
			fTray.setInput(fTrayRoots);
		setASTUptoDate(root != null);
		fClearAction.setEnabled(root != null);
		fFindDeclaringNodeAction.setEnabled(root != null);
		fPreviousDouble= null; // avoid leaking AST
	}

	private CompilationUnit createAST(ITypeRoot input, int offset) throws JavaModelException, CoreException {
		long startTime;
		long endTime;
		CompilationUnit root;

		if ((getCurrentInputKind() == ASTInputKindAction.USE_RECONCILE)) {
			final IProblemRequestor problemRequestor= new IProblemRequestor() { //strange: don't get bindings when supplying null as problemRequestor
				@Override
				public void acceptProblem(IProblem problem) {/*not interested*/}
				@Override
				public void beginReporting() {/*not interested*/}
				@Override
				public void endReporting() {/*not interested*/}
				@Override
				public boolean isActive() {
					return true;
				}
			};
			WorkingCopyOwner workingCopyOwner= new WorkingCopyOwner() {
				@Override
				public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
					return problemRequestor;
				}
			};
			ICompilationUnit wc= input.getWorkingCopy(workingCopyOwner, null);
			try {
				int reconcileFlags= ICompilationUnit.FORCE_PROBLEM_DETECTION;
				if (fStatementsRecovery)
					reconcileFlags |= ICompilationUnit.ENABLE_STATEMENTS_RECOVERY;
				if (fBindingsRecovery)
					reconcileFlags |= ICompilationUnit.ENABLE_BINDINGS_RECOVERY;
				if (fIgnoreMethodBodies)
					reconcileFlags |= ICompilationUnit.IGNORE_METHOD_BODIES;
				startTime= System.currentTimeMillis();
				root= wc.reconcile(fCurrentASTLevel, reconcileFlags, null, null);
				endTime= System.currentTimeMillis();
			} finally {
				wc.discardWorkingCopy();
			}

		} else if (input instanceof ICompilationUnit && (getCurrentInputKind() == ASTInputKindAction.USE_CACHE)) {
			ICompilationUnit cu= (ICompilationUnit) input;
			startTime= System.currentTimeMillis();
			root= SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_NO, null);
			endTime= System.currentTimeMillis();

		} else {
			ASTParser parser= ASTParser.newParser(fCurrentASTLevel);
			parser.setResolveBindings(fCreateBindings);
			parser.setSource(input);
			parser.setStatementsRecovery(fStatementsRecovery);
			parser.setBindingsRecovery(fBindingsRecovery);
			parser.setIgnoreMethodBodies(fIgnoreMethodBodies);
			if (getCurrentInputKind() == ASTInputKindAction.USE_FOCAL) {
				parser.setFocalPosition(offset);
			}
			startTime= System.currentTimeMillis();
			root= (CompilationUnit) parser.createAST(null);
			endTime= System.currentTimeMillis();
		}
		if (root != null) {
			root.accept(new StatementChecker());
			updateContentDescription(input, root, endTime - startTime);
		}
		return root;
	}

	protected void refreshASTSettingsActions() {
		boolean enabled;
		switch (getCurrentInputKind()) {
			case ASTInputKindAction.USE_CACHE:
				enabled= false;
				break;
			default:
				enabled= true;
				break;
		}
		fCreateBindingsAction.setEnabled(enabled && getCurrentInputKind() != ASTInputKindAction.USE_RECONCILE);
		fStatementsRecoveryAction.setEnabled(enabled);
		fBindingsRecoveryAction.setEnabled(enabled);
		fIgnoreMethodBodiesAction.setEnabled(enabled);
		for (ASTView.ASTLevelToggle action : fASTVersionToggleActions) {
			action.setEnabled(enabled);
		}
	}

	private void updateContentDescription(IJavaElement element, CompilationUnit root, long time) {
		StringBuilder version= new StringBuilder("AST Level ").append(root.getAST().apiLevel());
		switch (getCurrentInputKind()) {
		case ASTInputKindAction.USE_RECONCILE:
			version.append(", from reconciler"); //$NON-NLS-1$
			break;
		case ASTInputKindAction.USE_CACHE:
			version.append(", from ASTProvider"); //$NON-NLS-1$
			break;
		case ASTInputKindAction.USE_FOCAL:
			version.append(", using focal position"); //$NON-NLS-1$
			break;
		default:
			break;
		}
		TreeInfoCollector collector= new TreeInfoCollector(root);

		String msg= "{0} ({1}).  Creation time: {2,number} ms.  Size: {3,number} nodes, {4,number} bytes (AST nodes only)."; //$NON-NLS-1$
		Object[] args= { element.getElementName(), version.toString(), Long.valueOf(time),  Integer.valueOf(collector.getNumberOfNodes()), Integer.valueOf(collector.getSize())};
		setContentDescription(MessageFormat.format(msg, args));

	}

	@Override
	public void dispose() {
		if (fSuperListener != null) {
			if (fEditor != null) {
				uninstallModificationListener();
			}
			ISelectionService service= getSite().getWorkbenchWindow().getSelectionService();
			service.removePostSelectionListener(fSuperListener);
			getSite().getPage().removePartListener(fSuperListener);
			FileBuffers.getTextFileBufferManager().removeFileBufferListener(fSuperListener);
			fSuperListener.dispose(); // removes reference to view
			fSuperListener= null;
		}
		if (fTrayUpdater != null) {
			fViewer.removePostSelectionChangedListener(fTrayUpdater);
			fTray.removePostSelectionChangedListener(fTrayUpdater);
			fTrayUpdater= null;
		}
		super.dispose();
	}

	private IStatus getErrorStatus(String message, Throwable th) {
		return new Status(IStatus.ERROR, ASTViewPlugin.getPluginId(), IStatus.ERROR, message, th);
	}

	@Override
	public void createPartControl(Composite parent) {
		fSash= new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
		fViewer = new TreeViewer(fSash, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		fDrillDownAdapter = new DrillDownAdapter(fViewer);
		fViewer.setContentProvider(new ASTViewContentProvider());
		fASTLabelProvider= new ASTViewLabelProvider();
		fViewer.setLabelProvider(fASTLabelProvider);
		fViewer.addSelectionChangedListener(fSuperListener);
		fViewer.addDoubleClickListener(fSuperListener);
		fViewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (!fCreateBindings && element instanceof Binding)
					return false;
				return true;
			}
		});
		fViewer.addFilter(fNonRelevantFilter);


		ViewForm trayForm= new ViewForm(fSash, SWT.NONE);
		Label label= new Label(trayForm, SWT.NONE);
		label.setText(" Comparison Tray (* = selection in the upper tree):"); //$NON-NLS-1$
		trayForm.setTopLeft(label);

		fTray= new TreeViewer(trayForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		trayForm.setContent(fTray.getTree());

		fTrayRoots= new ArrayList<>();
		fTray.setContentProvider(new TrayContentProvider());
		final TrayLabelProvider trayLabelProvider= new TrayLabelProvider();
		fTray.setLabelProvider(trayLabelProvider);
		fTray.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		fTrayUpdater= event -> {
			IStructuredSelection viewerSelection= (IStructuredSelection) fViewer.getSelection();
			if (viewerSelection.size() == 1) {
				Object first= viewerSelection.getFirstElement();
				if (unwrapAttribute(first) != null) {
					trayLabelProvider.setViewerElement(first);
					return;
				}
			}
			trayLabelProvider.setViewerElement(null);
		};
		fTray.addPostSelectionChangedListener(fTrayUpdater);
		fViewer.addPostSelectionChangedListener(fTrayUpdater);
		fTray.addDoubleClickListener(event -> performTrayDoubleClick());
		fTray.addSelectionChangedListener(event -> {
			IStructuredSelection selection= (IStructuredSelection) event.getSelection();
			fDeleteAction.setEnabled(selection.size() >= 1 && fTray.getTree().isFocusControl());
		});
		fTray.getTree().addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				IStructuredSelection selection= (IStructuredSelection) fTray.getSelection();
				fDeleteAction.setEnabled(selection.size() >= 1);
			}
			@Override
			public void focusLost(FocusEvent e) {
				fDeleteAction.setEnabled(false);
			}
		});

		makeActions();
		hookContextMenu();
		hookTrayContextMenu();
		contributeToActionBars();
		getSite().setSelectionProvider(new ASTViewSelectionProvider());

		try {
			IEditorPart part= EditorUtility.getActiveEditor();
			if (part instanceof ITextEditor) {
				setInput((ITextEditor) part);
			}
		} catch (CoreException e) {
			// ignore
		}
		if (fTypeRoot == null) {
			clearView();
		} else {
			setASTUptoDate(fTypeRoot != null);
		}
	}


	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this::fillContextMenu);
		Menu menu = menuMgr.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, fViewer);
	}

	private void hookTrayContextMenu() {
		MenuManager menuMgr = new MenuManager("#TrayPopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> {
			manager.add(fCopyAction);
			manager.add(fDeleteAction);
			manager.add(new Separator());
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		});
		Menu menu = menuMgr.createContextMenu(fTray.getControl());
		fTray.getControl().setMenu(menu);
		getSite().registerContextMenu("#TrayPopupMenu", menuMgr, fTray); //$NON-NLS-1$
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
		bars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), fFocusAction);
		bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), fDeleteAction);

		IHandlerService handlerService= getViewSite().getService(IHandlerService.class);
		handlerService.activateHandler(IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR, new ActionHandler(fLinkWithEditor));
	}

	private void fillLocalPullDown(IMenuManager manager) {
		for (ASTView.ASTLevelToggle action : fASTVersionToggleActions) {
			manager.add(action);
		}
		manager.add(new Separator());
		manager.add(fCreateBindingsAction);
		manager.add(fStatementsRecoveryAction);
		manager.add(fBindingsRecoveryAction);
		manager.add(fIgnoreMethodBodiesAction);
		manager.add(new Separator());
		for (ASTView.ASTInputKindAction action : fASTInputKindActions) {
			manager.add(action);
		}
		manager.add(new Separator());
		manager.add(fFindDeclaringNodeAction);
		manager.add(fParseBindingFromKeyAction);
		manager.add(fParseBindingFromElementAction);
		manager.add(new Separator());
		manager.add(fFilterNonRelevantAction);
		manager.add(fLinkWithEditor);
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection= getSite().getSelectionProvider().getSelection();
		if (!selection.isEmpty() && ((IStructuredSelection) selection).getFirstElement() instanceof IJavaElement) {
			MenuManager showInSubMenu= new MenuManager(getShowInMenuLabel());
			IWorkbenchWindow workbenchWindow= getSite().getWorkbenchWindow();
			showInSubMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(workbenchWindow));
			manager.add(showInSubMenu);
			manager.add(new Separator());
		}
		manager.add(fFocusAction);
		manager.add(fRefreshAction);
		manager.add(fClearAction);
		manager.add(fCollapseAction);
		manager.add(fExpandAction);
		manager.add(new Separator());
		manager.add(fCopyAction);
		if (fAddToTrayAction.isEnabled())
			manager.add(fAddToTrayAction);
		manager.add(new Separator());

		fDrillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private String getShowInMenuLabel() {
		String keyBinding= null;

		IBindingService bindingService= PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		if (bindingService != null)
			keyBinding= bindingService.getBestActiveBindingFormattedFor(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);

		if (keyBinding == null)
			keyBinding= ""; //$NON-NLS-1$

		return "Sho&w In" + '\t' + keyBinding;
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(fFocusAction);
		manager.add(fRefreshAction);
		manager.add(fClearAction);
		manager.add(new Separator());
		fDrillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		manager.add(fExpandAction);
		manager.add(fCollapseAction);
		manager.add(fLinkWithEditor);
	}

	private void setASTUptoDate(boolean isuptoDate) {
		fRefreshAction.setEnabled(!isuptoDate && fTypeRoot != null);
	}

	private void makeActions() {
		fRefreshAction = new Action() {
			@Override
			public void run() {
				performRefresh();
			}
		};
		fRefreshAction.setText("&Refresh AST"); //$NON-NLS-1$
		fRefreshAction.setToolTipText("Refresh AST"); //$NON-NLS-1$
		fRefreshAction.setEnabled(false);
		ASTViewImages.setImageDescriptors(fRefreshAction, ASTViewImages.REFRESH);

		fClearAction = new Action() {
			@Override
			public void run() {
				performClear();
			}
		};
		fClearAction.setText("&Clear AST"); //$NON-NLS-1$
		fClearAction.setToolTipText("Clear AST and release memory"); //$NON-NLS-1$
		fClearAction.setEnabled(false);
		ASTViewImages.setImageDescriptors(fClearAction, ASTViewImages.CLEAR);

		fASTInputKindActions= new ASTInputKindAction[] {
				new ASTInputKindAction("Use ASTParser.&createAST", ASTInputKindAction.USE_PARSER), //$NON-NLS-1$
				new ASTInputKindAction("Use ASTParser with &focal position", ASTInputKindAction.USE_FOCAL), //$NON-NLS-1$
				new ASTInputKindAction("Use ICompilationUnit.&reconcile", ASTInputKindAction.USE_RECONCILE), //$NON-NLS-1$
				new ASTInputKindAction("Use SharedASTProvider.&getAST", ASTInputKindAction.USE_CACHE) //$NON-NLS-1$
		};

		fCreateBindingsAction = new Action("&Create Bindings", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			@Override
			public void run() {
				performCreateBindings();
			}
		};
		fCreateBindingsAction.setChecked(fCreateBindings);
		fCreateBindingsAction.setToolTipText("Create Bindings"); //$NON-NLS-1$
		fCreateBindingsAction.setEnabled(true);

		fStatementsRecoveryAction = new Action("&Statements Recovery", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			@Override
			public void run() {
				performStatementsRecovery();
			}
		};
		fStatementsRecoveryAction.setChecked(fStatementsRecovery);
		fStatementsRecoveryAction.setEnabled(true);

		fBindingsRecoveryAction = new Action("&Bindings Recovery", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			@Override
			public void run() {
				performBindingsRecovery();
			}
		};
		fBindingsRecoveryAction.setChecked(fBindingsRecovery);
		fBindingsRecoveryAction.setEnabled(true);

		fIgnoreMethodBodiesAction = new Action("&Ignore Method Bodies", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			@Override
			public void run() {
				performIgnoreMethodBodies();
			}
		};
		fIgnoreMethodBodiesAction.setChecked(fIgnoreMethodBodies);
		fIgnoreMethodBodiesAction.setEnabled(true);

		fFilterNonRelevantAction = new Action("&Hide Non-Relevant Attributes", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			@Override
			public void run() {
				performFilterNonRelevant();
			}
		};
		fFilterNonRelevantAction.setChecked(! fNonRelevantFilter.isShowNonRelevant());
		fFilterNonRelevantAction.setToolTipText("Hide non-relevant binding attributes"); //$NON-NLS-1$
		fFilterNonRelevantAction.setEnabled(true);

		fFindDeclaringNodeAction= new Action("Find &Declaring Node...", IAction.AS_PUSH_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				performFindDeclaringNode();
			}
		};
		fFindDeclaringNodeAction.setToolTipText("Find Declaring Node..."); //$NON-NLS-1$
		fFindDeclaringNodeAction.setEnabled(false);

		fParseBindingFromElementAction= new Action("&Parse Binding from &Element Handle...", IAction.AS_PUSH_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				performParseBindingFromElement();
			}
		};
		fParseBindingFromElementAction.setToolTipText("Parse Binding from Element Handle..."); //$NON-NLS-1$
		fParseBindingFromElementAction.setEnabled(true);

		fParseBindingFromKeyAction= new Action("Parse Binding from &Key...", IAction.AS_PUSH_BUTTON) { //$NON-NLS-1$
			@Override
			public void run() {
				performParseBindingFromKey();
			}
		};
		fParseBindingFromKeyAction.setToolTipText("Parse Binding from Key..."); //$NON-NLS-1$
		fParseBindingFromKeyAction.setEnabled(true);

		fFocusAction = new Action() {
			@Override
			public void run() {
				performSetFocus();
			}
		};
		fFocusAction.setText("&Show AST of active editor"); //$NON-NLS-1$
		fFocusAction.setToolTipText("Show AST of active editor"); //$NON-NLS-1$
		fFocusAction.setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
		ASTViewImages.setImageDescriptors(fFocusAction, ASTViewImages.SETFOCUS);

		fCollapseAction = new Action() {
			@Override
			public void run() {
				performCollapse();
			}
		};
		fCollapseAction.setText("C&ollapse"); //$NON-NLS-1$
		fCollapseAction.setToolTipText("Collapse Selected Node"); //$NON-NLS-1$
		fCollapseAction.setEnabled(false);
		ASTViewImages.setImageDescriptors(fCollapseAction, ASTViewImages.COLLAPSE);

		fExpandAction = new Action() {
			@Override
			public void run() {
				performExpand();
			}
		};
		fExpandAction.setText("E&xpand"); //$NON-NLS-1$
		fExpandAction.setToolTipText("Expand Selected Node"); //$NON-NLS-1$
		fExpandAction.setEnabled(false);
		ASTViewImages.setImageDescriptors(fExpandAction, ASTViewImages.EXPAND);

		fCopyAction= new TreeCopyAction(new Tree[] {fViewer.getTree(), fTray.getTree()});

		fDoubleClickAction = new Action() {
			@Override
			public void run() {
				performDoubleClick();
			}
		};

		fLinkWithEditor = new Action() {
			@Override
			public void run() {
				performLinkWithEditor();
			}
		};
		fLinkWithEditor.setChecked(fDoLinkWithEditor);
		fLinkWithEditor.setText("&Link with Editor"); //$NON-NLS-1$
		fLinkWithEditor.setToolTipText("Link With Editor"); //$NON-NLS-1$
		fLinkWithEditor.setActionDefinitionId(IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR);
		ASTViewImages.setImageDescriptors(fLinkWithEditor, ASTViewImages.LINK_WITH_EDITOR);

		fASTVersionToggleActions= new ASTLevelToggle[] {
				new ASTLevelToggle("AST Level &2 (1.2)", JLS2), //$NON-NLS-1$
				new ASTLevelToggle("AST Level &3 (1.5)", JLS3), //$NON-NLS-1$
				new ASTLevelToggle("AST Level &4 (1.7)", JLS4), //$NON-NLS-1$
				new ASTLevelToggle("AST Level &8 (1.8)", JLS8), //$NON-NLS-1$
				new ASTLevelToggle("AST Level &9 (9)", JLS9), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 1&0 (10)", JLS10), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 1&1 (11)", JLS11), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 1&2 (12)", JLS12), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 1&3 (13)", JLS13), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 1&4 (14)", JLS14), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 1&5 (15)", JLS15), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 1&6 (16)", JLS16), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 1&7 (17)", JLS17), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 1&8 (18)", JLS18), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 1&9 (19)", JLS19), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 2&0 (20)", JLS20), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 2&1 (21)", JLS21), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 2&2 (22)", JLS22), //$NON-NLS-1$
				new ASTLevelToggle("AST Level 2&3 (23)", JLS23), //$NON-NLS-1$
		};

		fAddToTrayAction= new Action() {
			@Override
			public void run() {
				performAddToTray();
			}
		};
		fAddToTrayAction.setText("&Add to Comparison Tray"); //$NON-NLS-1$
		fAddToTrayAction.setToolTipText("Add Selected Node to Comparison Tray"); //$NON-NLS-1$
		fAddToTrayAction.setEnabled(false);
		ASTViewImages.setImageDescriptors(fAddToTrayAction, ASTViewImages.ADD_TO_TRAY);

		fDeleteAction= new Action() {
			@Override
			public void run() {
				performDelete();
			}
		};
		fDeleteAction.setText("&Delete"); //$NON-NLS-1$
		fDeleteAction.setToolTipText("Delete Binding from Tray"); //$NON-NLS-1$
		fDeleteAction.setEnabled(false);
		fDeleteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		fDeleteAction.setId(ActionFactory.DELETE.getId());
		fDeleteAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);

		refreshASTSettingsActions();
	}


	private void refreshAST() throws CoreException {
		ASTNode node= getASTNodeNearSelection((IStructuredSelection) fViewer.getSelection());
		int offset= 0;
		int length= 0;
		if (node != null) {
			offset= node.getStartPosition();
			length= node.getLength();
		}

		internalSetInput(fTypeRoot, offset, length);
	}

	protected void setASTLevel(int level, boolean doRefresh) {
		int oldLevel= fCurrentASTLevel;
		fCurrentASTLevel= level;

		fDialogSettings.put(SETTINGS_JLS, fCurrentASTLevel);

		if (doRefresh && fTypeRoot != null && oldLevel != fCurrentASTLevel) {
			try {
				refreshAST();
			} catch (CoreException e) {
				showAndLogError("Could not set AST to new level.", e); //$NON-NLS-1$
				// set back to old level
				fCurrentASTLevel= oldLevel;
			}
		}
		// update action state
		for (ASTView.ASTLevelToggle action : fASTVersionToggleActions) {
			action.setChecked(action.getLevel() == fCurrentASTLevel);
		}
	}

	protected void setASTInputType(int inputKind) {
		if (inputKind != fCurrentInputKind) {
			fCurrentInputKind= inputKind;
			fDialogSettings.put(SETTINGS_INPUT_KIND, inputKind);
			for (ASTView.ASTInputKindAction action : fASTInputKindActions) {
				action.setChecked(action.getInputKind() == inputKind);
			}
			refreshASTSettingsActions();
			performRefresh();
		}
	}

	private ASTNode getASTNodeNearSelection(IStructuredSelection selection) {
		Object elem= selection.getFirstElement();
		if (elem instanceof ASTAttribute) {
			return ((ASTAttribute) elem).getParentASTNode();
		} else if (elem instanceof ASTNode) {
			return (ASTNode) elem;
		}
		return null;
	}

	private void installModificationListener() {
		fCurrentDocument= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		fCurrentDocument.addDocumentListener(fSuperListener);
	}

	private void uninstallModificationListener() {
		if (fCurrentDocument != null) {
			fCurrentDocument.removeDocumentListener(fSuperListener);
			fCurrentDocument= null;
		}
	}

	protected void handleDocumentDisposed() {
		uninstallModificationListener();
	}

	protected void handleDocumentChanged() {
		setASTUptoDate(false);
	}

	protected void handleSelectionChanged(ISelection selection) {
		fExpandAction.setEnabled(!selection.isEmpty());
		fCollapseAction.setEnabled(!selection.isEmpty());
		fCopyAction.setEnabled(!selection.isEmpty());

		boolean addEnabled= false;
		IStructuredSelection structuredSelection= (IStructuredSelection) selection;
		if (structuredSelection.size() == 1 && fViewer.getTree().isFocusControl()) {
			Object first= structuredSelection.getFirstElement();
			Object unwrapped= ASTView.unwrapAttribute(first);
			addEnabled= unwrapped != null;
		}
		fAddToTrayAction.setEnabled(addEnabled);
	}

	protected void handleEditorPostSelectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!(selection instanceof ITextSelection)) {
			return;
		}
		ITextSelection textSelection= (ITextSelection) selection;
		if (part == fEditor) {
			fViewer.getTree().setRedraw(false);
			try {
				fASTLabelProvider.setSelectedRange(textSelection.getOffset(), textSelection.getLength());
			} finally {
				fViewer.getTree().setRedraw(true);
			}
		}
		if (!fDoLinkWithEditor) {
			return;
		}
		if (fRoot == null || part != fEditor) {
			if (part instanceof ITextEditor && (EditorUtility.getJavaInput((ITextEditor) part) != null)) {
				try {
					setInput((ITextEditor) part);
				} catch (CoreException e) {
					setContentDescription(e.getStatus().getMessage());
				}
			}

		} else { // fRoot != null && part == fEditor
			doLinkWithEditor(selection);
		}
	}

	private void doLinkWithEditor(ISelection selection) {
		ITextSelection textSelection= (ITextSelection) selection;
		int offset= textSelection.getOffset();
		int length= textSelection.getLength();

		ASTNode covering= NodeFinder.perform(fRoot, offset, length);
		if (covering != null) {
			fViewer.reveal(covering);
			fViewer.setSelection(new StructuredSelection(covering));
		}
	}

	protected void handleDoubleClick() {
		fDoubleClickAction.run();
	}

	protected void performLinkWithEditor() {
		fDoLinkWithEditor= fLinkWithEditor.isChecked();
		fDialogSettings.put(SETTINGS_LINK_WITH_EDITOR, fDoLinkWithEditor);


		if (fDoLinkWithEditor && fEditor != null) {
			ISelectionProvider selectionProvider= fEditor.getSelectionProvider();
			if (selectionProvider != null) { // can be null when editor is closed
				doLinkWithEditor(selectionProvider.getSelection());
			}
		}
	}

	protected void performCollapse() {
		IStructuredSelection selection= (IStructuredSelection) fViewer.getSelection();
		if (selection.isEmpty()) {
			fViewer.collapseAll();
		} else {
			fViewer.getTree().setRedraw(false);
			for (Object s : selection.toArray()) {
				fViewer.collapseToLevel(s, AbstractTreeViewer.ALL_LEVELS);
			}
			fViewer.getTree().setRedraw(true);
		}
	}

	protected void performExpand() {
		IStructuredSelection selection= (IStructuredSelection) fViewer.getSelection();
		if (selection.isEmpty()) {
			fViewer.expandToLevel(3);
		} else {
			fViewer.getTree().setRedraw(false);
			for (Object s : selection.toArray()) {
				fViewer.expandToLevel(s, 3);
			}
			fViewer.getTree().setRedraw(true);
		}
	}

	protected void performSetFocus() {
		IEditorPart part= EditorUtility.getActiveEditor();
		if (part instanceof ITextEditor) {
			try {
				setInput((ITextEditor) part);
			} catch (CoreException e) {
				showAndLogError("Could not set AST view input ", e); //$NON-NLS-1$
			}
		}
	}

	protected void performRefresh() {
		if (fTypeRoot != null) {
			try {
				refreshAST();
			} catch (CoreException e) {
				showAndLogError("Could not set AST view input ", e); //$NON-NLS-1$
			}
		}
	}

	protected void performClear() {
		fTypeRoot= null;
		try {
			setInput(null);
		} catch (CoreException e) {
			showAndLogError("Could not reset AST view ", e); //$NON-NLS-1$
		}
		clearView();
	}

	private void showAndLogError(String message, CoreException e) {
		ASTViewPlugin.log(message, e);
		ErrorDialog.openError(getSite().getShell(), "AST View", message, e.getStatus()); //$NON-NLS-1$
	}

	private void showAndLogError(String message, Throwable e) {
		IStatus status= new Status(IStatus.ERROR, ASTViewPlugin.getPluginId(), 0, message, e);
		ASTViewPlugin.log(status);
		ErrorDialog.openError(getSite().getShell(), "AST View", null, status); //$NON-NLS-1$
	}

	protected void performCreateBindings() {
		fCreateBindings= fCreateBindingsAction.isChecked();
		fDialogSettings.put(SETTINGS_NO_BINDINGS, !fCreateBindings);
		performRefresh();
	}

	protected void performStatementsRecovery() {
		fStatementsRecovery= fStatementsRecoveryAction.isChecked();
		fDialogSettings.put(SETTINGS_NO_STATEMENTS_RECOVERY, !fStatementsRecovery);
		performRefresh();
	}

	protected void performBindingsRecovery() {
		fBindingsRecovery= fBindingsRecoveryAction.isChecked();
		fDialogSettings.put(SETTINGS_NO_BINDINGS_RECOVERY, !fBindingsRecovery);
		performRefresh();
	}

	protected void performIgnoreMethodBodies() {
		fIgnoreMethodBodies= fIgnoreMethodBodiesAction.isChecked();
		fDialogSettings.put(SETTINGS_IGNORE_METHOD_BODIES, fIgnoreMethodBodies);
		performRefresh();
	}

	protected void performFilterNonRelevant() {
		boolean showNonRelevant= !fFilterNonRelevantAction.isChecked();
		fNonRelevantFilter.setShowNonRelevant(showNonRelevant);
		fDialogSettings.put(SETTINGS_SHOW_NON_RELEVANT, showNonRelevant);
		fViewer.refresh();
	}

	protected void performFindDeclaringNode() {
		String msg= "Find Declaring Node from Key";
		String key= askForKey(msg);
		if (key == null)
			return;
		ASTNode node= fRoot.findDeclaringNode(key);
		if (node != null) {
			fViewer.setSelection(new StructuredSelection(node), true);
		} else {
			MessageDialog.openError(
					getSite().getShell(),
					"Find Declaring Node from Key",
					"The declaring node for key '" + key + "' could not be found");
		}
	}

	private String askForKey(String dialogTitle) {
		InputDialog dialog= new InputDialog(getSite().getShell(), dialogTitle, "Key: (optionally surrounded by <KEY: \"> and <\">)", "", null);
		if (dialog.open() != Window.OK)
			return null;

		String key= dialog.getValue();
		if (key.startsWith("KEY: \"") && key.endsWith("\""))
			key= key.substring(6, key.length() - 1);
		return key;
	}

	protected void performParseBindingFromKey() {
		String msg= "Parse Binding from Key";
		String key= askForKey(msg);
		if (key == null)
			return;
		ASTParser parser= ASTParser.newParser(fCurrentASTLevel);
		parser.setResolveBindings(true);
		parser.setProject(fTypeRoot.getJavaProject());
		class MyASTRequestor extends ASTRequestor {
			String fBindingKey;
			IBinding fBinding;
			@Override
			public void acceptBinding(String bindingKey, IBinding binding) {
				fBindingKey= bindingKey;
				fBinding= binding;
			}
		}
		MyASTRequestor requestor= new MyASTRequestor();
		ASTAttribute item;
		Object viewerInput= fViewer.getInput();
		try {
			parser.createASTs(new ICompilationUnit[0], new String[] { key }, requestor, null);
			if (requestor.fBindingKey != null) {
				String name= requestor.fBindingKey + ": " + Binding.getBindingLabel(requestor.fBinding);
				item= new Binding(viewerInput, name, requestor.fBinding, true);
			} else {
				item= new Error(viewerInput, "Key not resolved: " + key, null);
			}
		} catch (RuntimeException e) {
			item= new Error(viewerInput, "Error resolving key: " + key, e);
		}
		fViewer.add(viewerInput, item);
		fViewer.setSelection(new StructuredSelection(item), true);
	}

	protected void performParseBindingFromElement() {
		InputDialog dialog= new InputDialog(getSite().getShell(), "Parse Binding from Java Element", "IJavaElement#getHandleIdentifier():", "", null);
		if (dialog.open() != Window.OK)
			return;

		String handleIdentifier= dialog.getValue();
		IJavaElement handle= JavaCore.create(handleIdentifier);

		Object viewerInput= fViewer.getInput();
		ASTAttribute item;
		if (handle == null) {
			item= new Error(viewerInput, "handleIdentifier not resolved: " + handleIdentifier, null);
		} else if (! handle.exists()) {
			item= new Error(viewerInput, "element does not exist: " + handleIdentifier, null);
		} else if (handle.getJavaProject() == null) {
			item= new Error(viewerInput, "getJavaProject() is null: " + handleIdentifier, null);
		} else {
			IJavaProject project= handle.getJavaProject();
			ASTParser parser= ASTParser.newParser(fCurrentASTLevel);
			parser.setProject(project);
			IBinding[] bindings= parser.createBindings(new IJavaElement[] { handle }, null);
			String name= handleIdentifier + ": " + Binding.getBindingLabel(bindings[0]);
			item= new Binding(viewerInput, name, bindings[0], true);
		}
		fViewer.add(viewerInput, item);
		fViewer.setSelection(new StructuredSelection(item), true);
	}

	protected void performDoubleClick() {
		if (fEditor == null) {
			return;
		}

		ISelection selection = fViewer.getSelection();
		Object obj = ((IStructuredSelection) selection).getFirstElement();

		boolean isTripleClick= (obj == fPreviousDouble);
		fPreviousDouble= isTripleClick ? null : obj;

		if (obj instanceof ExceptionAttribute) {
			Throwable exception= ((ExceptionAttribute) obj).getException();
			if (exception != null) {
				String label= ((ExceptionAttribute) obj).getLabel();
				showAndLogError("An error occurred while calculating an AST View Label:\n" + label, exception); //$NON-NLS-1$
				return;
			}
		}

		ASTNode node= null, nodeEnd= null;
		if (obj instanceof ASTNode) {
			node= (ASTNode) obj;

		} else if (obj instanceof NodeProperty) {
			Object val= ((NodeProperty) obj).getNode();
			if (val instanceof ASTNode) {
				node= (ASTNode) val;
			} else if (val instanceof List) {
				List<?> list= (List<?>) val;
				if (list.size() > 0) {
					node= (ASTNode) list.get(0);
					nodeEnd= (ASTNode) list.get(list.size() - 1);
				} else {
					fViewer.getTree().getDisplay().beep();
				}
			}

		} else if (obj instanceof Binding) {
			IBinding binding= ((Binding) obj).getBinding();
			ASTNode declaring= fRoot.findDeclaringNode(binding);
			if (declaring != null) {
				fViewer.reveal(declaring);
				fViewer.setSelection(new StructuredSelection(declaring));
			} else {
				fViewer.getTree().getDisplay().beep();
			}
			return;

		} else if (obj instanceof ProblemNode) {
			ProblemNode problemNode= (ProblemNode) obj;
			EditorUtility.selectInEditor(fEditor, problemNode.getOffset(), problemNode.getLength());
			return;

		} else if (obj instanceof JavaElement) {
			IJavaElement javaElement= ((JavaElement) obj).getJavaElement();
			if (javaElement instanceof IPackageFragment) {
				try {
					IViewPart packageExplorer= getSite().getPage().showView(JavaUI.ID_PACKAGES);
					if (packageExplorer instanceof IShowInTarget) {
						IShowInTarget showInTarget= (IShowInTarget) packageExplorer;
						showInTarget.show(getShowInContext());
					}
				} catch (PartInitException e) {
					showAndLogError("Could not open Package Explorer.", e); //$NON-NLS-1$
				}
			} else {
				try {
					IEditorPart editorPart= JavaUI.openInEditor(javaElement);
					if (editorPart != null)
						JavaUI.revealInEditor(editorPart, javaElement);
				} catch (PartInitException e) {
					showAndLogError("Could not open editor.", e); //$NON-NLS-1$
				} catch (JavaModelException e) {
					showAndLogError("Could not open editor.", e); //$NON-NLS-1$
				}
			}
			return;
		}

		if (node != null) {
			int offset= isTripleClick ? fRoot.getExtendedStartPosition(node) : node.getStartPosition();
			int length;
			if (nodeEnd == null) {
				length= isTripleClick ? fRoot.getExtendedLength(node) : node.getLength();
			} else {
				length= isTripleClick
						? fRoot.getExtendedStartPosition(nodeEnd) + fRoot.getExtendedLength(nodeEnd) - fRoot.getExtendedStartPosition(node)
						: nodeEnd.getStartPosition() + nodeEnd.getLength() - node.getStartPosition();
			}
			EditorUtility.selectInEditor(fEditor, offset, length);
		}
	}

	protected void performAddToTray() {
		IStructuredSelection selection= (IStructuredSelection) fViewer.getSelection();
		Object firstElement= selection.getFirstElement();
		if (! fTrayRoots.contains(firstElement)) {
			fTrayRoots.add(firstElement);
			fTray.setInput(fTrayRoots);
		}
		if (fSash.getMaximizedControl() != null) {
			int trayHeight= fTray.getTree().getItemHeight() * (2 + TrayContentProvider.DEFAULT_CHILDREN_COUNT);
			int sashHeight= fSash.getClientArea().height;
			fSash.setWeights(new int[] { sashHeight - trayHeight, trayHeight });
			fSash.setMaximizedControl(null);
		}
		setTraySelection(selection);
	}

	private void setTraySelection(IStructuredSelection selection) {
		fTray.setSelection(selection, true);
		TreeItem[] itemSelection= fTray.getTree().getSelection();
		if (itemSelection.length > 0)
			fTray.getTree().setTopItem(itemSelection[0]);
	}

	protected void performTrayDoubleClick() {
		IStructuredSelection selection= (IStructuredSelection) fTray.getSelection();
		if (selection.size() != 1)
			return;
		Object obj = selection.getFirstElement();
		if (obj instanceof ExceptionAttribute) {
			Throwable exception= ((ExceptionAttribute) obj).getException();
			if (exception != null) {
				String label= ((ExceptionAttribute) obj).getLabel();
				showAndLogError("An error occurred while calculating an AST View Label:\n" + label, exception); //$NON-NLS-1$
				return;
			}
		}
		if (obj instanceof Binding) {
			Binding binding= (Binding) obj;
			fViewer.setSelection(new StructuredSelection(binding), true);
		}
	}

	protected void performDelete() {
		boolean removed= false;
		IStructuredSelection selection= (IStructuredSelection) fTray.getSelection();
		for (Iterator<?> iter= selection.iterator(); iter.hasNext();) {
			Object obj= iter.next();
			if (obj instanceof DynamicAttributeProperty)
				obj= ((DynamicAttributeProperty) obj).getParent();
			if (obj instanceof DynamicBindingProperty)
				obj= ((DynamicBindingProperty) obj).getParent();

			removed|= fTrayRoots.remove(obj);
		}
		if (removed)
			fTray.setInput(fTrayRoots);
	}

	@Override
	public void setFocus() {
		fViewer.getControl().setFocus();
	}

	@Override
	public ShowInContext getShowInContext() {
		return new ShowInContext(null, getSite().getSelectionProvider().getSelection());
	}

	@Override
	public String[] getShowInTargetIds() {
		return new String[] { "org.eclipse.jdt.jeview.views.JavaElementView", JavaUI.ID_PACKAGES };
	}

	/**
	 * @param attribute an attribute
	 * @return the object inside the attribute, or <code>null</code> iff none
	 */
	static Object unwrapAttribute(Object attribute) {
		if (attribute instanceof Binding) {
			return ((Binding) attribute).getBinding();
		} else if (attribute instanceof JavaElement) {
			return ((JavaElement) attribute).getJavaElement();
		} else if (attribute instanceof ASTNode) {
			return attribute;
		} else {
			return null;
		}
	}
}
