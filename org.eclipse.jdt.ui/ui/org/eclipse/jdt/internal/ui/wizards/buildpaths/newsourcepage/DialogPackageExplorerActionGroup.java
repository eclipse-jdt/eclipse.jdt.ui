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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.actions.ActionContext;

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
import org.eclipse.jdt.internal.corext.buildpath.LinkedSourceFolderOperation;
import org.eclipse.jdt.internal.corext.buildpath.PackageExplorerActionEvent;
import org.eclipse.jdt.internal.corext.buildpath.RemoveFromClasspathOperation;
import org.eclipse.jdt.internal.corext.buildpath.ResetAllOperation;
import org.eclipse.jdt.internal.corext.buildpath.UnexcludeOperation;
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
    
    public static class DialogExplorerActionContext extends ActionContext {
        private IJavaProject fJavaProject;
        private List fSelectedElements;

        /**
         * Constructor to create an action context for the dialog package explorer.
         * 
         * For reasons of completeness, the selection of the super class 
         * <code>ActionContext</code> is also set, but is not intendet to be used.
         * 
         * @param selectedElements a list of elements which are currently selected
         * @param jProject the element's Java project
         */
        public DialogExplorerActionContext(List selectedElements, IJavaProject jProject) {
            super(null);
            fJavaProject= jProject;
            fSelectedElements= selectedElements;
            IStructuredSelection structuredSelection= new StructuredSelection(new Object[] {selectedElements, jProject});
            super.setSelection(structuredSelection);
        }
        
        public IJavaProject getJavaProject() {
            return fJavaProject;
        }
        
        public List getSelectedElements() {
            return fSelectedElements;
        }
    }
    
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
    public static final int INCLUDED_FILE= 0xA;
    /** Included folder */
    public static final int INCLUDED_FOLDER= 0xB;
    /** Output folder (for a source folder) */
    public static final int OUTPUT= 0xC;
    /** A IPackageFragmentRoot with include/exclude filters set */
    public static final int MODIFIED_FRAGMENT_ROOT= 0xD;
    /** Default package fragment */
    public static final int DEFAULT_FRAGMENT= 0xE;
    /** Undefined type */
    public static final int UNDEFINED= 0xF;
    /** Multi selection */
    public static final int MULTI= 0x10;
    /** No elements selected */
    public static final int NULL_SELECTION= 0x11;
    
    private ClasspathModifierAction[] fActions;
    private int fLastType;
    private List fListeners;
    private static final int fContextSensitiveActions= 5;
    
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
        fListeners= new ArrayList();
        fActions= new ClasspathModifierAction[7];
        ClasspathModifierOperation op;
        op= new CreateFolderOperation(listener, provider);
        /*addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_TOOL_NEWPACKROOT, JavaPluginImages.DESC_DLCLL_NEWPACKROOT, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.CreateFolder")), //$NON-NLS-1$
                IClasspathInformationProvider.CREATE_FOLDER);*/
        op= new AddToClasspathOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_ADD_TO_BP, JavaPluginImages.DESC_DLCL_ADD_TO_BP, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.AddToCP")), //$NON-NLS-1$
                IClasspathInformationProvider.ADD_TO_BP);
        op= new RemoveFromClasspathOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_REMOVE_FROM_BP, JavaPluginImages.DESC_DLCL_REMOVE_FROM_BP, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.RemoveFromCP")), //$NON-NLS-1$
                IClasspathInformationProvider.REMOVE_FROM_BP);
        /*op= new IncludeOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_INCLUSION, JavaPluginImages.DESC_DLCL_INCLUSION, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Include")), //$NON-NLS-1$
                IClasspathInformationProvider.INCLUDE);
        op= new UnincludeOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_INCLUSION_UNDO, JavaPluginImages.DESC_DLCL_INCLUSION_UNDO, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Uninclude")), //$NON-NLS-1$
                IClasspathInformationProvider.UNINCLUDE);*/
        op= new ExcludeOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_EXCLUSION, JavaPluginImages.DESC_DLCL_EXCLUSION, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Exclude")), //$NON-NLS-1$
                IClasspathInformationProvider.EXCLUDE);
        op= new UnexcludeOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_INCLUSION, JavaPluginImages.DESC_DLCL_INCLUSION, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Unexclude")), //$NON-NLS-1$
                IClasspathInformationProvider.UNEXCLUDE);
        op= new EditOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_OBJS_TEXT_EDIT, JavaPluginImages.DESC_DLCL_TEXT_EDIT, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Edit")), //$NON-NLS-1$
                IClasspathInformationProvider.EDIT);
        op= new LinkedSourceFolderOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_TOOL_NEWPACKROOT, JavaPluginImages.DESC_DLCL_NEWPACKROOT, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.Link")), //$NON-NLS-1$
                IClasspathInformationProvider.CREATE_LINK);
        op= new ResetAllOperation(listener, provider);
        addAction(new ClasspathModifierAction(op, JavaPluginImages.DESC_ELCL_CLEAR, JavaPluginImages.DESC_DLCL_CLEAR, 
                NewWizardMessages.getString("NewSourceContainerWorkbookPage.ToolBar.UndoAll")), //$NON-NLS-1$
                IClasspathInformationProvider.RESET_ALL);
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
     * 
     * @see IClasspathInformationProvider
     */
    public ClasspathModifierAction getAction(int type) {
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
    public ToolBarManager createLeftToolBarManager(ViewerPane pane) {
        ToolBarManager tbm= pane.getToolBarManager();
        for (int i= 0; i < fContextSensitiveActions; i++) {
            tbm.add(fActions[i]);
            if (i == 1 || i == 3)
                tbm.add(new Separator());
        }
        tbm.update(true);
        return tbm;
    }
    
    /**
     * Create a toolbar manager for a given 
     * <code>ViewerPane</code>
     * 
     * @param pane the pane to create the help toolbar for
     * @return the created <code>ToolBarManager</code>
     */
    public ToolBarManager createLeftToolBar(ViewerPane pane) {
        ToolBar tb= new ToolBar(pane, SWT.FLAT);
        pane.setTopRight(tb);
        ToolBarManager tbm= new ToolBarManager(tb);
        for (int i= fContextSensitiveActions; i < fActions.length; i++) {
            tbm.add(fActions[i]);
        }
        tbm.add(new HelpAction());
        tbm.update(true);
        return tbm;
    }
    
    /**
     * Forces the action group to recompute the available actions 
     * and fire an event to all listeners
     * @throws JavaModelException 
     * 
     * @see #setContext(DialogExplorerActionContext)
     * @see #informListeners(String[], ClasspathModifierAction[])
     */
    public void refresh(DialogExplorerActionContext context) throws JavaModelException {
        super.setContext(context);
        if (context == null) // can happen when disposing
            return;
        List selectedElements= context.getSelectedElements();
        IJavaProject project= context.getJavaProject();
        
        int type= MULTI;
        if (selectedElements.size() == 0) {
            type= NULL_SELECTION;
            
            if (type == fLastType)
                return;
        }
        if (selectedElements.size() == 1 || identicalTypes(selectedElements, project)) {
            type= getType(selectedElements.get(0), project);
        }
        
        internalSetContext(selectedElements, project, type);
    }
    
    /**
     * Set the context of the action group. Note that this method is deprecated. 
     * <ul><li>Clients should use DialogPackageExplorerActionGroup.setContext(DialogExplorerActionContext) instead</li>
     * <li>If this method is called, it is expected that the provided context is of type 
     * <code>DialogExplorerActionContext</code>. If this is not the case, the caller will 
     * end up with a <code>ClassCastException</code>.
     * 
     * @deprecated use instead DialogPackageExplorerActionGroup.setContext(DialogExplorerActionContext)
     * 
     * @see #setContext(DialogExplorerActionContext)
     */
    public void setContext(ActionContext context) {
        try {
            setContext((DialogExplorerActionContext)context);
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
    }
    
    /**
     * Set the context for the action group. This also includes 
     * updating the actions (that is, enable or disable them). 
     * The decision which actions should be enabled or disabled is based 
     * on the content of the <code>DialogExplorerActionContext</code>
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
     * <code>PackageExplorerActionGroup.refresh(DialogExplorerActionContext)</code> can be 
     * called.
     * 
     * @param context the action context
     * 
     * @see IPackageExplorerActionListener
     * @see PackageExplorerActionEvent
     * @see DialogExplorerActionContext
     * @see #addListener(IPackageExplorerActionListener)
     * @see #refresh(DialogExplorerActionContext)
     * 
     * @throws JavaModelException if there is a failure while computing the available 
     * actions.
     */
    public void setContext(DialogExplorerActionContext context) throws JavaModelException {
        super.setContext(context);
        if (context == null) // can happen when disposing
            return;
        List selectedElements= context.getSelectedElements();
        IJavaProject project= context.getJavaProject();
        
        int type= MULTI;
        if (selectedElements.size() == 0) {
            type= NULL_SELECTION;
            
            if (type == fLastType)
                return;
        }
        else if (selectedElements.size() == 1 || identicalTypes(selectedElements, project)) {
            type= getType(selectedElements.get(0), project);
            
            if (selectedElements.size() > 1)
                type= type | MULTI;
        
            if (type == fLastType)
                return;
        }
        
        internalSetContext(selectedElements, project, type);
    }
    
    /**
     * Get a description for the last selection explaining 
     * why no operation is possible.<p>
     * This can be usefull if a context sensitive widget does 
     * not want to display all operations although some of them 
     * are valid.
     * 
     * @return a description for the last selection that explains 
     * why no operation is available.
     */
    public String getNoActionDescription() {
        String[] description= noAction(fLastType);
        return description[0];
    }
    
    /**
     * Internal method to set the context of the action group.
     * 
     * @param selectedElements a list of selected elements, can be empty
     * @param project the Java project
     * @param type the type of the selected element(s)
     * @throws JavaModelException
     */
    private void internalSetContext(List selectedElements, IJavaProject project, int type) throws JavaModelException {
        fLastType= type;
        List availableActions= getAvailableActions(selectedElements, project);
        ClasspathModifierAction[] actions= new ClasspathModifierAction[availableActions.size()];
        String[] descriptions= new String[availableActions.size()];
        if (availableActions.size() > 0) {
            for(int i= 0; i < availableActions.size(); i++) {
                int index= ((Integer)availableActions.get(i)).intValue();
                actions[i]= fActions[index];
                descriptions[i]= fActions[index].getDescription(type);
            }
        } else
            descriptions= noAction(type);

        informListeners(descriptions, actions);
    }
    
    /**
     * Finds out wheter the list of elements consists only of elements 
     * having the same type (for example all are of type 
     * DialogPackageExplorerActionGroup.COMPILATION_UNIT). This allows 
     * to use a description for the available actions which is more 
     * specific and therefore provides more information.
     * 
     * @param elements a list of elements to be compared to each other
     * @param project the java project
     * @return <code>true</code> if all elements are of the same type, 
     * <code>false</code> otherwise.
     * @throws JavaModelException 
     */
    private boolean identicalTypes(List elements, IJavaProject project) throws JavaModelException {
        Object firstElement= elements.get(0);
        int firstType= getType(firstElement, project);
        for(int i= 1; i < elements.size(); i++) {
            if(firstType != getType(elements.get(i), project))
                return false;
        }
        return true;
    }
    
    /**
     * Inform all listeners about new actions.
     * 
     * @param descriptions an array of descriptions for each 
     * actions, where the description at position 'i' belongs to 
     * the action at position 'i'
     * @param actions an array of available actions
     */
    private void informListeners(String[] descriptions, ClasspathModifierAction[] actions) {
        Iterator iterator= fListeners.iterator();
        PackageExplorerActionEvent event= new PackageExplorerActionEvent(descriptions, actions);
        while(iterator.hasNext()) {
            IPackageExplorerActionListener listener= (IPackageExplorerActionListener)iterator.next();
            listener.handlePackageExplorerActionEvent(event);
        }
    }
    
    /**
     * Returns string array with only one element which contains a short reason to indicate 
     * why there are no actions available.
     * 
     * @return a description to explain why there are no actions available
     */
    private String[] noAction(int type) {
        String reason;
        switch(type) {
            case FILE: reason= NewWizardMessages.getString("PackageExplorerActionGroup.NoAction.File"); break; //$NON-NLS-1$
            case FILE | MULTI: reason= NewWizardMessages.getString("PackageExplorerActionGroup.NoAction.File"); break; //$NON-NLS-1$
            case DEFAULT_FRAGMENT: reason= NewWizardMessages.getString("PackageExplorerActionGroup.NoAction.DefaultPackage"); break; //$NON-NLS-1$
            case DEFAULT_FRAGMENT | MULTI: reason= NewWizardMessages.getString("PackageExplorerActionGroup.NoAction.DefaultPackage"); break; //$NON-NLS-1$
            case NULL_SELECTION: reason= NewWizardMessages.getString("PackageExplorerActionGroup.NoAction.NullSelection"); break; //$NON-NLS-1$
            case MULTI: reason= NewWizardMessages.getString("PackageExplorerActionGroup.NoAction.MultiSelection"); break; //$NON-NLS-1$
            default: reason= NewWizardMessages.getString("PackageExplorerActionGroup.NoAction.NoReason"); //$NON-NLS-1$
        }
        return new String[] {reason};
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
     * @throws JavaModelException 
     */
    public static int getType(Object obj, IJavaProject project) throws JavaModelException {
            if (obj instanceof IJavaProject)
                return JAVA_PROJECT;
            if (obj instanceof IPackageFragmentRoot)
                return ClasspathModifier.filtersSet((IPackageFragmentRoot)obj) ? MODIFIED_FRAGMENT_ROOT : PACKAGE_FRAGMENT_ROOT;
            if (obj instanceof IPackageFragment) {
                if (ClasspathModifier.isDefaultFragment((IPackageFragment)obj))
                    return DEFAULT_FRAGMENT;
                if (ClasspathModifier.isIncluded((IJavaElement)obj, project, null))
                    return INCLUDED_FOLDER;
                return PACKAGE_FRAGMENT;
            }
            if (obj instanceof ICompilationUnit)
                return ClasspathModifier.isIncluded((IJavaElement)obj, project, null) ? INCLUDED_FILE : COMPILATION_UNIT;
            if (obj instanceof IFolder) {
                return getFolderType((IFolder)obj, project);
            }
            if (obj instanceof IFile)
                return getFileType((IFile)obj, project);
            if (obj instanceof CPListElementAttribute)
                return ClasspathModifier.isDefaultOutputFolder((CPListElementAttribute) obj) ? DEFAULT_OUTPUT : OUTPUT;
        return UNDEFINED;
    }
    
    /**
     * Get the type of the folder
     * 
     * @param folder folder to get the type from
     * @return the type code for the folder. Possible types are:<br>
     * PackageExplorerActionGroup.FOLDER<br>
     * PackageExplorerActionGroup.EXCLUDED_FOLDER;<br>
     * @throws JavaModelException 
     */
    private static int getFolderType(IFolder folder, IJavaProject project) throws JavaModelException {
        if (folder.getParent().getFullPath().equals(project.getPath()))
            return FOLDER;
        if (ClasspathModifier.getFragment(folder.getParent()) != null)
            return EXCLUDED_FOLDER;
        if (ClasspathModifier.getFragmentRoot(folder, project, null) == null)
            return FOLDER;
        if (ClasspathModifier.getFragmentRoot(folder, project, null).equals(JavaCore.create(folder.getParent())))
            return EXCLUDED_FOLDER;
        return FOLDER;              
    }
    
    /**
     * Get the type of the file
     * 
     * @param file file to get the type from
     * @return the type code for the file. Possible types are:<br>
     * PackageExplorerActionGroup.EXCLUDED_FILE<br>
     * PackageExplorerActionGroup.FILE
     * @throws JavaModelException 
     */
    private static int getFileType(IFile file, IJavaProject project) throws JavaModelException {
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
    }
    
    /**
     * Based on the given list of elements, get the list of available 
     * actions that can be applied on this elements
     * 
     * @param selectedElements the list of elements to get the actions for
     * @param project the Java project
     * @return a list of <code>ClasspathModifierAction</code>s
     * @throws JavaModelException
     */
    private List getAvailableActions(List selectedElements, IJavaProject project) throws JavaModelException {
        List actions= new ArrayList();
        int[] types= new int[selectedElements.size()];
        for(int i= 0; i < types.length; i++) {
            types[i]= getType(selectedElements.get(i), project);
        }
        for(int i= 0; i < fActions.length; i++) {
            if(fActions[i].isValid(selectedElements, types)) {
                actions.add(new Integer(i));
                if (!fActions[i].isEnabled())
                    fActions[i].setEnabled(true);
            } else {
                if (fActions[i].isEnabled())
                    fActions[i].setEnabled(false);
            }
        }
        return actions;
    }
    
    /**
     * Fill the context menu with the available actions
     * 
     * @param menu the menu to be filled up with actions
     */
    public void fillContextMenu(IMenuManager menu) {        
        for (int i= 0; i < fContextSensitiveActions; i++) {
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
        fListeners.add(listener);
    }
    
    /**
     * Remove the listener from the list of registered listeners.
     * 
     * @param listener the listener to be removed
     */
    public void removeListener(IPackageExplorerActionListener listener) {
        fListeners.remove(listener);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.actions.CompositeActionGroup#dispose()
     */
    public void dispose() {
        fListeners.clear();
        super.dispose();
    }
}
