/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.eclipse.jdt.internal.junit.runner.ITestRunListener;
import org.eclipse.jdt.internal.junit.runner.MessageIds;

/**
 * The client side of the RemoteTestRunner. Handles the
 * marshalling of th different messages.
 */
public class RemoteTestRunnerClient {
	/**
	 * A listener that is informed about test events.
	 */
	private ITestRunListener fListener;
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
			super("ServerConnection");
			fPort= port;
		}
		
		public void run() {
			try {
				if (fDebug)
					System.out.println("Creating server socket "+fPort);
				fServerSocket= new ServerSocket(fPort);
				fSocket= fServerSocket.accept();				
				fBufferedReader= new BufferedReader(new InputStreamReader(fSocket.getInputStream()));
				fWriter= new PrintWriter(fSocket.getOutputStream(), true);
				String message;
				while(fBufferedReader != null && (message= readMessage(fBufferedReader)) != null)
					receiveMessage(message);
			} catch (SocketException e) {
				fListener.testRunTerminated();
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
	public synchronized void startListening(ITestRunListener listener, int port) {
		fListener= listener;
		fPort= port;
		ServerConnection connection= new ServerConnection(port);
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
			fWriter.println(MessageIds.TEST_RERUN+className+" "+testName);
			fWriter.flush();
		}
	}

	private synchronized void shutDown() {
		if (fDebug) 
			System.out.println("shutdown "+fPort);
		
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
			fFailedTrace= "";
			return;
		}
		if (message.startsWith(MessageIds.TRACE_END)) {
			fInReadTrace= false;
			fListener.testFailed(fFailureKind, fFailedTest, fFailedTrace);
			fFailedTrace= "";
			return;
		}
		if (fInReadTrace) {
			fFailedTrace+= message + '\n';
			return;
		}
		
		if (message.startsWith(MessageIds.RTRACE_START)) {
			fInReadRerunTrace= true;
			fFailedRerunTrace= "";
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
			int count= Integer.parseInt(arg);
			fListener.testRunStarted(count);
			return;
		}
		if (message.startsWith(MessageIds.TEST_START)) {
			fListener.testStarted(arg);
			return;
		}
		if (message.startsWith(MessageIds.TEST_END)) {
			fListener.testEnded(arg);
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
			long elapsedTime= Long.parseLong(arg);
			fListener.testRunEnded(elapsedTime);
			return;
		}
		if (message.startsWith(MessageIds.TEST_STOPPED)) {
			long elapsedTime= Long.parseLong(arg);
			fListener.testRunStopped(elapsedTime);
			shutDown();
			return;
		}
		if (message.startsWith(MessageIds.TEST_TREE)) {
			fListener.testTreeEntry(arg);
			return;
		}
		if (message.startsWith(MessageIds.TEST_RERAN)) {
			// format: className" "testName" "status
			// status: FAILURE, ERROR, OK
			int c= arg.indexOf(" ");
			int t= arg.indexOf(" ", c+1);
			String className= arg.substring(0, c);
			String testName= arg.substring(c+1, t);
			String status= arg.substring(t+1);
			int statusCode= ITestRunListener.STATUS_OK;
			if (status.equals("FAILURE"))
				statusCode= ITestRunListener.STATUS_FAILURE;
			else if (status.equals("ERROR"))
				statusCode= ITestRunListener.STATUS_ERROR;
				
			String trace= "";
			if (statusCode != ITestRunListener.STATUS_OK)
				trace= fFailedRerunTrace; // assumption a rerun trace was sent before
			fListener.testReran(className, testName, statusCode, trace);
		}
	}
}