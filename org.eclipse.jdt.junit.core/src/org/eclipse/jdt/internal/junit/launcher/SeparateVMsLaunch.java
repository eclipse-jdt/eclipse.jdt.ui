/*******************************************************************************
 * Copyright (c) 2026 Hélios GILLES and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hélios GILLES - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.Messages;

/**
 * A launch that runs several VMs one after the other: a VM is started once the processes and
 * debug targets of the previous VM have terminated. The launch is terminated once its last VM has
 * terminated, so that it is seen as a single test run.
 */
public class SeparateVMsLaunch extends Launch {

	/**
	 * The VMs that have not been started yet.
	 */
	private final Queue<ICoreRunnable> fPendingVMs= new ArrayDeque<>();

	/**
	 * Whether a VM is being started.
	 */
	private boolean fStartingVM;

	/**
	 * Whether the pending VMs have been canceled.
	 */
	private boolean fCanceled;

	/**
	 * Whether the listeners have been notified that this launch has terminated.
	 */
	private boolean fTerminateFired;

	public SeparateVMsLaunch(ILaunchConfiguration launchConfiguration, String mode) {
		super(launchConfiguration, mode, null);
	}

	/**
	 * Starts the first of the given VMs, and the next ones in a job when the previous one has
	 * terminated.
	 *
	 * @param vms the VMs to start, each one adding its process and debug target to this launch
	 * @param monitor the progress monitor to start the first VM
	 * @throws CoreException if the first VM could not be started
	 */
	public void startVMs(List<ICoreRunnable> vms, IProgressMonitor monitor) throws CoreException {
		ICoreRunnable firstVM;
		synchronized (this) {
			fPendingVMs.addAll(vms);
			firstVM= fPendingVMs.remove();
			fStartingVM= true;
		}
		startVM(firstVM, monitor);
	}

	/**
	 * Cancels the VMs that have not been started yet.
	 */
	public void cancelPendingVMs() {
		synchronized (this) {
			fPendingVMs.clear();
			fCanceled= true;
		}
		fireTerminateIfTerminated();
	}

	private void startVM(ICoreRunnable vm, IProgressMonitor monitor) throws CoreException {
		try {
			vm.run(monitor);
		} catch (CoreException | RuntimeException e) {
			cancelPendingVMs();
			throw e;
		} finally {
			vmStarted();
		}
	}

	private void vmStarted() {
		boolean canceled;
		synchronized (this) {
			fStartingVM= false;
			canceled= fCanceled;
		}
		if (canceled) {
			// the VM has been canceled while it was being started
			try {
				super.terminate();
			} catch (DebugException e) {
				ILog.of(SeparateVMsLaunch.class).log(e.getStatus());
			}
			fireTerminateIfTerminated();
		} else if (!startNextVM()) {
			// the VM may already have terminated
			fireTerminateIfTerminated();
		}
	}

	/**
	 * Starts the next VM if the previous one has terminated.
	 *
	 * @return <code>true</code> if the next VM is being started
	 */
	private boolean startNextVM() {
		ICoreRunnable vm;
		synchronized (this) {
			if (fStartingVM || !super.isTerminated()) {
				return false;
			}
			vm= fPendingVMs.poll();
			if (vm == null) {
				return false;
			}
			fStartingVM= true;
		}
		String name= Messages.format(JUnitMessages.SeparateVMsLaunch_starting_vm, getLaunchConfiguration().getName());
		Job.create(name, (ICoreRunnable) monitor -> startVM(vm, monitor)).schedule();
		return true;
	}

	private void fireTerminateIfTerminated() {
		if (isTerminated()) {
			fireTerminate();
		}
	}

	@Override
	protected void fireTerminate() {
		synchronized (this) {
			if (fTerminateFired) {
				return;
			}
			fTerminateFired= true;
		}
		super.fireTerminate();
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		super.handleDebugEvents(events);
		for (DebugEvent event : events) {
			if (event.getKind() == DebugEvent.TERMINATE && isChild(event.getSource())) {
				startNextVM();
				return;
			}
		}
	}

	private boolean isChild(Object source) {
		if (source instanceof IProcess process) {
			return equals(process.getLaunch());
		}
		if (source instanceof IDebugTarget debugTarget) {
			return equals(debugTarget.getLaunch());
		}
		return false;
	}

	@Override
	public boolean isTerminated() {
		synchronized (this) {
			if (fStartingVM || !fPendingVMs.isEmpty()) {
				return false;
			}
		}
		return super.isTerminated();
	}

	@Override
	public boolean canTerminate() {
		synchronized (this) {
			if (fStartingVM || !fPendingVMs.isEmpty()) {
				return true;
			}
		}
		return super.canTerminate();
	}

	@Override
	public void terminate() throws DebugException {
		cancelPendingVMs();
		super.terminate();
	}

	@Override
	public void launchRemoved(ILaunch launch) {
		if (equals(launch)) {
			synchronized (this) {
				fPendingVMs.clear();
				fCanceled= true;
			}
		}
		super.launchRemoved(launch);
	}
}
