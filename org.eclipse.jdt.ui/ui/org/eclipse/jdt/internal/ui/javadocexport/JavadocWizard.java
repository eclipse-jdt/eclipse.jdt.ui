/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
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

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OpenBrowserUtil;
import org.eclipse.jdt.internal.ui.jarpackager.ConfirmSaveModifiedResourcesDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class JavadocWizard extends Wizard implements IExportWizard {

	private JavadocTreeWizardPage fJTWPage;
	private JavadocSpecificsWizardPage fJSWPage;
	private JavadocStandardWizardPage fJSpWPage;
	
	private IPath fDestination;

	private boolean fWriteCustom;
	private boolean fFromAnt;
	private boolean fOpenInBrowser;

	protected final String TreePageDesc= "JavadocTreePage"; //$NON-NLS-1$
	protected final String SpecificsPageDesc= "JavadocSpecificsPage"; //$NON-NLS-1$
	protected final String StandardPageDesc= "JavadocStandardPage"; //$NON-NLS-1$

	private JavadocOptionsManager fStore;
	private IWorkspaceRoot fRoot;
	private IJavaProject fSelectedProject;

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
		fFromAnt= (xmlJavadocFile != null);
		
		fSelectedProject= null;
	}

	/*
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() {
		
		//writes the new settings to store
		fJTWPage.finish();
		if(!fJTWPage.getCustom())
			fJSpWPage.finish();
		fJSWPage.finish();
		
		if (!checkPreconditions(fStore.getSourceElements())) {
			return false;
		}

		fDestination= new Path(fStore.getDestination(fStore.getJavaProject()));
		fDestination.toFile().mkdirs();

		this.fOpenInBrowser= fStore.doOpenInBrowser();

		try {
			URL currURL= JavaDocLocations.getProjectJavadocLocation(fStore.getJavaProject());
			URL newURL= fDestination.toFile().toURL();

			if (fStore.fromStandard() && ((currURL == null) || !(currURL.equals(newURL)))) {
				String message=  JavadocExportMessages.getFormattedString("JavadocWizard.updatejavadoclocation.message", new String[] { fStore.getJavaProject().getElementName(), fDestination.toOSString() }); //$NON-NLS-1$
				if (MessageDialog.openQuestion(getShell(), JavadocExportMessages.getString("JavadocWizard.updatejavadocdialog.label"), message)) { //$NON-NLS-1$
					JavaDocLocations.setProjectJavadocLocation(fStore.getJavaProject(), newURL);
				}
			}
		} catch (MalformedURLException e) {
			JavaPlugin.log(e);
		}

		if (fJSWPage.generateAnt()) {
			fStore.createXML();
			refresh(new Path(fStore.getAntpath(fStore.getJavaProject())));
		}

		if (!fFromAnt) {
			getDialogSettings().addSection(fStore.createDialogSettings());
		}

		try {
			String[] args= fStore.createArgumentArray();
		
			if (!executeJavadocGeneration(args))
				return false;
		} catch(CoreException e) {
			JavaPlugin.log(e);
			return false;
		}
		
		return true;
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

				IDebugEventSetListener listener= new JavadocDebugEventListener();
				DebugPlugin.getDefault().addDebugEventListener(listener);
				
				ILaunchConfigurationWorkingCopy wc= null;
				try {
					ILaunchConfigurationType lcType= DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
					String name= JavadocExportMessages.getString("JavadocWizard.launchconfig.name"); //$NON-NLS-1$
					wc= lcType.newInstance(null, name);
					wc.setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, (String) null);
					wc.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
					
					ILaunch newLaunch= new Launch(wc, ILaunchManager.RUN_MODE, null);
					IProcess iprocess= DebugPlugin.newProcess(newLaunch, process, JavadocExportMessages.getString("JavadocWizard.javadocprocess.label")); //$NON-NLS-1$
					iprocess.setAttribute(JavaRuntime.ATTR_CMDLINE, buf.toString());
	
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
	
	private boolean checkPreconditions(IJavaElement[] elements) {
		
		ArrayList resources= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			try {
				if (elements[i] instanceof ICompilationUnit) {
					resources.add(elements[i].getCorrespondingResource());
				}
			} catch(JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		

		//message could be null
		IFile[] unSavedFiles = getUnsavedFiles(resources);
		return saveModifiedResourcesIfUserConfirms(unSavedFiles);
	}
	
	/**
	 * Returns the files which are not saved and which are
	 * part of the files being exported.
	 * 
	 * @return an array of unsaved files
	 */
	private IFile[] getUnsavedFiles(List resources) {
		IEditorPart[] dirtyEditors = JavaPlugin.getDirtyEditors();
		Set unsavedFiles = new HashSet(dirtyEditors.length);
		if (dirtyEditors.length > 0) {
			for (int i = 0; i < dirtyEditors.length; i++) {
				if (dirtyEditors[i].getEditorInput() instanceof IFileEditorInput) {
					IFile dirtyFile =
						((IFileEditorInput) dirtyEditors[i].getEditorInput()).getFile();
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
				} catch(CoreException e) {
					ExceptionHandler.handle(e, getShell(), JavadocExportMessages.getString("JavadocWizard.saveresourcedialogCE.title"), JavadocExportMessages.getString("JavadocWizard.saveresourcedialogCE.message")); //$NON-NLS-1$ //$NON-NLS-2$
				} catch(InvocationTargetException e) {
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
							IFile dirtyFile= ((IFileEditorInput)editorsToSave[i].getEditorInput()).getFile();					
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
		
		this.fSelectedProject= fStore.getJavaProject();
	}

	public void init(IWorkbench workbench, IStructuredSelection structuredSelection) {
		IDialogSettings settings= getDialogSettings().getSection("javadoc"); //$NON-NLS-1$
		fStore= new JavadocOptionsManager(fXmlJavadocFile, settings, structuredSelection);
	}
	
	private void refresh(IPath path) {
		if (fRoot.getContainerForLocation(path) != null) {
			try {
				fRoot.refreshLocal(fJTWPage.fRoot.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}
	
	private void spawnInBrowser() {
		if (fOpenInBrowser) {
			try {
				IPath indexFile= fDestination.append("index.html"); //$NON-NLS-1$
				URL url= indexFile.toFile().toURL();
				OpenBrowserUtil.open(url, getShell(), getWindowTitle());
			} catch (MalformedURLException e) {
				JavaPlugin.log(e);
			}
		}
	}	

	private class JavadocDebugEventListener implements IDebugEventSetListener {
		public void handleDebugEvents(DebugEvent[] events) {
			for (int i= 0; i < events.length; i++) {
				if (events[i].getKind() == DebugEvent.TERMINATE) {
					try {
						if (!fWriteCustom) {
							refresh(fDestination); //If destination of javadoc is in workspace then refresh workspace
							spawnInBrowser();
							
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
		if(page instanceof JavadocTreeWizardPage) {
			if(!fJTWPage.getCustom()) {
				return fJSpWPage;
			}
			return fJSWPage;	
		} else if (page instanceof JavadocSpecificsWizardPage) {
			return null;
		} else if (page instanceof JavadocStandardWizardPage)
			return fJSWPage;
		else return null;
	}
	
	public IWizardPage getPreviousPage(IWizardPage page) {
		if(page instanceof JavadocSpecificsWizardPage) {
			if(!fJTWPage.getCustom()) {
				return fJSpWPage;
			}
			return fJSWPage;	
		} else if (page instanceof JavadocTreeWizardPage) {
			return null;
		} else if (page instanceof JavadocStandardWizardPage)
			return fJTWPage;
		else return null;	
	}
	
 	protected void setProject(IJavaProject project) {
 		this.fSelectedProject= project;	
 	}	
 	
 	protected IJavaProject getProject() {
 		return this.fSelectedProject;	
 	}
}