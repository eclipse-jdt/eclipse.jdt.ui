/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Vector;


public class TextTestListener implements ITestRunListener {
	PrintStream fWriter= System.out;
	int fColumn= 0;
	
	private Vector fErrors= new Vector();
	private Vector fFailures= new Vector();
	private int fTestCount= 0;
	private int fExecutedTests= 0;

	public static String[] fgFilterPatterns= new String[] {
		"org.eclipse.jdt.junit.internal",
		"org.eclipse.jdt.junit.eclipse.internal",
		"junit.framework.TestCase",
		"junit.framework.TestResult",
		"junit.framework.TestSuite",
		"junit.framework.Assert.", // don't filter AssertionFailure
		"junit.swingui.TestRunner",
		"junit.awtui.TestRunner",
		"junit.textui.TestRunner",
		"java.lang.reflect.Method.invoke"
	};

	/**
	 * @see ITestRunListener#testRunStarted(int)
	 */
	public void testRunStarted(int testCount) {
		fTestCount= testCount;
	}

	/**
	 * @see ITestRunListener#testRunEnded(long)
	 */
	public void testRunEnded(long elapsedTime) {
		showSummary(elapsedTime);
	}

	/**
	 * @see ITestRunListener#testRunStopped(long)
	 */
	public void testRunStopped(long elapsedTime) {
		writer().println();
		writer().println("Stopped after " + elapsedTimeAsString(elapsedTime) + " seconds.");
	    printErrors();
	    printFailures();
	    printHeader();
	}

	/**
	 * @see ITestRunListener#testStarted(String)
	 */
	public void testStarted(String testName) {
		fExecutedTests++;
		writer().print(".");
		if (fColumn++ >= 40) {
			writer().println();
			fColumn= 0;
		}
	}

	/**
	 * @see ITestRunListener#testEnded(String)
	 */
	public void testEnded(String testName) {
	}

	/**
	 * @see ITestRunListener#testFailed(int, String, String, String)
	 */
	public void testFailed(int status, String testName, String trace) {
		if(status == ITestRunListener.STATUS_ERROR) {
			writer().print("E");
			fErrors.addElement(new String[] {testName, trace} );
		}
		else
		{
			writer().print("F");
			fFailures.addElement(new String[] {testName, trace} );
		}
	}

	/**
	 * Prints failures to the standard output
	 */
	public synchronized void showSummary(long elapsedTime) {
		writer().println();
		writer().println("Finished after: " + elapsedTimeAsString(elapsedTime) + " seconds.");
	    printErrors();
	    printFailures();
	    printHeader();
	}
	
	/**
	 * Prints the errors to the standard output
	 */
	public void printErrors() {
		int size= fErrors.size();
	    if (size != 0) {
	        if (size == 1)
		        writer().println("There was "+ size +" error:");
	        else
		        writer().println("There were "+ size +" errors:");

			int i= 1;
			for (Enumeration e= fErrors.elements(); e.hasMoreElements(); i++) {
			    String[] error= (String[]) e.nextElement();
				writer().println(i + ") " + error[0]);
				writer().print(filterStack(error[1]));
		    }
		}
	}
	
	/**
	 * Prints failures to the standard output
	 */
	public void printFailures() {
		int size= fFailures.size();
	    if (size != 0) {
	        if (size == 1)
				writer().println("There was " + size + " failure:");
			else
				writer().println("There were " + size + " failures:");
				
			int i = 1;
			for (Enumeration e= fFailures.elements(); e.hasMoreElements(); i++) {
			    String[] failure= (String[]) e.nextElement();
				writer().println(i + ") " + failure[0]);
				writer().print(filterStack(failure[1]));
			}
		}
	}
	
	/**
	 * Prints the header of the report
	 */
	public void printHeader() {
		if (fErrors.size() + fFailures.size() == 0) {
			writer().println();
			writer().print("OK");
			writer().println (" (" + fExecutedTests + '/' + fTestCount + " tests)");

		} else {
			writer().println();
			writer().println("FAILURES!!!");
			writer().println("Tests run: "+ fTestCount + 
				         ",  Failures: "+ fFailures.size() +
				         ",  Errors: "+ fErrors.size() );
		}
	}

	protected PrintStream writer() {
		return fWriter;
	}

	/**
	 * Returns a filtered stack trace
	 */
	public static String getTrace(Throwable t) { 
		StringWriter stringWriter= new StringWriter();
		PrintWriter writer= new PrintWriter(stringWriter);
		t.printStackTrace(writer);
		StringBuffer buffer= stringWriter.getBuffer();
		return buffer.toString();
	}

	protected String filterStack(String stackTrace) {	
		if (stackTrace == null) 
			return stackTrace;
			
		StringWriter stringWriter= new StringWriter();
		PrintWriter printWriter= new PrintWriter(stringWriter);
		StringReader stringReader= new StringReader(stackTrace);
		BufferedReader bufferedReader= new BufferedReader(stringReader);
		
		String line;
		try {	
			while ((line= bufferedReader.readLine()) != null) {
				if (!filterLine(line))
					printWriter.println(line);
			}
		} catch (Exception IOException) {
			return stackTrace; // return the stack unfiltered
		}
		return stringWriter.toString();
	}
	
	protected static boolean filterLine(String line) {
		for (int i= 0; i < fgFilterPatterns.length; i++)
			if (line.indexOf(fgFilterPatterns[i]) > 0)
				return true;	
					
		return false;
	}

	/**
	 * Returns the formatted string of the elapsed time.
	 */
	protected static String elapsedTimeAsString(long runTime) {
		return NumberFormat.getInstance().format((double)runTime/1000);
	}
}

