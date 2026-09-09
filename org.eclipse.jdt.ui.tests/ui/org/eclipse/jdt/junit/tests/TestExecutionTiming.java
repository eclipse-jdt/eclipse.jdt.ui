/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.runner.FirstRunExecutionListener;
import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.MessageSender;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;

public class TestExecutionTiming extends AbstractTestRunListenerTest {

	@Override
	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("TestExecutionTiming", "bin"); //$NON-NLS-1$ //$NON-NLS-2$
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH));
		JavaProjectHelper.addRTJar18(fProject);
	}

	@Test
	public void testRunnerSendsTimingBeforeTestEnd() throws Exception {
		List<String> messages= new ArrayList<>();
		MessageSender sender= new MessageSender() {
			@Override
			public void sendMessage(String msg) {
				messages.add(msg);
			}

			@Override
			public void flush() {
			}
		};

		RemoteTestRunner runner= new RemoteTestRunner();
		runner.setMessageSender(sender);
		FirstRunExecutionListener listener= runner.firstRunExecutionListener();
		ITestIdentifier test= new ITestIdentifier() {
			@Override
			public String getName() {
				return "testTiming(pack.TimingTest)"; //$NON-NLS-1$
			}

			@Override
			public String getDisplayName() {
				return getName();
			}

			@Override
			public String getParameterTypes() {
				return ""; //$NON-NLS-1$
			}

			@Override
			public String getUniqueId() {
				return ""; //$NON-NLS-1$
			}
		};

		listener.notifyTestStarted(test);
		Thread.sleep(10);
		listener.notifyTestEnded(test);

		assertEquals(3, messages.size());
		assertTrue(messages.get(0).startsWith(MessageIds.TEST_START));
		assertTrue(messages.get(1).startsWith(MessageIds.TEST_TIMING));
		assertTrue(messages.get(2).startsWith(MessageIds.TEST_END));

		String startPayload= messages.get(0).substring(MessageIds.MSG_HEADER_LENGTH);
		String testId= startPayload.substring(0, startPayload.indexOf(','));
		String[] timing= messages.get(1).substring(MessageIds.MSG_HEADER_LENGTH).split(",", -1); //$NON-NLS-1$
		assertEquals(5, timing.length);
		assertEquals(testId, timing[0]);
		assertTrue(Long.parseLong(timing[1]) >= 0);
		assertTrue(Long.parseLong(timing[2]) > 0);
		long cpuTime= Long.parseLong(timing[3]);
		long userTime= Long.parseLong(timing[4]);
		assertTrue(cpuTime >= -1);
		assertTrue(userTime >= -1);
		if (cpuTime >= 0 && userTime >= 0) {
			assertTrue(userTime <= cpuTime);
		}
	}

	@Test
	public void testExecutionTimingReachesTestModel() throws Exception {
		String source=
				"""
				package pack;
				import org.junit.jupiter.api.Test;
				public class TimingTest {
				    @Test public void testTiming() throws Exception {
				        long end = System.nanoTime() + 20_000_000L;
				        while (System.nanoTime() < end) { Math.sqrt(12345.6789); }
				        Thread.sleep(100);
				    }
				}""";
		IType timingTest= createType(source, "pack", "TimingTest.java"); //$NON-NLS-1$ //$NON-NLS-2$

		AtomicReference<double[]> capturedTiming= new AtomicReference<>();
		TestRunLog log= new TestRunLog();
		TestRunListener sequenceListener= new TestRunListeners.SequenceTest(log);
		TestRunListener timingListener= new TestRunListener() {
			@Override
			public void testCaseFinished(ITestCaseElement testCaseElement) {
				TestElement element= (TestElement) testCaseElement;
				capturedTiming.set(new double[] {
						element.getElapsedTimeInSeconds(),
						element.getCpuTimeInSeconds(),
						element.getUserCpuTimeInSeconds(),
						element.getSystemCpuTimeInSeconds(),
						element.getNonCpuTimeInSeconds()
				});
			}
		};
		JUnitCore.addTestRunListener(sequenceListener);
		JUnitCore.addTestRunListener(timingListener);
		try {
			launchJUnit(timingTest, TestKindRegistry.JUNIT5_TEST_KIND_ID, log);
		} finally {
			JUnitCore.removeTestRunListener(timingListener);
			JUnitCore.removeTestRunListener(sequenceListener);
		}

		double[] timing= capturedTiming.get();
		assertNotNull(timing);
		assertTrue("wall time should include Thread.sleep", timing[0] >= 0.08d); //$NON-NLS-1$
		if (!Double.isNaN(timing[1])) {
			assertTrue(timing[1] >= 0.0d);
			assertTrue("non-CPU time should include most of Thread.sleep", timing[4] >= 0.05d); //$NON-NLS-1$
			if (!Double.isNaN(timing[2])) {
				assertTrue(timing[2] <= timing[1]);
				assertTrue(timing[3] >= 0.0d);
			}
		}
	}
}
