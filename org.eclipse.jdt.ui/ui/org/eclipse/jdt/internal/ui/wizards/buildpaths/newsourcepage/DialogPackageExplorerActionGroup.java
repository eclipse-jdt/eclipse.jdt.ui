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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.actions.ActionContext;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.AddToClasspathOperation;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifierOperation;
import org.eclipse.jdt.internal.corext.buildpath.CreateFolderOperation;
import org.eclipse.jdt.internal.corext.buildpath.EditOperation;
import org.eclipse.jdt.internal.corext.buildpath.ExcludeOperation;
import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.IPackageExplorerActionListener;
import org.eclipse.jdt.internal.corext.buildpath.IncludeOperation;
import org.eclipse.jdt.internal.corext.buildpath.PackageExplorerActionEvent;
import org.eclipse.jdt.internal.corext.buildpath.RemoveFromClasspathOperation;
import org.eclipse.jdt.internal.corext.buildpath.ResetOperation;
import org.eclipse.jdt.internal.corext.buildpath.UnexcludeOperation;
import org.eclipse.jdt.internal.corext.buildpath.UnincludeOperation;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.util.ViewerPane;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;

/**
 * Action group for the package explorer. Creates and manages a set 
 * of <code>ClasspathModifierOperation</code>s and creates a <code>ToolBarManager</code> 
 * on request. Based on this operations, <code>ClasspathModifierAction</code>s are generated. 
 * The available operations are:
 * 
 * @see org.eclipse.jdt.internal.corext.buildpath.CreateFolderOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.AddToClasspathOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.RemoveFromClasspathOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.IncludeOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.UnincludeOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.ExcludeOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.UnexcludeOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.EditOperation
 * @see org.eclipse.jdt.internal.corext.buildpath.ResetOperation
 */
public class DialogPackageExplorerActionGroup extends CompositeActionGroup {
    
    /** Java project */
    public static final int JAVA_PROJECT= 0x01;
    /** Package fragment root */
    public static final int PACKAGE_FRAGMENT_ROOT= 0x02;
    /** Package fragment */
    public static final int PACKAGE_FRAGMENT= 0x03;
    /** Compilation unit */
    public static final int COMPILATION_UNIT= 0x04;
    /** File */
    public static final int FILE= 0x05;
    /** Normal folder */
    public static final int FOLDER= 0x06;
    /** Excluded folder */
    public static final int EXCLUDED_FOLDER= 0x07;
    /** Excluded file */
    public static final int EXCLUDED_FILE= 0x08;
    /** Default output folder */
    public static final int DEFAULT_OUTPUT= 0x09;
    /** Included file */
    public static final int INCLUDED_FILE= 0x10;
    /** Included folder */
    public static final int INCLUDED_FOLDER= 0x11;
    /** Output folder (for a source folder) */
    public static final int OUTPUT= 0x12;
    /** A IPackageFragmentRoot with include/exclude filters set */
    public static final int MODIFIED_FRAGMENT_ROOT= 0x13;
    /** Default package fragment */
    public static final int DEFAULT_FRAGMENT= 0x14;
    /** Undefined type */
    public static final int UNDEFINED= 0x15;
    
    private ClasspathModifierAction[] fActions;
    private int fLastType;
    private List listeners;
    
    /**
     * Constructor which creates the operations and based on this 
     * operations the actions.
     * 
     * @param provider a information provider to pass necessary information 
     * to the operations
     * @param listener a listener for the changes on classpath entries, that is 
     * the listener will be notified whenever a classpath entry changed.
     * @see IClasspathModifierListener
     */
    public DialogPackageExplorerActionGroup(IClasspathInformationProvider provider, IClasspathModifierListener listener) {
        super();
        fLastType= UNDEFINED;
        listeners= new ArrayList();
        fActions= new ClasspathModifierAction[9];
        ClasspathModifierOperation op;
        op= new CreateFolderOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_TOOL_NEWPACKROOT, JavaPluginImages.DESC_DLCLL_NEWPACKROOT, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.CreateFolder")), //$NON-NLS-1$
                IClasspathInformationProvider.CREATE_FOLDER);
        op= new AddToClasspathOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_ADD_TO_BP, JavaPluginImages.DESC_DLCL_ADD_TO_BP, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.AddToCP")), //$NON-NLS-1$
                IClasspathInformationProvider.ADD_TO_BP);
        op= new RemoveFromClasspathOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_REMOVE_FROM_BP, JavaPluginImages.DESC_DLCL_REMOVE_FROM_BP, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.RemoveFromCP")), //$NON-NLS-1$
                IClasspathInformationProvider.REMOVE_FROM_BP);
        op= new IncludeOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_INCLUSION, JavaPluginImages.DESC_DLCL_INCLUSION, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Include")), //$NON-NLS-1$
                IClasspathInformationProvider.INCLUDE);
        op= new UnincludeOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_INCLUSION_UNDO, JavaPluginImages.DESC_DLCL_INCLUSION_UNDO, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Uninclude")), //$NON-NLS-1$
                IClasspathInformationProvider.UNINCLUDE);
        op= new ExcludeOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_EXCLUSION, JavaPluginImages.DESC_DLCL_EXCLUSION, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Exclude")), //$NON-NLS-1$
                IClasspathInformationProvider.EXCLUDE);
        op= new UnexcludeOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_EXCLUSION_UNDO, JavaPluginImages.DESC_DLCL_EXCLUSION_UNDO, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Unexclude")), //$NON-NLS-1$
                IClasspathInformationProvider.UNEXCLUDE);
        op= new EditOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_OBJS_TEXT_EDIT, JavaPluginImages.DESC_DLCL_TEXT_EDIT, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Edit")), //$NON-NLS-1$
                IClasspathInformationProvider.EDIT);
        op= new ResetOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_OBJS_CLEAR, JavaPluginImages.DESC_DLCL_CLEAR, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Reset")), //$NON-NLS-1$
                IClasspathInformationProvider.RESET);
    }
    
    private void addAction(ClasspathModifierAction action, int index) {
        fActions[index]= action;
    }
    
    /**
     * Get an action of the specified type
     * 
     * @param type the type of the desired action, must be a
     * constante of <code>IClasspathInformationProvider</code>
     * @return the requested action
     */
    private ClasspathModifierAction getAction(int type) {
        return fActions[type];
    }
    
    /**
     * Create a toolbar manager for a given 
     * <code>ViewerPane</code>
     * 
     * @param pane the pane to create the <code>
     * ToolBarManager</code> for.
     * @return the created <code>ToolBarManager</code>
     */
    public ToolBarManager createToolBarManager(ViewerPane pane) {
        ToolBarManager tbm= pane.getToolBarManager();
        for (int i= 0; i < fActions.length; i++)
            tbm.add(fActions[i]);
        tbm.update(true);
        return tbm;
    }
    
    /**
     * Create a toolbar manager with only one element which 
     * represents a help action (that links to a html page)
     * 
     * @param pane the pane to create the help toolbar for
     * @return the toolbar manager for this help action
     */
    public ToolBarManager createHelpToolBar(ViewerPane pane) {
        ToolBar tb= new ToolBar(pane, SWT.FLAT);
        pane.setTopRight(tb);
        ToolBarManager tbm= new ToolBarManager(tb);
        tbm.add(new HelpAction());
        tbm.update(true);
        return tbm;
    }
    
    /**
     * Get the type of the current selected object.
     * 
     * @return the type of the current selection. Is one 
     * of the constants of <code>PackageExplorerActionGroup</code>.
     */
    public int getType() {
        return fLastType;
    }
    
    /**
     * Forces the action group to recompute the available actions 
     * and fire an event to all listeners
     * 
     * @see #setContext(ActionContext)
     * @see #informListeners(String[], ClasspathModifierAction[])
     */
    public void refresh(ActionContext context) {
        super.setContext(context);
        IStructuredSelection selection= (IStructuredSelection) context.getSelection();
        List selectedElements= selection.toList();
        Object selectedObject= selectedElements.get(0);
        IJavaProject project= (IJavaProject) selectedElements.get(1);
        fLastType= getType(selectedObject, project);
        
        Object[][] result= computeActions(selectedObject, project, fLastType);
        informListeners((String[]) result[0], (ClasspathModifierAction[]) result[1]);
    }
    
    /**
     * Set the context for the action group. This also includes 
     * to update the actions (that is, enable or disable them). 
     * The decision which actions to enable or disable is based 
     * on the content of the <code>ActionContext</code> as follows:<p>
     * 
     * The <code>ActionContext.getSelection()</code> must return an 
     * element of type <code>IStructuredSelection</code>. The 
     * <code>IStructuredSelection</code> itself contains two elements: 
     * <li>1. An object which represents the element on which an action should 
     * be performed. Based on this object, the set of available actions 
     * is computed. For example, if the object is of type <code>
     * IPackageFragmentRoot</code>, then possible actions could be 
     * removing from classpath, editing inclusion/exclusion filters, ...</li>
     * <li>2. An <code>IJavaProject</code> to determine the available actions 
     * which often not only depends on the object, but also on the project 
     * structure.</li>
     * 
     * If the type of the selection changes, then listeners will be notified 
     * about the new set of available actions.
     * 
     * Note: notification is only done if the TYPE changes (not the selected object 
     * as such). This means that if elements of the same type are selected (for 
     * example two times a folder), NO notification will take place. There might 
     * be situations where the type of two objects is the same but the set of 
     * available actions is not. However, if clients decide that upon some action 
     * a recomputation of the available actions has to be forced, then 
     * <code>PackageExplorerActionGroup.refresh(ActionContext)</code> can be 
     * called.
     * 
     * Clients who want to force recomputation of and notification of the new 
     * actions can call <code>refresh(ActionContext).
     * 
     * @param context the action context containing the list of actions 
     * to be enabled.
     * 
     * @see IPackageExplorerActionListener
     * @see PackageExplorerActionEvent
     * @see #addListener(IPackageExplorerActionListener)
     */
    public void setContext(ActionContext context) {
        super.setContext(context);
        IStructuredSelection selection= (IStructuredSelection) context.getSelection();
        List selectedElements= selection.toList();
        Object selectedObject= selectedElements.get(0);
        IJavaProject project= (IJavaProject) selectedElements.get(1);
        int type= getType(selectedObject, project);
        
        if (type == fLastType)
            return;
        
        fLastType= type;
        Object[][] result= computeActions(selectedObject, project, type);
        
        informListeners((String[]) result[0], (ClasspathModifierAction[]) result[1]);
    }
    
    /**
     * Inform all listeners about new actions.
     * 
     * @param descriptions an array of descriptions for each 
     * actions
     * @param actions an array of available actions
     */
    private void informListeners(String[] descriptions, ClasspathModifierAction[] actions) {
        Iterator iterator= listeners.iterator();
        PackageExplorerActionEvent event= new PackageExplorerActionEvent(descriptions, actions);
        while(iterator.hasNext()) {
            IPackageExplorerActionListener listener= (IPackageExplorerActionListener)iterator.next();
            listener.handlePackageExplorerActionEvent(event);
        }
    }
    
    /**
     * Compute the set of available actions and it's descriptions.
     * 
     * @param obj the object to get the available actions for
     * @param project the java project
     * @param type the type of the selected object
     * @return two arrays, one the first one of type <code>String</code> 
     * containing the descriptions, the second one of type <code>
     * ClasspathModifierAction</code> with the actions itself.
     * If no actions are available, the the array containing the actions has size 
     * zero and the other array containing the descriptions has one string 
     * which describes the reason why there is no action.
     */
    private Object[][] computeActions(Object obj, IJavaProject project, int type) {
        switch (type) {
            case JAVA_PROJECT: return javaProjectSelected(obj, project);
            case PACKAGE_FRAGMENT_ROOT: return fragmentRootSelected(false);
            case PACKAGE_FRAGMENT: return fragmentSelected(false);
            case COMPILATION_UNIT: return javaFileSelected(false);
            case FOLDER: return folderSelected(obj, project);
            case EXCLUDED_FOLDER: return excludedFolderSelected(obj, project);
            case EXCLUDED_FILE: return excludedFileSelected(obj, project);
            case FILE: return noAction(NewWizardMessages.getString("PackageExplorerActionGroup.NoAction.File")); //$NON-NLS-1$
            case INCLUDED_FILE: return javaFileSelected(true);
            case INCLUDED_FOLDER: return fragmentSelected(true);
            case DEFAULT_OUTPUT: return outputFolderSelected(true);
            case OUTPUT: return outputFolderSelected(false);
            case MODIFIED_FRAGMENT_ROOT: return fragmentRootSelected(true);
            case DEFAULT_FRAGMENT: return noAction(NewWizardMessages.getString("PackageExplorerActionGroup.NoAction.DefaultPackage")); //$NON-NLS-1$
            default: return noAction(NewWizardMessages.getString("PackageExplorerActionGroup.NoAction.NoReason")); //$NON-NLS-1$
        }
    }
    
    /**
     * Gather the descriptions and actions for this kind selection.
     * 
     * @param obj the selected object
     * @param project the java project
     */
    private Object[][] javaProjectSelected(Object obj, IJavaProject project) {
        List descriptionText= new ArrayList();
        List actions= new ArrayList();
        List enableList= new ArrayList();
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.create"), IClasspathInformationProvider.CREATE_FOLDER,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        try {
            if (ClasspathModifier.getClasspathEntryFor(project.getPath(), project) == null)
                addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.ProjectToBuildpath"), IClasspathInformationProvider.ADD_TO_BP,  //$NON-NLS-1$
                        descriptionText, actions, enableList);
            else
                addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.ProjectFromBuildpath"), IClasspathInformationProvider.REMOVE_FROM_BP,  //$NON-NLS-1$
                        descriptionText, actions, enableList);
            if (project.isOnClasspath(project.getUnderlyingResource())) {
                addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Edit"), IClasspathInformationProvider.EDIT,  //$NON-NLS-1$
                        descriptionText, actions, enableList);
                IClasspathEntry entry= ClasspathModifier.getClasspathEntryFor(project.getPath(), project);
                if (entry.getInclusionPatterns().length != 0 || entry.getExclusionPatterns().length != 0) {
                    addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.ResetFilters"), IClasspathInformationProvider.RESET,  //$NON-NLS-1$
                            descriptionText, actions, enableList);
                }
            }
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        
        enableActions(enableList);
        
        return new Object[][] {(String[]) descriptionText.toArray(new String[descriptionText.size()]), 
                (ClasspathModifierAction[]) actions.toArray(new ClasspathModifierAction[actions.size()])};
    }
    
    /**
     * Gather the descriptions and actions for this kind selection.
     * 
     * @param filtersSet <code>true</code> if fragment root
     * has inclusion and/or exclusion filters set, <code>
     * false</code> otherwise.
     */
    private Object[][] fragmentRootSelected(boolean filtersSet) {
        List descriptionText= new ArrayList();
        List actions= new ArrayList();
        List enableList= new ArrayList();
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.create"), IClasspathInformationProvider.CREATE_FOLDER,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.fromBuildpath"), IClasspathInformationProvider.REMOVE_FROM_BP,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.Edit"), IClasspathInformationProvider.EDIT,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        if (filtersSet) {
            addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.ResetFilters"), IClasspathInformationProvider.RESET,  //$NON-NLS-1$
                    descriptionText, actions, enableList);
        }
        enableActions(enableList);
        
        return new Object[][] {(String[]) descriptionText.toArray(new String[descriptionText.size()]), 
                (ClasspathModifierAction[]) actions.toArray(new ClasspathModifierAction[actions.size()])};
    }
    
    /**
     * Gather the descriptions and actions for this kind selection.
     * 
     * @param included <code>true</code> if fragment
     * is included (this is set on the inclusion filter of its parent
     * source folder, </code>false</code> otherwise.
     * is the project's default fragment, </code>false</code> otherwise.
     */
    private Object[][] fragmentSelected(boolean included) {
        List descriptionText= new ArrayList();
        List actions= new ArrayList();
        List enableList= new ArrayList();
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.create"), IClasspathInformationProvider.CREATE_FOLDER,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.toBuildpath"), IClasspathInformationProvider.ADD_TO_BP,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        if (included) {
            addToLists(NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.Uninclude", "package"), IClasspathInformationProvider.UNINCLUDE,  //$NON-NLS-1$ //$NON-NLS-2$
                    descriptionText, actions, enableList);
        }
        else {
            addToLists(NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.Include", "package"), IClasspathInformationProvider.INCLUDE,  //$NON-NLS-1$ //$NON-NLS-2$
                    descriptionText, actions, enableList);
        }
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.ExcludePackage"), IClasspathInformationProvider.EXCLUDE,  //$NON-NLS-1$ //$NON-NLS-2$
                descriptionText, actions, enableList);
        enableActions(enableList);
        
        return new Object[][] {(String[]) descriptionText.toArray(new String[descriptionText.size()]), 
                (ClasspathModifierAction[]) actions.toArray(new ClasspathModifierAction[actions.size()])};
    }
    
    /**
     * Gather the descriptions and actions for this kind selection.
     * 
     * @param included <code>true</code> if fragment
     * is included (this is set on the inclusion filter of its parent
     * source folder, </code>false</code> otherwise.
     */
    private Object[][] javaFileSelected(boolean included) {
        List descriptionText= new ArrayList();
        List actions= new ArrayList();
        List enableList= new ArrayList();
        if (included) {
            addToLists(NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.Uninclude", "file"), IClasspathInformationProvider.UNINCLUDE,  //$NON-NLS-1$ //$NON-NLS-2$
                    descriptionText, actions, enableList);
        }
        else {
            addToLists(NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.Include", "file"), IClasspathInformationProvider.INCLUDE,  //$NON-NLS-1$ //$NON-NLS-2$
                    descriptionText, actions, enableList);
        }
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.ExcludeFile"), IClasspathInformationProvider.EXCLUDE,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        enableActions(enableList);
        
        return new Object[][] {(String[]) descriptionText.toArray(new String[descriptionText.size()]), 
                (ClasspathModifierAction[]) actions.toArray(new ClasspathModifierAction[actions.size()])};
    }
    
    /**
     * Gather the descriptions and actions for this kind selection.
     * 
     * @param obj the selected object
     * @param project the java project
     */
    private Object[][] folderSelected(Object obj, IJavaProject project) {
        List descriptionText= new ArrayList();
        List actions= new ArrayList();
        List enableList= new ArrayList();
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.create"), IClasspathInformationProvider.CREATE_FOLDER,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.toBuildpath"), IClasspathInformationProvider.ADD_TO_BP,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        IResource resource= (IResource)obj;
        try {
            if (project.isOnClasspath(project.getUnderlyingResource()) && resource.getProjectRelativePath().segmentCount() == 1 && 
                    ClasspathModifier.getFragmentRoot(resource, project, null).equals(ClasspathModifier.getFragmentRoot(project.getCorrespondingResource(), project, null))) { 
                addToLists(NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.Include", "folder"), IClasspathInformationProvider.INCLUDE,  //$NON-NLS-1$ //$NON-NLS-2$
                        descriptionText, actions, enableList);
                if (ClasspathModifier.isExcludedOnProject(resource, project)) {
                    addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.UnexcludeFolder"), IClasspathInformationProvider.UNEXCLUDE,  //$NON-NLS-1$
                            descriptionText, actions, enableList);
                }
            }
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        enableActions(enableList);
        
        return new Object[][] {(String[]) descriptionText.toArray(new String[descriptionText.size()]), 
                (ClasspathModifierAction[]) actions.toArray(new ClasspathModifierAction[actions.size()])};
    }
    
    /**
     * Gather the descriptions and actions for this kind selection.
     * 
     * @param obj the selected object
     * @param project the java project
     */
    private Object[][] excludedFolderSelected(Object obj, IJavaProject project) {
        List descriptionText= new ArrayList();
        List actions= new ArrayList();
        List enableList= new ArrayList();
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.create"), IClasspathInformationProvider.CREATE_FOLDER,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.toBuildpath"), IClasspathInformationProvider.ADD_TO_BP,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        addToLists(NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.Include", "folder"), IClasspathInformationProvider.INCLUDE,  //$NON-NLS-1$ //$NON-NLS-2$
                descriptionText, actions, enableList);
        try {
            if (ClasspathModifier.includeFiltersEmpty((IResource) obj, project, null)) { 
                addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.UnexcludeFolder"), IClasspathInformationProvider.UNEXCLUDE,  //$NON-NLS-1$
                        descriptionText, actions, enableList);
            }
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        } 
        enableActions(enableList);
        
        return new Object[][] {(String[]) descriptionText.toArray(new String[descriptionText.size()]), 
                (ClasspathModifierAction[]) actions.toArray(new ClasspathModifierAction[actions.size()])};
    }
    
    /**
     * Gather the descriptions and actions for this kind selection.
     * 
     * @param isDefaultOutput <code>true</code> if output folder
     * is the project's default output folder, </code>false</code> otherwise.
     */
    private Object[][] outputFolderSelected(boolean isDefaultOutput) {
        List descriptionText= new ArrayList();
        List actions= new ArrayList();
        List enableList= new ArrayList();
        addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.EditOutputFolder"), IClasspathInformationProvider.EDIT,  //$NON-NLS-1$
                descriptionText, actions, enableList);
        if (!isDefaultOutput) {
            addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.SetOutputToDefault"), IClasspathInformationProvider.RESET,  //$NON-NLS-1$
                    descriptionText, actions, enableList);
        }
        enableActions(enableList);
        
        return new Object[][] {(String[]) descriptionText.toArray(new String[descriptionText.size()]), 
                (ClasspathModifierAction[]) actions.toArray(new ClasspathModifierAction[actions.size()])};
    }
    
    /**
     * Gather the descriptions and actions for this kind selection.
     * 
     * @param obj the selected object
     * @param project the java project
     */
    private Object[][] excludedFileSelected(Object obj, IJavaProject project) {
        List descriptionText= new ArrayList();
        List actions= new ArrayList();
        List enableList= new ArrayList();
        IFile file= (IFile)obj;
        IPackageFragment fragment= ClasspathModifier.getFragment(file.getParent());
        addToLists(NewWizardMessages.getFormattedString("PackageExplorerActionGroup.FormText.Include", "file"), IClasspathInformationProvider.INCLUDE,  //$NON-NLS-1$ //$NON-NLS-2$
                descriptionText, actions, enableList);
        try {
            if (fragment != null || ClasspathModifier.getFragmentRoot(file, project, null).equals(JavaCore.create(file.getParent())) ||
                    (file.getParent().getFullPath().equals(project.getPath()) 
                    && ClasspathModifier.isExcludedOnProject(file, project) 
                    && ClasspathModifier.includeFiltersEmpty(project.getCorrespondingResource(), project, null))) {
                if (ClasspathModifier.includeFiltersEmpty(file, project, null))
                    addToLists(NewWizardMessages.getString("PackageExplorerActionGroup.FormText.UnexcludeFile"), IClasspathInformationProvider.UNEXCLUDE,  //$NON-NLS-1$
                            descriptionText, actions, enableList);
            }
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        enableActions(enableList); 
        
        return new Object[][] {(String[]) descriptionText.toArray(new String[descriptionText.size()]), 
                (ClasspathModifierAction[]) actions.toArray(new ClasspathModifierAction[actions.size()])};
    }
    
    /**
     * Returns two arrays, the one with the actions is empty and the 
     * other with the description contains a short reason to indicate 
     * why there are no actions available.
     * 
     * @return two empty arrays as no actions are available.
     */
    private Object[][] noAction(String reason) {
        enableActions(new ArrayList());
        return new Object[][] {new String[] {reason}, new ClasspathModifierAction[0]};
    }
    
    /**
     * Helper method to fill the lists with necessary information.
     * 
     * @param description the description of the action
     * @param type the type of the action
     * @param descriptionList the list storing all descriptions needed so far
     * @param actionList the list storing all actions needed so far
     * @param enableList a list of <code>Integers</code> to keep track of 
     * which actions have to be enabled (and therefore which are not).
     * 
     * @see #enableActions(List)
     */
    private void addToLists(String description, int type, List descriptionList, List actionList, List enableList) {
        descriptionList.add(description);
        actionList.add(getAction(type));
        enableList.add(new Integer(type));
    }
    
    /**
     * Computes the type based on the current selection. The type
     * can be usefull to set the content of the hint text group
     * properly.
     * 
     * @param obj the object to get the type from
     * @return the type of the current selection or UNDEFINED if no
     * appropriate type could be found. Possible types are:<br>
     * PackageExplorerActionGroup.FOLDER<br>
     * PackageExplorerActionGroup.EXCLUDED_FOLDER;<br>
     * PackageExplorerActionGroup.EXCLUDED_FILE<br>
     * PackageExplorerActionGroup.DEFAULT_OUTPUT<br>
     * PackageExplorerActionGroup.INCLUDED_FILE<br>
     * PackageExplorerActionGroup.INCLUDED_FOLDER<br>
     * PackageExplorerActionGroup.OUTPUT<br>
     * PackageExplorerActionGroup.MODIFIED_FRAGMENT_ROOT<br>
     * PackageExplorerActionGroup.DEFAULT_FRAGMENT<br>
     * PackageExplorerActionGroup.JAVA_PROJECT<br>
     * PackageExplorerActionGroup.PACKAGE_FRAGMENT_ROOT<br>
     * PackageExplorerActionGroup.PACKAGE_FRAGMENT<br>
     * PackageExplorerActionGroup.COMPILATION_UNIT<br>
     * PackageExplorerActionGroup.FILE<br>
     */
    private int getType(Object obj, IJavaProject project) {
        try {
            if (obj instanceof IJavaProject)
                return JAVA_PROJECT;
            if (obj instanceof IPackageFragmentRoot)
                return ClasspathModifier.filtersSet((IPackageFragmentRoot)obj) ? MODIFIED_FRAGMENT_ROOT : PACKAGE_FRAGMENT_ROOT;
            if (obj instanceof IPackageFragment) {
                if (ClasspathModifier.isDefaultFragment((IPackageFragment)obj))
                    return DEFAULT_FRAGMENT;
                if (ClasspathModifier.containsPath((IJavaElement)/*getSelection()*/obj, project, null))
                    return INCLUDED_FOLDER;
                return PACKAGE_FRAGMENT;
            }
            if (obj instanceof ICompilationUnit)
                return ClasspathModifier.containsPath((IJavaElement)/*getSelection()*/obj, project, null) ? INCLUDED_FILE : COMPILATION_UNIT;
            if (obj instanceof IFolder) {
                return getFolderType((IFolder)obj, project);
            }
            if (obj instanceof IFile)
                return getFileType((IFile)obj, project);
            if (obj instanceof CPListElementAttribute)
                return ClasspathModifier.isDefaultOutputFolder((CPListElementAttribute) obj) ? DEFAULT_OUTPUT : OUTPUT;
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        return UNDEFINED;
    }
    
    /**
     * Get the type of the folder
     * 
     * @param folder folder to get the type from
     * @return the type code for the folder. Possible types are:<br>
     * PackageExplorerActionGroup.FOLDER<br>
     * PackageExplorerActionGroup.EXCLUDED_FOLDER;<br>
     */
    private int getFolderType(IFolder folder, IJavaProject project) {
        if (folder.getParent().getFullPath().equals(project.getPath()))
            return FOLDER;
        if (ClasspathModifier.getFragment(folder.getParent()) != null)
            return EXCLUDED_FOLDER;
        try {
            if (ClasspathModifier.getFragmentRoot(folder, project, null) == null)
                return FOLDER;
            if (ClasspathModifier.getFragmentRoot(folder, project, null).equals(JavaCore.create(folder.getParent())))
                return EXCLUDED_FOLDER;
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        return FOLDER;              
    }
    
    /**
     * Get the type of the file
     * 
     * @param file file to get the type from
     * @return the type code for the file. Possible types are:<br>
     * PackageExplorerActionGroup.EXCLUDED_FILE<br>
     * PackageExplorerActionGroup.FILE
     */
    private int getFileType(IFile file, IJavaProject project) {
        try {
            if (!file.getName().endsWith(".java")) //$NON-NLS-1$
                return FILE;
            if (file.getParent().getFullPath().equals(project.getPath())) {
                if (project.isOnClasspath(project.getUnderlyingResource())) //$NON-NLS-1$
                    return EXCLUDED_FILE;
                return FILE;
            }
            if (ClasspathModifier.getFragmentRoot(file, project, null) == null)
                return FILE;
            if (ClasspathModifier.getFragmentRoot(file, project, null).equals(JavaCore.create(file.getParent())))
                return EXCLUDED_FILE;
            if (ClasspathModifier.getFragment(file.getParent()) == null) {
                if (ClasspathModifier.parentExcluded(file.getParent(), project))
                    return FILE;
                return EXCLUDED_FILE;
            }
            return EXCLUDED_FILE;
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        return UNDEFINED;
    }
    
    /**
     * Enable the actions of the 
     * with the indices contained in the <code>
     * List</code>
     * 
     * @param indices a list of indices of action 
     * which have to be enabled. The indices are 
     * taken from one of the constants of the interface
     * IClasspathInformationProvider.
     * 
     * @see #setContext(ActionContext)
     * @see IClasspathInformationProvider
     */
    private void enableActions(List indices) {
        for(int i= 0; i < fActions.length; i++) {
            IAction action= fActions[i];
            if (indices.contains(new Integer(i))) {
                if (!action.isEnabled())
                    action.setEnabled(true);
            }
            else {
                if (action.isEnabled())
                    action.setEnabled(false);
            }
        }
    }
    
    /**
     * Fill the context menu with the available actions
     * 
     * @param menu the menu to be filled up with actions
     */
    public void fillContextMenu(IMenuManager menu) {        
        for (int i= 0; i < fActions.length; i++) {
            IAction action= getAction(i);
            if (action.isEnabled())
                menu.add(action);
        }
        super.fillContextMenu(menu);
    }
    
    /**
     * Add listeners for the <code>PackageExplorerActionEvent</code>.
     * 
     * @param listener the listener to be added
     * 
     * @see PackageExplorerActionEvent
     * @see IPackageExplorerActionListener
     */
    public void addListener(IPackageExplorerActionListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove the listener from the list of registered listeners.
     * 
     * @param listener the listener to be removed
     */
    public void removeListener(IPackageExplorerActionListener listener) {
        listeners.remove(listener);
    }
}
