/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OpenExternalJavadocAction;

public class JavadocWizard extends Wizard implements IExportWizard {

	private JavadocTreeWizardPage fJTWPage;
	private JavadocSpecificsWizardPage fJSWPage;

	private IPath fDestination;
	private IJavaProject fCurrentProject;

	private boolean fWriteCustom;
	private boolean fFromAnt;
	private boolean fOpenInBrowser;

	protected final String TreePageDesc= "JavadocTreePage";
	protected final String SpecificsPageDesc= "JavadocSpecificsPage";

	private JavadocOptionsManager fStore;
	private IWorkspaceRoot fRoot;

	private IFile fXmlJavadocFile;
	
	//private ILaunchConfiguration fConfig;

	public JavadocWizard() {
		this(null);
	}

	public JavadocWizard(IFile xmlJavadocFile) {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR);
		setWindowTitle("Generate Javadoc");

		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());

		//@Added
		//--
		fRoot= ResourcesPlugin.getWorkspace().getRoot();
		fXmlJavadocFile= xmlJavadocFile;

		//--
		fCurrentProject= null;
		fWriteCustom= false;
		fFromAnt= (xmlJavadocFile != null);
	}

	/*
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() {

		//writes the new settings to store
		fJTWPage.finish();
		fJSWPage.finish();

		fDestination= new Path(fStore.getDestination());
		fDestination.toFile().mkdirs();

		if (fJSWPage.openInBrowser()) {
			this.fOpenInBrowser= true;
		}

		try {
			URL currURL= JavaDocLocations.getProjectJavadocLocation(fStore.getJavaProject());
			URL newURL= fDestination.toFile().toURL();

			if (fStore.fromStandard() && ((currURL == null) || !(currURL.equals(newURL)))) {
				String message=  "Do you want to update the Javadoc location for ''{0}'' with the chosen destination folder ''{1}''?";
				if (MessageDialog.openQuestion(getShell(), "Update Javadoc Location", MessageFormat.format(message, new String[] { fStore.getJavaProject().getElementName(), fStore.getDestination() }))) {
					JavaDocLocations.setProjectJavadocLocation(fStore.getJavaProject(), newURL);
				}
			}
		} catch (MalformedURLException e) {
			JavaPlugin.log(e);
		}

		if (fJSWPage.generateAnt()) {
			fStore.createXML();
			refresh(new Path(fStore.getAntpath()));
		}

		if (!fFromAnt) {
			getDialogSettings().addSection(fStore.createDialogSettings());
		}

		String[] args= fStore.createArgumentArray();
		if (!executeJavadocGeneration(args))
			return false;
		
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

				IProcess iprocess= DebugPlugin.newProcess(process, "Javadoc Generation");
				iprocess.setAttribute(JavaRuntime.ATTR_CMDLINE, buf.toString());

				IProcess[] iProcesses= new IProcess[] { iprocess };

				IDebugEventListener listener= new JavadocDebugEventListener();
				DebugPlugin.getDefault().addDebugEventListener(listener);
				
				ILaunchConfigurationWorkingCopy wc= null;
			//	fConfig= null;
				try {
					ILaunchConfigurationType lcType= DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
					String name= "Javadoc Export Wizard";// + System.currentTimeMillis();
					wc= lcType.newInstance(null, name);
					wc.setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, (String) null);
					//fConfig= wc.doSave();

					ILaunch newLaunch= new Launch(wc, ILaunchManager.RUN_MODE, null, iProcesses, null);
					DebugPlugin.getDefault().getLaunchManager().addLaunch(newLaunch);

				} catch (CoreException e) {
					JavaPlugin.logErrorMessage(e.getMessage());
				}

				return true;

			}
		} catch (IOException e) {
			JavaPlugin.log(e);
			return false;
		}
		return false;

	}

	/*
	 * @see IWizard#addPages()
	 */
	public void addPages() {
		fJTWPage= new JavadocTreeWizardPage(TreePageDesc, fStore);
		fJSWPage= new JavadocSpecificsWizardPage(SpecificsPageDesc, fStore, fJTWPage);

		super.addPage(fJTWPage);
		super.addPage(fJSWPage);

		fJTWPage.init();
		fJSWPage.init();
	}

	public void init(IWorkbench workbench, IStructuredSelection structuredSelection) {
		IDialogSettings settings= getDialogSettings();
		if (fXmlJavadocFile == null) {
			fStore= new JavadocOptionsManager(settings.getSection("javadoc"), fRoot, structuredSelection);
		} else {
			fStore= new JavadocOptionsManager(fXmlJavadocFile, settings, fRoot, structuredSelection);
		}
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
				IPath indexFile= fDestination.append("index.html");
				URL url= indexFile.toFile().toURL();
				OpenExternalJavadocAction.openInBrowser(url, getShell());
			} catch (MalformedURLException e) {
				JavaPlugin.logErrorMessage(e.getMessage());
			}
		}
	}	

	private class JavadocDebugEventListener implements IDebugEventListener {

		public JavadocDebugEventListener() {
		}

		public void handleDebugEvent(DebugEvent event) {
			if (event.getKind() == DebugEvent.TERMINATE) {
				try {
//					if (fConfig != null) {
//						try {
//							fConfig.delete();
//						} catch (CoreException e) {
//							JavaPlugin.log(e);
//						}
//					}
					if (!fWriteCustom) {
						refresh(fDestination); //If destination of javadoc is in workspace then refresh workspace
						spawnInBrowser();
					}
				} finally {
					DebugPlugin.getDefault().removeDebugEventListener(this);
				}
			}
		}
	}
	//	
}