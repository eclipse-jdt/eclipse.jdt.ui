/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *     Neale Upstone <neale@nealeupstone.com> - [JUnit] JUnit viewer doesn't recognise <skipped/> node - https://bugs.eclipse.org/bugs/show_bug.cgi?id=276068
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.model;

import java.util.Arrays;
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
	private Stack<Boolean> fNotRun= new Stack<>();

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

	@Override
	public void setDocumentLocator(Locator locator) {
		fLocator= locator;
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (fLocator != null && fMonitor != null) {
			int line= fLocator.getLineNumber();
			if (line - 20 >= fLastReportedLine) {
				line -= line % 20;
				fLastReportedLine= line;
				fMonitor.subTask(NLS.bind(ModelMessages.TestRunHandler_lines_read, Integer.valueOf(line)));
			}
		}
		if (Thread.interrupted())
			throw new OperationCanceledException();

		switch (qName) {
		case IXMLTags.NODE_TESTRUN:
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
				String includeTags= attributes.getValue(IXMLTags.ATTR_INCLUDE_TAGS);
				if (includeTags != null && includeTags.trim().length() > 0) {
					fTestRunSession.setIncludeTags(includeTags);
				}
				String excludeTags= attributes.getValue(IXMLTags.ATTR_EXCLUDE_TAGS);
				if (excludeTags != null && excludeTags.trim().length() > 0) {
					fTestRunSession.setExcludeTags(excludeTags);
				}
				//TODO: read counts?

			} else {
				fTestRunSession.reset();
			}
			fTestSuite= fTestRunSession.getTestRoot();
			break;
		// support Ant's 'junitreport' task; create suite from NODE_TESTSUITE
		case IXMLTags.NODE_TESTSUITES:
			break;
		case IXMLTags.NODE_TESTSUITE:
			{
				String name= attributes.getValue(IXMLTags.ATTR_NAME);
				if (fTestRunSession == null) {
					// support standalone suites and Ant's 'junitreport' task:
					fTestRunSession= new TestRunSession(name, null);
					fTestSuite= fTestRunSession.getTestRoot();
				}	String pack= attributes.getValue(IXMLTags.ATTR_PACKAGE);
				String suiteName= pack == null ? name : pack + "." + name; //$NON-NLS-1$
				String displayName= attributes.getValue(IXMLTags.ATTR_DISPLAY_NAME);
				String paramTypesStr= attributes.getValue(IXMLTags.ATTR_PARAMETER_TYPES);
				String[] paramTypes;
				if (paramTypesStr != null && !paramTypesStr.trim().isEmpty()) {
					paramTypes= paramTypesStr.split(","); //$NON-NLS-1$
					Arrays.parallelSetAll(paramTypes, i -> paramTypes[i].trim());
				} else {
					paramTypes= null;
				}	String uniqueId= attributes.getValue(IXMLTags.ATTR_UNIQUE_ID);
				if (uniqueId != null && uniqueId.trim().isEmpty()) {
					uniqueId= null;
				}	fTestSuite= (TestSuiteElement) fTestRunSession.createTestElement(fTestSuite, getNextId(), suiteName, true, 0, false, displayName, paramTypes, uniqueId);
				readTime(fTestSuite, attributes);
				fNotRun.push(Boolean.valueOf(attributes.getValue(IXMLTags.ATTR_INCOMPLETE)));
				break;
			}
		// not interested
		case IXMLTags.NODE_PROPERTIES:
		case IXMLTags.NODE_PROPERTY:
			break;
		case IXMLTags.NODE_TESTCASE:
			{
				String name= attributes.getValue(IXMLTags.ATTR_NAME);
				String classname= attributes.getValue(IXMLTags.ATTR_CLASSNAME);
				String testName= name + '(' + classname + ')';
				boolean isDynamicTest= Boolean.parseBoolean(attributes.getValue(IXMLTags.ATTR_DYNAMIC_TEST));
				String displayName= attributes.getValue(IXMLTags.ATTR_DISPLAY_NAME);
				String paramTypesStr= attributes.getValue(IXMLTags.ATTR_PARAMETER_TYPES);
				String[] paramTypes;
				if (paramTypesStr != null && !paramTypesStr.trim().isEmpty()) {
					paramTypes= paramTypesStr.split(","); //$NON-NLS-1$
					Arrays.parallelSetAll(paramTypes, i -> paramTypes[i].trim());
				} else {
					paramTypes= null;
				}	String uniqueId= attributes.getValue(IXMLTags.ATTR_UNIQUE_ID);
				if (uniqueId != null && uniqueId.trim().isEmpty()) {
					uniqueId= null;
				}	fTestCase= (TestCaseElement) fTestRunSession.createTestElement(fTestSuite, getNextId(), testName, false, 0, isDynamicTest, displayName, paramTypes, uniqueId);
				fNotRun.push(Boolean.parseBoolean(attributes.getValue(IXMLTags.ATTR_INCOMPLETE)));
				fTestCase.setIgnored(Boolean.parseBoolean(attributes.getValue(IXMLTags.ATTR_IGNORED)));
				readTime(fTestCase, attributes);
				break;
			}
		case IXMLTags.NODE_ERROR:
			//TODO: multiple failures: https://bugs.eclipse.org/bugs/show_bug.cgi?id=125296
			fStatus= Status.ERROR;
			fFailureBuffer= new StringBuffer();
			break;
		case IXMLTags.NODE_FAILURE:
			//TODO: multiple failures: https://bugs.eclipse.org/bugs/show_bug.cgi?id=125296
			fStatus= Status.FAILURE;
			fFailureBuffer= new StringBuffer();
			break;
		case IXMLTags.NODE_EXPECTED:
			fInExpected= true;
			fExpectedBuffer= new StringBuffer();
			break;
		case IXMLTags.NODE_ACTUAL:
			fInActual= true;
			fActualBuffer= new StringBuffer();
			break;
		// not interested
		case IXMLTags.NODE_SYSTEM_OUT:
		case IXMLTags.NODE_SYSTEM_ERR:
			break;
		case IXMLTags.NODE_ABORTED:		// fall through to the skipped
		case IXMLTags.NODE_SKIPPED:
			// before Ant 1.9.0: not an Ant JUnit tag, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=276068
			// later: child of <suite> or <test>, see https://issues.apache.org/bugzilla/show_bug.cgi?id=43969
			fStatus= Status.OK;
			fFailureBuffer= new StringBuffer();
			String message= attributes.getValue(IXMLTags.ATTR_MESSAGE);
			if (message != null) {
				fFailureBuffer.append(message).append('\n');
			}
			break;
		default:
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

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (fInExpected) {
			fExpectedBuffer.append(ch, start, length);

		} else if (fInActual) {
			fActualBuffer.append(ch, start, length);

		} else if (fFailureBuffer != null) {
			fFailureBuffer.append(ch, start, length);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		switch (qName) {
		// OK
		case IXMLTags.NODE_TESTRUN:
			break;
		// OK
		case IXMLTags.NODE_TESTSUITES:
			break;
		case IXMLTags.NODE_TESTSUITE:
			handleTestElementEnd(fTestSuite);
			fTestSuite= fTestSuite.getParent();
			//TODO: end suite: compare counters?
			break;
		// OK
		case IXMLTags.NODE_PROPERTIES:
		case IXMLTags.NODE_PROPERTY:
			break;
		case IXMLTags.NODE_TESTCASE:
			handleTestElementEnd(fTestCase);
			fTestCase= null;
			break;
		case IXMLTags.NODE_FAILURE:
		case IXMLTags.NODE_ERROR:
			{
				TestElement testElement= fTestCase;
				if (testElement == null)
					testElement= fTestSuite;
				handleFailure(testElement);
				break;
			}
		case IXMLTags.NODE_EXPECTED:
			fInExpected= false;
			if (fFailureBuffer != null) {
				// skip whitespace from before <expected> and <actual> nodes
				fFailureBuffer.setLength(0);
			}
			break;
		case IXMLTags.NODE_ACTUAL:
			fInActual= false;
			if (fFailureBuffer != null) {
				// skip whitespace from before <expected> and <actual> nodes
				fFailureBuffer.setLength(0);
			}
			break;
		// OK
		case IXMLTags.NODE_SYSTEM_OUT:
		case IXMLTags.NODE_SYSTEM_ERR:
			break;
		case IXMLTags.NODE_ABORTED:		// fall through to the skipped
		case IXMLTags.NODE_SKIPPED:
			{
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
				}	break;
			}
		default:
			handleUnknownNode(qName);
			break;
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
		StringBuilder msg= new StringBuilder("unknown node '").append(qName).append("'"); //$NON-NLS-1$//$NON-NLS-2$
		if (fLocator != null) {
			msg.append(" at line ").append(fLocator.getLineNumber()).append(", column ").append(fLocator.getColumnNumber());  //$NON-NLS-1$//$NON-NLS-2$
		}
		throw new SAXException(msg.toString());
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		throw e;
	}

	@Override
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
