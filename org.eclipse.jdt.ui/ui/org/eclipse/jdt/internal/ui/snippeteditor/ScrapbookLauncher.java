/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.snippeteditor;
 
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIEventFilter;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.DebugJavaUtils;
import org.eclipse.jdt.internal.debug.ui.JDIModelPresentation;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.launcher.JavaApplicationLauncher;
import org.eclipse.jdt.internal.ui.launcher.JavaLaunchUtils;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

public class ScrapbookLauncher extends JavaApplicationLauncher implements IDebugEventListener {
	
	IBreakpoint fMagicBreakpoint;
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
			if (launchers[i].getIdentifier().equals("org.eclipse.jdt.ui.launcher.ScrapbookLauncher")) { //$NON-NLS-1$
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
		
		if (!page.getFileExtension().equals("jpage")) { //$NON-NLS-1$
			showNoPageDialog();
			return false;
		}
		
		if (fScrapbookToVMs.get(page) != null) {
			//already launched
			return false;
		}
		
		IJavaProject javaProject= JavaCore.create(page.getProject());
			
		URL pluginInstallURL= JavaPlugin.getDefault().getDescriptor().getInstallURL();
		URL jarURL = null;
		try {
			jarURL = new URL(pluginInstallURL, "snippetsupport.jar"); //$NON-NLS-1$
			jarURL = Platform.asLocalURL(jarURL);
		} catch (MalformedURLException e) {
			JavaPlugin.log(e);
			return false;
		} catch (IOException e) {
			JavaPlugin.log(e);
			return false;
		}
		
		String[] classPath = new String[] {jarURL.getFile()};
		
		return doLaunch(javaProject, mode, page, classPath, launcher);
	}

	protected boolean doLaunch(IJavaProject p, String mode, IFile page, String[] classPath, ILauncher launcherProxy) {
		try {
			IVMRunner launcher= getJavaLauncher(p, mode);
			if (launcher == null) {
				showNoLauncherDialog();
				return false;
			}
			
			VMRunnerConfiguration config= new VMRunnerConfiguration("org.eclipse.jdt.internal.ui.snippeteditor.ScrapbookMain", classPath); //$NON-NLS-1$
			ISourceLocator sl= new ProjectSourceLocator(p);
			try {
				IPath outputLocation =	p.getOutputLocation();
				IResource outputFolder = p.getProject().getWorkspace().getRoot().findMember(outputLocation);
				if (outputFolder == null) {
					return false;
				}
				IPath osPath = outputFolder.getLocation();
				java.io.File f = new java.io.File(osPath.toOSString());
				URL u = null;
				try {
					u = f.toURL();
				} catch (MalformedURLException e) {
					return false;
				}
				String url = u.toExternalForm();
				config.setProgramArguments(new String[] {url});
			} catch (JavaModelException e) {
				return false;
			}
			
			VMRunnerResult result= launcher.run(config);
			if (result != null) {
				IDebugTarget dt = result.getDebugTarget();
				IBreakpoint magicBreakpoint = createMagicBreakpoint(getMainType(p));
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
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), SnippetMessages.getString("ScrapbookLauncher.error.title"), SnippetMessages.getString("ScrapbookLauncher.error.exception"), e.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
		}
		return false;
	}

	IBreakpoint createMagicBreakpoint(IType type) {
		try {
			fMagicBreakpoint= JDIDebugModel.createSnippetSupportLineBreakpoint(type, 54, -1, -1, 0);
			return fMagicBreakpoint;
		} catch (DebugException e) {
			e.printStackTrace();
		}
		return null;
	}

	IType getMainType(IJavaProject jp) {	
		try {
			return jp.getPackageFragmentRoot(jp.getUnderlyingResource()).
				getPackageFragment("org.eclipse.jdt.internal.ui.snippeteditor"). //$NON-NLS-1$
				getClassFile("ScrapbookMain.class").getType(); //$NON-NLS-1$
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
					if (o instanceof IFile && ((IFile)o).getFileExtension().equals("jpage")) { //$NON-NLS-1$
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
			cleanup((IDebugTarget)event.getSource());
		}
	}
	
	public IDebugTarget getDebugTarget(IFile page) {
		return (IDebugTarget)fScrapbookToVMs.get(page);
	}
	
	public IBreakpoint getMagicBreakpoint(IDebugTarget target) {
		return (IBreakpoint)fVMsToBreakpoints.get(target);
	}
	
	protected void showNoPageDialog() {
		String title= SnippetMessages.getString("ScrapbookLauncher.error.title"); //$NON-NLS-1$
		String msg= SnippetMessages.getString("ScrapbookLauncher.error.pagenotfound"); //$NON-NLS-1$
		MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(),title, msg);
	}
	
	protected void cleanup(IDebugTarget target) {
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