/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> bug 38692
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OpenBrowserUtil;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.jarpackager.ConfirmSaveModifiedResourcesDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class JavadocWizard extends Wizard implements IExportWizard {

	private JavadocTreeWizardPage fJTWPage;
	private JavadocSpecificsWizardPage fJSWPage;
	private JavadocStandardWizardPage fJSpWPage;

	private IPath fDestination;

	private boolean fWriteCustom;
	private boolean fOpenInBrowser;

	protected final String CommandDesc= "JavadocCommandPage"; //$NON-NLS-1$
	protected final String TreePageDesc= "JavadocTreePage"; //$NON-NLS-1$
	protected final String SpecificsPageDesc= "JavadocSpecificsPage"; //$NON-NLS-1$
	protected final String StandardPageDesc= "JavadocStandardPage"; //$NON-NLS-1$

	private final int YES= 0;
	private final int YES_TO_ALL= 1;
	private final int NO= 2;
	private final int NO_TO_ALL= 3;
	private final String JAVADOC_ANT_INFORMATION_DIALOG= "javadocAntInformationDialog";//$NON-NLS-1$


	private JavadocOptionsManager fStore;
	private IWorkspaceRoot fRoot;
	private Set fSelectedProjects;

	private IFile fXmlJavadocFile;

	//private ILaunchConfiguration fConfig;

	public JavadocWizard() {
		this(null);
	}

	public JavadocWizard(IFile xmlJavadocFile) {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_EXPORT_JAVADOC);
		setWindowTitle(JavadocExportMessages.getString("JavadocWizard.javadocwizard.title")); //$NON-NLS-1$

		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());

		fRoot= ResourcesPlugin.getWorkspace().getRoot();
		fXmlJavadocFile= xmlJavadocFile;

		fWriteCustom= false;

		fSelectedProjects= null;
	}

	/*
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() {

		IJavaProject[] projects= (IJavaProject[]) fSelectedProjects.toArray(new IJavaProject[fSelectedProjects.size()]);

		//writes the new settings to store
		fJTWPage.finish();
		if (!fJTWPage.getCustom())
			fJSpWPage.finish();
		fJSWPage.finish();

		// Wizard will not run with unsaved files.
		if (!checkPreconditions(fStore.getSourceElements())) {
			return false;
		}

		fDestination= new Path(fStore.getDestination());
		fDestination.toFile().mkdirs();

		this.fOpenInBrowser= fStore.doOpenInBrowser();

		//Ask if you wish to set the javadoc location for the projects (all) to 
		//the location of the newly generated javadoc 
		if (fStore.fromStandard()) {
			try {

				URL newURL= fDestination.toFile().toURL();
				List projs= new ArrayList();
				//get javadoc locations for all projects
				for (int i= 0; i < projects.length; i++) {
					IJavaProject iJavaProject= projects[i];
					URL currURL= JavaUI.getProjectJavadocLocation(iJavaProject);
					if (!newURL.equals(currURL)) { // currURL can be null
						//if not all projects have the same javadoc location ask if you want to change
						//them to have the same javadoc location
						projs.add(iJavaProject);
					}
				}
				if (!projs.isEmpty()) {
					setAllJavadocLocations((IJavaProject[]) projs.toArray(new IJavaProject[projs.size()]), newURL);
				}
			} catch (MalformedURLException e) {
				JavaPlugin.log(e);
			}
		}

		if (fJSWPage.generateAnt()) {
			//@Improve: make a better message
			OptionalMessageDialog.open(JAVADOC_ANT_INFORMATION_DIALOG, getShell(), JavadocExportMessages.getString("JavadocWizard.antInformationDialog.title"), Window.getDefaultImage(), JavadocExportMessages.getString("JavadocWizard.antInformationDialog.message"), MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				fStore.createXML();
			} catch (CoreException e) {
				ExceptionHandler.handle(e, getShell(),JavadocExportMessages.getString("JavadocWizard.error.writeANT.title"), JavadocExportMessages.getString("JavadocWizard.error.writeANT.message")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		//If the wizard was not launched from an ant file store the setttings 
		if (fXmlJavadocFile == null) {
			getDialogSettings().addSection(fStore.createDialogSettings());
		}

		if (!executeJavadocGeneration(fStore.createArgumentArray()))
			return false;

		return true;
	}

	public void setAllJavadocLocations(IJavaProject[] projects, URL newURL) {
		for (int j= 0; j < projects.length; j++) {
			IJavaProject iJavaProject= projects[j];
			String message= JavadocExportMessages.getFormattedString("JavadocWizard.updatejavadoclocation.message", new String[] { iJavaProject.getElementName(), fDestination.toOSString()}); //$NON-NLS-1$
			String[] buttonlabels= new String[] { IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.NO_TO_ALL_LABEL };
			MessageDialog dialog= new MessageDialog(getShell(), JavadocExportMessages.getString("JavadocWizard.updatejavadocdialog.label"), Dialog.getImage(Dialog.DLG_IMG_QUESTION), message, 4, buttonlabels, 1);//$NON-NLS-1$

			switch (dialog.open()) {
				case YES :
					JavaUI.setProjectJavadocLocation(iJavaProject, newURL);
					break;
				case YES_TO_ALL :
					for (int i= j; i < projects.length; i++) {
						iJavaProject= projects[i];
						JavaUI.setProjectJavadocLocation(iJavaProject, newURL);
						j++;
					}
					break;
				case NO_TO_ALL :
					j= projects.length;
					break;
				case NO :
				default :
					break;
			}
		}
	}

	private boolean executeJavadocGeneration(String[] args) {
		Process process= null;
		try {
			process= Runtime.getRuntime().exec(args);
			if (process != null) {
				// contruct a formatted command line for the process properties
				StringBuffer buf= new StringBuffer();
				for (int i= 0; i < args.length; i++) {
					buf.append(args[i]);
					buf.append(' ');
				}

				IDebugEventSetListener listener= new JavadocDebugEventListener(getShell().getDisplay());
				DebugPlugin.getDefault().addDebugEventListener(listener);

				ILaunchConfigurationWorkingCopy wc= null;
				try {
					ILaunchConfigurationType lcType= DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
					String name= JavadocExportMessages.getString("JavadocWizard.launchconfig.name"); //$NON-NLS-1$
					wc= lcType.newInstance(null, name);
					wc.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);

					ILaunch newLaunch= new Launch(wc, ILaunchManager.RUN_MODE, null);
					IProcess iprocess= DebugPlugin.newProcess(newLaunch, process, JavadocExportMessages.getString("JavadocWizard.javadocprocess.label")); //$NON-NLS-1$
					iprocess.setAttribute(IProcess.ATTR_CMDLINE, buf.toString());

					DebugPlugin.getDefault().getLaunchManager().addLaunch(newLaunch);

				} catch (CoreException e) {
					JavaPlugin.log(e);
				}

				return true;

			}
		} catch (IOException e) {
			JavaPlugin.log(e);
			return false;
		}
		return false;

	}

	/**
	 * Creates a list of all CompilationUnits and extracts from that list a list of dirty
	 * files. The user is then asked to confirm if those resources should be saved or
	 * not.
	 * 
	 * @return <code>true</code> if all preconditions are satisfied otherwise false
	 */
	private boolean checkPreconditions(IJavaElement[] elements) {

		ArrayList resources= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			if (elements[i] instanceof ICompilationUnit) {
				resources.add(elements[i].getResource());
			}
		}

		//message could be null
		IFile[] unSavedFiles= getUnsavedFiles(resources);
		return saveModifiedResourcesIfUserConfirms(unSavedFiles);
	}

	/**
	 * Returns the files which are not saved and which are
	 * part of the files being exported.
	 * 
	 * @return an array of unsaved files
	 */
	private IFile[] getUnsavedFiles(List resources) {
		IEditorPart[] dirtyEditors= JavaPlugin.getDirtyEditors();
		Set unsavedFiles= new HashSet(dirtyEditors.length);
		if (dirtyEditors.length > 0) {
			for (int i= 0; i < dirtyEditors.length; i++) {
				if (dirtyEditors[i].getEditorInput() instanceof IFileEditorInput) {
					IFile dirtyFile= ((IFileEditorInput) dirtyEditors[i].getEditorInput()).getFile();
					if (resources.contains(dirtyFile)) {
						unsavedFiles.add(dirtyFile);
					}
				}
			}
		}
		return (IFile[]) unsavedFiles.toArray(new IFile[unsavedFiles.size()]);
	}

	/**
	 * Asks to confirm to save the modified resources
	 * and save them if OK is pressed. Must be run in the display thread.
	 * 
	 * @return true if user pressed OK and save was successful.
	 */
	private boolean saveModifiedResourcesIfUserConfirms(IFile[] dirtyFiles) {
		if (confirmSaveModifiedResources(dirtyFiles)) {
			try {
				if (saveModifiedResources(dirtyFiles))
					return true;
			} catch (CoreException e) {
				ExceptionHandler.handle(e, getShell(), JavadocExportMessages.getString("JavadocWizard.saveresourcedialogCE.title"), JavadocExportMessages.getString("JavadocWizard.saveresourcedialogCE.message")); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), JavadocExportMessages.getString("JavadocWizard.saveresourcedialogITE.title"), JavadocExportMessages.getString("JavadocWizard.saveresourcedialogITE.message")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return false;
	}

	/**
	 * Asks the user to confirm to save the modified resources.
	 * 
	 * @return true if user pressed OK.
	 */
	private boolean confirmSaveModifiedResources(IFile[] dirtyFiles) {
		if (dirtyFiles == null || dirtyFiles.length == 0)
			return true;

		// Get display for further UI operations
		Display display= getShell().getDisplay();
		if (display == null || display.isDisposed())
			return false;

		// Ask user to confirm saving of all files
		final ConfirmSaveModifiedResourcesDialog dlg= new ConfirmSaveModifiedResourcesDialog(getShell(), dirtyFiles);
		final int[] intResult= new int[1];
		Runnable runnable= new Runnable() {
			public void run() {
				intResult[0]= dlg.open();
			}
		};
		display.syncExec(runnable);

		return intResult[0] == IDialogConstants.OK_ID;
	}

	/**
	 * Save all of the editors in the workbench.  Must be run in the display thread.
	 * 
	 * @return true if successful.
	 */
	private boolean saveModifiedResources(final IFile[] dirtyFiles) throws CoreException, InvocationTargetException {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description= workspace.getDescription();
		boolean autoBuild= description.isAutoBuilding();
		description.setAutoBuilding(false);
		try {
			workspace.setDescription(description);
			// This save operation can not be canceled.
			try {
				new ProgressMonitorDialog(getShell()).run(false, false, createSaveModifiedResourcesRunnable(dirtyFiles));
			} finally {
				description.setAutoBuilding(autoBuild);
				workspace.setDescription(description);
			}
		} catch (InterruptedException ex) {
			return false;
		}
		return true;
	}

	private IRunnableWithProgress createSaveModifiedResourcesRunnable(final IFile[] dirtyFiles) {
		return new IRunnableWithProgress() {
			public void run(final IProgressMonitor pm) {
				IEditorPart[] editorsToSave= JavaPlugin.getDirtyEditors();
				String name= JavadocExportMessages.getString("JavadocWizard.savetask.name"); //$NON-NLS-1$
				pm.beginTask(name, editorsToSave.length);
				try {
					List dirtyFilesList= Arrays.asList(dirtyFiles);
					for (int i= 0; i < editorsToSave.length; i++) {
						if (editorsToSave[i].getEditorInput() instanceof IFileEditorInput) {
							IFile dirtyFile= ((IFileEditorInput) editorsToSave[i].getEditorInput()).getFile();
							if (dirtyFilesList.contains((dirtyFile)))
								editorsToSave[i].doSave(new SubProgressMonitor(pm, 1));
						}
						pm.worked(1);
					}
				} finally {
					pm.done();
				}
			}
		};
	}

	/*
	 * @see IWizard#addPages()
	 */
	public void addPages() {
		
		fJTWPage= new JavadocTreeWizardPage(TreePageDesc, fStore);
		fJSWPage= new JavadocSpecificsWizardPage(SpecificsPageDesc, fStore);
		fJSpWPage= new JavadocStandardWizardPage(StandardPageDesc, fStore);

		super.addPage(fJTWPage);
		super.addPage(fJSpWPage);
		super.addPage(fJSWPage);

		fJTWPage.init();
		fJSpWPage.init();
		fJSWPage.init();

	}

	public void init(IWorkbench workbench, IStructuredSelection structuredSelection) {
		IDialogSettings settings= getDialogSettings().getSection("javadoc"); //$NON-NLS-1$
		fStore= new JavadocOptionsManager(fXmlJavadocFile, settings, structuredSelection);
		fSelectedProjects= new HashSet(fStore.getJavaProjects());
	}

	private void refresh(IPath path) {
		if (fRoot.findContainersForLocation(path).length > 0) {
			try {
				fRoot.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}

	private void spawnInBrowser(Display display) {
		if (fOpenInBrowser) {
			try {
				IPath indexFile= fDestination.append("index.html"); //$NON-NLS-1$
				URL url= indexFile.toFile().toURL();
				OpenBrowserUtil.open(url, display, getWindowTitle());
			} catch (MalformedURLException e) {
				JavaPlugin.log(e);
			}
		}
	}

	private class JavadocDebugEventListener implements IDebugEventSetListener {
		private Display fDisplay;

		public JavadocDebugEventListener(Display display) {
			fDisplay= display;
		}
		
		public void handleDebugEvents(DebugEvent[] events) {
			for (int i= 0; i < events.length; i++) {
				if (events[i].getKind() == DebugEvent.TERMINATE) {
					try {
						if (!fWriteCustom) {
							refresh(fDestination); //If destination of javadoc is in workspace then refresh workspace
							spawnInBrowser(fDisplay);
						}
					} finally {
						DebugPlugin.getDefault().removeDebugEventListener(this);
					}
					return;
				}
			}
		}
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (page instanceof JavadocTreeWizardPage) {
			if (!fJTWPage.getCustom()) {
				return fJSpWPage;
			}
			return fJSWPage;
		} else if (page instanceof JavadocSpecificsWizardPage) {
			return null;
		} else if (page instanceof JavadocStandardWizardPage)
			return fJSWPage;
		else
			return null;
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page instanceof JavadocSpecificsWizardPage) {
			if (!fJTWPage.getCustom()) {
				return fJSpWPage;
			}
			return fJSWPage;
		} else if (page instanceof JavadocTreeWizardPage) {
			return null;
		} else if (page instanceof JavadocStandardWizardPage)
			return fJTWPage;
		else
			return null;
	}

	protected void setSelectedProjects(Set projects) {
		fSelectedProjects= projects;
	}

	protected Set getSelectedProjects() {
		return fSelectedProjects;
	}

	protected void addSelectedProject(IJavaProject project) {
		fSelectedProjects.add(project);
	}

	protected void removeSelectedProject(IJavaProject project) {
		if (fSelectedProjects.contains(project))
			fSelectedProjects.remove(project);
	}
	
	void removeAllProjects() {
		fSelectedProjects.clear();	
	}
	
}
