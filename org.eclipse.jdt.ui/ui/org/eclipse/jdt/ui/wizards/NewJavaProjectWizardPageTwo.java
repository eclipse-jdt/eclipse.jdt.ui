/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.wizards;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.InfoFilesUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.Progress;
import org.eclipse.jdt.internal.ui.wizards.ClassPathDetector;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * The second page of the New Java project wizard. It allows to configure the build path and output location.
 * As addition to the {@link JavaCapabilityConfigurationPage}, the wizard page does an
 * early project creation (so that linked folders can be defined) and, if an
 * existing external location was specified, detects the class path.
 *
 * <p>
 * Clients may instantiate or subclass.
 * </p>
 *
 * @since 3.4
 */
public class NewJavaProjectWizardPageTwo extends JavaCapabilityConfigurationPage {

	private static final String FILENAME_PROJECT= ".project"; //$NON-NLS-1$
	private static final String FILENAME_CLASSPATH= ".classpath"; //$NON-NLS-1$

	private final NewJavaProjectWizardPageOne fFirstPage;

	private URI fCurrProjectLocation; // null if location is platform location
	private IProject fCurrProject;

	private boolean fKeepContent;

	private File fDotProjectBackup;
	private File fDotClasspathBackup;
	private Boolean fIsAutobuild;
	private HashSet<IFileStore> fOrginalFolders;

	/**
	 * Constructor for the {@link NewJavaProjectWizardPageTwo}.
	 *
	 * @param mainPage the first page of the wizard
	 */
	public NewJavaProjectWizardPageTwo(NewJavaProjectWizardPageOne mainPage) {
		fFirstPage= mainPage;
		fCurrProjectLocation= null;
		fCurrProject= null;
		fKeepContent= false;

		fDotProjectBackup= null;
		fDotClasspathBackup= null;
		fIsAutobuild= null;
	}

	@Override
	protected final boolean useNewSourcePage() {
		return true;
	}


	@Override
	public void setVisible(boolean visible) {
		boolean isShownFirstTime= visible && fCurrProject == null;
		if (visible) {
			if (isShownFirstTime) { // entering from the first page
				createProvisonalProject();
			}
		} else {
			if (getContainer().getCurrentPage() == fFirstPage) { // leaving back to the first page
				removeProvisonalProject();
			}
		}
		super.setVisible(visible);
		if (isShownFirstTime) {
			setFocus();
		}
	}



	private boolean hasExistingContent(URI realLocation) throws CoreException {
		IFileStore file= EFS.getStore(realLocation);
		return file.fetchInfo().exists();
	}

	private IStatus changeToNewProject() {
		class UpdateRunnable implements IRunnableWithProgress {
			public IStatus infoStatus= Status.OK_STATUS;

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					if (fIsAutobuild == null) {
						fIsAutobuild= CoreUtility.setAutoBuilding(false);
					}
					infoStatus= updateProject(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (OperationCanceledException e) {
					throw new InterruptedException();
				} finally {
					monitor.done();
				}
			}
		}
		UpdateRunnable op= new UpdateRunnable();
		try {
			getContainer().run(true, false, new WorkspaceModifyDelegatingOperation(op));
			return op.infoStatus;
		} catch (InvocationTargetException e) {
			final String title= NewWizardMessages.NewJavaProjectWizardPageTwo_error_title;
			final String message= NewWizardMessages.NewJavaProjectWizardPageTwo_error_message;
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch  (InterruptedException e) {
			// cancel pressed
		}
		return null;
	}

	private static URI getRealLocation(String projectName, URI location) {
		if (location == null) {  // inside workspace
			try {
				URI rootLocation= ResourcesPlugin.getWorkspace().getRoot().getLocationURI();

				location= new URI(rootLocation.getScheme(), null,
						Path.fromPortableString(rootLocation.getPath()).append(projectName).toString(),
						null);
			} catch (URISyntaxException e) {
				Assert.isTrue(false, "Can't happen"); //$NON-NLS-1$
			}
		}
		return location;
	}

	private final IStatus updateProject(IProgressMonitor monitor) throws CoreException, InterruptedException {
		IStatus result= StatusInfo.OK_STATUS;
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.beginTask(NewWizardMessages.NewJavaProjectWizardPageTwo_operation_initialize, 7);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			String projectName= fFirstPage.getProjectName();

			fCurrProject= ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			fCurrProjectLocation= fFirstPage.getProjectLocationURI();

			URI realLocation= getRealLocation(projectName, fCurrProjectLocation);
			fKeepContent= hasExistingContent(realLocation);

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			if (fKeepContent) {
				rememberExistingFiles(realLocation);
				rememberExisitingFolders(realLocation);
			}

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			try {
				createProject(fCurrProject, fCurrProjectLocation, Progress.subMonitor(monitor, 2));
			} catch (CoreException e) {
				if (e.getStatus().getCode() == IResourceStatus.FAILED_READ_METADATA) {
					result= new StatusInfo(IStatus.INFO, Messages.format(NewWizardMessages.NewJavaProjectWizardPageTwo_DeleteCorruptProjectFile_message, e.getLocalizedMessage()));

					deleteProjectFile(realLocation);
					if (fCurrProject.exists())
						fCurrProject.delete(true, null);

					createProject(fCurrProject, fCurrProjectLocation, null);
				} else {
					throw e;
				}
			}

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			initializeBuildPath(JavaCore.create(fCurrProject), Progress.subMonitor(monitor, 2));
			configureJavaProject(Progress.subMonitor(monitor, 3)); // create the Java project to allow the use of the new source folder page
		} finally {
			monitor.done();
		}
		return result;
	}

	/**
	 * Evaluates the new build path and output folder according to the settings on the first page.
	 * The resulting build path is set by calling {@link #init(IJavaProject, IPath, IClasspathEntry[], boolean)}.
	 * Clients can override this method.
	 *
	 * @param javaProject the new project which is already created when this method is called.
	 * @param monitor the progress monitor
	 * @throws CoreException thrown when initializing the build path failed
	 */
	protected void initializeBuildPath(IJavaProject javaProject, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask(NewWizardMessages.NewJavaProjectWizardPageTwo_monitor_init_build_path, 2);

		try {
			IClasspathEntry[] entries= null;
			IPath outputLocation= null;
			IProject project= javaProject.getProject();

			if (fKeepContent) {
				if (!project.getFile(FILENAME_CLASSPATH).exists()) {
					final ClassPathDetector detector= new ClassPathDetector(fCurrProject, Progress.subMonitor(monitor, 2));
					entries= detector.getClasspath();
					outputLocation= detector.getOutputLocation();
					if (entries.length == 0)
						entries= null;
				} else {
					monitor.worked(2);
				}
			} else {
				List<IClasspathEntry> cpEntries= new ArrayList<>();
				IWorkspaceRoot root= project.getWorkspace().getRoot();

				for (IClasspathEntry sourceClasspathEntry : fFirstPage.getSourceClasspathEntries()) {
					IPath path= sourceClasspathEntry.getPath();
					if (path.segmentCount() > 1) {
						IFolder folder= root.getFolder(path);
						CoreUtility.createFolder(folder, true, true, Progress.subMonitor(monitor, 1));
					}
					cpEntries.add(sourceClasspathEntry);
				}

				cpEntries.addAll(0, Arrays.asList(fFirstPage.getDefaultClasspathEntries()));

				entries= cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);

				outputLocation= fFirstPage.getOutputLocation();
				if (outputLocation.segmentCount() > 1) {
					IFolder folder= root.getFolder(outputLocation);
					CoreUtility.createDerivedFolder(folder, true, true, Progress.subMonitor(monitor, 1));
				}
			}
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			init(javaProject, outputLocation, entries, false);
		} finally {
			monitor.done();
		}
	}

	private void deleteProjectFile(URI projectLocation) throws CoreException {
		IFileStore file= EFS.getStore(projectLocation);
		if (file.fetchInfo().exists()) {
			IFileStore projectFile= file.getChild(FILENAME_PROJECT);
			if (projectFile.fetchInfo().exists()) {
				projectFile.delete(EFS.NONE, null);
			}
		}
	}

	private void rememberExisitingFolders(URI projectLocation) {
		fOrginalFolders= new HashSet<>();

		try {
			for (IFileStore child : EFS.getStore(projectLocation).childStores(EFS.NONE, null)) {
				IFileInfo info= child.fetchInfo();
				if (info.isDirectory() && info.exists()) {
					fOrginalFolders.add(child);
				}
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
	}

	private void restoreExistingFolders(URI projectLocation) {
		HashSet<IFileStore> foldersToKeep= new HashSet<>(fOrginalFolders);
		// workaround for bug 319054: Eclipse deletes all files when I cancel a project creation (symlink in project location path)
		for (IFileStore originalFileStore : fOrginalFolders) {
			try {
				File localFile= originalFileStore.toLocalFile(EFS.NONE, null);
				if (localFile != null) {
					File canonicalFile= localFile.getCanonicalFile();
					IFileStore canonicalFileStore= originalFileStore.getFileSystem().fromLocalFile(canonicalFile);
					if (! originalFileStore.equals(canonicalFileStore)) {
						foldersToKeep.add(canonicalFileStore);
					}
				}
			} catch (IOException | CoreException e) {
			}
		}

		try {
			for (IFileStore child : EFS.getStore(projectLocation).childStores(EFS.NONE, null)) {
				IFileInfo info= child.fetchInfo();
				if (info.isDirectory() && info.exists() && !foldersToKeep.contains(child)) {
					child.delete(EFS.NONE, null);
					fOrginalFolders.remove(child);
				}
			}

			for (IFileStore deleted : fOrginalFolders) {
				deleted.mkdir(EFS.NONE, null);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
	}

	private void rememberExistingFiles(URI projectLocation) throws CoreException {
		fDotProjectBackup= null;
		fDotClasspathBackup= null;

		IFileStore file= EFS.getStore(projectLocation);
		if (file.fetchInfo().exists()) {
			IFileStore projectFile= file.getChild(FILENAME_PROJECT);
			if (projectFile.fetchInfo().exists()) {
				fDotProjectBackup= createBackup(projectFile, "project-desc"); //$NON-NLS-1$
			}
			IFileStore classpathFile= file.getChild(FILENAME_CLASSPATH);
			if (classpathFile.fetchInfo().exists()) {
				fDotClasspathBackup= createBackup(classpathFile, "classpath-desc"); //$NON-NLS-1$
			}
		}
	}

	private void restoreExistingFiles(URI projectLocation, IProgressMonitor monitor) throws CoreException {
		int ticks= ((fDotProjectBackup != null ? 1 : 0) + (fDotClasspathBackup != null ? 1 : 0)) * 2;
		monitor.beginTask("", ticks); //$NON-NLS-1$
		try {
			IFileStore projectFile= EFS.getStore(projectLocation).getChild(FILENAME_PROJECT);
			projectFile.delete(EFS.NONE, Progress.subMonitor(monitor, 1));
			if (fDotProjectBackup != null) {
				copyFile(fDotProjectBackup, projectFile, Progress.subMonitor(monitor, 1));
			}
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, NewWizardMessages.NewJavaProjectWizardPageTwo_problem_restore_project, e);
			throw new CoreException(status);
		}
		try {
			IFileStore classpathFile= EFS.getStore(projectLocation).getChild(FILENAME_CLASSPATH);
			classpathFile.delete(EFS.NONE, Progress.subMonitor(monitor, 1));
			if (fDotClasspathBackup != null) {
				copyFile(fDotClasspathBackup, classpathFile, Progress.subMonitor(monitor, 1));
			}
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, NewWizardMessages.NewJavaProjectWizardPageTwo_problem_restore_classpath, e);
			throw new CoreException(status);
		}
	}

	private File createBackup(IFileStore source, String name) throws CoreException {
		try {
			File bak= File.createTempFile("eclipse-" + name, ".bak");  //$NON-NLS-1$//$NON-NLS-2$
			copyFile(source, bak);
			return bak;
		} catch (IOException e) {
			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, Messages.format(NewWizardMessages.NewJavaProjectWizardPageTwo_problem_backup, name), e);
			throw new CoreException(status);
		}
	}

	private void copyFile(IFileStore source, File target) throws IOException, CoreException {
		try(InputStream is= source.openInputStream(EFS.NONE, null)) {
			Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void copyFile(File source, IFileStore target, IProgressMonitor monitor) throws IOException, CoreException {
		try(OutputStream os= target.openOutputStream(EFS.NONE, monitor)){
			Files.copy(source.toPath(), os);
		}
	}

	/**
	 * Called from the wizard on finish.
	 *
	 * @param monitor the progress monitor
	 * @throws CoreException thrown when the project creation or configuration failed
	 * @throws InterruptedException thrown when the user cancelled the project creation
	 */
	public void performFinish(IProgressMonitor monitor) throws CoreException, InterruptedException {
		try {
			monitor.beginTask(NewWizardMessages.NewJavaProjectWizardPageTwo_operation_create, 3);
			if (fCurrProject == null) {
				updateProject(Progress.subMonitor(monitor, 1));
			}
			String newProjectCompliance= fKeepContent ? null : fFirstPage.getCompilerCompliance();
			configureJavaProject(newProjectCompliance, Progress.subMonitor(monitor, 2));
			createJavaProjectModuleInfoFile();
		} finally {
			monitor.done();
			fCurrProject= null;
			if (fIsAutobuild != null) {
				CoreUtility.setAutoBuilding(fIsAutobuild);
				fIsAutobuild= null;
			}
		}
	}

	/**
	 * Creates the provisional project on which the wizard is working on. The provisional project is typically
	 * created when the page is entered the first time. The early project creation is required to configure linked folders.
	 *
	 * @return the provisional project
	 */
	protected IProject createProvisonalProject() {
		IStatus status= changeToNewProject();
		if (status != null && !status.isOK()) {
			ErrorDialog.openError(getShell(), NewWizardMessages.NewJavaProjectWizardPageTwo_error_title, null, status);
		}
		return fCurrProject;
	}

	/**
	 * Removes the provisional project. The provisional project is typically removed when the user cancels the wizard or goes
	 * back to the first page.
	 */
	protected void removeProvisonalProject() {
		if (!fCurrProject.exists()) {
			fCurrProject= null;
			return;
		}

		IRunnableWithProgress op= this::doRemoveProject;

		try {
			getContainer().run(true, true, new WorkspaceModifyDelegatingOperation(op));
		} catch (InvocationTargetException e) {
			final String title= NewWizardMessages.NewJavaProjectWizardPageTwo_error_remove_title;
			final String message= NewWizardMessages.NewJavaProjectWizardPageTwo_error_remove_message;
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}

	private final void doRemoveProject(IProgressMonitor monitor) throws InvocationTargetException {
		final boolean noProgressMonitor= (fCurrProjectLocation == null); // inside workspace
		if (monitor == null || noProgressMonitor) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask(NewWizardMessages.NewJavaProjectWizardPageTwo_operation_remove, 3);
		try {
			try {
				URI projLoc= fCurrProject.getLocationURI();

				boolean removeContent= !fKeepContent && fCurrProject.isSynchronized(IResource.DEPTH_INFINITE);
				if (!removeContent) {
					restoreExistingFolders(projLoc);
				}
				fCurrProject.delete(removeContent, false, Progress.subMonitor(monitor, 2));

				restoreExistingFiles(projLoc, Progress.subMonitor(monitor, 1));
			} finally {
				CoreUtility.setAutoBuilding(fIsAutobuild); // fIsAutobuild must be set
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
		if (fCurrProject != null) {
			removeProvisonalProject();
		}
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	private void createJavaProjectModuleInfoFile() throws CoreException{
		boolean createModuleInfoFile= fFirstPage.getCreateModuleInfoFile();
		IPackageFragmentRoot packageFragmentRoot= getPackageFragmentRoot();
		IPackageFragment pkgFragment= (packageFragmentRoot == null) ? null : packageFragmentRoot.getPackageFragment(""); //$NON-NLS-1$
		if (createModuleInfoFile && pkgFragment != null) {
			String moduleName= fFirstPage.getModuleName();
			String moduleContent= "module " + moduleName + " {" +  System.lineSeparator() + "}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			InfoFilesUtil.createInfoJavaFile(JavaModelUtil.MODULE_INFO_JAVA, moduleContent, pkgFragment, fFirstPage.getGenerateCommentsInModuleInfoFile(), new NullProgressMonitor());
		}
	}

	private IPackageFragmentRoot getPackageFragmentRoot() {
		IJavaProject javaProject= getJavaProject();
		try {
			IPackageFragmentRoot[] packageFragmentRoots= javaProject.getPackageFragmentRoots();
			List<IPackageFragmentRoot> packageFragmentRootsAsList= new ArrayList<>(Arrays.asList(packageFragmentRoots));
			for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
				IResource res= packageFragmentRoot.getCorrespondingResource();
				if (res == null || res.getType() != IResource.FOLDER || packageFragmentRoot.getKind() != IPackageFragmentRoot.K_SOURCE) {
					packageFragmentRootsAsList.remove(packageFragmentRoot);
				}
			}
			packageFragmentRoots= packageFragmentRootsAsList.toArray(new IPackageFragmentRoot[packageFragmentRootsAsList.size()]);

			IPackageFragmentRoot targetPkgFragmentRoot= null;

			if (packageFragmentRoots.length > 0) {
				targetPkgFragmentRoot= packageFragmentRoots[0];
			}
			return targetPkgFragmentRoot;
		} catch (JavaModelException e) {
			return null;
		}

	}
}
