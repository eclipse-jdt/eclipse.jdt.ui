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
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import org.junit.Test;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.SeparateVMsLaunch;

/**
 * Tests the launch that runs several VMs one after the other.
 */
public class SeparateVMsLaunchTest {

	private static class TestProcess implements IProcess {
		private final ILaunch fLaunch;

		private volatile boolean fTerminated;

		TestProcess(ILaunch launch) {
			fLaunch= launch;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public boolean canTerminate() {
			return !fTerminated;
		}

		@Override
		public boolean isTerminated() {
			return fTerminated;
		}

		@Override
		public void terminate() {
			fTerminated= true;
		}

		@Override
		public String getLabel() {
			return "TestProcess";
		}

		@Override
		public ILaunch getLaunch() {
			return fLaunch;
		}

		@Override
		public IStreamsProxy getStreamsProxy() {
			return null;
		}

		@Override
		public void setAttribute(String key, String value) {
		}

		@Override
		public String getAttribute(String key) {
			return null;
		}

		@Override
		public int getExitValue() {
			return 0;
		}
	}

	private final List<TestProcess> fStartedVMs= new CopyOnWriteArrayList<>();

	private SeparateVMsLaunch fLaunch;

	private void startVMs(int count) throws Exception {
		ILaunchConfiguration configuration= DebugPlugin.getDefault().getLaunchManager()
				.getLaunchConfigurationType(JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION)
				.newInstance(null, "SeparateVMsLaunchTest");
		fLaunch= new SeparateVMsLaunch(configuration, ILaunchManager.RUN_MODE);
		ICoreRunnable vm= _ -> {
			TestProcess process= new TestProcess(fLaunch);
			fStartedVMs.add(process);
			fLaunch.addProcess(process);
		};
		fLaunch.startVMs(Collections.nCopies(count, vm), null);
	}

	private void terminateVM(TestProcess process) {
		process.terminate();
		fLaunch.handleDebugEvents(new DebugEvent[] { new DebugEvent(process, DebugEvent.TERMINATE) });
	}

	private boolean waitFor(BooleanSupplier condition) throws InterruptedException {
		long end= System.currentTimeMillis() + 10_000;
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > end) {
				return false;
			}
			Thread.sleep(20);
		}
		return true;
	}

	@Test
	public void testVMsAreStartedOneAfterTheOther() throws Exception {
		startVMs(3);
		assertEquals(1, fStartedVMs.size());
		assertFalse(fLaunch.isTerminated());

		terminateVM(fStartedVMs.get(0));
		assertTrue("Second VM has not been started", waitFor(() -> fStartedVMs.size() == 2));
		assertFalse(fLaunch.isTerminated());

		terminateVM(fStartedVMs.get(1));
		assertTrue("Third VM has not been started", waitFor(() -> fStartedVMs.size() == 3));
		assertFalse(fLaunch.isTerminated());

		terminateVM(fStartedVMs.get(2));
		assertTrue("Launch has not terminated", waitFor(fLaunch::isTerminated));
		assertEquals(3, fStartedVMs.size());
	}

	@Test
	public void testTerminateCancelsPendingVMs() throws Exception {
		startVMs(3);
		fLaunch.terminate();

		assertTrue(fStartedVMs.get(0).isTerminated());
		assertTrue("Launch has not terminated", waitFor(fLaunch::isTerminated));
		assertEquals(1, fStartedVMs.size());
	}

	/**
	 * A VM that is about to be started by a job, as the previous one has terminated, must not be
	 * started if the launch is terminated before the job runs.
	 */
	@Test
	public void testTerminateBeforeScheduledVMIsStarted() throws Exception {
		startVMs(2);
		Job.getJobManager().suspend();
		try {
			// the job that starts the second VM is scheduled, but cannot run yet
			terminateVM(fStartedVMs.get(0));
			assertFalse(fLaunch.isTerminated());
			fLaunch.terminate();
		} finally {
			Job.getJobManager().resume();
		}

		assertTrue("Launch has not terminated", waitFor(fLaunch::isTerminated));
		assertEquals("A VM has been started after the launch was terminated", 1, fStartedVMs.size());
	}
}
