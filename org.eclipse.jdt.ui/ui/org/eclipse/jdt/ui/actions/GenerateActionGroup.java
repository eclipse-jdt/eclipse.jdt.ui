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
package org.eclipse.jdt.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.editors.text.ITextEditorHelpContextIds;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.AddBookmarkAction;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.ConvertLineDelimitersAction;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.AddTaskAction;
import org.eclipse.jdt.internal.ui.javaeditor.AddImportOnSelectionAction;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

/**
 * Action group that adds the source and generate actions to a part's context
 * menu and installs handlers for the corresponding global menu actions.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class GenerateActionGroup extends ActionGroup {
	
	private CompilationUnitEditor fEditor;
	private IWorkbenchSite fSite;
	private String fGroupName= IContextMenuConstants.GROUP_REORGANIZE;
	private List fRegisteredSelectionListeners;
	
	private AddImportOnSelectionAction fAddImport;
	private OverrideMethodsAction fOverrideMethods;
	private AddGetterSetterAction fAddGetterSetter;
	private AddDelegateMethodsAction fAddDelegateMethods;
	private AddUnimplementedConstructorsAction fAddUnimplementedConstructors;
	private GenerateNewConstructorUsingFieldsAction fGenerateConstructorUsingFields;
	private AddJavaDocStubAction fAddJavaDocStub;
	private AddBookmarkAction fAddBookmark;
	private AddTaskAction fAddTaskAction;
	private ExternalizeStringsAction fExternalizeStrings;
	private FindStringsToExternalizeAction fFindStringsToExternalize;
	private SurroundWithTryCatchAction fSurroundWithTryCatch;
	private AddToClasspathAction fAddToClasspathAction;
	private RemoveFromClasspathAction fRemoveFromClasspathAction;
	
	private OrganizeImportsAction fOrganizeImports;
	private SortMembersAction fSortMembers;

	private ConvertLineDelimitersAction fConvertToWindows;
	private ConvertLineDelimitersAction fConvertToUNIX;
	private ConvertLineDelimitersAction fConvertToMac;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public GenerateActionGroup(CompilationUnitEditor editor, String groupName) {
		fSite= editor.getSite();
		fEditor= editor;
		fGroupName= groupName;
				
		ISelectionProvider provider= fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();
	
		fAddImport= new AddImportOnSelectionAction(editor);
		fAddImport.setActionDefinitionId(IJavaEditorActionDefinitionIds.ADD_IMPORT);
		fAddImport.update();
		editor.setAction("AddImport", fAddImport); //$NON-NLS-1$
		
		fOrganizeImports= new OrganizeImportsAction(editor);
		fOrganizeImports.setActionDefinitionId(IJavaEditorActionDefinitionIds.ORGANIZE_IMPORTS);
		editor.setAction("OrganizeImports", fOrganizeImports); //$NON-NLS-1$

		fSortMembers= new SortMembersAction(editor);
		fSortMembers.setActionDefinitionId(IJavaEditorActionDefinitionIds.SORT_MEMBERS);
		editor.setAction("SortMembers", fSortMembers); //$NON-NLS-1$

		fOverrideMethods= new OverrideMethodsAction(editor);
		fOverrideMethods.setActionDefinitionId(IJavaEditorActionDefinitionIds.OVERRIDE_METHODS);
		editor.setAction("OverrideMethods", fOverrideMethods); //$NON-NLS-1$
		
		fAddGetterSetter= new AddGetterSetterAction(editor);
		fAddGetterSetter.setActionDefinitionId(IJavaEditorActionDefinitionIds.CREATE_GETTER_SETTER);
		editor.setAction("AddGetterSetter", fAddGetterSetter); //$NON-NLS-1$

		fAddDelegateMethods= new AddDelegateMethodsAction(editor);
		fAddDelegateMethods.setActionDefinitionId(IJavaEditorActionDefinitionIds.CREATE_DELEGATE_METHODS);
		editor.setAction("AddDelegateMethods", fAddDelegateMethods); //$NON-NLS-1$
			
		fAddUnimplementedConstructors= new AddUnimplementedConstructorsAction(editor);
		fAddUnimplementedConstructors.setActionDefinitionId(IJavaEditorActionDefinitionIds.ADD_UNIMPLEMENTED_CONTRUCTORS);
		editor.setAction("AddUnimplementedConstructors", fAddUnimplementedConstructors); //$NON-NLS-1$		

		fGenerateConstructorUsingFields= new GenerateNewConstructorUsingFieldsAction(editor);
		fGenerateConstructorUsingFields.setActionDefinitionId(IJavaEditorActionDefinitionIds.GENERATE_CONSTRUCTOR_USING_FIELDS);
		editor.setAction("GenerateConstructorUsingFields", fGenerateConstructorUsingFields); //$NON-NLS-1$		

		fAddJavaDocStub= new AddJavaDocStubAction(editor);
		fAddJavaDocStub.setActionDefinitionId(IJavaEditorActionDefinitionIds.ADD_JAVADOC_COMMENT);
		editor.setAction("AddJavadocComment", fAddJavaDocStub); //$NON-NLS-1$		

		
		fSurroundWithTryCatch= new SurroundWithTryCatchAction(editor);
		fSurroundWithTryCatch.setActionDefinitionId(IJavaEditorActionDefinitionIds.SURROUND_WITH_TRY_CATCH);
		fSurroundWithTryCatch.update(selection);
		provider.addSelectionChangedListener(fSurroundWithTryCatch);
		editor.setAction("SurroundWithTryCatch", fSurroundWithTryCatch); //$NON-NLS-1$		
		
		fExternalizeStrings= new ExternalizeStringsAction(editor);
		fExternalizeStrings.setActionDefinitionId(IJavaEditorActionDefinitionIds.EXTERNALIZE_STRINGS);
		editor.setAction("ExternalizeStrings", fExternalizeStrings); //$NON-NLS-1$		
		
		fConvertToWindows= new ConvertLineDelimitersAction(editor, "\r\n"); //$NON-NLS-1$
		fConvertToWindows.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONVERT_LINE_DELIMITERS_TO_WINDOWS);
		fConvertToWindows.setHelpContextId(ITextEditorHelpContextIds.CONVERT_LINE_DELIMITERS_TO_WINDOWS);
		editor.setAction("ConvertLineDelimitersToWindows", fConvertToWindows); //$NON-NLS-1$		
		
		fConvertToUNIX= new ConvertLineDelimitersAction(editor, "\n"); //$NON-NLS-1$
		fConvertToUNIX.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONVERT_LINE_DELIMITERS_TO_UNIX);
		fConvertToUNIX.setHelpContextId(ITextEditorHelpContextIds.CONVERT_LINE_DELIMITERS_TO_UNIX);
		editor.setAction("ConvertLineDelimitersToUNIX", fConvertToUNIX); //$NON-NLS-1$		
	
		fConvertToMac= new ConvertLineDelimitersAction(editor, "\r"); //$NON-NLS-1$
		fConvertToMac.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONVERT_LINE_DELIMITERS_TO_MAC);
		fConvertToMac.setHelpContextId(ITextEditorHelpContextIds.CONVERT_LINE_DELIMITERS_TO_MAC);
		editor.setAction("ConvertLineDelimitersToMac", fConvertToMac); //$NON-NLS-1$
	}
	
	/**
	 * Creates a new <code>GenerateActionGroup</code>. The group 
	 * requires that the selection provided by the page's selection provider 
	 * is of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public GenerateActionGroup(Page page) {
		this(page.getSite());
	}

	/**
	 * Creates a new <code>GenerateActionGroup</code>. The group 
	 * requires that the selection provided by the part's selection provider 
	 * is of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public GenerateActionGroup(IViewPart part) {
		this(part.getSite());
	}
	
	private GenerateActionGroup(IWorkbenchSite site) {
		fSite= site;
		ISelectionProvider provider= fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();
		
		fOverrideMethods= new OverrideMethodsAction(site);
		fAddGetterSetter= new AddGetterSetterAction(site);
		fAddDelegateMethods= new AddDelegateMethodsAction(site);
		fAddUnimplementedConstructors= new AddUnimplementedConstructorsAction(site);
		fGenerateConstructorUsingFields= new GenerateNewConstructorUsingFieldsAction(site);
		fAddJavaDocStub= new AddJavaDocStubAction(site);
		fAddBookmark= new AddBookmarkAction(site.getShell());
		fAddToClasspathAction= new AddToClasspathAction(site);
		fRemoveFromClasspathAction= new RemoveFromClasspathAction(site);
		fAddTaskAction= new AddTaskAction(site);
		fExternalizeStrings= new ExternalizeStringsAction(site);
		fFindStringsToExternalize= new FindStringsToExternalizeAction(site);
		fOrganizeImports= new OrganizeImportsAction(site);
		fSortMembers= new SortMembersAction(site);
		
		fOverrideMethods.update(selection);
		fAddGetterSetter.update(selection);
		fAddDelegateMethods.update(selection);
		fAddUnimplementedConstructors.update(selection);	
		fGenerateConstructorUsingFields.update(selection);
		fAddJavaDocStub.update(selection);
		fAddToClasspathAction.update(selection);
		fRemoveFromClasspathAction.update(selection);
		fExternalizeStrings.update(selection);
		fFindStringsToExternalize.update(selection);
		fAddTaskAction.update(selection);
		fOrganizeImports.update(selection);
		fSortMembers.update(selection);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection)selection;
			fAddBookmark.selectionChanged(ss);
		} else {
			fAddBookmark.setEnabled(false);
		}
		
		registerSelectionListener(provider, fOverrideMethods);
		registerSelectionListener(provider, fAddGetterSetter);
		registerSelectionListener(provider, fAddDelegateMethods);
		registerSelectionListener(provider, fAddUnimplementedConstructors);
		registerSelectionListener(provider, fGenerateConstructorUsingFields);
		registerSelectionListener(provider, fAddJavaDocStub);
		registerSelectionListener(provider, fAddBookmark);
		registerSelectionListener(provider, fAddToClasspathAction);
		registerSelectionListener(provider, fRemoveFromClasspathAction);
		registerSelectionListener(provider, fExternalizeStrings);
		registerSelectionListener(provider, fFindStringsToExternalize);
		registerSelectionListener(provider, fOrganizeImports);
		registerSelectionListener(provider, fSortMembers);
		registerSelectionListener(provider, fAddTaskAction);
	}
	
	private void registerSelectionListener(ISelectionProvider provider, ISelectionChangedListener listener) {
		if (fRegisteredSelectionListeners == null)
			fRegisteredSelectionListeners= new ArrayList(20);
		provider.addSelectionChangedListener(listener);
		fRegisteredSelectionListeners.add(listener);
	}
	
	/*
	 * The state of the editor owning this action group has changed. 
	 * This method does nothing if the group's owner isn't an
	 * editor.
	 */
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 */
	public void editorStateChanged() {
		Assert.isTrue(isEditorOwner());
		
		// http://dev.eclipse.org/bugs/show_bug.cgi?id=17709
		fConvertToMac.update();
		fConvertToUNIX.update();
		fConvertToWindows.update();
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		setGlobalActionHandlers(actionBar);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		IMenuManager subMenu= null;
		if (isEditorOwner()) {
			subMenu= createEditorSubMenu(menu);
		} else {
			subMenu= createViewSubMenu(menu);
		}
		if (subMenu != null)
			menu.appendToGroup(fGroupName, subMenu);
	}
	
	private IMenuManager createEditorSubMenu(IMenuManager mainMenu) {
		IMenuManager result= new MenuManager(ActionMessages.getString("SourceMenu.label")); //$NON-NLS-1$
		int added= 0;
		added+= addEditorAction(result, "ToggleComment"); //$NON-NLS-1$
		added+= addEditorAction(result, "AddBlockComment"); //$NON-NLS-1$
		added+= addEditorAction(result, "RemoveBlockComment"); //$NON-NLS-1$
		added+= addEditorAction(result, "Format"); //$NON-NLS-1$
		added+= addEditorAction(result, "Indent"); //$NON-NLS-1$
		result.add(new Separator());
		added+= addAction(result, fOrganizeImports);
		added+= addAction(result, fAddImport);
		result.add(new Separator());
		added+= addAction(result, fOverrideMethods);
		added+= addAction(result, fAddGetterSetter);
		added+= addAction(result, fAddDelegateMethods);
		added+= addAction(result, fAddUnimplementedConstructors);
		added+= addAction(result, fGenerateConstructorUsingFields);
		added+= addAction(result, fAddJavaDocStub);
		result.add(new Separator());		
		added+= addAction(result, fSurroundWithTryCatch);
		if (added == 0)
			result= null;
		return result;
	}

	private IMenuManager createViewSubMenu(IMenuManager mainMenu) {
		IMenuManager result= new MenuManager(ActionMessages.getString("SourceMenu.label")); //$NON-NLS-1$
		int added= 0;
		added+= addAction(result, fSortMembers);
		result.add(new Separator());
		added+= addAction(result, fOrganizeImports);
		added+= addAction(result, fAddImport);
		result.add(new Separator());
		added+= addAction(result, fOverrideMethods);
		added+= addAction(result, fAddGetterSetter);
		added+= addAction(result, fAddDelegateMethods);
		added+= addAction(result, fAddUnimplementedConstructors);
		added+= addAction(result, fGenerateConstructorUsingFields);
		added+= addAction(result, fAddJavaDocStub);
		added+= addAction(result, fAddToClasspathAction);
		added+= addAction(result, fRemoveFromClasspathAction);
		result.add(new Separator());		
		added+= addAction(result, fSurroundWithTryCatch);
		added+= addAction(result, fExternalizeStrings);
		added+= addAction(result, fFindStringsToExternalize);
		if (added == 0)
			result= null;
		return result;
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void dispose() {
		if (fRegisteredSelectionListeners != null) {
			ISelectionProvider provider= fSite.getSelectionProvider();
			for (Iterator iter= fRegisteredSelectionListeners.iterator(); iter.hasNext();) {
				ISelectionChangedListener listener= (ISelectionChangedListener) iter.next();
				provider.removeSelectionChangedListener(listener);
			}
		}
		fEditor= null;
		super.dispose();
	}
	
	private void setGlobalActionHandlers(IActionBars actionBar) {
		actionBar.setGlobalActionHandler(JdtActionConstants.ADD_IMPORT, fAddImport);
		actionBar.setGlobalActionHandler(JdtActionConstants.SURROUND_WITH_TRY_CATCH, fSurroundWithTryCatch);
		actionBar.setGlobalActionHandler(JdtActionConstants.OVERRIDE_METHODS, fOverrideMethods);
		actionBar.setGlobalActionHandler(JdtActionConstants.GENERATE_GETTER_SETTER, fAddGetterSetter);
		actionBar.setGlobalActionHandler(JdtActionConstants.GENERATE_DELEGATE_METHODS, fAddDelegateMethods);
		actionBar.setGlobalActionHandler(JdtActionConstants.ADD_CONSTRUCTOR_FROM_SUPERCLASS, fAddUnimplementedConstructors);		
		actionBar.setGlobalActionHandler(JdtActionConstants.GENERATE_CONSTRUCTOR_USING_FIELDS, fGenerateConstructorUsingFields);
		actionBar.setGlobalActionHandler(JdtActionConstants.ADD_JAVA_DOC_COMMENT, fAddJavaDocStub);
		actionBar.setGlobalActionHandler(JdtActionConstants.EXTERNALIZE_STRINGS, fExternalizeStrings);
		actionBar.setGlobalActionHandler(JdtActionConstants.FIND_STRINGS_TO_EXTERNALIZE, fFindStringsToExternalize);
		actionBar.setGlobalActionHandler(JdtActionConstants.ORGANIZE_IMPORTS, fOrganizeImports);
		actionBar.setGlobalActionHandler(JdtActionConstants.SORT_MEMBERS, fSortMembers);
		actionBar.setGlobalActionHandler(JdtActionConstants.CONVERT_LINE_DELIMITERS_TO_WINDOWS, fConvertToWindows);
		actionBar.setGlobalActionHandler(JdtActionConstants.CONVERT_LINE_DELIMITERS_TO_UNIX, fConvertToUNIX);
		actionBar.setGlobalActionHandler(JdtActionConstants.CONVERT_LINE_DELIMITERS_TO_MAC, fConvertToMac);
		if (!isEditorOwner()) {
			// editor provides its own implementation of these actions.
			actionBar.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), fAddBookmark);
			actionBar.setGlobalActionHandler(IDEActionFactory.ADD_TASK.getId(), fAddTaskAction);
		}
	}
	
	private int addAction(IMenuManager menu, IAction action) {
		if (action != null && action.isEnabled()) {
			menu.add(action);
			return 1;
		}
		return 0;
	}	
	
	private int addEditorAction(IMenuManager menu, String actionID) {
		if (fEditor == null)
			return 0;
		IAction action= fEditor.getAction(actionID);
		if (action == null)
			return 0;
		if (action instanceof IUpdate)
			((IUpdate)action).update();
		if (action.isEnabled()) {
			menu.add(action);
			return 1;
		}
		return 0;
	}
	
	private boolean isEditorOwner() {
		return fEditor != null;
	}	
}
