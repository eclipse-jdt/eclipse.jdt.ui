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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

/**
 * As addition to the JavaCapabilityConfigurationPage, the wizard does an
 * early project creation (so that linked folders can be defined) and, if an
 * existing external location was specified, offers to do a classpath detection
 */
public class JavaProjectWizardSecondPage extends JavaCapabilityConfigurationPage {

	private final JavaProjectWizardFirstPage fFirstPage;

	protected IPath fCurrProjectLocation;
	protected IProject fCurrProject;
	
	protected boolean fKeepContent;
    private List fRemoveList;

	/**
	 * Constructor for NewProjectCreationWizardPage.
	 */
	public JavaProjectWizardSecondPage(JavaProjectWizardFirstPage mainPage) {
		super();
		fFirstPage= mainPage;
		fCurrProjectLocation= null;
		fCurrProject= null;
		fKeepContent= false;
        fRemoveList= new ArrayList();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			changeToNewProject();
		} else {
            deleteCreatedResources(); // delete new folders
			removeProject();
		}
		super.setVisible(visible);
	}
	
	private void changeToNewProject() {
		fKeepContent= fFirstPage.getDetect();
		
		final IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
                    monitor.beginTask(NewWizardMessages.getString("JavaProjectWizardSecondPage.operation.create"), 3); //$NON-NLS-1$
                    updateProject(new SubProgressMonitor(monitor, 11));
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (OperationCanceledException e) {
					throw new InterruptedException();
				} finally {
                    monitor.done();
                }
			}
		};
	
		try {
			getContainer().run(true, false, new WorkspaceModifyDelegatingOperation(op));
		} catch (InvocationTargetException e) {
			final String title= NewWizardMessages.getString("JavaProjectWizardSecondPage.error.title"); //$NON-NLS-1$
			final String message= NewWizardMessages.getString("JavaProjectWizardSecondPage.error.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}
	
	protected void updateProject(IProgressMonitor monitor) throws CoreException {
		
		fCurrProject= fFirstPage.getProjectHandle();
		fCurrProjectLocation= fFirstPage.getLocationPath();
        boolean removeProjectFile= false;
        boolean removeOutputFolder= false;
        boolean removeClasspathFile= false;
		
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.beginTask(NewWizardMessages.getString("JavaProjectWizardSecondPage.operation.initialize"), 7); //$NON-NLS-1$
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
            File file= new File(fCurrProjectLocation.append(fCurrProject.getName()).append(".classpath").toString()); //$NON-NLS-1$
            if (!file.exists())
                removeClasspathFile= true;
            file= new File(fCurrProjectLocation.append(fCurrProject.getName()).append(".project").toString()); //$NON-NLS-1$
            if (!file.exists())
                removeProjectFile= true;
			createProject(fCurrProject, fCurrProjectLocation, new SubProgressMonitor(monitor, 1));
				
			IClasspathEntry[] entries= null;
			IPath outputLocation= null;
	
			if (fFirstPage.getDetect()) {
				if (!fCurrProject.getFile(".classpath").exists()) { //$NON-NLS-1$
					final ClassPathDetector detector= new ClassPathDetector(fCurrProject);
					entries= detector.getClasspath();
                    outputLocation= detector.getOutputLocation();
                    if (outputLocation == null || !fCurrProject.getFolder(outputLocation.removeFirstSegments(fCurrProject.getFullPath().segmentCount())).exists())
                        removeOutputFolder= true;
                    outputLocation= detector.getOutputLocation();
				}
			} else if (fFirstPage.isSrcBin()) {
				IPreferenceStore store= PreferenceConstants.getPreferenceStore();
				IPath srcPath= new Path(store.getString(PreferenceConstants.SRCBIN_SRCNAME));
				IPath binPath= new Path(store.getString(PreferenceConstants.SRCBIN_BINNAME));
				
				if (srcPath.segmentCount() > 0) {
					IFolder folder= fCurrProject.getFolder(srcPath);
					CoreUtility.createFolder(folder, true, true, null);
				}
				
				if (binPath.segmentCount() > 0 && !binPath.equals(srcPath)) {
					IFolder folder= fCurrProject.getFolder(binPath);
					CoreUtility.createFolder(folder, true, true, null);
				}
				
				final IPath projectPath= fCurrProject.getFullPath();

				// configure the classpath entries, including the default jre library.
				List cpEntries= new ArrayList();
				cpEntries.add(JavaCore.newSourceEntry(projectPath.append(srcPath)));
				cpEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));
				entries= (IClasspathEntry[]) cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
				
				// configure the output location
				outputLocation= projectPath.append(binPath);
			} else {
				IPath projectPath= fCurrProject.getFullPath();
				List cpEntries= new ArrayList();
				cpEntries.add(JavaCore.newSourceEntry(projectPath));
				cpEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));
				entries= (IClasspathEntry[]) cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);

				outputLocation= projectPath;
			}
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			
            IJavaProject jProject= null;
            jProject= internalConfigureJavaProject(entries, outputLocation, monitor);
            init(jProject == null ? JavaCore.create(fCurrProject) : jProject, outputLocation, entries, false);
            fRemoveList= new ArrayList();
            if (removeClasspathFile)
                fRemoveList.add(fCurrProject.getFile(".classpath")); //$NON-NLS-1$
            if (removeProjectFile)
                fRemoveList.add(fCurrProject.getFile(".project")); //$NON-NLS-1$
            if (removeOutputFolder && outputLocation != null)
                fRemoveList.add(fCurrProject.getFolder(outputLocation.removeFirstSegments(fCurrProject.getFullPath().segmentCount())));
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Called from the wizard on finish.
	 */
	public void performFinish(IProgressMonitor monitor) throws CoreException, InterruptedException {
		try {
			monitor.beginTask(NewWizardMessages.getString("JavaProjectWizardSecondPage.operation.create"), 3); //$NON-NLS-1$
			if (fCurrProject == null) {
				updateProject(new SubProgressMonitor(monitor, 1));
			}
			configureJavaProject(new SubProgressMonitor(monitor, 2));
		} finally {
			monitor.done();
			fCurrProject= null;
		}
	}

	private void removeProject() { 
		if (fCurrProject == null || !fCurrProject.exists()) {
			return;
		}
		
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {

				final boolean noProgressMonitor= Platform.getLocation().equals(fCurrProjectLocation);

				if (monitor == null || noProgressMonitor) {
					monitor= new NullProgressMonitor();
				}

				monitor.beginTask(NewWizardMessages.getString("JavaProjectWizardSecondPage.operation.remove"), 3); //$NON-NLS-1$

				try {
                    boolean removeContent= !fKeepContent && fCurrProject.isSynchronized(IResource.DEPTH_INFINITE);
                    fCurrProject.delete(removeContent, false, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
					fCurrProject= null;
					fKeepContent= false;
				}
			}
		};
	
		try {
			getContainer().run(true, true, new WorkspaceModifyDelegatingOperation(op));
		} catch (InvocationTargetException e) {
			final String title= NewWizardMessages.getString("JavaProjectWizardSecondPage.error.remove.title"); //$NON-NLS-1$
			final String message= NewWizardMessages.getString("JavaProjectWizardSecondPage.error.remove.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);		
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}		
			
	/**
	 * Called from the wizard on cancel.
	 */
	public void performCancel() {
        deleteCreatedResources(); // delete new folders
		removeProject();
	}
    
    private void deleteCreatedResources() {
        Iterator iterator= fRemoveList.iterator();
        while(iterator.hasNext()) {
            IResource resource= (IResource)iterator.next();
            try {
                resource.delete(true, null);
            } catch (CoreException e) {
                // do nothing
            }
        }
        super.performCancel(); // undo changes
    }
    
    /*
     * Creates the Java project and sets the configured build path and output location.
     * If the project already exists only build paths are updated.
     */
    private IJavaProject internalConfigureJavaProject(IClasspathEntry[] entries, IPath outputLocation, final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
        // 10 monitor steps to go
        IJavaProject jProject= JavaCore.create(fCurrProject);
        IWorkspaceRoot workspaceRoot= fCurrProject.getWorkspace().getRoot();
        
        // create and set the output path first
        if (!workspaceRoot.exists(outputLocation)) {
            IFolder folder= workspaceRoot.getFolder(outputLocation);
            CoreUtility.createFolder(folder, true, true, null);
            folder.setDerived(true);        
        }
        
        boolean projectExists= (fCurrProject.exists() && fCurrProject.getFile(".classpath").exists()); //$NON-NLS-1$
        if  (projectExists) {
            if (outputLocation == null) {
                outputLocation=  jProject.readOutputLocation();
            }
            if (entries == null) {
                entries=  jProject.readRawClasspath();
            }
        }
        if (entries == null)
            entries= getDefaultClassPath(jProject);
        if (outputLocation == null) {
            outputLocation= getDefaultBuildPath(jProject);
        }
        
        List sortedEntries= new ArrayList();
        // create and set the class path
        for (int i= 0; i < entries.length; i++) {
            CPListElement entry= CPListElement.createFromExisting(entries[i], jProject);
            IResource res= entry.getResource();
            insertSorted(entries[i], sortedEntries, jProject);
            if ((res instanceof IFolder) && !res.exists()) {
                CoreUtility.createFolder((IFolder)res, true, true, null);
            }
        }   
        entries= (IClasspathEntry[]) sortedEntries.toArray(new IClasspathEntry[sortedEntries.size()]);
        
        final CoreException[] exception= {null};
        BuildPathsBlock.addJavaNature(fCurrProject, new SubProgressMonitor(monitor, 5));
        if (exception[0] != null)
            throw exception[0];
        jProject.setRawClasspath(entries, outputLocation, new SubProgressMonitor(monitor, 5));
        return jProject;
    }
    
    private IClasspathEntry[] getDefaultClassPath(IJavaProject jproj) {
        List list= new ArrayList();
        IResource srcFolder;
        IPreferenceStore store= PreferenceConstants.getPreferenceStore();
        String sourceFolderName= store.getString(PreferenceConstants.SRCBIN_SRCNAME);
        if (store.getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ) && sourceFolderName.length() > 0) {
            srcFolder= jproj.getProject().getFolder(sourceFolderName);
        } else {
            srcFolder= jproj.getProject();
        }

        list.add(new CPListElement(jproj, IClasspathEntry.CPE_SOURCE, srcFolder.getFullPath(), srcFolder));

        IClasspathEntry[] jreEntries= PreferenceConstants.getDefaultJRELibrary();
        IClasspathEntry[] entries= new IClasspathEntry[jreEntries.length + 1];
        System.arraycopy(jreEntries, 0, entries, 1, jreEntries.length);
        entries[0]= JavaCore.newSourceEntry(srcFolder.getFullPath());
        return entries;
    }
    
    private IPath getDefaultBuildPath(IJavaProject jproj) {
        IPreferenceStore store= PreferenceConstants.getPreferenceStore();
        if (store.getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ)) {
            String outputLocationName= store.getString(PreferenceConstants.SRCBIN_BINNAME);
            return jproj.getProject().getFullPath().append(outputLocationName);
        } else {
            return jproj.getProject().getFullPath();
        }
    }
    
    /**
     * Insert the entry into the list of other <code>IClasspathEntries</code> in the following way:
     * if the new entry is a source entry, then insert it sorted by path into the existing 
     * src entries, otherwise just append it at the end. 
     * 
     * @param entry the entry to be inserted
     * @param list a sorted list of <code>IClasspathEntries</code>
     * @param project the java project
     */
    private void insertSorted(IClasspathEntry entry, List list, IJavaProject project) {
        if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
            if (entry.getPath().equals(project.getPath()))
                list.add(0, entry); // project as root is always the first element
            else {
                // insert sorted among all source entries
                int i;
                for(i= 0; i < list.size(); i++) {
                    IClasspathEntry curr= (IClasspathEntry)list.get(i);
                    if (curr.getEntryKind() != IClasspathEntry.CPE_SOURCE) { // begin of entries which are not src entries --> insert
                        continue;
                    } else if (curr.getPath().toString().compareTo(entry.getPath().toString()) > 0) {
                        break;
                    }
                }
                list.add(i, entry);
            }
        } else {
            list.add(entry);
        }
    }
}
