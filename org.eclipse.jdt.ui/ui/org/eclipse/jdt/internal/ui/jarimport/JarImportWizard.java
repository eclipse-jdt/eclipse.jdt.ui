/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarimport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IInitializableRefactoringComponent;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.core.refactoring.participants.GenericRefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.ui.refactoring.history.RefactoringHistoryControlConfiguration;
import org.eclipse.ltk.ui.refactoring.history.RefactoringHistoryWizard;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.JavaRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.binary.StubCreationOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

/**
 * Import wizard to import a refactoring-aware Java Archive (JAR) file.
 * <p>
 * This class may be instantiated and used without further configuration; this
 * class is not intended to be subclassed.
 * </p>
 * <p>
 * Example:
 * 
 * <pre>
 * IWizard wizard= new JarImportWizard();
 * wizard.init(workbench, selection);
 * WizardDialog dialog= new WizardDialog(shell, wizard);
 * dialog.open();
 * </pre>
 * 
 * During the call to <code>open</code>, the wizard dialog is presented to
 * the user. When the user hits Finish, the user-selected JAR file is inspected
 * for associated refactorings, the wizard executes eventual refactorings,
 * copies the JAR file over its old version, the dialog closes, and the call to
 * <code>open</code> returns.
 * </p>
 * 
 * @since 3.2
 */
public final class JarImportWizard extends RefactoringHistoryWizard implements IImportWizard {

	/** Proxy which requests the refactoring history from the import data */
	private final class RefactoringHistoryProxy extends RefactoringHistory {

		/** The cached refactoring history delta */
		private RefactoringDescriptorProxy[] fHistoryDelta= null;

		/**
		 * {@inheritDoc}
		 */
		public RefactoringDescriptorProxy[] getDescriptors() {
			if (fHistoryDelta != null)
				return fHistoryDelta;
			final RefactoringHistory incoming= fImportData.getRefactoringHistory();
			if (incoming != null) {
				fHistoryDelta= incoming.getDescriptors();
				final IPackageFragmentRoot root= fImportData.getPackageFragmentRoot();
				if (root != null) {
					try {
						final URI uri= getLocationURI(root.getRawClasspathEntry());
						if (uri != null) {
							final File file= new File(uri);
							if (file.exists()) {
								ZipFile zip= null;
								try {
									zip= new ZipFile(file, ZipFile.OPEN_READ);
									ZipEntry entry= zip.getEntry(JarPackagerUtil.getRefactoringsEntryName());
									if (entry != null) {
										InputStream stream= null;
										try {
											stream= zip.getInputStream(entry);
											final RefactoringHistory existing= RefactoringCore.getRefactoringHistoryService().readRefactoringHistory(stream, JavaRefactorings.IMPORTABLE);
											if (existing != null)
												fHistoryDelta= incoming.removeAll(existing).getDescriptors();
										} finally {
											if (stream != null) {
												try {
													stream.close();
												} catch (IOException exception) {
													// Do nothing
												}
											}
										}
									}
								} catch (IOException exception) {
									// Just leave it
								}
							}
						}
					} catch (CoreException exception) {
						JavaPlugin.log(exception);
					}
				}
				return fHistoryDelta;
			}
			return new RefactoringDescriptorProxy[0];
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isEmpty() {
			final RefactoringDescriptorProxy[] proxies= getDescriptors();
			if (proxies != null)
				return proxies.length == 0;
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		public RefactoringHistory removeAll(final RefactoringHistory history) {
			throw new UnsupportedOperationException();
		}
	}

	/** The dialog settings key */
	private static String DIALOG_SETTINGS_KEY= "JarImportWizard"; //$NON-NLS-1$

	/** The meta-inf fragment */
	private static final String META_INF_FRAGMENT= JarFile.MANIFEST_NAME.substring(0, JarFile.MANIFEST_NAME.indexOf('/'));

	/** The temporary linked source folder */
	private static final String SOURCE_FOLDER= ".src"; //$NON-NLS-1$

	/** The temporary stubs folder */
	private static final String STUB_FOLDER= ".stubs"; //$NON-NLS-1$

	/**
	 * Updates the new classpath with exclusion patterns for the specified path.
	 * 
	 * @param entries
	 *            the classpath entries
	 * @param path
	 *            the path
	 */
	private static void addExclusionPatterns(final List entries, final IPath path) {
		for (int index= 0; index < entries.size(); index++) {
			final IClasspathEntry entry= (IClasspathEntry) entries.get(index);
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE && entry.getPath().isPrefixOf(path)) {
				final IPath[] patterns= entry.getExclusionPatterns();
				if (!JavaModelUtil.isExcludedPath(path, patterns)) {
					final IPath[] filters= new IPath[patterns.length + 1];
					System.arraycopy(patterns, 0, filters, 0, patterns.length);
					filters[patterns.length]= path.removeFirstSegments(entry.getPath().segmentCount()).addTrailingSeparator();
					entries.set(index, JavaCore.newSourceEntry(entry.getPath(), filters, entry.getOutputLocation()));
				}
			}
		}
	}

	/**
	 * Checks whether the archive referenced by the package fragment root is not
	 * shared
	 * 
	 * @param root
	 *            the package fragment root
	 * @param monitor
	 *            the progress monitor to use
	 * @return the status of the operation
	 */
	private static RefactoringStatus checkPackageFragmentRoots(final IPackageFragmentRoot root, final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, 100);
			final IWorkspaceRoot workspace= ResourcesPlugin.getWorkspace().getRoot();
			if (workspace != null) {
				final IJavaModel model= JavaCore.create(workspace);
				if (model != null) {
					try {
						final URI uri= getLocationURI(root.getRawClasspathEntry());
						if (uri != null) {
							final IJavaProject[] projects= model.getJavaProjects();
							final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
							try {
								subMonitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, projects.length * 100);
								for (int index= 0; index < projects.length; index++) {
									final IPackageFragmentRoot[] roots= projects[index].getPackageFragmentRoots();
									final IProgressMonitor subsubMonitor= new SubProgressMonitor(subMonitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
									try {
										subsubMonitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, roots.length);
										for (int offset= 0; offset < roots.length; offset++) {
											final IPackageFragmentRoot current= roots[offset];
											if (!current.equals(root) && current.getKind() == IPackageFragmentRoot.K_BINARY) {
												final IClasspathEntry entry= current.getRawClasspathEntry();
												if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
													final URI location= getLocationURI(entry);
													if (uri.equals(location))
														status.addFatalError(MessageFormat.format(JarImportMessages.JarImportWizard_error_shared_jar, new String[] { current.getJavaProject().getElementName()}));
												}
											}
											subsubMonitor.worked(1);
										}
									} finally {
										subsubMonitor.done();
									}
								}
							} finally {
								subMonitor.done();
							}
						}
					} catch (JavaModelException exception) {
						status.addError(exception.getLocalizedMessage());
					}
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Returns the location URI of the classpath entry
	 * 
	 * @param entry
	 *            the classpath entry
	 * @return the location URI
	 */
	private static URI getLocationURI(final IClasspathEntry entry) {
		final IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		final IPath path= entry.getPath();
		URI location= null;
		if (root.exists(path)) {
			location= root.getFile(path).getRawLocationURI();
		} else
			location= new File(path.toOSString()).toURI();
		return location;
	}

	/**
	 * Is the specified class path entry pointing to a valid location for
	 * import?
	 * 
	 * @param entry
	 *            the class path entry
	 * @return <code>true</code> if it is a valid package fragment root,
	 *         <code>false</code> otherwise
	 */
	public static boolean isValidClassPathEntry(final IClasspathEntry entry) {
		Assert.isNotNull(entry);
		return entry.getContentKind() == IPackageFragmentRoot.K_BINARY && entry.getEntryKind() != IClasspathEntry.CPE_CONTAINER;
	}

	/**
	 * Is the specified java project a valid project for import?
	 * 
	 * @param project
	 *            the java project
	 * @throws JavaModelException
	 *             if an error occurs
	 */
	public static boolean isValidJavaProject(final IJavaProject project) throws JavaModelException {
		Assert.isNotNull(project);
		boolean result= false;
		final IProject resource= project.getProject();
		if (resource.isAccessible()) {
			try {
				result= true;
				final IProjectDescription description= resource.getDescription();
				final String[] ids= description.getNatureIds();
				for (int offset= 0; offset < ids.length && result; offset++) {
					if ("org.eclipse.pde.PluginNature".equals(ids[offset])) { //$NON-NLS-1$
						boolean found= false;
						final IClasspathEntry[] entries= project.getRawClasspath();
						for (int position= 0; position < entries.length && !found; position++) {
							if (entries[position].getContentKind() == IPackageFragmentRoot.K_SOURCE && entries[position].getEntryKind() == IClasspathEntry.CPE_SOURCE)
								found= true;
						}
						if (!found)
							result= false;
					}
				}
			} catch (CoreException exception) {
				throw new JavaModelException(exception);
			}
		}
		return result;
	}

	/**
	 * Tries to set up the class path for the java project
	 * 
	 * @param project
	 *            the java project
	 * @param root
	 *            the package fragment root to refactor
	 * @param folder
	 *            the temporary source folder
	 * @param monitor
	 *            the progress monitor to use
	 * @throws IllegalStateException
	 *             if the plugin state location does not exist
	 * @throws CoreException
	 *             if an error occurs while setting up the class path
	 */
	private static void setupClassPath(final IJavaProject project, final IPackageFragmentRoot root, final IFolder folder, final IProgressMonitor monitor) throws IllegalStateException, CoreException {
		try {
			monitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, 200);
			final IClasspathEntry entry= root.getRawClasspathEntry();
			final IClasspathEntry[] entries= project.getRawClasspath();
			final List list= new ArrayList();
			list.addAll(Arrays.asList(entries));
			final IFileStore store= EFS.getLocalFileSystem().getStore(JavaPlugin.getDefault().getStateLocation().append(STUB_FOLDER).append(project.getElementName()));
			if (store.fetchInfo(EFS.NONE, new SubProgressMonitor(monitor, 25, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).exists())
				store.delete(EFS.NONE, new SubProgressMonitor(monitor, 25, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			store.mkdir(EFS.NONE, new SubProgressMonitor(monitor, 25, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			folder.createLink(store.toURI(), IResource.NONE, new SubProgressMonitor(monitor, 25, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			addExclusionPatterns(list, folder.getFullPath());
			for (int index= 0; index < entries.length; index++) {
				if (entries[index].equals(entry))
					list.add(index, JavaCore.newSourceEntry(folder.getFullPath()));
			}
			project.setRawClasspath((IClasspathEntry[]) list.toArray(new IClasspathEntry[list.size()]), false, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
		} finally {
			monitor.done();
		}
	}

	/** Is auto build enabled? */
	private boolean fAutoBuild= true;

	/** Has the wizard been cancelled? */
	private boolean fCancelled= false;

	/** The jar import data */
	private final JarImportData fImportData= new JarImportData();

	/** The jar import page, or <code>null</code> */
	private JarImportWizardPage fImportPage= null;

	/** Is the wizard part of an import wizard? */
	private boolean fImportWizard= true;

	/** The java project or <code>null</code> */
	private IJavaProject fJavaProject= null;

	/** Has the wizard new dialog settings? */
	private boolean fNewSettings;

	/**
	 * The packages which already have been processed (element type:
	 * &lt;IPackageFragment&gt;)
	 */
	private final Collection fProcessedFragments= new HashSet();

	/** The temporary source folder, or <code>null</code> */
	private IFolder fSourceFolder= null;

	/**
	 * Creates a new jar import wizard.
	 */
	public JarImportWizard() {
		super(JarImportMessages.JarImportWizard_window_title, JarImportMessages.RefactoringImportPreviewPage_title, JarImportMessages.RefactoringImportPreviewPage_description);
		fImportData.setRefactoringAware(true);
		fImportData.setIncludeDirectoryEntries(true);
		setInput(new RefactoringHistoryProxy());
		final IDialogSettings section= JavaPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS_KEY);
		if (section == null)
			fNewSettings= true;
		else {
			fNewSettings= false;
			setDialogSettings(section);
		}
		setConfiguration(new RefactoringHistoryControlConfiguration(null, false, false) {

			public String getProjectPattern() {
				return JarImportMessages.JarImportWizard_project_pattern;
			}

			public String getWorkspaceCaption() {
				return JarImportMessages.JarImportWizard_workspace_caption;
			}
		});
	}

	/**
	 * Creates a new jar import wizard.
	 * 
	 * @param wizard
	 *            <code>true</code> if the wizard is part of an import wizard,
	 *            <code>false</code> otherwise
	 */
	public JarImportWizard(final boolean wizard) {
		this();
		fImportWizard= wizard;
		setWindowTitle(JarImportMessages.JarImportWizard_replace_title);
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus aboutToPerformHistory(final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			fJavaProject= null;
			fSourceFolder= null;
			fProcessedFragments.clear();
			monitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, 500);
			final IPackageFragmentRoot root= fImportData.getPackageFragmentRoot();
			if (root != null) {
				status.merge(checkPackageFragmentRoots(root, new SubProgressMonitor(monitor, 90, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
				if (!status.hasFatalError()) {
					status.merge(super.aboutToPerformHistory(new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
					if (!status.hasFatalError()) {
						final IJavaProject project= root.getJavaProject();
						if (project != null) {
							final IFolder folder= project.getProject().getFolder(SOURCE_FOLDER + String.valueOf(System.currentTimeMillis()));
							try {
								fAutoBuild= CoreUtility.enableAutoBuild(false);
								if (fImportData.getRefactoringHistory() != null)
									setupClassPath(project, root, folder, new SubProgressMonitor(monitor, 300, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
							} catch (CoreException exception) {
								status.merge(RefactoringStatus.createFatalErrorStatus(exception.getLocalizedMessage()));
								try {
									project.setRawClasspath(project.readRawClasspath(), false, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
								} catch (JavaModelException throwable) {
									JavaPlugin.log(throwable);
								}
							} finally {
								if (!status.hasFatalError()) {
									fJavaProject= project;
									fSourceFolder= folder;
								}
							}
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Hook method which is called before the a refactoring of the history is
	 * executed. The refactoring itself is in an uninitialized state at the time
	 * of the method call. This implementation initializes the refactoring based
	 * on the refactoring arguments stored in the descriptor. All handles
	 * provided in the {@link RefactoringDescriptor#INPUT} or
	 * {@link RefactoringDescriptor#ELEMENT} attributes are converted to match
	 * the project layout of the client. This method may be called from non-UI
	 * threads.
	 * <p>
	 * Subclasses may extend this method to perform any special processing.
	 * </p>
	 * <p>
	 * Returning a status of severity {@link RefactoringStatus#FATAL} will
	 * terminate the execution of the refactorings.
	 * </p>
	 * 
	 * @param refactoring
	 *            the refactoring about to be executed
	 * @param descriptor
	 *            the refactoring descriptor
	 * @return a status describing the outcome of the initialization
	 */
	private RefactoringStatus aboutToPerformRefactoring(final Refactoring refactoring, final RefactoringDescriptor descriptor) {
		Assert.isNotNull(refactoring);
		Assert.isNotNull(descriptor);
		Assert.isNotNull(refactoring);
		Assert.isNotNull(descriptor);
		final RefactoringStatus status= new RefactoringStatus();
		if (refactoring instanceof IInitializableRefactoringComponent) {
			final IInitializableRefactoringComponent component= (IInitializableRefactoringComponent) refactoring;
			final RefactoringArguments arguments= RefactoringCore.getRefactoringInstanceCreator().createArguments(descriptor);
			if (arguments instanceof GenericRefactoringArguments) {
				final GenericRefactoringArguments generic= (GenericRefactoringArguments) arguments;
				String value= generic.getAttribute(RefactoringDescriptor.INPUT);
				if (value != null && !"".equals(value)) //$NON-NLS-1$
					generic.setAttribute(RefactoringDescriptor.INPUT, getTransformedInputValue(value));
				int count= 1;
				String attribute= RefactoringDescriptor.ELEMENT + count;
				while ((value= generic.getAttribute(attribute)) != null) {
					if (!"".equals(value)) //$NON-NLS-1$
						generic.setAttribute(attribute, getTransformedInputValue(value));
					count++;
					attribute= RefactoringDescriptor.ELEMENT + count;
				}
				status.merge(component.initialize(generic));
			} else
				status.addFatalError(MessageFormat.format(JarImportMessages.PerformRefactoringsOperation_init_error, new String[] { descriptor.getDescription()}));
		} else
			status.addFatalError(MessageFormat.format(JarImportMessages.PerformRefactoringsOperation_init_error, new String[] { descriptor.getDescription()}));
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus aboutToPerformRefactoring(final Refactoring refactoring, final RefactoringDescriptor descriptor, final IProgressMonitor monitor) {
		Assert.isNotNull(refactoring);
		Assert.isNotNull(descriptor);
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, 100);
			status.merge(createJarStubs(refactoring, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
			if (!status.hasFatalError())
				status.merge(aboutToPerformRefactoring(refactoring, descriptor));
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void addUserDefinedPages() {
		fImportPage= new JarImportWizardPage(fImportData, fImportWizard);
		addPage(fImportPage);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFinish() {
		return super.canFinish() && fImportData.getPackageFragmentRoot() != null && fImportData.getRefactoringFileLocation() != null;
	}

	/**
	 * Creates the necessary type stubs for the refactoring.
	 * 
	 * @param refactoring
	 *            the refactoring to create the type stubs for
	 * @param monitor
	 *            the progress monitor to use
	 */
	private RefactoringStatus createJarStubs(final Refactoring refactoring, final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, 240);
			final IPackageFragmentRoot root= fImportData.getPackageFragmentRoot();
			if (root != null && fSourceFolder != null) {
				try {
					final SubProgressMonitor subMonitor= new SubProgressMonitor(monitor, 40, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
					final IJavaElement[] elements= root.getChildren();
					final List list= new ArrayList(elements.length);
					try {
						subMonitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, elements.length);
						for (int index= 0; index < elements.length; index++) {
							if (!fProcessedFragments.contains(elements[index]) && !elements[index].getElementName().equals(META_INF_FRAGMENT))
								list.add(elements[index]);
							subMonitor.worked(1);
						}
					} finally {
						subMonitor.done();
					}
					if (!list.isEmpty()) {
						final URI uri= fSourceFolder.getRawLocationURI();
						if (uri != null) {
							final StubCreationOperation operation= new StubCreationOperation(uri, list, true);
							try {
								operation.run(new SubProgressMonitor(monitor, 150, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
							} finally {
								fSourceFolder.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 50, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
							}
						}
					}
				} catch (CoreException exception) {
					status.addFatalError(exception.getLocalizedMessage());
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	public IWizardPage getNextPage(final IWizardPage page) {
		if (page == fImportPage && fImportData.getRefactoringHistory() == null)
			return null;
		return super.getNextPage(page);
	}

	/**
	 * Returns the target path to be used for the updated classpath entry.
	 * 
	 * @param entry
	 *            the classpath entry
	 * @return the target path, or <code>null</code>
	 * @throws CoreException
	 *             if an error occurs
	 */
	private IPath getTargetPath(final IClasspathEntry entry) throws CoreException {
		final URI location= getLocationURI(entry);
		if (location != null) {
			final URI target= getTargetURI(location);
			if (target != null) {
				IPath path= URIUtil.toPath(target);
				if (path != null) {
					final IPath workspace= ResourcesPlugin.getWorkspace().getRoot().getLocation();
					if (workspace.isPrefixOf(path)) {
						path= path.removeFirstSegments(workspace.segmentCount());
						path= path.setDevice(null);
						path= path.makeAbsolute();
					}
				}
				return path;
			}
		}
		return null;
	}

	/**
	 * Returns the target uri taking any renaming of the jar file into account.
	 * 
	 * @param uri
	 *            the location uri
	 * @return the target uri
	 * @throws CoreException
	 *             if an error occurs
	 */
	private URI getTargetURI(final URI uri) throws CoreException {
		Assert.isNotNull(uri);
		final IFileStore parent= EFS.getStore(uri).getParent();
		if (parent != null) {
			final URI location= fImportData.getRefactoringFileLocation();
			if (location != null)
				return parent.getChild(EFS.getStore(location).getName()).toURI();
		}
		return uri;
	}

	/**
	 * Returns the transformed input value of the specified value.
	 * 
	 * @param value
	 *            the value to transform
	 * @return the transformed value, or the original one
	 */
	private String getTransformedInputValue(final String value) {
		if (fSourceFolder != null) {
			final IJavaElement target= JavaCore.create(fSourceFolder);
			if (target instanceof IPackageFragmentRoot) {
				final IPackageFragmentRoot extended= (IPackageFragmentRoot) target;
				final String targetIdentifier= extended.getHandleIdentifier();
				String sourceIdentifier= null;
				final IJavaElement element= JavaCore.create(value);
				if (element != null) {
					final IPackageFragmentRoot root= (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					if (root != null)
						sourceIdentifier= root.getHandleIdentifier();
					else {
						final IJavaProject project= element.getJavaProject();
						if (project != null)
							sourceIdentifier= project.getHandleIdentifier();
					}
				}
				if (sourceIdentifier != null)
					return targetIdentifier + element.getHandleIdentifier().substring(sourceIdentifier.length());
			}
		}
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus historyPerformed(final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask(JarImportMessages.JarImportWizard_cleanup_import, 100);
			final RefactoringStatus status= super.historyPerformed(new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			if (!status.hasFatalError()) {
				try {
					resetClassPath(new SubProgressMonitor(monitor, 90, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				} catch (CoreException exception) {
					status.addError(exception.getLocalizedMessage());
				} finally {
					try {
						CoreUtility.enableAutoBuild(fAutoBuild);
					} catch (CoreException exception) {
						JavaPlugin.log(exception);
					}
				}
			}
			return status;
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void init(final IWorkbench workbench, final IStructuredSelection selection) {
		if (selection.size() == 1) {
			final Object element= selection.getFirstElement();
			if (element instanceof IPackageFragmentRoot) {
				final IPackageFragmentRoot root= (IPackageFragmentRoot) element;
				try {
					final IClasspathEntry entry= root.getRawClasspathEntry();
					if (isValidClassPathEntry(entry))
						fImportData.setPackageFragmentRoot(root);
				} catch (JavaModelException exception) {
					JavaPlugin.log(exception);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean performCancel() {
		fCancelled= true;
		return super.performCancel();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean performFinish() {
		final boolean result= super.performFinish();
		if (fNewSettings) {
			final IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
			IDialogSettings section= settings.getSection(DIALOG_SETTINGS_KEY);
			section= settings.addNewSection(DIALOG_SETTINGS_KEY);
			setDialogSettings(section);
		}
		return result;
	}

	/**
	 * Replaces the old jar file with the new one.
	 * 
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs
	 */
	private void replaceJarFile(final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(JarImportMessages.JarImportWizard_cleanup_import, 250);
			final URI location= fImportData.getRefactoringFileLocation();
			if (location != null) {
				final IPackageFragmentRoot root= fImportData.getPackageFragmentRoot();
				if (root != null) {
					final URI uri= getLocationURI(root.getRawClasspathEntry());
					if (uri != null) {
						final IFileStore store= EFS.getStore(location);
						if (fImportData.isRenameJarFile()) {
							final URI target= getTargetURI(uri);
							store.copy(EFS.getStore(target), EFS.OVERWRITE, new SubProgressMonitor(monitor, 50, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
							if (!uri.equals(target))
								EFS.getStore(uri).delete(EFS.NONE, new SubProgressMonitor(monitor, 50, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
						} else
							store.copy(EFS.getStore(uri), EFS.OVERWRITE, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
						if (fJavaProject != null)
							fJavaProject.getResource().refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 50, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
						return;
					}
				}
			}
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, JarImportMessages.JarImportWizard_error_copying_jar, null));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Resets the class path to the state on disk.
	 * 
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs while resetting the classpath
	 */
	private void resetClassPath(final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(JarImportMessages.JarImportWizard_cleanup_import, 200);
			if (fJavaProject != null) {
				final IClasspathEntry[] entries= fJavaProject.readRawClasspath();
				final boolean rename= fImportData.isRenameJarFile();
				if (rename && !fCancelled) {
					final IPackageFragmentRoot root= fImportData.getPackageFragmentRoot();
					if (root != null) {
						final IClasspathEntry entry= root.getRawClasspathEntry();
						for (int index= 0; index < entries.length; index++) {
							if (entries[index].equals(entry)) {
								final IPath path= getTargetPath(entries[index]);
								if (path != null)
									entries[index]= JavaCore.newLibraryEntry(path, entries[index].getSourceAttachmentPath(), entries[index].getSourceAttachmentRootPath(), entries[index].getAccessRules(), entries[index].getExtraAttributes(), entries[index].isExported());
							}
						}
					}
				}
				final RefactoringHistory history= fImportData.getRefactoringHistory();
				if (!fCancelled) {
					replaceJarFile(new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
					if (history != null)
						RefactoringCore.getUndoManager().flush();
				}
				if (history != null || rename)
					fJavaProject.setRawClasspath(entries, rename, new SubProgressMonitor(monitor, 60, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				fJavaProject= null;
			}
			if (fSourceFolder != null) {
				final URI uri= fSourceFolder.getRawLocationURI();
				final IFileStore store= EFS.getStore(uri);
				if (store.fetchInfo(EFS.NONE, new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).exists())
					store.delete(EFS.NONE, new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				fSourceFolder.delete(true, false, new SubProgressMonitor(monitor, 20, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				fSourceFolder= null;
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean selectPreviewChange(final Change change) {
		if (fSourceFolder != null) {
			final IPath source= fSourceFolder.getFullPath();
			final Object element= change.getModifiedElement();
			if (element instanceof IAdaptable) {
				final IAdaptable adaptable= (IAdaptable) element;
				final IResource resource= (IResource) adaptable.getAdapter(IResource.class);
				if (resource != null && source.isPrefixOf(resource.getFullPath()))
					return false;
			}
		}
		return super.selectPreviewChange(change);
	}

	/**
	 * {@inheritDoc}
	 */
	protected boolean selectStatusEntry(final RefactoringStatusEntry entry) {
		if (fSourceFolder != null) {
			final IPath source= fSourceFolder.getFullPath();
			final RefactoringStatusContext context= entry.getContext();
			if (context instanceof JavaStatusContext) {
				final JavaStatusContext extended= (JavaStatusContext) context;
				final ICompilationUnit unit= extended.getCompilationUnit();
				if (unit != null) {
					final IResource resource= unit.getResource();
					if (resource != null && source.isPrefixOf(resource.getFullPath()))
						return false;
				}
			}
		}
		return super.selectStatusEntry(entry);
	}
}