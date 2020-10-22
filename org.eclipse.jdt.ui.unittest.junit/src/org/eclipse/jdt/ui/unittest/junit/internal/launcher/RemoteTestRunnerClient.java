/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.unittest.junit.internal.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestRunSession;

import org.eclipse.core.runtime.ISafeRunnable;

import org.eclipse.jdt.ui.unittest.junit.JUnitTestPlugin;

/**
 * The client side of the RemoteTestRunner. Handles the marshaling of the
 * different messages.
 */
public abstract class RemoteTestRunnerClient implements ITestRunnerClient {

	public abstract class ListenerSafeRunnable implements ISafeRunnable {
		@Override
		public void handleException(Throwable exception) {
			JUnitTestPlugin.log(exception);
		}
	}

	/**
	 * A simple state machine to process requests from the RemoteTestRunner
	 */
	abstract class ProcessingState {
		abstract ProcessingState readMessage(String message);
	}

	private int fPort = -1;
	protected String fLastLineDelimiter;
	protected InputStream fInputStream;
	protected PrintWriter fWriter;
	protected PushbackReader fPushbackReader;

	/**
	 * The protocol version
	 */
	protected String fVersion;

	protected boolean fDebug = false;
	protected final ITestRunSession fTestRunSession;

	/**
	 * Reads the message stream from the RemoteTestRunner
	 */
	private class ServerConnection extends Thread {
		int fServerPort;

		public ServerConnection(int port) {
			super("ServerConnection"); //$NON-NLS-1$
			fServerPort = port;
		}

		@Override
		public void run() {
			try {
				if (fDebug)
					System.out.println("Creating server socket " + fServerPort); //$NON-NLS-1$
				fServerSocket = new ServerSocket(fServerPort);
				fSocket = fServerSocket.accept();
				fPushbackReader = new PushbackReader(
						new BufferedReader(new InputStreamReader(fSocket.getInputStream(), StandardCharsets.UTF_8)));
				fWriter = new PrintWriter(new OutputStreamWriter(fSocket.getOutputStream(), StandardCharsets.UTF_8),
						true);
				String message;
				while (fPushbackReader != null && (message = readMessage(fPushbackReader)) != null)
					receiveMessage(message);
			} catch (SocketException e) {
				fTestRunSession.notifyTestSessionAborted(null, e);
			} catch (IOException e) {
				JUnitTestPlugin.log(e);
				// fall through
			}
			shutDown();
		}
	}

	protected RemoteTestRunnerClient(int port, ITestRunSession testRunSession) {
		this.fPort = port;
		fTestRunSession = testRunSession;
	}

	@Override
	public void startMonitoring() {
		ServerConnection connection = new ServerConnection(fPort);
		connection.start();
	}

	public abstract void receiveMessage(String message);

	public synchronized void shutDown() {
		if (fDebug)
			System.out.println("shutdown " + fPort); //$NON-NLS-1$

		if (fWriter != null) {
			fWriter.close();
			fWriter = null;
		}
		try {
			if (fPushbackReader != null) {
				fPushbackReader.close();
				fPushbackReader = null;
			}
		} catch (IOException e) {
			// Ignore
		}
		if (fDebug)
			System.out.println("shutdown"); //$NON-NLS-1$

		try {
			if (fSocket != null) {
				fSocket.close();
				fSocket = null;
			}
		} catch (IOException e) {
			// Ignore
		}
		try {
			if (fServerSocket != null) {
				fServerSocket.close();
				fServerSocket = null;
			}
		} catch (IOException e) {
			// Ignore
		}
	}

	private String readMessage(PushbackReader in) throws IOException {
		StringBuilder buf = new StringBuilder(128);
		int ch;
		while ((ch = in.read()) != -1) {
			switch (ch) {
			case '\n':
				fLastLineDelimiter = "\n"; //$NON-NLS-1$
				return buf.toString();
			case '\r':
				ch = in.read();
				if (ch == '\n') {
					fLastLineDelimiter = "\r\n"; //$NON-NLS-1$
				} else {
					in.unread(ch);
					fLastLineDelimiter = "\r"; //$NON-NLS-1$
				}
				return buf.toString();
			default:
				buf.append((char) ch);
				break;
			}
		}
		fLastLineDelimiter = null;
		if (buf.length() == 0)
			return null;
		return buf.toString();
	}

	/**
	 * The server socket
	 */
	protected ServerSocket fServerSocket;
	protected Socket fSocket;

	@Override
	public synchronized void stopMonitoring() {
		if (fServerSocket != null && !fServerSocket.isClosed() && fSocket == null) {
			shutDown(); // will throw a SocketException in Threads that wait in ServerSocket#accept()
		}
	}

}
