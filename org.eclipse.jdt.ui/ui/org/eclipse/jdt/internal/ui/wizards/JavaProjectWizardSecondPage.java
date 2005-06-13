/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;

/**
 * As addition to the JavaCapabilityConfigurationPage, the wizard does an
 * early project creation (so that linked folders can be defined) and, if an
 * existing external location was specified, offers to do a classpath detection
 */
public class JavaProjectWizardSecondPage extends JavaCapabilityConfigurationPage {

	private static final String FILENAME_PROJECT= ".project"; //$NON-NLS-1$
	private static final String FILENAME_CLASSPATH= ".classpath"; //$NON-NLS-1$

	private final JavaProjectWizardFirstPage fFirstPage;

	private IPath fCurrProjectLocation;
	private IProject fCurrProject;
	
	private boolean fKeepContent;

	private File fDotProjectBackup;
	private File fDotClasspathBackup;
	private Boolean fIsAutobuild;

	/**
	 * Constructor for JavaProjectWizardSecondPage.
	 */
	public JavaProjectWizardSecondPage(JavaProjectWizardFirstPage mainPage) {
		fFirstPage= mainPage;
		fCurrProjectLocation= null;
		fCurrProject= null;
		fKeepContent= false;
		
		fDotProjectBackup= null;
		fDotClasspathBackup= null;
		fIsAutobuild= null;
	}
	
	protected boolean useNewSourcePage() {
		return true;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			changeToNewProject();
		} else {
			removeProject();
		}
		super.setVisible(visible);
	}
	
	private void changeToNewProject() {
		fKeepContent= fFirstPage.getDetect();
		
		final IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					if (fIsAutobuild == null) {
						fIsAutobuild= Boolean.valueOf(CoreUtility.enableAutoBuild(false));
					}
                    updateProject(monitor);
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
			final String title= NewWizardMessages.JavaProjectWizardSecondPage_error_title; 
			final String message= NewWizardMessages.JavaProjectWizardSecondPage_error_message; 
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}
	
	final void updateProject(IProgressMonitor monitor) throws CoreException, InterruptedException {
		
		fCurrProject= fFirstPage.getProjectHandle();
		fCurrProjectLocation= fFirstPage.getLocationPath();
		
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.beginTask(NewWizardMessages.JavaProjectWizardSecondPage_operation_initialize, 7); 
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
						
			IPath realLocation= fCurrProjectLocation;
			if (Platform.getLocation().equals(fCurrProjectLocation)) {
				realLocation= fCurrProjectLocation.append(fCurrProject.getName());		
			}

			rememberExistingFiles(realLocation);
            
			createProject(fCurrProject, fCurrProjectLocation, new SubProgressMonitor(monitor, 2));
				
			IClasspathEntry[] entries= null;
			IPath outputLocation= null;
	
			if (fFirstPage.getDetect()) {
				if (!fCurrProject.getFile(FILENAME_CLASSPATH).exists()) { //$NON-NLS-1$
					final ClassPathDetector detector= new ClassPathDetector(fCurrProject, new SubProgressMonitor(monitor, 2));
					entries= detector.getClasspath();
                    outputLocation= detector.getOutputLocation();
				} else {
					monitor.worked(2);
				}
			} else if (fFirstPage.isSrcBin()) {
				IPreferenceStore store= PreferenceConstants.getPreferenceStore();
				IPath srcPath= new Path(store.getString(PreferenceConstants.SRCBIN_SRCNAME));
				IPath binPath= new Path(store.getString(PreferenceConstants.SRCBIN_BINNAME));
				
				if (srcPath.segmentCount() > 0) {
					IFolder folder= fCurrProject.getFolder(srcPath);
					CoreUtility.createFolder(folder, true, true, new SubProgressMonitor(monitor, 1));
				} else {
					monitor.worked(1);
				}
				
				if (binPath.segmentCount() > 0 && !binPath.equals(srcPath)) {
					IFolder folder= fCurrProject.getFolder(binPath);
					CoreUtility.createFolder(folder, true, true, new SubProgressMonitor(monitor, 1));
				} else {
					monitor.worked(1);
				}
				
				final IPath projectPath= fCurrProject.getFullPath();

				// configure the classpath entries, including the default jre library.
				List cpEntries= new ArrayList();
				cpEntries.add(JavaCore.newSourceEntry(projectPath.append(srcPath)));
				cpEntries.addAll(Arrays.asList(getDefaultClasspathEntry()));
				entries= (IClasspathEntry[]) cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
				
				// configure the output location
				outputLocation= projectPath.append(binPath);
			} else {
				IPath projectPath= fCurrProject.getFullPath();
				List cpEntries= new ArrayList();
				cpEntries.add(JavaCore.newSourceEntry(projectPath));
				cpEntries.addAll(Arrays.asList(getDefaultClasspathEntry()));
				entries= (IClasspathEntry[]) cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);

				outputLocation= projectPath;
				monitor.worked(2);
			}
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			
            init(JavaCore.create(fCurrProject), outputLocation, entries, false);
			configureJavaProject(new SubProgressMonitor(monitor, 3)); // create the Java project to allow the use of the new source folder page
		} finally {
			monitor.done();
		}
	}
	
	private IClasspathEntry[] getDefaultClasspathEntry() {
		IClasspathEntry[] defaultJRELibrary= PreferenceConstants.getDefaultJRELibrary();
		String compliance= fFirstPage.getJRECompliance();
		IPath jreContainerPath= new Path(JavaRuntime.JRE_CONTAINER);
		if (compliance == null || defaultJRELibrary.length > 1 || !jreContainerPath.isPrefixOf(defaultJRELibrary[0].getPath())) {
			// use default
			return defaultJRELibrary;
		}
		// try to find a compatible JRE
		IVMInstall inst= BuildPathSupport.findMatchingJREInstall(compliance);
		if (inst != null) {
			IPath newPath= jreContainerPath.append(inst.getVMInstallType().getId()).append(inst.getName());
			return new IClasspathEntry[] { JavaCore.newContainerEntry(newPath) };
		}
		return defaultJRELibrary;
	}
	
	private void rememberExistingFiles(IPath currProjectLocation) throws CoreException {
		fDotProjectBackup= null;
		fDotClasspathBackup= null;
		
		File file= currProjectLocation.toFile();
		if (file.exists()) {
			File projectFile= new File(file, FILENAME_PROJECT);
			if (projectFile.exists()) {
				fDotProjectBackup= createBackup(projectFile, "project-desc"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			File classpathFile= new File(file, FILENAME_CLASSPATH);
			if (classpathFile.exists()) {
				fDotClasspathBackup= createBackup(classpathFile, "classpath-desc"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	private void restoreExistingFiles(File projectLocation) throws CoreException {
		try {
			if (fDotProjectBackup != null) {
				File projectFile= new File(projectLocation, FILENAME_PROJECT);
				if (projectFile.delete()) {
					copyFile(fDotProjectBackup, projectFile);
				}
			}
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, NewWizardMessages.JavaProjectWizardSecondPage_problem_restore_project, e); 
			throw new CoreException(status);
		}
		try {
			if (fDotClasspathBackup != null) {
				File classpathFile= new File(projectLocation, FILENAME_CLASSPATH);
				if (classpathFile.delete()) {
					copyFile(fDotClasspathBackup, classpathFile);
				}
			}
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, NewWizardMessages.JavaProjectWizardSecondPage_problem_restore_classpath, e); 
			throw new CoreException(status);
		}
	}
	
	private File createBackup(File file, String name) throws CoreException {
		try {
			File bak= File.createTempFile("eclipse-" + name, "bak");  //$NON-NLS-1$//$NON-NLS-2$
			copyFile(file, bak);
			return bak;
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, Messages.format(NewWizardMessages.JavaProjectWizardSecondPage_problem_backup, name), e); 
			throw new CoreException(status);
		} 
	}
			
	private void copyFile(File file, File target) throws IOException {		
		FileInputStream is= new FileInputStream(file);
		FileOutputStream os= new FileOutputStream(target);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead= is.read(buffer);
				if (bytesRead == -1)
					break;
				
				os.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				is.close();
			} finally {
				os.close();
			}
		}
	}
	
	/**
	 * Called from the wizard on finish.
	 */
	public void performFinish(IProgressMonitor monitor) throws CoreException, InterruptedException {
		try {
			monitor.beginTask(NewWizardMessages.JavaProjectWizardSecondPage_operation_create, 3); 
			if (fCurrProject == null) {
				updateProject(new SubProgressMonitor(monitor, 1));
			}
			configureJavaProject(new SubProgressMonitor(monitor, 2));
			String compliance= fFirstPage.getJRECompliance();
			if (compliance != null) {
				IJavaProject project= JavaCore.create(fCurrProject);
				Map options= project.getOptions(false);
				JavaModelUtil.setCompilanceOptions(options, compliance);
				project.setOptions(options);
			}
		} finally {
			monitor.done();
			fCurrProject= null;
			if (fIsAutobuild != null) {
				CoreUtility.enableAutoBuild(fIsAutobuild.booleanValue());
				fIsAutobuild= null;
			}
		}
	}

	private void removeProject() { 
		if (fCurrProject == null || !fCurrProject.exists()) {
			return;
		}
		
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				doRemoveProject(monitor);
			}
		};
	
		try {
			getContainer().run(true, true, new WorkspaceModifyDelegatingOperation(op));
		} catch (InvocationTargetException e) {
			final String title= NewWizardMessages.JavaProjectWizardSecondPage_error_remove_title; 
			final String message= NewWizardMessages.JavaProjectWizardSecondPage_error_remove_message; 
			ExceptionHandler.handle(e, getShell(), title, message);		
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}
	
	final void doRemoveProject(IProgressMonitor monitor) throws InvocationTargetException {
		final boolean noProgressMonitor= Platform.getLocation().equals(fCurrProjectLocation);
		if (monitor == null || noProgressMonitor) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask(NewWizardMessages.JavaProjectWizardSecondPage_operation_remove, 3); 
		try {
			try {
				File projLoc= fCurrProject.getLocation().toFile();
				
			    boolean removeContent= !fKeepContent && fCurrProject.isSynchronized(IResource.DEPTH_INFINITE);
			    fCurrProject.delete(removeContent, false, monitor);
				
				restoreExistingFiles(projLoc);
			} finally {
				CoreUtility.enableAutoBuild(fIsAutobuild.booleanValue()); // fIsAutobuild must be set
				fIsAutobuild= null;
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
			fCurrProject= null;
			fKeepContent= false;
		}
	}

	/**
	 * Called from the wizard on cancel.
	 */
	public void performCancel() {
		removeProject();
	}

        
 }
