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
import java.util.Vector;

import org.eclipse.jdt.internal.junit.runner.MessageIds;

/**
 * The client side of the RemoteTestRunner. 
 */
public class RemoteTestRunnerClient {
	protected ITestRunListener fListener;
	
	protected ServerSocket fServerSocket;
	protected Socket fSocket;
	protected PrintWriter fWriter;
	protected BufferedReader fBufferedReader;

	// communication states with remote test runner
	protected boolean fInReadTrace= false;
	protected boolean fInFailedMessage= false;
	
	protected String fFailedTest;
	protected String fFailedMessage;
	protected String fFailedTrace;
	protected int fFailureKind;
	protected long fElapsedTime;

	private class ServerConnection extends Thread {
		int fPort;
		
		public ServerConnection(int port) {
			super("ServerConnection");
			fPort= port;
		}
		
		public void run() {
			try {	 
				fServerSocket= new ServerSocket(fPort);
				fSocket= fServerSocket.accept();				
				fBufferedReader= new BufferedReader(new InputStreamReader(fSocket.getInputStream()));
				fWriter= new PrintWriter(fSocket.getOutputStream(), true);
				String line;
				while((line= readMessage(fBufferedReader)) != null)
					receiveMessage(line);
			} catch (IOException e) {
				stopTest();
				shutDown();
			}
			stopTest();
			shutDown();
		}
	}

	protected Vector fListeners= new Vector();

	public void addTestRunListener(ITestRunListener listener) {
		fListeners.add(listener);
	}
	
	public void removeTestRunListener(ITestRunListener listener) {
		fListeners.remove(listener);
	}
	
	public void startListening(ITestRunListener listener, int port) {
		fListener= listener;
		ServerConnection connection= new ServerConnection(port);
		connection.start();		
	}
	
	public void stopTest() {
		if (isRunning()) {
			fWriter.println(MessageIds.TEST_STOP);
			fWriter.flush();
		}
	}

	public void shutDown() {
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
	
	protected String readMessage(BufferedReader in) throws IOException {
		String line= null;
		if (in != null)
			line= in.readLine();
		return line;
	}
		
	protected void receiveMessage(String message) {
		if(message == null)
			return;	
		if (message.startsWith(MessageIds.TEST_TREE_START)) {
			fListener.testTreeStart();
			return;
		}
		if (message.startsWith(MessageIds.TRACE_START)) {
			fInReadTrace= true;
			fFailedTrace= "";
			return;
		}
		if (message.startsWith(MessageIds.TRACE_END)) {
			fInReadTrace= false;
			fListener.testFailed(fFailureKind, fFailedTest, fFailedTrace);
			return;
		}
		if (fInReadTrace) {
			fFailedTrace+= message + '\n';
			return;
		}
		
		String arg= message.substring(MessageIds.MSG_HEADER_LENGTH);
		if (message.startsWith(MessageIds.TEST_COUNT)) {
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
		if (message.startsWith(MessageIds.TEST_ELAPSED_TIME)) {
			fElapsedTime= Long.parseLong(arg);
			fListener.testRunEnded(fElapsedTime);
			return;
		}
		if (message.startsWith(MessageIds.TEST_STOPPED)) {
			fElapsedTime= Long.parseLong(arg);
			fListener.testRunStopped(fElapsedTime);
			return;
		}
		if (message.startsWith(MessageIds.TEST_TREE)) {
			fListener.testTreeEntry(arg);
			return;
		}
	}
}