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

import java.io.File;
import java.io.FileWriter;
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OpenBrowserUtil;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.jarpackager.ConfirmSaveModifiedResourcesDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.ProgressService;

public class JavadocWizard extends Wizard implements IExportWizard {

	private JavadocTreeWizardPage fJTWPage;
	private JavadocSpecificsWizardPage fJSWPage;
	private JavadocStandardWizardPage fJSpWPage;

	private IPath fDestination;

	private boolean fWriteCustom;
	private boolean fOpenInBrowser;

	private final String TREE_PAGE_DESC= "JavadocTreePage"; //$NON-NLS-1$
	private final String SPECIFICS_PAGE_DESC= "JavadocSpecificsPage"; //$NON-NLS-1$
	private final String STANDARD_PAGE_DESC= "JavadocStandardPage"; //$NON-NLS-1$

	private final int YES= 0;
	private final int YES_TO_ALL= 1;
	private final int NO= 2;
	private final int NO_TO_ALL= 3;
	private final String JAVADOC_ANT_INFORMATION_DIALOG= "javadocAntInformationDialog";//$NON-NLS-1$


	private JavadocOptionsManager fStore;
	private IWorkspaceRoot fRoot;

	private IFile fXmlJavadocFile;
	
	private static final String ID_JAVADOC_PROCESS_TYPE= "org.eclipse.jdt.ui.javadocProcess"; //$NON-NLS-1$

	public static void openJavadocWizard(JavadocWizard wizard, Shell shell, IStructuredSelection selection ) {
		wizard.init(PlatformUI.getWorkbench(), selection);
		
		PixelConverter converter= new PixelConverter(shell);
		
		WizardDialog dialog= new WizardDialog(shell, wizard);
		dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(100), converter.convertHeightInCharsToPixels(20));
		dialog.open();
	}
	
	
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
	}
		
	/*
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() {

		IJavaProject[] checkedProjects= fJTWPage.getCheckedProjects();
		updateStore(checkedProjects);
		
		//If the wizard was not launched from an ant file store the setttings 
		if (fXmlJavadocFile == null) {
			fStore.updateDialogSettings(getDialogSettings(), checkedProjects);
		}
		

		// Wizard will not run with unsaved files.
		if (!checkPreconditions(fStore.getSourceElements())) {
			return false;
		}

		fDestination= new Path(fStore.getDestination());
		fDestination.toFile().mkdirs();

		fOpenInBrowser= fStore.doOpenInBrowser();

		//Ask if you wish to set the javadoc location for the projects (all) to 
		//the location of the newly generated javadoc 
		if (fStore.isFromStandard()) {
			try {

				URL newURL= fDestination.toFile().toURL();
				List projs= new ArrayList();
				//get javadoc locations for all projects
				for (int i= 0; i < checkedProjects.length; i++) {
					IJavaProject curr= checkedProjects[i];
					URL currURL= JavaUI.getProjectJavadocLocation(curr);
					if (!newURL.equals(currURL)) { // currURL can be null
						//if not all projects have the same javadoc location ask if you want to change
						//them to have the same javadoc location
						projs.add(curr);
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
			OptionalMessageDialog.open(JAVADOC_ANT_INFORMATION_DIALOG, getShell(), JavadocExportMessages.getString("JavadocWizard.antInformationDialog.title"), null, JavadocExportMessages.getString("JavadocWizard.antInformationDialog.message"), MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				fStore.createXML(checkedProjects);
			} catch (CoreException e) {
				ExceptionHandler.handle(e, getShell(),JavadocExportMessages.getString("JavadocWizard.error.writeANT.title"), JavadocExportMessages.getString("JavadocWizard.error.writeANT.message")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (!executeJavadocGeneration())
			return false;

		return true;
	}
	
	private void updateStore(IJavaProject[] checkedProjects) {
		//writes the new settings to store
		fJTWPage.updateStore(checkedProjects);
		if (!fJTWPage.getCustom())
			fJSpWPage.updateStore();
		fJSWPage.updateStore();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performCancel()
	 */
	public boolean performCancel() {
		
		IJavaProject[] checkedProjects= fJTWPage.getCheckedProjects();
		updateStore(checkedProjects);
		
		//If the wizard was not launched from an ant file store the setttings 
		if (fXmlJavadocFile == null) {
			fStore.updateDialogSettings(getDialogSettings(), checkedProjects);
		}
		return super.performCancel();
	}

	private void setAllJavadocLocations(IJavaProject[] projects, URL newURL) {
		Shell shell= getShell();
		Image image= shell == null ? null : shell.getDisplay().getSystemImage(SWT.ICON_QUESTION);
		String[] buttonlabels= new String[] { IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.NO_TO_ALL_LABEL };

		for (int j= 0; j < projects.length; j++) {
			IJavaProject iJavaProject= projects[j];
			String message= JavadocExportMessages.getFormattedString("JavadocWizard.updatejavadoclocation.message", new String[] { iJavaProject.getElementName(), fDestination.toOSString()}); //$NON-NLS-1$
			MessageDialog dialog= new MessageDialog(shell, JavadocExportMessages.getString("JavadocWizard.updatejavadocdialog.label"), image, message, 4, buttonlabels, 1);//$NON-NLS-1$

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

	private boolean executeJavadocGeneration() {
		Process process= null;
		try {
			ArrayList vmArgs= new ArrayList();
			ArrayList progArgs= new ArrayList();
			
			fStore.getArgumentArray(vmArgs, progArgs);
			
			File file= File.createTempFile("javadoc-arguments", ".tmp");  //$NON-NLS-1$//$NON-NLS-2$
			vmArgs.add('@' + file.getAbsolutePath());

			FileWriter writer= new FileWriter(file);
			try {
				for (int i= 0; i < progArgs.size(); i++) {
					String curr= (String) progArgs.get(i);
					curr= checkForSpaces(curr);
					
					writer.write(curr);
					writer.write(' ');
				}
			} finally {
				writer.close();
			}
			
			String[] args= (String[]) vmArgs.toArray(new String[vmArgs.size()]);
			process= Runtime.getRuntime().exec(args);
			if (process != null) {
				// construct a formatted command line for the process properties
				StringBuffer buf= new StringBuffer();
				for (int i= 0; i < args.length; i++) {
					buf.append(args[i]);
					buf.append(' ');
				}

				IDebugEventSetListener listener= new JavadocDebugEventListener(getShell().getDisplay(), file);
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
					iprocess.setAttribute(IProcess.ATTR_PROCESS_TYPE, ID_JAVADOC_PROCESS_TYPE);

					DebugPlugin.getDefault().getLaunchManager().addLaunch(newLaunch);

				} catch (CoreException e) {
					String title= JavadocExportMessages.getString("JavadocWizard.error.title"); //$NON-NLS-1$
					String message= JavadocExportMessages.getString("JavadocWizard.launch.error.message"); //$NON-NLS-1$
					ExceptionHandler.handle(e, getShell(), title, message);
				}

				return true;

			}
		} catch (IOException e) {
			String title= JavadocExportMessages.getString("JavadocWizard.error.title"); //$NON-NLS-1$
			String message= JavadocExportMessages.getString("JavadocWizard.exec.error.message"); //$NON-NLS-1$

			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, e.getMessage(), e);
			ExceptionHandler.handle(new CoreException(status), getShell(), title, message);
			return false;
		}
		return false;

	}

	private String checkForSpaces(String curr) {
		if (curr.indexOf(' ') == -1) {
			return curr;
		}	
		StringBuffer buf= new StringBuffer();
		buf.append('\'');
		for (int i= 0; i < curr.length(); i++) {
			char ch= curr.charAt(i);
			if (ch == '\\' || ch == '\'') {
				buf.append('\\');				
			}
			buf.append(ch);
		}
		buf.append('\'');
		return buf.toString();
	}

	/**
	 * Creates a list of all CompilationUnits and extracts from that list a list of dirty
	 * files. The user is then asked to confirm if those resources should be saved or
	 * not.
	 * 
	 * @param elements
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
	 * @param resources
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
	 * @param dirtyFiles
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
	 * @param dirtyFiles
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
	 * @param dirtyFiles
	 * @return true if successful.
	 * @throws CoreException
	 * @throws InvocationTargetException
	 */
	private boolean saveModifiedResources(final IFile[] dirtyFiles) throws CoreException, InvocationTargetException {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description= workspace.getDescription();
		boolean autoBuild= description.isAutoBuilding();
		description.setAutoBuilding(false);
		try {
			workspace.setDescription(description);
			// This save operation can not be cancelled.
			try {
				ProgressService.runSuspended(false, false, createSaveModifiedResourcesRunnable(dirtyFiles), workspace.getRoot());
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
		
		fJTWPage= new JavadocTreeWizardPage(TREE_PAGE_DESC, fStore);
		fJSWPage= new JavadocSpecificsWizardPage(SPECIFICS_PAGE_DESC, fJTWPage, fStore);
		fJSpWPage= new JavadocStandardWizardPage(STANDARD_PAGE_DESC, fJTWPage, fStore);

		super.addPage(fJTWPage);
		super.addPage(fJSpWPage);
		super.addPage(fJSWPage);

		fJTWPage.init();
		fJSpWPage.init();
		fJSWPage.init();

	}

	public void init(IWorkbench workbench, IStructuredSelection structuredSelection) {
		List selected= structuredSelection.toList();
		if (selected.isEmpty()) {
			IJavaElement element= EditorUtility.getActiveEditorJavaInput();
			selected= new ArrayList();
			selected.add(element);
		}
		fStore= new JavadocOptionsManager(fXmlJavadocFile, getDialogSettings(), selected);
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
		private File fFile;

		public JavadocDebugEventListener(Display display, File file) {
			fDisplay= display;
			fFile= file;
		}
		
		public void handleDebugEvents(DebugEvent[] events) {
			for (int i= 0; i < events.length; i++) {
				if (events[i].getKind() == DebugEvent.TERMINATE) {
					try {
						if (!fWriteCustom) {
							fFile.delete();
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
	
}
