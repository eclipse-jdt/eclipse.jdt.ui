/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.internal.corext.buildpath.AddToClasspathOperation;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifierOperation;
import org.eclipse.jdt.internal.corext.buildpath.EditFiltersOperation;
import org.eclipse.jdt.internal.corext.buildpath.EditOutputFolderOperation;
import org.eclipse.jdt.internal.corext.buildpath.ExcludeOperation;
import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.LinkedSourceFolderOperation;
import org.eclipse.jdt.internal.corext.buildpath.RemoveFromClasspathOperation;
import org.eclipse.jdt.internal.corext.buildpath.UnexcludeOperation;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * Action group that adds the source and generate actions to a part's context
 * menu and installs handlers for the corresponding global menu actions.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.1
 */
public class GenerateBuildPathActionGroup extends ActionGroup {
    /**
     * Pop-up menu: id of the source sub menu (value <code>org.eclipse.jdt.ui.buildpath.menu</code>).
     * 
     * @since 3.1
     */
    public static final String MENU_ID= "org.eclipse.jdt.ui.buildpath.menu"; //$NON-NLS-1$
    
    /**
     * Pop-up menu: id of the buildpath (add /remove) group of the buildpath sub menu (value
     * <code>buildpathGroup</code>).
     * 
     * @since 3.1
     */
    public static final String GROUP_BUILDPATH= "buildpathGroup";  //$NON-NLS-1$
    
    /**
     * Pop-up menu: id of the filter (include / exclude) group of the buildpath sub menu (value
     * <code>filterGroup</code>).
     * 
     * @since 3.1
     */
    public static final String GROUP_FILTER= "filterGroup";  //$NON-NLS-1$
    
    /**
     * Pop-up menu: id of the customize (filters / output folder) group of the buildpath sub menu (value
     * <code>customizeGroup</code>).
     * 
     * @since 3.1
     */
    public static final String GROUP_CUSTOMIZE= "customizeGroup";  //$NON-NLS-1$
    
    private CompilationUnitEditor fEditor;
    private IWorkbenchSite fSite;
    private String fGroupName= IContextMenuConstants.GROUP_REORGANIZE;
    private List fRegisteredSelectionListeners;
    
    private BuildPathAction[] fActions;
    
    /**
     * Note: This constructor is for internal use only. Clients should not call this constructor.
     */
    public GenerateBuildPathActionGroup(CompilationUnitEditor editor, String groupName) {
        fSite= editor.getSite();
        fEditor= editor;
        fGroupName= groupName;
    }
    
    /**
     * Creates a new <code>GenerateActionGroup</code>. The group 
     * requires that the selection provided by the page's selection provider 
     * is of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
     * 
     * @param page the page that owns this action group
     */
    public GenerateBuildPathActionGroup(Page page) {
        this(page.getSite(), null);
    }
    
    /**
     * Creates a new <code>GenerateActionGroup</code>. The group 
     * requires that the selection provided by the part's selection provider 
     * is of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
     * 
     * @param part the view part that owns this action group
     */
    public GenerateBuildPathActionGroup(IViewPart part) {
        this(part.getSite(), part.getSite().getKeyBindingService());
    }
    
    private GenerateBuildPathActionGroup(IWorkbenchSite site, IKeyBindingService keyBindingService) {
        fSite= site;
        ISelectionProvider provider= fSite.getSelectionProvider();
        ISelection selection= provider.getSelection();
        
        fActions= new BuildPathAction[7];

        fActions[0]= createBuildPathAction(site, IClasspathInformationProvider.CREATE_LINK);
        fActions[1]= createBuildPathAction(site, IClasspathInformationProvider.ADD_TO_BP);
        fActions[2]= createBuildPathAction(site, IClasspathInformationProvider.REMOVE_FROM_BP);
        fActions[3]= createBuildPathAction(site, IClasspathInformationProvider.EXCLUDE);
        fActions[4]= createBuildPathAction(site, IClasspathInformationProvider.UNEXCLUDE);
        fActions[5]= createBuildPathAction(site, IClasspathInformationProvider.EDIT_FILTERS);
        fActions[6]= createBuildPathAction(site, IClasspathInformationProvider.EDIT_OUTPUT);
        
        for(int i= 0; i < fActions.length; i++) {
            fActions[i].update(selection);
        }
        
        for(int i= 0; i < fActions.length; i++) {
            registerSelectionListener(provider, fActions[i]);
        }
    }
    
    /**
     * Creates a <code>BuildPathAction</code>.
     * 
     * @param site the site providing context information for this action
     * @param type the type of the operation, must be a constant of <code>
     * IClasspathInformationProvider</code>
     * @return a BuildPathAction for the operation of the corresponding type
     * 
     * @see IClasspathInformationProvider
     */
    private BuildPathAction createBuildPathAction(IWorkbenchSite site, int type) {
        ImageDescriptor imageDescriptor= null, disabledImageDescriptor= null; 
        String text= null, tooltip= null;
        ClasspathModifierOperation operation= null;
        BuildPathAction action= new BuildPathAction(site);
        switch(type) {
            case IClasspathInformationProvider.CREATE_LINK: {
                imageDescriptor= JavaPluginImages.DESC_TOOL_NEWPACKROOT;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_NEWPACKROOT;
                text= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Link.label"); //$NON-NLS-1$
                tooltip= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Link.tooltip"); //$NON-NLS-1$
                operation= new LinkedSourceFolderOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.ADD_TO_BP: {
                imageDescriptor= JavaPluginImages.DESC_ELCL_ADD_TO_BP;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_ADD_TO_BP;
                text= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.AddToCP.label"); //$NON-NLS-1$
                tooltip= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.AddToCP.tooltip"); //$NON-NLS-1$
                operation= new AddToClasspathOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.REMOVE_FROM_BP: {
                imageDescriptor= JavaPluginImages.DESC_ELCL_REMOVE_FROM_BP;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_REMOVE_FROM_BP;
                text= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.RemoveFromCP.label"); //$NON-NLS-1$
                tooltip= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.RemoveFromCP.tooltip"); //$NON-NLS-1$
                operation= new RemoveFromClasspathOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.EXCLUDE: {
                imageDescriptor= JavaPluginImages.DESC_ELCL_EXCLUSION;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_EXCLUSION;
                text= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Exclude.label"); //$NON-NLS-1$
                tooltip= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Exclude.tooltip"); //$NON-NLS-1$
                operation= new ExcludeOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.UNEXCLUDE: {
                imageDescriptor= JavaPluginImages.DESC_ELCL_INCLUSION;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_INCLUSION;
                text= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Unexclude.label"); //$NON-NLS-1$
                tooltip= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Unexclude.tooltip"); //$NON-NLS-1$
                operation= new UnexcludeOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.EDIT_FILTERS: {
                imageDescriptor= JavaPluginImages.DESC_OBJS_TEXT_EDIT;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_TEXT_EDIT;
                text= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Edit.label"); //$NON-NLS-1$
                tooltip= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Edit.tooltip"); //$NON-NLS-1$
                operation= new EditFiltersOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.EDIT_OUTPUT: {
                imageDescriptor= JavaPluginImages.DESC_OBJS_OUTPUT_FOLDER_ATTRIB;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_OUTPUT_FOLDER_ATTRIB;
                text= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.EditOutput.label"); //$NON-NLS-1$
                tooltip= NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.EditOutput.tooltip"); //$NON-NLS-1$
                operation= new EditOutputFolderOperation(null, action);
                ((EditOutputFolderOperation)operation).showOutputFolders(true);
                break;
            }
            default: break;
        }
        action.initialize(operation, imageDescriptor, disabledImageDescriptor, text, tooltip);
        return action;
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
        String menuText= ActionMessages.getString("BuildPath.label"); //$NON-NLS-1$
        IMenuManager subMenu= new MenuManager(menuText, MENU_ID); 
        int added= 0;
        if (isEditorOwner()) {
            added= fillEditorSubMenu(subMenu);
        } else {
            added= fillViewSubMenu(subMenu);
        }
        if (added > 0)
            menu.appendToGroup(fGroupName, subMenu);
    }
    
    private int fillEditorSubMenu(IMenuManager source) {
        int added= 0;
        // TODO implement
        return added;
    }
    
    private int fillViewSubMenu(IMenuManager source) {
        int added= 0;
        for(int i= 0; i < fActions.length; i++) {
            if(i == 1)
                source.add(new Separator(GROUP_BUILDPATH));
            else if(i == 3)
                source.add(new Separator(GROUP_FILTER));
            else if (i == 5)
                source.add(new Separator(GROUP_CUSTOMIZE));
            added+= addAction(source, fActions[i]);
        }
        return added;
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
        // TODO implement
    }
    
    private int addAction(IMenuManager menu, IAction action) {
        if (action != null && action.isEnabled()) {
            menu.add(action);
            return 1;
        }
        return 0;
    }
    
    private boolean isEditorOwner() {
        return fEditor != null;
    }   
}
