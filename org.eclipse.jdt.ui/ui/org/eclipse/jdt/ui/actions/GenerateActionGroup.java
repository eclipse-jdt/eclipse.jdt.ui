/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.AddBookmarkAction;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.ConvertLineDelimitersAction;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.AddImportOnSelectionAction;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

/**
 * Action group that adds the source and generate actions to a context menu and
 * action bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class GenerateActionGroup extends ActionGroup {
	
	private boolean fEditorIsOwner;
	private IWorkbenchSite fSite;
	private String fGroupName= IContextMenuConstants.GROUP_SOURCE;
	
	private AddImportOnSelectionAction fAddImport;
	private OverrideMethodsAction fOverrideMethods;
	private AddGetterSetterAction fAddGetterSetter;
	private AddUnimplementedConstructorsAction fAddUnimplementedConstructors;
	private AddJavaDocStubAction fAddJavaDocStub;
	private AddBookmarkAction fAddBookmark;
	private ExternalizeStringsAction fExternalizeStrings;
	private FindStringsToExternalizeAction fFindStringsToExternalize;
	private SurroundWithTryCatchAction fSurroundWithTryCatch;
	
	private OrganizeImportsAction fOrganizeImports;

	private ConvertLineDelimitersAction fConvertToWindows;
	private ConvertLineDelimitersAction fConvertToUNIX;
	private ConvertLineDelimitersAction fConvertToMac;
	
	/**
	 * Creates a new <code>GenerateActionGroup</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public GenerateActionGroup(CompilationUnitEditor editor, String groupName) {
		fSite= editor.getSite();
		fEditorIsOwner= true;
		fGroupName= groupName;
		ISelectionProvider provider= fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();

		fAddImport= new AddImportOnSelectionAction(editor);
		fAddImport.update();
		
		fOrganizeImports= new OrganizeImportsAction(editor);
		fOrganizeImports.editorStateChanged();

		fOverrideMethods= new OverrideMethodsAction(editor);
		fOverrideMethods.editorStateChanged();
		
		fAddUnimplementedConstructors= new AddUnimplementedConstructorsAction(editor);
		fAddUnimplementedConstructors.editorStateChanged();
		
		fAddJavaDocStub= new AddJavaDocStubAction(editor);
		fAddJavaDocStub.editorStateChanged();
		
		fSurroundWithTryCatch= new SurroundWithTryCatchAction(editor);
		fSurroundWithTryCatch.update(selection);
		provider.addSelectionChangedListener(fSurroundWithTryCatch);
		
		fExternalizeStrings= new ExternalizeStringsAction(editor);
		fExternalizeStrings.editorStateChanged();
		
		fConvertToWindows= new ConvertLineDelimitersAction(editor, "\r\n"); //$NON-NLS-1$
		fConvertToUNIX= new ConvertLineDelimitersAction(editor, "\n"); //$NON-NLS-1$
		fConvertToMac= new ConvertLineDelimitersAction(editor, "\r"); //$NON-NLS-1$
	}
	
	/**
	 * Creates a new <code>GenerateActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public GenerateActionGroup(Page page) {
		this(page.getSite());
	}

	/**
	 * Creates a new <code>GenerateActionGroup</code>.
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
		fAddUnimplementedConstructors= new AddUnimplementedConstructorsAction(site);
		fAddJavaDocStub= new AddJavaDocStubAction(site);
		fAddBookmark= new AddBookmarkAction(site.getShell());
		fExternalizeStrings= new ExternalizeStringsAction(site);
		fFindStringsToExternalize= new FindStringsToExternalizeAction(site);
		fOrganizeImports= new OrganizeImportsAction(site);
		
		fOverrideMethods.update(selection);
		fAddGetterSetter.update(selection);
		fAddUnimplementedConstructors.update(selection);	
		fAddJavaDocStub.update(selection);
		fExternalizeStrings.update(selection);
		fFindStringsToExternalize.update(selection);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection)selection;
			fAddBookmark.selectionChanged(ss);
		} else {
			fAddBookmark.setEnabled(false);
		}
		fOrganizeImports.update(selection);
		
		provider.addSelectionChangedListener(fOverrideMethods);
		provider.addSelectionChangedListener(fAddGetterSetter);
		provider.addSelectionChangedListener(fAddUnimplementedConstructors);
		provider.addSelectionChangedListener(fAddJavaDocStub);
		provider.addSelectionChangedListener(fAddBookmark);
		provider.addSelectionChangedListener(fExternalizeStrings);
		provider.addSelectionChangedListener(fFindStringsToExternalize);
		provider.addSelectionChangedListener(fOrganizeImports);
	}
	
	/**
	 * The state of the editor owning this action group has changed. 
	 * This method does nothing if the group's owner isn't an
	 * editor.
	 * <p>
	 * Note: This method is for internal use only. Clients should not call this method.
	 * </p>
	 */
	public void editorStateChanged() {
		Assert.isTrue(fEditorIsOwner);
		fAddImport.update();
		fExternalizeStrings.editorStateChanged();
		fOrganizeImports.editorStateChanged();
		fOverrideMethods.editorStateChanged();
		fAddUnimplementedConstructors.editorStateChanged();
		fAddJavaDocStub.editorStateChanged();
		fSurroundWithTryCatch.editorStateChanged();
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
		IMenuManager target= menu;
		IMenuManager generateMenu= null;
		if (fEditorIsOwner) {
			generateMenu= new MenuManager(ActionMessages.getString("SourceMenu.label"));
			generateMenu.add(new GroupMarker(fGroupName));
			target= generateMenu;
		}
		int added= 0;
		added+= appendToGroup(target, fAddImport);
		added+= appendToGroup(target, fOrganizeImports);
		added+= appendToGroup(target, fSurroundWithTryCatch);
		added+= appendToGroup(target, fOverrideMethods);
		added+= appendToGroup(target, fAddGetterSetter);
		added+= appendToGroup(target, fAddUnimplementedConstructors);
		added+= appendToGroup(target, fAddJavaDocStub);
		added+= appendToGroup(target, fAddBookmark);
		added+= appendToGroup(target, fExternalizeStrings);
		if (generateMenu != null && added > 0)
			menu.appendToGroup(fGroupName, generateMenu);
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void dispose() {
		ISelectionProvider provider= fSite.getSelectionProvider();
		disposeAction(fOverrideMethods, provider);
		disposeAction(fAddGetterSetter, provider);
		disposeAction(fAddUnimplementedConstructors, provider);
		disposeAction(fAddJavaDocStub, provider);
		disposeAction(fAddBookmark, provider);
		disposeAction(fExternalizeStrings, provider);
		disposeAction(fFindStringsToExternalize, provider);
		disposeAction(fOrganizeImports, provider);
		disposeAction(fSurroundWithTryCatch, provider);
		super.dispose();
	}
	
	private void setGlobalActionHandlers(IActionBars actionBar) {
		actionBar.setGlobalActionHandler(JdtActionConstants.ADD_IMPORT, fAddImport);
		actionBar.setGlobalActionHandler(JdtActionConstants.SURROUND_WITH_TRY_CATCH, fSurroundWithTryCatch);
		actionBar.setGlobalActionHandler(JdtActionConstants.OVERRIDE_METHODS, fOverrideMethods);
		actionBar.setGlobalActionHandler(JdtActionConstants.GENERATE_GETTER_SETTER, fAddGetterSetter);
		actionBar.setGlobalActionHandler(JdtActionConstants.ADD_CONSTRUCTOR_FROM_SUPERCLASS, fAddUnimplementedConstructors);
		actionBar.setGlobalActionHandler(JdtActionConstants.ADD_JAVA_DOC_COMMENT, fAddJavaDocStub);
		actionBar.setGlobalActionHandler(IWorkbenchActionConstants.BOOKMARK, fAddBookmark);
		actionBar.setGlobalActionHandler(JdtActionConstants.EXTERNALIZE_STRINGS, fExternalizeStrings);
		actionBar.setGlobalActionHandler(JdtActionConstants.FIND_STRINGS_TO_EXTERNALIZE, fFindStringsToExternalize);
		actionBar.setGlobalActionHandler(JdtActionConstants.ORGANIZE_IMPORTS, fOrganizeImports);
		actionBar.setGlobalActionHandler(JdtActionConstants.CONVERT_LINE_DELIMITERS_TO_WINDOWS, fConvertToWindows);
		actionBar.setGlobalActionHandler(JdtActionConstants.CONVERT_LINE_DELIMITERS_TO_UNIX, fConvertToUNIX);
		actionBar.setGlobalActionHandler(JdtActionConstants.CONVERT_LINE_DELIMITERS_TO_MAC, fConvertToMac);
	}
	
	private int appendToGroup(IMenuManager menu, IAction action) {
		if (action != null && action.isEnabled()) {
			menu.appendToGroup(fGroupName, action);
			return 1;
		}
		return 0;
	}	

	private void addAction(IMenuManager menu, IAction action) {
		if (action != null && action.isEnabled())
			menu.add(action);
	}	
	
	private void disposeAction(ISelectionChangedListener action, ISelectionProvider provider) {
		if (action != null)
			provider.removeSelectionChangedListener(action);
	}	
}
