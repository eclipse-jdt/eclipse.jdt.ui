/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;

/**
 * @version 	1.0
 * @author
 */
public class JavadocWizard extends Wizard implements IExportWizard {

	private JavadocTreeWizardPage fJTWPage;
	private JavadocSpecificsWizardPage fJSWPage;

	final static int PRIVATE= 0;
	final static int PACKAGE= 1;
	final static int PROTECTED= 2;
	final static int PUBLIC= 3;

	public JavadocWizard() {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR);
		setWindowTitle("Generate Javadoc");

		//used to record settings
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());

	}

	/*
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() {

		ArrayList cFlags= new ArrayList();

		String jdocCommand= JavadocPreferencePage.getJavaDocCommand();

		StringBuffer commandLine= new StringBuffer(jdocCommand);
		fJTWPage.collectArguments(cFlags);
		fJSWPage.collectArguments(cFlags);
		Object[] flags= cFlags.toArray();
		for (int i= 0; i < flags.length; i++) {
			commandLine.append((String) flags[i]);
		}
		commandLine.append(fJTWPage.getFileListArgument());

		//for testing
		//System.out.println(commandLine);
		
		IPath dest= fJTWPage.getDestination();
		if (dest != null) {
			dest.toFile().mkdirs();
		}
		

		if (!executeJavadocGeneration(commandLine.toString()))
			return false;
			
		fJTWPage.preserveDialogSettings();
		fJSWPage.preserveDialogSettings();

		return true;
	}

	private boolean executeJavadocGeneration(String command) {
		Process process= null;
		try {
			process= Runtime.getRuntime().exec(command);
			if (process != null) {
				IProcess iprocess= DebugPlugin.newProcess(process, "Javadoc Generation");
				iprocess.setAttribute(JavaRuntime.ATTR_CMDLINE, command);

				IProcess[] iProcesses= new IProcess[] { iprocess };

				Launch newLaunch= new Launch(null, ILaunchManager.RUN_MODE, fJTWPage.fRoot, null, iProcesses, null);
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
		fJTWPage= new JavadocTreeWizardPage("Javadoc Tree Page", getDialogSettings());
		fJSWPage= new JavadocSpecificsWizardPage("Javadoc Page", getDialogSettings(), fJTWPage);

		super.addPage(fJTWPage);
		super.addPage(fJSWPage);

		fJTWPage.init();
		fJSWPage.init();
	}

	public void init(IWorkbench workbench, IStructuredSelection structuredSelection) {
	}

}