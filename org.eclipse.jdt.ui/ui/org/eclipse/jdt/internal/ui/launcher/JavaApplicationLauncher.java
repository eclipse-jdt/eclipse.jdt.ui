package org.eclipse.jdt.internal.ui.launcher;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

import org.eclipse.ui.IFileEditorInput;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILauncherDelegate;
import org.eclipse.debug.core.model.ISourceLocator;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.debug.ui.JavaApplicationWizard;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.util.Utilities;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.ProjectSourceLocator;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.VMRunnerResult;

/**
 * A launcher for running java main classes. Uses JDI to launch a vm in debug 
 * mode.
 */
public class JavaApplicationLauncher implements ILauncherDelegate, IExecutableExtension {
	
	private String fId;
	private ISourceLocator fSourceLocator;
	private List fModes= new ArrayList(2);
		
	private static final String ATTR_MODE= "modes";
	private static final String ATTR_ID= "id";
	protected final static String PREFIX= "launcher.";
	protected final static String LABEL= PREFIX+"label";
	protected final static String ERROR_NOFILE_PREFIX= PREFIX+ "error.noFile.";
	protected final static String ERROR_NOARGS_PREFIX= PREFIX+ "info.noArgs.";
	protected final static String ERROR_CLASSPATH_PREFIX= PREFIX+"error.classpath.";
	protected final static String ERROR_LAUNCH_PREFIX= PREFIX+ "error.launch.";
	protected final static String ERROR_NO_LAUNCHER_PREFIX= PREFIX + "error.no_launcher.";
	
	protected final static String INFO_NOMAIN= PREFIX+"info.noMain.";
	
	protected final static String PROGRESS_LAUNCHING= PREFIX+"progress.launching";

	
	/** The list of managed processes */
	protected MainMethodFinder fTargetFinder= new MainMethodFinder();
		
	/**
	 * Creates a new launcher
	 */
	public JavaApplicationLauncher() {
	}
		
	/**
	 *	@see ILauncher#launch
	 */
	public boolean launch(Object runnable, String mode, ILauncher launcher) {
		
		if (!(runnable instanceof IType)) {
			showNoMainDialog();
			return false;
		}
		IType mainType= (IType)runnable;

		IJavaProject javaProject= mainType.getJavaProject();
		fSourceLocator= new ProjectSourceLocator(javaProject);
		ExecutionArguments args= null;
		String[] classPath= null;
		try {
			classPath= JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
		} catch (CoreException e) {
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), ERROR_CLASSPATH_PREFIX, e.getStatus());
			return false;
		}
		try {
			args= ExecutionArguments.getArguments(mainType);
		} catch (CoreException e) {
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), ERROR_NOARGS_PREFIX, e.getStatus());
			return false;
		}
		return doLaunch(javaProject, mode, mainType, args, classPath, launcher);
	}
	
	/**
	 *	@see ILauncher#launch
	 */
	public boolean launch(Object[] objects, String mode, ILauncher launcher) {
		IStructuredSelection selection = new StructuredSelection(objects);
		if (selection == null) {
			return false;
		}
		List elements= getLaunchableElements(selection, mode);
		if (elements.size() > 1) {
			return useWizard(elements, selection, mode, launcher);
		}
		if (elements.size() == 0) {
			showNoMainDialog();
			return false;
		}
		Iterator itr= elements.iterator();
		if (itr.hasNext()) {
			Object launchable= itr.next();
			return launch(launchable, mode, launcher);
		}
		return true;
	}	
	
	/**
	 * Use the wizard to do the launch.
	 */
	protected boolean useWizard(List elements, IStructuredSelection selection, String mode, ILauncher launcher) {
		JavaApplicationWizard w= new JavaApplicationWizard(elements);
		w.init(launcher, mode, selection);
		WizardDialog dialog= new WizardDialog(JavaPlugin.getActiveWorkbenchShell(), w);
		return dialog.open() == dialog.OK;
	}
	
	protected void showNoMainDialog() {
		String title= JavaLaunchUtils.getResourceString(INFO_NOMAIN+"title");
		String msg= JavaLaunchUtils.getResourceString(INFO_NOMAIN+"message");
		MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(),title, msg);
	}

	protected void showNoLauncherDialog() {
		String title= JavaLaunchUtils.getResourceString(ERROR_NO_LAUNCHER_PREFIX+"title");
		String msg= JavaLaunchUtils.getResourceString(ERROR_NO_LAUNCHER_PREFIX+"message");
		MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(),title, msg);
	}

	protected boolean doLaunch(final IJavaProject p, final String mode, final IType mainType, ExecutionArguments args, String[] classPath, final ILauncher launcherProxy) {
		try {
			final IVMRunner launcher= getJavaLauncher(p, mode);
			if (launcher == null) {
				showNoLauncherDialog();
				return false;
			}
			
			String vmArgs= null;
			String programArgs= null;
			if (args != null) {
				vmArgs= args.getVMArguments();
				programArgs= args.getProgramArguments();
			}
			final VMRunnerConfiguration config= new VMRunnerConfiguration(JavaModelUtility.getFullyQualifiedName(mainType), classPath);
			config.setVMArguments(JavaLaunchUtils.parseArguments(vmArgs));
			config.setProgramArguments(JavaLaunchUtils.parseArguments(programArgs));
			
			IRunnableWithProgress r= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					if (!p.getProject().getWorkspace().isAutoBuilding()) {
						try {
							p.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, pm);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					}
					pm.beginTask(JavaLaunchUtils.getResourceString(PROGRESS_LAUNCHING), IProgressMonitor.UNKNOWN);
					VMRunnerResult result= launcher.run(config);
					if (result != null) {
						Launch newLaunch= new Launch(launcherProxy, mode, mainType, getSourceLocator(),result.getProcesses(), result.getDebugTarget());
						registerLaunch(newLaunch);
					}
				}
			};
			
			try {
				new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell()).run(true, false, r);		
			} catch (InterruptedException e) {
				return false;
			} catch (InvocationTargetException e) {
				return false;
			}
			return true;
		} catch (CoreException e) {
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), ERROR_NO_LAUNCHER_PREFIX, e.getStatus());
		}
		return false;
	}
	
	private void registerLaunch(final ILaunch launch) {
		Utilities.getDisplay(null).syncExec(new Runnable() {
			public void run() {
				DebugPlugin.getDefault().getLaunchManager().registerLaunch(launch);
			}
		});
	}

	protected IJavaProject getProjectFor(Object o) {
		if (o instanceof IFileEditorInput) {
			o= ((IFileEditorInput)o).getFile();
		} else if (o instanceof ClassFileEditorInput) {
			o= ((ClassFileEditorInput)o).getClassFile();
		}
		if (o instanceof IJavaElement) {
			IJavaElement element= (IJavaElement)o;
			IJavaProject javaProject= element.getJavaProject();
			if (javaProject != null)
				return javaProject;
			return null;
		} 
		if (o instanceof IResource) {
			IResource res= (IResource)o;
			return JavaCore.create(res.getProject());
		}
		return null;
	}
	
	protected IVMRunner getJavaLauncher(IJavaProject p, String mode) throws CoreException {
		String vm= null;
		if (p != null)
			vm= JavaRuntime.getJavaRuntime(p);
		if (vm == null)
			vm= JavaPlugin.getDefault().getPreferenceStore().getString(VMPreferencePage.PREF_VM);
		return JavaRuntime.getVMLauncher(mode, vm);
	}
	
	protected IVMRunner getJavaLauncher(IStructuredSelection selection, String mode) throws CoreException {
		Iterator elements= selection.iterator();
		Object o= null;
		if (elements.hasNext())
			o= elements.next();
		return getJavaLauncher(getProjectFor(o), mode);
	}

	
	
	
	
	
	
	
	/**
	 * Returns a collection of elements this launcher is capable of launching
	 * in the specified mode based on the given selection. If this launcher cannot launch any
	 * elements in the current selection, an empty collection or <code>null</code>
	 * is returned.
	 */
	public List getLaunchableElements(IStructuredSelection selection, String mode) {
		try {
			if (getJavaLauncher(selection, mode) == null) {
				showNoLauncherDialog();
				return new ArrayList(1);
			}
		} catch (CoreException e) {
			// ignore, we simply can't launch.
		}
		return fTargetFinder.findTargets(selection);
	}

	protected ISourceLocator getSourceLocator() {
		return fSourceLocator;
	}

	// initialization
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		String modes= config.getAttribute(ATTR_MODE);
		fId= config.getAttribute(ATTR_ID);
		StringTokenizer tokenizer= new StringTokenizer(modes, ",");
		while (tokenizer.hasMoreTokens()) {
			fModes.add(tokenizer.nextToken().trim());
		}
	}
}
