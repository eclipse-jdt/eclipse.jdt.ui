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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.IPackageExplorerActionListener;
import org.eclipse.jdt.internal.corext.buildpath.PackageExplorerActionEvent;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IFolderCreationQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IInclusionExclusionQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.ILinkToQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputLocationQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup.DialogExplorerActionContext;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * Displays a set of available links to modify or adjust the project.
 * The links contain a short description about the consequences of 
 * this action.
 * 
 * The content depends on the selection made on the project.
 * If selection changes, then the <code>HintTextGroup</code> will be 
 * notified through the <code>IPackageExplorerActionListener</code> interface. 
 */
public final class HintTextGroup implements IClasspathInformationProvider, IPackageExplorerActionListener {
    
    private StringDialogField fOutputLocationField;
    private Composite fTopComposite;
    private DialogPackageExplorerActionGroup fActionGroup;
    private DialogPackageExplorer fPackageExplorer;
    private IRunnableContext fRunnableContext;
    private IJavaProject fCurrJProject;
    private List fNewFolders;
    private String fOldOutputLocation;
    private HashMap fImageMap;
    
    public HintTextGroup(DialogPackageExplorer packageExplorer, StringDialogField outputLocationField, SelectionButtonDialogField useFolderOutputs, IRunnableContext runnableContext) {
        fPackageExplorer= packageExplorer;
        fRunnableContext= runnableContext;
        fCurrJProject= null;
        fNewFolders= new ArrayList();
        fImageMap= new HashMap();
        fOutputLocationField= outputLocationField;
    }
    
    public Composite createControl(Composite parent) {
        fTopComposite= new Composite(parent, SWT.NONE);
        GridData gridData= new GridData(GridData.FILL_BOTH);
        PixelConverter converter= new PixelConverter(fTopComposite);
        gridData.heightHint= converter.convertHeightInCharsToPixels(12);
        GridLayout gridLayout= new GridLayout();
        gridLayout.marginLeft= -converter.convertWidthInCharsToPixels(2);
        gridLayout.marginRight= -4;
        fTopComposite.setLayout(gridLayout);
        fTopComposite.setLayoutData(gridData);
        fTopComposite.setData(null);
        fTopComposite.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                Collection collection= fImageMap.values();
                Iterator iterator= collection.iterator();
                while(iterator.hasNext()) {
                    Image image= (Image)iterator.next();
                    image.dispose();
                }
            }
        });
        return fTopComposite;
    }
    
    private Shell getShell() {
        return JavaPlugin.getActiveWorkbenchShell();
    }
    
    /**
     * Set the java project
     * 
     * @param jProject the java project to work on
     */
    public void setJavaProject(IJavaProject jProject) {
        fCurrJProject= jProject;
        fOldOutputLocation= fOutputLocationField.getText();
    }
    
    /**
     * An action group managing the actions needed by 
     * the <code>HintTextGroup</code>.
     * 
     * Note: This method has to be called on initialization.
     * Calling this method in the constructor is not 
     * possible because the actions might need a reference to 
     * this class.
     * 
     * @param actionGroup the action group containing the necessary 
     * actions
     * 
     * @see DialogPackageExplorerActionGroup
     */
    public void setActionGroup(DialogPackageExplorerActionGroup actionGroup) {
        fActionGroup= actionGroup;
    }
    
    /**
     * Creates a form text.
     * 
     * @param parent the parent to put the form text on
     * @param text the form text to be displayed
     * @return the created form text
     * 
     * @see FormToolkit#createFormText(org.eclipse.swt.widgets.Composite, boolean)
     */
    private FormText createFormText(Composite parent, String text) {
        FormToolkit toolkit= new FormToolkit(getShell().getDisplay());
        FormText formText = toolkit.createFormText(parent, true);
        try {
            formText.setText(text, true, false);
        } catch (SWTException e) {
            formText.setText(e.getMessage(), false, false);
        }
        formText.marginHeight= 2;
        formText.setBackground(null);
        formText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
        toolkit.dispose();
        return formText;
    }
    
    /**
     * Create a label with a hyperlink and a picture.
     * 
     * @param parent the parent widget of the label
     * @param text the text of the label
     * @param action the action to be executed if the hyperlink is activated
     * @param context the runnable context under which the action is executed
     */
    private void createLabel(Composite parent, String text, final ClasspathModifierAction action, final IRunnableContext context) {
        FormText formText= createFormText(parent, text);
        Image image= (Image)fImageMap.get(action.getId());
        if (image == null) {
            image= action.getImageDescriptor().createImage();
            fImageMap.put(action.getId(), image);
        }
        formText.setImage("defaultImage", image); //$NON-NLS-1$
        formText.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {
                try {
                    context.run(false, false, action.getOperation());
                } catch (InvocationTargetException err) {
                    ExceptionHandler.handle(err, getShell(), NewWizardMessages.getString("HintTextGroup.JavaModelException.Title"), err.getMessage()); //$NON-NLS-1$
                } catch (InterruptedException err) {
                    // Cancel pressed
                }
            }
            
        });
    }
    
    /**
     * Returns the current selection which is a list of 
     * elements in a tree.
     * 
     * @return the current selection
     * @see IClasspathInformationProvider#getSelection()
     */
    public List getSelection() {
        return fPackageExplorer.getSelection();
    }
    
    /**
     * Set the selection for the <code>DialogPackageExplorer</code>
     * 
     * @param elements a list of elements to be selected
     * 
     * @see DialogPackageExplorer#setSelection(List)
     */
    public void setSelection(List elements) {
        fPackageExplorer.setSelection(elements);
    }
    
    /**
     * Returns the Java project.
     * 
     * @see IClasspathInformationProvider#getJavaProject()
     */
    public IJavaProject getJavaProject() {
        return fCurrJProject;
    }
    
    /**
     * Handle the result of an action. Note that first, the exception has to be 
     * checked and computation can only continue if the exception is <code>null</code>.
     * 
     * @see IClasspathInformationProvider#handleResult(List, CoreException, int)
     */
    public void handleResult(List resultElements, CoreException exception, int actionType) {
        if (exception != null) {
            ExceptionHandler.handle(exception, getShell(), NewWizardMessages.getString("HintTextGroup.JavaModelException.Title"), exception.getLocalizedMessage()); //$NON-NLS-1$
            return;
        }
        
        switch(actionType) {
            case CREATE_FOLDER: handleFolderCreation(resultElements); break;
            case CREATE_LINK: handleFolderCreation(resultElements); break;
            case EDIT: handleEdit(resultElements, false); break;
            case ADD_TO_BP: handleAddToCP(resultElements); break;
            case REMOVE_FROM_BP: handleRemoveFromBP(resultElements, false); break;
            case INCLUDE: defaultHandle(resultElements, true); break;
            case EXCLUDE: defaultHandle(resultElements, false); break;
            case UNINCLUDE: defaultHandle(resultElements, false); break;
            case UNEXCLUDE: defaultHandle(resultElements, true); break;
            case RESET: defaultHandle(resultElements, false); break;
            case RESET_ALL: handleResetAll(); break;
            default: break;
        }
    }
    
    /**
     * Default result handle. This includes:
     * <li>Set the selection of the <code>fPackageExplorer</code>
     * to the result object</li>
     * <li>Force the hint text to be rebuilt if necessary</li>
     *  
     * @param result the result list of object to be selected by the 
     * <code>fPackageExplorer</code>
     * @param forceRebuild <code>true</code> if the area containing 
     * the links and their descriptions should be rebuilt, <code>
     * false</code> otherwise
     * 
     * @see DialogPackageExplorer#setSelection(List)
     * @see DialogPackageExplorerActionGroup#refresh(DialogExplorerActionContext)
     */
    private void defaultHandle(List result, boolean forceRebuild) {
        try {
            fPackageExplorer.setSelection(result);
            if (forceRebuild) {
                fActionGroup.refresh(new DialogExplorerActionContext(result, fCurrJProject));
            }
        } catch (JavaModelException e) {
            ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("HintTextGroup.JavaModelException.Title"), e.getLocalizedMessage()); //$NON-NLS-1$
        }
    }
    
    /**
     * Handle edit on either:
     * <li>Inclusion and exclusion filters of a source folder</li>
     * <li>The output folder of a source folder</li>
     * 
     * @param result the result list of an object to be selected by the 
     * <code>fPackageExplorer</code>. In this case, the list only can 
     * have size one and contains either an element of type <code>IJavaElement</code> 
     * or <code>CPlistElementAttribute</code>
     * @param forceRebuild <code>true</code> if the area containing 
     * the links and their descriptions should be rebuilt, <code>
     * false</code> otherwise
     */
    private void handleEdit(List result, boolean forceRebuild) {
        if (result.size() != 1)
            return;
        if (result.get(0) instanceof IJavaElement)
            defaultHandle(result, forceRebuild);
        else
            handleEditOutputFolder(result);
    }
    
    /**
     * Handle folder creation. This includes:
     * <li>Set the selection of the <code>fPackageExplorer</code>
     * to the result object, unless the result object is <code>
     * null</code></li>
     * <li>Add the created folder to the list of new folders</li>
     * <li>Adjust the text of <code>fOutputLocationField</code> 
     * and add the project's new output location to the list of 
     * new folders, if necessary
     * 
     * In this case, the list consists only of one element on which the 
     * new folder has been created
     *  
     * @param result a list with only one element to be selected by the 
     * <code>fPackageExplorer</code>, or an empty list if creation was 
     * aborted
     */
    private void handleFolderCreation(List result) {
        if (result.size() == 1) {
            fNewFolders.add(result.get(0));
            fPackageExplorer.setSelection(result);
            setOutputLocationFieldText(getOldOutputLocation());
        }
    }
    
    /**
     * Handle adding to classpath. This includes:
     * <li>Set the selection of the <code>fPackageExplorer</code>
     * to the list of result elements</li>
     * <li>Set adjust the text of <code>fOutputLocationField</code> 
     * and add the project's new output location to the list of 
     * new folders, if necessary
     *  
     * @param result the result list of object to be selected by the 
     * <code>fPackageExplorer</code>
     */
    private void handleAddToCP(List result) {
        try {
            if (containsJavaProject(result)) {
                fPackageExplorer.setSelection(result);
                fActionGroup.refresh(new DialogExplorerActionContext(result, fCurrJProject));
            }
            else
                fPackageExplorer.setSelection(result);
            setOutputLocationFieldText(getOldOutputLocation());
        } catch (JavaModelException e) {
            ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("HintTextGroup.JavaModelException.Title"), e.getLocalizedMessage()); //$NON-NLS-1$
        }
    }
    
    /**
     * Handle removing from classpath. The goup area is rebuilt if 
     * either <code>forceRebuild</code> is <code>true</code> or 
     * the result contains one element of type <code>IJavaProject</code>.
     * 
     * @param result the result to be selected in the explorer
     * @param forceRebuild <code>true</code> if the hint text group must 
     * be rebuilt, <code>false</code> otherwise.
     */
    private void handleRemoveFromBP(List result, boolean forceRebuild) {
        fPackageExplorer.setSelection(result);
        try {
            if (forceRebuild || containsJavaProject(result)) {
                fActionGroup.refresh(new DialogExplorerActionContext(result, fCurrJProject));
            }
        } catch (JavaModelException e) {
            ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("HintTextGroup.JavaModelException.Title"), e.getLocalizedMessage()); //$NON-NLS-1$
        }
    }
    
    /**
     * Handle output folder editing. This includes:
     * <li>Set the selection of the <code>fPackageExplorer</code>
     * to the only one element contained in the list if this 
     * element is not <code>null</code></li>
     * <li>Add the edited folder to the list of new folders</li>
     *  
     * @param result a list containing only one element of type 
     * <code>CPListElementAttribute</code> which can be <code>null</code>
     */
    private void handleEditOutputFolder(List result) {
        CPListElementAttribute attribute= (CPListElementAttribute)result.get(0);
        if (attribute != null) {
            fPackageExplorer.setSelection(result);
            IPath path= (IPath)attribute.getValue();
            if (path != null) {
                IFolder folder= fCurrJProject.getProject().getWorkspace().getRoot().getFolder(path);
                if (!folder.exists())
                    fNewFolders.add(folder);
            }
            setOutputLocationFieldText(getOldOutputLocation());
        }
    }

    /**
     * Handle resetting of the Java project to the original 
     * state. This only means that the package explorer 
     * needs to be updated and the selection should 
     * be set to the Java project root itself.
     */
    private void handleResetAll() {
        List list= new ArrayList();
        list.add(fCurrJProject);
        setSelection(list);
    }
    
    /**
     * Find out whether the list contains one element of 
     * type <code>IJavaProject</code>
     * 
     * @param elements the list to be examined
     * @return <code>true</code> if the list contains one element of 
     * type <code>IJavaProject</code>, <code>false</code> otherwise
     */
    private boolean containsJavaProject(List elements) {
        for(int i= 0; i < elements.size(); i++) {
            if (elements.get(i) instanceof IJavaProject)
                return true;
        }
        return false;
    }
    
    private IPath getOldOutputLocation() {
        return new Path(fOutputLocationField.getText()).makeAbsolute();
    }
    
    /**
     * Set the text of <code>fOutputLocationField</code>.
     * 
     * @param oldOutputLocation the project's old output location
     */
    private void setOutputLocationFieldText(IPath oldOutputLocation) {
        try {
            if (!fCurrJProject.getOutputLocation().equals(oldOutputLocation)) {
                fOutputLocationField.setText(fCurrJProject.getOutputLocation().makeRelative().toString());
                IJavaElement element= fCurrJProject.findElement(fCurrJProject.getOutputLocation().removeFirstSegments(1));
                if (element != null)
                    fNewFolders.add(element);
            }
        } catch (JavaModelException exception) {
            ExceptionHandler.handle(exception, getShell(), NewWizardMessages.getString("HintTextGroup.JavaModelException.Title"), exception.getMessage()); //$NON-NLS-1$
        }
    }
    
    /**
     * Return an <code>IOutputFolderQuery</code>.
     * 
     * @see ClasspathModifierQueries#getDefaultFolderQuery(Shell, IPath)
     * @see IClasspathInformationProvider#getOutputFolderQuery()
     */
    public IOutputFolderQuery getOutputFolderQuery() {
        return ClasspathModifierQueries.getDefaultFolderQuery(getShell(), new Path(fOutputLocationField.getText()));

    }

    /**
     * Return an <code>IInclusionExclusionQuery</code>.
     * 
     * @see ClasspathModifierQueries#getDefaultInclusionExclusionQuery(Shell)
     * @see IClasspathInformationProvider#getInclusionExclusionQuery()
     */
    public IInclusionExclusionQuery getInclusionExclusionQuery() {
        return ClasspathModifierQueries.getDefaultInclusionExclusionQuery(getShell());
    }

    /**
     * Return an <code>IOutputLocationQuery</code>.
     * @throws JavaModelException 
     * 
     * @see ClasspathModifierQueries#getDefaultOutputLocationQuery(Shell, IPath, List)
     * @see IClasspathInformationProvider#getOutputLocationQuery()
     */
    public IOutputLocationQuery getOutputLocationQuery() throws JavaModelException {
        List classpathEntries= ClasspathModifier.getExistingEntries(fCurrJProject);
        return ClasspathModifierQueries.getDefaultOutputLocationQuery(getShell(), new Path(fOutputLocationField.getText()), classpathEntries);
    }

    /**
     * Return an <code>IFolderCreationQuery</code>.
     * 
     * @see ClasspathModifierQueries#getDefaultFolderCreationQuery(Shell, Object)
     * @see IClasspathInformationProvider#getFolderCreationQuery()
     */
    public IFolderCreationQuery getFolderCreationQuery() {
        return ClasspathModifierQueries.getDefaultFolderCreationQuery(getShell(), getSelection().get(0));
    }
    
    /**
     * Return an <code>ILinkToQuery</code>.
     * 
     * @see ClasspathModifierQueries#getDefaultLinkQuery(Shell, IJavaProject, IPath)
     * @see IClasspathInformationProvider#getLinkFolderQuery()
     */
    public ILinkToQuery getLinkFolderQuery() throws JavaModelException {
        return ClasspathModifierQueries.getDefaultLinkQuery(getShell(), fCurrJProject, new Path(fOutputLocationField.getText()));
    }
    
    /**
     * Delete all newly created folders and files.
     * Resources that existed before will not be 
     * deleted.
     */
    public void deleteCreatedResources() {
        Iterator iterator= fNewFolders.iterator();
        while (iterator.hasNext()) {
            Object element= iterator.next();
            IFolder folder;
            try {
                if (element instanceof IFolder)
                    folder= (IFolder)element;
                else if (element instanceof IJavaElement)
                    folder= fCurrJProject.getProject().getWorkspace().getRoot().getFolder(((IJavaElement)element).getPath());
                else {
                    ((IFile)element).delete(false, null);
                    continue;
                }
                folder.delete(false, null);
            } catch (CoreException e) {
            }            
        }
        
        fOutputLocationField.setText(fOldOutputLocation);
        fNewFolders= new ArrayList();
    }

    /**
     * Handle the package explorer action event.
     * This includes:
     * <li>Disposing the old composite which contained the labels with the links</li>
     * <li>Create a new composite</li>
     * <li>Create new labels with the new actions as child of the new composite. The 
     * information which lables have to be created is contained in the event.
     * 
     * @see PackageExplorerActionEvent
     * @see IPackageExplorerActionListener#handlePackageExplorerActionEvent(PackageExplorerActionEvent)
     */
    public void handlePackageExplorerActionEvent(PackageExplorerActionEvent event) {
        // Get the child composite of the top composite
        Composite childComposite= (Composite)fTopComposite.getData();
        
        // Dispose old composite (if necessary)
        if (childComposite != null && childComposite.getParent() != null)
            childComposite.getParent().dispose();
        
        // Create new composite
        ScrolledPageContent spc= new ScrolledPageContent(fTopComposite, SWT.V_SCROLL);
        spc.getVerticalBar().setIncrement(5);
        spc.setLayoutData(new GridData(GridData.FILL_BOTH));
        childComposite= spc.getBody();
        childComposite.setLayout(new TableWrapLayout());
        childComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        fTopComposite.setData(childComposite);
        
        // Display available actions
        ClasspathModifierAction[] actions= event.getEnabledActions();
        String[] descriptionText= event.getEnabledActionsText();
        if (actions.length == 0) {
            createFormText(childComposite, NewWizardMessages.getFormattedString("HintTextGroup.NoAction", descriptionText[0])); //$NON-NLS-1$
            fTopComposite.layout(true);
            return;
        }
        
        for (int i= 0; i < actions.length; i++) {
            int id= Integer.parseInt(actions[i].getId());
            if (id == IClasspathInformationProvider.RESET_ALL)
                continue;
            if (id == IClasspathInformationProvider.CREATE_LINK)
                continue;
            createLabel(childComposite, descriptionText[i], actions[i], fRunnableContext);
        }
        
        fTopComposite.layout(true);
    }
}
