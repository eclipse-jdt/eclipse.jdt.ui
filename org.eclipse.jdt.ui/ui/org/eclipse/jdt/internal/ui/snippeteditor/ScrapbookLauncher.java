/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;
 
import java.io.File;import java.io.IOException;import java.net.MalformedURLException;import java.net.URL;import java.util.ArrayList;import java.util.HashMap;import java.util.Iterator;import java.util.List;import java.util.Map;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IMarker;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRunnable;import org.eclipse.core.resources.ResourcesPlugin;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.MultiStatus;import org.eclipse.core.runtime.Platform;import org.eclipse.debug.core.DebugEvent;import org.eclipse.debug.core.DebugException;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.IDebugEventListener;import org.eclipse.debug.core.ILaunchManager;import org.eclipse.debug.core.ILauncher;import org.eclipse.debug.core.Launch;import org.eclipse.debug.core.model.IDebugTarget;import org.eclipse.debug.core.model.ISourceLocator;import org.eclipse.debug.ui.DebugUITools;import org.eclipse.debug.ui.IDebugUIEventFilter;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.debug.core.JDIDebugModel;import org.eclipse.jdt.internal.debug.core.DebugJavaUtils;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.launcher.JavaApplicationLauncher;import org.eclipse.jdt.internal.ui.launcher.JavaLaunchUtils;import org.eclipse.jdt.internal.ui.util.PortingFinder;import org.eclipse.jdt.launching.IVMRunner;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.launching.VMRunnerConfiguration;import org.eclipse.jdt.launching.VMRunnerResult;import org.eclipse.jdt.launching.WorkspaceSourceLocator;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.viewers.IStructuredSelection;

public class ScrapbookLauncher extends JavaApplicationLauncher implements IDebugEventListener {
	
	IMarker fMagicBreakpoint;
	DebugException fDebugException;
	
	HashMap fScrapbookToVMs = new HashMap(10);
	HashMap fVMsToBreakpoints = new HashMap(10);
	HashMap fVMsToScrapbooks = new HashMap(10);
	HashMap fVMsToFilters = new HashMap(10);
	
	public ScrapbookLauncher() {
		DebugPlugin.getDefault().addDebugEventListener(this);
	}
	
	public static ILauncher getLauncher() {
		ILauncher[] launchers = DebugPlugin.getDefault().getLaunchManager().getLaunchers();
		ILauncher me= null;
		for (int i = 0; i < launchers.length; i++) {
			if (launchers[i].getIdentifier().equals("org.eclipse.jdt.ui.launcher.ScrapbookLauncher")) {
				me = launchers[i];
				break; 
			}
		}
		return me;
	}
	
	public static ScrapbookLauncher getDefault() {
		return (ScrapbookLauncher)getLauncher().getDelegate();
	}
	
	/**
	 *	@see ILauncher#launch
	 */
	public boolean launch(Object runnable, String mode, ILauncher launcher) {
		
		if (!(runnable instanceof IFile)) {
			showNoPageDialog();
			return false;
		}

		IFile page = (IFile)runnable;
		
		if (!page.getFileExtension().equals("jpage")) {
			showNoPageDialog();
			return false;
		}
		
		if (fScrapbookToVMs.get(page) != null) {
			//already launched
			return false;
		}
		
		IJavaProject javaProject= JavaCore.create(page.getProject());
		ISourceLocator locator= new WorkspaceSourceLocator(javaProject.getProject().getWorkspace());
		String[] classPath= null;
		MultiStatus warnings= new MultiStatus(JavaPlugin.getPluginId(), IStatus.OK, "status", null);
		try {
			classPath= JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
		} catch (CoreException e) {
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), ERROR_CLASSPATH_PREFIX, warnings);
			return false;
		}
		
		String[] extraPath = new String[classPath.length + 1];
		System.arraycopy(classPath, 0, extraPath, 0, classPath.length);
			
		URL pluginInstallURL= JavaPlugin.getDefault().getDescriptor().getInstallURL();
		URL jarURL = null;
		try {
			jarURL = new URL(pluginInstallURL, "snippetsupport.jar");
			jarURL = Platform.asLocalURL(jarURL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		extraPath[classPath.length] = jarURL.getFile();
		
		return doLaunch(javaProject, mode, page, extraPath, launcher);
	}

	protected boolean doLaunch(IJavaProject p, String mode, IFile page, String[] classPath, ILauncher launcherProxy) {
		try {
			IVMRunner launcher= getJavaLauncher(p, mode);
			if (launcher == null) {
				showNoLauncherDialog();
				return false;
			}
			
			VMRunnerConfiguration config= new VMRunnerConfiguration("org.eclipse.jdt.internal.ui.snippeteditor.ScrapbookMain", classPath);
			ISourceLocator sl = new WorkspaceSourceLocator(p.getProject().getWorkspace());
			try {
				IPath outputLocation =	p.getOutputLocation();
				IResource outputFolder = p.getProject().getWorkspace().getRoot().findMember(outputLocation);
				if (outputFolder == null) {
					return false;
				}
				IPath osPath = outputFolder.getLocation();
				String url = "file:/" + osPath.toOSString();
				url = url.replace(File.separatorChar, '/');
				config.setProgramArguments(new String[] {url});
			} catch (JavaModelException e) {
				return false;
			}
			
			VMRunnerResult result= launcher.run(config);
			if (result != null) {
				IDebugTarget dt = result.getDebugTarget();
				IMarker magicBreakpoint = createMagicBreakpoint(getMainType(p));
				fScrapbookToVMs.put(page, dt);
				fVMsToScrapbooks.put(dt, page);
				fVMsToBreakpoints.put(dt, magicBreakpoint);
				dt.breakpointAdded(magicBreakpoint);
				Launch newLaunch= new Launch(launcherProxy, mode, page, sl,result.getProcesses(), dt);
				IDebugUIEventFilter filter = new ScrapbookEventFilter(newLaunch);
				fVMsToFilters.put(dt, filter);
				DebugUITools.addEventFilter(filter);
				DebugPlugin.getDefault().getLaunchManager().registerLaunch(newLaunch);
				return true;
			}
		} catch (CoreException e) {
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), ERROR_NO_LAUNCHER_PREFIX, e.getStatus());
		}
		return false;
	}

	IMarker createMagicBreakpoint(IType type) {
		try {
			return createSnippetSupportBreakpoint(type, 60, -1, -1, 0);
		} catch (DebugException e) {
			e.printStackTrace();
		}
		return null;
	}


	private IMarker createSnippetSupportBreakpoint(final IType type, final int lineNumber, final int charStart, final int charEnd, final int hitCount) throws DebugException {
		// determine the resource to associate the marker with

		IWorkspaceRunnable wr= new IWorkspaceRunnable() {

			public void run(IProgressMonitor monitor) throws CoreException {
				IResource resource= type.getJavaProject().getProject();
				// create the marker
				try {
					fMagicBreakpoint = resource.createMarker("org.eclipse.jdt.ui.snippetSupportLineBreakpoint");
				} catch (CoreException e) {
					fDebugException = new DebugException(e.getStatus());
					return;
				}

				// configure the standard attributes
				try {
					DebugPlugin.getDefault().getBreakpointManager().configureLineBreakpoint(fMagicBreakpoint, JDIDebugModel.getPluginIdentifier(), true, lineNumber, charStart, charEnd);
				} catch (CoreException e) {
					fDebugException= new DebugException(e.getStatus());
					return;
				}

				// configure the type handle and hit count
				DebugJavaUtils.setTypeAndHitCount(fMagicBreakpoint, type, hitCount);

				// configure the marker as a Java marker
				Map attributes= fMagicBreakpoint.getAttributes();
				JavaCore.addJavaElementMarkerAttributes(attributes, type);
				fMagicBreakpoint.setAttributes(attributes);
			}

		};
		
		try {
			ResourcesPlugin.getWorkspace().run(wr, null);
		} catch (CoreException e) {
			if (fDebugException == null) {
				fDebugException = new DebugException(e.getStatus());
			}
		}

		if (fDebugException != null) {
			throw fDebugException;
		}
		return fMagicBreakpoint;
	}

	IType getMainType(IJavaProject jp) {	
		try {
			return jp.getPackageFragmentRoot(jp.getUnderlyingResource()).
				getPackageFragment("org.eclipse.jdt.internal.ui.snippeteditor").
				getClassFile("ScrapbookMain.class").getType();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return null;
	}
		
	/**
	 * Returns a collection of elements this launcher is capable of launching
	 * in the specified mode based on the given selection. If this launcher cannot launch any
	 * elements in the current selection, an empty collection or <code>null</code>
	 * is returned.
	 */
	public List getLaunchableElements(IStructuredSelection selection, String mode) {
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			if (!selection.isEmpty()) {
				ArrayList list = new ArrayList(1);
				Iterator i = selection.iterator();
				while (i.hasNext()) {
					Object o = i.next();
					if (o instanceof IFile && ((IFile)o).getFileExtension().equals("jpage")) {
						list.add(o);
					}
				}
				return list;
			}
		} 
		return new ArrayList(0);
	}

	public void handleDebugEvent(DebugEvent event) {
		if (event.getSource() instanceof IDebugTarget && event.getKind() == DebugEvent.TERMINATE) {
			Object target = event.getSource();
			Object page = fVMsToScrapbooks.get(target);
			if (page != null) {
				fVMsToScrapbooks.remove(target);
				fScrapbookToVMs.remove(page);
				fVMsToBreakpoints.remove(target);
				IDebugUIEventFilter filter = (IDebugUIEventFilter)fVMsToFilters.remove(target);
				DebugUITools.removeEventFilter(filter);
			}
		}
	}
	
	public IDebugTarget getDebugTarget(IFile page) {
		return (IDebugTarget)fScrapbookToVMs.get(page);
	}
	
	public IMarker getMagicBreakpoint(IDebugTarget target) {
		return (IMarker)fVMsToBreakpoints.get(target);
	}
	
	protected void showNoPageDialog() {
		String title= JavaPlugin.getResourceString("SnippetEditor.error.nopagetitle");
		String msg= JavaPlugin.getResourceString("SnippetEditor.error.nopagemsg");
		MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(),title, msg);
	}
}