/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.launcher;

import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.Iterator;import java.util.List;import java.util.StringTokenizer;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IncrementalProjectBuilder;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IConfigurationElement;import org.eclipse.core.runtime.IExecutableExtension;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.ILaunch;import org.eclipse.debug.core.ILauncher;import org.eclipse.debug.core.Launch;import org.eclipse.debug.core.model.ILauncherDelegate;import org.eclipse.debug.core.model.ISourceLocator;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.debug.ui.JavaApplicationWizard;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.internal.ui.util.Utilities;import org.eclipse.jdt.launching.ExecutionArguments;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMRunner;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.launching.ProjectSourceLocator;import org.eclipse.jdt.launching.VMRunnerConfiguration;import org.eclipse.jdt.launching.VMRunnerResult;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.wizard.WizardDialog;import org.eclipse.ui.IFileEditorInput;

/**
 * A launcher for running java main classes. Uses JDI to launch a vm in debug 
 * mode.
 */
public class JavaApplicationLauncher implements ILauncherDelegate, IExecutableExtension {
	
	private String fId;
	private ISourceLocator fSourceLocator;
	private List fModes= new ArrayList(2);
		
	private static final String ATTR_MODE= "modes"; //$NON-NLS-1$
	private static final String ATTR_ID= "id"; //$NON-NLS-1$
	
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
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), LauncherMessages.getString("javaAppLauncher.error.title"), LauncherMessages.getString("javaAppLauncher.error.classpath"), e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		try {
			args= ExecutionArguments.getArguments(mainType);
		} catch (CoreException e) {
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), LauncherMessages.getString("javaAppLauncher.error.title"), LauncherMessages.getString("javaAppLauncher.error.readArgs"), e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
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
		String title= LauncherMessages.getString("javaAppLauncher.error.title"); //$NON-NLS-1$
		String msg= LauncherMessages.getString("javaAppLauncher.noMainClass"); //$NON-NLS-1$
		MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(),title, msg);
	}

	protected void showNoLauncherDialog() {
		String title= LauncherMessages.getString("javaAppLauncher.error.title"); //$NON-NLS-1$
		String msg= LauncherMessages.getString("javaAppLauncher.error.noJRE"); //$NON-NLS-1$
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
			
			final VMRunnerResult[] result= new VMRunnerResult[1];
			
			IRunnableWithProgress r= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					if (!p.getProject().getWorkspace().isAutoBuilding()) {
						try {
							p.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, pm);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					}
					pm.beginTask(LauncherMessages.getString("javaAppLauncher.progress.startVM"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
					result[0]= launcher.run(config);
				}
			};
			
			try {
				new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell()).run(true, false, r);		
			} catch (InterruptedException e) {
				return false;
			} catch (InvocationTargetException e) {
				return false;
			}
				if (result[0] != null) {
					Launch newLaunch= new Launch(launcherProxy, mode, mainType, getSourceLocator(),result[0].getProcesses(), result[0].getDebugTarget());
					registerLaunch(newLaunch);
				}
			return true;
		} catch (CoreException e) {
			showNoLauncherDialog();
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
		if (p != null) {
			IVMInstall vm= JavaRuntime.getVMInstall(p);
			if (vm == null)
				vm= JavaRuntime.getDefaultVMInstall();
			if (vm != null)
				return vm.getVMRunner(mode);
		}
		return null;
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
		StringTokenizer tokenizer= new StringTokenizer(modes, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			fModes.add(tokenizer.nextToken().trim());
		}
	}
	
	public String getLaunchMemento(Object element) {
		if (element instanceof IJavaElement) {
			return ((IJavaElement)element).getHandleIdentifier();
		}
		return null;
	}
	
	public Object getLaunchObject(String memento) {
		return JavaCore.create(memento);
	}
}
