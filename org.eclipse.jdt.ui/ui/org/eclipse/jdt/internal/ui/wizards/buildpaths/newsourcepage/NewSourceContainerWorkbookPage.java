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

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.ResetAllOutputFoldersOperation;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.ViewerPane;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.DialogPackageExplorerActionGroup.DialogExplorerActionContext;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class NewSourceContainerWorkbookPage extends BuildPathBasePage implements IClasspathModifierListener {
    
    public static final String OPEN_SETTING= "org.eclipse.jdt.internal.ui.wizards.buildpaths.NewSourceContainerPage.openSetting";  //$NON-NLS-1$
    
    private ListDialogField fClassPathList;
    private List fOriginalState;
    private HintTextGroup fHintTextGroup;
    private DialogPackageExplorer fPackageExplorer;
    private SelectionButtonDialogField fUseFolderOutputs;
    private IDocument fCPContent;
    private IDocument fProjectContent;
    
    /**
     * Constructor of the <code>NewSourceContainerWorkbookPage</code> which consists of 
     * a tree representing the project, a toolbar with the available actions, an area 
     * containing hyperlinks that perform the same actions as those in the toolbar but 
     * additionally with some short description.
     * 
     * @param classPathList
     * @param outputLocationField
     * @param context a runnable context, can be <code>null</code>
     */
    public NewSourceContainerWorkbookPage(ListDialogField classPathList, StringDialogField outputLocationField, IRunnableContext context) {
        fClassPathList= classPathList;
        fOriginalState= fClassPathList.getElements();
    
        fUseFolderOutputs= new SelectionButtonDialogField(SWT.CHECK);
        fUseFolderOutputs.setSelection(false);
        fUseFolderOutputs.setLabelText(NewWizardMessages.getString("SourceContainerWorkbookPage.folders.check")); //$NON-NLS-1$
        
        fPackageExplorer= new DialogPackageExplorer();
        fHintTextGroup= new HintTextGroup(fPackageExplorer, outputLocationField, fUseFolderOutputs, context);
    }
    
    /**
     * Initialize the controls displaying
     * the content of the java project and saving 
     * the '.classpath' and '.project' file.
     * 
     * Must be called before initializing the 
     * controls using <code>getControl(Composite)</code>.
     * 
     * @param jProject the current java project
     */
    public void init(IJavaProject jProject) {
        IJavaProject javaProject= jProject;
        fHintTextGroup.setJavaProject(jProject);
        
        saveFile(javaProject.getProject());
        fPackageExplorer.setContentProvider();
        fPackageExplorer.setInput(javaProject);
        try {
            if (ClasspathModifier.hasOutputFolders(javaProject, null)) {
                fUseFolderOutputs.setSelection(true);
                fUseFolderOutputs.dialogFieldChanged();
            }
            else {
                fUseFolderOutputs.setSelection(false);
                fUseFolderOutputs.dialogFieldChanged();
            }
        } catch (JavaModelException e) {
            ExceptionHandler.handle(e, getShell(), NewWizardMessages.getString("NewSourceContainerWorkbookPage.Exception.init"), e.getMessage()); //$NON-NLS-1$
            fUseFolderOutputs.setSelection(false);
        }
    }
    
    /**
     * Create a copy of the files ".classpath" and
     * ".project" to restore later in case of an
     * undo operation
     * 
     * @param project the current project
     * 
     * @see #restoreFile(IFile, IDocument)
     * @see #undoAll()
     */
    private void saveFile(IProject project) {
        IFile cPFile= project.getFile(".classpath"); //$NON-NLS-1$
        IFile projectFile= project.getFile(".project"); //$NON-NLS-1$
        fCPContent= null;
        fProjectContent= null;
        try {
            if (cPFile.exists())
                fCPContent= getDocument(cPFile.getFullPath());
            if (projectFile.exists())
                fProjectContent= getDocument(projectFile.getFullPath());
        } catch (CoreException e) {
            ExceptionHandler.handle(e, getShell(), 
                    NewWizardMessages.getString("NewSourceContainerWorkbookPage.Exception.saveFile"),  //$NON-NLS-1$
                    e.getMessage());
        }
    }
    
    /**
     * Restore the previously saved content in
     * <code>document</code> in the given <code>
     * file</code>
     * 
     * @param file the file to store the content to; if
     * <code>file.exists()</code> then the file is deleted
     * first.
     * 
     * @param document contains the content to be
     * stored to the given <code>file</code>
     */
    private void restoreFile(IFile file, IDocument document) {            
        if (document == null || file == null || !file.exists())
            return;

        try {
            file.delete(true, null);
            if (file.getLocation() == null)
                return;
            file.create(new ByteArrayInputStream(document.get().getBytes()), true, null);
        } catch (CoreException e) {
            ExceptionHandler.handle(e, getShell(), 
                    NewWizardMessages.getFormattedString("NewSourceContainerWorkbookPage.Exception.restoreFile", file.getName()),  //$NON-NLS-1$
                    e.getMessage());
        }
    }
    
    /**
     * Gets the content of a file at the
     * given <code>IPath</code>
     * 
     * @param path the path to a file
     * @return returns an <code>IDocument</code> with
     * the content of the file at the given path
     */
    private IDocument getDocument(IPath path) throws CoreException{
        try {
            FileBuffers.getTextFileBufferManager().connect(path, new NullProgressMonitor());
            ITextFileBuffer buffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(path);
            IDocument document= buffer.getDocument();
            return document;
        } finally {
            FileBuffers.getTextFileBufferManager().disconnect(path, new NullProgressMonitor());
        }
    }
    
    /**
     * Initializ controls and return composite containing
     * these controls.
     * 
     * Before calling this method, make sure to have 
     * initialized this instance with a java project 
     * using <code>init(IJavaProject)</code>.
     * 
     * @param parent the parent composite
     * @return composite containing controls
     * 
     * @see #init(IJavaProject)
     */
    public Control getControl(Composite parent) {
        final int[] sashWeight= {60};
        final IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
        preferenceStore.setDefault(OPEN_SETTING, true);
        
        // ScrolledPageContent is needed for resizing on expand the expandable composite
        ScrolledPageContent scrolledContent = new ScrolledPageContent(parent);
        Composite body= scrolledContent.getBody();
        body.setLayout(new GridLayout());
        
        final SashForm sashForm= new SashForm(body, SWT.VERTICAL | SWT.NONE);
        
        ViewerPane pane= new ViewerPane(sashForm, SWT.BORDER | SWT.FLAT);
        pane.setContent(fPackageExplorer.createControl(pane));
        
        final ExpandableComposite excomposite= new ExpandableComposite(sashForm, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
        excomposite.setText(NewWizardMessages.getString("NewSourceContainerWorkbookPage.HintTextGroup.title")); //$NON-NLS-1$
        final boolean isExpanded= preferenceStore.getBoolean(OPEN_SETTING);
        excomposite.setExpanded(isExpanded);
        excomposite.addExpansionListener(new ExpansionAdapter() {
                       public void expansionStateChanged(ExpansionEvent e) {
                           ScrolledPageContent parentScrolledComposite= getParentScrolledComposite(excomposite);
                           if (parentScrolledComposite != null) {
                              boolean expanded= excomposite.isExpanded();
                              parentScrolledComposite.reflow(true);
                              adjustSashForm(sashWeight, sashForm, expanded);
                              preferenceStore.setValue(OPEN_SETTING, expanded);
                           }
                       }
                 });
        
        excomposite.setClient(fHintTextGroup.createControl(excomposite));
        fUseFolderOutputs.doFillIntoGrid(body, 1);
        fUseFolderOutputs.setDialogFieldListener(new IDialogFieldListener() {
            public void dialogFieldChanged(DialogField field) {
                Button button= ((SelectionButtonDialogField)field).getSelectionButton(null);
                if (button.getSelection()) {
                    ResetAllOutputFoldersOperation op= new ResetAllOutputFoldersOperation(NewSourceContainerWorkbookPage.this, fHintTextGroup);
                    try {
                        op.run(null);
                    } catch (InvocationTargetException e) {
                        ExceptionHandler.handle(e, getShell(),
                                NewWizardMessages.getFormattedString("NewSourceContainerWorkbookPage.Exception.Title", op.getName()), e.getMessage()); //$NON-NLS-1$
                    }
                }
            }
        });
        final DialogPackageExplorerActionGroup actionGroup= new DialogPackageExplorerActionGroup(fHintTextGroup, this);
        fUseFolderOutputs.getSelectionButton(null).addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent event) {
                boolean show= fUseFolderOutputs.getSelectionButton(null).getSelection();
                fPackageExplorer.showOutputFolders(show);
                try {
                    ISelection selection= fPackageExplorer.getSelection();
                    actionGroup.refresh(new DialogExplorerActionContext(selection, fHintTextGroup.getJavaProject()));
                } catch (JavaModelException e) {
                    ExceptionHandler.handle(e, getShell(),
                            NewWizardMessages.getString("NewSourceContainerWorkbookPage.Exception.refresh"), e.getMessage()); //$NON-NLS-1$
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
            
        });
        
        // Create toolbar with actions on the left
        ToolBarManager tbm= actionGroup.createLeftToolBarManager(pane);
        pane.setTopCenter(null);
        pane.setTopLeft(tbm.getControl());
        
        // Create toolbar with help on the right
        tbm= actionGroup.createLeftToolBar(pane);
        pane.setTopRight(tbm.getControl());
        
        fHintTextGroup.setActionGroup(actionGroup);
        fPackageExplorer.setActionGroup(actionGroup);
        actionGroup.addListener(fHintTextGroup);
        
        sashForm.setWeights(new int[] {60, 40});
        adjustSashForm(sashWeight, sashForm, excomposite.isExpanded());
        GridData gd= new GridData(GridData.FILL_BOTH);
        PixelConverter converter= new PixelConverter(parent);
        gd.heightHint= converter.convertHeightInCharsToPixels(25);
        sashForm.setLayoutData(gd);
        
        fUseFolderOutputs.dialogFieldChanged();
        
        parent.layout(true);

        return scrolledContent;
    }
    
    /**
     * Adjust the size of the sash form.
     * 
     * @param sashWeight the weight to be read or written
     * @param sashForm the sash form to apply the new weights to
     * @param isExpanded <code>true</code> if the expandable composite is 
     * expanded, <code>false</code> otherwise
     */
    private void adjustSashForm(int[] sashWeight, SashForm sashForm, boolean isExpanded) {
        if (isExpanded) {
            int upperWeight= sashWeight[0];
            sashForm.setWeights(new int[]{upperWeight, 100 - upperWeight});
        }
        else {
            // TODO Dividing by 10 because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=81939
            sashWeight[0]= sashForm.getWeights()[0] / 10;
            sashForm.setWeights(new int[]{95, 5});
        }
        sashForm.layout(true);
    }
    
    /**
     * Get the scrolled page content of the given control by 
     * traversing the parents.
     * 
     * @param control the control to get the scrolled page content for 
     * @return the scrolled page content or <code>null</code> if none found
     */
    private ScrolledPageContent getParentScrolledComposite(Control control) {
       Control parent= control.getParent();
       while (!(parent instanceof ScrolledPageContent)) {
           parent= parent.getParent();
       }
       if (parent instanceof ScrolledPageContent) {
           return (ScrolledPageContent) parent;
       }
       return null;
   }
    
    /**
     * Get the active shell.
     * 
     * @return the active shell
     */
    private Shell getShell() {
        return JavaPlugin.getActiveWorkbenchShell();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage#getSelection()
     */
    public List getSelection() {
        List selectedList= new ArrayList();
        
        IJavaProject project= fHintTextGroup.getJavaProject();
        try {
            List list= ((IStructuredSelection)fHintTextGroup.getSelection()).toList();
            List existingEntries= ClasspathModifier.getExistingEntries(project);
        
            for(int i= 0; i < list.size(); i++) {
                Object obj= list.get(i);
                if (obj instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot element= (IPackageFragmentRoot)obj;
                    CPListElement cpElement= ClasspathModifier.getClasspathEntry(existingEntries, element); 
                    selectedList.add(cpElement);
                }
                else if (obj instanceof IJavaProject) {
                    IClasspathEntry entry= ClasspathModifier.getClasspathEntryFor(project.getPath(), project);
                    if (entry == null)
                        continue;
                    CPListElement cpElement= CPListElement.createFromExisting(entry, project);
                    selectedList.add(cpElement);
                }
            }
        } catch (JavaModelException e) {
            return new ArrayList();
        }
        return selectedList;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage#setSelection(java.util.List)
     */
    public void setSelection(List selection) {
        if (selection.size() == 0)
            return;
        IJavaProject project= ((CPListElement)selection.get(0)).getJavaProject();
        List cpEntries= new ArrayList();
        for(int i= 0; i < selection.size(); i++) {
            CPListElement element= (CPListElement) selection.get(i);
            IPackageFragmentRoot root= project.findPackageFragmentRoots(element.getClasspathEntry())[0];
            if (root.getPath().equals(root.getJavaProject().getPath()))
                cpEntries.add(project);
            else
                cpEntries.add(root);
        }
        
        // refresh classpath
        List list= fClassPathList.getElements();
        IClasspathEntry[] entries= new IClasspathEntry[list.size()];
        for(int i= 0; i < list.size(); i++) {
            CPListElement entry= (CPListElement)list.get(i);
            entries[i]= entry.getClasspathEntry(); 
        }
        try {
            project.setRawClasspath(entries, null);
            fPackageExplorer.refresh();
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        
        fHintTextGroup.setSelection(cpEntries);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage#isEntryKind(int)
     */
    public boolean isEntryKind(int kind) {
        return kind == IClasspathEntry.CPE_SOURCE;
    }
    
    /**
     * Undo all changes. This includes: <br> 
     * <li> Restore the ".classpath" and ".project" files.
     * <li> Deleting all newly created folders
     * 
     * @see HintTextGroup#deleteCreatedResources()
     */
    public void undoAll() {
        if (fHintTextGroup.getJavaProject() == null) // Project not initialized yet
            return;
        IProject project= fHintTextGroup.getJavaProject().getProject();
        IFile cPFile= project.getFile(".classpath");  //$NON-NLS-1$
        IFile projectFile= project.getFile(".project"); //$NON-NLS-1$
        
        restoreFile(cPFile, fCPContent);
        restoreFile(projectFile, fProjectContent);
        
        fClassPathList.setElements(fOriginalState);
        
        fHintTextGroup.deleteCreatedResources();
        
        fCPContent= null;
        fProjectContent= null;
    }

    /**
     * Update <code>fClassPathList</code>.
     */
    public void classpathEntryChanged(List newEntries) {
        fClassPathList.setElements(newEntries);
    }
}
