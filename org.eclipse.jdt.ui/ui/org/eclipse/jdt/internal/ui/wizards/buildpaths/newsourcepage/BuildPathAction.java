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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.AddExternalArchivesOperation;
import org.eclipse.jdt.internal.corext.buildpath.AddLibraryOperation;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifierOperation;
import org.eclipse.jdt.internal.corext.buildpath.EditOutputFolderOperation;
import org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider;
import org.eclipse.jdt.internal.corext.buildpath.LinkedSourceFolderOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IAddArchivesQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IAddLibrariesQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IFolderCreationQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IInclusionExclusionQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.ILinkToQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputLocationQuery;

/**
 * Buildpath action class implementing the <code>IClasspathInformationProvider</code> interface to pass 
 * information to the <code>ClasspathModifierOperation</code> that is passed as parameter in 
 * the contructor.
 */
public class BuildPathAction extends SelectionDispatchAction implements IClasspathInformationProvider {
    protected ClasspathModifierOperation fOperation;
    protected IJavaProject fJavaProject;
    
    /**
     * Creates an <code>AbstractModifierAction</code>.
     * 
     * @param site the site providing context information for this action
     */
    public BuildPathAction(IWorkbenchSite site) {
        super(site);
    }
    
    /*
     * The reason why the operation is not passed as parameter in the 
     * constructor is this: the operation needs an information provider 
     * as parameter in its contructor. In this case, the BuildPathAction 
     * implements the provider for the operation. Therefore, it is not 
     * possible to pass the operation to the action before the action itself 
     * was created.
     */
    /**
     * Set the operation for this action. This method has to be called right 
     * after having initialized this class. 
     * 
     * @param operation the operation to be used in this action
     * @param imageDescriptor the image descriptor for the icon
     * @param disabledImageDescriptor the image descriptor for the disabled icon
     * @param text the text to be set as label for the action
     * @param tooltip the text to be set as tool tip
     */
    public void initialize(ClasspathModifierOperation operation, 
            ImageDescriptor imageDescriptor, ImageDescriptor disabledImageDescriptor, 
            String text, String tooltip) {
        fOperation= operation;
        setImageDescriptor(imageDescriptor);
        setDisabledImageDescriptor(disabledImageDescriptor);
        setText(text);
        setToolTipText(tooltip);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run()
     */
    public void run() {
        try {
            fOperation.run(null);
        } catch (InvocationTargetException e) {
            ExceptionHandler.handle(e, getShell(), 
                    NewWizardMessages.getFormattedString("HintTextGroup.Exception.Title", //$NON-NLS-1$
                            fOperation.getName()), e.getMessage());
        } catch (InterruptedException e) {
            // operation canceled
        }
    }
    
    /* (non-Javadoc)
     * Method declared in SelectionDispatchAction
     */
    public void selectionChanged(IStructuredSelection selection) {
        try {   
            List elements= fOperation.getSelectedElements();
            fJavaProject= getJavaProject(selection);
            
            if (fJavaProject == null) {
                setEnabled(false);
                return;
            }
            
            int[] types= new int[elements.size()];
            for(int i= 0; i < elements.size(); i++) {
                types[i]= DialogPackageExplorerActionGroup.getType(elements.get(i), fJavaProject);
            }
            
            if(fOperation instanceof LinkedSourceFolderOperation || 
                    fOperation instanceof AddExternalArchivesOperation ||
                    fOperation instanceof AddLibraryOperation)
                setEnabled(elements.size() == 1 && types[0] == DialogPackageExplorerActionGroup.JAVA_PROJECT);
            else 
                setEnabled(fOperation.isValid(elements, types) && sameJavaProject());
        } catch (JavaModelException e) {
            // http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
            if (JavaModelUtil.filterNotPresentException(e))
                JavaPlugin.log(e);
            setEnabled(false);
        }
    }
    
    /**
     * Get the Java project from the first element in the provided selection.
     * 
     * @param selection the selection containing a list of elements 
     * @return the Java project of the first element of the selection, or 
     * <code>null</code> if the selection is empty or no Java project could 
     * be found.
     */
    protected IJavaProject getJavaProject(IStructuredSelection selection) {
        if(selection.isEmpty())
            return null;
        Object element= selection.getFirstElement();
        return getJavaProject(element);
    }
    
    /**
     * For a given element, try to get it's Java project
     * 
     * @param element the element to get the Java project from
     * 
     * @return the Java project of the provided element, or 
     * <code>null</code> if no Java project could be found.
     */
    private IJavaProject getJavaProject(Object element) {
        if(!(element instanceof IAdaptable))
            return null;
        IResource resource= (IResource)((IAdaptable)element).getAdapter(IResource.class);
        if(resource == null) {
            if(element instanceof IPackageFragmentRoot)
                return ((IPackageFragmentRoot)element).getJavaProject(); // this case is necessary for external archives
            if(element instanceof ClassPathContainer)
                return ((ClassPathContainer)element).getJavaProject();
            return null;
        }
        IJavaProject project= JavaCore.create(resource.getProject());
        return project;
    }
    
    /**
     * Find out whether all elements have the 
     * same Java project as parent.
     * 
     * @return <code>true</code> if all elements have the 
     * same Java project as parent, <code>false</code> otherwise.
     */
    protected boolean sameJavaProject() {
        List elements= fOperation.getSelectedElements();
        for(int i= 0; i < elements.size(); i++) {
            IJavaProject project= getJavaProject(elements.get(i));
            if(!fJavaProject.equals(project))
                return false;
        }
        return true;
    }
    
    /**
     * Default getter of the Java project.
     * 
     * @return an Java project
     */
    public IJavaProject getJavaProject() {
        return fJavaProject;
    }

    /**
     * Return an <code>IOutputFolderQuery</code>.
     * 
     * @see ClasspathModifierQueries#getDefaultFolderQuery(Shell, IPath)
     * @see IClasspathInformationProvider#getOutputFolderQuery()
     * 
     * @throws JavaModelException if the project output location can not be retrieved
     */
    public IOutputFolderQuery getOutputFolderQuery() throws JavaModelException {
        return ClasspathModifierQueries.getDefaultFolderQuery(getShell(), fJavaProject.getOutputLocation().makeRelative());
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
        List classpathEntries= ClasspathModifier.getExistingEntries(fJavaProject);
        return ClasspathModifierQueries.getDefaultOutputLocationQuery(getShell(), fJavaProject.getOutputLocation().makeRelative(), classpathEntries);
    }

    /**
     * Return an <code>IFolderCreationQuery</code>.
     * 
     * @see ClasspathModifierQueries#getDefaultFolderCreationQuery(Shell, Object)
     * @see IClasspathInformationProvider#getFolderCreationQuery()
     */
    public IFolderCreationQuery getFolderCreationQuery() {
        IStructuredSelection selection= (IStructuredSelection)getSelection();
        return ClasspathModifierQueries.getDefaultFolderCreationQuery(getShell(), selection.getFirstElement());
    }

    /**
     * Get a query to create a linked source folder.
     * 
     * @see ILinkToQuery
     * @see org.eclipse.jdt.internal.corext.buildpath.IClasspathInformationProvider
     */
    public ILinkToQuery getLinkFolderQuery() throws JavaModelException {
        return ClasspathModifierQueries.getDefaultLinkQuery(getShell(), fJavaProject, fJavaProject.getOutputLocation().makeRelative());
    }
    
    /**
     * Return an <code>IAddArchivesQuery</code>.
     * 
     * @see ClasspathModifierQueries#getDefaultArchivesQuery(Shell)
     * @see IClasspathInformationProvider#getExternalArchivesQuery()
     */
    public IAddArchivesQuery getExternalArchivesQuery() throws JavaModelException {
        return ClasspathModifierQueries.getDefaultArchivesQuery(getShell());
    }
    
    /**
     * Return an <code>IAddLibrariesQuery</code>.
     * 
     * @see ClasspathModifierQueries#getDefaultLibrariesQuery(Shell)
     * @see IClasspathInformationProvider#getLibrariesQuery()
     */
    public IAddLibrariesQuery getLibrariesQuery() throws JavaModelException {
        return ClasspathModifierQueries.getDefaultLibrariesQuery(getShell());
    }

    /**
     * Does not do anything. Needs to be implemented if 
     * resources should be deleted.
     * 
     * @see IClasspathInformationProvider#deleteCreatedResources()
     */
    public void deleteCreatedResources() {
        // nothing to do
    }
    
    /**
     * Handle the result by selecting the result elements or handle the exception if 
     * the exception was not <code>null</code>.
     */
    public void handleResult(List resultElements, CoreException exception, int operationType) {
        if (exception != null) {
            ExceptionHandler.handle(exception, getShell(), 
                    NewWizardMessages.getFormattedString("HintTextGroup.Exception.Title", //$NON-NLS-1$
                            fOperation.getName()), exception.getMessage());
            return;
        }
        if(resultElements.size() == 0)
            return; // nothing changed
        
        IStructuredSelection selection;
        if(fOperation instanceof EditOutputFolderOperation)
            selection= (StructuredSelection)getSelection();
        else
            selection= new StructuredSelection(resultElements);
        selectAndReveal(selection, getSite());
    }
    
    /**
     * For a given selection, try to select and reveal this selection for 
     * all parts of this site.
     * 
     * @param selection the elements to be selected
     * @param site a workbench site
     */
    protected static void selectAndReveal(final ISelection selection, IWorkbenchSite site) {
        // validate the input
        IWorkbenchPage page= site.getPage();
        if (page == null)
            return;

        // get all the view and editor parts
        List parts = new ArrayList();
        IWorkbenchPartReference refs[] = page.getViewReferences();
        for (int i = 0; i < refs.length; i++) {
            IWorkbenchPart part = refs[i].getPart(false);
            if (part != null)
                parts.add(part);
        }
        refs = page.getEditorReferences();
        for (int i = 0; i < refs.length; i++) {
            if (refs[i].getPart(false) != null)
                parts.add(refs[i].getPart(false));
        }

        Iterator itr = parts.iterator();
        while (itr.hasNext()) {
            IWorkbenchPart part = (IWorkbenchPart) itr.next();

            // get the part's ISetSelectionTarget implementation
            ISetSelectionTarget target = null;
            if (part instanceof ISetSelectionTarget)
                target = (ISetSelectionTarget) part;
            else
                target = (ISetSelectionTarget) part
                        .getAdapter(ISetSelectionTarget.class);

            if (target != null) {
                // select and reveal resource
                final ISetSelectionTarget finalTarget = target;
                page.getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        finalTarget.selectReveal(selection);
                    }
                });
            }
        }
    }
}
