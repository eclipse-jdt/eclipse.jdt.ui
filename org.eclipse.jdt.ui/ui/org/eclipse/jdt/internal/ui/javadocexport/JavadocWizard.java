/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.xml.sax.SAXException;

/**
 * @version 	1.0
 * @author
 */
public class JavadocWizard extends Wizard implements IExportWizard {

	private JavadocTreeWizardPage fJTWPage;
	private JavadocSpecificsWizardPage fJSWPage;
	
	protected static IJavaProject currentProject;
	protected static boolean WRITECUSTOM;
	private boolean fromAnt;
	
	private IFile xmlJavadocFile;
	
	protected final static String STANDARD= "destdir";
	protected final static String DOCLET= "doclet";
	protected final static String DOCLETPATH= "docletpath";
	protected final static String SOURCEPATH= "sourcepath";
	protected final static String CLASSPATH= "classpath";
	protected final static String PRIVATE= "private";
	protected final static String PROTECTED= "protected";
	protected final static String PACKAGE= "package";
	protected final static String PUBLIC= "public";
	protected final static String NOTREE= "notree";
	protected final static String NOINDEX= "noindex";
	protected final static String NONAVBAR= "nonavbar";
	protected final static String NODEPRECATED= "nodeprecated";
	protected final static String NODEPRECATEDLIST= "nodeprecatedlist";
	protected final static String VERSION= "version";
	protected final static String AUTHOR= "author";
	protected final static String SPLITINDEX= "splitindex";
	protected final static String STYLESHEET= "stylesheetfile";
	protected final static String OVERVIEW= "overview";
	
	protected final static String VISIBILITY= "access";
	protected final static String PACKAGENAMES= "packagenames";
	protected final static String EXTRAOPTIONS= "additionalparam";
	protected final static String JAVADOCCOMMAND= "javadoccommand";
	protected final static String PROJECT= "projectname";
	protected final static String ANT= "ant";

	protected final static String TreePageDesc= "JavadocTreePage";
	protected final static String SpecificsPageDesc= "JavadocSpecificsPage";
	
	private DialogSettingsModel settings;

	public JavadocWizard() {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR);
		setWindowTitle("Generate Javadoc");

		//used to record settings
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		
		currentProject= null;
		WRITECUSTOM= false;
		fromAnt= false;

	}

	public JavadocWizard(IFile xmlJavadocFile){
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR);
		setWindowTitle("Generate Javadoc");
		
		//shouldn't need to do this here
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		
		currentProject= null;
		WRITECUSTOM= false;
		
		fromAnt= true;
		this.xmlJavadocFile= xmlJavadocFile;
	}
	
	/*
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() { 

		ArrayList cFlags= new ArrayList();
		Map map= new HashMap();

		String jdocCommand= JavadocPreferencePage.getJavaDocCommand();

		cFlags.add(jdocCommand);
		map.put(JavadocWizard.JAVADOCCOMMAND, jdocCommand);
		fJTWPage.collectArguments(cFlags, map);
		fJSWPage.collectArguments(cFlags, map);
		String filename= fJTWPage.getFileListArgument(map);
		cFlags.add("@"+ filename);
		//@testing
		//System.out.println(filename);
		
		
		
		IPath dest= fJTWPage.getDestination();
		if (dest != null) {
			dest.toFile().mkdirs();
		}
		
		String[] args= (String[]) cFlags.toArray(new String[cFlags.size()]);
		
		if (!executeJavadocGeneration(args))
			return false;
		
		if(fJSWPage.generateAnt()) {
			try {
				dest= currentProject.getCorrespondingResource().getLocation();
				File file= dest.addTrailingSeparator().append(fJSWPage.fAntText.getText()).toFile();
			
				FileOutputStream objectStreamOutput= new FileOutputStream(file);
				JavadocWriter writer= new JavadocWriter(objectStreamOutput);
				writer.writeXML(map);
				
				objectStreamOutput.close();
			} catch(JavaModelException e) {
				JavaPlugin.log(e);
			} catch(IOException e) {
				JavaPlugin.log(e);
			}	
		}
		if(!fromAnt) {	
			settings.addSection(fJTWPage.preserveDialogSettings());
			settings.addSection(fJSWPage.preserveDialogSettings());
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
				
			//@hack
			//--	
				IDebugEventListener listener= new JavadocDebugEventListener(iprocess);
				DebugPlugin.getDefault().addDebugEventListener(listener);
				
				ILaunch newLaunch= new Launch(null, ILaunchManager.RUN_MODE, fJTWPage.fRoot, null, iProcesses, null);
				DebugPlugin.getDefault().getLaunchManager().addLaunch(newLaunch);
			//--
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
		try {
			
			if (!fromAnt) {
				this.settings = new DialogSettingsModel(getDialogSettings());
				fJTWPage = new JavadocTreeWizardPage(TreePageDesc, settings.getSection(TreePageDesc));
			fJSWPage =
				new JavadocSpecificsWizardPage(
					SpecificsPageDesc,
					settings.getSection(SpecificsPageDesc),
					fJTWPage);
			} else {
				JavadocReader reader = new JavadocReader(xmlJavadocFile.getContents());
				this.settings = new DialogSettingsModel(reader.readXML());
				fJTWPage = new JavadocTreeWizardPage(TreePageDesc, settings);
				fJSWPage =
				new JavadocSpecificsWizardPage(
					SpecificsPageDesc,
					settings,
					fJTWPage);
			}

			
		} catch (CoreException e) {
			JavaPlugin.logErrorMessage("Core exception, unable to create wizard");
			return;
		} catch (IOException e) {
			JavaPlugin.logErrorMessage("IO exception, unable to create wizard");
			return;
		} catch (SAXException e) {
			JavaPlugin.logErrorMessage("SAX exception, unable to create wizard");
			return;
		}
		super.addPage(fJTWPage);
		super.addPage(fJSWPage);

		fJTWPage.init();
		fJSWPage.init();
	}

	public void init(IWorkbench workbench, IStructuredSelection structuredSelection) {
	}
	
	private class JavadocDebugEventListener implements IDebugEventListener {
	
		private IProcess iprocess;
		private boolean finished;
	
		public JavadocDebugEventListener(IProcess process){
			this.iprocess= process;
			finished= false;
		}
		
		public void handleDebugEvent(DebugEvent event) {
			if(iprocess.isTerminated() && !finished) {
				//System.out.println("terminated");
				fJSWPage.finish();
				fJTWPage.finish();
				finished= true;	
			}	
			
		}
	}
}