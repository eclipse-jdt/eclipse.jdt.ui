/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.JDIDebugModel;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

public class AddExceptionAction implements IViewActionDelegate {
	protected final static String PREFIX= "launcher.add_exception.";
	protected final static String ERROR_HIERARCHY_PREFIX= PREFIX+"error_hierarchy.";
	protected final static String ERROR_HIERARCHY_STATUS= ERROR_HIERARCHY_PREFIX+"status";

	private static class ExceptionRenderer2 extends LabelProvider {
		public String getText(Object element) {
			if (element instanceof ExceptionItem) {
				ExceptionItem item= (ExceptionItem)element;
				String fullName= item.getName();
				int index= fullName.lastIndexOf(".");
				if (index < 0)
					return fullName;
				String pkg= fullName.substring(0, index);
				String simpleName= fullName.substring(index+1);
				return simpleName + " - " + pkg;
			}
			return super.getText(element);
		}
		public Image getImage(Object element) {
			if (element instanceof ExceptionItem) {
				ExceptionItem item= (ExceptionItem)element;	
				if (item.isError())
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ERROR);		
			}
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		}
	}
	
	private static class ExceptionItem {
		private IType fType;
		private boolean fIsError;
		public ExceptionItem(IType type, boolean isError) {
			fType = type;
			fIsError= isError;
		}
		
		public String getName() {
			return JavaModelUtility.getFullyQualifiedName(fType);
		}
		
		public boolean isError() {
			return fIsError;
		}
	}

	public void run(IAction action) {
		IWorkspace ws= JavaPlugin.getWorkspace();
		final IProject[] projects= ws.getRoot().getProjects();
		final Hashtable exceptions= new Hashtable();
		
		String statusLabel= JavaLaunchUtils.getResourceString(ERROR_HIERARCHY_STATUS);
		final MultiStatus status= new MultiStatus(JavaPlugin.getPluginId(), IStatus.OK, statusLabel, null);
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				Hashtable roots= new Hashtable(5);
				for (int i= 0; i < projects.length; i++) {
					IJavaProject javaProject= JavaCore.create(projects[i]);
					try {
						IType root= getRootException(javaProject);
						if (root != null)
							roots.put(root, root);
					} catch (JavaModelException e) {
						status.merge(e.getStatus());
					}
				}
				Enumeration r= roots.keys();
				while (r.hasMoreElements()) {
					try {
						collectExceptions((IType)r.nextElement(), exceptions, monitor);
					} catch (JavaModelException e) {
						status.merge(e.getStatus());
					}
				}
					
			}
		};
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		try {
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
			dialog.run(true, true, op);
		} catch (InvocationTargetException x) {
			// will now happen
		} catch (InterruptedException e) {
			return;
		}
		
		if (!status.isOK()) {
			JavaLaunchUtils.errorDialog(shell, ERROR_HIERARCHY_PREFIX, status);
		}

		showDialog(shell, exceptions);
	}
	
	private void collectExceptions(IType root, Hashtable exceptions, IProgressMonitor pm) throws JavaModelException {
		if (root != null) {
			ITypeHierarchy hierarchy= root.newTypeHierarchy(pm);
			recursivelyAddExceptions(hierarchy, root, false, exceptions);
		}
	}
	
	private void recursivelyAddExceptions(ITypeHierarchy h, IType root, boolean isError, Hashtable ht) {
		String name= JavaModelUtility.getFullyQualifiedName(root);
		if (!isError)
			isError= "java.lang.Error".equals(name) || "java.lang.RuntimeException".equals(name);
		if (!ht.containsKey(name)) {
			ht.put(name, new ExceptionItem(root, isError));
		}
		IType subtypes[]= h.getSubtypes(root);
		for (int i= 0; i < subtypes.length; i++)  {
			recursivelyAddExceptions(h, subtypes[i], isError, ht);
		}
			
	}
	
	private IType getRootException(IJavaProject proj) throws JavaModelException {
		IJavaElement element= proj.findElement(new Path("java/lang/Throwable.java"));
		if (element == null)
			element= proj.findElement(new Path("java/lang/Throwable.class"));
		if (element instanceof IClassFile)
			return ((IClassFile)element).getType();
		if (element instanceof ICompilationUnit) 
			return ((ICompilationUnit)element).getType("Throwable");
		return null;
	}
	
	private void showDialog(Shell window, Map ht) {
		List names= new ArrayList(ht.size());
		names.addAll(ht.values());
				
		ExceptionSelectionDialog dialog= new ExceptionSelectionDialog(window, new ExceptionRenderer2());
		if (dialog.open(names) == dialog.OK) {
			Object[] result= dialog.getResult();
			boolean caught= dialog.isCaughtEnabled();
			boolean uncaught= dialog.isUncaughtEnabled();
			IBreakpointManager mgr= DebugPlugin.getDefault().getBreakpointManager();
			for (int j= 0; j < result.length; j++) {
				try {
					ExceptionItem item= (ExceptionItem)result[j];
					IMarker e= JDIDebugModel.createExceptionBreakpoint(item.fType, caught, uncaught, item.isError());
					mgr.addBreakpoint(e);
				} catch (DebugException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * @see IViewActionDelegate
	 */
	public void init(IViewPart view) {
	}
	
	/**
	 * @see IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}



}