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
package org.eclipse.jdt.internal.ui.wizards;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ExclusionInclusionDialog;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.OutputLocationDialog;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock.IRemoveOldBinariesQuery;

public class ClasspathModifier extends Observable {
    /**
     * Query to get information about whether the project should be removed as
     * source folder and update build folder to <code>outputLocation</code>
     */
    public static interface IOutputFolderQuery {
        /**
         * Query to get information about whether the project should be removed as
         * source folder and update build folder to <code>outputLocation</code>
         * 
         * @param useNeverButton <code>true</code> if additionally to 'yes' and 'no'
         * there should be a 'never' button, <code>false</code> otherwise
         * @param outputLocation the output location to be set for the project
         * @param project the java project
         * @return <code>true</code> if the project should be removed as source folder
         * and the output folder should be updated, <code>false</code> otherwise.
         */
        boolean doQuery(final boolean useNeverButton, final String outputLocation, final IJavaProject project);
    }
    
    public static interface IInclusionExclusionQuery {
        /**
         * Query to get information about the
         * inclusion and exclusion filters of
         * an element.
         * 
         * While executing <code>doQuery</code>,
         * these filter might change.
         * 
         * On calling <code>getInclusionPatter</code>
         * or <code>getExclusionPattern</code> it
         * is expected to get the new and updated
         * filters back.
         * 
         * @param element the element to get the
         * information from
         * @param focusOnExcluded
         * @return <code>true</code> if changes
         * have been accepted and <code>getInclusionPatter</code>
         * or <code>getExclusionPattern</code> can
         * be called.
         */
        boolean doQuery(CPListElement element, boolean focusOnExcluded);
        
        /**
         * Can only be called after <code>
         * doQuery</code> has been executed and
         * has returned <code>true</code>
         * 
         * @return the inclusion filters
         */
        public IPath[] getInclusionPattern();
        
        /**
         * Can only be called after <code>
         * doQuery</code> has been executed and
         * has returned <code>true</code>
         *
         * @return the exclusion filters
         */
        public IPath[] getExclusionPattern();
    }
    
    /**
     * Query to get information about the output
     * location that should be used for a 
     * given element.
     */
    public static interface IOutputLocationQuery {
        /**
         * Query to get information about the output
         * location that should be used for a 
         * given element.
         * 
         * @param element the element to get
         * an output location for
         * @return <code>true</code> if the output
         * location has changed, <code>false</code>
         * otherwise.
         */
        boolean doQuery(CPListElement element);
        
        /**
         * Gets the new output location.
         * 
         * May only be called after having
         * executed <code>doQuery</code> which
         * must have returned <code>true</code>
         * 
         * @return the new output location
         */
        public IPath getOutputLocation();
        
        /**
         * Get a query for information about whether the project should be removed as
         * source folder and update build folder
         * 
         * @return query giving information about output and source folders
         * 
         * @see IOutputFolderQuery
         */
        public IOutputFolderQuery getOutputFolderQuery();
    }
    
    /**
     * Query to create a folder.
     */
    public static interface IFolderCreationQuery {
        /**
         * Query to create a folder.
         * 
         * Clients need to implement their own
         * initialization methods so that
         * <code>doQuery</code> can be executed
         * safely on behalf of this
         * initialization
         * 
         * @return <code>true</code> if the operation
         * was successful (e.g. no cancelled), <code>
         * false</code> otherwise
         */
        boolean doQuery();
        
        /**
         * Find out whether a source folder is about
         * to be created or a normal folder which
         * is not on the classpath (and therefore
         * might have to be excluded).
         * 
         * Should only be called after having executed
         * <code>doQuery</code>, because otherwise
         * it might not be sure if a result exists or
         * not.
         * 
         * @return <code>true</code> if a source
         * folder should be created, <code>false
         * </code> otherwise
         */
        boolean isSourceFolder();
        
        /**
         * Get the newly created folder.
         * This method is only valid after having
         * called <code>doQuery</code>.
         * 
         * @return the created folder of type
         * <code>IFolder</code>
         */
        IFolder getCreatedFolder();
    }
    
    private IPath fOutputLocation;
    private IClasspathEntry[] fEntries;
    private IClasspathEntry[] fNewEntries;
    private IPath fNewOutputLocation;
    private boolean fNeverShowDialog= false;
    
    public ClasspathModifier() {
    }
    
    /**
     * Create a folder given a <code>FolderCreationQuery</code>.
     * The query does only have to handle the creation of the folder,
     * filter manipulations are handlet by the <code>
     * Classpathmodifier</code> itself using the return value
     * of <code>FolderCreationQuery.getCreatedFolder</code>.
     * 
     * @param folderQuery query to create the new folder
     * @param outputQuery query to get information about whether the project should be 
     * removed as source folder and update build folder to <code>outputLocation</code>
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return the created object (either of type <code>IResource</code>
     * of <code>IJavaElement</code>, or <code>null</code> if 
     * no folder was created.
     * @throws JavaModelException
     * 
     * @see IFolderCreationQuery
     * @see IOutputFolderQuery
     */
    public Object createFolder(IFolderCreationQuery folderQuery, IOutputFolderQuery outputQuery, IJavaProject project, IProgressMonitor monitor) throws JavaModelException{
        if (folderQuery.doQuery()) {
            IFolder folder= folderQuery.getCreatedFolder();
            if (folderQuery.isSourceFolder())
                return addToClasspath(folder, project, outputQuery, monitor);
            
            exclude(folder.getFullPath(), project, monitor);
            return folder;
        }
        return null;
    }
    
    /**
     * Adds a folder to the classpath.
     * 
     * @param folder folder to be added to the classpath, must not exist
     * @param project the java project
     * @param query for information about whether the project should be removed as
     * source folder and update build folder
     * @param monitor progress monitor, can be <code>null</code> 
     * @return returns the new element of type <code>IPackageFragmentRoot</code> that has been added to the classpath or
     * <code>null</code> if an internal exception happened
     * @see IOutputFolderQuery
     */
    public IPackageFragmentRoot addToClasspath(IFolder folder, IJavaProject project, IOutputFolderQuery query, IProgressMonitor monitor) throws JavaModelException{
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IPackageFragmentRoot result;
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.AddToCP"), 4); //$NON-NLS-1$
            exclude(folder.getFullPath(), project, new SubProgressMonitor(monitor, 4));
            result= addToClasspath(folder.getFullPath(), project, query, new SubProgressMonitor(monitor, 4));
        } finally {
            monitor.done();
        }
        return result;
    }
    
    /**
     * Adds a java element to the classpath.
     * 
     * @param javaElement element to be added to the classpath, must not exist
     * @param project the java project
     * @param query for information about whether the project should be removed as
     * source folder and update build folder
     * @param monitor progress monitor, can be <code>null</code> 
     * @return returns the new element of type <code>IPackageFragmentRoot</code> that has been added to the classpath or
     * <code>null</code> if an internal exception happened
     * @see IOutputFolderQuery
     */
    public IPackageFragmentRoot addToClasspath(IJavaElement javaElement, IJavaProject project, IOutputFolderQuery query, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IPackageFragmentRoot result;
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.AddToCP"), 10); //$NON-NLS-1$
            IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
            exclude(javaElement.getElementName(), null, CPListElement.createFromExisting(root.getRawClasspathEntry(), project), project, new SubProgressMonitor(monitor, 3));
            result= addToClasspath(javaElement.getPath(), project, query, new SubProgressMonitor(monitor, 7));
        } finally {
            monitor.done();
        }
        return result;
    }
    
    /**
     * Removes an element with a given <code>path</code>from the classpath.
     * If the path is not on the classpath, then nothing is removed and
     * <code>null</code> is returned.
     * 
     * @param path the absolute path of the item to be removed from the classpath. An example is:
     * path= /MyProject/SomeFolder/FolderX. This will remove 'FolderX' from the classpath
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return returns the new element of type <code>IPackageFragmentRoot</code> that has been added to the classpath or
     * <code>null</code> if an exception happened or no element had to be excluded
     * @see #fireEvent(IClasspathEntry)
     */
    public Object removeFromClasspath(IPath path, IJavaProject project, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.RemoveFromCP"), 5); //$NON-NLS-1$
            IClasspathEntry entry= null;
            fEntries= project.getRawClasspath();
            ArrayList newEntries= new ArrayList();
            for (int i= 0; i < fEntries.length; i++) {
                if (!fEntries[i].getPath().equals(path))
                    newEntries.add(fEntries[i]);
                else
                    entry= fEntries[i];
            }
            fNewEntries= (IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]);
            fNewOutputLocation= project.getOutputLocation();
            
            project.setRawClasspath(fNewEntries, fNewOutputLocation, new SubProgressMonitor(monitor, 3));
            
            fireEvent(entry);
            
            if (!path.equals(project.getPath())) {
                IFolder folder= project.getProject().getWorkspace().getRoot().getFolder(path);
                return folder;
            }
        } catch (CoreException e) {
            JavaPlugin.log(e);
        } finally {
            monitor.done();
        }
        return null;
    }
    
    /**
     * Includes an object. This means that the inclusion filter for the
     * corresponding <code>IPackageFragmentRoot</code> needs to be modified.
     * The type of the object to be included must either be <code>IResource
     * </code> or <code>IJavaElement</code>.
     * 
     * @param toBeIncluded the element to be included. Must be either of type
     * <code>IResource</code> or <code>IJavaElement</code>.
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an <code>IJavaElement</code> corresponding to the included one.
     * For example if the object to be included was previously a folder of type 
     * <code>IResource</code>, then the returned java element would be of
     * type <code>IPackageFragment</code> which is a subtype of <code>
     * IJavaElement</code>.
     * @throws JavaModelException
     * 
     * @see #exclude(IJavaElement, IJavaProject, IProgressMonitor)
     */
    public IJavaElement include(Object toBeIncluded, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IJavaElement newElement;
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.Including"), 10); //$NON-NLS-1$
            IResource resource;        
            if (toBeIncluded instanceof IResource)
                resource= (IResource)toBeIncluded;
            else {
                IJavaElement elem= (IJavaElement)toBeIncluded;
                resource= elem.getResource();
            }
            
            IPath path= resource.getFullPath();
            IPackageFragmentRoot javaElem= getFragmentRoot(resource, project, new SubProgressMonitor(monitor, 2));
            String name= getName(path, javaElem.getPath());
            
            IClasspathEntry entry= javaElem.getRawClasspathEntry();
            CPListElement elem= CPListElement.createFromExisting(entry, project);        
            
            IPath[] includedPath= (IPath[]) elem.getAttribute(CPListElement.INCLUSION);
            IPath[] newIncludedPath= new IPath[includedPath.length + 1];
            String completedName= completeName(name);
            IPath relPath= new Path(completedName);
            if (!contains(relPath, includedPath, new SubProgressMonitor(monitor, 2))) {
                System.arraycopy(includedPath, 0, newIncludedPath, 0, includedPath.length);
                newIncludedPath[includedPath.length]= relPath;
                elem.setAttribute(CPListElement.INCLUSION, newIncludedPath);
                elem.setAttribute(CPListElement.EXCLUSION, remove(relPath, (IPath[]) elem.getAttribute(CPListElement.EXCLUSION), new SubProgressMonitor(monitor, 2)));
            }
            newElement= getCorrespondingJavaElement(name, relPath, javaElem);
            updateClasspath(elem.getClasspathEntry(), newElement, project, new SubProgressMonitor(monitor, 4));
        } finally {
            monitor.done();
        }
        return newElement;
    }
    
    /**
     * Exclude a java element. This means that the exclusion filter for the
     * corresponding <code>IPackageFragmentRoot</code> needs to be modified.
     * 
     * @param javaElement java element to be excluded
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an object representing the excluded element
     * @throws JavaModelException
     */
    public Object exclude(IJavaElement javaElement, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        Object result;
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.Excluding"), 5); //$NON-NLS-1$
            IPackageFragmentRoot root= (IPackageFragmentRoot) javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
            IClasspathEntry entry= root.getRawClasspathEntry();
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            String name= getName(javaElement.getPath(), root.getPath());
            result= exclude(name, javaElement.getPath(), elem, project, new SubProgressMonitor(monitor, 5));
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
     * @param path absolute path of an object to be excluded
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     */
    public void exclude(IPath path, IJavaProject project, IProgressMonitor monitor) throws JavaModelException{
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.Excluding"), 5); //$NON-NLS-1$
            if (path.segmentCount() == 2) {
                if (project.isOnClasspath(project.getUnderlyingResource()))
                    modifyProjectFilter(path, project, new SubProgressMonitor(monitor, 5));
                return;
            }
            IClasspathEntry entry= null;
            int i= 0;
            do {
                i++;
                IPath rootPath= path.removeLastSegments(i);
                
                if (rootPath.segmentCount() == 0)
                    return;
                    
                IPackageFragmentRoot root= getPackageFragmentRoot(rootPath, project);
                if (root != null)
                    entry= root.getRawClasspathEntry();
            } while (entry == null);
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            exclude(path.removeFirstSegments(path.segmentCount() - i).toString(), null, elem, project, new SubProgressMonitor(monitor, 5)); //$NON-NLS-1$
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Inverse operation to <code>include</code>.
     * The resource's path will be removed from
     * it's fragment roots inclusion filter.
     * 
     * @param javaElement the resource to be unincluded
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an object representing the unexcluded element 
     * @throws JavaModelException
     * 
     * @see #include(Object, IJavaProject, IProgressMonitor)
     */
    public Object unInclude(IJavaElement javaElement, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        Object result;
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.RemoveInclusion"), 10); //$NON-NLS-1$
            IPath path= javaElement.getPath();
            IPackageFragmentRoot root= getFragmentRoot(javaElement.getResource(), project, new SubProgressMonitor(monitor, 3));                        
            String name= getName(path, root.getPath());  
            
            IClasspathEntry entry= root.getRawClasspathEntry();
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            IPath[] includedPath= (IPath[]) elem.getAttribute(CPListElement.INCLUSION);
            String completedName= completeName(name);
            IPath relPath= new Path(completedName);
            IPath[] newIncludedPath= remove(relPath, includedPath, new SubProgressMonitor(monitor, 3));
            elem.setAttribute(CPListElement.INCLUSION, newIncludedPath);
            IJavaElement newElement= getCorrespondingJavaElement(name, new Path(name), root);
            updateClasspath(elem.getClasspathEntry(), newElement, project, new SubProgressMonitor(monitor, 4));
            result= getRepresentation(newElement, project);
        } finally {
            monitor.done();
        }
        return result;
    }
    
    /**
     * On <code>uninclude</code> or <code>unexclude</code>, the
     * representation of an element might have changed (for example
     * from <code>IJavaElement</code> to <code>IResource</code>).
     * 
     * Note that the javaElements underlying resource should
     * exist. If not, a <code>JavaModelException</code>
     * will be thrown.
     * 
     * <code>getRepresentation</code> tries to find the correct
     * representatino given an <code>IJavaElement</code> and
     * returns it.
     * 
     * @param javaElement the java element to find an appropriate
     * representation for
     * @param project the java project
     * @return the found representation of the element which is either
     * a <code>IResource</code> or a <code>IJavaElement</code>
     * @throws JavaModelException
     */
    private Object getRepresentation(IJavaElement javaElement, IJavaProject project) throws JavaModelException {
        if (!(javaElement instanceof ICompilationUnit)) {
            if (getPackageFragment(javaElement.getUnderlyingResource()) != null)
                return javaElement;
            return javaElement.getCorrespondingResource();
        }
        if (javaElement.getPath().segmentCount() == 2 && includeFiltersEmpty(javaElement.getCorrespondingResource(), project, null))
            return javaElement;
        if (getFragment(javaElement.getCorrespondingResource()) == null)
           return javaElement.getCorrespondingResource();
        return javaElement;
    }
    
    /**
     * Inverse operation to <code>exclude</code>.
     * The resource's path will be removed from
     * it's fragment roots exlusion filter.
     * 
     * @param resource the resource to be unexcluded
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an object representing the unexcluded element 
     * @throws JavaModelException
     * 
     * @see #exclude(IJavaElement, IJavaProject, IProgressMonitor)
     * @see #exclude(IPath, IJavaProject, IProgressMonitor)
     */
    public Object unExclude(IResource resource, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        Object result;
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.RemoveExclusion"), 10); //$NON-NLS-1$
            IPath path= resource.getFullPath();
            IPackageFragmentRoot javaElem= getFragmentRoot(resource, project, new SubProgressMonitor(monitor, 3));
            String name= getName(path, javaElem.getPath());
            IClasspathEntry entry= javaElem.getRawClasspathEntry();
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            IPath[] excludedPath= (IPath[]) elem.getAttribute(CPListElement.EXCLUSION);
            String completedName= completeName(name);
            IPath[] newExcludedPath= remove(new Path(completedName), excludedPath, new SubProgressMonitor(monitor, 3));
            elem.setAttribute(CPListElement.EXCLUSION, newExcludedPath);
            IJavaElement newElement= getCorrespondingJavaElement(name, new Path(name), javaElem);
            updateClasspath(elem.getClasspathEntry(), newElement, project, new SubProgressMonitor(monitor, 4));
            result= getRepresentation(newElement, project);
        } finally {
            monitor.done();
        }
        return result;
    }
    
    /**
     * Opens a dialog to edit the filters (inclusion/exclusion) for the
     * given <code>IJavaElement</code>.
     * 
     * @param element the java element to edit the filters on. Must be either of
     * type <code>IJavaProject</code> or <code>IPackageFragmentRoot</code>.
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return returns the edited element or <code>null</code> if the dialog was
     * cancelled
     * @throws JavaModelException
     */
    public Object editFilters(IJavaElement element, IJavaProject project, IInclusionExclusionQuery query, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IClasspathEntry entry;
        if (element instanceof IJavaProject)
            entry= getClasspathEntryFor(project.getPath(), project);
        else
            entry= ((IPackageFragmentRoot)element).getRawClasspathEntry();
        CPListElement elem= CPListElement.createFromExisting(entry, project);
        if (query.doQuery(elem, false)) {
            elem.setAttribute(CPListElement.INCLUSION, query.getInclusionPattern());
            elem.setAttribute(CPListElement.EXCLUSION, query.getExclusionPattern());
            updateClasspath(elem.getClasspathEntry(), element, project, new NullProgressMonitor());
            return element;
        }
        return null;
    }
    
    /**
     * Updates the classpath if changes have been applied to a
     * classpath entry. For example, this can be necessary after
     * having edited some filters on a classpath entry, which can happen
     * when including or excluding an object.
     * 
     * @param newEntry the new entry to be set on the classpath. It will
     * replace the original one identifying the matching entry
     * by comparing the paths (which are unique for each classpath entry)
     * @param newElement the new element that has been created before having
     * called this method, can be <code>null</code>
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return returns <code>newElement</code>, if <code>newElement</code> was
     * not null, or otherwise the <code>project</code>. If an exception occurrs, 
     * then <code>null</code> will be returned.
     * 
     * @see #include(Object, IJavaProject, IProgressMonitor)
     * @see #exclude(IJavaElement, IJavaProject, IProgressMonitor)
     */
    public Object updateClasspath(IClasspathEntry newEntry, Object newElement, IJavaProject project, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            fEntries= project.getRawClasspath();
            for (int i= 0; i < fEntries.length; i++) {
                if (fEntries[i].getPath().equals(newEntry.getPath()))
                        fEntries[i]= newEntry;
            }
            fNewEntries= fEntries;
            fNewOutputLocation= project.getOutputLocation();
            
            project.setRawClasspath(fNewEntries, fNewOutputLocation, new SubProgressMonitor(monitor, 2));
            fireEvent(newEntry);
            if (newElement != null)
                return newElement;
            else
                return project;
        } catch (CoreException e) {
            JavaPlugin.log(e);
        }
        return null;
    }
    
    /**
     * Check whether at least one source folder of the given
     * java project has an output folder set.
     * 
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return <code>true</code> if at least one outputfolder
     * is set, <code>false</code> otherwise
     */
    public boolean hasOutputFolders(IJavaProject project, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
            for(int i= 0; i < roots.length; i++) {
                if (roots[i].getRawClasspathEntry().getOutputLocation() != null)
                    return true;
            }
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        return false;
    }
    
    /**
     * Creates an output folder for the given fragment root.
     * 
     * @param root the fragment root to create the ouptut folder for
     * @param outputLocation the output location of the project (NOT the one to be set for the
     * selected source folder!)
     * @param query Query to get information about the output
     * location that should be used for a given element
     * @param project the current java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return a CPListElementAttribute object representing the new folder or <null>code</null>
     * if the folder was not created (e.g. because the user cancelled the creation)
     * @throws JavaModelException
     * 
     * @see #editOutputFolder(CPListElement, String, IJavaProject, IOutputLocationQuery, IProgressMonitor)
     * @see IOutputLocationQuery
     */
    public CPListElementAttribute createOutputFolder(IPackageFragmentRoot root, String outputLocation, IOutputLocationQuery query, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        if (!outputLocation.startsWith("/")) //$NON-NLS-1$
            outputLocation= '/' + outputLocation;
        IClasspathEntry entry= root.getRawClasspathEntry();
        CPListElement elem= CPListElement.createFromExisting(entry, project);
        return editOutputFolder(elem, outputLocation, project, query, monitor);
    }
    
    /**
     * Show a dialog for editing an output folder.
     * This includes resetting the folder to the default output
     * folder or choosing another one.
     * 
     * @param element an element representing the output folder
     * @param outputLocation the project's output location to be
     * set. Note: this is not the location for the edited
     * output folder.
     * @param project the java project
     * @param query Query to get information about the output
     * location that should be used for a given element
     * @param monitor progress monitor, can be <code>null</code>
     * @return an attribute representing the modified output folder
     * @throws JavaModelException
     * 
     * @see #createOutputFolder(IPackageFragmentRoot, String, IOutputLocationQuery, IJavaProject, IProgressMonitor)
     */
    public CPListElementAttribute editOutputFolder(CPListElement element, String outputLocation, IJavaProject project, IOutputLocationQuery query, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        if (!outputLocation.startsWith("/")) //$NON-NLS-1$
            outputLocation= '/' + outputLocation; 
        if (query.doQuery(element)) {
            if (project.getPath().equals(project.getOutputLocation())) {
                if (!query.getOutputFolderQuery().doQuery(false, outputLocation, project))
                    return null;
            }
            if (query.getOutputLocation().segmentCount() == 2) {
                if (project.isOnClasspath(project.getUnderlyingResource())) {
                    modifyProjectFilter(query.getOutputLocation(), project, new SubProgressMonitor(monitor, 3));
                }
            }
            else   
                exclude(query.getOutputLocation(), project, null);
            IClasspathEntry newEntry= getClasspathEntryFor(element.getPath(), project);
            element= CPListElement.createFromExisting(newEntry, project);
            element.setAttribute(CPListElement.OUTPUT, query.getOutputLocation());
            newEntry= element.getClasspathEntry();
            CPListElementAttribute outputFolder= new CPListElementAttribute(element, CPListElement.OUTPUT, 
                    element.getAttribute(CPListElement.OUTPUT));
            updateClasspath(newEntry, outputFolder, project, new NullProgressMonitor());
            return outputFolder;
        }
        return null;
    }
    
    /**
     * Reset the output folder for the given classpath
     * entry to the default output folder
     * 
     * @param entry the classpath entry to be edited
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an attribute representing the modified output folder
     */
    public CPListElementAttribute resetOutputFolder(IClasspathEntry entry, IJavaProject project, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        CPListElement elem= CPListElement.createFromExisting(entry, project);
        elem.setAttribute(CPListElement.OUTPUT, null);
        CPListElementAttribute outputFolder= new CPListElementAttribute(elem, CPListElement.OUTPUT, 
                elem.getAttribute(CPListElement.OUTPUT));
        updateClasspath(elem.getClasspathEntry(), outputFolder, project, new NullProgressMonitor());
        return outputFolder;
    }
    
    /**
     * Reset all output folder for the given java
     * project.
     * 
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     */
    public void resetOutputFolders(IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.ResetOutputFolder"), 3 * roots.length); //$NON-NLS-1$
            for(int i= 0; i < roots.length; i++) {
                monitor.worked(1);
                if (roots[i].isArchive())
                    continue;
                IClasspathEntry entry= roots[i].getRawClasspathEntry();
                resetOutputFolder(entry, project, new SubProgressMonitor(monitor, 2));
            }
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Adds an element to the classpath.
     * 
     * @param path the absolute path of the item to be added to the classpath. An example is:
     * path= /MyProject/SomeFolder/FolderX<br>
     * This will add 'FolderX' to the classpath 
     * @param project the java project
     * @param query for information about whether the project should be removed as
     * source folder and update build folder
     * @param monitor progress monitor, can be <code>null</code>
     * @return returns the new element of type <code>IPackageFragmentRoot</code> that has been added to the classpath or
     * <code>null</code> if an internal exception happened
     * 
     * @see IOutputFolderQuery
     */
    private IPackageFragmentRoot addToClasspath(IPath path, IJavaProject project, IOutputFolderQuery query, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            boolean removeProjectAsRoot= false;
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.AddToCP"), 10); //$NON-NLS-1$
            IWorkspaceRoot workspaceRoot= JavaPlugin.getWorkspace().getRoot();
            if (project.getProject().hasNature(JavaCore.NATURE_ID)) {
                fEntries= project.getRawClasspath();
                fOutputLocation= project.getOutputLocation();
                IPath projPath= project.getProject().getFullPath();
                
                if ((project.isOnClasspath(project.getUnderlyingResource()) || fOutputLocation.equals(projPath)) && ! fNeverShowDialog) {
                    if (query.doQuery(true, fOutputLocation.toString(), project)) {
                        removeProjectAsRoot= project.isOnClasspath(project.getUnderlyingResource());
                        IRemoveOldBinariesQuery reorgQuery= BuildPathsBlock.getRemoveOldBinariesQuery(null);
                        if (BuildPathsBlock.hasClassfiles(project.getProject()) && reorgQuery.doQuery(fOutputLocation)) {
                            IResource res= workspaceRoot.findMember(fOutputLocation);
                            if (res instanceof IContainer && BuildPathsBlock.hasClassfiles(res)) {
                                BuildPathsBlock.removeOldClassfiles(res);
                            }
                        }
                        fEntries= new IClasspathEntry[fNewEntries.length];
                        System.arraycopy(fNewEntries, 0, fEntries, 0, fEntries.length);
                    }
                }
                    
                fNewOutputLocation= fOutputLocation;
                
                setNewEntry(path, removeProjectAsRoot, project, new SubProgressMonitor(monitor, 7));
                project.setRawClasspath(fNewEntries, fNewOutputLocation, new SubProgressMonitor(monitor, 2));
                IPackageFragmentRoot root= getPackageFragmentRoot(path, project);
                fireEvent(root.getRawClasspathEntry());
                return root;            
            }
        } catch (CoreException e) {
            JavaPlugin.log(e);
        } finally {
            monitor.done();
        }
        return null;
    }
    /**
     * For a given <code>IResource</code>, get it's parent
     * and return it as <code>IPackageFragment</code>
     * if the parent is a package fragment, otherwise
     * return <code>null</code>
     * 
     * @param resource the resource to get the parent as 
     * package fragment if the parent is so
     * @return the <code>resource<code>'s parent as
     * <code>IPackageFragment</code>,or <code>null</code>
     * if there is none
     * 
     * @see #getPackageFragment(IResource)
     */
    public IPackageFragment getFragment(IResource resource) {
        /*IJavaElement elem= JavaCore.create(resource.getParent());
        if (elem instanceof IPackageFragment)
            return (IPackageFragment) elem;
        return null;*/
        return getPackageFragment(resource.getParent());
    }
    
    /**
     * For a given <code>IResource</code>, try to
     * convert it into a package fragment and return it
     * if possible, otherwise return <code>null</code>
     * 
     * @param resource the resource to be converted into a 
     * package fragment
     * @return the <code>resource<code> as
     * <code>IPackageFragment</code>,or <code>null</code>
     * if converting was not possible
     * 
     * @see #getFragment(IResource)
     */
    private IPackageFragment getPackageFragment(IResource resource) {
        IJavaElement elem= JavaCore.create(resource);
        if (elem instanceof IPackageFragment)
            return (IPackageFragment) elem;
        return null;
    }
    
    /**
     * Get the package fragment with the given <code>name</code>.
     * Retrieval is done by getting the <code>
     * IPackageFragmentRoot</code> from the provided java element
     * and then asking the root to get the corresponding fragment.
     * <br>
     * The name can contain slashes like a path expression, for example:
     * pack1/fragment/x2.
     * 
     * @param name the fragment's name
     * @param javaElem the java element to start exploration from
     * @return an <code>IPackageFragment</code> with the given
     * <code>name</code>
     */
    private IPackageFragment getPackageFragment(String name, IJavaElement javaElem) {
        IPackageFragmentRoot root= (IPackageFragmentRoot)javaElem.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
        return root.getPackageFragment(name.replace('/', '.'));
    }
    
    /**
     * Get the <code>IPackageFragmentRoot</code> at the given
     * <code>path</code> or <code>null</code> if there
     * is no root with this path.
     * 
     * @param path the path for the fragment root
     * @param project the java project
     * @return the <code>IPackageFragmentRoot</code> corresponding
     * to the <code>path</code>or <code>null</code> if there
     * is no root with this path.
     * @throws JavaModelException
     */
    private IPackageFragmentRoot getPackageFragmentRoot(IPath path, IJavaProject project) throws JavaModelException {
        if (path.segmentCount() == 1)
            return project.getPackageFragmentRoot(project.getUnderlyingResource());
        IJavaElement elem= JavaCore.create(project.getProject().getWorkspace().getRoot().getFolder(path));
        if (!(elem instanceof IPackageFragmentRoot))
            return null;
        return (IPackageFragmentRoot) elem;
    }
    
    /**
     * Get the <code>IClasspathEntry</code> for the
     * given <code>path</code> by looking up all
     * classpath entries on the <code>project</code>
     * 
     * @param path the path to find a classpath entry for
     * @param project the java project
     * @return the <code>IClasspathEntry</code> corresponding
     * to the <code>path</code>or <code>null</code> if there
     * is no such entry
     * @throws JavaModelException
     */
    public IClasspathEntry getClasspathEntryFor(IPath path, IJavaProject project) throws JavaModelException {
        IClasspathEntry[] entries= project.getRawClasspath();
        for(int i= 0; i < entries.length; i++) {
            IClasspathEntry entry= entries[i];
            if (entry.getPath().equals(path))
                return entry;
        }
        return null;
    }
    
    /**
     * Get a java element with the given <code>name</code>
     * and <code>path</code> which must be a child of
     * the <code>IPackageFragmentRoot</code>
     * 
     * @param name the name of the element to be found 
     * @param path the absolute path of the element
     * (including the name).
     * @param root the fragment root which is the 
     * (direct or indirect) parent of the element
     * to be found 
     * @return the java element corresponding to the
     * <code>name</code>
     */
    private IJavaElement getCorrespondingJavaElement(String name, IPath path, IPackageFragmentRoot root) {
        if (path.getFileExtension() != null) {
          IPackageFragment fragment= getPackageFragment(path.removeLastSegments(1).toString(), root);
          return fragment.getCompilationUnit(path.lastSegment());
        }
        else {
          return getPackageFragment(name, root);
        }
    }
    
    /**
     * Changes the classpath settings of the project and if necessary the
     * project's exlcusion filter. Calling this method can be necessary
     * if for example the project itself is on the classpath.<br>
     * An example for a path is:
     * path= /MyProject/SomeFolder/FolderX<br><br>
     * 
     * @param path the absolute path of an element on which the changes should
     * be applied to.
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     */
    private void modifyProjectFilter(IPath path, IJavaProject project, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.ModifyProjectFilter"), 10); //$NON-NLS-1$
            IClasspathEntry entry= getClasspathEntryFor(project.getPath(), project);
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            if (fEntries == null)
                fEntries= project.getRawClasspath();
            IPath[] excludedPath= (IPath[]) elem.getAttribute(CPListElement.EXCLUSION);
            if (contains(new Path(path.lastSegment()), excludedPath, new SubProgressMonitor(monitor, 4)))
                return;
            IClasspathEntry[] entries= new IClasspathEntry[fEntries.length + 1];
            System.arraycopy(fEntries, 0, entries, 0, fEntries.length);
            entries[fEntries.length]= entry;
            fEntries= entries;
            exclude(path.lastSegment(), null, elem, project, new SubProgressMonitor(monitor, 6));
        }
        catch (JavaModelException e) {
            JavaPlugin.log(e);
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Returns for the given absolute path the corresponding
     * resource, this is either element of type <code>IFile</code>
     * or <code>IFolder</code>.
     *  
     * @param path an absolute path to a resource
     * @param project the java project
     * @return the resource matching to the path. Can be
     * either an <code>IFile</code> or an <code>IFolder</code>.
     */
    private IResource getResource(IPath path, IJavaProject project) {
        if (path.getFileExtension() != null)
            return project.getProject().getWorkspace().getRoot().getFile(path);
        return project.getProject().getWorkspace().getRoot().getFolder(path);
    }
    
    /**
     * Determines whether the <code>IPackageFragment</code>) or
     * one of it's children is on the inclusion filter 
     * of its parent source folder.
     * 
     * @param fragment package fragment
     * @return <code>true</code> if the fragment is included,
     * <code>false</code> otherwise.
     */
    public boolean isIncluded(IPackageFragment fragment) {
        try {
            IPackageFragmentRoot root= (IPackageFragmentRoot) fragment.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
            return contains(new Path(fragment.getPath().lastSegment()), root.getRawClasspathEntry().getInclusionPatterns(), null);
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        return false;
    }
    
    /**
     * Determines whether the <code>IResource</code>'s
     * fragment root has the resource on it's
     * exclusion filter
     * 
     * @param resource the resource
     * @param project the java project
     * @return <code>true</code> if the resource is excluded,
     * <code>false</code> otherwise.
     */
    public boolean isExcluded(IResource resource, IJavaProject project) {
        try {
            IPackageFragmentRoot root= getFragmentRoot(resource, project, null);
            IClasspathEntry entry= getClasspathEntryFor(root.getPath(), project);
            return contains(resource.getFullPath().removeFirstSegments(root.getPath().segmentCount()), entry.getExclusionPatterns(), null);
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        return false;
    }
    
    /**
     * Determine whether the given <code>IResource</code> is
     * excluded on the project.
     * 
     * @param resource the resource to be checked
     * @param project the java project
     * @return <code>true</code> if the resource is excluded
     * on the project, <code>false</code> otherwise
     * @throws JavaModelException
     */
    public boolean isExcludedOnProject(IResource resource, IJavaProject project) throws JavaModelException {
        IClasspathEntry entry= getClasspathEntryFor(project.getPath(), project);
        if (entry != null && contains(resource.getFullPath().removeFirstSegments(1), entry.getExclusionPatterns(), null))
            return true;
        return false;
    }
    
    public boolean parentExcluded(IResource resource, IJavaProject project) {
        try {
            if (resource.getFullPath().equals(project.getPath()))
                return false;
            IPackageFragmentRoot root= getFragmentRoot(resource, project, null);
            IPath path= resource.getFullPath().removeFirstSegments(root.getPath().segmentCount());
            IClasspathEntry entry= getClasspathEntryFor(root.getPath(), project);
            while (path.segmentCount() > 0) {
                if (contains(path, entry.getExclusionPatterns(), null))
                    return true;
                path= path.removeLastSegments(1);
            }
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        return false;
    }
    
    /**
     * Check whether the current selection is the project's 
     * default output folder or not
     * 
     * @param attrib the attribute to be checked
     * @return <code>true</code> if is the default output folder,
     * <code>false</code> otherwise.
     */
    public boolean isDefaultOutputFolder(CPListElementAttribute attrib) {
        return attrib.getValue() == null;
    }
    
    /**
     * Check whether the <code>IPackageFragment</code>
     * corresponds to the project's default fragment.
     * 
     * @param fragment the package fragment to be checked
     * @return <code>true</code> if is the default package fragment,
     * <code>false</code> otherwise.
     */
    public boolean isDefaultFragment(IPackageFragment fragment ) {
        return fragment.getElementName().equals(""); //$NON-NLS-1$
    }
    
    /**
     * Determines whether the current selection (of type
     * <code>ICompilationUnit</code> or <code>IPackageFragment</code>)
     * is on the inclusion filter of it's parent source folder.
     * 
     * @param selection the current java element
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return <code>true</code> if the current selection is included,
     * <code>false</code> otherwise.
     */
    public boolean containsPath(IJavaElement selection, IJavaProject project, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.ContainsPath"), 4); //$NON-NLS-1$
            IPackageFragmentRoot root= getFragmentRoot(selection.getCorrespondingResource(), project, new SubProgressMonitor(monitor, 2));
            IClasspathEntry entry= root.getRawClasspathEntry();
            return contains(selection.getPath().removeFirstSegments(root.getPath().segmentCount()), entry.getInclusionPatterns(), new SubProgressMonitor(monitor, 2));
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        } finally {
            monitor.done();
        }
        return false;
    }
    
    /**
     * Exclude an element with a given name and absolute path
     * from the classpath.
     * 
     * @param name the name of the element to be excluded
     * @param fullPath the absolute path of the element
     * @param entry the classpath entry to be modified
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an object corresponding to the excluded element
     */
    private Object exclude(String name, IPath fullPath, CPListElement entry, IJavaProject project, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        Object result;
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.Excluding"), 10); //$NON-NLS-1$
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
            Object resource= fullPath == null ? null : getResource(fullPath, project);
            result= updateClasspath(entry.getClasspathEntry(), resource, project, new SubProgressMonitor(monitor, 4));
        } finally {
            monitor.done();
        }
        return result;
    }
    
    /**
     * Add a '/' at the end of the name if
     * it does not end with '.java'.
     * 
     * @param name append '/' at the end if
     * necessary
     * @return modified string
     */
    private String completeName(String name) {
        if (!name.endsWith(".java")) { //$NON-NLS-1$
            name= name + "/"; //$NON-NLS-1$
            name= name.replace('.', '/');
            return name;
        }
        return name;
    }
    
    /**
     * Find out whether <code>path</code> equals to one
     * in <code>paths</code>.
     * 
     * @param path path to find an equivalent for
     * @param paths set of paths to compare with
     * @param monitor progress monitor, can be <code>null</code>
     * @return <code>true</code> if there is an occurrence, <code>
     * false</code> otherwise
     */
    public boolean contains(IPath path, IPath[] paths, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        if (path == null)
            return false;
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.ComparePaths"), paths.length); //$NON-NLS-1$
            if (path.getFileExtension() == null && !path.toString().endsWith("/")) //$NON-NLS-1$
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
     * Determines whether the current elements inclusion filter is empty
     * or not
     * @return <code>true</code> if the inclusion filter is empty,
     * <code>false</code> otherwise.
     */
    public boolean includeFiltersEmpty(IResource resource, IJavaProject project, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.ExamineInputFilters"), 4); //$NON-NLS-1$
            IPackageFragmentRoot root= getFragmentRoot(resource, project, new SubProgressMonitor(monitor, 4));
            IClasspathEntry entry= root.getRawClasspathEntry();
            return entry.getInclusionPatterns() == null || entry.getInclusionPatterns().length == 0;
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        } finally {
            monitor.done();
        }
        return false;
    }
    
    /**
     * Get the source folder of a given <code>IResource</code> element,
     * starting with the resource's parent.
     * 
     * @param resource the resource to get the fragment root from
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return resolved fragment root
     * @throws JavaModelException
     */
    public IPackageFragmentRoot getFragmentRoot(IResource resource, IJavaProject project, IProgressMonitor monitor) throws JavaModelException{
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
    public IPath[] remove(IPath path, IPath[] paths, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IPath[] newPaths;
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.RemovePath"), paths.length + 5); //$NON-NLS-1$
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
     * Removes a given <code>path</code> from the filters of it's
     * parent fragment root.
     * 
     * This is a very expensive operation, because the fragment
     * root has to be found using the given path.
     * 
     * @param path path to be removed
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * 
     * @see #remove(IPath, IPath[], IProgressMonitor)
     * @see #resetFilters(IJavaElement, IJavaProject, IProgressMonitor)
     */
    public void removeFromFilters(IPath path, IJavaProject project, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.RemoveFromFilters"), path.segmentCount() + 15); //$NON-NLS-1$
            IClasspathEntry entry= null;
            int i= 0;
            do {
                monitor.worked(1);
                i++;
                if (i == path.segmentCount())
                    return;
                IPath rootPath= path.removeLastSegments(i);
                IPackageFragmentRoot root= getPackageFragmentRoot(rootPath, project);
                entry= root.getRawClasspathEntry();
            } while (entry == null);
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            IPath removePath= path.removeFirstSegments(path.segmentCount() - i);
            IPath[] inclusions= (IPath[]) elem.getAttribute(CPListElement.INCLUSION);
            elem.setAttribute(CPListElement.INCLUSION, remove(removePath, inclusions, new SubProgressMonitor(monitor, 5)));
            IPath[] exclusions= (IPath[]) elem.getAttribute(CPListElement.EXCLUSION);
            elem.setAttribute(CPListElement.EXCLUSION, remove(removePath, exclusions, new SubProgressMonitor(monitor, 5)));
            updateClasspath(elem.getClasspathEntry(), null, project, new SubProgressMonitor(monitor, 5));
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Resets inclusion and exclusion filters for the given
     * <code>IJavaElement</code>
     * 
     * @param element element to reset it's filters
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return element representing an object with reseted filters
     * @throws JavaModelException
     */
    public Object resetFilters(IJavaElement element, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        Object result;
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.ResetFilters"), 10); //$NON-NLS-1$
            IClasspathEntry entry;
            if (element instanceof IJavaProject)
                entry= getClasspathEntryFor(project.getPath(), project);
            else
                entry= ((IPackageFragmentRoot)element).getRawClasspathEntry();
            
            List exclusionList= getFoldersOnCP(element.getPath(), project, new SubProgressMonitor(monitor, 5));
            if (entry.getOutputLocation() != null) {
                if (contains(new Path(completeName(entry.getOutputLocation().lastSegment())), entry.getExclusionPatterns(), null)) {
                    exclusionList.add(new Path(completeName(entry.getOutputLocation().lastSegment())));
                }
            }
            IPath[] exclusions= (IPath[]) exclusionList.toArray(new IPath[exclusionList.size()]);
            
            CPListElement listElem= CPListElement.createFromExisting(entry, project);
            listElem.setAttribute(CPListElement.INCLUSION, new IPath[0]);
            listElem.setAttribute(CPListElement.EXCLUSION, exclusions);
            result= updateClasspath(listElem.getClasspathEntry(), element, project, new SubProgressMonitor(monitor, 5));
        } finally {
            monitor.done();
        }
        return result;
    }
    
    /**
     * Find all folders that are on the classpath and
     * <code>path</code> is a prefix of those folders
     * path entry, that is, all folders which are a
     * subfolder of <code>path</code>.
     * 
     * For example, if <code>path</code>=/MyProject/src 
     * then all folders having a path like /MyProject/src/*,
     * where * can be any valid string are returned if
     * they are also on the project's classpath.
     * 
     * @param path absolute path
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an array of paths which belong to subfolders
     * of <code>path</code> and which are on the classpath
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
    private String getName(IPath path, IPath rootPath) {
        return path.removeFirstSegments(rootPath.segmentCount()).toString();
    }
    
    /**
     * Set the new classpath entry corresponding to <code>path</code>.
     * Validates that the entry is correct and does not violate
     * any java conventions or classpath requirements.
     * 
     * @param path path to create an entry for
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     */
    private void setNewEntry(IPath path, boolean removeProjectAsRoot, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("NewSourceContainerWorkbookPage.Monitor.SetNewEntry"), 10); //$NON-NLS-1$
            IPath projPath= project.getProject().getFullPath();
            IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
            IStatus validate= workspaceRoot.getWorkspace().validatePath(path.toString(), IResource.FOLDER);
            StatusInfo rootStatus= new StatusInfo();
            rootStatus.setOK();
            
            if (validate.matches(IStatus.ERROR)) {
                rootStatus.setError(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.error.InvalidRootName", validate.getMessage())); //$NON-NLS-1$
            } else {
                IResource res= workspaceRoot.findMember(path);
                if (res != null) {
                    if (res.getType() != IResource.FOLDER) {
                        rootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.NotAFolder")); //$NON-NLS-1$
                        return;
                    }
                } else {
                    IPath projLocation= project.getProject().getLocation();
                    if (projLocation != null && path.toFile().exists()) {
                        rootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.AlreadyExistingDifferentCase")); //$NON-NLS-1$
                        return;
                    }
                }
                monitor.worked(1);
                ArrayList newEntries= new ArrayList(fEntries.length + 1);
                int projectEntryIndex= -1;
                
                for (int i= 0; i < fEntries.length; i++) {
                    IClasspathEntry curr= fEntries[i];
                    if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                        if (path.equals(curr.getPath())) {
                            rootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.AlreadyExisting")); //$NON-NLS-1$
                            return;
                        }
                        if (projPath.equals(curr.getPath())) {
                            projectEntryIndex= i;
                        }   
                    }
                    newEntries.add(curr);
                }
                
                monitor.worked(3);
                
                IClasspathEntry newEntry= JavaCore.newSourceEntry(path);
                CPListElement elem= CPListElement.createFromExisting(newEntry, project);
                Iterator iterator= newEntries.iterator();
                boolean entryModified= false;
                while(iterator.hasNext()) {
                    IClasspathEntry entry= (IClasspathEntry) iterator.next();
                    if (entry.getPath().matchingFirstSegments(path) == path.segmentCount()) {
                        exclude(entry.getPath().removeFirstSegments(path.segmentCount()).toString(), null, elem, project, null);
                        entryModified= true;
                    }
                }
                
                monitor.worked(3);
                
                newEntry= elem.getClasspathEntry();
                
                if (projectEntryIndex != -1 && !project.getPath().equals(fNewOutputLocation) && removeProjectAsRoot)
                    newEntries.set(projectEntryIndex, newEntry);
                else {
                    if (entryModified)
                        newEntries.add(newEntry);
                    else
                        newEntries.add(JavaCore.newSourceEntry(path));
                }
                
                monitor.worked(1);
                
                fNewEntries= (IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]);
                IJavaModelStatus status= JavaConventions.validateClasspath(project, fNewEntries, fNewOutputLocation);
                if (!status.isOK()) {
                    if (fOutputLocation.equals(projPath)) {
                        IStatus status2= JavaConventions.validateClasspath(project, fNewEntries, fNewOutputLocation);
                        if (status2.isOK()) {
                          if (project.isOnClasspath(project.getUnderlyingResource())) {
                              rootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceSFandOL", fNewOutputLocation.makeRelative().toString())); //$NON-NLS-1$
                          } else {
                                rootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceOL", fNewOutputLocation.makeRelative().toString())); //$NON-NLS-1$
                          }
                          return;
                        }
                    }
                    rootStatus.setError(status.getMessage());
                    return;
                }
                
                if (project.isOnClasspath(project.getUnderlyingResource())) {
                    rootStatus.setInfo(NewWizardMessages.getString("NewSourceFolderWizardPage.warning.ReplaceSF")); //$NON-NLS-1$
                    return;
                }
                
                rootStatus.setOK();
                return;
            }
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Event fired whenever a classpathentry has changed.
     * The event parameter corresponds to the 
     * new <code>IClasspathEntry</code>
     * 
     * @param newEntry
     * 
     * @see #addToClasspath(IPath, IJavaProject, IOutputFolderQuery, IProgressMonitor)
     * @see #removeFromClasspath(IPath, IJavaProject, IProgressMonitor)
     * @see #updateClasspath(IClasspathEntry, Object, IJavaProject, IProgressMonitor)
     */
    public void fireEvent(IClasspathEntry newEntry) {
        setChanged();
        notifyObservers(newEntry);
    }
    
    /**
     * Event fired when the output location has changed.
     * The event parameter corresponds to a string
     * for the output location.
     * 
     * @param outputLocation the new output location
     */
    public void fireEvent(String outputLocation) {
        setChanged();
        notifyObservers(outputLocation);
    }
    
    /**
     * A default folder query that can be used for this class.
     * The query is used to get information about whether the project should be removed as
     * source folder and update build folder to <code>outputLocation</code>
     * 
     * @param shell shell if there is any or <code>null</code>
     * @return an <code>IOutputFolderQuery</code> that can be executed
     * 
     * @see IOutputFolderQuery
     * @see #addToClasspath(IFolder, IJavaProject, IOutputFolderQuery, IProgressMonitor)
     * @see #addToClasspath(IJavaElement, IJavaProject, IOutputFolderQuery, IProgressMonitor)
     */
    public IOutputFolderQuery getDefaultFolderQuery(final Shell shell) {
        return new IOutputFolderQuery() {
            public boolean doQuery(final boolean useNeverButton, final String outputLocation, final IJavaProject project) {
                final boolean[] result= new boolean[1];
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
                        IPath outputFolder= new Path(outputLocation);
                        
                        IPath newOutputFolder= null;
                        String message;
                        if (outputFolder.segmentCount() == 1 && useNeverButton) {
                            String outputFolderName= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME);
                            newOutputFolder= outputFolder.append(outputFolderName);
                            message= NewWizardMessages.getFormattedString("SourceContainerWorkbookPage.ChangeOutputLocationDialog.project_and_output.message", newOutputFolder); //$NON-NLS-1$
                        } else if (useNeverButton){
                            message= NewWizardMessages.getString("SourceContainerWorkbookPage.ChangeOutputLocationDialog.project.message"); //$NON-NLS-1$
                        } else {
                            String outputFolderName= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME);
                            newOutputFolder= outputFolder.append(outputFolderName);
                            message= NewWizardMessages.getFormattedString("NewSourceContainerWorkbookPage.ChangeOutputLocationDialog.project.message", newOutputFolder); //$NON-NLS-1$
                        }
                        String title= NewWizardMessages.getString("SourceContainerWorkbookPage.ChangeOutputLocationDialog.title"); //$NON-NLS-1$$
                        MessageDialog dialog;
                        if (!useNeverButton) 
                            dialog= new MessageDialog(sh, title, null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
                        else
                            dialog= new MessageDialog(sh, title, null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, "Never"}, 0); //$NON-NLS-1$
                        int code= dialog.open();
                        boolean ok= code == IDialogConstants.OK_ID;
                        if (ok) {
                            if (newOutputFolder != null) {
                                fireEvent(newOutputFolder.toString().substring(1));
                                fOutputLocation= new Path(newOutputFolder.toString());
                                try {
                                    project.setOutputLocation(fOutputLocation, null);
                                } catch (JavaModelException e1) {
                                    JavaPlugin.log(e1);
                                }
                            }
                            removeFromClasspath(project.getPath(), project, null);
                        }
                        if (code == 2)
                            fNeverShowDialog= true;
                        result[0]= ok;
                    }
                });
                return result[0];
            }
        };
    }
    
    /**
     * A default query for inclusion and exclusion filters.
     * The query is used to get information about the
     * inclusion and exclusion filters of an element.
     * 
     * @param shell shell if there is any or <code>null</code>
     * @return an <code>IInclusionExclusionQuery</code> that can be executed
     * 
     * @see IInclusionExclusionQuery
     * @see #editFilters(IJavaElement, IJavaProject, IInclusionExclusionQuery, IProgressMonitor)
     */
    public IInclusionExclusionQuery getDefaultInclusionExclusionQuery(final Shell shell) {
        return new IInclusionExclusionQuery() {
            final boolean[] result= new boolean[1];
            final IPath[][] inclusionPattern= new IPath[1][1];
            final IPath[][] exclusionPattern= new IPath[1][1];
            public boolean doQuery(final CPListElement element, final boolean focusOnExcluded) {
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
                        ExclusionInclusionDialog dialog= new ExclusionInclusionDialog(sh, element, focusOnExcluded);
                        result[0]= dialog.open() == Window.OK;
                        inclusionPattern[0]= dialog.getInclusionPattern();
                        exclusionPattern[0]= dialog.getExclusionPattern();
                    }
                });
                return result[0];
            }
            
            public IPath[] getInclusionPattern() {
                return inclusionPattern[0];
            }
            
            public IPath[] getExclusionPattern() {
                return exclusionPattern[0];
            }
        };
    }
    
    /**
     * A default query for the output location.
     * The query is used to get information about the output location 
     * that should be used for a given element.
     * 
     * @param shell shell if there is any or <code>null</code>
     * @return an <code>IOutputLocationQuery</code> that can be executed
     * 
     * @see IOutputLocationQuery
     * @see #createOutputFolder(IPackageFragmentRoot, String, IOutputLocationQuery, IJavaProject, IProgressMonitor)
     * @see #editOutputFolder(CPListElement, String, IJavaProject, IOutputLocationQuery, IProgressMonitor)
     */
    public IOutputLocationQuery getDefaultOutputLocationQuery(final Shell shell) {
        return new IOutputLocationQuery() {
            final boolean[] result= new boolean[1];
            final IPath[] path= new IPath[1];
            public boolean doQuery(final CPListElement element) {
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
                        OutputLocationDialog dialog= new OutputLocationDialog(sh, element);
                        result[0]= dialog.open() == Window.OK;
                        path[0]= dialog.getOutputLocation();
                    }
                });
                return result[0];
            }
            
            public IPath getOutputLocation() {
                return path[0];
            }
            
            public IOutputFolderQuery getOutputFolderQuery() {
                return getDefaultFolderQuery(shell);
            }
        };
    }
    
    public IFolderCreationQuery getDefaultFolderCreationQuery(final Shell shell, final Object selection, final int type) {
        return new IFolderCreationQuery() {
            final boolean[] isSourceFolder= {false};
            final IFolder[] folder= {null};
            
            public boolean doQuery() {
                final boolean[] isOK= {false};
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
                        IContainer container;
                        
                        if (selection instanceof IFolder)
                            container= (IFolder)selection;
                        else {
                            IJavaElement javaElement= (IJavaElement)selection;
                            IJavaProject project= javaElement.getJavaProject();
                            container= project.getProject();
                            if (!(selection instanceof IJavaProject)) {
                                container= container.getFolder(javaElement.getPath().removeFirstSegments(1));
                            }
                        }
                        
                        ExtendedNewFolderDialog dialog= new ExtendedNewFolderDialog(sh, container, type);
                        isOK[0]= dialog.open() == Window.OK;
                        if (isOK[0]) {
                            folder[0]= dialog.getCreatedFolder();
                            isSourceFolder[0]= dialog.isSourceFolder();
                        }
                    }
                });
                return isOK[0];
            }
            
            public boolean isSourceFolder() {
                return isSourceFolder[0];
            }

            public IFolder getCreatedFolder() {
                return folder[0];
            }
            
        };
    }
}
