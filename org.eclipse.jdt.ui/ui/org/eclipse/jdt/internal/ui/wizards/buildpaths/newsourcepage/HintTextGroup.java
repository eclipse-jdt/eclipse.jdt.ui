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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.actions.ActionContext;

import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.IPackageExplorerActionListener;
import org.eclipse.jdt.internal.corext.buildpath.PackageExplorerActionEvent;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IFolderCreationQuery;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IInclusionExclusionQuery;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IOutputFolderQuery;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IOutputLocationQuery;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
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
    
    public HintTextGroup(DialogPackageExplorer packageExplorer, StringDialogField outputLocationField, SelectionButtonDialogField useFolderOutputs, IRunnableContext runnableContext) {
        fPackageExplorer= packageExplorer;
        fRunnableContext= runnableContext;
        fCurrJProject= null;
        fNewFolders= new ArrayList();
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
     * possible because the actions need a reference to 
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
        FormText formText= createFormText(parent, text); //$NON-NLS-1$
        formText.setImage("defaultImage", JavaPlugin.getImageDescriptorRegistry().get(action.getImageDescriptor())); //$NON-NLS-1$
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
     * Returns the current selection.
     * 
     * @return the current selection
     * @see IClasspathInformationProvider#getSelection()
     */
    public Object getSelection() {
        return fPackageExplorer.getSelection();
    }
    
    /**
     * Set the selection for the <code>DialogPackageExplorer</code>
     * 
     * @param selection the selection to be set
     */
    public void setSelection(Object selection) {
        fPackageExplorer.setSelection(selection);
    }
    
    /**
     * Returns the java project.
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
     * @see IClasspathInformationProvider#handleResult(Object, IPath, CoreException, int)
     */
    public void handleResult(Object result, IPath oldOutputPath, CoreException exception, int actionType) {
        if (exception != null) {
            ExceptionHandler.handle(exception, getShell(), NewWizardMessages.getString("HintTextGroup.JavaModelException.Title"), exception.getLocalizedMessage()); //$NON-NLS-1$
            return;
        }
        
        switch(actionType) {
            case CREATE_FOLDER: handleFolderCreation(result, oldOutputPath); break;
            case EDIT: handleEdit(result, oldOutputPath, false); break;
            case ADD_TO_BP: handleAddToCP(result, oldOutputPath); break;
            case REMOVE_FROM_BP: handleRemoveFromBP(result, false); break;
            case INCLUDE: defaultHandle(result, true); break;
            case EXCLUDE: defaultHandle(result, false); break;
            case UNINCLUDE: defaultHandle(result, false); break;
            case UNEXCLUDE: defaultHandle(result, true); break;
            case RESET: defaultHandle(result, false); break;
            default: break;
        }
    }
    
    /**
     * Default result handle. This includes:
     * <li>Set the selection of the <code>fPackageExplorer</code>
     * to the result object, unless the result object is <code>
     * null</code></li>
     * <li>Force the hint text to be rebuilt if necessary
     *  
     * @param result the result object to be selected by the 
     * <code>fPackageExplorer</code>, can be <code>null</code>
     * @param forceRebuild <code>true</code> if the area containing 
     * the links and their descriptions should be rebuilt, <code>
     * false</code> otherwise
     */
    private void defaultHandle(Object result, boolean forceRebuild) {
        if (result != null) {
            fPackageExplorer.setSelection(result);
            if (forceRebuild)
                fActionGroup.refresh(new ActionContext(new StructuredSelection(new Object[] {result, fCurrJProject})));
        }
    }
    
    /**
     * Handle edit on either:
     * <li>inclusion and exclusion filters of a source folder</li>
     * <li>the output folder of a source folder</li>
     * 
     * @param result the result object to be selected by the 
     * <code>fPackageExplorer</code>, can be <code>null</code>
     * @param oldOutputLocation the project's old output location
     * @param forceRebuild <code>true</code> if the area containing 
     * the links and their descriptions should be rebuilt, <code>
     * false</code> otherwise
     */
    private void handleEdit(Object result, IPath oldOutputLocation, boolean forceRebuild) {
        if (result instanceof IJavaElement)
            defaultHandle(result, forceRebuild);
        else
            handleEditOutputFolder(result, oldOutputLocation);
    }
    
    /**
     * Handle folder creation. This includes:
     * <li>Set the selection of the <code>fPackageExplorer</code>
     * to the result object, unless the result object is <code>
     * null</code></li>
     * <li>Add the created folder to the list of new folders</li>
     * <li>Set adjust the text of <code>fOutputLocationField</code> 
     * and add the project's new output location to the list of 
     * new folders, if necessary
     *  
     * @param result the result object to be selected by the 
     * <code>fPackageExplorer</code>, can be <code>null</code>
     * @param oldOutputLocation the project's old output location
     */
    private void handleFolderCreation(Object result, IPath oldOutputLocation) {
        if (result != null) {
            fNewFolders.add(result);
            fPackageExplorer.setSelection(result);
            try {
                if (!fCurrJProject.getOutputLocation().equals(oldOutputLocation)) {
                    fOutputLocationField.setText(fCurrJProject.getOutputLocation().makeRelative().toString());
                    fNewFolders.add(fCurrJProject.getProject().getFolder(fCurrJProject.getOutputLocation().removeFirstSegments(1)));
                }
            } catch (JavaModelException exception) {
                ExceptionHandler.handle(exception, getShell(), NewWizardMessages.getString("HintTextGroup.JavaModelException.Title"), exception.getMessage()); //$NON-NLS-1$
            }
        }
    }
    
    /**
     * Handle adding to classpath. This includes:
     * <li>Set the selection of the <code>fPackageExplorer</code>
     * to the result object, unless the result object is <code>
     * null</code></li>
     * <li>Set adjust the text of <code>fOutputLocationField</code> 
     * and add the project's new output location to the list of 
     * new folders, if necessary
     *  
     * @param result the result object to be selected by the 
     * <code>fPackageExplorer</code>, can be <code>null</code>
     * @param oldOutputLocation the project's old output location
     */
    private void handleAddToCP(Object result, IPath oldOutputLocation) {
        if (result != null) {
            if (((IPackageFragmentRoot)result).getPath().equals(fCurrJProject.getPath())) {
                fPackageExplorer.setSelection(fCurrJProject);
                fActionGroup.refresh(new ActionContext(new StructuredSelection(new Object[] {fCurrJProject, fCurrJProject})));
            }
            else
                fPackageExplorer.setSelection(result);
            setOutputLocationFieldText(oldOutputLocation);
        }
    }
    
    /**
     * Handle removing from classpath. The goup area is rebuilt if 
     * either <code>forceRebuild</code> is <code>true</code> or 
     * the result is of type <code>IJavaProject</code>.
     * 
     * @param result the result to be selected in the explorer
     * @param forceRebuild <code>true</code> if the hint text group must 
     * be rebuilt, <code>false</code> otherwise.
     */
    private void handleRemoveFromBP(Object result, boolean forceRebuild) {
        fPackageExplorer.setSelection(result);
        if (forceRebuild || result instanceof IJavaProject)
            fActionGroup.refresh(new ActionContext(new StructuredSelection(new Object[] {result, fCurrJProject})));
    }
    
    /**
     * Handle output folder editing. This includes:
     * <li>Set the selection of the <code>fPackageExplorer</code>
     * to the result object, unless the result object is <code>
     * null</code></li>
     * <li>Add the edited folder to the list of new folders</li>
     *  
     * @param result the result object to be selected by the 
     * <code>fPackageExplorer</code>, can be <code>null</code>
     */
    private void handleEditOutputFolder(Object result, IPath oldOutputLocation) {
        CPListElementAttribute attribute= (CPListElementAttribute)result;
        if (attribute != null) {
            fPackageExplorer.setSelection(result);
            IFolder folder= fCurrJProject.getProject().getWorkspace().getRoot().getFolder((IPath)attribute.getValue());
            if (attribute.getValue() != null && !folder.exists())
                fNewFolders.add(folder);
            setOutputLocationFieldText(oldOutputLocation);
        }
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
     * Return an <code>IOutputFolderQuery</code>. In this case, 
     * the default query is taken from the <code>ClasspathModifier</code>.
     * 
     * @see ClasspathModifier#getDefaultFolderQuery(Shell, IPath)
     * @see IClasspathInformationProvider#getOutputFolderQuery()
     */
    public IOutputFolderQuery getOutputFolderQuery() {
        return ClasspathModifier.getDefaultFolderQuery(getShell(), new Path(fOutputLocationField.getText()));

    }

    /**
     * Return an <code>IInclusionExclusionQuery</code>. In this case, 
     * the default query is taken from the <code>ClasspathModifier</code>.
     * 
     * @see ClasspathModifier#getDefaultInclusionExclusionQuery(Shell)
     * @see IClasspathInformationProvider#getInclusionExclusionQuery()
     */
    public IInclusionExclusionQuery getInclusionExclusionQuery() {
        return ClasspathModifier.getDefaultInclusionExclusionQuery(getShell());
    }

    /**
     * Return an <code>IOutputLocationQuery</code>. In this case, 
     * the default query is taken from the <code>ClasspathModifier</code>.
     * 
     * @see ClasspathModifier#getDefaultOutputLocationQuery(Shell, IPath, List)
     * @see IClasspathInformationProvider#getOutputLocationQuery()
     */
    public IOutputLocationQuery getOutputLocationQuery() {
        return ClasspathModifier.getDefaultOutputLocationQuery(getShell(), new Path(fOutputLocationField.getText()), generateClasspathList());
    }
    
    private List generateClasspathList() {
        IClasspathEntry[] entries;
        List list= new ArrayList();
        try {
            entries= fCurrJProject.getRawClasspath();
        } catch (JavaModelException e) {
            ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("HintTextGroup.JavaModelException.Title"), e.getMessage()); //$NON-NLS-1$
            return list;
        }
        for(int i= 0; i < entries.length; i++) {
            list.add(CPListElement.createFromExisting(entries[i], fCurrJProject));
        }
        return list;
    }

    /**
     * Return an <code>IFolderCreationQuery</code>. In this case, 
     * the default query is taken from the <code>ClasspathModifier</code>.
     * 
     * @see ClasspathModifier#getDefaultFolderCreationQuery(Shell, Object, int)
     * @see IClasspathInformationProvider#getFolderCreationQuery()
     */
    public IFolderCreationQuery getFolderCreationQuery() {
        int type= fActionGroup.getType();
        return ClasspathModifier.getDefaultFolderCreationQuery(getShell(), getSelection(), type);
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
            createLabel(childComposite, descriptionText[i], actions[i], fRunnableContext);
        }
        
        fTopComposite.layout(true);
    }
}
