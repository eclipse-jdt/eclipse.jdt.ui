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
package org.eclipse.jdt.internal.corext.buildpath;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ArchiveFileFilter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock.IRemoveOldBinariesQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IAddArchivesQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IAddLibrariesQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IFolderCreationQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IInclusionExclusionQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.ILinkToQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputLocationQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.OutputFolderValidator;

public class ClasspathModifier {
    
    /**
     * Interface for listeners that want to receive a notification about 
     * changes on <code>IClasspathEntry</code>. For example, if a source 
     * folder changes one of it's inclusion/exclusion filters, then 
     * this event will be fired.
     */
    public static interface IClasspathModifierListener { 
        /**
         * The new build path entry that was generated upon calling a method of 
         * <code>ClasspathModifier</code>. The type indicates which kind of 
         * interaction was executed on this entry.
         * 
         * Note that the list does not contain elements of type 
         * <code>IClasspathEntry</code>, but <code>CPListElement</code>
         * 
         * @param newEntries list of <code>CPListElement</code>
         */
        public void classpathEntryChanged(List newEntries);
    }
    
    private IClasspathModifierListener fListener;
    
    protected ClasspathModifier() {
        this(null);
    }
    
    protected ClasspathModifier(IClasspathModifierListener listener) {
        fListener= listener;
    }
    
    /**
     * Create a linked source folder.
     * 
     * @param query a query to create a linked source folder
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return a list containing a <code>IPackageFragmentRoot</code> representing 
     * the linked source folder
     * @throws CoreException
     */
    protected List createLinkedSourceFolder(ILinkToQuery query, IJavaProject project, IProgressMonitor monitor) throws CoreException {
        if (query.doQuery()) {
            IFolder folder= query.getCreatedFolder();
            List folderList= new ArrayList();
            folderList.add(folder);
            List root= addToClasspath(folderList, project, query.getOutputFolderQuery(), monitor);
            if (root.size() == 0)
                folder.delete(false, null);
            return root;
        }
        return new ArrayList();
    }
    
    /**
     * Create a folder given a <code>FolderCreationQuery</code>.
     * The query does only have to handle the creation of the folder,
     * filter manipulations are handlet by the <code>
     * Classpathmodifier</code> itself using the return value
     * of <code>FolderCreationQuery.getCreatedFolder()</code>.
     * 
     * @param folderQuery query to create the new folder
     * @param outputQuery query to get information about whether the project should be 
     * removed as source folder and update build folder to <code>outputLocation</code>
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return a list containing the created object (either of type <code>IResource</code>
     * of <code>IJavaElement</code>, or an empty list if no folder was created 
     * (e.g. the operation was cancelled).
     * @throws CoreException 
     * @throws OperationCanceledException 
     * @see ClasspathModifierQueries.IFolderCreationQuery
     * @see ClasspathModifierQueries.IOutputFolderQuery
     */
    protected List createFolder(IFolderCreationQuery folderQuery, IOutputFolderQuery outputQuery, IJavaProject project, IProgressMonitor monitor) throws OperationCanceledException, CoreException{
        if (folderQuery.doQuery()) {
            IFolder folder= folderQuery.getCreatedFolder();
            List folderList= new ArrayList();
            folderList.add(folder);
            if (folderQuery.isSourceFolder()) {
                List root= addToClasspath(folderList, project, outputQuery, monitor);
                if (root.size() == 0)
                    folder.delete(false, null);
                return root;
            } else {
                List entries= getExistingEntries(project);
                exclude(folder.getFullPath(), entries, new ArrayList(), project, monitor);
                updateClasspath(entries, project, null);
            }
            return folderList;
        }
        return new ArrayList();
    }
    
    /**
     * Add a list of elements to the build path.
     * 
     * @param elements a list of elements to be added to the build path. An element 
     * must either be of type <code>IFolder</code>, <code>IJavaElement</code> or 
     * <code>IFile</code> (only allowed if the file is a .jar or .zip file!).
     * @param project the Java project
     * @param query for information about whether the project should be removed as
     * source folder and update build folder
     * @param monitor progress monitor, can be <code>null</code> 
     * @return returns a list of elements of type <code>IPackageFragmentRoot</code> or 
     * <code>IJavaProject</code> that have been added to the build path or an 
     * empty list if the operation was aborted
     * @throws CoreException 
     * @throws OperationCanceledException 
     * @see ClasspathModifierQueries.IOutputFolderQuery
     */
    protected List addToClasspath(List elements, IJavaProject project, IOutputFolderQuery query, IProgressMonitor monitor) throws OperationCanceledException, CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.AddToBuildpath"), 2 * elements.size() + 3); //$NON-NLS-1$
            IWorkspaceRoot workspaceRoot= JavaPlugin.getWorkspace().getRoot();
            
            if (project.getProject().hasNature(JavaCore.NATURE_ID)) {
                IPath outputLocation= project.getOutputLocation();
                IPath projPath= project.getProject().getFullPath();
                List existingEntries= getExistingEntries(project);
                if (!(elements.size() == 1 && elements.get(0) instanceof IJavaProject) && //if only the project should be added, then the query does not need to be executed 
                        (outputLocation.equals(projPath) || query.getDesiredOutputLocation().segmentCount() == 1)) {
                    if (query.doQuery(false, getValidator(elements, project), project)) {
                        project.setOutputLocation(query.getOutputLocation(), null);
                        // remove the project
                        if (query.removeProjectFromClasspath()) {
                            removeFromClasspath(project, existingEntries, null);
                        }
                        
                        IRemoveOldBinariesQuery reorgQuery= BuildPathsBlock.getRemoveOldBinariesQuery(null);
                        if (BuildPathsBlock.hasClassfiles(project.getProject()) && outputLocation.equals(projPath) && reorgQuery.doQuery(projPath)) {
                            IResource res= workspaceRoot.findMember(outputLocation);
                            if (res instanceof IContainer && BuildPathsBlock.hasClassfiles(res)) {
                                BuildPathsBlock.removeOldClassfiles(res);
                            }
                        }
                        outputLocation= project.getOutputLocation();
                    } else
                        return new ArrayList();
                }
                
                List newEntries= new ArrayList();
                for(int i= 0; i < elements.size(); i++) {
                    Object element= elements.get(i);
                    CPListElement entry;
                    if (element instanceof IResource)
                        entry= addToClasspath((IResource)element, existingEntries, newEntries, project, monitor);
                    else
                        entry= addToClasspath((IJavaElement)element, existingEntries, newEntries, project, monitor);
                    newEntries.add(entry);
                }
                
                setNewEntry(existingEntries, newEntries, project, new SubProgressMonitor(monitor, 1));
                
                updateClasspath(existingEntries, project, new SubProgressMonitor(monitor, 1));
                
                List result= new ArrayList();
                for(int i= 0; i < newEntries.size(); i++) {
                    IClasspathEntry entry= ((CPListElement)newEntries.get(i)).getClasspathEntry();
                    IJavaElement root;
                    if (entry.getPath().equals(project.getPath()))
                        root= project;
                    else
                        root= project.findPackageFragmentRoot(entry.getPath());
                    result.add(root);
                }
                
                return result;
            }
            else {
                StatusInfo rootStatus= new StatusInfo();
                rootStatus.setError(NewWizardMessages.getString("ClasspathModifier.Error.NoNatures")); //$NON-NLS-1$
                throw new CoreException(rootStatus);
            }
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Add external archives (.jar and .zip files) to the buildpath. The 
     * method uses the query to find out which entries need to be added. 
     * 
     * @param query the query to get the information which entries need to be added
     * @param project the java project 
     * @param monitor progress monitor, can be <code>null</code> 
     * @return a list of <code>IPackageFragmentRoot</code>s representing the added 
     * archives or an empty list if no element was added.
     * @throws CoreException
     * 
     * @see IAddArchivesQuery
     */
    protected List addExternalJars(IAddArchivesQuery query, IJavaProject project, IProgressMonitor monitor) throws CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IPath[] selected= query.doQuery();
        List addedEntries= new ArrayList();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.AddToBuildpath"), 4); //$NON-NLS-1$
            if (selected != null) {
                for (int i= 0; i < selected.length; i++) {
                    addedEntries.add(new CPListElement(project, IClasspathEntry.CPE_LIBRARY, selected[i], null));
                }
                monitor.worked(1);
                
                List existingEntries= getExistingEntries(project);
                setNewEntry(existingEntries, addedEntries, project, new SubProgressMonitor(monitor, 1));
                updateClasspath(existingEntries, project, new SubProgressMonitor(monitor, 1));
                
                List result= new ArrayList(addedEntries.size());
                for(int i= 0; i < addedEntries.size(); i++) {
                    IClasspathEntry entry= ((CPListElement)addedEntries.get(i)).getClasspathEntry();
                    result.add(project.findPackageFragmentRoot(entry.getPath()));
                }
                monitor.worked(1);
                return result;
            }
        } finally {
            monitor.done();
        }
        return new ArrayList();
    }
    
    /**
     * Add libraries to the buildpath. The 
     * method uses the query to find out which entries need to be added. 
     * 
     * @param query the query to get the information which entries need to be added
     * @param project the java project 
     * @param monitor progress monitor, can be <code>null</code> 
     * @return a list of <code>ClasspathContainer</code>s representing the added 
     * archives or an empty list if no element was added.
     * @throws CoreException
     * 
     * @see IAddArchivesQuery
     */
    protected List addLibraries(IAddLibrariesQuery query, IJavaProject project, IProgressMonitor monitor) throws CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IClasspathEntry[] selected= query.doQuery(project, project.getRawClasspath());
        List addedEntries= new ArrayList();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.AddToBuildpath"), 4); //$NON-NLS-1$
            if (selected != null) {
                for (int i= 0; i < selected.length; i++) {
                    addedEntries.add(new CPListElement(project, IClasspathEntry.CPE_CONTAINER, selected[i].getPath(), null));
                }
                monitor.worked(1);
                
                List existingEntries= getExistingEntries(project);
                setNewEntry(existingEntries, addedEntries, project, new SubProgressMonitor(monitor, 1));
                updateClasspath(existingEntries, project, new SubProgressMonitor(monitor, 1));
                
                List result= new ArrayList(addedEntries.size());
                for(int i= 0; i < addedEntries.size(); i++) {
                    result.add(new ClassPathContainer(project, selected[i]));
                }
                monitor.worked(1);
                return result;
            }
        } finally {
            monitor.done();
        }
        return new ArrayList();
    }
    
    
    /**
     * Remove a list of elements to the build path.
     * 
     * @param elements a list of elements to be removed from the build path. An element 
     * must either be of type <code>IJavaProject</code>, <code>IPackageFragmentRoot</code> or 
     * <code>ClassPathContainer</code>
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code> 
     * @return returns a list of elements of type <code>IFile</code> (in case of removed archives) or 
     * <code>IFolder</code> that have been removed from the build path
     * @throws CoreException 
     * @throws OperationCanceledException 
     */
    protected List removeFromClasspath(List elements, IJavaProject project, IProgressMonitor monitor) throws CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.RemoveFromBuildpath"), elements.size() + 1); //$NON-NLS-1$
            List existingEntries= getExistingEntries(project);
            List resultElements= new ArrayList();
            
            boolean archiveRemoved= false;
            for(int i= 0; i < elements.size(); i++) {
                Object element= elements.get(i);
                if (element instanceof IJavaProject) {
                    resultElements.add(removeFromClasspath(project, existingEntries, new SubProgressMonitor(monitor, 1)));
                }
                else if (element instanceof IPackageFragmentRoot){
                    IPackageFragmentRoot root= (IPackageFragmentRoot)element;
                    if(isExternalArchiveOrLibrary(CPListElement.createFromExisting(root.getRawClasspathEntry(), project), project)) {
                        archiveRemoved= true;
                        removeFromClasspath(root, existingEntries, project, new SubProgressMonitor(monitor, 1));
                    }
                    else
                        resultElements.add(removeFromClasspath(root, existingEntries, project, new SubProgressMonitor(monitor, 1)));
                }
                else {
                    archiveRemoved= true;
                    ClassPathContainer container= (ClassPathContainer)element;
                    existingEntries.remove(CPListElement.createFromExisting(container.getClasspathEntry(), project));
                }
            }
            
            updateClasspath(existingEntries, project, new SubProgressMonitor(monitor, 1));
            fireEvent(existingEntries);
            if(archiveRemoved && resultElements.size() == 0)
                resultElements.add(project);
            return resultElements;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Include a list of elements to the build path. This means that the inclusion filter for the
     * corresponding <code>IPackageFragmentRoot</code>s need to be modified.
     * All elements must be either be of type <code>IResource</code> 
     * or <code>IJavaElement</code>.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param elements a list of elements to be included. The elements must be either of type
     * <code>IResource</code> or <code>IJavaElement</code>.
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return a list of <code>IJavaElement</code>s corresponding to the included ones.
     * @throws JavaModelException
     * 
     * @see #exclude(List, IJavaProject, IProgressMonitor)
     */
    protected List include(List elements, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Including"), 2 * elements.size()); //$NON-NLS-1$
            
            List existingEntries= getExistingEntries(project);
            List resources= new ArrayList();
            for(int i= 0; i < elements.size(); i++) {
                IResource resource;        
                if (elements.get(i) instanceof IResource)
                    resource= (IResource)elements.get(i);
                else {
                    IJavaElement elem= (IJavaElement)elements.get(i);
                    resource= elem.getResource();
                }
                resources.add(resource);
                IPackageFragmentRoot root= getFragmentRoot(resource, project, new SubProgressMonitor(monitor, 1));
                CPListElement entry= getClasspathEntry(existingEntries, root);
                
                include(resource, entry, project, new SubProgressMonitor(monitor, 1));
            }
            
            updateClasspath(existingEntries, project, new SubProgressMonitor(monitor, 4));
            List javaElements= getCorrespondingElements(resources, project);
            return javaElements;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Exclude a list of <code>IJavaElement</code>s. This means that the exclusion filter for the
     * corresponding <code>IPackageFragmentRoot</code>s needs to be modified.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param javaElements list of Java elements to be excluded
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return list of objects representing the excluded elements
     * @throws JavaModelException
     */
    protected List exclude(List javaElements, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Excluding"), javaElements.size() + 4); //$NON-NLS-1$
            
            List existingEntries= getExistingEntries(project);
            List resources= new ArrayList();
            for(int i= 0; i < javaElements.size(); i++) {
                IJavaElement javaElement= (IJavaElement)javaElements.get(i);
                IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
                CPListElement entry= getClasspathEntry(existingEntries, root);
                
                IResource resource= exclude(javaElement, entry, project, new SubProgressMonitor(monitor, 1));
                resources.add(resource);
            }
            
            updateClasspath(existingEntries, project, new SubProgressMonitor(monitor, 4));
            return resources;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Inverse operation to include.
     * The <code>IJavaElement</code>s in the list will be removed from
     * their fragment roots inclusion filter.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param javaElements a list of <code>IJavaElements</code> to be unincluded
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return a list of elements representing unexcluded elements 
     * @throws JavaModelException
     * 
     * @see #include(List, IJavaProject, IProgressMonitor)
     */
    protected List unInclude(List javaElements, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.RemoveInclusion"), 10); //$NON-NLS-1$
            
            List existingEntries= getExistingEntries(project);
            for(int i= 0; i < javaElements.size(); i++) {
                IJavaElement javaElement= (IJavaElement)javaElements.get(i);
                IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);  
                CPListElement entry= getClasspathEntry(existingEntries, root);
                
                unInclude(javaElement, entry, project, new SubProgressMonitor(monitor, 1));
            }
            
            updateClasspath(existingEntries, project, new SubProgressMonitor(monitor, 4));
            List result= getCorrespondingElements(javaElements, project);
            return result;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Inverse operation to <code>exclude</code>.
     * The list of elements of type <code>IResource</code> will be 
     * removed from the exclusion filters of their parent roots.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param elements list of <code>IResource</code>s to be unexcluded
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an object representing the unexcluded element 
     * @throws JavaModelException
     * 
     * @see #exclude(List, IJavaProject, IProgressMonitor)
     * @see #unExclude(List, IJavaProject, IProgressMonitor)
     */
    protected List unExclude(List elements, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Including"), 2 * elements.size()); //$NON-NLS-1$
            
            List entries= getExistingEntries(project);
            for(int i= 0; i < elements.size(); i++) {
                IResource resource= (IResource)elements.get(i);
                IPackageFragmentRoot root= getFragmentRoot(resource, project, new SubProgressMonitor(monitor, 1));
                CPListElement entry= getClasspathEntry(entries, root);
                
                unExclude(resource, entry, project, new SubProgressMonitor(monitor, 1));
            }
            
            updateClasspath(entries, project, new SubProgressMonitor(monitor, 4));
            List resultElements= getCorrespondingElements(elements, project);
            return resultElements;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Edit the filters of a given <code>IJavaElement</code> by using the 
     * passed <code>IInclusionExclusionQuery</code>.
     * 
     * @param element the Java element to edit the filters on. Must be either of
     * type <code>IJavaProject</code> or <code>IPackageFragmentRoot</code>.
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return returns the edited Java element or <code>null</code> if the operation was
     * cancelled
     * @throws JavaModelException
     */
    protected IJavaElement editFilters(IJavaElement element, IJavaProject project, IInclusionExclusionQuery query, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.EditInclusionExclusionFilters"), 4); //$NON-NLS-1$
            CPListElement entry;
            List existingEntries= getExistingEntries(project);
            entry= getListElement(element.getPath(), existingEntries);
            if (query.doQuery(entry, false)) {
                entry.setAttribute(CPListElement.INCLUSION, query.getInclusionPattern());
                entry.setAttribute(CPListElement.EXCLUSION, query.getExclusionPattern());
                updateClasspath(existingEntries, project, new SubProgressMonitor(monitor, 4));
                return element;
            }
        } finally {
            monitor.done();
        }
        return null;
    }
    
    /**
     * Creates an output folder for the given fragment root.
     * 
     * Note: the folder is also created in the file system.
     * Therefore clients have to ensure that is is deleted if
     * necessary because the <code>ClasspathModifier</code> does
     * not handle this.
     * 
     * @param root the fragment root to create the ouptut folder for
     * @param query a query to get information about the output
     * location that should be used for a given element
     * @param project the current Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return a CPListElementAttribute object representing the new folder or <code>null</code>
     * if the folder was not created (e.g. because the user cancelled the creation)
     * @throws CoreException 
     * @see #editOutputFolder(CPListElement, IJavaProject, IOutputLocationQuery, IProgressMonitor)
     * @see ClasspathModifierQueries.IOutputLocationQuery
     */
    protected CPListElementAttribute createOutputFolder(IPackageFragmentRoot root, IOutputLocationQuery query, IJavaProject project, IProgressMonitor monitor) throws CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IClasspathEntry entry= root.getRawClasspathEntry();
        CPListElement elem= CPListElement.createFromExisting(entry, project);
        return editOutputFolder(elem, project, query, monitor);
    }
    
    /**
     * Edit the output folder entry for a given <code>
     * CPListElement</code> which corresponds to
     * a <code>IPackageFragmentRoot</code>. The <code>
     * IOutputLocationQuery</code> provides the information
     * necessary to edit this entry.
     * 
     * Note: a folder can be created in the file system.
     * Therefore clients have to ensure that is is deleted if
     * necessary because the <code>ClasspathModifier</code> does
     * not handle this.
     * 
     * @param element an element representing the output folder's parent
     * @param project the Java project
     * @param query Query to get information about the output
     * location that should be used for a given element
     * @param monitor progress monitor, can be <code>null</code>
     * @return an attribute representing the modified output folder, or
     * <code>null</code> if editing was cancelled
     * @throws CoreException 
     * @see #createOutputFolder(IPackageFragmentRoot, IOutputLocationQuery, IJavaProject, IProgressMonitor)
     * @see CPListElement#createFromExisting(IClasspathEntry, IJavaProject)
     */
    protected CPListElementAttribute editOutputFolder(CPListElement element, IJavaProject project, IOutputLocationQuery query, IProgressMonitor monitor) throws CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        List entries= getExistingEntries(project);
        element= getClasspathEntry(entries, element);
        if (query.doQuery(element)) {
            IOutputFolderQuery outputFolderQuery= query.getOutputFolderQuery(query.getOutputLocation());
            if (outputFolderQuery.getDesiredOutputLocation().segmentCount() == 1) {
                if (!outputFolderQuery.doQuery(true, getTrueValidator(project), project))
                    return null;
                project.setOutputLocation(outputFolderQuery.getOutputLocation(), null);
                if (outputFolderQuery.removeProjectFromClasspath()) {
                    removeFromClasspath(project, entries, null);
                }
            }
            if (query.getOutputLocation() == null) {
                CPListElementAttribute attr= resetOutputFolder(element, project);
                updateClasspath(entries, project, new NullProgressMonitor());
                return attr;
            }
            exclude(query.getOutputLocation(), entries, new ArrayList(), project, null);
            element.setAttribute(CPListElement.OUTPUT, query.getOutputLocation());
            CPListElementAttribute outputFolder= new CPListElementAttribute(element, CPListElement.OUTPUT, 
                    element.getAttribute(CPListElement.OUTPUT));
            updateClasspath(entries, project, new NullProgressMonitor());
            return outputFolder;
        }
        return null;
    }
    
    /**
     * Reset all output folder for the given Java project.
     * 
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     */
    protected void resetOutputFolders(IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.ResetOutputFolder"), roots.length + 10); //$NON-NLS-1$
            List entries= new ArrayList();
            for(int i= 0; i < roots.length; i++) {
                monitor.worked(1);
                if (roots[i].isArchive())
                    continue;
                IClasspathEntry entry= roots[i].getRawClasspathEntry();
                CPListElement element= CPListElement.createFromExisting(entry, project);
                CPListElementAttribute outputFolder= new CPListElementAttribute(element, CPListElement.OUTPUT, 
                        element.getAttribute(CPListElement.OUTPUT));
                entries.add(outputFolder);
            }
            reset(entries, project, new SubProgressMonitor(monitor, 10));
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Reset a list of elements. The elements can be either of type 
     * <li><code>IJavaProject</code></li>
     * <li><code>IPackageFragmentRoot</code></li>
     * <li><code>CPListElementAttribute</code></li><br>
     * 
     * Depending on the element, resetting performs two different operations:
     * <li>On <code>IJavaProject</code> or <code>IPackageFragmentRoot</code>, the 
     * inclusion and exclusion filters are reset. Only entries in the filters that 
     * correspond to either source folders or output folders will not be 
     * removed (to prevent damage on the project layout)</li>
     * <li>On <code>CPListElementAttribute</code>, the output location of the 
     * given attribute is reset to the default output location.</li>
     * 
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return a list of elements representing the elements on which 'reset' was called. 
     * They can either be of type <code>CPListElement</code>, <code>IJavaProject</code> or 
     * <code>IPackageFragmentRoot</code>
     */
    protected List reset(List elements, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Resetting"), elements.size()); //$NON-NLS-1$
            List entries= getExistingEntries(project);
            List result= new ArrayList();
            for(int i= 0; i < elements.size(); i++) {
                Object element= elements.get(i);
                if (element instanceof IJavaElement) {
                    IJavaElement javaElement= (IJavaElement)element;
                    IPackageFragmentRoot root;
                    if (element instanceof IJavaProject)
                        root= project.getPackageFragmentRoot(project.getUnderlyingResource());
                    else
                        root= (IPackageFragmentRoot)element;
                    CPListElement entry= getClasspathEntry(entries, root);
                    resetFilters(javaElement, entry, project, new SubProgressMonitor(monitor, 1));
                    result.add(javaElement);
                } else {
                    CPListElement selElement=  ((CPListElementAttribute)element).getParent();
                    CPListElement entry= getClasspathEntry(entries, selElement);
                    CPListElementAttribute outputFolder= resetOutputFolder(entry, project);
                    result.add(outputFolder);
                }
            }
            
            updateClasspath(entries, project, null);
            fireEvent(entries);
            return result;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Get the <code>IClasspathEntry</code> from the project and 
     * convert it into a list of <code>CPListElement</code>s.
     * 
     * @param project the Java project to get it's build path entries from
     * @return a list of <code>CPListElement</code>s corresponding to the 
     * build path entries of the project
     * @throws JavaModelException
     */
    public static List getExistingEntries(IJavaProject project) throws JavaModelException {
        IClasspathEntry[] classpathEntries= project.getRawClasspath();
        ArrayList newClassPath= new ArrayList();
        for (int i= 0; i < classpathEntries.length; i++) {
            IClasspathEntry curr= classpathEntries[i];
            newClassPath.add(CPListElement.createFromExisting(curr, project));
        }
        return newClassPath;
    }

    /**
     * Try to find the corresponding and modified <code>CPListElement</code> for the root 
     * in the list of elements and return it.
     * If no one can be found, the roots <code>ClasspathEntry</code> is converted to a 
     * <code>CPListElement</code> and returned.
     * 
     * @param elements a list of <code>CPListElements</code>
     * @param root the root to find the <code>ClasspathEntry</code> for represented by 
     * a <code>CPListElement</code>
     * @return the <code>CPListElement</code> found in the list (matching by using the path) or 
     * the roots own <code>IClasspathEntry</code> converted to a <code>CPListElement</code>.
     * @throws JavaModelException
     */
    public static CPListElement getClasspathEntry(List elements, IPackageFragmentRoot root) throws JavaModelException {
        IClasspathEntry entry= root.getRawClasspathEntry();
        for(int i= 0; i < elements.size(); i++) {
            CPListElement element= (CPListElement)elements.get(i);
            if(element.getPath().equals(root.getPath()) && element.getEntryKind() == entry.getEntryKind())
                return (CPListElement)elements.get(i);
        }
        CPListElement newElement= CPListElement.createFromExisting(entry, root.getJavaProject());
        elements.add(newElement);
        return newElement;
    }
    
    /**
     * For a given <code>IResource</code>, try to
     * convert it into a <code>IPackageFragmentRoot</code>
     * if possible or return <code>null</code> if no
     * fragment root could be created.
     * 
     * @param resource the resource to be converted
     * @return the <code>resource<code> as
     * <code>IPackageFragment</code>,or <code>null</code>
     * if failed to convert
     */
    public static IPackageFragment getFragment(IResource resource) {
        IJavaElement elem= JavaCore.create(resource);
        if (elem instanceof IPackageFragment)
            return (IPackageFragment) elem;
        return null;
    }
    
    /**
     * Get the source folder of a given <code>IResource</code> element,
     * starting with the resource's parent.
     * 
     * @param resource the resource to get the fragment root from
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return resolved fragment root
     * @throws JavaModelException
     */
    public static IPackageFragmentRoot getFragmentRoot(IResource resource, IJavaProject project, IProgressMonitor monitor) throws JavaModelException{
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IJavaElement javaElem= null;
        if (resource.getFullPath().equals(project.getPath()))
            return project.getPackageFragmentRoot(project.getUnderlyingResource());
        IContainer container= resource.getParent();
        do {
            if (container instanceof IFolder)
                javaElem= JavaCore.create((IFolder)container);
            if (container.getFullPath().equals(project.getPath())) {
                javaElem= project;
                break;
            }
            container= container.getParent();
            if (container == null)
                return null;
        } while (javaElem == null || !(javaElem instanceof IPackageFragmentRoot));
        if (javaElem instanceof IJavaProject)
            javaElem= project.getPackageFragmentRoot(project.getUnderlyingResource());
        return (IPackageFragmentRoot) javaElem;
    }
    
    /**
     * Get the <code>IClasspathEntry</code> for the
     * given path by looking up all
     * build path entries on the project
     * 
     * @param path the path to find a build path entry for
     * @param project the Java project
     * @return the <code>IClasspathEntry</code> corresponding
     * to the <code>path</code> or <code>null</code> if there
     * is no such entry
     * @throws JavaModelException
     */
    public static IClasspathEntry getClasspathEntryFor(IPath path, IJavaProject project, int entryKind) throws JavaModelException {
        IClasspathEntry[] entries= project.getRawClasspath();
        for(int i= 0; i < entries.length; i++) {
            IClasspathEntry entry= entries[i];
            if (entry.getPath().equals(path) && equalEntryKind(entry, entryKind))
                return entry;
        }
        return null;
    }
    
    /**
     * Check whether the current selection is the project's 
     * default output folder or not
     * 
     * @param attrib the attribute to be checked
     * @return <code>true</code> if is the default output folder,
     * <code>false</code> otherwise.
     */
    public static boolean isDefaultOutputFolder(CPListElementAttribute attrib) {
        return attrib.getValue() == null;
    }
    
    /**
     * Determines whether the current selection (of type
     * <code>ICompilationUnit</code> or <code>IPackageFragment</code>)
     * is on the inclusion filter of it's parent source folder.
     * 
     * @param selection the current Java element
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return <code>true</code> if the current selection is included,
     * <code>false</code> otherwise.
     * @throws JavaModelException 
     */
    public static boolean isIncluded(IJavaElement selection, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.ContainsPath"), 4); //$NON-NLS-1$
            IPackageFragmentRoot root= (IPackageFragmentRoot) selection.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
            IClasspathEntry entry= root.getRawClasspathEntry();
            if(entry == null)
                return false;
            return contains(selection.getPath().removeFirstSegments(root.getPath().segmentCount()), entry.getInclusionPatterns(), new SubProgressMonitor(monitor, 2));
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Find out whether the <code>IResource</code> excluded or not.
     * 
     * @param resource the resource to be checked
     * @param project the Java project
     * @return <code>true</code> if the resource is excluded, <code>
     * false</code> otherwise
     * @throws JavaModelException
     */
    public static boolean isExcluded(IResource resource, IJavaProject project) throws JavaModelException {
        IPackageFragmentRoot root= getFragmentRoot(resource, project, null);
        if (root == null)
            return false;
        String fragmentName= getName(resource.getFullPath(), root.getPath());
        fragmentName= completeName(fragmentName);
        IClasspathEntry entry= root.getRawClasspathEntry();
        return entry != null && contains(new Path(fragmentName), entry.getExclusionPatterns(), null);
    }
    
    /**
     * Find out whether one of the <code>IResource</code>'s parents
     * is excluded.
     * 
     * @param resource check the resources parents whether they are
     * excluded or not
     * @param project the Java project
     * @return <code>true</code> if there is an excluded parent, 
     * <code>false</code> otherwise
     * @throws JavaModelException
     */
    public static boolean parentExcluded(IResource resource, IJavaProject project) throws JavaModelException {
        if (resource.getFullPath().equals(project.getPath()))
            return false;
        IPackageFragmentRoot root= getFragmentRoot(resource, project, null);
        IPath path= resource.getFullPath().removeFirstSegments(root.getPath().segmentCount());
        IClasspathEntry entry= root.getRawClasspathEntry();
        if (entry == null)
            return true; // there is no build path entry, this is equal to the fact that the parent is excluded
        while (path.segmentCount() > 0) {
            if (contains(path, entry.getExclusionPatterns(), null))
                return true;
            path= path.removeLastSegments(1);
        }
        return false;
    }
    
    /**
     * Check wheter the output location of the <code>IPackageFragmentRoot</code>
     * is <code>null</code>. If this holds, then the root 
     * does use the default output folder.
     * 
     * @param root the root to examine the output location for
     * @return <code>true</code> if the root uses the default output folder, <code>false
     * </code> otherwise.
     * @throws JavaModelException
     */
    public static boolean hasDefaultOutputFolder(IPackageFragmentRoot root) throws JavaModelException {
        return root.getRawClasspathEntry().getOutputLocation() == null;
    }
    
    /**
     * Check whether at least one source folder of the given
     * Java project has an output folder set.
     * 
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return <code>true</code> if at least one outputfolder
     * is set, <code>false</code> otherwise
     * @throws JavaModelException 
     */
    public static boolean hasOutputFolders(IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.CheckOutputFolders"), roots.length); //$NON-NLS-1$
            for(int i= 0; i < roots.length; i++) {
                if (roots[i].getRawClasspathEntry().getOutputLocation() != null)
                    return true;
                monitor.worked(1);
            }
        } finally {
            monitor.done();
        }
        return false;
    }
    
    /**
     * Check whether the <code>IPackageFragment</code>
     * corresponds to the project's default fragment.
     * 
     * @param fragment the package fragment to be checked
     * @return <code>true</code> if is the default package fragment,
     * <code>false</code> otherwise.
     */
    public static boolean isDefaultFragment(IPackageFragment fragment ) {
        return fragment.getElementName().length() == 0;
    }
    
    /**
     * Determines whether the inclusion filter of the element's source folder is empty
     * or not
     * @return <code>true</code> if the inclusion filter is empty,
     * <code>false</code> otherwise.
     * @throws JavaModelException 
     */
    public static boolean includeFiltersEmpty(IResource resource, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.ExamineInputFilters"), 4); //$NON-NLS-1$
            IPackageFragmentRoot root= getFragmentRoot(resource, project, new SubProgressMonitor(monitor, 4));
            IClasspathEntry entry= root.getRawClasspathEntry();
            return entry.getInclusionPatterns().length == 0;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Check whether the input paramenter of type <code>
     * IPackageFragmentRoot</code> has either it's inclusion or
     * exclusion filter or both set (that means they are
     * not empty).
     * 
     * @param root the fragment root to be inspected
     * @return <code>true</code> inclusion or exclusion filter set,
     * <code>false</code> otherwise.
     */
    public static boolean filtersSet(IPackageFragmentRoot root) throws JavaModelException {
        if(root == null)
            return false;
        IClasspathEntry entry= root.getRawClasspathEntry();
        IPath[] inclusions= entry.getInclusionPatterns();
        IPath[] exclusions= entry.getExclusionPatterns();
        if (inclusions != null && inclusions.length > 0)
            return true;
        if (exclusions != null && exclusions.length > 0)
            return true;
        return false;
    }

    /**
     * Add a resource to the build path.
     * 
     * @param resource the resource to be added to the build path
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code> 
     * @return returns the new element of type <code>IPackageFragmentRoot</code> that has been added to the build path
     * @throws CoreException 
     * @throws OperationCanceledException 
     */
    private CPListElement addToClasspath(IResource resource, List existingEntries, List newEntries, IJavaProject project, IProgressMonitor monitor) throws OperationCanceledException, CoreException{
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.AddToBuildpath"), 2); //$NON-NLS-1$
            exclude(resource.getFullPath(), existingEntries, newEntries, project, new SubProgressMonitor(monitor, 1));
            int entryKind= resource instanceof IFolder ? IClasspathEntry.CPE_SOURCE : IClasspathEntry.CPE_LIBRARY;
            CPListElement entry= new CPListElement(project, entryKind, resource.getFullPath(), resource);
            return entry;
        } finally {
            monitor.done();
        }
    }

    /**
     * Check whether the provided file is an archive (.jar or .zip).
     * 
     * @param file the file to be checked
     * @param project the Java project
     * @return <code>true</code> if the file is an archive, <code>false</code> 
     * otherwise
     * @throws JavaModelException
     */
    public static boolean isArchive(IFile file, IJavaProject project) throws JavaModelException {
        if (!ArchiveFileFilter.isArchivePath(file.getFullPath()))
            return false;
        if (project != null && project.exists() && (project.findPackageFragmentRoot(file.getFullPath()) == null))
            return true;
        return false;
    }

    /**
     * Add a Java element to the build path.
     * 
     * @param javaElement element to be added to the build path
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code> 
     * @return returns the new element of type <code>IPackageFragmentRoot</code> that has been added to the build path
     * @throws CoreException 
     * @throws OperationCanceledException 
     */
    private CPListElement addToClasspath(IJavaElement javaElement, List existingEntries, List newEntries, IJavaProject project, IProgressMonitor monitor) throws OperationCanceledException, CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.AddToBuildpath"), 10); //$NON-NLS-1$
            CPListElement entry= new CPListElement(project, IClasspathEntry.CPE_SOURCE, javaElement.getPath(), javaElement.getUnderlyingResource());
            return entry;
        } finally {
            monitor.done();
        }
    }

    /**
     * Remove the Java project from the build path
     * 
     * @param project the project to be removed
     * @param existingEntries a list of existing <code>CPListElement</code>. This list 
     * will be traversed and the entry for the project will be removed.
     * @param monitor progress monitor, can be <code>null</code>
     * @return returns the Java project
     * @throws CoreException
     */
    private IJavaProject removeFromClasspath(IJavaProject project, List existingEntries, IProgressMonitor monitor) throws CoreException {
        CPListElement elem= getListElement(project.getPath(), existingEntries);
        existingEntries.remove(elem);
        return project;
    }

    /**
     * Remove a given <code>IPackageFragmentRoot</code> from the build path.
     * 
     * @param root the <code>IPackageFragmentRoot</code> to be removed from the build path
     * @param existingEntries a list of <code>CPListElements</code> representing the build path 
     * entries of the project. The entry for the root will be looked up and removed from the list.
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return returns the <code>IResource</code> that has been removed from the build path; 
     * is of type <code>IFile</code> if the root was an archive, otherwise <code>IFolder</code>
     */
    private IResource removeFromClasspath(IPackageFragmentRoot root, List existingEntries, IJavaProject project, IProgressMonitor monitor) throws CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.RemoveFromBuildpath"), 1); //$NON-NLS-1$
            IClasspathEntry entry= root.getRawClasspathEntry();
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            existingEntries.remove(elem);
            IResource resource;
            if(root.isArchive())
                resource= project.getProject().getWorkspace().getRoot().getFile(root.getPath());
            else
                resource= project.getProject().getWorkspace().getRoot().getFolder(root.getPath());
            return resource;
        } finally {
            monitor.done();
        }
    }

    /**
     * Include the given <code>IResource</code>. This means that the inclusion filter for the
     * corresponding <code>IPackageFragmentRoot</code> needs to be modified.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param resource the element to be included
     * @param entry the <code>CPListElement</code> representing the 
     * <code>IClasspathEntry</code> of the resource's root
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     *
     * @throws JavaModelException
     * 
     * @see #exclude(List, IJavaProject, IProgressMonitor)
     */
    private void include(IResource resource, CPListElement entry, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Including"), 10); //$NON-NLS-1$
            
            String name= getName(resource.getFullPath(), entry.getPath());        
            
            IPath[] includedPath= (IPath[]) entry.getAttribute(CPListElement.INCLUSION);
            IPath[] newIncludedPath= new IPath[includedPath.length + 1];
            String completedName= completeName(name);
            IPath relPath= new Path(completedName);
            if (!contains(relPath, includedPath, new SubProgressMonitor(monitor, 2))) {
                System.arraycopy(includedPath, 0, newIncludedPath, 0, includedPath.length);
                newIncludedPath[includedPath.length]= relPath;
                entry.setAttribute(CPListElement.INCLUSION, newIncludedPath);
                entry.setAttribute(CPListElement.EXCLUSION, remove(relPath, (IPath[]) entry.getAttribute(CPListElement.EXCLUSION), new SubProgressMonitor(monitor, 2)));
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Exclude an element with a given name and absolute path
     * from the build path.
     * 
     * @param name the name of the element to be excluded
     * @param fullPath the absolute path of the element
     * @param entry the build path entry to be modified
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return a <code>IResource</code> corresponding to the excluded element
     * @throws JavaModelException 
     */
    private IResource exclude(String name, IPath fullPath, CPListElement entry, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IResource result;
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Excluding"), 6); //$NON-NLS-1$
            IPath[] excludedPath= (IPath[]) entry.getAttribute(CPListElement.EXCLUSION);
            IPath[] newExcludedPath= new IPath[excludedPath.length + 1];
            name= completeName(name);
            IPath path= new Path(name);
            if (!contains(path, excludedPath, new SubProgressMonitor(monitor, 2))) {
                System.arraycopy(excludedPath, 0, newExcludedPath, 0, excludedPath.length);
                newExcludedPath[excludedPath.length]= path;
                entry.setAttribute(CPListElement.EXCLUSION, newExcludedPath);
                entry.setAttribute(CPListElement.INCLUSION, remove(path, (IPath[]) entry.getAttribute(CPListElement.INCLUSION), new SubProgressMonitor(monitor, 4)));
            }
            result= fullPath == null ? null : getResource(fullPath, project);
        } finally {
            monitor.done();
        }
        return result;
    }

    /**
     * Exclude an object at a given path.
     * This means that the exclusion filter for the
     * corresponding <code>IPackageFragmentRoot</code> needs to be modified.
     * 
     * First, the fragment root needs to be found. To do so, the new entries 
     * are and the existing entries are traversed for a match and the entry 
     * with the path is removed from one of those lists.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param path absolute path of an object to be excluded
     * @param existingEntries a list of existing build path entries
     * @param newEntries a list of new build path entries
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     */
    private void exclude(IPath path, List existingEntries, List newEntries, IJavaProject project, IProgressMonitor monitor) throws JavaModelException{
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Excluding"), 1); //$NON-NLS-1$
            CPListElement elem= null;
            CPListElement existingElem= null;
            int i= 0;
            do {
                i++;
                IPath rootPath= path.removeLastSegments(i);
                
                if (rootPath.segmentCount() == 0)
                    return;
                
                elem= getListElement(rootPath, newEntries);
                existingElem= getListElement(rootPath, existingEntries);
            } while (existingElem == null && elem == null);
            if (elem == null) {
                elem= existingElem;
            }
            exclude(path.removeFirstSegments(path.segmentCount() - i).toString(), null, elem, project, new SubProgressMonitor(monitor, 1)); //$NON-NLS-1$
        } finally {
            monitor.done();
        }
    }

    /**
     * Exclude a <code>IJavaElement</code>. This means that the exclusion filter for the
     * corresponding <code>IPackageFragmentRoot</code>s need to be modified.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param javaElement the Java element to be excluded
     * @param entry the <code>CPListElement</code> representing the 
     * <code>IClasspathEntry</code> of the Java element's root.
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * 
     * @return the resulting <code>IResource<code>
     * @throws JavaModelException
     */
    private IResource exclude(IJavaElement javaElement, CPListElement entry, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            String name= getName(javaElement.getPath(), entry.getPath());
            return exclude(name, javaElement.getPath(), entry, project, new SubProgressMonitor(monitor, 1));
        } finally {
            monitor.done();
        }
    }

    /**
     * Inverse operation to <code>include</code>. The provided 
     * <code>IJavaElement</code> will be removed from the inclusion 
     * filters of it's root.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param javaElement the Java element to be unincluded
     * @param entry the <code>CPListElement</code> representing the 
     * <code>IClasspathEntry</code> of the root.
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @throws JavaModelException
     * 
     * @see #include(List, IJavaProject, IProgressMonitor)
     */
    private void unInclude(IJavaElement javaElement, CPListElement entry, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.RemoveInclusion"), 10); //$NON-NLS-1$                   
            String name= getName(javaElement.getPath(), entry.getPath());  
            
            IPath[] includedPath= (IPath[]) entry.getAttribute(CPListElement.INCLUSION);
            IPath relPath= new Path(completeName(name));
            IPath[] newIncludedPath= remove(relPath, includedPath, new SubProgressMonitor(monitor, 3));
            entry.setAttribute(CPListElement.INCLUSION, newIncludedPath);
        } finally {
            monitor.done();
        }
    }

    /**
     * Inverse operation to <code>exclude</code>.
     * The resource removed from it's fragment roots exlusion filter.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param resource the resource to be unexcluded
     * @param entry the <code>CPListElement</code> representing the 
     * <code>IClasspathEntry</code> of the resource's root.
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @throws JavaModelException
     * 
     * @see #exclude(List, IJavaProject, IProgressMonitor)
     */
    private void unExclude(IResource resource, CPListElement entry, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.RemoveExclusion"), 10); //$NON-NLS-1$
            String name= getName(resource.getFullPath(), entry.getPath());
            IPath[] excludedPath= (IPath[]) entry.getAttribute(CPListElement.EXCLUSION);
            IPath[] newExcludedPath= remove(new Path(completeName(name)), excludedPath, new SubProgressMonitor(monitor, 3));
            entry.setAttribute(CPListElement.EXCLUSION, newExcludedPath);
        } finally {
            monitor.done();
        }
    }

    /**
     * Resets inclusion and exclusion filters for the given
     * <code>IJavaElement</code>
     * 
     * @param element element to reset it's filters
     * @param entry the <code>CPListElement</code> to reset its filters for
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @throws JavaModelException
     */
    private void resetFilters(IJavaElement element, CPListElement entry, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.ResetFilters"), 3); //$NON-NLS-1$
            
            List exclusionList= getFoldersOnCP(element.getPath(), project, new SubProgressMonitor(monitor, 2));
            IPath outputLocation= (IPath)entry.getAttribute(CPListElement.OUTPUT);
            if (outputLocation != null) {
                IPath[] exclusionPatterns= (IPath[])entry.getAttribute(CPListElement.EXCLUSION);
                if (contains(new Path(completeName(outputLocation.lastSegment())), exclusionPatterns, null)) {
                    exclusionList.add(new Path(completeName(outputLocation.lastSegment())));
                }
            }
            IPath[] exclusions= (IPath[]) exclusionList.toArray(new IPath[exclusionList.size()]);
            
            entry.setAttribute(CPListElement.INCLUSION, new IPath[0]);
            entry.setAttribute(CPListElement.EXCLUSION, exclusions);
        } finally {
            monitor.done();
        }
    }

    /**
     * Reset the output folder for the given entry to the default output folder
     * 
     * @param entry the <code>CPListElement</code> to be edited
     * @param project the Java project
     * @return an attribute representing the modified output folder
     * @throws JavaModelException 
     */
    private CPListElementAttribute resetOutputFolder(CPListElement entry, IJavaProject project) throws JavaModelException {
        entry.setAttribute(CPListElement.OUTPUT, null);
        CPListElementAttribute outputFolder= new CPListElementAttribute(entry, CPListElement.OUTPUT, 
                entry.getAttribute(CPListElement.OUTPUT));
        return outputFolder;
    }

    /**
     * Try to find the corresponding and modified <code>CPListElement</code> for the provided 
     * <code>CPListElement</code> in the list of elements and return it.
     * If no one can be found, the provided <code>CPListElement</code> is returned.
     * 
     * @param elements a list of <code>CPListElements</code>
     * @param cpElement the <code>CPListElement</code> to find the corresponding entry in 
     * the list
     * @return the <code>CPListElement</code> found in the list (matching by using the path) or 
     * the second <code>CPListElement</code> parameter itself if there is no match.
     * @throws JavaModelException
     */
    private CPListElement getClasspathEntry(List elements, CPListElement cpElement) throws JavaModelException {
        for(int i= 0; i < elements.size(); i++) {
            if(((CPListElement)elements.get(i)).getPath().equals(cpElement.getPath()))
                return (CPListElement)elements.get(i);
        }
        elements.add(cpElement);
        return cpElement;
    }

    /**
     * For a given path, find the corresponding element in the list.
     * 
     * @param path the path to found an entry for
     * @param elements a list of <code>CPListElement</code>s
     * @return the mathed <code>CPListElement</code> or <code>null</code> if 
     * no match could be found
     */
    private CPListElement getListElement(IPath path, List elements) {
        for(int i= 0; i < elements.size(); i++) {
            CPListElement element= (CPListElement)elements.get(i);
            if (element.getEntryKind() == IClasspathEntry.CPE_SOURCE &&
                    element.getPath().equals(path)) {
                return element;
            }
        }
        return null;
    }
    
    /**
     * Updates the build path if changes have been applied to a
     * build path entry. For example, this can be necessary after
     * having edited some filters on a build path entry, which can happen
     * when including or excluding an object.
     * 
     * @param newEntries a list of <code>CPListElements</code> that should be used 
     * as build path entries for the project.
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @throws JavaModelException in case that validation for the new entries fails
     */
    private void updateClasspath(List newEntries, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            IClasspathEntry[] entries= convert(newEntries);
            IPath outputLocation= project.getOutputLocation();
            
            IJavaModelStatus status= JavaConventions.validateClasspath(project, entries, outputLocation);
            if (!status.isOK())
               throw new JavaModelException(status);
            
            project.setRawClasspath(entries, outputLocation, new SubProgressMonitor(monitor, 2));
            fireEvent(newEntries);
        } finally {
            monitor.done();
        }
    }
    
    /**
     * For a given list of entries, find out what representation they 
     * will have in the project and return a list with corresponding 
     * elements.
     * 
     * @param entries a list of entries to find an appropriate representation 
     * for. The list can contain elements of two types: 
     * <li><code>IResource</code></li>
     * <li><code>IJavaElement</code></li>
     * @param project the Java project
     * @return a list of elements corresponding to the passed entries.
     */
    private List getCorrespondingElements(List entries, IJavaProject project) {
        List result= new ArrayList();
        for(int i= 0; i < entries.size(); i++) {
            Object element= entries.get(i);
            IPath path;
            if (element instanceof IResource)
                path= ((IResource)element).getFullPath();
            else
                path= ((IJavaElement)element).getPath();
            IResource resource= getResource(path, project);
            IJavaElement elem= JavaCore.create(resource);
            if (elem != null && project.isOnClasspath(elem))
                result.add(elem);
            else
                result.add(resource);
            
        }
        return result;
    }
    
    /**
     * Returns for the given absolute path the corresponding
     * resource, this is either element of type <code>IFile</code>
     * or <code>IFolder</code>.
     *  
     * @param path an absolute path to a resource
     * @param project the Java project
     * @return the resource matching to the path. Can be
     * either an <code>IFile</code> or an <code>IFolder</code>.
     */
    private IResource getResource(IPath path, IJavaProject project) {
        return project.getProject().getWorkspace().getRoot().findMember(path);
    }
    
    /**
     * Find out whether the provided path equals to one
     * in the array.
     * 
     * @param path path to find an equivalent for
     * @param paths set of paths to compare with
     * @param monitor progress monitor, can be <code>null</code>
     * @return <code>true</code> if there is an occurrence, <code>
     * false</code> otherwise
     */
    private static boolean contains(IPath path, IPath[] paths, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        if (path == null)
            return false;
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.ComparePaths"), paths.length); //$NON-NLS-1$
            if (path.getFileExtension() == null)
                path= new Path(completeName(path.toString())); //$NON-NLS-1$
            for (int i=0; i < paths.length; i++) {
                if (paths[i].equals(path))
                    return true;
                monitor.worked(1);
            }
        } finally {
           monitor.done();
       }            
       return false;
    }
    
    /**
     * Add a '/' at the end of the name if
     * it does not end with '.java'.
     * 
     * @param name append '/' at the end if
     * necessary
     * @return modified string
     */
    private static String completeName(String name) {
        if (!name.endsWith(".java")) { //$NON-NLS-1$
            name= name + "/"; //$NON-NLS-1$
            name= name.replace('.', '/');
            return name;
        }
        return name;
    }
    
    /**
     * Removes <code>path</code> out of the set of given <code>
     * paths</code>. If the path is not contained, then the 
     * initially provided array of paths is returned.
     * 
     * Only the first occurrence will be removed.
     * 
     * @param path path to be removed
     * @param paths array of path to apply the removal on
     * @param monitor progress monitor, can be <code>null</code>
     * @return array which does not contain <code>path</code>
     */
    private IPath[] remove(IPath path, IPath[] paths, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IPath[] newPaths;
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.RemovePath"), paths.length + 5); //$NON-NLS-1$
            if (!contains(path, paths, new SubProgressMonitor(monitor, 5)))
                return paths;
            
            newPaths= new Path[paths.length - 1];
            int j= 0;
            for (int i=0; i < paths.length; i++) {
                monitor.worked(1);
                if (!paths[i].equals(path))
                    newPaths[j++]= paths[i];            
            }
        } finally {
            monitor.done();
        }
        return newPaths;
    }
    
    /**
     * Find all folders that are on the build path and
     * <code>path</code> is a prefix of those folders
     * path entry, that is, all folders which are a
     * subfolder of <code>path</code>.
     * 
     * For example, if <code>path</code>=/MyProject/src 
     * then all folders having a path like /MyProject/src/*,
     * where * can be any valid string are returned if
     * they are also on the project's build path.
     * 
     * @param path absolute path
     * @param project the Java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an array of paths which belong to subfolders
     * of <code>path</code> and which are on the build path
     * @throws JavaModelException
     */
    private List getFoldersOnCP(IPath path, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        List srcFolders= new ArrayList();
        IClasspathEntry[] cpEntries= project.getRawClasspath();
        for(int i= 0; i < cpEntries.length; i++) {
            IPath cpPath= cpEntries[i].getPath();
            if (path.isPrefixOf(cpPath) && path.segmentCount() + 1 == cpPath.segmentCount())
                srcFolders.add(new Path(completeName(cpPath.lastSegment())));
        }
        return srcFolders;
    }
    
    /**
     * Returns a string corresponding to the <code>path</code>
     * with the <code>rootPath<code>'s number of segments
     * removed
     * 
     * @param path path to remove segments
     * @param rootPath provides the number of segments to
     * be removed
     * @return a string corresponding to the mentioned
     * action
     */
    private static String getName(IPath path, IPath rootPath) {
        return path.removeFirstSegments(rootPath.segmentCount()).toString();
    }
    
    /**
     * Sets and validates the new entries. Note that the elments of 
     * the list containing the new entries will be added to the list of 
     * existing entries (therefore, there is no return list for this method).
     * 
     * @param existingEntries a list of existing classpath entries
     * @param newEntries a list of entries to be added to the existing ones
     * @param project the Java project
     * @param monitor a progress monitor, can be <code>null</code>
     * @throws CoreException in case that validation on one of the new entries fails
     */
    private void setNewEntry(List existingEntries, List newEntries, IJavaProject project, IProgressMonitor monitor) throws CoreException {
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.SetNewEntry"), existingEntries.size()); //$NON-NLS-1$
            for (int i= 0; i < newEntries.size(); i++) {
                CPListElement entry= (CPListElement)newEntries.get(i);
                validateAndAddEntry(entry, existingEntries, project);
                monitor.worked(1);
            }
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Convert a list of <code>CPListElement</code>s to 
     * an array of <code>IClasspathEntry</code>.
     * 
     * @param list the list to be converted
     * @return an array containing build path entries 
     * corresponding to the list
     */
    private IClasspathEntry[] convert(List list) {
        IClasspathEntry[] entries= new IClasspathEntry[list.size()];
        for(int i= 0; i < list.size(); i++) {
            CPListElement element= (CPListElement)list.get(i);
            entries[i]= element.getClasspathEntry();
        }
        return entries;
    }
    
    /**
     * Validate the new entry in the context of the existing entries. Furthermore, 
     * check if exclusion filters need to be applied and do so if necessary.
     * 
     * If validation was successfull, add the new entry to the list of existing entries.
     * 
     * @param entry the entry to be validated and added to the list of existing entries.
     * @param existingEntries a list of existing entries representing the build path
     * @param project the Java project
     * @throws CoreException in case that validation fails
     */
    private void validateAndAddEntry(CPListElement entry, List existingEntries, IJavaProject project) throws CoreException {
        IPath path= entry.getPath();
        IPath projPath= project.getProject().getFullPath();
        IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
        IStatus validate= workspaceRoot.getWorkspace().validatePath(path.toString(), IResource.FOLDER);
        StatusInfo rootStatus= new StatusInfo();
        rootStatus.setOK();
        boolean isExternal= isExternalArchiveOrLibrary(entry, project);
        if (!isExternal && validate.matches(IStatus.ERROR) && !project.getPath().equals(path)) {
            rootStatus.setError(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.error.InvalidRootName", validate.getMessage())); //$NON-NLS-1$
            throw new CoreException(rootStatus);
        } else {
            if (!isExternal && !project.getPath().equals(path)) {
                IResource res= workspaceRoot.findMember(path);
                if (res != null) {
                    if (res.getType() != IResource.FOLDER && res.getType() != IResource.FILE) {
                        rootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.NotAFolder")); //$NON-NLS-1$
                        throw new CoreException(rootStatus);
                    }
                } else {
                    IPath projLocation= project.getProject().getLocation();
                    if (projLocation != null && path.toFile().exists()) {
                        rootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.AlreadyExistingDifferentCase")); //$NON-NLS-1$
                        throw new CoreException(rootStatus);
                    }
                }
            }
            
            for (int i= 0; i < existingEntries.size(); i++) {
                CPListElement curr= (CPListElement)existingEntries.get(i);
                if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    if (path.equals(curr.getPath()) && !project.getPath().equals(path)) {
                        rootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.AlreadyExisting")); //$NON-NLS-1$
                        throw new CoreException(rootStatus);
                    }
                }
                if (curr.getPath().matchingFirstSegments(path) == path.segmentCount()) {
                    exclude(curr.getPath().removeFirstSegments(path.segmentCount()).toString(), null, entry, project, null);
                }
            }
            
            if(!isExternal && !entry.getPath().equals(project.getPath()))
                exclude(entry.getPath(), existingEntries, new ArrayList(), project, null);
            
            IPath outputLocation= project.getOutputLocation();
            existingEntries.add(entry);            
            
            IClasspathEntry[] entries= convert(existingEntries);
            
            IJavaModelStatus status= JavaConventions.validateClasspath(project, entries, outputLocation);
            if (!status.isOK()) {
                if (outputLocation.equals(projPath)) {
                    IStatus status2= JavaConventions.validateClasspath(project, entries, outputLocation);
                    if (status2.isOK()) {
                      if (project.isOnClasspath(project.getUnderlyingResource())) {
                          rootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceSFandOL", outputLocation.makeRelative().toString())); //$NON-NLS-1$
                      } else {
                          rootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceOL", outputLocation.makeRelative().toString())); //$NON-NLS-1$
                      }
                      return;
                    }
                }
                rootStatus.setError(status.getMessage());
                throw new CoreException(rootStatus);
            }
            
            if (getClasspathEntryFor(project.getPath(), project, IClasspathEntry.CPE_SOURCE) != null || project.getPath().equals(path)) {
                rootStatus.setWarning(NewWizardMessages.getString("NewSourceFolderWizardPage.warning.ReplaceSF")); //$NON-NLS-1$
                return;
            }
            
            rootStatus.setOK();
            return;
        }
    }
    
    public boolean isExternalArchiveOrLibrary(CPListElement entry, IJavaProject project) {
        if(entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY || entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
            if(entry.getPath().matchingFirstSegments(project.getPath()) != 1)
                return true;
        }
        return false;
    }
    
    /**
     * Test if the provided kind is of type
     * <code>IClasspathEntry.CPE_SOURCE</code>
     * 
     * @param entry the classpath entry to be compared with the provided type
     * @param kind the kind to be checked
     * @return <code>true</code> if kind equals
     * <code>IClasspathEntry.CPE_SOURCE</code>, 
     * <code>false</code> otherwise
     */
    private static boolean equalEntryKind(IClasspathEntry entry, int kind) {
        return entry.getEntryKind() == kind;
    }
    
    /**
     * Event fired whenever build pathentries changed.
     * The event parameter corresponds to the 
     * a <code>List</code> of <code>CPListElement</code>s
     * 
     * @param newEntries
     * 
     * @see #addToClasspath(List, IJavaProject, IOutputFolderQuery, IProgressMonitor)
     * @see #removeFromClasspath(List, IJavaProject, IProgressMonitor)
     */
    private void fireEvent(List newEntries) {
        if (fListener != null)
            fListener.classpathEntryChanged(newEntries);
    }
    
    private OutputFolderValidator getTrueValidator(IJavaProject project) throws JavaModelException {
        return new OutputFolderValidator(null, project) {
            public boolean validate(IPath outputLocation) {
                return true;
            }
        };
    }
    
    private OutputFolderValidator getValidator(final List newElements, final IJavaProject project) throws JavaModelException {
        return new OutputFolderValidator(newElements, project) {

            public boolean validate(IPath outputLocation) {
                for(int i= 0; i < newElements.size(); i++) {
                    if (isInvalid(newElements.get(i), outputLocation))
                        return false;
                }
                
                for(int i= 0; i < fEntries.length; i++) {
                    if (isInvalid(fEntries[i], outputLocation))
                        return false;
                }
                return true;
            }
            
            /**
             * Check if the output location for the given object is valid
             * 
             * @param object the object to retrieve its path from and compare it 
             * to the output location
             * @param outputLocation the output location
             * @return <code>true</code> if the output location is invalid, that is, 
             * if it is a subfolder of the provided object.
             */
            private boolean isInvalid(Object object, IPath outputLocation) {
                IPath path= null;
                if (object instanceof IFolder)
                    path= getFolderPath(object);
                else if (object instanceof IJavaElement)
                    path= getJavaElementPath(object);
                else if (object instanceof IClasspathEntry)
                    path= getCPEntryPath(object);
                return isSubFolderOf(path, outputLocation);
            }
            
            /**
             * Get an <code>IFolder</code>'s path
             * 
             * @param element an element which is of type <code>IFolder</code>
             * @return the path of the folder
             */
            private IPath getFolderPath(Object element) {
                return ((IFolder)element).getFullPath();
            }
            
            /**
             * Get an <code>IJavaElement</code>'s path
             * 
             * @param element an element which is of type <code>IJavaElement</code>
             * @return the path of the Java element
             */
            private IPath getJavaElementPath(Object element) {
                return ((IJavaElement)element).getPath();
            }
            
            /**
             * Get an <code>IClasspathEntry</code>'s path
             * 
             * @param entry an element which is of type <code>IClasspathEntry</code>
             * @return the path of the classpath entry
             */
            private IPath getCPEntryPath(Object entry) {
                return ((IClasspathEntry)entry).getPath();
            }
            
            /**
             * 
             * @param path1 the first path
             * @param path2 the second path
             * @return <code>true</code> if path1 is a subfolder of 
             * path2, <code>false</code> otherwise
             */
            private boolean isSubFolderOf(IPath path1, IPath path2) {
                if (path1 == null || path2 == null) {
                    if (path1 == null && path2 == null)
                        return true;
                    return false;
                }
                return path2.matchingFirstSegments(path1) == path2.segmentCount();
            }
            
        };
    }
}
