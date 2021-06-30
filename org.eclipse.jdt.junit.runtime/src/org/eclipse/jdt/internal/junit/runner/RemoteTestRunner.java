/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Sebastian Davids: sdavids@gmx.de bug 26754
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.runner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Vector;

import org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestLoader;

/**
 * A TestRunner that reports results via a socket connection.
 * See MessageIds for more information about the protocol.
 */
public class RemoteTestRunner implements MessageSender, IVisitsTestTrees {
	/**
	 * Holder for information for a rerun request
	 */
	private static class RerunRequest {
		String fRerunClassName;
		String fRerunTestName;
		int fRerunTestId;

		public RerunRequest(int testId, String className, String testName) {
			fRerunTestId= testId;
			fRerunClassName= className;
			fRerunTestName= testName;
		}

	}

	public static final String RERAN_FAILURE = "FAILURE"; //$NON-NLS-1$

	public static final String RERAN_ERROR = "ERROR"; //$NON-NLS-1$

	public static final String RERAN_OK = "OK"; //$NON-NLS-1$

	/**
	 * The name of the test classes to be executed
	 */
	private String[] fTestClassNames;
	/**
	 * The name of the test (argument -test)
	 */
	private String fTestName;
	/**
	 * The names of the packages containing tests to run
	 */
	private String[] fPackageNames;
	/**
	 * The unique ID of test to run or "" if not available
	 */
	private String fUniqueId;
	/**
	 * Tags to be included and excluded in the test run
	 */
	private String[][] fIncludeExcludeTags= new String[2][];

	/**
	 * The current test result
	 */
	private TestExecution fExecution;

	/**
	 * The client socket.
	 */
	private Socket fClientSocket;
	/**
	 * Print writer for sending messages
	 */
	private PrintWriter fWriter;
	/**
	 * Reader for incoming messages
	 */
	private BufferedReader fReader;
	/**
	 * Host to connect to, default is the localhost
	 */
	private String fHost= ""; //$NON-NLS-1$
	/**
	 * Port to connect to.
	 */
	private int fPort= -1;
	/**
	 * Is the debug mode enabled?
	 */
	private boolean fDebugMode= false;
	/**
	 * Keep the test run server alive after a test run has finished.
	 * This allows to rerun tests.
	 */
	private boolean fKeepAlive= false;
	/**
	 * Has the server been stopped
	 */
	private boolean fStopped= false;
	/**
	 * Queue of rerun requests.
	 */
	private Vector<RerunRequest> fRerunRequests= new Vector<>(10);
	/**
	 * Thread reading from the socket
	 */
	private ReaderThread fReaderThread;

	private String fRerunTest;

	private final TestIdMap fIds = new TestIdMap();

	private String[] fFailureNames;

	private ITestLoader fLoader;

	private MessageSender fSender;

	private boolean fConsoleMode = false;

	/**
	 * Reader thread that processes messages from the client.
	 */
	private class ReaderThread extends Thread {
		public ReaderThread() {
			super("ReaderThread"); //$NON-NLS-1$
		}

		@Override
		public void run(){
			try {
				String message= null;
				while (true) {
					if ((message= fReader.readLine()) != null) {

						if (message.startsWith(MessageIds.TEST_STOP)){
							fStopped= true;
							RemoteTestRunner.this.stop();
							synchronized(RemoteTestRunner.this) {
								RemoteTestRunner.this.notifyAll();
							}
							break;
						}

						else if (message.startsWith(MessageIds.TEST_RERUN)) {
							String arg= message.substring(MessageIds.MSG_HEADER_LENGTH);
							//format: testId className testName
							int c0= arg.indexOf(' ');
							int c1= arg.indexOf(' ', c0+1);
							String s= arg.substring(0, c0);
							int testId= Integer.parseInt(s);
							String className= arg.substring(c0+1, c1);
							String testName= arg.substring(c1+1, arg.length());
							synchronized(RemoteTestRunner.this) {
								fRerunRequests.add(new RerunRequest(testId, className, testName));
								RemoteTestRunner.this.notifyAll();
							}
						}
					}
				}
			} catch (Exception e) {
				RemoteTestRunner.this.stop();
			}
		}
	}

	public RemoteTestRunner() {
		setMessageSender(this);
	}

	public void setMessageSender(MessageSender sender) {
		fSender = sender;
	}

	/**
	 * The main entry point.
	 *
	 * @param args Parameters:
	 * <pre>-classnames: the name of the test suite class
	 * -testfilename: the name of a file containing classnames of test suites
	 * -test: the test method name (format classname testname)
	 * -packagenamefile: the name of a file containing package names of tests
	 * -host: the host to connect to default local host
	 * -port: the port to connect to, mandatory argument
	 * -keepalive: keep the process alive after a test run
     * </pre>
     */
	public static void main(String[] args) {
		try {
			RemoteTestRunner testRunServer= new RemoteTestRunner();
			testRunServer.init(args);
			testRunServer.run();
		} catch (Throwable e) {
			e.printStackTrace(); // don't allow System.exit(0) to swallow exceptions
		} finally {
			// fix for 14434
			System.exit(0);
		}
	}

	/**
	 * Parse command line arguments. Hook for subclasses to process
	 * additional arguments.
	 * @param args the arguments
	 */
	protected void init(String[] args) {
		defaultInit(args);
	}

	/**
	 * The class loader to be used for loading tests.
	 * Subclasses may override to use another class loader.
	 * @return the class loader to lead test classes
	 */
	protected ClassLoader getTestClassLoader() {
		return getClass().getClassLoader();
	}

	/**
	 * Process the default arguments.
	 * @param args arguments
	 */
	protected final void defaultInit(String[] args) {
		for(int i= 0; i < args.length; i++) {
			if("-classnames".equals(args[i].toLowerCase()) || "-classname".equals(args[i].toLowerCase())){ //$NON-NLS-1$ //$NON-NLS-2$
				Vector<String> list= new Vector<>();
				for (int j= i+1; j < args.length; j++) {
					if (args[j].startsWith("-")) //$NON-NLS-1$
						break;
					list.add(args[j]);
				}
				fTestClassNames= list.toArray(new String[list.size()]);
			}
			else if("-test".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				String testName= args[i+1];
				int p= testName.indexOf(':');
				if (p == -1)
					throw new IllegalArgumentException("Testname not separated by \'%\'"); //$NON-NLS-1$
				fTestName= testName.substring(p+1);
				fTestClassNames= new String[]{ testName.substring(0, p)  };
				i++;
			}
			else if("-testnamefile".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				String testNameFile= args[i+1];
				try {
					readTestNames(testNameFile);
				} catch (IOException e) {
					throw new IllegalArgumentException("Cannot read testname file.");		 //$NON-NLS-1$
				}
				i++;

			} else if ("-packagenamefile".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				String pkgNameFile= args[i+1];
				try {
					readPackageNames(pkgNameFile);
				} catch (IOException e) {
					throw new IllegalArgumentException("Cannot read packagename file.");		 //$NON-NLS-1$
				}
				i++;

			} else if ("-testfailures".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				String testFailuresFile= args[i+1];
				try {
					readFailureNames(testFailuresFile);
				} catch (IOException e) {
					throw new IllegalArgumentException("Cannot read testfailures file.");		 //$NON-NLS-1$
				}
				i++;

			} else if("-port".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				fPort= Integer.parseInt(args[i+1]);
				i++;
			}
			else if("-host".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				fHost= args[i+1];
				i++;
			}
			else if("-rerun".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				fRerunTest= args[i+1];
				i++;
			}
			else if("-keepalive".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				fKeepAlive= true;
			}
			else if("-debugging".equals(args[i].toLowerCase()) || "-debug".equals(args[i].toLowerCase())){ //$NON-NLS-1$ //$NON-NLS-2$
			    fDebugMode= true;

			} else if ("-junitconsole".equals(args[i].toLowerCase())) { //$NON-NLS-1$
			    fConsoleMode  = true;
			} else if ("-testloaderclass".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				String className = args[i + 1];
				createLoader(className);
				i++;
			} else if("-uniqueid".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				fUniqueId= args[i+1];
				i++;
			} else if ("--include-tag".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				String[] includeTags= fIncludeExcludeTags[0];
				if (includeTags == null) {
					includeTags= new String[1];
					includeTags[0]= args[i + 1];
				} else {
					String[] tags= new String[includeTags.length + 1];
					System.arraycopy(includeTags, 0, tags, 0, includeTags.length);
					tags[includeTags.length]= args[i + 1];
					includeTags= tags;
				}
				fIncludeExcludeTags[0]= includeTags;
				i++;
			} else if ("--exclude-tag".equals(args[i].toLowerCase())) { //$NON-NLS-1$
				String[] excludeTags= fIncludeExcludeTags[1];
				if (excludeTags == null) {
					excludeTags= new String[1];
					excludeTags[0]= args[i + 1];
				} else {
					String[] tags= new String[excludeTags.length + 1];
					System.arraycopy(excludeTags, 0, tags, 0, excludeTags.length);
					tags[excludeTags.length]= args[i + 1];
					excludeTags= tags;
				}
				fIncludeExcludeTags[1]= excludeTags;
				i++;
			}
		}

		if (getTestLoader() == null)
			initDefaultLoader();

		if(fTestClassNames == null || fTestClassNames.length == 0) {
			if (fPackageNames == null || fPackageNames.length == 0) {
				throw new IllegalArgumentException(JUnitMessages.getString("RemoteTestRunner.error.classnamemissing")); //$NON-NLS-1$
			} else {
				fTestClassNames= new String[0];
			}
		}

		if (fPort == -1)
			throw new IllegalArgumentException(JUnitMessages.getString("RemoteTestRunner.error.portmissing")); //$NON-NLS-1$
		if (fDebugMode)
			System.out.println("keepalive "+fKeepAlive); //$NON-NLS-1$
	}

	public void initDefaultLoader() {
		createLoader(JUnit3TestLoader.class.getName());
	}

	public void createLoader(String className) {
		setLoader(createRawTestLoader(className));
	}

	protected ITestLoader createRawTestLoader(String className) {
		try {
			return (ITestLoader) loadTestLoaderClass(className).newInstance();
		} catch (Exception e) {
			StringWriter trace= new StringWriter();
			e.printStackTrace(new PrintWriter(trace));
			String message= JUnitMessages.getFormattedString("RemoteTestRunner.error.invalidloader", new Object[] {className, trace.toString()}); //$NON-NLS-1$
			throw new IllegalArgumentException(message);
		}
	}

	protected Class<?> loadTestLoaderClass(String className) throws ClassNotFoundException {
		return Class.forName(className);
	}

	public void setLoader(ITestLoader newInstance) {
		fLoader = newInstance;
	}

	private void readPackageNames(String pkgNameFile) throws IOException {
		try(BufferedReader br= new BufferedReader(new InputStreamReader(new FileInputStream(new File(pkgNameFile)), "UTF-8"))) { //$NON-NLS-1$
			String line;
			Vector<String> list= new Vector<>();
			while ((line= br.readLine()) != null) {
				list.add(line);
			}
			fPackageNames= list.toArray(new String[list.size()]);
		}
		if (fDebugMode) {
			System.out.println("Packages:"); //$NON-NLS-1$
			for (String fPackageName : fPackageNames) {
				System.out.println("    "+fPackageName); //$NON-NLS-1$
			}
		}
	}

	private void readTestNames(String testNameFile) throws IOException {
		try(BufferedReader br= new BufferedReader(new InputStreamReader(new FileInputStream(new File(testNameFile)), "UTF-8"))) { //$NON-NLS-1$
			String line;
			Vector<String> list= new Vector<>();
			while ((line= br.readLine()) != null) {
				list.add(line);
			}
			fTestClassNames= list.toArray(new String[list.size()]);
		}
		if (fDebugMode) {
			System.out.println("Tests:"); //$NON-NLS-1$
			for (String fTestClassName : fTestClassNames) {
				System.out.println("    "+fTestClassName); //$NON-NLS-1$
			}
		}
	}

	private void readFailureNames(String testFailureFile) throws IOException {
		try(BufferedReader br= new BufferedReader(new InputStreamReader(new FileInputStream(new File(testFailureFile)), "UTF-8"))) { //$NON-NLS-1$
			String line;
			Vector<String> list= new Vector<>();
			while ((line= br.readLine()) != null) {
				list.add(line);
			}
			fFailureNames= list.toArray(new String[list.size()]);
		}
		if (fDebugMode) {
			System.out.println("Failures:"); //$NON-NLS-1$
			for (String fFailureName : fFailureNames) {
				System.out.println("    "+fFailureName); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Connects to the remote ports and runs the tests.
	 */
	protected void run() {
		if (!connect())
			return;
		if (fRerunTest != null) {
			rerunTest(new RerunRequest(Integer.parseInt(fRerunTest), fTestClassNames[0], fTestName));
			return;
		}

		FirstRunExecutionListener listener= firstRunExecutionListener();
		fExecution= new TestExecution(listener, getClassifier());
		runTests(fExecution);
		if (fKeepAlive)
			waitForReruns();

		shutDown();

	}

	public FirstRunExecutionListener firstRunExecutionListener() {
		return new FirstRunExecutionListener(fSender, fIds);
	}

	/**
	 * Waits for rerun requests until an explicit stop request
	 */
	private synchronized void waitForReruns() {
		while (!fStopped) {
			try {
				wait();
				if (!fStopped && fRerunRequests.size() > 0) {
					RerunRequest r= fRerunRequests.remove(0);
					rerunTest(r);
				}
			} catch (InterruptedException e) {
			}
		}
	}

	public void runFailed(String message, Exception exception) {
		//TODO: remove System.err.println?
		System.err.println(message);
		if (exception != null)
			exception.printStackTrace(System.err);
	}

	protected Class<?>[] loadClasses(String[] testClassNames) {
		Vector<Class<?>> classes= new Vector<>();
		for (String name : testClassNames) {
			Class<?> clazz = loadClass(name, this);
			if (clazz != null) {
				classes.add(clazz);
			}
		}
		return classes.toArray(new Class[classes.size()]);
	}

	protected void notifyListenersOfTestEnd(TestExecution execution,
			long testStartTime) {
		if (execution == null || execution.shouldStop())
			notifyTestRunStopped(System.currentTimeMillis() - testStartTime);
		else
			notifyTestRunEnded(System.currentTimeMillis() - testStartTime);
	}

	/**
	 * Runs a set of tests.
	 * @param testClassNames classes to be run
	 * @param testName individual method to be run
	 * @param execution executor
	 */
	public void runTests(String[] testClassNames, String testName, TestExecution execution) {
		ITestReference[] suites= fLoader.loadTests(loadClasses(testClassNames), testName, fFailureNames, fPackageNames, fIncludeExcludeTags, fUniqueId, this);

		// count all testMethods and inform ITestRunListeners
		int count= countTests(suites);

		notifyTestRunStarted(count);

		// test count is 0 if only dynamic tests will be run (i.e. only @TestFactory methods are present), hence test run should continue.
		/*if (count == 0) {
			notifyTestRunEnded(0);
			return;
		}*/

		sendTrees(suites);

		long testStartTime= System.currentTimeMillis();
		execution.run(suites);
		notifyListenersOfTestEnd(execution, testStartTime);
	}

	private void sendTrees(ITestReference[] suites) {
		long startTime = System.currentTimeMillis();
		if (fDebugMode)
			System.out.print("start send tree..."); //$NON-NLS-1$
		for (ITestReference suite : suites) {
			suite.sendTree(this);
			}
		if (fDebugMode)
			System.out.println("done send tree - time(ms): " + (System.currentTimeMillis() - startTime)); //$NON-NLS-1$
	}

	private int countTests(ITestReference[] tests) {
		int count= 0;
		for (ITestReference test : tests) {
			if (test != null)
				count= count + test.countTestCases();
		}
		return count;
	}

	/**
	 * Reruns a test as defined by the fully qualified class name and
	 * the name of the test.
	 * @param r rerun request
	 */
	public void rerunTest(RerunRequest r) {
		final Class<?>[] classes= loadClasses(new String[] { r.fRerunClassName });
		ITestReference rerunTest1= fLoader.loadTests(classes, r.fRerunTestName, null, null, fIncludeExcludeTags, fUniqueId, this)[0];
		RerunExecutionListener service= rerunExecutionListener();

		TestExecution execution= new TestExecution(service, getClassifier());
		ITestReference[] suites= new ITestReference[] { rerunTest1 };
		execution.run(suites);

		notifyRerunComplete(r, service.getStatus());
	}

	public RerunExecutionListener rerunExecutionListener() {
		return new RerunExecutionListener(fSender, fIds);
	}

	protected IClassifiesThrowables getClassifier() {
		return new DefaultClassifier();
	}

	@Override
	public void visitTreeEntry(ITestIdentifier identifier, boolean hasChildren, int testCount, boolean isDynamicTest, String parentId) {
		String treeEntry= getTestId(identifier) + ',' + escapeText(identifier.getName()) + ',' + hasChildren + ',' + testCount
				+ ',' + isDynamicTest + ',' + parentId + ',' + escapeText(identifier.getDisplayName()) + ',' + escapeText(identifier.getParameterTypes())
				+ ',' + escapeText(identifier.getUniqueId());
		notifyTestTreeEntry(treeEntry);
	}

	public static String escapeText(String s) {
		if ((s.indexOf(',') < 0) && (s.indexOf('\\') < 0) && (s.indexOf('\r') < 0) && (s.indexOf('\n') < 0))
			return s;
		StringBuilder sb= new StringBuilder(s.length()+10);
		for (int i= 0; i < s.length(); i++) {
			char c= s.charAt(i);
			switch (c) {
			case ',':
				sb.append("\\,"); //$NON-NLS-1$
				break;
			case '\\':
				sb.append("\\\\"); //$NON-NLS-1$
				break;
			case '\r':
				if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
					i++;
				}
				sb.append(' ');
				break;
			case '\n':
				sb.append(' ');
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return sb.toString();
	}

	// WANT: work in bug fixes since RC2?
	public String getTestId(ITestIdentifier id) {
		return fIds.getTestId(id);
	}

	/**
	 * Stop the current test run.
	 */
	protected void stop() {
		if (fExecution != null) {
			fExecution.stop();
		}
	}

	/**
	 * Connect to the remote test listener.
	 *
	 * @return <code>true</code> if connection successful, <code>false</code> if failed
	 */
	protected boolean connect() {
		if (fConsoleMode) {
			fClientSocket = null;
			fWriter = new PrintWriter(System.out);
			fReader = new BufferedReader(new InputStreamReader(System.in));
			fReaderThread= new ReaderThread();
			fReaderThread.start();
			return true;
		}
		if (fDebugMode)
			System.out.println("RemoteTestRunner: trying to connect" + fHost + ":" + fPort); //$NON-NLS-1$ //$NON-NLS-2$
		Exception exception= null;
		for (int i= 1; i < 20; i++) {
			try{
				fClientSocket= new Socket(fHost, fPort);
				try {
				    fWriter= new PrintWriter(new BufferedWriter(new OutputStreamWriter(fClientSocket.getOutputStream(), "UTF-8")), false/*true*/); //$NON-NLS-1$
	            } catch (UnsupportedEncodingException e1) {
	                fWriter= new PrintWriter(new BufferedWriter(new OutputStreamWriter(fClientSocket.getOutputStream())), false/*true*/);
	            }
				try {
				    fReader= new BufferedReader(new InputStreamReader(fClientSocket.getInputStream(), "UTF-8")); //$NON-NLS-1$
                } catch (UnsupportedEncodingException e1) {
                    fReader= new BufferedReader(new InputStreamReader(fClientSocket.getInputStream()));
                }
				fReaderThread= new ReaderThread();
				fReaderThread.start();
				return true;
			} catch(IOException e){
				exception= e;
			}
			try {
				Thread.sleep(2000);
			} catch(InterruptedException e) {
			}
		}
		runFailed(JUnitMessages.getFormattedString("RemoteTestRunner.error.connect", new String[]{fHost, Integer.toString(fPort)} ), exception); //$NON-NLS-1$
		return false;
	}

	/**
	 * Shutsdown the connection to the remote test listener.
	 */
	private void shutDown() {
		if (fWriter != null) {
			fWriter.close();
			fWriter= null;
		}
		try {
			if (fReaderThread != null)   {
				// interrupt reader thread so that we don't block on close
				// on a lock held by the BufferedReader
				// fix for bug: 38955
				fReaderThread.interrupt();
			}
			if (fReader != null) {
				fReader.close();
				fReader= null;
			}
		} catch(IOException e) {
			if (fDebugMode)
				e.printStackTrace();
		}

		try {
			if (fClientSocket != null) {
				fClientSocket.close();
				fClientSocket= null;
			}
		} catch(IOException e) {
			if (fDebugMode)
				e.printStackTrace();
		}
	}

	@Override
	public void sendMessage(String msg) {
		if(fWriter == null)
			return;
		fWriter.println(msg);
//		if (!fConsoleMode)
//			System.out.println(msg);
	}

	protected void notifyTestRunStarted(int testCount) {
		fSender.sendMessage(MessageIds.TEST_RUN_START + testCount + " " + "v2"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void notifyTestRunEnded(long elapsedTime) {
		fSender.sendMessage(MessageIds.TEST_RUN_END + elapsedTime);
		fSender.flush();
		//shutDown();
	}

	protected void notifyTestRunStopped(long elapsedTime) {
		fSender.sendMessage(MessageIds.TEST_STOPPED + elapsedTime);
		fSender.flush();
		//shutDown();
	}

	protected void notifyTestTreeEntry(String treeEntry) {
		fSender.sendMessage(MessageIds.TEST_TREE + treeEntry);
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.runner.RerunCompletionListener#notifyRerunComplete(org.eclipse.jdt.internal.junit.runner.RerunRequest,
	 *      java.lang.String)
	 */
	public void notifyRerunComplete(RerunRequest r, String status) {
		if (fPort != -1) {
			fSender.sendMessage(MessageIds.TEST_RERAN + r.fRerunTestId + " " + r.fRerunClassName + " " + r.fRerunTestName + " " + status); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			fSender.flush();
		}
	}

	@Override
	public void flush() {
	    fWriter.flush();
	}

	public void runTests(TestExecution execution) {
		runTests(fTestClassNames, fTestName, execution);
	}

	public ITestLoader getTestLoader() {
		return fLoader;
	}

	public Class<?> loadClass(String className, RemoteTestRunner listener) {
		Class<?> clazz= null;
		try {
			clazz= getTestClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {
			listener.runFailed(JUnitMessages.getFormattedString("RemoteTestRunner.error.classnotfound", className), e); //$NON-NLS-1$
		}
		return clazz;
	}
}
