/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import junit.framework.*;
import junit.extensions.*;

/**
 * Server that runs JUnit Tests.
 */
public class TestRunner implements TestListener {
	private String[] fTestClassNames= null;
	private String[] fListenerClassNames= new String[0];
	private TestResult fTestResult;

	private Socket fClientSocket;
	private PrintWriter fWriter;
	private BufferedReader fReader;
	
	private static final String SUITE_METHODNAME= "suite";	
	
	private String fHost= "127.0.0.1";
	private int fPort= -1;
	private long fStartTime;
	private boolean fDebugMode= false;	
	private Vector fTestListeners= new Vector();
	private Vector fTestRunListeners= new Vector();
	
	private class StopThread extends Thread {
		/**
		 * listening for a MessageIds.TEST_STOP command
		 */
		public void run(){
			try { 
				String line= null; 
				if ((line= fReader.readLine()) != null) {
					if (line.startsWith(MessageIds.TEST_STOP)){
						TestRunner.this.stop();
					}
				} 
			} catch (Exception e) {
				TestRunner.this.stop();
			}
		}
	}	
	
	/**
	 * extended classes have to override this method
	 */
	public static void main(String[] args) throws InvocationTargetException{
		TestRunner testRunServer= new TestRunner();
		testRunServer.init(args);
		testRunServer.run();
	}
	
	/**
	 * extended classes can override this method
	 */
	protected void init(String[] args) throws InvocationTargetException{
		defaultInit(args);		// overriding classes should call this
	}	
	
	/**
	 * returns the ClassLoader for loading the tests
	 * extended classes can override this method
	 */
	protected ClassLoader getClassLoader() throws InvocationTargetException {
		return getClass().getClassLoader();
	}
	
	/**
	 * parses the arguments passed by run(String[] args) 
	 * testClassNames, host, port, listeners 
	 * and debugMode are set
	 */
	protected final void defaultInit(String[] args) throws InvocationTargetException {
		for(int i= 0; i < args.length; i++) {
			if(args[i].toLowerCase().equals("-classnames") || args[i].toLowerCase().equals("-classname")){
				ArrayList list= new ArrayList();
				for (int j= i+1; j < args.length; j++) {
					if (args[j].startsWith("-"))
						break;
					list.add(args[j]);
				}
				fTestClassNames= (String[]) list.toArray(new String[list.size()]);
			}		
			if(args[i].toLowerCase().equals("-testlistener") || args[i].toLowerCase().equals("-testlisteners")){
				ArrayList list= new ArrayList();
				for (int j= i+1; j < args.length; j++) {
					if (args[j].startsWith("-"))
						break;
					list.add(args[j]);
				}
				fListenerClassNames= (String[]) list.toArray(new String[list.size()]);
			}
			if(args[i].toLowerCase().equals("-port")){
				fPort= Integer.parseInt(args[i+1]);
			}
			if(args[i].toLowerCase().equals("-host")){
				fHost= args[i+1];
			}
			if(args[i].toLowerCase().equals("-debugging") || args[i].toLowerCase().equals("-debug")){
				fDebugMode= true;
			}
		}
		if(fTestClassNames == null || fTestClassNames.length == 0)
			throw new InvocationTargetException(new Exception("error: parameter '-classNames' or '-className' not specified"));
	}

	public final void addTestRunListener(ITestRunListener listener) {
		fTestRunListeners.addElement(listener);
	}
	
	public final void removeTestRunListener(ITestRunListener listener) {
		fTestRunListeners.removeElement(listener);
	}
	
	protected final void run() throws InvocationTargetException {
		if(fPort != -1)
			connect();
			
		fTestResult= new TestResult();
		fTestResult.addListener(this);
		
		// register all Listeners of type junit.framework.TestListener 
		// and ITestRunListener, if not of this type -> exception
		for (int i= 0; i < fListenerClassNames.length; i++) {
			try {
				Class listenerClass= Class.forName(fListenerClassNames[i]);
				Object obj= listenerClass.newInstance();
				if (obj instanceof ITestRunListener)
					addTestRunListener((ITestRunListener) obj);
				if (obj instanceof TestListener) {
					fTestListeners.addElement((TestListener) obj);
					// register TestListeners directly on the TestResult
					fTestResult.addListener((TestListener) obj);
				}
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
		}	
		
		// if no listener and no port is specified,
		// then try to use the default TextTestListener
		if (fPort == -1 && fTestListeners.size() + fTestRunListeners.size() == 0) {
			try {
				Class listenerClass= Class.forName(TextTestListener.class.getName());
				ITestRunListener listener= (ITestRunListener) listenerClass.newInstance();
				addTestRunListener(listener);	
			} catch (Exception e) {
				if (fDebugMode)
					e.printStackTrace();
				throw new InvocationTargetException(e);
			}
		}
		
		runTests(fTestClassNames);
		
		// remove all testListeners
		fTestRunListeners.removeAllElements();
		Enumeration enum= fTestListeners.elements();
		while (enum.hasMoreElements())
			fTestResult.removeListener((TestListener) enum.nextElement());
		
		fTestResult.removeListener(this);
		if (fTestResult != null) {
			fTestResult.stop();
			fTestResult= null;
		}
	}
	
	/**
	 * Returns the Test corresponding to to the given className
	 */
	private final Test getTest(String className) throws InvocationTargetException{
		if (className == null) {
			return null;
		}
		try {
			Class clazz= getClassLoader().loadClass(className);
			return getTest(clazz);
		} catch (ClassNotFoundException e) {
			if (fDebugMode)
				e.printStackTrace();
			throw new InvocationTargetException(e);
		}
	}

	private final Test getTest(Class clazz) throws InvocationTargetException {
		try { 
			Object obj= clazz.newInstance();
			if (obj instanceof TestSuite)
				return (TestSuite) obj;
		} catch (Exception e) {
			if (fDebugMode) {
				System.out.println();
				System.out.println("ClassLoader info:");
				System.out.println("could not instantiate " + clazz.getName() + " with default constructor.");
				e.printStackTrace();
			}
		}
		try { 
			Method suiteMethod= clazz.getMethod(SUITE_METHODNAME, new Class[0]);
			Object obj= suiteMethod.invoke(null, new Class[0]);
			return (Test) obj;
		} catch (Exception e) {
			if (fDebugMode) {
				System.out.println();
				System.out.println("ClassLoader info:");
				System.out.println(clazz.getName() + " has no static method suite() that returns a Test.");
				e.printStackTrace();
			}
		}
		try {
			return new TestSuite(clazz);
		} catch (Exception e) {
			// print always, this should not happen.
			System.out.println();
			System.out.println("ClassLoader error:");
			System.out.println("could not load " + clazz.getName());
			e.printStackTrace();			
		}
		return null;
	}
			
	/**
	 * @param testClassNames String array of full qualified class names of test classes
	 */
	private final void runTests(String[] testClassNames) throws InvocationTargetException {
		// instantiate all tests
		Test[] suites= new Test[testClassNames.length];
		for (int i= 0; i < suites.length; i++)
			suites[i]= getTest(testClassNames[i]);
		
		// count all testMethods and inform ITestRunListeners		
		int count= countTests(suites);
		notifyTestRunStarted(count);

		notifyTestTreeStart();
		sendTree(suites[0]);
	
		long fgStartTime= System.currentTimeMillis();
		for (int i= 0; i < suites.length; i++) {
			if (suites[i] instanceof Test) {
				if (suites[i] instanceof TestCase) 
					suites[i]= new TestSuite(suites[i].getClass().getName());
				suites[i].run(fTestResult);
			}
			else
				System.err.println("Could not run " + suites[i] + " - no instanceof Test");
		}
		// inform ITestRunListeners of test end
		if (fTestResult == null || fTestResult.shouldStop())
			notifyTestRunStopped(System.currentTimeMillis() - fgStartTime);
		else
			notifyTestRunEnded(System.currentTimeMillis() - fgStartTime);
	}
	
	private final int countTests(Test[] tests) {
		int count= 0;
		for (int i= 0; i < tests.length; i++) {
			count= count + tests[i].countTestCases();
		}
		return count;
	}

	/**
	 * @see TestListener#addError(Test, Throwable)
	 */
	public final void addError(Test test, Throwable throwable) {
		notifyTestFailed(ITestRunListener.STATUS_ERROR, test.toString(), getTrace(throwable));
	}

	/**
	 * @see TestListener#addFailure(Test, AssertionFailedError)
	 */
	public final void addFailure(Test test, AssertionFailedError assertionFailedError) {
		notifyTestFailed(ITestRunListener.STATUS_FAILURE, test.toString(), getTrace(assertionFailedError));
	}

	/**
	 * @see TestListener#endTest(Test)
	 */
	public final void endTest(Test test) {
		notifyTestEnded(test.toString());
	}

	/**
	 * @see TestListener#startTest(Test)
	 */
	public final void startTest(Test test) {
		notifyTestStarted(test.toString());
	}
	
	private final void sendTree(Test test){
		if(fPort == -1) return;
		if(test instanceof TestSetup){
			TestSetup testSetup= (TestSetup) test;
			sendTree(testSetup.getTest());		
		}
		else if(test instanceof TestSuite){
			TestSuite suite= (TestSuite) test;
			notifyTestTreeEntry(suite.toString().trim() + ',' + true + ',' + suite.testCount());
			for(int i=0; i< suite.testCount(); i++){	
				sendTree(suite.testAt(i));		
			}				
		}
		else {
			notifyTestTreeEntry(test.toString().trim() + ',' + false + ',' +  test.countTestCases());
		}
	}
	
	/**
	 * Returns a filtered stack trace
	 */
	private static String getTrace(Throwable t) { 
		StringWriter stringWriter= new StringWriter();
		PrintWriter writer= new PrintWriter(stringWriter);
		t.printStackTrace(writer);
		StringBuffer buffer= stringWriter.getBuffer();
		return buffer.toString();
	}	

	protected void stop() {
		if (fTestResult != null) {
			fTestResult.stop();
		}
	}
	
	/**
	 * connect to fgHost on fgPort
	 */
	protected boolean connect(){
		try{
			fClientSocket= new Socket(fHost, fPort);
			fWriter= new PrintWriter(fClientSocket.getOutputStream(), true);
			fReader= new BufferedReader(new InputStreamReader(fClientSocket.getInputStream()));
			new StopThread().start();
			return true;
		} catch(IOException e){
			// print always
			System.err.println("could not connect to: " + fHost + ":" + fPort);			
			e.printStackTrace();
		}
		return false;
	}

	protected void shutDown() {
		
		if (fWriter != null) {
			fWriter.close();
			fWriter= null;
		}
		
		try{
			if (fReader != null) {
				fReader.close();
				fReader= null;
			}
		} catch(IOException e) {
			if (fDebugMode)
				e.printStackTrace();
		}
		
		try{
			if (fClientSocket != null) {
				fClientSocket.close();
				fClientSocket= null;
			}
		} catch(IOException e) {
			if (fDebugMode)	
				e.printStackTrace();
		}
	}

	protected void sendMessage(String msg) {
		try {
			if(msg == null && fWriter == null) return;
			fWriter.println(msg);
		} catch (NullPointerException e) {
			if (fDebugMode)
				e.printStackTrace();
		}
	}

	public void notifyTestRunStarted(int testCount) {
		if (fPort != -1) {	
			sendMessage(MessageIds.TEST_COUNT + testCount);
			fStartTime= System.currentTimeMillis();
		}
		Enumeration enum= fTestRunListeners.elements();
		while(enum.hasMoreElements())
			((ITestRunListener) enum.nextElement()).testRunStarted(testCount);
	}

	public void notifyTestRunEnded(long elapsedTime) {
		if (fPort != -1) {	
			sendMessage(MessageIds.TEST_ELAPSED_TIME + elapsedTime);
			shutDown();
		}
		Enumeration enum= fTestRunListeners.elements();
		while(enum.hasMoreElements())
			((ITestRunListener) enum.nextElement()).testRunEnded(elapsedTime);
	}

	public void notifyTestRunStopped(long elapsedTime) {
		if (fPort != -1) {
			sendMessage(MessageIds.TEST_STOPPED + elapsedTime);
			shutDown();
		}
		Enumeration enum= fTestRunListeners.elements();
		while(enum.hasMoreElements())
			((ITestRunListener) enum.nextElement()).testRunStopped(elapsedTime);
	}

	public void notifyTestStarted(String testName) {
		if (fPort != -1)
			sendMessage(MessageIds.TEST_START + testName);
		Enumeration enum= fTestRunListeners.elements();
		while(enum.hasMoreElements())
			((ITestRunListener) enum.nextElement()).testStarted(testName);
	}

	public void notifyTestEnded(String testName) {
		if (fPort != -1)
			sendMessage(MessageIds.TEST_END + testName);
		Enumeration enum= fTestRunListeners.elements();
		while(enum.hasMoreElements())
			((ITestRunListener) enum.nextElement()).testEnded(testName);
	}

	public void notifyTestFailed(int status, String testName, String trace) {
		if (fPort != -1) {		
			if(status == ITestRunListener.STATUS_FAILURE)
				sendMessage(MessageIds.TEST_FAILED + testName);
			else
				sendMessage(MessageIds.TEST_ERROR + testName);

			sendMessage(MessageIds.TRACE_START);
			sendMessage(trace);
			sendMessage(MessageIds.TRACE_END);
		}
		Enumeration enum= fTestRunListeners.elements();
		while(enum.hasMoreElements())
			((ITestRunListener) enum.nextElement()).testFailed(status, testName, trace);
	}

	public void notifyTestTreeStart() {
		if (fPort != -1)
			sendMessage(MessageIds.TEST_TREE_START); 
	}

	public void notifyTestTreeEntry(String treeEntry) {
		if (fPort != -1)
			sendMessage(MessageIds.TEST_TREE + treeEntry);
	}
}	
