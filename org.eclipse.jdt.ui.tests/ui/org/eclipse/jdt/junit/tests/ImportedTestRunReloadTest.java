/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial tests
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunSession;

public class ImportedTestRunReloadTest {

	private Path fTemporaryDirectory;
	private final List<TestRunSession> fSessionsToRemove= new ArrayList<>();

	@Before
	public void setUp() throws Exception {
		fTemporaryDirectory= Files.createTempDirectory("junit-reload-test"); //$NON-NLS-1$
	}

	@After
	public void tearDown() throws Exception {
		JUnitModel model= JUnitCorePlugin.getModel();
		for (TestRunSession session : fSessionsToRemove) {
			if (session != null && model.getTestRunSessions().contains(session)) {
				model.removeTestRunSession(session);
			}
		}
		if (fTemporaryDirectory != null) {
			try (Stream<Path> paths= Files.walk(fTemporaryDirectory)) {
				paths.sorted(Comparator.reverseOrder()).forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (Exception e) {
						// Best-effort cleanup.
					}
				});
			}
		}
	}

	@Test
	public void testReloadReplacesSessionAtSameHistoryPosition() throws Exception {
		Path firstFile= fTemporaryDirectory.resolve("first.xml"); //$NON-NLS-1$
		Path secondFile= fTemporaryDirectory.resolve("second.xml"); //$NON-NLS-1$
		writeTestRun(firstFile, "first run", "firstTest"); //$NON-NLS-1$ //$NON-NLS-2$
		writeTestRun(secondFile, "second run", "secondTest"); //$NON-NLS-1$ //$NON-NLS-2$

		JUnitModel model= JUnitCorePlugin.getModel();
		TestRunSession first= JUnitModel.importTestRunSession(firstFile.toFile());
		TestRunSession second= JUnitModel.importTestRunSession(secondFile.toFile());
		fSessionsToRemove.add(first);
		fSessionsToRemove.add(second);

		List<TestRunSession> beforeReload= model.getTestRunSessions();
		int firstIndex= beforeReload.indexOf(first);
		int secondIndex= beforeReload.indexOf(second);
		assertTrue(firstIndex >= 0);
		assertTrue(secondIndex >= 0);
		assertEquals(firstFile.toAbsolutePath().normalize().toFile(), model.getImportedTestRunSource(first));

		writeTestRun(firstFile, "reloaded run", "reloadedTest"); //$NON-NLS-1$ //$NON-NLS-2$
		TestRunSession reloaded= JUnitModel.reloadTestRunSession(first);
		fSessionsToRemove.add(reloaded);

		List<TestRunSession> afterReload= model.getTestRunSessions();
		assertEquals(beforeReload.size(), afterReload.size());
		assertSame(second, afterReload.get(secondIndex));
		assertSame(reloaded, afterReload.get(firstIndex));
		assertFalse(afterReload.contains(first));
		assertEquals(firstFile.toAbsolutePath().normalize().toFile(), model.getImportedTestRunSource(reloaded));
		assertEquals("reloaded run", reloaded.getTestRunName()); //$NON-NLS-1$
	}

	@Test
	public void testReloadRemovedSessionReturnsNull() throws Exception {
		Path resultFile= fTemporaryDirectory.resolve("removed.xml"); //$NON-NLS-1$
		writeTestRun(resultFile, "removed run", "test"); //$NON-NLS-1$ //$NON-NLS-2$

		JUnitModel model= JUnitCorePlugin.getModel();
		TestRunSession imported= JUnitModel.importTestRunSession(resultFile.toFile());
		fSessionsToRemove.add(imported);
		model.removeTestRunSession(imported);

		assertNull(JUnitModel.reloadTestRunSession(imported));
		assertFalse(model.getTestRunSessions().contains(imported));
	}

	@Test
	public void testFailedReloadKeepsPreviousSession() throws Exception {
		Path resultFile= fTemporaryDirectory.resolve("results.xml"); //$NON-NLS-1$
		writeTestRun(resultFile, "valid run", "test"); //$NON-NLS-1$ //$NON-NLS-2$

		JUnitModel model= JUnitCorePlugin.getModel();
		TestRunSession imported= JUnitModel.importTestRunSession(resultFile.toFile());
		fSessionsToRemove.add(imported);
		List<TestRunSession> beforeReload= model.getTestRunSessions();
		int importedIndex= beforeReload.indexOf(imported);

		Files.writeString(resultFile, "<testrun", StandardCharsets.UTF_8); //$NON-NLS-1$
		try {
			JUnitModel.reloadTestRunSession(imported);
			fail("Expected invalid XML to fail"); //$NON-NLS-1$
		} catch (CoreException expected) {
			// Expected.
		}

		List<TestRunSession> afterReload= model.getTestRunSessions();
		assertEquals(beforeReload.size(), afterReload.size());
		assertSame(imported, afterReload.get(importedIndex));
		assertEquals(resultFile.toAbsolutePath().normalize().toFile(), model.getImportedTestRunSource(imported));
		assertEquals("valid run", imported.getTestRunName()); //$NON-NLS-1$
	}

	private static void writeTestRun(Path file, String runName, String testName) throws Exception {
		String xml= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
				+ "<testrun name=\"" + runName //$NON-NLS-1$
				+ "\" tests=\"1\" started=\"1\" failures=\"0\" errors=\"0\" ignored=\"0\">\n" //$NON-NLS-1$
				+ "  <testsuite name=\"example.Tests\" time=\"0.0\">\n" //$NON-NLS-1$
				+ "    <testcase name=\"" + testName //$NON-NLS-1$
				+ "\" classname=\"example.Tests\" time=\"0.0\"/>\n" //$NON-NLS-1$
				+ "  </testsuite>\n" //$NON-NLS-1$
				+ "</testrun>\n"; //$NON-NLS-1$
		Files.writeString(file, xml, StandardCharsets.UTF_8);
	}
}
