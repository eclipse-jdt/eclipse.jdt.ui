/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de bug 26754 
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.runner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.util.Vector;

import junit.extensions.TestDecorator;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * A TestRunner that reports results via a socket connection.
 * See MessageIds for more information about the protocl.
 */
public class RemoteTestRunner implements TestListener {
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
	
	private static final String SET_UP_TEST_METHOD_NAME= "setUpTest"; //$NON-NLS-1$
	
	private static final String SUITE_METHODNAME= "suite";	 //$NON-NLS-1$
	
	/**
	 * The name of the test classes to be executed
	 */
	private String[] fTestClassNames;
	/**
	 * The name of the test (argument -test)
	 */
	private String fTestName;
	/**
	 * The current test result
	 */
	private TestResult fTestResult;

	/**
	 * The version expected by the client
	 */
	private String fVersion= ""; //$NON-NLS-1$
	
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
	private Vector fRerunRequests= new Vector(10);
	/**
	 * Thread reading from the socket
	 */
	private ReaderThread fReaderThread;

	private String fRerunTest;
	/**
	 * Reader thread that processes messages from the client.
	 */
	private class ReaderThread extends Thread {
		public ReaderThread() {
			super("ReaderThread"); //$NON-NLS-1$
		}

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
							int c0= arg.indexOf(' '); //$NON-NLS-1$
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
	
	/** 
	 * The main entry point.
	 * Parameters<pre>
	 * -classnames: the name of the test suite class
	 * -testfilename: the name of a file containing classnames of test suites
	 * -test: the test method name (format classname testname) 
	 * -host: the host to connect to default local host 
	 * -port: the port to connect to, mandatory argument 
	 * -keepalive: keep the process alive after a test run
     * </pre>
     */
	public static void main(String[] args) {
		RemoteTestRunner testRunServer= new RemoteTestRunner();
		testRunServer.init(args);
		testRunServer.run();
		// fix for 14434
		System.exit(0);
	}
	
	/**
	 * Parse command line arguments. Hook for subclasses to process
	 * additional arguments.
	 */
	protected void init(String[] args) {
		defaultInit(args);		
	}	
	
	/**
	 * The class loader to be used for loading tests.
	 * Subclasses may override to use another class loader.
	 */
	protected ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}
	
	/**
	 * Process the default arguments.
	 */
	protected final void defaultInit(String[] args) {
		for(int i= 0; i < args.length; i++) {
			if(args[i].toLowerCase().equals("-classnames") || args[i].toLowerCase().equals("-classname")){ //$NON-NLS-1$ //$NON-NLS-2$
				Vector list= new Vector();
				for (int j= i+1; j < args.length; j++) {
					if (args[j].startsWith("-")) //$NON-NLS-1$
						break;
					list.add(args[j]);
				}
				fTestClassNames= (String[]) list.toArray(new String[list.size()]);
			}	
			else if(args[i].toLowerCase().equals("-test")) { //$NON-NLS-1$
				String testName= args[i+1];
				int p= testName.indexOf(':');
				if (p == -1)
					throw new IllegalArgumentException("Testname not separated by \'%\'"); //$NON-NLS-1$
				fTestName= testName.substring(p+1);
				fTestClassNames= new String[]{ testName.substring(0, p)  };
				i++;
			}			
			else if(args[i].toLowerCase().equals("-testnamefile")) { //$NON-NLS-1$
				String testNameFile= args[i+1];
				try {
					readTestNames(testNameFile);
				} catch (IOException e) {
					throw new IllegalArgumentException("Cannot read testname file.");		 //$NON-NLS-1$
				}
				i++;
			
			} else if(args[i].toLowerCase().equals("-port")) { //$NON-NLS-1$
				fPort= Integer.parseInt(args[i+1]);
				i++;
			}
			else if(args[i].toLowerCase().equals("-host")) { //$NON-NLS-1$
				fHost= args[i+1];
				i++;
			}
			else if(args[i].toLowerCase().equals("-rerun")) { //$NON-NLS-1$
				fRerunTest= args[i+1];
				i++;
			}
			else if(args[i].toLowerCase().equals("-keepalive")) { //$NON-NLS-1$
				fKeepAlive= true;
			}
			else if(args[i].toLowerCase().equals("-debugging") || args[i].toLowerCase().equals("-debug")){ //$NON-NLS-1$ //$NON-NLS-2$
			    fDebugMode= true;
			}
			else if(args[i].toLowerCase().equals("-version")){ //$NON-NLS-1$
			    fVersion= args[i+1];
			    i++;
			}
		}
		if(fTestClassNames == null || fTestClassNames.length == 0)
			throw new IllegalArgumentException(JUnitMessages.getString("RemoteTestRunner.error.classnamemissing")); //$NON-NLS-1$

		if (fPort == -1)
			throw new IllegalArgumentException(JUnitMessages.getString("RemoteTestRunner.error.portmissing")); //$NON-NLS-1$
		if (fDebugMode)
			System.out.println("keepalive "+fKeepAlive); //$NON-NLS-1$
	}

	private void readTestNames(String testNameFile) throws IOException {
		BufferedReader br= new BufferedReader(new FileReader(new File(testNameFile)));
		try {
			String line;
			Vector list= new Vector();
			while ((line= br.readLine()) != null) {
				list.add(line);
			}
			fTestClassNames= (String[]) list.toArray(new String[list.size()]);
		}
		finally {
			br.close();
		}
		if (fDebugMode) {
			System.out.println("Tests:"); //$NON-NLS-1$
			for (int i= 0; i < fTestClassNames.length; i++) {
				System.out.println("    "+fTestClassNames[i]); //$NON-NLS-1$
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
			rerunTest(Integer.parseInt(fRerunTest), fTestClassNames[0], fTestName);
			return;
		}
		fTestResult= new TestResult();
		fTestResult.addListener(this);
		runTests(fTestClassNames, fTestName);
		fTestResult.removeListener(this);
		
		if (fTestResult != null) {
			fTestResult.stop();
			fTestResult= null;
		}
		if (fKeepAlive)
			waitForReruns();
			
		shutDown();
		
	}

	/**
	 * Waits for rerun requests until an explicit stop request
	 */
	private synchronized void waitForReruns() {
		while (!fStopped) {
			try {
				wait();
				if (!fStopped && fRerunRequests.size() > 0) {
					RerunRequest r= (RerunRequest)fRerunRequests.remove(0);
					rerunTest(r.fRerunTestId, r.fRerunClassName, r.fRerunTestName);
				}
			} catch (InterruptedException e) {
			}
		}
	}
	
	/**
	 * Returns the Test corresponding to the given suite. 
	 */
	private Test getTest(String suiteClassName, String testName) {
		Class testClass= null;
		try {
			testClass= loadSuiteClass(suiteClassName);
		} catch (ClassNotFoundException e) {
			String clazz= e.getMessage();
			if (clazz == null) 
				clazz= suiteClassName;
			runFailed(JUnitMessages.getFormattedString("RemoteTestRunner.error.classnotfound", clazz)); //$NON-NLS-1$
			return null;
		} catch(Exception e) {
			runFailed(JUnitMessages.getFormattedString("RemoteTestRunner.error.exception", e )); //$NON-NLS-1$
			return null;
		}
		if (testName != null) {
			return setupTest(testClass, createTest(testName, testClass));
		}
		Method suiteMethod= null;
		try {
			suiteMethod= testClass.getMethod(SUITE_METHODNAME, new Class[0]);
	 	} catch(Exception e) {
	 		// try to extract a test suite automatically
			return new TestSuite(testClass);
		}
		Test test= null;
		try {
			test= (Test)suiteMethod.invoke(null, new Class[0]); // static method
		} 
		catch (InvocationTargetException e) {
			runFailed(JUnitMessages.getFormattedString("RemoteTestRunner.error.invoke", e.getTargetException().toString() )); //$NON-NLS-1$
			return null;
		}
		catch (IllegalAccessException e) {
			runFailed(JUnitMessages.getFormattedString("RemoteTestRunner.error.invoke", e.toString() )); //$NON-NLS-1$
			return null;
		}
		return test;
	}

	protected void runFailed(String message) {
		System.err.println(message);
	}
	
	/**
	 * Loads the test suite class.
	 */
	private Class loadSuiteClass(String className) throws ClassNotFoundException {
		if (className == null) 
			return null;
		return getClassLoader().loadClass(className);
	}
			
	/**
	 * Runs a set of tests.
	 */
	private void runTests(String[] testClassNames, String testName) {
		// instantiate all tests
		Test[] suites= new Test[testClassNames.length];
		
		for (int i= 0; i < suites.length; i++) {
			suites[i]= getTest(testClassNames[i], testName);
		}
		
		// count all testMethods and inform ITestRunListeners		
		int count= countTests(suites);
		notifyTestRunStarted(count);
		
		if (count == 0) {
			notifyTestRunEnded(0);
			return;
		}
		
		long startTime= System.currentTimeMillis();
		if (fDebugMode)
			System.out.print("start send tree..."); //$NON-NLS-1$
		for (int i= 0; i < suites.length; i++) {
			sendTree(suites[i]);
		}
		if (fDebugMode)
			System.out.println("done send tree - time(ms): "+(System.currentTimeMillis()-startTime)); //$NON-NLS-1$

		long testStartTime= System.currentTimeMillis();
		for (int i= 0; i < suites.length; i++) {
			suites[i].run(fTestResult);
		}
		// inform ITestRunListeners of test end
		if (fTestResult == null || fTestResult.shouldStop())
			notifyTestRunStopped(System.currentTimeMillis() - testStartTime);
		else
			notifyTestRunEnded(System.currentTimeMillis() - testStartTime);
	}
	
	private int countTests(Test[] tests) {
		int count= 0;
		for (int i= 0; i < tests.length; i++) {
			if (tests[i] != null)
				count= count + tests[i].countTestCases();
		}
		return count;
	}
	
	/**
	 * Reruns a test as defined by the fully qualified class name and
	 * the name of the test.
	 */
	public void rerunTest(int testId, String className, String testName) {
		Test reloadedTest= null;
		Class reloadedTestClass= null;
		try {
			reloadedTestClass= getClassLoader().loadClass(className);
			reloadedTest= createTest(testName, reloadedTestClass);
		} catch(Exception e) {
			reloadedTest= warning(JUnitMessages.getFormattedString("RemoteTestRunner.error.couldnotcreate", testName));  //$NON-NLS-1$ 
		}
		Test rerunTest= setupTest(reloadedTestClass, reloadedTest);
		TestResult result= new TestResult();
		rerunTest.run(result);
		notifyTestReran(result, Integer.toString(testId), className, testName);
	}

	/**
	 * Prepare a single test to be run standalone. If the test case class provides
	 * a static method Test setUpTest(Test test) then this method will be invoked.
	 * Instead of calling the test method directly the "decorated" test returned from
	 * setUpTest will be called. The purpose of this mechanism is to enable
	 * tests which requires a set-up to be run individually.
	 */
	private Test setupTest(Class reloadedTestClass, Test reloadedTest) {
		Method setup= null;
		try {
			setup= reloadedTestClass.getMethod(SET_UP_TEST_METHOD_NAME, new Class[] {Test.class});
		} catch (SecurityException e1) {
			return reloadedTest;
		} catch (NoSuchMethodException e) {
			return reloadedTest;
		}
		if (setup.getReturnType() != Test.class)
			return warning(JUnitMessages.getString("RemoteTestRunner.error.notestreturn")); //$NON-NLS-1$
		if (!Modifier.isPublic(setup.getModifiers()))
			return warning(JUnitMessages.getString("RemoteTestRunner.error.shouldbepublic"));  //$NON-NLS-1$
		if (!Modifier.isStatic(setup.getModifiers()))
			return warning(JUnitMessages.getString("RemoteTestRunner.error.shouldbestatic"));  //$NON-NLS-1$
		try {
			Test test= (Test)setup.invoke(null, new Object[] {reloadedTest});
			if (test == null)
				return warning(JUnitMessages.getString("RemoteTestRunner.error.nullreturn")); //$NON-NLS-1$
			return test;
		} catch (IllegalArgumentException e) {
			return warning(JUnitMessages.getFormattedString("RemoteTestRunner.error.couldnotinvoke", e)); //$NON-NLS-1$
		} catch (IllegalAccessException e) {
			return warning(JUnitMessages.getFormattedString("RemoteTestRunner.error.couldnotinvoke", e)); //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			return warning(JUnitMessages.getFormattedString("RemoteTestRunner.error.invocationexception", e.getTargetException())); //$NON-NLS-1$
		} 
	}

	/**
	 * Returns a test which will fail and log a warning message.
	 */
	 private Test warning(final String message) {
		return new TestCase("warning") { //$NON-NLS-1$
			protected void runTest() {
				fail(message);
			}
		};		
	}

	private Test createTest(String testName, Class testClass) {
		Class[] classArgs= { String.class };
		Test test;
		Constructor constructor= null;
		try {
			try {
				constructor= testClass.getConstructor(classArgs);
				test= (Test)constructor.newInstance(new Object[]{testName});
			} catch (NoSuchMethodException e) {
				// try the no arg constructor supported in 3.8.1
				constructor= testClass.getConstructor(new Class[0]);
				test= (Test)constructor.newInstance(new Object[0]);
				if (test instanceof TestCase)
					((TestCase) test).setName(testName);
			}
			if (test != null)
				return test;
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		} catch (NoSuchMethodException e) {
		} catch (ClassCastException e) {
		}
		return warning("Could not create test \'"+testName+"\' "); //$NON-NLS-1$ //$NON-NLS-2$
	}


	/*
	 * @see TestListener#addError(Test, Throwable)
	 */
	public final void addError(Test test, Throwable throwable) {
		notifyTestFailed(test, MessageIds.TEST_ERROR, getTrace(throwable));
	}

	/*
	 * @see TestListener#addFailure(Test, AssertionFailedError)
	 */
	public final void addFailure(Test test, AssertionFailedError assertionFailedError) {
		if ("3".equals(fVersion)) { //$NON-NLS-1$
			if (isComparisonFailure(assertionFailedError)) {
		        // transmit the expected and the actual string
		        String expected = getField(assertionFailedError, "fExpected"); //$NON-NLS-1$
		        String actual = getField(assertionFailedError, "fActual"); //$NON-NLS-1$
		        if (expected != null && actual != null) {
		            notifyTestFailed2(test, MessageIds.TEST_FAILED, getTrace(assertionFailedError), expected, actual);
		            return;
		       }
		    }
		} 
		notifyTestFailed(test, MessageIds.TEST_FAILED, getTrace(assertionFailedError));
	}

	private boolean isComparisonFailure(Throwable throwable) {
		// avoid reference to comparison failure to avoid a dependency on 3.8.1
		return throwable.getClass().getName().equals("junit.framework.ComparisonFailure"); //$NON-NLS-1$
	}

	/*
	 * @see TestListener#endTest(Test)
	 */
	public void endTest(Test test) {
		notifyTestEnded(test);
	}

	/*
	 * @see TestListener#startTest(Test)
	 */
	public void startTest(Test test) {
		notifyTestStarted(test);
	}
	
	private void sendTree(Test test){
		if(test instanceof TestDecorator){
			TestDecorator decorator= (TestDecorator) test;
			sendTree(decorator.getTest());		
		}
		else if(test instanceof TestSuite){
			TestSuite suite= (TestSuite) test;
			notifyTestTreeEntry(getTestId(test)+','+escapeComma(suite.toString().trim()) + ',' + true + ',' + suite.testCount());
			for(int i=0; i < suite.testCount(); i++){	
				sendTree(suite.testAt(i));		
			}				
		}
		else {
			notifyTestTreeEntry(getTestId(test)+ ',' + escapeComma(getTestName(test).trim()) + ',' + false + ',' +  test.countTestCases());
		}
	}
	
	private String escapeComma(String s) {
		if ((s.indexOf(',') < 0) && (s.indexOf('\\') < 0))
			return s;
		StringBuffer sb= new StringBuffer(s.length()+10);
		for (int i= 0; i < s.length(); i++) {
			char c= s.charAt(i);
			if (c == ',') 
				sb.append("\\,"); //$NON-NLS-1$
			else if (c == '\\')
				sb.append("\\\\"); //$NON-NLS-1$
			else
				sb.append(c);
		}
		return sb.toString();
	}

	private String getTestId(Test test) {
		return Integer.toString(System.identityHashCode(test));
	}
	
	private String getTestName(Test test) {
		if (test instanceof TestCase) {
			TestCase testCase= (TestCase) test;
			return JUnitMessages.getFormattedString("RemoteTestRunner.testName", new String[] {testCase.getName(),  test.getClass().getName()}); //$NON-NLS-1$
		}
		if (test instanceof TestSuite) {
			TestSuite suite= (TestSuite) test;
			if (suite.getName() != null)
				return suite.getName();
			return getClass().getName();
		}
		return test.toString();
	}
	
	/**
	 * Returns the stack trace for the given throwable.
	 */
	private String getTrace(Throwable t) { 
		StringWriter stringWriter= new StringWriter();
		PrintWriter writer= new PrintWriter(stringWriter);
		t.printStackTrace(writer);
		StringBuffer buffer= stringWriter.getBuffer();
		return buffer.toString();
	}	

	/**
	 * Stop the current test run.
	 */
	protected void stop() {
		if (fTestResult != null) {
			fTestResult.stop();
		}
	}
	
	/**
	 * Connect to the remote test listener.
	 */
	private boolean connect() {
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
		runFailed(JUnitMessages.getFormattedString("RemoteTestRunner.error.connect", new String[]{fHost, Integer.toString(fPort)} )); //$NON-NLS-1$
		exception.printStackTrace();
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


	private void sendMessage(String msg) {
		if(fWriter == null) 
			return;
		fWriter.println(msg);
	}


	private void notifyTestRunStarted(int testCount) {
		sendMessage(MessageIds.TEST_RUN_START + testCount + " " + "v2"); //$NON-NLS-1$ //$NON-NLS-2$
	}


	private void notifyTestRunEnded(long elapsedTime) {
		sendMessage(MessageIds.TEST_RUN_END + elapsedTime);
		fWriter.flush();
		//shutDown();
	}


	private void notifyTestRunStopped(long elapsedTime) {
		sendMessage(MessageIds.TEST_STOPPED + elapsedTime );
		fWriter.flush();
		//shutDown();
	}

	private void notifyTestStarted(Test test) {
		sendMessage(MessageIds.TEST_START + getTestId(test) + ','+test.toString());
		fWriter.flush();
	}

	private void notifyTestEnded(Test test) {
		sendMessage(MessageIds.TEST_END + getTestId(test)+','+getTestName(test));
	}

	private void notifyTestFailed(Test test, String status, String trace) {
		sendMessage(status + getTestId(test) + ',' + getTestName(test));
		sendMessage(MessageIds.TRACE_START);
		sendMessage(trace);
		sendMessage(MessageIds.TRACE_END);
		fWriter.flush();
	}

	private void notifyTestFailed2(Test test, String status, String trace, String expected, String actual) {
	    sendMessage(status + getTestId(test) + ',' + getTestName(test));
	    
	    sendMessage(MessageIds.EXPECTED_START);
	    sendMessage(expected);
	    sendMessage(MessageIds.EXPECTED_END);
	    
	    sendMessage(MessageIds.ACTUAL_START);
	    sendMessage(actual);
	    sendMessage(MessageIds.ACTUAL_END);
	    
	    sendMessage(MessageIds.TRACE_START);
	    sendMessage(trace);
	    sendMessage(MessageIds.TRACE_END);
	    
	    fWriter.flush();
	}
	
	private void notifyTestTreeEntry(String treeEntry) {
		sendMessage(MessageIds.TEST_TREE + treeEntry);
	}
	
	private void notifyTestReran(TestResult result, String testId, String testClass, String testName) {
		TestFailure failure= null;
		if (result.errorCount() > 0) {
			failure= (TestFailure)result.errors().nextElement();
		}
		if (result.failureCount() > 0) {
			failure= (TestFailure)result.failures().nextElement();
		}
		if (failure != null) {
			Throwable t= failure.thrownException();
			
			if ("3".equals(fVersion)) { //$NON-NLS-1$
			    if (isComparisonFailure(t)) {
			        // transmit the expected and the actual string
			        String expected = getField(t, "fExpected"); //$NON-NLS-1$
			        String actual = getField(t, "fActual"); //$NON-NLS-1$
			        if (expected != null && actual != null) {
			    	    sendMessage(MessageIds.EXPECTED_START);
			    	    sendMessage(expected);
			    	    sendMessage(MessageIds.EXPECTED_END);
			    	    
			    	    sendMessage(MessageIds.ACTUAL_START);
			    	    sendMessage(actual);
			    	    sendMessage(MessageIds.ACTUAL_END);
			    	    			    	    			       }
			    }
			}
			String trace= getTrace(t);
			sendMessage(MessageIds.RTRACE_START);
			sendMessage(trace);
			sendMessage(MessageIds.RTRACE_END);
			fWriter.flush();
		}
		String status= "OK"; //$NON-NLS-1$
		if (result.errorCount() > 0)
			status= "ERROR"; //$NON-NLS-1$
		else if (result.failureCount() > 0)
			status= "FAILURE"; //$NON-NLS-1$
		if (fPort != -1) {
			sendMessage(MessageIds.TEST_RERAN + testId+ " "+testClass+" "+testName+" "+status); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			fWriter.flush();
		}
	}
	
	private String getField(Object object, String fieldName) {
	    Class clazz= object.getClass();
	    try {
	        Field field= clazz.getDeclaredField(fieldName);
	        field.setAccessible(true);
	        Object result= field.get(object);
	        return result.toString();
	    } catch (Exception e) {
	        // fall through
	    }
	    return null;
	}
}	
