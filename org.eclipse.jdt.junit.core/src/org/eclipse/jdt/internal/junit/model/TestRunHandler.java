/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *     Neale Upstone <neale@nealeupstone.com> - [JUnit] JUnit viewer doesn't recognise <skipped/> node - https://bugs.eclipse.org/bugs/show_bug.cgi?id=276068
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.model;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.eclipse.osgi.util.NLS;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.model.TestElement.Status;

public class TestRunHandler extends DefaultHandler {

	/*
	 * TODO: validate (currently assumes correct XML)
	 */

	private int fId;

	private TestRunSession fTestRunSession;
	private TestSuiteElement fTestSuite;
	private TestCaseElement fTestCase;
	private Stack/*<Boolean>*/ fNotRun= new Stack();

	private StringBuffer fFailureBuffer;
	private boolean fInExpected;
	private boolean fInActual;
	private StringBuffer fExpectedBuffer;
	private StringBuffer fActualBuffer;

	private Locator fLocator;

	private Status fStatus;

	private IProgressMonitor fMonitor;
	private int fLastReportedLine;

	public TestRunHandler() {

	}

	public TestRunHandler(IProgressMonitor monitor) {
		fMonitor= monitor;
	}
	
	public TestRunHandler(TestRunSession testRunSession) {
		fTestRunSession= testRunSession;
	}

	public void setDocumentLocator(Locator locator) {
		fLocator= locator;
	}

	public void startDocument() throws SAXException {
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (fLocator != null && fMonitor != null) {
			int line= fLocator.getLineNumber();
			if (line - 20 >= fLastReportedLine) {
				line -= line % 20;
				fLastReportedLine= line;
				fMonitor.subTask(NLS.bind(ModelMessages.TestRunHandler_lines_read, new Integer(line)));
			}
		}
		if (Thread.interrupted())
			throw new OperationCanceledException();
		
		if (qName.equals(IXMLTags.NODE_TESTRUN)) {
			if (fTestRunSession == null) {
				String name= attributes.getValue(IXMLTags.ATTR_NAME);
				String project= attributes.getValue(IXMLTags.ATTR_PROJECT);
				IJavaProject javaProject= null;
				if (project != null) {
					IJavaModel javaModel= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
					javaProject= javaModel.getJavaProject(project);
					if (! javaProject.exists())
						javaProject= null;
				}
				fTestRunSession= new TestRunSession(name, javaProject);
				//TODO: read counts?

			} else {
				fTestRunSession.reset();
			}
			fTestSuite= fTestRunSession.getTestRoot();

		} else if (qName.equals(IXMLTags.NODE_TESTSUITES)) {
			// support Ant's 'junitreport' task; create suite from NODE_TESTSUITE

		} else if (qName.equals(IXMLTags.NODE_TESTSUITE)) {
			String name= attributes.getValue(IXMLTags.ATTR_NAME);

			if (fTestRunSession == null) {
				// support standalone suites and Ant's 'junitreport' task:
				fTestRunSession= new TestRunSession(name, null);
				fTestSuite= fTestRunSession.getTestRoot();
			}

			String pack= attributes.getValue(IXMLTags.ATTR_PACKAGE);
			String suiteName= pack == null ? name : pack + "." + name; //$NON-NLS-1$
			fTestSuite= (TestSuiteElement) fTestRunSession.createTestElement(fTestSuite, getNextId(), suiteName, true, 0);
			readTime(fTestSuite, attributes);
			fNotRun.push(Boolean.valueOf(attributes.getValue(IXMLTags.ATTR_INCOMPLETE)));

		} else if (qName.equals(IXMLTags.NODE_PROPERTIES) || qName.equals(IXMLTags.NODE_PROPERTY)) {
			// not interested

		} else if (qName.equals(IXMLTags.NODE_TESTCASE)) {
			String name= attributes.getValue(IXMLTags.ATTR_NAME);
			String classname= attributes.getValue(IXMLTags.ATTR_CLASSNAME);
			fTestCase= (TestCaseElement) fTestRunSession.createTestElement(fTestSuite, getNextId(), name + '(' + classname + ')', false, 0);
			fNotRun.push(Boolean.valueOf(attributes.getValue(IXMLTags.ATTR_INCOMPLETE)));
			fTestCase.setIgnored(Boolean.valueOf(attributes.getValue(IXMLTags.ATTR_IGNORED)).booleanValue());
			readTime(fTestCase, attributes);

		} else if (qName.equals(IXMLTags.NODE_ERROR)) {
			//TODO: multiple failures: https://bugs.eclipse.org/bugs/show_bug.cgi?id=125296
			fStatus= Status.ERROR;
			fFailureBuffer= new StringBuffer();

		} else if (qName.equals(IXMLTags.NODE_FAILURE)) {
			//TODO: multiple failures: https://bugs.eclipse.org/bugs/show_bug.cgi?id=125296
			fStatus= Status.FAILURE;
			fFailureBuffer= new StringBuffer();

		} else if (qName.equals(IXMLTags.NODE_EXPECTED)) {
			fInExpected= true;
			fExpectedBuffer= new StringBuffer();

		} else if (qName.equals(IXMLTags.NODE_ACTUAL)) {
			fInActual= true;
			fActualBuffer= new StringBuffer();

		} else if (qName.equals(IXMLTags.NODE_SYSTEM_OUT) || qName.equals(IXMLTags.NODE_SYSTEM_ERR)) {
			// not interested

		} else if (qName.equals(IXMLTags.NODE_SKIPPED)) {
			// before Ant 1.9.0: not an Ant JUnit tag, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=276068
			// later: child of <suite> or <test>, see https://issues.apache.org/bugzilla/show_bug.cgi?id=43969
			fStatus= Status.OK;
			fFailureBuffer= new StringBuffer();
			String message= attributes.getValue(IXMLTags.ATTR_MESSAGE);
			if (message != null) {
				fFailureBuffer.append(message).append('\n');
			}

		} else {
			throw new SAXParseException("unknown node '" + qName + "'", fLocator);  //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	private void readTime(TestElement testElement, Attributes attributes) {
		String timeString= attributes.getValue(IXMLTags.ATTR_TIME);
		if (timeString != null) {
			try {
				testElement.setElapsedTimeInSeconds(Double.parseDouble(timeString));
			} catch (NumberFormatException e) {
			}
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		if (fInExpected) {
			fExpectedBuffer.append(ch, start, length);

		} else if (fInActual) {
			fActualBuffer.append(ch, start, length);

		} else if (fFailureBuffer != null) {
			fFailureBuffer.append(ch, start, length);
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equals(IXMLTags.NODE_TESTRUN)) {
			// OK

		} else if (qName.equals(IXMLTags.NODE_TESTSUITES)) {
			// OK

		} else if (qName.equals(IXMLTags.NODE_TESTSUITE)) {
			handleTestElementEnd(fTestSuite);
			fTestSuite= fTestSuite.getParent();
			//TODO: end suite: compare counters?

		} else if (qName.equals(IXMLTags.NODE_PROPERTIES) || qName.equals(IXMLTags.NODE_PROPERTY)) {
			// OK

		} else if (qName.equals(IXMLTags.NODE_TESTCASE)) {
			handleTestElementEnd(fTestCase);
			fTestCase= null;

		} else if (qName.equals(IXMLTags.NODE_FAILURE) || qName.equals(IXMLTags.NODE_ERROR)) {
			TestElement testElement= fTestCase;
			if (testElement == null)
				testElement= fTestSuite;
			handleFailure(testElement);

		} else if (qName.equals(IXMLTags.NODE_EXPECTED)) {
			fInExpected= false;
			if (fFailureBuffer != null) {
				// skip whitespace from before <expected> and <actual> nodes
				fFailureBuffer.setLength(0);
			}

		} else if (qName.equals(IXMLTags.NODE_ACTUAL)) {
			fInActual= false;
			if (fFailureBuffer != null) {
				// skip whitespace from before <expected> and <actual> nodes
				fFailureBuffer.setLength(0);
			}

		} else if (qName.equals(IXMLTags.NODE_SYSTEM_OUT) || qName.equals(IXMLTags.NODE_SYSTEM_ERR)) {
			// OK

		} else if (qName.equals(IXMLTags.NODE_SKIPPED)) {
			TestElement testElement= fTestCase;
			if (testElement == null)
				testElement= fTestSuite;
			
			if (fFailureBuffer != null && fFailureBuffer.length() > 0) {
				handleFailure(testElement);
				testElement.setAssumptionFailed(true);
			} else if (fTestCase != null) {
				fTestCase.setIgnored(true);
			} else { // not expected
				testElement.setAssumptionFailed(true);
			}

		} else {

			handleUnknownNode(qName);
		}
	}

	private void handleTestElementEnd(TestElement testElement) {
		boolean completed= fNotRun.pop() != Boolean.TRUE;
		fTestRunSession.registerTestEnded(testElement, completed);
	}

	private void handleFailure(TestElement testElement) {
		if (fFailureBuffer != null) {
			fTestRunSession.registerTestFailureStatus(testElement, fStatus, fFailureBuffer.toString(), toString(fExpectedBuffer), toString(fActualBuffer));
			fFailureBuffer= null;
			fExpectedBuffer= null;
			fActualBuffer= null;
			fStatus= null;
		}
	}

	private String toString(StringBuffer buffer) {
		return buffer != null ? buffer.toString() : null;
	}

	private void handleUnknownNode(String qName) throws SAXException {
		//TODO: just log if debug option is enabled?
		String msg= "unknown node '" + qName + "'"; //$NON-NLS-1$//$NON-NLS-2$
		if (fLocator != null) {
			msg += " at line " + fLocator.getLineNumber() + ", column " + fLocator.getColumnNumber();  //$NON-NLS-1$//$NON-NLS-2$
		}
		throw new SAXException(msg);
	}

	public void error(SAXParseException e) throws SAXException {
		throw e;
	}

	public void warning(SAXParseException e) throws SAXException {
		throw e;
	}

	private String getNextId() {
		return Integer.toString(fId++);
	}

	/**
	 * @return the parsed test run session, or <code>null</code>
	 */
	public TestRunSession getTestRunSession() {
		return fTestRunSession;
	}
}
