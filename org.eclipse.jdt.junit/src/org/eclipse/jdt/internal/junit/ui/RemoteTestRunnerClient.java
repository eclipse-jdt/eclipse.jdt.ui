/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *   Julien Ruaux: jruaux@octo.com
 * 	 Vincent Massol: vmassol@octo.com
 ******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.junit.ITestRunListener;

/**
 * The client side of the RemoteTestRunner. Handles the
 * marshalling of th different messages.
 */
public class RemoteTestRunnerClient {
	public abstract class ListenerSafeRunnable implements ISafeRunnable {
		public void handleException(Throwable exception) {
			JUnitPlugin.log(exception);
		}
	}
	
	/**
	 * An array of listeners that are informed about test events.
	 */
	private ITestRunListener[] fListeners;

	/**
	 * The server socket
	 */
	private ServerSocket fServerSocket;
	private Socket fSocket;
	private int fPort= -1;
	private PrintWriter fWriter;
	private BufferedReader fBufferedReader;

	/**
	 * RemoteTestRunner is sending trace. 
	 */
	private boolean fInReadTrace= false;
	/**
	 * RemoteTestRunner is sending the rerun trace. 
	 */
	private boolean fInReadRerunTrace= false;
	/**
	 * The currently received failed message
	 */
	private boolean fInFailedMessage= false;
	/**
	 * The failed test that is currently reported from the RemoteTestRunner
	 */
	private String fFailedTest;
	/**
	 * The failed message that is currently reported from the RemoteTestRunner
	 */
	private String fFailedMessage;
	/**
	 * The failed trace that is currently reported from the RemoteTestRunner
	 */
	private String fFailedTrace;
	/**
	 * The failed trace of a reran test
	 */
	private String fFailedRerunTrace;
	/**
	 * The kind of failure of the test that is currently reported as failed
	 */
	private int fFailureKind;
	
	private boolean fDebug= false;
	
	/**
	 * Reads the message stream from the RemoteTestRunner
	 */
	private class ServerConnection extends Thread {
		int fPort;
		
		public ServerConnection(int port) {
			super("ServerConnection"); //$NON-NLS-1$
			fPort= port;
		}
		
		public void run() {
			try {
				if (fDebug)
					System.out.println("Creating server socket "+fPort); //$NON-NLS-1$
				fServerSocket= new ServerSocket(fPort);
				fSocket= fServerSocket.accept();				
				fBufferedReader= new BufferedReader(new InputStreamReader(fSocket.getInputStream()));
				fWriter= new PrintWriter(fSocket.getOutputStream(), true);
				String message;
				while(fBufferedReader != null && (message= readMessage(fBufferedReader)) != null)
					receiveMessage(message);
			} catch (SocketException e) {
				notifyTestRunTerminated();
			} catch (IOException e) {
				System.out.println(e);
				// fall through
			}
			shutDown();
		}
	}

	/**
	 * Start listening to a test run. Start a server connection that
	 * the RemoteTestRunner can connect to.
	 */
	public synchronized void startListening(
		ITestRunListener[] listeners,
		int port) {
		fListeners = listeners;
		fPort = port;
		ServerConnection connection = new ServerConnection(port);
		connection.start();
	}
	
	/**
	 * Requests to stop the remote test run.
	 */
	public synchronized void stopTest() {
		if (isRunning()) {
			fWriter.println(MessageIds.TEST_STOP);
			fWriter.flush();
		}
	}

	/**
	 * Requests to rerun a test
	 */
	public synchronized void rerunTest(String className, String testName) {
		if (isRunning()) {
			fWriter.println(MessageIds.TEST_RERUN+className+" "+testName); //$NON-NLS-1$
			fWriter.flush();
		}
	}

	private synchronized void shutDown() {
		if (fDebug) 
			System.out.println("shutdown "+fPort); //$NON-NLS-1$
		
		if (fWriter != null) {
			fWriter.close();
			fWriter= null;
		}
		try {
			if (fBufferedReader != null) {
				fBufferedReader.close();
				fBufferedReader= null;
			}
		} catch(IOException e) {
		}	
		try{
			if(fSocket != null) {
				fSocket.close();
				fSocket= null;
			}
		} catch(IOException e) {
		}
		try{
			if(fServerSocket != null) {
				fServerSocket.close();
				fServerSocket= null;
			}
		} catch(IOException e) {
		}
	}
	
	public boolean isRunning() {
		return fSocket != null;
	}
	
	private String readMessage(BufferedReader in) throws IOException {
		return in.readLine();
	}
		
	private void receiveMessage(String message) {
		if (message.startsWith(MessageIds.TRACE_START)) {
			fInReadTrace= true;
			fFailedTrace= ""; //$NON-NLS-1$
			return;
		}
		if (message.startsWith(MessageIds.TRACE_END)) {
			fInReadTrace = false;

			notifyTestFailed();

			fFailedTrace = ""; //$NON-NLS-1$
			return;
		}
		if (fInReadTrace) {
			fFailedTrace+= message + '\n';
			return;
		}
		
		if (message.startsWith(MessageIds.RTRACE_START)) {
			fInReadRerunTrace= true;
			fFailedRerunTrace= ""; //$NON-NLS-1$
			return;
		}
		if (message.startsWith(MessageIds.RTRACE_END)) {
			fInReadRerunTrace= false;
			return;
		}
		if (fInReadRerunTrace) {
			fFailedRerunTrace+= message + '\n';
			return;
		}

		String arg= message.substring(MessageIds.MSG_HEADER_LENGTH);
		if (message.startsWith(MessageIds.TEST_RUN_START)) {
			int count = Integer.parseInt(arg);
			notifyTestRunStarted(count);
			return;
		}
		if (message.startsWith(MessageIds.TEST_START)) {
			notifyTestStarted(arg);
			return;
		}
		if (message.startsWith(MessageIds.TEST_END)) {
			notifyTestEnded(arg);
			return;
		}
		if (message.startsWith(MessageIds.TEST_ERROR)) {
			fFailedTest= arg;
			fFailureKind= ITestRunListener.STATUS_ERROR;
			return;
		}
		if (message.startsWith(MessageIds.TEST_FAILED)) {
			fFailedTest= arg;
			fFailureKind= ITestRunListener.STATUS_FAILURE;
			return;
		}
		if (message.startsWith(MessageIds.TEST_RUN_END)) {
			long elapsedTime = Long.parseLong(arg);
			testRunEnded(elapsedTime);
			return;
		}
		if (message.startsWith(MessageIds.TEST_STOPPED)) {
			long elapsedTime = Long.parseLong(arg);
			notifyTestRunStopped(elapsedTime);

			shutDown();
			return;
		}
		if (message.startsWith(MessageIds.TEST_TREE)) {
			notifyTestTreeEntry(arg);
			return;
		}
		if (message.startsWith(MessageIds.TEST_RERAN)) {
			// format: className" "testName" "status
			// status: FAILURE, ERROR, OK
			int c= arg.indexOf(" "); //$NON-NLS-1$
			int t= arg.indexOf(" ", c+1); //$NON-NLS-1$
			String className= arg.substring(0, c);
			String testName= arg.substring(c+1, t);
			String status= arg.substring(t+1);
			int statusCode= ITestRunListener.STATUS_OK;
			if (status.equals("FAILURE")) //$NON-NLS-1$
				statusCode= ITestRunListener.STATUS_FAILURE;
			else if (status.equals("ERROR")) //$NON-NLS-1$
				statusCode= ITestRunListener.STATUS_ERROR;
				
			String trace= ""; //$NON-NLS-1$
			if (statusCode != ITestRunListener.STATUS_OK)
				trace = fFailedRerunTrace;
			// assumption a rerun trace was sent before
			notifyTestReran(className, testName, statusCode, trace);
		}
	}

	private void notifyTestReran(final String className, final String testName, final int statusCode, final String trace) {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					listener.testReran(className, testName, statusCode, trace);
				}
			});
		}
	}

	private void notifyTestTreeEntry(final String treeEntry) {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					listener.testTreeEntry(treeEntry);
				}
			});
		}
	}

	private void notifyTestRunStopped(final long elapsedTime) {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					listener.testRunStopped(elapsedTime);
				}
			});
		}
	}

	private void testRunEnded(final long elapsedTime) {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					listener.testRunEnded(elapsedTime);
				}
			});
		}
	}

	private void notifyTestEnded(final String test) {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					listener.testEnded(test);
				}
			});
		}
	}

	private void notifyTestStarted(final String test) {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					listener.testStarted(test);
				}
			});
		}
	}

	private void notifyTestRunStarted(final int count) {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					listener.testRunStarted(count);
				}
			});
		}
	}

	private void notifyTestFailed() {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					listener.testFailed(fFailureKind, fFailedTest, fFailedTrace);
				}
			});
		}
	}
	
	private void notifyTestRunTerminated() {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					listener.testRunTerminated();
				}
			});
		}
	}
}