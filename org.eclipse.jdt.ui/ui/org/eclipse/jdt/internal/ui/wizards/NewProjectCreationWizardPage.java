/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

/**
 * As addition to the JavaCapabilityConfigurationPage, the wizard checks if an existing external
 * location was specified and offers to do an early project creation so that classpath can be detected.
 */
public class NewProjectCreationWizardPage extends JavaCapabilityConfigurationPage {

	private WizardNewProjectCreationPage fMainPage;
	private IPath fCurrProjectLocation;
	private boolean fProjectCreated;

	/**
	 * Constructor for ProjectWizardPage.
	 */
	public NewProjectCreationWizardPage(WizardNewProjectCreationPage mainPage) {
		super();
		fMainPage= mainPage;
		fCurrProjectLocation= fMainPage.getLocationPath();
		fProjectCreated= false;
	}
	
	private boolean canDetectExistingClassPath(IPath projLocation) {
		return projLocation.toFile().exists() && !Platform.getLocation().equals(projLocation);
	}
	
	private void update() {
		IPath projLocation= fMainPage.getLocationPath();
		if (!projLocation.equals(fCurrProjectLocation) && canDetectExistingClassPath(projLocation)) {
			String title= NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationDialog.title"); //$NON-NLS-1$
			String description= NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationDialog.description"); //$NON-NLS-1$
			if (MessageDialog.openQuestion(getShell(), title, description)) {
				createAndDetect();
			}
		}
					
		fCurrProjectLocation= projLocation;
				
		IJavaProject prevProject= getJavaProject();
		IProject currProject= fMainPage.getProjectHandle();
		if ((prevProject == null) || !currProject.equals(prevProject.getProject())) {
			init(JavaCore.create(currProject), null, null, false);
		}		
	}

	private void createAndDetect() {
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				if (monitor == null)
					monitor= new NullProgressMonitor();		
		
				monitor.beginTask(NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationOperation.desc"), 3);				 //$NON-NLS-1$
				try {
					BuildPathsBlock.createProject(fMainPage.getProjectHandle(), fMainPage.getLocationPath(), new SubProgressMonitor(monitor, 1));
					fProjectCreated= true;					
					initFromExistingStructures(new SubProgressMonitor(monitor, 2));						
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};

		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			String title= NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationOperation.error.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("NewProjectCreationWizardPage.EarlyCreationOperation.error.desc");			 //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
		} catch  (InterruptedException e) {
			// cancel pressed
		}
	}
		
	/* (non-Javadoc)
	 * @see IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			update();
		}
		super.setVisible(visible);
	}
	
	/* (non-Javadoc)
	 * @see IWizardPage#getPreviousPage()
	 */
	public IWizardPage getPreviousPage() {
		if (fProjectCreated) {
			return null;
		}
		return super.getPreviousPage();
	}
		
	public void createProject(IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {		
			monitor.beginTask(NewWizardMessages.getString("NewProjectCreationWizardPage.NormalCreationOperation.desc"), 4); //$NON-NLS-1$
			BuildPathsBlock.createProject(fMainPage.getProjectHandle(), fMainPage.getLocationPath(), new SubProgressMonitor(monitor, 1));
			if (getJavaProject() == null) {
				initFromExistingStructures(new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}
			configureJavaProject(new SubProgressMonitor(monitor, 2));
		} finally {
			monitor.done();
		}
	}
	
	private void initFromExistingStructures(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(NewWizardMessages.getString("NewProjectCreationWizardPage.DetectingClasspathOperation.desc"), 2); //$NON-NLS-1$
		try {
			IProject project= fMainPage.getProjectHandle();
			
			if (project.getFile(".classpath").exists()) { //$NON-NLS-1$
				init(JavaCore.create(project), null, null, false);
				monitor.worked(2);
			} else{
				final HashSet sourceFolders= new HashSet();
				IResourceVisitor visitor= new IResourceVisitor() {
					public boolean visit(IResource resource) throws CoreException {
						return doVisit(resource, sourceFolders);
					}
				};
				project.accept(visitor);
				monitor.worked(1);
								
				IClasspathEntry[] entries= null;
				IPath outputLocation= null;
				
				if (!sourceFolders.isEmpty()) {
					int nSourceFolders= sourceFolders.size();
					IClasspathEntry[] jreEntries= PreferenceConstants.getDefaultJRELibrary();
					entries= new IClasspathEntry[nSourceFolders + jreEntries.length];
					Iterator iter = sourceFolders.iterator();
					for (int i = 0; i < nSourceFolders; i++) {
						entries[i]= JavaCore.newSourceEntry((IPath) iter.next());
					}
					System.arraycopy(jreEntries, 0, entries, nSourceFolders, jreEntries.length);
					
					IPath projPath= project.getFullPath();
					if (nSourceFolders == 1 && entries[0].getPath().equals(projPath)) {
						outputLocation= projPath;
					} else {
						outputLocation= projPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME));
					} 				
					if (!JavaConventions.validateClasspath(JavaCore.create(project), entries, outputLocation).isOK()) {
						outputLocation= null;
						entries= null;
					}
				}
				init(JavaCore.create(project), outputLocation, entries, false);
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
		
	}
	
	private boolean doVisit(IResource resource, HashSet sourceFolders) throws JavaModelException {
		if (!sourceFolders.isEmpty()) {
			IResource curr= resource;
			while (curr.getType() != IResource.ROOT) {
				if (sourceFolders.contains(curr.getFullPath())) {
					return false;
				}
				curr= curr.getParent();
			}
		}
		if (resource.getType() == IResource.FILE) {
			if ("java".equals(resource.getFileExtension())) { //$NON-NLS-1$
				ICompilationUnit cu= JavaCore.createCompilationUnitFrom((IFile) resource);
				if (cu != null) {
					IPath packPath= resource.getParent().getFullPath();
					IPackageDeclaration[] decls= cu.getPackageDeclarations();
					if (decls.length == 0) {
						sourceFolders.add(packPath);
					} else {
						IPath relpath= new Path(decls[0].getElementName().replace('.', '/'));
						int remainingSegments= packPath.segmentCount() - relpath.segmentCount();
						if (remainingSegments >= 0) {
							IPath prefix= packPath.uptoSegment(remainingSegments);
							IPath common= packPath.removeFirstSegments(remainingSegments);
							if (common.equals(relpath)) {
								sourceFolders.add(prefix);
							}
						}
					}
				}
			}
		}
		return true;
	}

	
	/**
	 * Called from the wizard on cancel.
	 */
	public void performCancel() {
		if (fProjectCreated) {
			try {
				fMainPage.getProjectHandle().delete(false, false, null);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}
}
