/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Julien Ruaux: jruaux@octo.com
 * 	   Vincent Massol: vmassol@octo.com
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
	 * A simple state machine to process requests from the RemoteTestRunner
	 */
	abstract class ProcessingState {
	    abstract ProcessingState readMessage(String message);
	}
	
	class DefaultProcessingState extends ProcessingState {
	    ProcessingState readMessage(String message) {
	        if (message.startsWith(MessageIds.TRACE_START)) {
	            fFailedTrace= ""; //$NON-NLS-1$
	            return fTraceState;
	        }
	        if (message.startsWith(MessageIds.EXPECTED_START)) {
	            fExpectedResult= null;
	            return fExpectedState;
	        }
	        if (message.startsWith(MessageIds.ACTUAL_START)) {
	            fActualResult= null;
	            return fActualState;
	        }
	        if (message.startsWith(MessageIds.RTRACE_START)) {
	            fFailedRerunTrace= ""; //$NON-NLS-1$
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
	        if (message.startsWith(MessageIds.TEST_END)) {
	            notifyTestEnded(arg);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_ERROR)) {
	            extractFailure(arg, ITestRunListener.STATUS_ERROR);
	            return this;
	        }
	        if (message.startsWith(MessageIds.TEST_FAILED)) {
	            extractFailure(arg, ITestRunListener.STATUS_FAILURE);
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
	
	class TraceProcessingState extends ProcessingState {
	    ProcessingState readMessage(String message) {
	        if (message.startsWith(MessageIds.TRACE_END)) {
	            notifyTestFailed();
	            fFailedTrace = ""; //$NON-NLS-1$
	            fExpectedResult= null;
	            fActualResult = null;
	            return fDefaultState;
	        }
	        fFailedTrace+= message + '\n';
	        return this;
	    }
	}
	class ExpectedProcessingState extends ProcessingState {
	    ProcessingState readMessage(String message) {
	        if (message.startsWith(MessageIds.EXPECTED_END)) 
	            return fDefaultState;
	        if (fExpectedResult == null)
	        	fExpectedResult= message + '\n';
	        else
	        	fExpectedResult+= message + '\n';
	        return this;
	    }
	}
	class ActualProcessingState extends ProcessingState {
	    ProcessingState readMessage(String message) {
	        if (message.startsWith(MessageIds.ACTUAL_END)) 
	            return fDefaultState;
	        if (fActualResult == null)
	        	fActualResult= message + '\n';
	        else 
				fActualResult+= message + '\n';
	        return this;
	    }
	}
	class RerunTraceProcessingState extends ProcessingState {
	    ProcessingState readMessage(String message) {
	        if (message.startsWith(MessageIds.RTRACE_END)) 
	            return fDefaultState;
	        fFailedRerunTrace+= message + '\n';
	        return this;
	    }
	}
	ProcessingState fDefaultState= new DefaultProcessingState();
	ProcessingState fTraceState= new TraceProcessingState();
	ProcessingState fExpectedState= new ExpectedProcessingState();
	ProcessingState fActualState= new ActualProcessingState();
	ProcessingState fRerunState= new RerunTraceProcessingState();
	ProcessingState fCurrentState= fDefaultState;
	
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
	 * The failed trace that is currently reported from the RemoteTestRunner
	 */
	private String fFailedTrace;
	/**
	 * The expected test result
	 */
	private String fExpectedResult;
	/**
	 * The actual test result
	 */
	private String fActualResult;
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
		try {
			if (fSocket != null) {
				fSocket.close();
				fSocket= null;
			}
		} catch(IOException e) {
		}
		try{
			if (fServerSocket != null) {
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
		int statusCode= ITestRunListener.STATUS_OK;
		if (status.equals("FAILURE")) //$NON-NLS-1$
			statusCode= ITestRunListener.STATUS_FAILURE;
		else if (status.equals("ERROR")) //$NON-NLS-1$
			statusCode= ITestRunListener.STATUS_ERROR;
				
		String trace= ""; //$NON-NLS-1$
		if (statusCode != ITestRunListener.STATUS_OK)
			trace = fFailedRerunTrace;
		// assumption a rerun trace was sent before
		notifyTestReran(className+testName, className, testName, statusCode, trace);
	}

	private void scanReranMessage(String arg) {
		// format: testId" "className" "testName" "status
		// status: FAILURE, ERROR, OK
		int i= arg.indexOf(' ');
		int c= arg.indexOf(' ', i+1); //$NON-NLS-1$
		int t= arg.indexOf(' ', c+1); //$NON-NLS-1$
		String testId= arg.substring(0, i);
		String className= arg.substring(i+1, c);
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
		notifyTestReran(testId, className, testName, statusCode, trace);
	}

	private void extractFailure(String arg, int status) {
		String s[]= extractTestId(arg);
		fFailedTestId= s[0];
		fFailedTest= s[1];
		fFailureKind= status;
	}

	/**
	 * Returns an array with two elements. The first one is the testId, the second one the testName.
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
		return fVersion.equals("v2"); //$NON-NLS-1$
	}

	private void notifyTestReran(final String testId, final String className, final String testName, final int statusCode, final String trace) {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					listener.testReran(testId, className, testName, statusCode, trace);
				}
			});
		}
	}

	private void notifyTestTreeEntry(final String treeEntry) {
		for (int i= 0; i < fListeners.length; i++) {
			if (fListeners[i] instanceof ITestRunListener2) {
				ITestRunListener2 listener= (ITestRunListener2)fListeners[i];
				if (!hasTestId()) 
					listener.testTreeEntry(fakeTestId(treeEntry));
				else
					listener.testTreeEntry(treeEntry);
			}
		}
	}

	private String fakeTestId(String treeEntry) {
		// extract the test name and add it as the testId
		int index0= treeEntry.indexOf(',');
		String testName= treeEntry.substring(0, index0).trim();
		return testName+","+treeEntry; //$NON-NLS-1$
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
					String s[]= extractTestId(test);
					listener.testEnded(s[0], s[1]);
				}
			});
		}
	}

	private void notifyTestStarted(final String test) {
		for (int i= 0; i < fListeners.length; i++) {
			final ITestRunListener listener= fListeners[i];
			Platform.run(new ListenerSafeRunnable() { 
				public void run() {
					String s[]= extractTestId(test);
					listener.testStarted(s[0], s[1]);
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
				    if (listener instanceof ITestRunListener3 )
				        ((ITestRunListener3)listener).testFailed(fFailureKind, fFailedTestId, 
				                fFailedTest, fFailedTrace, fExpectedResult, fActualResult);
				    else
				        listener.testFailed(fFailureKind, fFailedTestId, fFailedTest, fFailedTrace);
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

	public void rerunTest(String testId, String className, String testName) {
		if (isRunning()) {
			fWriter.println(MessageIds.TEST_RERUN+testId+" "+className+" "+testName); //$NON-NLS-1$ //$NON-NLS-2$
			fWriter.flush();
		}
	}
}