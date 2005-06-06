/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.internal.corext.buildpath.AddExternalArchivesOperation;
import org.eclipse.jdt.internal.corext.buildpath.AddLibraryOperation;
import org.eclipse.jdt.internal.corext.buildpath.AddSelectedLibraryOperation;
import org.eclipse.jdt.internal.corext.buildpath.AddSelectedSourceFolderOperation;
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
    
	private static class NoActionAvailable extends Action {
		public NoActionAvailable() {
			setEnabled(true);
			setText(NewWizardMessages.GenerateBuildPathActionGroup_no_action_available); 
		}
	}
	private Action fNoActionAvailable= new NoActionAvailable(); 
    
    private IWorkbenchSite fSite;
    private Action[] fActions;

	private String fGroupName= IContextMenuConstants.GROUP_REORGANIZE;
        
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
    }

	private Action[] getActions() {
		if (fActions == null) {
			final BuildActionSelectionContext context= new BuildActionSelectionContext();
			final Action[] actions= new Action[] {
					createBuildPathAction(fSite, IClasspathInformationProvider.CREATE_LINK, context),
					createBuildPathAction(fSite, IClasspathInformationProvider.ADD_SEL_SF_TO_BP, context),
					createBuildPathAction(fSite, IClasspathInformationProvider.ADD_SEL_LIB_TO_BP, context),
					createBuildPathAction(fSite, IClasspathInformationProvider.REMOVE_FROM_BP, context),
					createBuildPathAction(fSite, IClasspathInformationProvider.ADD_JAR_TO_BP, context),
					createBuildPathAction(fSite, IClasspathInformationProvider.ADD_LIB_TO_BP, context),
					createBuildPathAction(fSite, IClasspathInformationProvider.EXCLUDE, context),
					createBuildPathAction(fSite, IClasspathInformationProvider.UNEXCLUDE, context),
					createBuildPathAction(fSite, IClasspathInformationProvider.EDIT_FILTERS, context),
					createBuildPathAction(fSite, IClasspathInformationProvider.EDIT_OUTPUT, context),
					createConfigureAction(fSite)
			};
			fActions= actions;
		}
		final Action[] actions= fActions;
		
		for (int i= 0; i < actions.length; i++) {
			((IUpdate) actions[i]).update();
        }
		return actions;
	}
    
	private Action createConfigureAction(IWorkbenchSite site) {
		ConfigureBuildPathAction action= new ConfigureBuildPathAction(site);
		action.setImageDescriptor(JavaPluginImages.DESC_ELCL_CONFIGURE_BUILDPATH);
		action.setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CONFIGURE_BUILDPATH);
		action.setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ConfigureBP_label); 
		action.setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_ConfigureBP_tooltip); 
		return action;
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
    private BuildPathAction createBuildPathAction(IWorkbenchSite site, int type, BuildActionSelectionContext context) {
        ImageDescriptor imageDescriptor= null, disabledImageDescriptor= null; 
        String text= null, tooltip= null;
        ClasspathModifierOperation operation= null;
		
        BuildPathAction action= new BuildPathAction(site, context);
        switch(type) {
            case IClasspathInformationProvider.CREATE_LINK: {
                imageDescriptor= JavaPluginImages.DESC_ELCL_ADD_LINKED_SOURCE_TO_BUILDPATH;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_ADD_LINKED_SOURCE_TO_BUILDPATH;
                text= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Link_label; 
                tooltip= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Link_tooltip; 
                operation= new LinkedSourceFolderOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.ADD_SEL_SF_TO_BP: {
                imageDescriptor= JavaPluginImages.DESC_OBJS_PACKFRAG_ROOT;
                text= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelSFToCP_label; 
                tooltip= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelSFToCP_tooltip; 
                operation= new AddSelectedSourceFolderOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.ADD_SEL_LIB_TO_BP: {
                imageDescriptor= JavaPluginImages.DESC_OBJS_EXTJAR;
                // TODO add disabled icon
                text= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelLibToCP_label; 
                tooltip= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelLibToCP_tooltip; 
                operation= new AddSelectedLibraryOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.REMOVE_FROM_BP: {
                imageDescriptor= JavaPluginImages.DESC_ELCL_REMOVE_FROM_BP;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_REMOVE_FROM_BP;
                text= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_RemoveFromCP_label; 
                tooltip= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_RemoveFromCP_tooltip; 
                operation= new RemoveFromClasspathOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.EXCLUDE: {
                imageDescriptor= JavaPluginImages.DESC_ELCL_EXCLUDE_FROM_BUILDPATH;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_EXCLUDE_FROM_BUILDPATH;
                text= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Exclude_label; 
                tooltip= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Exclude_tooltip; 
                operation= new ExcludeOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.UNEXCLUDE: {
                imageDescriptor= JavaPluginImages.DESC_ELCL_INCLUDE_ON_BUILDPATH;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_INCLUDE_ON_BUILDPATH;
                text= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Unexclude_label; 
                tooltip= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Unexclude_tooltip; 
                operation= new UnexcludeOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.EDIT_FILTERS: {
                imageDescriptor= JavaPluginImages.DESC_ELCL_CONFIGURE_BUILDPATH_FILTERS;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_CONFIGURE_BUILDPATH_FILTERS;
                text= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Edit_label; 
                tooltip= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Edit_tooltip; 
                operation= new EditFiltersOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.EDIT_OUTPUT: {
                imageDescriptor= JavaPluginImages.DESC_ELCL_CONFIGURE_OUTPUT_FOLDER;
                disabledImageDescriptor= JavaPluginImages.DESC_DLCL_CONFIGURE_OUTPUT_FOLDER;
                text= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_EditOutput_label; 
                tooltip= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_EditOutput_tooltip; 
                operation= new EditOutputFolderOperation(null, action);
                ((EditOutputFolderOperation)operation).showOutputFolders(true);
                break;
            }
            case IClasspathInformationProvider.ADD_JAR_TO_BP: {
                imageDescriptor= JavaPluginImages.DESC_OBJS_EXTJAR;
                // TODO add disabled icon
                text= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddJarCP_label; 
                tooltip= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddJarCP_tooltip; 
                operation= new AddExternalArchivesOperation(null, action);
                break;
            }
            case IClasspathInformationProvider.ADD_LIB_TO_BP: {
                imageDescriptor= JavaPluginImages.DESC_OBJS_LIBRARY;
                // TODO add disabled icon
                text= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddLibCP_label; 
                tooltip= NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddLibCP_tooltip; 
                operation= new AddLibraryOperation(null, action);
                break;
            }
            default: break;
        }
        action.initialize(operation, imageDescriptor, disabledImageDescriptor, text, tooltip);
        return action;
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
        if (!canOperateOnSelection())
        	return;
        String menuText= ActionMessages.BuildPath_label;
        IMenuManager subMenu= new MenuManager(menuText, MENU_ID);
        subMenu.addMenuListener(new IMenuListener() {
        	public void menuAboutToShow(IMenuManager manager) {
        		fillViewSubMenu(manager);
        	}
        });
        subMenu.setRemoveAllWhenShown(true);
        subMenu.add(createConfigureAction(fSite));
        menu.appendToGroup(fGroupName, subMenu);
    }
        
	private void fillViewSubMenu(IMenuManager source) {
        int added= 0;
        
        Action[] actions= getActions();
        for (int i= 0; i < actions.length; i++) {
            if (i == 1)
                source.add(new Separator(GROUP_BUILDPATH));
            else if (i == 6)
                source.add(new Separator(GROUP_FILTER));
            else if (i == 8)
                source.add(new Separator(GROUP_CUSTOMIZE));
            added+= addAction(source, actions[i]);
        }

        if (added == 0) {
        	source.add(fNoActionAvailable);
        }
        
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
    
    private boolean canOperateOnSelection() {
    	ISelection sel= fSite.getSelectionProvider().getSelection();
    	if (!(sel instanceof IStructuredSelection))
    		return false;
    	IStructuredSelection selection= (IStructuredSelection)sel;
    	for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IWorkingSet)
				return false;
		}
    	return true;
    }
}
