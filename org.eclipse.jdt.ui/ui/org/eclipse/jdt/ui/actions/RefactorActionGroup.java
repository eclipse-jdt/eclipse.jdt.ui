/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.operations.IUndoContext;

import org.eclipse.core.runtime.PerformanceStats;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.ExtractSuperTypeAction;
import org.eclipse.jdt.internal.ui.actions.JDTQuickMenuAction;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * Action group that adds refactor actions (for example 'Rename', 'Move')
 * to a context menu and the global menu bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class RefactorActionGroup extends ActionGroup {
	
	private static final String PERF_REFACTOR_ACTION_GROUP= "org.eclipse.jdt.ui/perf/explorer/RefactorActionGroup"; //$NON-NLS-1$

	/**
	 * Pop-up menu: id of the refactor sub menu (value <code>org.eclipse.jdt.ui.refactoring.menu</code>).
	 * 
	 * @since 2.1
	 */
	public static final String MENU_ID= "org.eclipse.jdt.ui.refactoring.menu"; //$NON-NLS-1$

	/**
	 * Pop-up menu: id of the reorg group of the refactor sub menu (value
	 * <code>reorgGroup</code>).
	 * 
	 * @since 2.1
	 */
	public static final String GROUP_REORG= "reorgGroup"; //$NON-NLS-1$

	/**
	 * Pop-up menu: id of the type group of the refactor sub menu (value
	 * <code>typeGroup</code>).
	 * 
	 * @since 2.1
	 */
	public static final String GROUP_TYPE= "typeGroup"; //$NON-NLS-1$

	/**
	 * Pop-up menu: id of the coding group of the refactor sub menu (value
	 * <code>codingGroup</code>).
	 * 
	 * @since 2.1
	 */
	public static final String GROUP_CODING= "codingGroup"; //$NON-NLS-1$
	
	/**
	 * Pop-up menu: id of the coding group 2 of the refactor sub menu (value
	 * <code>codingGroup2</code>).
	 * 
	 * @since 3.2
	 */
	public static final String GROUP_CODING2= "codingGroup2"; //$NON-NLS-1$

	/**
	 * Pop-up menu: id of the reorg group 2 of the refactor sub menu (value
	 * <code>reorgGroup2</code>).
	 * 
	 * @since 3.2
	 */
	private static final String GROUP_REORG2= "reorgGroup2"; //$NON-NLS-1$ //TODO(3.3): make public
	
	/**
	 * Pop-up menu: id of the type group 2 of the refactor sub menu (value
	 * <code>typeGroup2</code>).
	 * 
	 * @since 3.2
	 */
	private static final String GROUP_TYPE2= "typeGroup2"; //$NON-NLS-1$ //TODO(3.3): make public
	
	private IWorkbenchSite fSite;
	private CompilationUnitEditor fEditor;
	private String fGroupName= IContextMenuConstants.GROUP_REORGANIZE;

 	private SelectionDispatchAction fMoveAction;
	private SelectionDispatchAction fRenameAction;
	private SelectionDispatchAction fModifyParametersAction;
	private SelectionDispatchAction fConvertAnonymousToNestedAction;
	private SelectionDispatchAction fConvertNestedToTopAction;
	
	private SelectionDispatchAction fPullUpAction;
	private SelectionDispatchAction fPushDownAction;
	private SelectionDispatchAction fExtractInterfaceAction;
	private SelectionDispatchAction fExtractSupertypeAction;
	private SelectionDispatchAction fChangeTypeAction;
	private SelectionDispatchAction fUseSupertypeAction;
	private SelectionDispatchAction fInferTypeArgumentsAction;
	
	private SelectionDispatchAction fInlineAction;
//	private SelectionDispatchAction fReplaceInvocationsAction;
	private SelectionDispatchAction fIntroduceIndirectionAction;
	private SelectionDispatchAction fExtractMethodAction;
	private SelectionDispatchAction fExtractTempAction;
	private SelectionDispatchAction fExtractConstantAction;
	private SelectionDispatchAction fIntroduceParameterAction;
	private SelectionDispatchAction fIntroduceFactoryAction;
	private SelectionDispatchAction fConvertLocalToFieldAction;
	private SelectionDispatchAction fSelfEncapsulateField;
	
	private UndoRedoActionGroup fUndoRedoActionGroup;
	
	private final List fActions= new ArrayList();
	
	private static final String QUICK_MENU_ID= "org.eclipse.jdt.ui.edit.text.java.refactor.quickMenu"; //$NON-NLS-1$
	
	private class RefactorQuickAccessAction extends JDTQuickMenuAction {
		public RefactorQuickAccessAction(CompilationUnitEditor editor) {
			super(editor, QUICK_MENU_ID); 
		}
		protected void fillMenu(IMenuManager menu) {
			fillQuickMenu(menu);
		}
	}
	
	private JDTQuickMenuAction fQuickAccessAction;
	private IHandlerActivation fQuickAccessHandlerActivation;
	private IHandlerService fHandlerService;

	private static class NoActionAvailable extends Action {
		public NoActionAvailable() {
			setEnabled(true);
			setText(RefactoringMessages.RefactorActionGroup_no_refactoring_available); 
		}
	}
	private Action fNoActionAvailable= new NoActionAvailable(); 
		
	/**
	 * Creates a new <code>RefactorActionGroup</code>. The group requires
	 * that the selection provided by the part's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public RefactorActionGroup(IViewPart part) {
		this(part.getSite());
		
		IUndoContext workspaceContext= (IUndoContext)ResourcesPlugin.getWorkspace().getAdapter(IUndoContext.class);
		fUndoRedoActionGroup= new UndoRedoActionGroup(part.getViewSite(), workspaceContext, true);
	}	
	
	/**
	 * Creates a new <code>RefactorActionGroup</code>. The action requires
	 * that the selection provided by the page's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public RefactorActionGroup(Page page) {
		this(page.getSite());
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the compilation unit editor
	 * @param groupName the group name to add the actions to
	 */
	public RefactorActionGroup(CompilationUnitEditor editor, String groupName) {

		final PerformanceStats stats= PerformanceStats.getStats(PERF_REFACTOR_ACTION_GROUP, this);
		stats.startRun();

		fSite= editor.getEditorSite();
		fEditor= editor;
		fGroupName= groupName;
		ISelectionProvider provider= editor.getSelectionProvider();
		ISelection selection= provider.getSelection();
		
		fRenameAction= new RenameAction(editor);
		initAction(fRenameAction, selection, IJavaEditorActionDefinitionIds.RENAME_ELEMENT);
		editor.setAction("RenameElement", fRenameAction); //$NON-NLS-1$
		
		fMoveAction= new MoveAction(editor);
		initAction(fMoveAction, selection, IJavaEditorActionDefinitionIds.MOVE_ELEMENT);
		editor.setAction("MoveElement", fMoveAction); //$NON-NLS-1$
				
		fModifyParametersAction= new ModifyParametersAction(editor);
		initAction(fModifyParametersAction, selection, IJavaEditorActionDefinitionIds.MODIFY_METHOD_PARAMETERS);
		editor.setAction("ModifyParameters", fModifyParametersAction); //$NON-NLS-1$

		fConvertAnonymousToNestedAction= new ConvertAnonymousToNestedAction(editor);
		initUpdatingAction(fConvertAnonymousToNestedAction, provider, selection, IJavaEditorActionDefinitionIds.CONVERT_ANONYMOUS_TO_NESTED);
		editor.setAction("ConvertAnonymousToNested", fConvertAnonymousToNestedAction); //$NON-NLS-1$

		fConvertNestedToTopAction= new ConvertNestedToTopAction(editor);
		initAction(fConvertNestedToTopAction, selection, IJavaEditorActionDefinitionIds.MOVE_INNER_TO_TOP);
		editor.setAction("MoveInnerToTop", fConvertNestedToTopAction); //$NON-NLS-1$
		
		fPullUpAction= new PullUpAction(editor);
		initAction(fPullUpAction, selection, IJavaEditorActionDefinitionIds.PULL_UP);
		editor.setAction("PullUp", fPullUpAction); //$NON-NLS-1$

		fPushDownAction= new PushDownAction(editor);
		initAction(fPushDownAction, selection, IJavaEditorActionDefinitionIds.PUSH_DOWN);
		editor.setAction("PushDown", fPushDownAction); //$NON-NLS-1$

		fExtractSupertypeAction= new ExtractSuperTypeAction(editor);
		initAction(fExtractSupertypeAction, selection, ExtractSuperTypeAction.EXTRACT_SUPERTYPE);
		editor.setAction("ExtractSupertype", fExtractSupertypeAction); //$NON-NLS-1$
		
		fExtractInterfaceAction= new ExtractInterfaceAction(editor);
		initAction(fExtractInterfaceAction, selection, IJavaEditorActionDefinitionIds.EXTRACT_INTERFACE);
		editor.setAction("ExtractInterface", fExtractInterfaceAction); //$NON-NLS-1$

		fChangeTypeAction= new ChangeTypeAction(editor);
		initUpdatingAction(fChangeTypeAction, provider, selection, IJavaEditorActionDefinitionIds.CHANGE_TYPE);
		editor.setAction("ChangeType", fChangeTypeAction); //$NON-NLS-1$

		fUseSupertypeAction= new UseSupertypeAction(editor);
		initAction(fUseSupertypeAction, selection, IJavaEditorActionDefinitionIds.USE_SUPERTYPE);
		editor.setAction("UseSupertype", fUseSupertypeAction); //$NON-NLS-1$
		
		fInferTypeArgumentsAction= new InferTypeArgumentsAction(editor);
		initAction(fInferTypeArgumentsAction, selection, IJavaEditorActionDefinitionIds.INFER_TYPE_ARGUMENTS_ACTION);
		editor.setAction("InferTypeArguments", fInferTypeArgumentsAction); //$NON-NLS-1$
		
		fInlineAction= new InlineAction(editor);
		initAction(fInlineAction, selection, IJavaEditorActionDefinitionIds.INLINE);
		editor.setAction("Inline", fInlineAction); //$NON-NLS-1$
		
		fExtractMethodAction= new ExtractMethodAction(editor);
		initUpdatingAction(fExtractMethodAction, provider, selection, IJavaEditorActionDefinitionIds.EXTRACT_METHOD);
		editor.setAction("ExtractMethod", fExtractMethodAction); //$NON-NLS-1$

		fExtractTempAction= new ExtractTempAction(editor);
		initUpdatingAction(fExtractTempAction, provider, selection, IJavaEditorActionDefinitionIds.EXTRACT_LOCAL_VARIABLE);
		editor.setAction("ExtractLocalVariable", fExtractTempAction); //$NON-NLS-1$

		fExtractConstantAction= new ExtractConstantAction(editor);
		initUpdatingAction(fExtractConstantAction, provider, selection, IJavaEditorActionDefinitionIds.EXTRACT_CONSTANT);
		editor.setAction("ExtractConstant", fExtractConstantAction); //$NON-NLS-1$
		
//		fReplaceInvocationsAction= new ReplaceInvocationsAction(editor);
//		initUpdatingAction(fReplaceInvocationsAction, provider, selection, IJavaEditorActionDefinitionIds.REPLACE_INVOCATIONS);
//		editor.setAction("ReplaceInvocations", fReplaceInvocationsAction); //$NON-NLS-1$
		
		fIntroduceIndirectionAction= new IntroduceIndirectionAction(editor);
		initUpdatingAction(fIntroduceIndirectionAction, provider, selection, IJavaEditorActionDefinitionIds.INTRODUCE_INDIRECTION);
		editor.setAction("IntroduceIndirection", fIntroduceIndirectionAction); //$NON-NLS-1$
		
		fIntroduceParameterAction= new IntroduceParameterAction(editor);
		initUpdatingAction(fIntroduceParameterAction, provider, selection, IJavaEditorActionDefinitionIds.INTRODUCE_PARAMETER);
		editor.setAction("IntroduceParameter", fIntroduceParameterAction); //$NON-NLS-1$

		fIntroduceFactoryAction= new IntroduceFactoryAction(editor);
		initUpdatingAction(fIntroduceFactoryAction, provider, selection, IJavaEditorActionDefinitionIds.INTRODUCE_FACTORY);
		editor.setAction("IntroduceFactory", fIntroduceFactoryAction); //$NON-NLS-1$

		fConvertLocalToFieldAction= new ConvertLocalToFieldAction(editor);
		initUpdatingAction(fConvertLocalToFieldAction, provider, selection, IJavaEditorActionDefinitionIds.PROMOTE_LOCAL_VARIABLE);
		editor.setAction("PromoteTemp", fConvertLocalToFieldAction); //$NON-NLS-1$

		fSelfEncapsulateField= new SelfEncapsulateFieldAction(editor);
		initAction(fSelfEncapsulateField, selection, IJavaEditorActionDefinitionIds.SELF_ENCAPSULATE_FIELD);
		editor.setAction("SelfEncapsulateField", fSelfEncapsulateField); //$NON-NLS-1$
		
		installQuickAccessAction();
		
		stats.endRun();
	}

	private RefactorActionGroup(IWorkbenchSite site) {

		final PerformanceStats stats= PerformanceStats.getStats(PERF_REFACTOR_ACTION_GROUP, this);
		stats.startRun();

		fSite= site;
		ISelectionProvider provider= fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();
		
		fMoveAction= new MoveAction(site);
		initUpdatingAction(fMoveAction, provider, selection, IJavaEditorActionDefinitionIds.MOVE_ELEMENT);
		
		fRenameAction= new RenameAction(site);
		initUpdatingAction(fRenameAction, provider, selection, IJavaEditorActionDefinitionIds.RENAME_ELEMENT);
		
		fModifyParametersAction= new ModifyParametersAction(fSite);
		initUpdatingAction(fModifyParametersAction, provider, selection, IJavaEditorActionDefinitionIds.MODIFY_METHOD_PARAMETERS);
		
		fPullUpAction= new PullUpAction(fSite);
		initUpdatingAction(fPullUpAction, provider, selection, IJavaEditorActionDefinitionIds.PULL_UP);

		fPushDownAction= new PushDownAction(fSite);
		initUpdatingAction(fPushDownAction, provider, selection, IJavaEditorActionDefinitionIds.PUSH_DOWN);
		
		fSelfEncapsulateField= new SelfEncapsulateFieldAction(fSite);
		initUpdatingAction(fSelfEncapsulateField, provider, selection, IJavaEditorActionDefinitionIds.SELF_ENCAPSULATE_FIELD);

		fExtractSupertypeAction= new ExtractSuperTypeAction(fSite);
		initUpdatingAction(fExtractSupertypeAction, provider, selection, ExtractSuperTypeAction.EXTRACT_SUPERTYPE);

		fExtractInterfaceAction= new ExtractInterfaceAction(fSite);
		initUpdatingAction(fExtractInterfaceAction, provider, selection, IJavaEditorActionDefinitionIds.EXTRACT_INTERFACE);

		fChangeTypeAction= new ChangeTypeAction(fSite);
		initUpdatingAction(fChangeTypeAction, provider, selection, IJavaEditorActionDefinitionIds.CHANGE_TYPE);

		fConvertNestedToTopAction= new ConvertNestedToTopAction(fSite);
		initUpdatingAction(fConvertNestedToTopAction, provider, selection, IJavaEditorActionDefinitionIds.MOVE_INNER_TO_TOP);

		fUseSupertypeAction= new UseSupertypeAction(fSite);
		initUpdatingAction(fUseSupertypeAction, provider, selection, IJavaEditorActionDefinitionIds.USE_SUPERTYPE);
		
		fInferTypeArgumentsAction= new InferTypeArgumentsAction(fSite);
		initUpdatingAction(fInferTypeArgumentsAction, provider, selection, IJavaEditorActionDefinitionIds.INFER_TYPE_ARGUMENTS_ACTION);
		
		fInlineAction= new InlineAction(fSite);
		initUpdatingAction(fInlineAction, provider, selection, IJavaEditorActionDefinitionIds.INLINE);
		
//		fReplaceInvocationsAction= new ReplaceInvocationsAction(fSite);
//		initUpdatingAction(fReplaceInvocationsAction, provider, selection, IJavaEditorActionDefinitionIds.REPLACE_INVOCATIONS);
		
		fIntroduceIndirectionAction= new IntroduceIndirectionAction(fSite);
		initUpdatingAction(fIntroduceIndirectionAction, provider, selection, IJavaEditorActionDefinitionIds.INTRODUCE_INDIRECTION);
		
		fIntroduceFactoryAction= new IntroduceFactoryAction(fSite);
		initUpdatingAction(fIntroduceFactoryAction, provider, selection, IJavaEditorActionDefinitionIds.INTRODUCE_FACTORY);
		
		fConvertAnonymousToNestedAction= new ConvertAnonymousToNestedAction(fSite);
		initUpdatingAction(fConvertAnonymousToNestedAction, provider, selection, IJavaEditorActionDefinitionIds.CONVERT_ANONYMOUS_TO_NESTED);
		
		installQuickAccessAction();
		
		stats.endRun();
	}
	
	private void installQuickAccessAction() {
		fHandlerService= (IHandlerService)fSite.getService(IHandlerService.class);
		if (fHandlerService != null) {
			fQuickAccessAction= new RefactorQuickAccessAction(fEditor);
			fQuickAccessHandlerActivation= fHandlerService.activateHandler(fQuickAccessAction.getActionDefinitionId(), new ActionHandler(fQuickAccessAction));
		}
	}
	
	private void initAction(SelectionDispatchAction action, ISelection selection, String actionDefinitionId){
		initUpdatingAction(action, null, selection, actionDefinitionId);
	}
	
	/**
	 * Sets actionDefinitionId, updates enablement, adds to fActions,
	 * and adds selection changed listener if provider is not <code>null</code>.
	 *  
	 * @param action
	 * @param provider can be <code>null</code>
	 * @param selection
	 * @param actionDefinitionId
	 */
	private void initUpdatingAction(SelectionDispatchAction action, ISelectionProvider provider, ISelection selection, String actionDefinitionId){
		action.setActionDefinitionId(actionDefinitionId);
		action.update(selection);
		if (provider != null)
			provider.addSelectionChangedListener(action);
		fActions.add(action);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(JdtActionConstants.SELF_ENCAPSULATE_FIELD, fSelfEncapsulateField);
		actionBars.setGlobalActionHandler(JdtActionConstants.MOVE, fMoveAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.RENAME, fRenameAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.MODIFY_PARAMETERS, fModifyParametersAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.PULL_UP, fPullUpAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.PUSH_DOWN, fPushDownAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_TEMP, fExtractTempAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_CONSTANT, fExtractConstantAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.INTRODUCE_PARAMETER, fIntroduceParameterAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.INTRODUCE_FACTORY, fIntroduceFactoryAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_METHOD, fExtractMethodAction);
//		actionBars.setGlobalActionHandler(JdtActionConstants.REPLACE_INVOCATIONS, fReplaceInvocationsAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.INTRODUCE_INDIRECTION, fIntroduceIndirectionAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.INLINE, fInlineAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_INTERFACE, fExtractInterfaceAction);
		actionBars.setGlobalActionHandler(ExtractSuperTypeAction.EXTRACT_SUPERTYPES, fExtractSupertypeAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.CHANGE_TYPE, fChangeTypeAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.CONVERT_NESTED_TO_TOP, fConvertNestedToTopAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.USE_SUPERTYPE, fUseSupertypeAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.INFER_TYPE_ARGUMENTS, fInferTypeArgumentsAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.CONVERT_LOCAL_TO_FIELD, fConvertLocalToFieldAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.CONVERT_ANONYMOUS_TO_NESTED, fConvertAnonymousToNestedAction);
		if (fUndoRedoActionGroup != null) {
			fUndoRedoActionGroup.fillActionBars(actionBars);
		}
	}
	
	/**
	 * Retargets the File actions with the corresponding refactoring actions.
	 * 
	 * @param actionBars the action bar to register the move and rename action with
	 */
	public void retargetFileMenuActions(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(), fRenameAction);
		actionBars.setGlobalActionHandler(ActionFactory.MOVE.getId(), fMoveAction);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		addRefactorSubmenu(menu);
	}
	
	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		ISelectionProvider provider= fSite.getSelectionProvider();
		disposeAction(fSelfEncapsulateField, provider);
		disposeAction(fMoveAction, provider);
		disposeAction(fRenameAction, provider);
		disposeAction(fModifyParametersAction, provider);
		disposeAction(fPullUpAction, provider);
		disposeAction(fPushDownAction, provider);
		disposeAction(fExtractTempAction, provider);
		disposeAction(fExtractConstantAction, provider);
		disposeAction(fIntroduceParameterAction, provider);
		disposeAction(fIntroduceFactoryAction, provider);
		disposeAction(fExtractMethodAction, provider);
		disposeAction(fIntroduceIndirectionAction, provider);
		disposeAction(fInlineAction, provider);
//		disposeAction(fReplaceInvocationsAction, provider);
		disposeAction(fExtractInterfaceAction, provider);
		disposeAction(fExtractSupertypeAction, provider);
		disposeAction(fChangeTypeAction, provider);
		disposeAction(fConvertNestedToTopAction, provider);
		disposeAction(fUseSupertypeAction, provider);
		disposeAction(fInferTypeArgumentsAction, provider);
		disposeAction(fConvertLocalToFieldAction, provider);
		disposeAction(fConvertAnonymousToNestedAction, provider);
		if (fQuickAccessHandlerActivation != null && fHandlerService != null) {
			fHandlerService.deactivateHandler(fQuickAccessHandlerActivation);
		}
		if (fUndoRedoActionGroup != null) {
			fUndoRedoActionGroup.dispose();
		}
		super.dispose();
	}
	
	private void disposeAction(ISelectionChangedListener action, ISelectionProvider provider) {
		if (action != null)
			provider.removeSelectionChangedListener(action);
	}
	
	private void addRefactorSubmenu(IMenuManager menu) {
		String menuText= ActionMessages.RefactorMenu_label; 
		if (fQuickAccessAction != null) {
			menuText= fQuickAccessAction.addShortcut(menuText); 
		}
		IMenuManager refactorSubmenu= new MenuManager(menuText, MENU_ID);  
		if (fEditor != null) {
			IJavaElement element= SelectionConverter.getInput(fEditor);
			if (element != null && ActionUtil.isOnBuildPath(element)) {
				refactorSubmenu.addMenuListener(new IMenuListener() {
					public void menuAboutToShow(IMenuManager manager) {
						refactorMenuShown(manager);
					}
				});
				refactorSubmenu.add(fNoActionAvailable);
				menu.appendToGroup(fGroupName, refactorSubmenu);
			} 
		} else {
			ISelection selection= fSite.getSelectionProvider().getSelection();
			for (Iterator iter= fActions.iterator(); iter.hasNext(); ) {
				((SelectionDispatchAction)iter.next()).update(selection);
			}
			if (fillRefactorMenu(refactorSubmenu) > 0)
				menu.appendToGroup(fGroupName, refactorSubmenu);
		}
	}
	
	private int fillRefactorMenu(IMenuManager refactorSubmenu) {
		int added= 0;
		refactorSubmenu.add(new Separator(GROUP_REORG));
		added+= addAction(refactorSubmenu, fRenameAction);
		added+= addAction(refactorSubmenu, fMoveAction);
		refactorSubmenu.add(new Separator(GROUP_CODING));
		added+= addAction(refactorSubmenu, fModifyParametersAction);
		added+= addAction(refactorSubmenu, fExtractMethodAction);
		added+= addAction(refactorSubmenu, fExtractTempAction);
		added+= addAction(refactorSubmenu, fExtractConstantAction);
		added+= addAction(refactorSubmenu, fInlineAction);
		refactorSubmenu.add(new Separator(GROUP_REORG2));
		added+= addAction(refactorSubmenu, fConvertAnonymousToNestedAction);
		added+= addAction(refactorSubmenu, fConvertNestedToTopAction);
		added+= addAction(refactorSubmenu, fConvertLocalToFieldAction);
		refactorSubmenu.add(new Separator(GROUP_TYPE));
		added+= addAction(refactorSubmenu, fExtractInterfaceAction);
		added+= addAction(refactorSubmenu, fExtractSupertypeAction);
		added+= addAction(refactorSubmenu, fUseSupertypeAction);
		added+= addAction(refactorSubmenu, fPullUpAction);
		added+= addAction(refactorSubmenu, fPushDownAction);
		refactorSubmenu.add(new Separator(GROUP_CODING2));
		added+= addAction(refactorSubmenu, fIntroduceIndirectionAction);
		added+= addAction(refactorSubmenu, fIntroduceFactoryAction);
		added+= addAction(refactorSubmenu, fIntroduceParameterAction);
		added+= addAction(refactorSubmenu, fSelfEncapsulateField);
//		added+= addAction(refactorSubmenu, fReplaceInvocationsAction);
		refactorSubmenu.add(new Separator(GROUP_TYPE2));
		added+= addAction(refactorSubmenu, fChangeTypeAction);
		added+= addAction(refactorSubmenu, fInferTypeArgumentsAction);
		return added;
	}
	
	private int addAction(IMenuManager menu, IAction action) {
		if (action != null && action.isEnabled()) {
			menu.add(action);
			return 1;
		}
		return 0;
	}
	
	private void refactorMenuShown(final IMenuManager refactorSubmenu) {
		// we know that we have an MenuManager since we created it in
		// addRefactorSubmenu.
		Menu menu= ((MenuManager)refactorSubmenu).getMenu();
		menu.addMenuListener(new MenuAdapter() {
			public void menuHidden(MenuEvent e) {
				refactorMenuHidden(refactorSubmenu);
			}
		});
		ITextSelection textSelection= (ITextSelection)fEditor.getSelectionProvider().getSelection();
		JavaTextSelection javaSelection= new JavaTextSelection(
			getEditorInput(), getDocument(), textSelection.getOffset(), textSelection.getLength());
		
		for (Iterator iter= fActions.iterator(); iter.hasNext(); ) {
			SelectionDispatchAction action= (SelectionDispatchAction)iter.next();
			action.update(javaSelection);
		}
		refactorSubmenu.removeAll();
		if (fillRefactorMenu(refactorSubmenu) == 0)
			refactorSubmenu.add(fNoActionAvailable);
	}
	
	private void refactorMenuHidden(IMenuManager manager) {
		ITextSelection textSelection= (ITextSelection)fEditor.getSelectionProvider().getSelection();
		for (Iterator iter= fActions.iterator(); iter.hasNext(); ) {
			SelectionDispatchAction action= (SelectionDispatchAction)iter.next();
			action.update(textSelection);
		}
	}
	
	private IJavaElement getEditorInput() {
		return JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(
			fEditor.getEditorInput());		
	}
	
	private IDocument getDocument() {
		return JavaPlugin.getDefault().getCompilationUnitDocumentProvider().
			getDocument(fEditor.getEditorInput());
	}
	
	private void fillQuickMenu(IMenuManager menu) {
		if (fEditor != null) {
			IJavaElement element= SelectionConverter.getInput(fEditor);
			if (element == null || !ActionUtil.isOnBuildPath(element)) {
				menu.add(fNoActionAvailable);
				return;
			}
			ITextSelection textSelection= (ITextSelection)fEditor.getSelectionProvider().getSelection();
			JavaTextSelection javaSelection= new JavaTextSelection(
				getEditorInput(), getDocument(), textSelection.getOffset(), textSelection.getLength());
			
			for (Iterator iter= fActions.iterator(); iter.hasNext(); ) {
				((SelectionDispatchAction)iter.next()).update(javaSelection);
			}
			fillRefactorMenu(menu);
			for (Iterator iter= fActions.iterator(); iter.hasNext(); ) {
				((SelectionDispatchAction)iter.next()).update(textSelection);
			}
			
		} else {
			ISelection selection= fSite.getSelectionProvider().getSelection();
			for (Iterator iter= fActions.iterator(); iter.hasNext(); ) {
				((SelectionDispatchAction)iter.next()).update(selection);
			}
			fillRefactorMenu(menu);
		}
	}
}
