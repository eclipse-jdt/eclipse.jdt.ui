package org.eclipse.jdt.testplugin.ui;

import junit.textui.TestRunner;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.preference.PreferenceManager;

import org.eclipse.core.boot.IPlatformRunnable;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.WorkbenchPlugin;

public class JavaTestUIApplication implements IPlatformRunnable {

	public static final String APP_NAME= "org.eclipse.jdt.testplugin.test";

	public Object run(Object arguments) throws Exception {
		
		if (arguments instanceof String[]) {
			// create a display for the current thread
			Display display=new Display();
			// a dummy workbench
			WorkbenchPlugin.getDefault().setWorkbench(new TestWorkbench());
			TestRunner.main((String[])arguments);
		}
		return null;
	}
	
	
	public class TestWorkbench implements IWorkbench {

		public boolean close() {
			return true;
		}

		public IWorkbenchWindow getActiveWorkbenchWindow() {
			return null;
		}

		public IEditorRegistry getEditorRegistry() {
			return WorkbenchPlugin.getDefault().getEditorRegistry();
		}

		public IPerspectiveRegistry getPerspectiveRegistry() {
			return WorkbenchPlugin.getDefault().getPerspectiveRegistry();
		}

		public PreferenceManager getPreferenceManager() {
			return WorkbenchPlugin.getDefault().getPreferenceManager();
		}

		public ISharedImages getSharedImages() {
			return WorkbenchPlugin.getDefault().getSharedImages();
		}

		public IWorkbenchWindow[] getWorkbenchWindows() {
			return new IWorkbenchWindow[0];
		}

		public IWorkbenchWindow openWorkbenchWindow(IAdaptable input) throws WorkbenchException {
			return null;
		}

		public IWorkbenchWindow openWorkbenchWindow(String perspID, IAdaptable input) throws WorkbenchException {
			return null;
		}
	}	
}