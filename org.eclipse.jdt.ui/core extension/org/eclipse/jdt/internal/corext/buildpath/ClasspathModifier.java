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
import java.util.Iterator;
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
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock.IRemoveOldBinariesQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IFolderCreationQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IInclusionExclusionQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IOutputLocationQuery;

public class ClasspathModifier {
    
    /**
     * Interface for listeners that want to receive a notification about 
     * changes on <code>IClasspathEntry</code>. For example, if a source 
     * folder changes one of it's inclusion/exclusion filters, then 
     * this event will be fired.
     */
    public static interface IClasspathModifierListener {
        public static final int ADD= 0x01;
        public static final int REMOVE= 0x02;
        public static final int EDIT= 0x03;
        
        /**
         * The new classpath entry that was generated upon calling a method of 
         * <code>ClasspathModifier</code>. The type indicates which kind of 
         * interaction was executed on this entry.
         * 
         * @param newEntry the new entry
         * @param type the type of this entry. Is one of the constants:
         * <li>ClasspathModifierListener.ADD</li>
         * <li>ClasspathModifierListener.REMOVE</li>
         * <li>ClasspathModifierListener.EDIT</li
         */
        public void classpathEntryChanged(IClasspathEntry newEntry, int type);
    }
    
    private IClasspathModifierListener fListener;
    
    protected ClasspathModifier() {
        this(null);
    }
    
    protected ClasspathModifier(IClasspathModifierListener listener) {
        fListener= listener;
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
     * no folder was created (e.g. the operation was cancelled or the project has no natures.
     * @throws CoreException 
     * @throws OperationCanceledException 
     * @see ClasspathModifierQueries.IFolderCreationQuery
     * @see ClasspathModifierQueries.IOutputFolderQuery
     */
    protected Object createFolder(IFolderCreationQuery folderQuery, IOutputFolderQuery outputQuery, IJavaProject project, IProgressMonitor monitor) throws OperationCanceledException, CoreException{
        if (folderQuery.doQuery()) {
            IFolder folder= folderQuery.getCreatedFolder();
            if (folderQuery.isSourceFolder()) {
                Object root= addToClasspath(folder, project, outputQuery, monitor);
                if (root == null)
                    folder.delete(false, null);
                return root;
            }
            exclude(folder.getFullPath(), project, monitor);
            return folder;
        }
        return null;
    }
    
    /**
     * Adds a folder to the classpath.
     * 
     * @param folder folder to be added to the classpath
     * @param project the java project
     * @param query for information about whether the project should be removed as
     * source folder and update build folder
     * @param monitor progress monitor, can be <code>null</code> 
     * @return returns the new element of type <code>IPackageFragmentRoot</code> that has been added to the classpath or
     * <code>null</code> if the project has no natures
     * @throws CoreException 
     * @throws OperationCanceledException 
     * @see ClasspathModifierQueries.IOutputFolderQuery
     */
    protected IPackageFragmentRoot addToClasspath(IFolder folder, IJavaProject project, IOutputFolderQuery query, IProgressMonitor monitor) throws OperationCanceledException, CoreException{
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.AddToBuildpath"), 4); //$NON-NLS-1$
            exclude(folder.getFullPath(), project, new SubProgressMonitor(monitor, 4));
            IPackageFragmentRoot result= addToClasspath(folder.getFullPath(), project, query, new SubProgressMonitor(monitor, 4));
            return result;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Adds a java element to the classpath.
     * 
     * @param javaElement element to be added to the classpath
     * @param project the java project
     * @param query for information about whether the project should be removed as
     * source folder and update build folder
     * @param monitor progress monitor, can be <code>null</code> 
     * @return returns the new element of type <code>IPackageFragmentRoot</code> that has been added to the classpath or
     * <code>null</code> if the project has no natures
     * @throws CoreException 
     * @throws OperationCanceledException 
     * @see ClasspathModifierQueries.IOutputFolderQuery
     */
    protected IPackageFragmentRoot addToClasspath(IJavaElement javaElement, IJavaProject project, IOutputFolderQuery query, IProgressMonitor monitor) throws OperationCanceledException, CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.AddToBuildpath"), 10); //$NON-NLS-1$
            if (!(javaElement instanceof IJavaProject)) {
                IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
                exclude(javaElement.getElementName(), null, CPListElement.createFromExisting(root.getRawClasspathEntry(), project), project, new SubProgressMonitor(monitor, 3));
            }
            IPackageFragmentRoot result= addToClasspath(javaElement.getPath(), project, query, new SubProgressMonitor(monitor, 7));
            return result;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Remove the java project from the classpath and verify that the classpath entries are valid.
     * 
     * @param project the project to be removed
     * @param monitor progress monitor, can be <code>null</code>
     * @return returns the Java project
     * @throws CoreException
     */
    protected IJavaProject removeFromClasspath(IJavaProject project, IProgressMonitor monitor) throws CoreException {
        IClasspathEntry entry= getClasspathEntryFor(project.getPath(), project);
        if (entry == null) // no entry found
            return project;
        IClasspathEntry[] entries= project.getRawClasspath();
        List list= new ArrayList(entries.length - 1);
        for(int i= 0; i < entries.length; i++) {
            if (!entries[i].equals(entry))
                list.add(entries[i]);
        }
        IClasspathEntry[] newEntries= (IClasspathEntry[]) list.toArray((new IClasspathEntry[list.size()]));
        
        StatusInfo rootStatus= new StatusInfo();
        rootStatus.setOK();
        IJavaModelStatus status= JavaConventions.validateClasspath(project, newEntries, project.getOutputLocation());
        if (!status.isOK()) {
            if (project.getPath().equals(project.getOutputLocation())) {
                IStatus status2= JavaConventions.validateClasspath(project, newEntries, project.getOutputLocation());
                if (status2.isOK()) {
                  if (project.isOnClasspath(project.getUnderlyingResource())) {
                      rootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceSFandOL", project.getOutputLocation().makeRelative().toString())); //$NON-NLS-1$
                  } else {
                      rootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceOL", project.getOutputLocation().makeRelative().toString())); //$NON-NLS-1$
                  }
                  return project;
                }
            }
            rootStatus.setError(status.getMessage());
            return project;
        }
        project.setRawClasspath(newEntries, project.getOutputLocation(), monitor);
        fireEvent(entry, IClasspathModifierListener.REMOVE);
        return project;
    }
    
    /**
     * Remove a given <code>IPackageFragmentRoot</code> from the classpath.
     * 
     * @param root the <code>IPackageFragmentRoot</code> to be removed from the classpath
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return returns the <code>IFolder</code> that has been removed from the classpath
     * @see #fireEvent(IClasspathEntry, int)
     */
    protected IFolder removeFromClasspath(IPackageFragmentRoot root, IJavaProject project, IProgressMonitor monitor) throws CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.RemoveFromBuildpath"), 3); //$NON-NLS-1$
            IClasspathEntry entry= root.getRawClasspathEntry();
            int jCoreFlags= IPackageFragmentRoot.NO_RESOURCE_MODIFICATION | IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH;
            root.delete(IResource.NONE, jCoreFlags, new SubProgressMonitor(monitor, 1));
            
            fireEvent(entry, IClasspathModifierListener.REMOVE);
            
            IFolder folder= project.getProject().getWorkspace().getRoot().getFolder(root.getPath());
            return folder;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Includes an object. This means that the inclusion filter for the
     * corresponding <code>IPackageFragmentRoot</code> needs to be modified.
     * The type of the object to be included must either be <code>IResource
     * </code> or <code>IJavaElement</code>.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
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
    protected IJavaElement include(Object toBeIncluded, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Including"), 10); //$NON-NLS-1$
            IResource resource;        
            if (toBeIncluded instanceof IResource)
                resource= (IResource)toBeIncluded;
            else {
                IJavaElement elem= (IJavaElement)toBeIncluded;
                resource= elem.getResource();
            }
            
            IPath path= resource.getFullPath();
            IPackageFragmentRoot root= getFragmentRoot(resource, project, new SubProgressMonitor(monitor, 2));
            String name= getName(path, root.getPath());
            
            IClasspathEntry entry= root.getRawClasspathEntry();
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
            
            updateClasspath(elem.getClasspathEntry(), project, new SubProgressMonitor(monitor, 4));
            IJavaElement newElement= getCorrespondingJavaElement(resource.getFullPath(), project);
            return newElement;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Exclude a java element. This means that the exclusion filter for the
     * corresponding <code>IPackageFragmentRoot</code> needs to be modified.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param javaElement java element to be excluded
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an object representing the excluded element
     * @throws JavaModelException
     */
    protected Object exclude(IJavaElement javaElement, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Excluding"), 5); //$NON-NLS-1$
            IPackageFragmentRoot root= (IPackageFragmentRoot) javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
            IClasspathEntry entry= root.getRawClasspathEntry();
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            String name= getName(javaElement.getPath(), root.getPath());
            Object result= exclude(name, javaElement.getPath(), elem, project, new SubProgressMonitor(monitor, 5));
            return result;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Inverse operation to <code>include</code>.
     * The resource's path will be removed from
     * it's fragment roots inclusion filter.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param javaElement the resource to be unincluded
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return an object representing the unexcluded element 
     * @throws JavaModelException
     * 
     * @see #include(Object, IJavaProject, IProgressMonitor)
     */
    protected Object unInclude(IJavaElement javaElement, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.RemoveInclusion"), 10); //$NON-NLS-1$
            IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);                        
            String name= getName(javaElement.getPath(), root.getPath());  
            
            IClasspathEntry entry= root.getRawClasspathEntry();
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            IPath[] includedPath= (IPath[]) elem.getAttribute(CPListElement.INCLUSION);
            IPath relPath= new Path(completeName(name));
            IPath[] newIncludedPath= remove(relPath, includedPath, new SubProgressMonitor(monitor, 3));
            elem.setAttribute(CPListElement.INCLUSION, newIncludedPath);
            updateClasspath(elem.getClasspathEntry(), project, new SubProgressMonitor(monitor, 4));
            Object result= getCorrespondingJavaElement(javaElement.getPath(), project);
            if (result == null)
                result= getResource(javaElement.getPath(), project);
            return result;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Inverse operation to <code>exclude</code>.
     * The resource's path will be removed from
     * it's fragment roots exlusion filter.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
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
    protected Object unExclude(IResource resource, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.RemoveExclusion"), 10); //$NON-NLS-1$
            IPackageFragmentRoot javaElem= getFragmentRoot(resource, project, new SubProgressMonitor(monitor, 3));
            String name= getName(resource.getFullPath(), javaElem.getPath());
            IClasspathEntry entry= javaElem.getRawClasspathEntry();
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            IPath[] excludedPath= (IPath[]) elem.getAttribute(CPListElement.EXCLUSION);
            IPath[] newExcludedPath= remove(new Path(completeName(name)), excludedPath, new SubProgressMonitor(monitor, 3));
            elem.setAttribute(CPListElement.EXCLUSION, newExcludedPath);
            updateClasspath(elem.getClasspathEntry(), project, new SubProgressMonitor(monitor, 4));
            Object result= getCorrespondingJavaElement(resource.getFullPath(), project);
            if (result == null)
                result= getResource(resource.getFullPath(), project);
            return result;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Opens a dialog to edit the filters (inclusion/exclusion) for the
     * given <code>IJavaElement</code>.
     * 
     * @param element the java element to edit the filters on. Must be either of
     * type <code>IJavaProject</code> or <code>IPackageFragmentRoot</code>.
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return returns the edited java element or <code>null</code> if the dialog was
     * cancelled
     * @throws JavaModelException
     */
    protected IJavaElement editFilters(IJavaElement element, IJavaProject project, IInclusionExclusionQuery query, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.EditInclusionExclusionFilters"), 4); //$NON-NLS-1$
            IClasspathEntry entry;
            if (element instanceof IJavaProject)
                entry= getClasspathEntryFor(project.getPath(), project);
            else
                entry= ((IPackageFragmentRoot)element).getRawClasspathEntry();
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            if (query.doQuery(elem, false)) {
                elem.setAttribute(CPListElement.INCLUSION, query.getInclusionPattern());
                elem.setAttribute(CPListElement.EXCLUSION, query.getExclusionPattern());
                updateClasspath(elem.getClasspathEntry(), project, new SubProgressMonitor(monitor, 4));
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
     * @param query Query to get information about the output
     * location that should be used for a given element
     * @param project the current java project
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
     * Edits the output folder entry for a given <code>
     * CPListElementAttribute</code> which corresponds to
     * a <code>IPackageFragmentRoot</code>. The <code>
     * IOutputLocationQuery</code> provides the information
     * necessary to edit this setting.
     * 
     * Note: the folder is also created in the file system.
     * Therefore clients have to ensure that is is deleted if
     * necessary because the <code>ClasspathModifier</code> does
     * not handle this.
     * 
     * @param element an element representing the output folder
     * @param project the java project
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
        if (query.doQuery(element)) {
            if (project.getPath().equals(project.getOutputLocation())) {
                IOutputFolderQuery outputFolderQuery= query.getOutputFolderQuery(query.getOutputLocation());
                if (!outputFolderQuery.doQuery(true, project))
                    return null;
                project.setOutputLocation(outputFolderQuery.getOutputLocation(), null);
                if (outputFolderQuery.removeProjectFromClasspath())
                    removeFromClasspath(project, null);
            }
            if (query.getOutputLocation() == null) {
                return resetOutputFolder(element.getClasspathEntry(), project, monitor);
            }
            if (query.getOutputLocation().segmentCount() == 2) {
                if (project.isOnClasspath(project.getUnderlyingResource())) {
                    modifyProjectFilter(query.getOutputLocation(), project, new SubProgressMonitor(monitor, 3));
                }
            }
            else   
                exclude(query.getOutputLocation(), project, null);
            IClasspathEntry newEntry= element.getClasspathEntry();
            element= CPListElement.createFromExisting(newEntry, project);
            element.setAttribute(CPListElement.OUTPUT, query.getOutputLocation());
            newEntry= element.getClasspathEntry();
            CPListElementAttribute outputFolder= new CPListElementAttribute(element, CPListElement.OUTPUT, 
                    element.getAttribute(CPListElement.OUTPUT));
            updateClasspath(newEntry, project, new NullProgressMonitor());
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
     * @throws JavaModelException 
     */
    protected CPListElementAttribute resetOutputFolder(IClasspathEntry entry, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        CPListElement elem= CPListElement.createFromExisting(entry, project);
        elem.setAttribute(CPListElement.OUTPUT, null);
        CPListElementAttribute outputFolder= new CPListElementAttribute(elem, CPListElement.OUTPUT, 
                elem.getAttribute(CPListElement.OUTPUT));
        updateClasspath(elem.getClasspathEntry(), project, new NullProgressMonitor());
        return outputFolder;
    }
    
    /**
     * Reset all output folder for the given java
     * project.
     * 
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     */
    protected void resetOutputFolders(IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.ResetOutputFolder"), 3 * roots.length); //$NON-NLS-1$
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
     * Resets inclusion and exclusion filters for the given
     * <code>IJavaElement</code>
     * 
     * @param element element to reset it's filters
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return element the java element with reseted filters
     * @throws JavaModelException
     */
    protected IJavaElement resetFilters(IJavaElement element, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.ResetFilters"), 10); //$NON-NLS-1$
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
            updateClasspath(listElem.getClasspathEntry(), project, new SubProgressMonitor(monitor, 5));
            return element;
        } finally {
            monitor.done();
        }
    }
    
    /**
     * Check whether at least one source folder of the given
     * java project has an output folder set.
     * 
     * @param project the java project
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
     * Get the <code>IClasspathEntry</code> for the
     * given <code>path</code> by looking up all
     * classpath entries on the <code>project</code>
     * 
     * @param path the path to find a classpath entry for
     * @param project the java project
     * @return the <code>IClasspathEntry</code> corresponding
     * to the <code>path</code> or <code>null</code> if there
     * is no such entry
     * @throws JavaModelException
     */
    public static IClasspathEntry getClasspathEntryFor(IPath path, IJavaProject project) throws JavaModelException {
        IClasspathEntry[] entries= project.getRawClasspath();
        for(int i= 0; i < entries.length; i++) {
            IClasspathEntry entry= entries[i];
            if (entry.getPath().equals(path) && isEntryKind(entry.getEntryKind()))
                return entry;
        }
        return null;
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
    public static boolean isExcludedOnProject(IResource resource, IJavaProject project) throws JavaModelException {
        IClasspathEntry entry= project.getPackageFragmentRoot(project.getUnderlyingResource()).getRawClasspathEntry();
        if (entry != null && contains(resource.getFullPath().removeFirstSegments(1), entry.getExclusionPatterns(), null))
            return true;
        return false;
    }
    
    /**
     * Find out whether one of the <code>IResource</code>'s parents
     * is excluded.
     * 
     * @param resource check the resources parents whether they are
     * excluded or not
     * @param project the java project
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
            return true; // there is no classpath entry, this is equal to the fact that the parent is excluded
        while (path.segmentCount() > 0) {
            if (contains(path, entry.getExclusionPatterns(), null))
                return true;
            path= path.removeLastSegments(1);
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
    public static boolean isDefaultOutputFolder(CPListElementAttribute attrib) {
        return attrib.getValue() == null;
    }
    
    // TODO Testcases & verifying, ev. merge with 'isExcludedOnProject'
    public static boolean isExcludedOnFragmentRoot(IResource resource, IJavaProject project) throws JavaModelException {
        IPackageFragmentRoot root= getFragmentRoot(resource, project, null);
        if (root == null)
            return false;
        String fragmentName= getName(resource.getFullPath(), root.getPath());
        fragmentName= completeName(fragmentName);
        IClasspathEntry entry= root.getRawClasspathEntry();
        return contains(new Path(fragmentName), entry.getExclusionPatterns(), null);
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
     * Determines whether the current selection (of type
     * <code>ICompilationUnit</code> or <code>IPackageFragment</code>)
     * is on the inclusion filter of it's parent source folder.
     * 
     * @param selection the current java element
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * @return <code>true</code> if the current selection is included,
     * <code>false</code> otherwise.
     * @throws JavaModelException 
     */ // TODO better name
    public static boolean containsPath(IJavaElement selection, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.ContainsPath"), 4); //$NON-NLS-1$
            IPackageFragmentRoot root= (IPackageFragmentRoot) selection.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
            IClasspathEntry entry= root.getRawClasspathEntry();
            return contains(selection.getPath().removeFirstSegments(root.getPath().segmentCount()), entry.getInclusionPatterns(), new SubProgressMonitor(monitor, 2));
        } finally {
            monitor.done();
        }
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
    public static boolean contains(IPath path, IPath[] paths, IProgressMonitor monitor) {
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
     * Determines whether the current elements inclusion filter is empty
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
     * Get the source folder of a given <code>IResource</code> element,
     * starting with the resource's parent.
     * 
     * @param resource the resource to get the fragment root from
     * @param project the java project
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
     * Check whether the input paramenter of type <code>
     * IPackageFragmentRoot</code> has either it's inclusion or
     * exclusion filter or both set (that means they are
     * not empty).
     * 
     * @param root the fragment root to be inspected
     * @return <code>true</code> inclusion or exclusion filter set,
     * <code>false</code> otherwise.
     */
    public static boolean filtersSet(IPackageFragmentRoot root) throws JavaModelException{
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
     * <code>null</code> if project has no nature or classpath validation failed.
     * @throws CoreException 
     * @throws OperationCanceledException 
     * @throws JavaModelException 
     * 
     * @see ClasspathModifierQueries.IOutputFolderQuery
     */
    private IPackageFragmentRoot addToClasspath(IPath path, IJavaProject project, IOutputFolderQuery query, IProgressMonitor monitor) throws JavaModelException, OperationCanceledException, CoreException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        IPackageFragmentRoot root= null;
        try {
            boolean removeProjectAsRoot= false;
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.AddToBuildpath"), 10); //$NON-NLS-1$
            IWorkspaceRoot workspaceRoot= JavaPlugin.getWorkspace().getRoot();
            IClasspathEntry[] fEntries;
            if (project.getProject().hasNature(JavaCore.NATURE_ID)) {
                fEntries= project.getRawClasspath();
                IPath outputLocation= project.getOutputLocation();
                IPath projPath= project.getProject().getFullPath();
                
                if (!path.equals(project.getPath()) && (outputLocation.equals(projPath) || query.getDesiredOutputLocation().segmentCount() == 1)) {
                    if (query.doQuery(false, project)) {
                        project.setOutputLocation(query.getOutputLocation(), null);
                        // remove the project
                        if (query.removeProjectFromClasspath()) {
                            removeFromClasspath(project, null);
                            removeProjectAsRoot= true;
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
                        return null;
                }
                
                fEntries= setNewEntry(fEntries, path, removeProjectAsRoot, project, new SubProgressMonitor(monitor, 7));
                if (fEntries == null)
                    return null;
                project.setRawClasspath(fEntries, outputLocation, new SubProgressMonitor(monitor, 2));
                root= getPackageFragmentRoot(path, project);
                fireEvent(root.getRawClasspathEntry(), IClasspathModifierListener.ADD);         
            }
        } finally {
            monitor.done();
        }
        return root;
    }
    
    /**
     * Exclude an object at a given path.
     * This means that the exclusion filter for the
     * corresponding <code>IPackageFragmentRoot</code> needs to be modified.
     * 
     * Note: the <code>IJavaElement</code>'s fragment (if there is one)
     * is not allowed to be excluded! However, inclusion (or simply no
     * filter) on the parent fragment is allowed.
     * 
     * @param path absolute path of an object to be excluded
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     */
    private void exclude(IPath path, IJavaProject project, IProgressMonitor monitor) throws JavaModelException{
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Excluding"), 5); //$NON-NLS-1$
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
     * Updates the classpath if changes have been applied to a
     * classpath entry. For example, this can be necessary after
     * having edited some filters on a classpath entry, which can happen
     * when including or excluding an object.
     * 
     * @param newEntry the new entry to be set on the classpath. It will
     * replace the original one identifying the matching entry
     * by comparing the paths (which are unique for each classpath entry)
     * @param project the java project
     * @param monitor progress monitor, can be <code>null</code>
     * 
     * @see #include(Object, IJavaProject, IProgressMonitor)
     * @see #exclude(IJavaElement, IJavaProject, IProgressMonitor)
     */
    private void updateClasspath(IClasspathEntry newEntry, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            IClasspathEntry[] fEntries= project.getRawClasspath();
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.UpdatingBuildpath"), fEntries.length + 2); //$NON-NLS-1$
            for (int i= 0; i < fEntries.length; i++) {
                if (fEntries[i].getPath().equals(newEntry.getPath()))
                        fEntries[i]= newEntry;
                monitor.worked(1);
            }

            IPath outputLocation= project.getOutputLocation();
            
            IJavaModelStatus status= JavaConventions.validateClasspath(project, fEntries, outputLocation);
            if (!status.isOK())
               throw new JavaModelException(status);
            
            project.setRawClasspath(fEntries, outputLocation, new SubProgressMonitor(monitor, 2));
            fireEvent(newEntry, IClasspathModifierListener.EDIT);
        } finally {
            monitor.done();
        }
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
     * Get a java element with the given <code>path</code>.
     * If the resource with this path cannot be associated with 
     * a <code>IJavaElement</code>, then the return value is 
     * <code>null</code>.
     * 
     * @param path the path of the element to be found
     * @param project the Java project
     * @return the corresponding <code>IJavaElement</code> to 
     * the given path, or <code>null</code> if no one found.
     * @throws JavaModelException
     */
    private IJavaElement getCorrespondingJavaElement(IPath path, IJavaProject project) throws JavaModelException {
        IResource resource= getResource(path, project);
        IJavaElement elem= JavaCore.create(resource);
        if (elem != null && project.isOnClasspath(elem))
            return elem;
        return null;
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
     * @throws JavaModelException 
     */
    private void modifyProjectFilter(IPath path, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.ModifyProjectFilter"), 10); //$NON-NLS-1$
            IClasspathEntry entry= getClasspathEntryFor(project.getPath(), project);
            CPListElement elem= CPListElement.createFromExisting(entry, project);
            IPath[] excludedPath= (IPath[]) elem.getAttribute(CPListElement.EXCLUSION);
            if (contains(new Path(path.lastSegment()), excludedPath, new SubProgressMonitor(monitor, 4)))
                return;
            exclude(path.lastSegment(), null, elem, project, new SubProgressMonitor(monitor, 6));
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
        return project.getProject().getWorkspace().getRoot().findMember(path);
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
     * @throws JavaModelException 
     */
    private Object exclude(String name, IPath fullPath, CPListElement entry, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        Object result;
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.Excluding"), 10); //$NON-NLS-1$
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
            updateClasspath(entry.getClasspathEntry(), project, new SubProgressMonitor(monitor, 4));
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
    private static String getName(IPath path, IPath rootPath) {
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
    private IClasspathEntry[] setNewEntry(IClasspathEntry[] fEntries, IPath path, boolean removeProjectAsRoot, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        try {
            monitor.beginTask(NewWizardMessages.getString("ClasspathModifier.Monitor.SetNewEntry"), 10); //$NON-NLS-1$
            IPath projPath= project.getProject().getFullPath();
            IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
            IStatus validate= workspaceRoot.getWorkspace().validatePath(path.toString(), IResource.FOLDER);
            StatusInfo rootStatus= new StatusInfo();
            rootStatus.setOK();
            
            if (validate.matches(IStatus.ERROR) && !project.getPath().equals(path)) {
                rootStatus.setError(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.error.InvalidRootName", validate.getMessage())); //$NON-NLS-1$
            } else {
                if (!project.getPath().equals(path)) {
                    IResource res= workspaceRoot.findMember(path);
                    if (res != null) {
                        if (res.getType() != IResource.FOLDER) {
                            rootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.NotAFolder")); //$NON-NLS-1$
                            return null;
                        }
                    } else {
                        IPath projLocation= project.getProject().getLocation();
                        if (projLocation != null && path.toFile().exists()) {
                            rootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.AlreadyExistingDifferentCase")); //$NON-NLS-1$
                            return null;
                        }
                    }
                }
                monitor.worked(1);
                ArrayList newEntries= new ArrayList(fEntries.length + 1);
                
                for (int i= 0; i < fEntries.length; i++) {
                    IClasspathEntry curr= fEntries[i];
                    if (curr.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                        if (path.equals(curr.getPath())) {
                            rootStatus.setError(NewWizardMessages.getString("NewSourceFolderWizardPage.error.AlreadyExisting")); //$NON-NLS-1$
                            return null;
                        }
                    }
                    newEntries.add(curr);
                }
                
                monitor.worked(3);
                
                IClasspathEntry newEntry= JavaCore.newSourceEntry(path);
                CPListElement elem= CPListElement.createFromExisting(newEntry, project);
                
                Iterator iterator= newEntries.iterator();
                while(iterator.hasNext()) {
                    IClasspathEntry entry= (IClasspathEntry) iterator.next();
                    if (entry.getPath().matchingFirstSegments(path) == path.segmentCount()) {
                        exclude(entry.getPath().removeFirstSegments(path.segmentCount()).toString(), null, elem, project, null);
                    }
                }
                
                monitor.worked(3);
                
                newEntry= elem.getClasspathEntry();
                IPath outputLocation= project.getOutputLocation();
                
                if (removeProjectAsRoot)
                    newEntries.remove(0);
                newEntries.add(newEntry);
                
                monitor.worked(1);
                
                fEntries= (IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]);
                IJavaModelStatus status= JavaConventions.validateClasspath(project, fEntries, outputLocation);
                if (!status.isOK()) {
                    if (outputLocation.equals(projPath)) {
                        IStatus status2= JavaConventions.validateClasspath(project, fEntries, outputLocation);
                        if (status2.isOK()) {
                          if (project.isOnClasspath(project.getUnderlyingResource())) {
                              rootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceSFandOL", outputLocation.makeRelative().toString())); //$NON-NLS-1$
                          } else {
                              rootStatus.setInfo(NewWizardMessages.getFormattedString("NewSourceFolderWizardPage.warning.ReplaceOL", outputLocation.makeRelative().toString())); //$NON-NLS-1$
                          }
                          return fEntries;
                        }
                    }
                    rootStatus.setError(status.getMessage());
                    return null;
                }
                
                if (getClasspathEntryFor(project.getPath(), project) != null || project.getPath().equals(path)) {
                    rootStatus.setWarning(NewWizardMessages.getString("NewSourceFolderWizardPage.warning.ReplaceSF")); //$NON-NLS-1$
                    return fEntries;
                }
                
                rootStatus.setOK();
                return fEntries;
            }
        } finally {
            monitor.done();
        }
        return null;
    }
    
    /**
     * Test if the provided kind is of type
     * <code>IClasspathEntry.CPE_SOURCE</code>
     * 
     * @param kind the kind to be checked
     * @return <code>true</code> if kind equals
     * <code>IClasspathEntry.CPE_SOURCE</code>, 
     * <code>false</code> otherwise
     */
    private static boolean isEntryKind(int kind) {
        return kind == IClasspathEntry.CPE_SOURCE;
    }
    
    /**
     * Event fired whenever a classpathentry has changed.
     * The event parameter corresponds to the 
     * new <code>IClasspathEntry</code>
     * 
     * @param newEntry
     * 
     * @see #addToClasspath(IPath, IJavaProject, IOutputFolderQuery, IProgressMonitor)
     * @see #removeFromClasspath(IPackageFragmentRoot, IJavaProject, IProgressMonitor)
     */
    private void fireEvent(IClasspathEntry newEntry, int type) {
        if (fListener != null)
            fListener.classpathEntryChanged(newEntry, type);
    }
}
