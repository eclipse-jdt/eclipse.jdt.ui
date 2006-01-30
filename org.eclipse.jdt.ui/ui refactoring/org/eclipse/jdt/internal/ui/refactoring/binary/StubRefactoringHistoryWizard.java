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
package org.eclipse.jdt.internal.ui.refactoring.binary;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.ui.refactoring.history.RefactoringHistoryWizard;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.binary.StubCreationOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.jarimport.JarImportMessages;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

/**
 * Partial implementation of a refactoring history wizard which creates stubs
 * from a binary package fragment root while refactoring.
 * 
 * @since 3.2
 */
public abstract class StubRefactoringHistoryWizard extends RefactoringHistoryWizard {

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
	 * shared with multiple java projects in the workspace.
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
														status.addFatalError(MessageFormat.format(JarImportMessages.JarImportWizard_error_shared_jar, new String[] { current.getJavaProject().getElementName() }));
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
					} catch (CoreException exception) {
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
	 * Configures the classpath of the project before refactoring.
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
	 *             if an error occurs while configuring the class path
	 */
	private static void configureClasspath(final IJavaProject project, final IPackageFragmentRoot root, final IFolder folder, final IProgressMonitor monitor) throws IllegalStateException, CoreException {
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

	/**
	 * Returns the location URI of the classpath entry
	 * 
	 * @param entry
	 *            the classpath entry
	 * @return the location URI
	 */
	protected static URI getLocationURI(final IClasspathEntry entry) {
		final IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		final IPath path= entry.getPath();
		URI location= null;
		if (root.exists(path)) {
			location= root.getFile(path).getRawLocationURI();
		} else
			location= URIUtil.toURI(path);
		return location;
	}

	/** Is auto build enabled? */
	private boolean fAutoBuild= true;

	/** Has the wizard been cancelled? */
	protected boolean fCancelled= false;

	/** The java project or <code>null</code> */
	protected IJavaProject fJavaProject= null;

	/**
	 * The packages which already have been processed (element type:
	 * &lt;IPackageFragment&gt;)
	 */
	private final Collection fProcessedFragments= new HashSet();

	/** The temporary source folder, or <code>null</code> */
	protected IFolder fSourceFolder= null;

	/**
	 * Creates a new stub refactoring history wizard.
	 * 
	 * @param caption
	 *            the wizard caption
	 * @param title
	 *            the wizard title
	 * @param description
	 *            the wizard description
	 */
	protected StubRefactoringHistoryWizard(final String caption, final String title, final String description) {
		super(caption, title, description);
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus aboutToPerformHistory(final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			fJavaProject= null;
			fSourceFolder= null;
			fProcessedFragments.clear();
			monitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, 500);
			final IPackageFragmentRoot root= getPackageFragmentRoot();
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
								if (getRefactoringHistory() != null)
									configureClasspath(project, root, folder, new SubProgressMonitor(monitor, 300, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
							} catch (CoreException exception) {
								status.merge(RefactoringStatus.createFatalErrorStatus(exception.getLocalizedMessage()));
								try {
									project.setRawClasspath(project.readRawClasspath(), false, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
								} catch (CoreException throwable) {
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
	protected abstract RefactoringStatus aboutToPerformRefactoring(Refactoring refactoring, RefactoringDescriptor descriptor);

	/**
	 * {@inheritDoc}
	 */
	protected final RefactoringStatus aboutToPerformRefactoring(final Refactoring refactoring, final RefactoringDescriptor descriptor, final IProgressMonitor monitor) {
		monitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, 120);
		final RefactoringStatus status= super.aboutToPerformRefactoring(refactoring, descriptor, new SubProgressMonitor(monitor, 20, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
		try {
			if (!status.hasFatalError()) {
				status.merge(createTypeStubs(refactoring, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)));
				if (!status.hasFatalError())
					status.merge(aboutToPerformRefactoring(refactoring, descriptor));
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Creates the necessary type stubs for the refactoring.
	 * 
	 * @param refactoring
	 *            the refactoring to create the type stubs for
	 * @param monitor
	 *            the progress monitor to use
	 */
	private RefactoringStatus createTypeStubs(final Refactoring refactoring, final IProgressMonitor monitor) {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(JarImportMessages.JarImportWizard_prepare_import, 240);
			final IPackageFragmentRoot root= getPackageFragmentRoot();
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
	 * Deconfigures the classpath after all refactoring have been performed.
	 * 
	 * @param entries
	 *            the classpath entries to reset the project to
	 * @param monitor
	 *            the progress monitor to use
	 * @return <code>true</code> if the classpath has been changed,
	 *         <code>false</code> otherwise
	 * @throws CoreException
	 *             if an error occurs while deconfiguring the classpath
	 */
	protected boolean deconfigureClasspath(IClasspathEntry[] entries, IProgressMonitor monitor) throws CoreException {
		return false;
	}

	/**
	 * Deconfigures the classpath of the project after refactoring.
	 * 
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs while deconfiguring the classpath
	 */
	private void deconfigureClasspath(final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(JarImportMessages.JarImportWizard_cleanup_import, 200);
			if (fJavaProject != null) {
				final IClasspathEntry[] entries= fJavaProject.readRawClasspath();
				final boolean changed= deconfigureClasspath(entries, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				final RefactoringHistory history= getRefactoringHistory();
				if (history != null)
					RefactoringCore.getUndoManager().flush();
				if (history != null || changed)
					fJavaProject.setRawClasspath(entries, changed, new SubProgressMonitor(monitor, 60, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				fJavaProject= null;
			}
			if (fSourceFolder != null) {
				final IFileStore store= EFS.getStore(fSourceFolder.getRawLocationURI());
				if (store.fetchInfo(EFS.NONE, new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).exists())
					store.delete(EFS.NONE, new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				fSourceFolder.delete(true, false, new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				fSourceFolder.clearHistory(new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
				fSourceFolder= null;
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Returns the package fragment root to stub.
	 * 
	 * @return the package fragment root to stub, or <code>null</code>
	 */
	protected abstract IPackageFragmentRoot getPackageFragmentRoot();

	/**
	 * Returns the refactoring history to perform.
	 * 
	 * @return the refactoring history to perform, or <code>null</code>
	 */
	protected abstract RefactoringHistory getRefactoringHistory();

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus historyPerformed(final IProgressMonitor monitor) {
		try {
			monitor.beginTask(JarImportMessages.JarImportWizard_cleanup_import, 100);
			final RefactoringStatus status= super.historyPerformed(new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			if (!status.hasFatalError()) {
				try {
					deconfigureClasspath(new SubProgressMonitor(monitor, 90, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
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
	public boolean performCancel() {
		fCancelled= true;
		return super.performCancel();
	}

	/**
	 * {@inheritDoc}
	 */
	protected RefactoringStatus refactoringPerformed(final Refactoring refactoring, final IProgressMonitor monitor) {
		try {
			monitor.beginTask("", 120); //$NON-NLS-1$
			final RefactoringStatus status= super.refactoringPerformed(refactoring, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			if (!status.hasFatalError()) {
				if (fSourceFolder != null) {
					try {
						fSourceFolder.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 100, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
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
