/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.ITestRunListener2;
import org.eclipse.jdt.internal.junit.model.RemoteTestRunnerClient;
import org.eclipse.jdt.internal.junit.runner.MessageIds;

public class RemoteTestRunnerClientTest {

	private static final int TIMEOUT_MILLIS= 10_000;

	@Test
	public void testUnexpectedEofNotifiesTerminationForJUnit3() throws Exception {
		assertUnexpectedEofNotifiesTermination(TestKindRegistry.JUNIT3_TEST_KIND_ID);
	}

	@Test
	public void testUnexpectedEofNotifiesTerminationForJUnit4() throws Exception {
		assertUnexpectedEofNotifiesTermination(TestKindRegistry.JUNIT4_TEST_KIND_ID);
	}

	private static void assertUnexpectedEofNotifiesTermination(String testKindId) throws Exception {
		RecordingListener listener= new RecordingListener();
		int port= findFreePort();
		RemoteTestRunnerClient client= startClient(listener, port, testKindId);
		try (Socket socket= connect(port);
				InputStream input= socket.getInputStream();
				OutputStream output= socket.getOutputStream()) {
			socket.setSoTimeout(TIMEOUT_MILLIS);
			sendRunStarted(output, listener);
			closeOutputAndWaitForShutdown(socket, input, client);
		} finally {
			client.stopWaiting();
		}

		assertEquals(0, listener.endedCount.get());
		assertEquals(0, listener.stoppedCount.get());
		assertEquals(1, listener.terminatedCount.get());
	}

	@Test
	public void testRunEndIsNotReportedAsTermination() throws Exception {
		RecordingListener listener= new RecordingListener();
		int port= findFreePort();
		RemoteTestRunnerClient client= startClient(listener, port, TestKindRegistry.JUNIT4_TEST_KIND_ID);
		try (Socket socket= connect(port);
				InputStream input= socket.getInputStream();
				OutputStream output= socket.getOutputStream()) {
			socket.setSoTimeout(TIMEOUT_MILLIS);
			sendRunStarted(output, listener);
			sendMessage(output, MessageIds.TEST_RUN_END + "0"); //$NON-NLS-1$
			closeOutputAndWaitForShutdown(socket, input, client);
		} finally {
			client.stopWaiting();
		}

		assertEquals(1, listener.endedCount.get());
		assertEquals(0, listener.stoppedCount.get());
		assertEquals(0, listener.terminatedCount.get());
	}

	@Test
	public void testRunEndAfterStopRequestIsNotReportedAsStopped() throws Exception {
		RecordingListener listener= new RecordingListener();
		int port= findFreePort();
		RemoteTestRunnerClient client= startClient(listener, port, TestKindRegistry.JUNIT4_TEST_KIND_ID);
		try (Socket socket= connect(port);
				InputStream input= socket.getInputStream();
				OutputStream output= socket.getOutputStream()) {
			socket.setSoTimeout(TIMEOUT_MILLIS);
			sendRunStarted(output, listener);
			client.stopTest();
			sendMessage(output, MessageIds.TEST_RUN_END + "0"); //$NON-NLS-1$
			closeOutputAndWaitForShutdown(socket, input, client);
		} finally {
			client.stopWaiting();
		}

		assertEquals(1, listener.endedCount.get());
		assertEquals(0, listener.stoppedCount.get());
		assertEquals(0, listener.terminatedCount.get());
	}

	@Test
	public void testStoppedRunIsNotReportedTwiceOnEof() throws Exception {
		RecordingListener listener= new RecordingListener();
		int port= findFreePort();
		RemoteTestRunnerClient client= startClient(listener, port, TestKindRegistry.JUNIT4_TEST_KIND_ID);
		try (Socket socket= connect(port);
				InputStream input= socket.getInputStream();
				OutputStream output= socket.getOutputStream()) {
			socket.setSoTimeout(TIMEOUT_MILLIS);
			sendRunStarted(output, listener);
			sendMessage(output, MessageIds.TEST_STOPPED + "0"); //$NON-NLS-1$
			waitForShutdown(input, client);
		} finally {
			client.stopWaiting();
		}

		assertEquals(0, listener.endedCount.get());
		assertEquals(1, listener.stoppedCount.get());
		assertEquals(0, listener.terminatedCount.get());
	}

	private static RemoteTestRunnerClient startClient(RecordingListener listener, int port, String testKindId) {
		ITestKind testKind= TestKindRegistry.getDefault().getKind(testKindId);
		assertFalse("JUnit test kind is unavailable: " + testKindId, testKind.isNull());
		RemoteTestRunnerClient client= new RemoteTestRunnerClient(testKind);
		client.startListening(new ITestRunListener2[] { listener }, port);
		return client;
	}

	private static Socket connect(int port) throws Exception {
		IOException lastException= null;
		long deadline= System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);
		do {
			try {
				return new Socket(InetAddress.getLoopbackAddress(), port);
			} catch (IOException e) {
				lastException= e;
				Thread.sleep(10);
			}
		} while (System.nanoTime() < deadline);
		throw new IOException("Could not connect to the JUnit client", lastException); //$NON-NLS-1$
	}

	private static int findFreePort() throws IOException {
		try (ServerSocket serverSocket= new ServerSocket(0)) {
			return serverSocket.getLocalPort();
		}
	}

	private static void sendRunStarted(OutputStream output, RecordingListener listener) throws Exception {
		sendMessage(output, MessageIds.TEST_RUN_START + "1 v2"); //$NON-NLS-1$
		assertTrue("JUnit client did not report the test run start",
				listener.started.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
	}

	private static void sendMessage(OutputStream output, String message) throws IOException {
		output.write((message + '\n').getBytes(StandardCharsets.UTF_8));
		output.flush();
	}

	private static void closeOutputAndWaitForShutdown(Socket socket, InputStream input, RemoteTestRunnerClient client) throws Exception {
		socket.shutdownOutput();
		waitForShutdown(input, client);
	}

	private static void waitForShutdown(InputStream input, RemoteTestRunnerClient client) throws Exception {
		while (input.read() != -1) {
			// Drain requests sent by the client until it closes the connection.
		}
		// Synchronize with RemoteTestRunnerClient.shutDown(), which uses the same monitor.
		client.stopWaiting();
	}

	private static class RecordingListener implements ITestRunListener2 {

		final CountDownLatch started= new CountDownLatch(1);
		final AtomicInteger endedCount= new AtomicInteger();
		final AtomicInteger stoppedCount= new AtomicInteger();
		final AtomicInteger terminatedCount= new AtomicInteger();

		@Override
		public void testRunStarted(int testCount) {
			started.countDown();
		}

		@Override
		public void testRunEnded(long elapsedTime) {
			endedCount.incrementAndGet();
		}

		@Override
		public void testRunStopped(long elapsedTime) {
			stoppedCount.incrementAndGet();
		}

		@Override
		public void testRunTerminated() {
			terminatedCount.incrementAndGet();
		}

		@Override
		public void testStarted(String testId, String testName) {
		}

		@Override
		public void testEnded(String testId, String testName) {
		}

		@Override
		public void testTreeEntry(String description) {
		}

		@Override
		public void testFailed(int status, String testId, String testName, String trace, String expected, String actual) {
		}

		@Override
		public void testReran(String testId, String testClass, String testName, int status, String trace,
				String expected, String actual) {
		}
	}
}
