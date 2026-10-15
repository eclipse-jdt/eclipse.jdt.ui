/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Julien Ruaux: jruaux@octo.com
 * 	   Vincent Massol: vmassol@octo.com
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;

/**
 * The client side of the RemoteTestRunner. Handles the
 * marshaling of the different messages.
 */
public class RemoteTestRunnerClient {

	private static final ILog LOG = ILog.of(RemoteTestRunnerClient.class);

	public abstract static class ListenerSafeRunnable implements ISafeRunnable {
		@Override
		public void handleException(Throwable exception) {
			LOG.error(exception.getMessage(), exception);
		}
	}
	/**
	 * A simple state machine to process requests from the RemoteTestRunner
	 */
	abstract static class ProcessingState {
	    abstract ProcessingState readMessage(String message);
	}

	class DefaultProcessingState extends ProcessingState {
	    @Override
		ProcessingState readMessage(String message) {
	        if (message.startsWith(MessageIds.TRACE_START)) {
	        	fFailedTrace.setLength(0);
	            return fTraceState;
	        }
	        if (message.startsWith(MessageIds.EXPECTED_START)) {
	            fExpectedResult.setLength(0);
	            return fExpectedState;
	        }
	        if (message.startsWith(MessageIds.ACTUAL_START)) {
	            fActualResult.setLength(0);
	            return fActualState;
	        }
	        if (message.startsWith(MessageIds.RTRACE_START)) {
	            fFailedRerunTrace.setLength(0);
	            return fRerunState;
	        }
	        String arg= message.substring(MessageIds.MSG_HEADER_LENGTH);
	        if (message.startsWith(MessageIds.TEST_RUN_START)) {
	            // version < 2 format: count
	            // version >= 2 format: count+" "+version
	            int count= 0;
	            int v= arg.indexOf(' ');
	            if (v == -1) {
	                fVersion= "v1"; //$NON-NLS-1$
	                count= Integer.parseInt(arg);
	            } else {
	                fVersion= arg.substring(v+1);
	                String sc= arg.substring(0, v);
	                count= Integer.parseInt(sc);
	            }
	            notifyTestRunStarted(count);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_START)) {
	            notifyTestStarted(arg);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_TIMING)) {
	            scanTestTiming(arg);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_END)) {
	            notifyTestEnded(arg);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_ERROR)) {
	            extractFailure(arg, ITestRunListener2.STATUS_ERROR);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_FAILED)) {
	            extractFailure(arg, ITestRunListener2.STATUS_FAILURE);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_RUN_END)) {
	            long elapsedTime = Long.parseLong(arg);
	            testRunEnded(elapsedTime);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_STOPPED)) {
	            long elapsedTime = Long.parseLong(arg);
	            notifyTestRunStopped(elapsedTime);
	            shutDown();
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_TREE)) {
	            notifyTestTreeEntry(arg);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_RERAN)) {
	            if (hasTestId())
	                scanReranMessage(arg);
	            else
	                scanOldReranMessage(arg);
	            return this;
	        }
	        return this;
	    }
	}

	/**
	 * Base class for states in which messages are appended to an internal
	 * string buffer until an end message is read.
	 */
	class AppendingProcessingState extends ProcessingState {
		private final StringBuffer fBuffer;
		private String fEndString;

		AppendingProcessingState(StringBuffer buffer, String endString) {
			this.fBuffer= buffer;
			this.fEndString = endString;
		}

		@Override
		ProcessingState readMessage(String message) {
			if (message.startsWith(fEndString)) {
				entireStringRead();
				return fDefaultState;
			}
			fBuffer.append(message);
			if (fLastLineDelimiter != null)
				fBuffer.append(fLastLineDelimiter);
			return this;
		}

		/**
		 * subclasses can override to do special things when end message is read
		 */
		void entireStringRead() {
		}
	}

	class TraceProcessingState extends AppendingProcessingState {
		TraceProcessingState() {
			super(fFailedTrace, MessageIds.TRACE_END);
		}

		@Override
		void entireStringRead() {
            notifyTestFailed();
            fExpectedResult.setLength(0);
            fActualResult.setLength(0);
		}

	    @Override
		ProcessingState readMessage(String message) {
	        if (message.startsWith(MessageIds.TRACE_END)) {
	        	// Workaround for JUnit 5 test execution stop
	        	// triggered by user: see JUnit5TestReference
	        	String trace = fFailedTrace.toString();
				if(trace.startsWith("java.lang.OutOfMemoryError: Junit5 test stopped by user")) {//$NON-NLS-1$
					// Faked JUnit5 test error, just stop the test
					notifyTestRunStopped(0);
				} else {
					// default Junit4 handling
					notifyTestFailed();
				}
	            fFailedTrace.setLength(0);
	            fActualResult.setLength(0);
	            fExpectedResult.setLength(0);
	            return fDefaultState;
	        }
	        fFailedTrace.append(message);
	        if (fLastLineDelimiter != null)
	        	fFailedTrace.append(fLastLineDelimiter);
	        return this;
	    }
	}

	/**
	 * The failed trace that is currently reported from the RemoteTestRunner
	 */
	private final StringBuffer fFailedTrace = new StringBuffer();
	/**
	 * The expected test result
	 */
	private final StringBuffer fExpectedResult = new StringBuffer();
	/**
	 * The actual test result
	 */
	private final StringBuffer fActualResult = new StringBuffer();
	/**
	 * The failed trace of a reran test
	 */
	private final StringBuffer fFailedRerunTrace = new StringBuffer();


	ProcessingState fDefaultState= new DefaultProcessingState();
	ProcessingState fTraceState= new TraceProcessingState();
	ProcessingState fExpectedState= new AppendingProcessingState(fExpectedResult, MessageIds.EXPECTED_END);
	ProcessingState fActualState= new AppendingProcessingState(fActualResult, MessageIds.ACTUAL_END);
	ProcessingState fRerunState= new AppendingProcessingState(fFailedRerunTrace, MessageIds.RTRACE_END);
	ProcessingState fCurrentState= fDefaultState;

	/**
	 * An array of listeners that are informed about test events.
	 */
	private ITestRunListener2[] fListeners;

	/**
	 * The server socket
	 */
	private ServerSocket fServerSocket;
	private Socket fSocket;
	private int fPort= -1;
	private PrintWriter fWriter;
	private PushbackReader fPushbackReader;
	private String fLastLineDelimiter;
	/**
	 * The protocol version
	 */
	private String fVersion;
	/**
	 * The failed test that is currently reported from the RemoteTestRunner
	 */
	private String fFailedTest;
	/**
	 * The Id of the failed test
	 */
	private String fFailedTestId;
	/**
	 * The kind of failure of the test that is currently reported as failed
	 */
	private int fFailureKind;

	private boolean fDebug;

	private volatile boolean stopped;
	private volatile boolean ended;
	/**
	 * The number of test runs still expected, including the current one. Each test run is performed
	 * by a VM that connects to the server socket after the VM of the previous test run.
	 */
	private volatile int fRemainingTestRuns;
	/**
	 * The elapsed time of the previous test runs, which is added to the one of the current test run
	 * when the listeners are told that the test runs have ended or have been stopped. Only the
	 * thread that reads the messages of the test runs changes it.
	 */
	private volatile long fPreviousElapsedTime;
	/**
	 * Whether the VM of the current test run has ended it, while other test runs were expected: it
	 * sends nothing more, not even {@link MessageIds#TEST_STOPPED} if the test runs are stopped.
	 */
	private volatile boolean fCurrentTestRunEnded;
	/** The number of test runs, see {@link #startListening(ITestRunListener2[], int, int)}. */
	private int fTestRunCount;
	/** The test runs have been stopped, maybe while no VM was connected. */
	private volatile boolean fStopRequested;
	/** No VM connected for the next test run: the listeners have been told why. */
	private volatile boolean fNoVMConnected;
	/** All the VMs have exited: the ones that have connected are read, no other one will connect. */
	private volatile boolean fLaunchTerminated;
	/** How often the server socket checks that the VMs that have not connected yet are still running. */
	private static final int ACCEPT_TIMEOUT_MILLIS= 500;
	private final ITestKind testRunnerKind;

	public RemoteTestRunnerClient(ITestKind testRunnerKind) {
		this.testRunnerKind= testRunnerKind;
	}

	/**
	 * Reads the message stream from the RemoteTestRunner
	 */
	private class ServerConnection extends Thread {
		int fServerPort;

		public ServerConnection(int port) {
			super("ServerConnection"); //$NON-NLS-1$
			fServerPort= port;
		}

		@Override
		public void run() {
			try {
				if (fDebug)
					System.out.println("Creating server socket "+fServerPort); //$NON-NLS-1$
				ServerSocket serverSocket= fServerSocket= new ServerSocket(fServerPort);
				do {
					@SuppressWarnings("resource") // closed by closeConnection() once the VM has been read
					Socket socket= accept(serverSocket);
					if (socket == null) {
						noVMConnected();
						break;
					}
					try {
						connect(socket);
					} catch (IOException | RuntimeException e) {
						socket.close();
						throw e;
					}
					String message;
					while(fPushbackReader != null && (message= readMessage(fPushbackReader)) != null)
						receiveMessage(message);
				} while (prepareNextTestRun());
			} catch (SocketException e) {
				notifyTestRunTerminated();
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
				// fall through
			}
			shutDown();
		}

		/**
		 * Reads the messages of the given VM from now on. A test run stopped while no VM was
		 * connected is stopped in this VM too.
		 */
		private void connect(Socket socket) throws IOException {
			PushbackReader reader= new PushbackReader(new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)));
			PrintWriter writer= new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
			synchronized (RemoteTestRunnerClient.this) {
				// stopTest() sees the socket with its writer
				fPushbackReader= reader;
				fWriter= writer;
				fSocket= socket;
				if (fStopRequested) {
					stopTest();
				}
			}
		}

		/**
		 * No VM connected for the next test run: all the VMs have exited.
		 */
		private void noVMConnected() {
			fNoVMConnected= true;
			if (fStopRequested) {
				// the VM that would have run next has been canceled
				notifyTestRunStopped(0);
			} else {
				// a VM exited without connecting
				notifyTestRunTerminated();
			}
		}

		/**
		 * Waits for the VM of the next test run.
		 *
		 * @return the connection of the VM, or <code>null</code> if all the VMs have exited and
		 *         none is waiting to be accepted
		 */
		private Socket accept(ServerSocket serverSocket) throws IOException {
			if (fTestRunCount <= 1) {
				// stopWaiting() closes the server socket if the VM exits without connecting
				return serverSocket.accept();
			}
			// the server socket is not closed: a VM that has connected while the previous one was
			// read must be read, even if all the VMs have exited since
			serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MILLIS);
			while (true) {
				try {
					return serverSocket.accept();
				} catch (SocketTimeoutException e) {
					if (fLaunchTerminated) {
						// The VMs exited before: a VM that has connected is waiting to be accepted,
						// also if it connected while this thread was waiting for the timeout.
						serverSocket.setSoTimeout(1);
						try {
							return serverSocket.accept();
						} catch (SocketTimeoutException e2) {
							return null;
						}
					}
				}
			}
		}
	}

	/**
	 * Start listening to test runs. Start a server connection that
	 * the RemoteTestRunners can connect to, one after the other.
	 *
	 * @param listeners listeners to inform
	 * @param port port on which the server socket will be opened
	 * @param testRunCount the number of test runs, each one performed by a VM
	 */
	public synchronized void startListening(ITestRunListener2[] listeners, int port, int testRunCount) {
		fListeners= listeners;
		fPort= port;
		fRemainingTestRuns= testRunCount;
		fTestRunCount= testRunCount;
		ServerConnection connection= new ServerConnection(port);
		connection.start();
	}

	/**
	 * Requests to stop the remote test run.
	 */
	public synchronized void stopTest() {
		fRemainingTestRuns= 1;
		// a VM that connects later is stopped as soon as it connects
		fStopRequested= true;
		if (isRunning()) {
			fWriter.println(MessageIds.TEST_STOP);
			fWriter.flush();
			stopped = true;
		}
	}

	public synchronized void stopWaiting() {
		// All the VMs have exited, also when the test runs end normally: the VMs that have connected
		// are still read, see ServerConnection#accept(ServerSocket).
		fLaunchTerminated= true;
		if (fTestRunCount <= 1 && fServerSocket != null  && ! fServerSocket.isClosed() && fSocket == null) {
			shutDown(); // will throw a SocketException in Threads that wait in ServerSocket#accept()
		}
	}

	/**
	 * Closes the connection of the finished test run, if another test run is expected.
	 *
	 * @return <code>true</code> if another test run is expected
	 */
	private boolean prepareNextTestRun() {
		boolean interrupted;
		synchronized (this) {
			if (fServerSocket == null || --fRemainingTestRuns <= 0) {
				return false;
			}
			// the VM may have exited while it was reporting a failure
			interrupted= !fCurrentTestRunEnded;
			closeConnection();
			fCurrentState= fDefaultState;
			fCurrentTestRunEnded= false;
			fFailedTrace.setLength(0);
			fExpectedResult.setLength(0);
			fActualResult.setLength(0);
		}
		if (interrupted) {
			notifyTestRunInterrupted();
		}
		return true;
	}

	private void closeConnection() {
		if (fWriter != null) {
			fWriter.close();
			fWriter= null;
		}
		try {
			if (fPushbackReader != null) {
				fPushbackReader.close();
				fPushbackReader= null;
			}
		} catch(IOException e) {
		}
		try {
			if (fSocket != null) {
				fSocket.close();
				fSocket= null;
			}
		} catch(IOException e) {
		}
	}

	private synchronized void shutDown() {
		if (fDebug)
			System.out.println("shutdown "+fPort); //$NON-NLS-1$

		closeConnection();
		try{
			if (fServerSocket != null) {
				fServerSocket.close();
				fServerSocket= null;
			}
		} catch(IOException e) {
		}
		if (fNoVMConnected) {
			// the listeners have been told that the test runs were stopped or terminated
			return;
		}
		if (stopped || !ended) {
			// JUnit 3 and 4 RemoteTestRunnerClient properly handle notifying stopped/terminated,
			// unless their test run had ended: the test runs were stopped after it
			String testKind= testRunnerKind.getId();
			if (!fCurrentTestRunEnded && (TestKindRegistry.JUNIT3_TEST_KIND_ID.equals(testKind)
					|| TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKind))) {
				return;
			}
			notifyTestRunStopped(0);
		}
	}

	public boolean isRunning() {
		return fSocket != null;
	}

	private String readMessage(PushbackReader in) throws IOException {
		StringBuilder buf= new StringBuilder(128);
		int ch;
		while ((ch= in.read()) != -1) {
			switch (ch) {
			case '\n':
				fLastLineDelimiter= "\n"; //$NON-NLS-1$
				return buf.toString();
			case '\r':
				ch= in.read();
				if (ch == '\n') {
					fLastLineDelimiter= "\r\n"; //$NON-NLS-1$
				} else {
					in.unread(ch);
					fLastLineDelimiter= "\r"; //$NON-NLS-1$
				}
				return buf.toString();
			default:
				buf.append((char) ch);
				break;
			}
		}
		fLastLineDelimiter= null;
		if (buf.length() == 0)
			return null;
		return buf.toString();
	}

	private void receiveMessage(String message) {
	    fCurrentState= fCurrentState.readMessage(message);
	}

	private void scanOldReranMessage(String arg) {
		// OLD V1 format
		// format: className" "testName" "status
		// status: FAILURE, ERROR, OK
		int c= arg.indexOf(" "); //$NON-NLS-1$
		int t= arg.indexOf(" ", c+1); //$NON-NLS-1$
		String className= arg.substring(0, c);
		String testName= arg.substring(c+1, t);
		String status= arg.substring(t+1);
		String testId = className+testName;
		notifyTestReran(testId, className, testName, status);
	}

	private void scanReranMessage(String arg) {
		// format: testId" "className" "testName" "status
		// status: FAILURE, ERROR, OK
		int i= arg.indexOf(' ');
		int c= arg.indexOf(' ', i+1);
		int t; // special treatment, since testName can contain spaces:
		if (arg.endsWith(RemoteTestRunner.RERAN_ERROR)) {
			t= arg.length() - RemoteTestRunner.RERAN_ERROR.length() - 1;
		} else if (arg.endsWith(RemoteTestRunner.RERAN_FAILURE)) {
			t= arg.length() - RemoteTestRunner.RERAN_FAILURE.length() - 1;
		} else if (arg.endsWith(RemoteTestRunner.RERAN_OK)) {
			t= arg.length() - RemoteTestRunner.RERAN_OK.length() - 1;
		} else {
			t= arg.indexOf(' ', c+1);
		}
		String testId= arg.substring(0, i);
		String className= arg.substring(i+1, c);
		String testName= arg.substring(c+1, t);
		String status= arg.substring(t+1);
		notifyTestReran(testId, className, testName, status);
	}

	private void scanTestTiming(String arg) {
		String[] timing= arg.split(",", -1); //$NON-NLS-1$
		if (timing.length != 5 || timing[0].isEmpty())
			return;
		try {
			long startTimeNanos= Long.parseLong(timing[1]);
			long elapsedTimeNanos= Long.parseLong(timing[2]);
			long cpuTimeNanos= Long.parseLong(timing[3]);
			long userTimeNanos= Long.parseLong(timing[4]);
			if (startTimeNanos < 0 || elapsedTimeNanos < 0 || cpuTimeNanos < -1 || userTimeNanos < -1)
				return;
			// The model derives suite intervals from start + elapsed; the end must not overflow.
			if (startTimeNanos > Long.MAX_VALUE - elapsedTimeNanos)
				return;
			notifyTestTiming(timing[0], startTimeNanos, elapsedTimeNanos, cpuTimeNanos, userTimeNanos);
		} catch (NumberFormatException e) {
			// Ignore malformed optional timing data. Test status/result messages remain authoritative.
		}
	}

	private void notifyTestReran(String testId, String className, String testName, String status) {
		int statusCode= ITestRunListener2.STATUS_OK;
		if ("FAILURE".equals(status)) //$NON-NLS-1$
			statusCode= ITestRunListener2.STATUS_FAILURE;
		else if ("ERROR".equals(status)) //$NON-NLS-1$
			statusCode= ITestRunListener2.STATUS_ERROR;

		String trace= ""; //$NON-NLS-1$
		if (statusCode != ITestRunListener2.STATUS_OK)
			trace = fFailedRerunTrace.toString();
		// assumption a rerun trace was sent before
		notifyTestReran(testId, className, testName, statusCode, trace);
	}

	private void extractFailure(String arg, int status) {
		String s[]= extractTestId(arg);
		fFailedTestId= s[0];
		fFailedTest= s[1];
		fFailureKind= status;
	}

	/**
	 * @param arg test name
	 * @return an array with two elements. The first one is the testId, the second one the testName.
	 */
	String[] extractTestId(String arg) {
		String[] result= new String[2];
		if (!hasTestId()) {
			result[0]= arg; // use the test name as the test Id
			result[1]= arg;
			return result;
		}
		int i= arg.indexOf(',');
		result[0]= arg.substring(0, i);
		result[1]= arg.substring(i+1, arg.length());
		return result;
	}

	private boolean hasTestId() {
		if (fVersion == null) // TODO fix me
			return true;
		return "v2".equals(fVersion); //$NON-NLS-1$
	}

	private void notifyTestReran(final String testId, final String className, final String testName, final int statusCode, final String trace) {
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testReran(testId,
						className, testName, statusCode, trace,
						nullifyEmpty(fExpectedResult), nullifyEmpty(fActualResult));
				}
			});
		}
	}

	private void notifyTestTiming(final String testId, final long startTimeNanos, final long elapsedTimeNanos, final long cpuTimeNanos, final long userTimeNanos) {
		if (JUnitCorePlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testTiming(testId, startTimeNanos, elapsedTimeNanos, cpuTimeNanos, userTimeNanos);
				}
			});
		}
	}

	private void notifyTestTreeEntry(final String treeEntry) {
		for (ITestRunListener2 listener : fListeners) {
			if (!hasTestId())
				listener.testTreeEntry(fakeTestId(treeEntry));
			else
				listener.testTreeEntry(treeEntry);
		}
	}

	private String fakeTestId(String treeEntry) {
		// extract the test name and add it as the testId
		int index0= treeEntry.indexOf(',');
		String testName= treeEntry.substring(0, index0).trim();
		return testName+","+treeEntry; //$NON-NLS-1$
	}

	private void notifyTestRunStopped(final long elapsedTime) {
		if (JUnitCorePlugin.isStopped())
			return;
		// the test run that is stopped may follow other ones, each one in its VM
		final long totalElapsedTime= fPreviousElapsedTime + elapsedTime;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunStopped(totalElapsedTime);
				}
			});
		}
		stopped = false;
	}

	private void testRunEnded(final long elapsedTime) {
		if (fRemainingTestRuns > 1) {
			// the test run continues in the next VM
			fPreviousElapsedTime+= elapsedTime;
			fCurrentTestRunEnded= true;
			return;
		}
		ended = true;
		if (JUnitCorePlugin.isStopped())
			return;
		final long totalElapsedTime= fPreviousElapsedTime + elapsedTime;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunEnded(totalElapsedTime);
				}
			});
		}
	}

	private void notifyTestEnded(final String test) {
		if (JUnitCorePlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					String s[]= extractTestId(test);
					listener.testEnded(s[0], s[1]);
				}
			});
		}
	}

	private void notifyTestStarted(final String test) {
		if (JUnitCorePlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					String s[]= extractTestId(test);
					listener.testStarted(s[0], s[1]);
				}
			});
		}
	}

	private void notifyTestRunStarted(final int count) {
		if (JUnitCorePlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunStarted(count);
				}
			});
		}
	}

	private void notifyTestFailed() {
		if (JUnitCorePlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testFailed(fFailureKind, fFailedTestId,
						fFailedTest, fFailedTrace.toString(), nullifyEmpty(fExpectedResult), nullifyEmpty(fActualResult));
				}
			});
		}
	}

	/**
	 * Returns a comparison result from the given buffer.
	 * Removes the terminating line delimiter.
	 *
	 * @param buf the comparison result
	 * @return the result or <code>null</code> if empty
	 * @since 3.7
	 */
	private static String nullifyEmpty(StringBuffer buf) {
		int length= buf.length();
		if (length == 0)
			return null;

		char last= buf.charAt(length - 1);
		if (last == '\n') {
			if (length > 1 && buf.charAt(length - 2) == '\r')
				return buf.substring(0, length - 2);
			else
				return buf.substring(0, length - 1);
		} else if (last == '\r') {
			return buf.substring(0, length - 1);
		}
		return buf.toString();
	}

	private void notifyTestRunTerminated() {
		// fix for 77771 RemoteTestRunnerClient doing work after junit shutdown [JUnit]
		if (JUnitCorePlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunTerminated();
				}
			});
		}
	}

	/**
	 * Tells the listeners that the VM of a test run exited without ending it, while other test runs
	 * are expected.
	 */
	private void notifyTestRunInterrupted() {
		if (JUnitCorePlugin.isStopped())
			return;
		for (ITestRunListener2 listener : fListeners) {
			SafeRunner.run(new ListenerSafeRunnable() {
				@Override
				public void run() {
					listener.testRunInterrupted();
				}
			});
		}
	}

	public void rerunTest(String testId, String className, String testName) {
		if (isRunning()) {
			fActualResult.setLength(0);
			fExpectedResult.setLength(0);
			fWriter.println(MessageIds.TEST_RERUN+testId+" "+className+" "+testName); //$NON-NLS-1$ //$NON-NLS-2$
			fWriter.flush();
		}
	}
}
