package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A <code>IRunnableWithProgress</code> that runs a wrapped 
 * <code>IWorkspaceRunnable</code>. <code>OperationCanceledException</code> throws
 * by the runnabled are cought and rethrown as a <code>InterruptedException</code>
 */
public class WorkbenchRunnableWrapper implements IRunnableWithProgress {
	
	private IWorkspaceRunnable fWorkspaceRunnable;
	
	public WorkbenchRunnableWrapper(IWorkspaceRunnable runnable) {
		fWorkspaceRunnable= runnable;
	}

	/*
	 * @see IRunnableWithProgress#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			JavaPlugin.getWorkspace().run(fWorkspaceRunnable, monitor);
		} catch (OperationCanceledException e) {
			throw new InterruptedException(e.getMessage());
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

}

