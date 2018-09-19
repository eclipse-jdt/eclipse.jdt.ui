/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.search;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.search.IQueryParticipant;

import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchResult;
import org.eclipse.jdt.internal.ui.search.SearchParticipantDescriptor;
import org.eclipse.jdt.internal.ui.search.SearchParticipantRecord;
import org.eclipse.jdt.internal.ui.search.SearchParticipantsExtensionPoint;

/**
 */
public class ParticipantTest extends TestCase {

	private static Class<ParticipantTest> THIS= ParticipantTest.class;

	static class TestExtensionPoint extends SearchParticipantsExtensionPoint {
		@Override
		public SearchParticipantRecord[] getSearchParticipants(IProject[] concernedProjects) {
			return new SearchParticipantRecord[] { new SearchParticipantRecord(new TestParticipantRecord(), new TestParticipant()) };
		}
	}

	static class TestParticipantRecord extends SearchParticipantDescriptor {

		TestParticipantRecord() {
			super(null);
		}

		@Override
		protected IStatus checkSyntax() {
			return Status.OK_STATUS;
		}

		@Override
		protected IQueryParticipant create() throws CoreException {
			return new TestParticipant();
		}

		@Override
		public String getID() {
			return "TestParticipant1 ID";
		}

		@Override
		protected String getNature() {
			return JavaCore.NATURE_ID;
		}
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new JUnitSourceSetup(test, new TestExtensionPoint());
	}

	public ParticipantTest(String name) {
		super(name);
	}

	public void testSimpleParticipant() throws Exception {
		JavaSearchQuery query= SearchTestHelper.runMethodRefQuery("frufru");
		JavaSearchResult result= (JavaSearchResult) query.getSearchResult();
		assertEquals(20, result.getMatchCount());

		Object[] elements= result.getElements();
		for (int i= 0; i < elements.length; i++) {
			assertTrue(elements[i] instanceof Integer);
		}
	}

}
