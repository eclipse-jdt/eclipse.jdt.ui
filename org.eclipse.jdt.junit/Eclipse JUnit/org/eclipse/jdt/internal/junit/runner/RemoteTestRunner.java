/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.runner;

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

import junit.extensions.TestSetup;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.eclipse.jdt.internal.junit.ui.*;
import org.eclipse.jdt.internal.junit.runner.*;

/**
 * RemoteTestRunner that reports results via a socket connection.
 */
public class RemoteTestRunner implements TestListener {
	private String[] fTestClassNames= null;
	private TestResult fTestResult;

	private Socket fClientSocket;
	private PrintWriter fWriter;
	private BufferedReader fReader;
	
	private static final String SUITE_METHODNAME= "suite";	
	
	private String fHost= "127.0.0.1";
	private int fPort= -1;
	private long fStartTime;
	private boolean fDebugMode= false;	
	
	private class StopThread extends Thread {
		/**
		 * listening for a MessageIds.TEST_STOP command
		 */
		public void run(){
			try { 
				String line= null; 
				if ((line= fReader.readLine()) != null) {
					if (line.startsWith(MessageIds.TEST_STOP)){
						RemoteTestRunner.this.stop();
					}
				} 
			} catch (Exception e) {
				RemoteTestRunner.this.stop();
			}
		}
	}	
	
	public static void main(String[] args) throws InvocationTargetException{
		RemoteTestRunner testRunServer= new RemoteTestRunner();
		testRunServer.init(args);
		testRunServer.run();
	}
	
	protected void init(String[] args) throws InvocationTargetException{
		defaultInit(args);		// overriding classes should call this
	}	
	
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
			throw new InvocationTargetException(new Exception("Error: parameter '-classNames' or '-className' not specified"));
	}
	
	protected final void run() throws InvocationTargetException {
		if(fPort != -1)
			connect();
			
		fTestResult= new TestResult();
		fTestResult.addListener(this);
		
		runTests(fTestClassNames);
				
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
		
		try {
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
	}


	public void notifyTestRunEnded(long elapsedTime) {
		if (fPort != -1) {	
			sendMessage(MessageIds.TEST_ELAPSED_TIME + elapsedTime);
			shutDown();
		}
	}


	public void notifyTestRunStopped(long elapsedTime) {
		if (fPort != -1) {
			sendMessage(MessageIds.TEST_STOPPED + elapsedTime);
			shutDown();
		}
	}

	public void notifyTestStarted(String testName) {
		if (fPort != -1)
			sendMessage(MessageIds.TEST_START + testName);
	}

	public void notifyTestEnded(String testName) {
		if (fPort != -1)
			sendMessage(MessageIds.TEST_END + testName);
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
