/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.IOException;

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
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class JavadocWizard extends Wizard implements IExportWizard {

	private JavadocTreeWizardPage fJTWPage;
	private JavadocSpecificsWizardPage fJSWPage;
	
	private IPath dest;
	protected static IJavaProject currentProject;
	
	protected static boolean fWriteCustom;
	private boolean fromAnt;
	
	private IFile xmlJavadocFile;

	protected final String TreePageDesc= "JavadocTreePage";
	protected final String SpecificsPageDesc= "JavadocSpecificsPage";
	
	private JavadocOptionsManager store;
	private IWorkspaceRoot fRoot;
	
	private IFile fXmlJavadocFile;

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
		currentProject= null;
		fWriteCustom= false;
		fromAnt= (xmlJavadocFile != null);
	}
	
	public IWorkspaceRoot getRoot() {
		return fRoot;
	}
	
	/*
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() { 

		
		//writes the new settings to store
		fJTWPage.finish();
		fJSWPage.finish();

		dest= new Path(store.getDestination());
		if (dest != null) {
			dest.toFile().mkdirs();
		}
		
		String[] args= store.creatArgumentArray();
		
		if (!executeJavadocGeneration(args))
			return false;
		
		if(fJSWPage.generateAnt()) {
			store.createXML();	
		}
		
		if(!fromAnt) {	
			getDialogSettings().addSection(store.createDialogSettings());
		}
		try {
			JavaPlugin.getWorkspace().getRoot().refreshLocal(IWorkspaceRoot.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			JavaPlugin.logErrorMessage("unable to refresh");
		}
		return true;
	}

	private boolean executeJavadocGeneration(String[] args) {
		Process process= null;	
		try {
			process= Runtime.getRuntime().exec(args);
			if (process != null) {
				IProcess iprocess= DebugPlugin.newProcess(process, "Javadoc Generation");
				iprocess.setAttribute(JavaRuntime.ATTR_CMDLINE, "Javadoc Generation");

				IProcess[] iProcesses= new IProcess[] { iprocess };
				
				IDebugEventListener listener= new JavadocDebugEventListener(iprocess);
				DebugPlugin.getDefault().addDebugEventListener(listener);
				
				ILaunch newLaunch= new Launch(null, ILaunchManager.RUN_MODE, fJTWPage.fRoot, null, iProcesses, null);
				DebugPlugin.getDefault().getLaunchManager().addLaunch(newLaunch);
			
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
		fJTWPage = new JavadocTreeWizardPage(TreePageDesc, store);
		fJSWPage = new JavadocSpecificsWizardPage(SpecificsPageDesc, store, fJTWPage);

		super.addPage(fJTWPage);
		super.addPage(fJSWPage);

		fJTWPage.init();
		fJSWPage.init();
	}
	

	public void init(IWorkbench workbench, IStructuredSelection structuredSelection) {
		if (fXmlJavadocFile == null) {
			IDialogSettings settings= getDialogSettings();
			store= new JavadocOptionsManager(settings.getSection("javadoc"), fRoot, structuredSelection);
		} else {
			store= new JavadocOptionsManager(fXmlJavadocFile, fRoot, structuredSelection);
		}
	}
	
	private class JavadocDebugEventListener implements IDebugEventListener {
	
		private IProcess iprocess;
		private boolean finished;
	
		public JavadocDebugEventListener(IProcess process){
			this.iprocess= process;
			finished= false;
		}
		
		public void handleDebugEvent(DebugEvent event) {
			if (event.getKind()==DebugEvent.TERMINATE) {
				//@test
				//System.out.println("terminated");
				
				//If destination of javadoc is in workspace then refresh workspace
				if (!fWriteCustom) {
					if (fRoot.getContainerForLocation(dest) != null) {
						try {
							fRoot.refreshLocal(fJTWPage.fRoot.DEPTH_INFINITE, null);
							//@test
							//System.out.println("refreshed");
						} catch (CoreException e) {
							//@test
							//System.out.println("unable to refres");
						}
					}else { //@test
						//System.out.println("not in workspace...no refresh");
					}
				} else {
					//@test
					//System.out.println("not using standard doclet");
				}
			}
		}
	}
}