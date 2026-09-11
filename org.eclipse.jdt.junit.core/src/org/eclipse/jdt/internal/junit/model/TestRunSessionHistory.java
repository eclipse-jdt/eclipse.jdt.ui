/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.model.ITestElement.Result;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;


/**
 * Persists the bounded JUnit test-run history independently of the transient
 * swap files used by {@link TestRunSession} while Eclipse is running.
 */
public final class TestRunSessionHistory {

	private static final int FORMAT_VERSION= 2;
	private static final int MAX_INDEX_ENTRIES= 10_000;

	private static final String INDEX_FILE_NAME= "history.properties"; //$NON-NLS-1$
	private static final String HISTORY_FILE_PREFIX= "history-"; //$NON-NLS-1$
	private static final String SWAP_FILE_PREFIX= "swap-"; //$NON-NLS-1$
	private static final String XML_SUFFIX= ".xml"; //$NON-NLS-1$
	private static final String TEMP_SUFFIX= ".tmp"; //$NON-NLS-1$

	private static final String KEY_FORMAT_VERSION= "formatVersion"; //$NON-NLS-1$
	private static final String KEY_ENTRY_COUNT= "entryCount"; //$NON-NLS-1$
	private static final String KEY_ID= "id"; //$NON-NLS-1$
	private static final String KEY_NAME= "name"; //$NON-NLS-1$
	private static final String KEY_PROJECT= "project"; //$NON-NLS-1$
	private static final String KEY_LAUNCH_CONFIGURATION_MEMENTO= "launchConfigurationMemento"; //$NON-NLS-1$
	private static final String KEY_LAUNCH_MODE= "launchMode"; //$NON-NLS-1$
	private static final String KEY_START_TIME= "startTime"; //$NON-NLS-1$
	private static final String KEY_HISTORY_TIMESTAMP= "historyTimestamp"; //$NON-NLS-1$
	private static final String KEY_PROGRESS= "progress"; //$NON-NLS-1$
	private static final String KEY_RESULT= "result"; //$NON-NLS-1$
	private static final String KEY_TOTAL_COUNT= "totalCount"; //$NON-NLS-1$
	private static final String KEY_STARTED_COUNT= "startedCount"; //$NON-NLS-1$
	private static final String KEY_FAILURE_COUNT= "failureCount"; //$NON-NLS-1$
	private static final String KEY_ERROR_COUNT= "errorCount"; //$NON-NLS-1$
	private static final String KEY_IGNORED_COUNT= "ignoredCount"; //$NON-NLS-1$
	private static final String KEY_ASSUMPTION_FAILURE_COUNT= "assumptionFailureCount"; //$NON-NLS-1$
	private static final String KEY_INCLUDE_TAGS= "includeTags"; //$NON-NLS-1$
	private static final String KEY_EXCLUDE_TAGS= "excludeTags"; //$NON-NLS-1$
	private static final String KEY_FILE_LENGTH= "fileLength"; //$NON-NLS-1$

	private static final String PROGRESS_COMPLETED= "completed"; //$NON-NLS-1$
	private static final String PROGRESS_STOPPED= "stopped"; //$NON-NLS-1$

	private static final ILog LOG= ILog.of(TestRunSessionHistory.class);
	private static final Set<Path> BLOCKED_HISTORY_DIRECTORIES= ConcurrentHashMap.newKeySet();

	private TestRunSessionHistory() {
	}

	/**
	 * Restores at most {@code maxCount} history entries. Only the small index is
	 * read here; the test trees remain in their XML files until selected.
	 *
	 * @param historyDirectory the JUnit history directory
	 * @param maxCount the maximum number of entries to restore
	 * @return the restored sessions, youngest first
	 */
	public static List<TestRunSession> load(File historyDirectory, int maxCount) {
		Path historyPath= historyDirectoryPath(historyDirectory);
		deleteTemporaryFiles(historyDirectory);
		deleteTransientSwapFiles(historyDirectory);

		File indexFile= new File(historyDirectory, INDEX_FILE_NAME);
		if (!indexFile.exists()) {
			BLOCKED_HISTORY_DIRECTORIES.remove(historyPath);
			deleteOrphanedXmlFiles(historyDirectory, Set.of(), true);
			return List.of();
		}
		if (!indexFile.isFile()) {
			BLOCKED_HISTORY_DIRECTORIES.add(historyPath);
			LOG.error("Could not read the JUnit history index: " + indexFile); //$NON-NLS-1$
			return List.of();
		}

		Properties properties= new Properties();
		try (BufferedInputStream input= new BufferedInputStream(new FileInputStream(indexFile))) {
			properties.load(input);
		} catch (IOException | IllegalArgumentException e) {
			BLOCKED_HISTORY_DIRECTORIES.add(historyPath);
			LOG.error("Could not read the JUnit history index", e); //$NON-NLS-1$
			return List.of();
		}

		int formatVersion;
		int entryCount;
		try {
			formatVersion= readInt(properties, KEY_FORMAT_VERSION, 0, Integer.MAX_VALUE);
			entryCount= readInt(properties, KEY_ENTRY_COUNT, 0, MAX_INDEX_ENTRIES);
		} catch (IllegalArgumentException e) {
			BLOCKED_HISTORY_DIRECTORIES.add(historyPath);
			LOG.error("Invalid JUnit history index", e); //$NON-NLS-1$
			return List.of();
		}
		if (formatVersion != FORMAT_VERSION) {
			BLOCKED_HISTORY_DIRECTORIES.add(historyPath);
			LOG.error("Unsupported JUnit history index version: " + formatVersion); //$NON-NLS-1$
			return List.of();
		}
		BLOCKED_HISTORY_DIRECTORIES.remove(historyPath);

		int limit= Math.max(0, maxCount);
		List<StoredSession> validEntries= new ArrayList<>(entryCount);
		Set<String> referencedFiles= new HashSet<>();
		Set<String> seenIds= new HashSet<>();
		boolean indexFullyUnderstood= true;
		for (int i= 0; i < entryCount; i++) {
			try {
				StoredSession storedSession= readEntry(properties, i, historyDirectory);
				if (!seenIds.add(storedSession.fId))
					throw new IllegalArgumentException("Duplicate JUnit history identifier: " + storedSession.fId); //$NON-NLS-1$
				File historyFile= storedSession.fHistoryFile;
				if (!historyFile.isFile() || historyFile.length() != storedSession.fFileLength) {
					LOG.error("Ignoring incomplete JUnit history file: " + historyFile); //$NON-NLS-1$
					indexFullyUnderstood= false;
					continue;
				}
				referencedFiles.add(historyFile.getName());
				validEntries.add(storedSession);
			} catch (IllegalArgumentException e) {
				indexFullyUnderstood= false;
				BLOCKED_HISTORY_DIRECTORIES.add(historyPath);
				LOG.error("Invalid JUnit history entry " + i, e); //$NON-NLS-1$
			}
		}

		if (indexFullyUnderstood)
			deleteOrphanedXmlFiles(historyDirectory, referencedFiles, false);

		validEntries.sort(Comparator.comparingLong((StoredSession entry) -> entry.fHistoryTimestamp).reversed());
		List<TestRunSession> sessions= new ArrayList<>(Math.min(limit, validEntries.size()));
		for (StoredSession storedSession : validEntries) {
			if (sessions.size() >= limit)
				break;
			sessions.add(new RestoredTestRunSession(storedSession));
		}
		return sessions;
	}

	/**
	 * Atomically persists at most {@code maxCount} completed sessions and then
	 * atomically publishes the new history index. Files not referenced by the
	 * new index are removed only after the index has been committed.
	 *
	 * @param sessions sessions ordered youngest first
	 * @param historyDirectory the JUnit history directory
	 * @param maxCount the maximum number of entries to retain
	 */
	public static void store(List<TestRunSession> sessions, File historyDirectory, int maxCount) {
		if (BLOCKED_HISTORY_DIRECTORIES.contains(historyDirectoryPath(historyDirectory))) {
			LOG.error("Keeping the existing JUnit history because its index could not be read"); //$NON-NLS-1$
			return;
		}

		try {
			Files.createDirectories(historyDirectory.toPath());
		} catch (IOException e) {
			LOG.error("Could not create the JUnit history directory", e); //$NON-NLS-1$
			return;
		}

		int limit= Math.max(0, maxCount);
		List<StoredSession> storedSessions= new ArrayList<>(Math.min(limit, sessions.size()));
		boolean persistenceFailure= false;
		for (TestRunSession session : sessions) {
			if (storedSessions.size() >= limit)
				break;
			if (!isPersistable(session))
				continue;

			StoredSession storedSession= null;
			if (session instanceof RestoredTestRunSession restoredSession) {
				storedSession= restoredSession.reusableStoredSession();
				if (storedSession == null && !restoredSession.canWriteFromMemory()) {
					persistenceFailure= true;
					continue;
				}
			}
			if (storedSession == null) {
				try {
					storedSession= writeSessionAtomically(session, historyDirectory);
				} catch (CoreException | IOException | RuntimeException e) {
					persistenceFailure= true;
					LOG.error("Could not persist JUnit test run history", e); //$NON-NLS-1$
				}
			}
			if (storedSession != null)
				storedSessions.add(storedSession);
		}

		if (persistenceFailure) {
			LOG.error("Keeping the previous JUnit history because not all sessions could be persisted"); //$NON-NLS-1$
			return;
		}

		if (!writeIndexAtomically(storedSessions, historyDirectory))
			return;

		Set<String> retainedFiles= new HashSet<>();
		for (StoredSession storedSession : storedSessions)
			retainedFiles.add(storedSession.fHistoryFile.getName());
		deleteOrphanedXmlFiles(historyDirectory, retainedFiles, true);
	}

	private static boolean isPersistable(TestRunSession session) {
		if (session instanceof RestoredTestRunSession restoredSession && !restoredSession.fValid)
			return false;
		return session.getStartTime() != 0
				&& !session.isRunning()
				&& !session.isStarting()
				&& !session.isKeptAlive();
	}

	static void exportAtomically(TestRunSession session, File targetFile) throws CoreException, IOException {
		File directory= targetFile.getParentFile();
		Files.createDirectories(directory.toPath());
		File temporaryFile= Files.createTempFile(directory.toPath(), targetFile.getName() + '.', TEMP_SUFFIX).toFile();
		try {
			JUnitCore.exportTestRunSession(session, temporaryFile);
			if (session.hasSwapInFailed())
				throw new IOException("Could not load the complete JUnit test tree"); //$NON-NLS-1$
			moveReplacing(temporaryFile, targetFile);
		} finally {
			delete(temporaryFile);
		}
	}

	private static StoredSession writeSessionAtomically(TestRunSession session, File historyDirectory) throws CoreException, IOException {
		String id;
		File historyFile;
		do {
			id= UUID.randomUUID().toString();
			historyFile= new File(historyDirectory, historyFileName(id));
		} while (historyFile.exists());
		exportAtomically(session, historyFile);
		return StoredSession.from(session, id, historyFile, historyTimestamp(session.getStartTime()));
	}

	private static boolean writeIndexAtomically(List<StoredSession> storedSessions, File historyDirectory) {
		Properties properties= new Properties();
		properties.setProperty(KEY_FORMAT_VERSION, Integer.toString(FORMAT_VERSION));
		properties.setProperty(KEY_ENTRY_COUNT, Integer.toString(storedSessions.size()));
		for (int i= 0; i < storedSessions.size(); i++)
			writeEntry(properties, i, storedSessions.get(i));

		File indexFile= new File(historyDirectory, INDEX_FILE_NAME);
		File temporaryFile;
		try {
			temporaryFile= Files.createTempFile(historyDirectory.toPath(), INDEX_FILE_NAME + '.', TEMP_SUFFIX).toFile();
		} catch (IOException e) {
			LOG.error("Could not create a temporary JUnit history index", e); //$NON-NLS-1$
			return false;
		}

		try {
			try (BufferedOutputStream output= new BufferedOutputStream(new FileOutputStream(temporaryFile))) {
				properties.store(output, null);
			}
			moveReplacing(temporaryFile, indexFile);
			return true;
		} catch (IOException e) {
			LOG.error("Could not write the JUnit history index", e); //$NON-NLS-1$
			return false;
		} finally {
			delete(temporaryFile);
		}
	}

	private static void writeEntry(Properties properties, int index, StoredSession session) {
		properties.setProperty(entryKey(index, KEY_ID), session.fId);
		properties.setProperty(entryKey(index, KEY_NAME), session.fTestRunName);
		setOptional(properties, entryKey(index, KEY_PROJECT), session.fProjectName);
		setOptional(properties, entryKey(index, KEY_LAUNCH_CONFIGURATION_MEMENTO), session.fLaunchConfigurationMemento);
		setOptional(properties, entryKey(index, KEY_LAUNCH_MODE), session.fLaunchMode);
		properties.setProperty(entryKey(index, KEY_START_TIME), Long.toString(session.fStartTime));
		properties.setProperty(entryKey(index, KEY_HISTORY_TIMESTAMP), Long.toString(session.fHistoryTimestamp));
		properties.setProperty(entryKey(index, KEY_PROGRESS), session.fStopped ? PROGRESS_STOPPED : PROGRESS_COMPLETED);
		properties.setProperty(entryKey(index, KEY_RESULT), resultName(session.fResult));
		properties.setProperty(entryKey(index, KEY_TOTAL_COUNT), Integer.toString(session.fTotalCount));
		properties.setProperty(entryKey(index, KEY_STARTED_COUNT), Integer.toString(session.fStartedCount));
		properties.setProperty(entryKey(index, KEY_FAILURE_COUNT), Integer.toString(session.fFailureCount));
		properties.setProperty(entryKey(index, KEY_ERROR_COUNT), Integer.toString(session.fErrorCount));
		properties.setProperty(entryKey(index, KEY_IGNORED_COUNT), Integer.toString(session.fIgnoredCount));
		properties.setProperty(entryKey(index, KEY_ASSUMPTION_FAILURE_COUNT), Integer.toString(session.fAssumptionFailureCount));
		setOptional(properties, entryKey(index, KEY_INCLUDE_TAGS), session.fIncludeTags);
		setOptional(properties, entryKey(index, KEY_EXCLUDE_TAGS), session.fExcludeTags);
		properties.setProperty(entryKey(index, KEY_FILE_LENGTH), Long.toString(session.fFileLength));
	}

	private static StoredSession readEntry(Properties properties, int index, File historyDirectory) {
		String id= UUID.fromString(required(properties, entryKey(index, KEY_ID))).toString();
		String testRunName= required(properties, entryKey(index, KEY_NAME));
		String projectName= properties.getProperty(entryKey(index, KEY_PROJECT));
		String launchConfigurationMemento= properties.getProperty(entryKey(index, KEY_LAUNCH_CONFIGURATION_MEMENTO));
		String launchMode= properties.getProperty(entryKey(index, KEY_LAUNCH_MODE));
		if (launchConfigurationMemento == null || launchConfigurationMemento.isEmpty()
				|| launchMode == null || launchMode.isEmpty()) {
			launchConfigurationMemento= null;
			launchMode= null;
		}
		long startTime= readLong(properties, entryKey(index, KEY_START_TIME));
		long historyTimestamp= readLong(properties, entryKey(index, KEY_HISTORY_TIMESTAMP), 0, Long.MAX_VALUE);
		String progress= required(properties, entryKey(index, KEY_PROGRESS));
		boolean stopped;
		if (PROGRESS_STOPPED.equals(progress)) {
			stopped= true;
		} else if (PROGRESS_COMPLETED.equals(progress)) {
			stopped= false;
		} else {
			throw new IllegalArgumentException("Unsupported JUnit history progress state: " + progress); //$NON-NLS-1$
		}

		String resultName= required(properties, entryKey(index, KEY_RESULT));
		Result result= parseResult(resultName);

		int totalCount= readInt(properties, entryKey(index, KEY_TOTAL_COUNT), 0, Integer.MAX_VALUE);
		int startedCount= readInt(properties, entryKey(index, KEY_STARTED_COUNT), 0, Integer.MAX_VALUE);
		int failureCount= readInt(properties, entryKey(index, KEY_FAILURE_COUNT), 0, Integer.MAX_VALUE);
		int errorCount= readInt(properties, entryKey(index, KEY_ERROR_COUNT), 0, Integer.MAX_VALUE);
		int ignoredCount= readInt(properties, entryKey(index, KEY_IGNORED_COUNT), 0, Integer.MAX_VALUE);
		int assumptionFailureCount= readInt(properties, entryKey(index, KEY_ASSUMPTION_FAILURE_COUNT), 0, Integer.MAX_VALUE);
		String includeTags= properties.getProperty(entryKey(index, KEY_INCLUDE_TAGS));
		String excludeTags= properties.getProperty(entryKey(index, KEY_EXCLUDE_TAGS));
		long fileLength= readLong(properties, entryKey(index, KEY_FILE_LENGTH), 1, Long.MAX_VALUE);
		File historyFile= new File(historyDirectory, historyFileName(id));
		return new StoredSession(id, historyFile, fileLength, testRunName, projectName,
				launchConfigurationMemento, launchMode, startTime, historyTimestamp, stopped, result,
				totalCount, startedCount, failureCount, errorCount, ignoredCount, assumptionFailureCount,
				includeTags, excludeTags);
	}

	private static String resultName(Result result) {
		if (result == Result.UNDEFINED)
			return "UNDEFINED"; //$NON-NLS-1$
		if (result == Result.OK)
			return "OK"; //$NON-NLS-1$
		if (result == Result.ERROR)
			return "ERROR"; //$NON-NLS-1$
		if (result == Result.FAILURE)
			return "FAILURE"; //$NON-NLS-1$
		if (result == Result.IGNORED)
			return "IGNORED"; //$NON-NLS-1$
		throw new IllegalArgumentException("Unsupported JUnit history result: " + result); //$NON-NLS-1$
	}

	private static Result parseResult(String resultName) {
		return switch (resultName) {
			case "UNDEFINED" -> Result.UNDEFINED; //$NON-NLS-1$
			case "OK" -> Result.OK; //$NON-NLS-1$
			case "ERROR" -> Result.ERROR; //$NON-NLS-1$
			case "FAILURE" -> Result.FAILURE; //$NON-NLS-1$
			case "IGNORED" -> Result.IGNORED; //$NON-NLS-1$
			default -> throw new IllegalArgumentException("Unsupported JUnit history result: " + resultName); //$NON-NLS-1$
		};
	}

	private static String historyFileName(String id) {
		return HISTORY_FILE_PREFIX + id + XML_SUFFIX;
	}

	private static String entryKey(int index, String name) {
		return "entry." + index + '.' + name; //$NON-NLS-1$
	}

	private static String required(Properties properties, String key) {
		String value= properties.getProperty(key);
		if (value == null)
			throw new IllegalArgumentException("Missing JUnit history property: " + key); //$NON-NLS-1$
		return value;
	}

	private static int readInt(Properties properties, String key, int minimum, int maximum) {
		long value= readLong(properties, key, minimum, maximum);
		return (int) value;
	}

	private static long readLong(Properties properties, String key) {
		String value= required(properties, key);
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid JUnit history number: " + key, e); //$NON-NLS-1$
		}
	}

	private static long readLong(Properties properties, String key, long minimum, long maximum) {
		long value= readLong(properties, key);
		if (value < minimum || value > maximum)
			throw new IllegalArgumentException("JUnit history number out of range: " + key); //$NON-NLS-1$
		return value;
	}

	private static void setOptional(Properties properties, String key, String value) {
		if (value != null)
			properties.setProperty(key, value);
	}

	private static long historyTimestamp(long startTime) {
		return startTime == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(startTime);
	}

	private static Path historyDirectoryPath(File historyDirectory) {
		return historyDirectory.toPath().toAbsolutePath().normalize();
	}

	private static ILaunchConfiguration resolveLaunchConfiguration(String memento) {
		if (memento == null)
			return null;
		try {
			return DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(memento);
		} catch (CoreException e) {
			LOG.log(e.getStatus());
			return null;
		}
	}

	private static IJavaProject resolveProject(String projectName) {
		if (projectName == null)
			return null;
		IJavaModel javaModel= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		IJavaProject project= javaModel.getJavaProject(projectName);
		return project.exists() ? project : null;
	}

	private static void deleteTransientSwapFiles(File historyDirectory) {
		File[] swapFiles= historyDirectory.listFiles(file -> file.isFile()
				&& file.getName().startsWith(SWAP_FILE_PREFIX)
				&& file.getName().endsWith(XML_SUFFIX));
		if (swapFiles != null) {
			for (File swapFile : swapFiles)
				delete(swapFile);
		}
	}

	private static void deleteTemporaryFiles(File historyDirectory) {
		File[] temporaryFiles= historyDirectory.listFiles(file -> file.isFile() && file.getName().endsWith(TEMP_SUFFIX));
		if (temporaryFiles != null) {
			for (File temporaryFile : temporaryFiles)
				delete(temporaryFile);
		}
	}

	private static void deleteOrphanedXmlFiles(File historyDirectory, Set<String> retainedFileNames, boolean deleteLegacyFiles) {
		File[] historyFiles= historyDirectory.listFiles(file -> file.isFile() && file.getName().endsWith(XML_SUFFIX));
		if (historyFiles != null) {
			for (File historyFile : historyFiles) {
				String fileName= historyFile.getName();
				if (!retainedFileNames.contains(fileName)
						&& (deleteLegacyFiles || fileName.startsWith(HISTORY_FILE_PREFIX)))
					delete(historyFile);
			}
		}
	}

	private static void moveReplacing(File source, File target) throws IOException {
		try {
			Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void delete(File file) {
		try {
			Files.deleteIfExists(file.toPath());
		} catch (IOException e) {
			LOG.error("Could not delete obsolete JUnit history file", e); //$NON-NLS-1$
		}
	}

	private static final class StoredSession {
		final String fId;
		final File fHistoryFile;
		final long fFileLength;
		final String fTestRunName;
		final String fProjectName;
		final String fLaunchConfigurationMemento;
		final String fLaunchMode;
		final long fStartTime;
		final long fHistoryTimestamp;
		final boolean fStopped;
		final Result fResult;
		final int fTotalCount;
		final int fStartedCount;
		final int fFailureCount;
		final int fErrorCount;
		final int fIgnoredCount;
		final int fAssumptionFailureCount;
		final String fIncludeTags;
		final String fExcludeTags;

		StoredSession(String id, File historyFile, long fileLength, String testRunName, String projectName,
				String launchConfigurationMemento, String launchMode, long startTime, long historyTimestamp,
				boolean stopped, Result result, int totalCount, int startedCount, int failureCount, int errorCount,
				int ignoredCount, int assumptionFailureCount, String includeTags, String excludeTags) {
			fId= id;
			fHistoryFile= historyFile;
			fFileLength= fileLength;
			fTestRunName= testRunName;
			fProjectName= projectName;
			fLaunchConfigurationMemento= launchConfigurationMemento;
			fLaunchMode= launchMode;
			fStartTime= startTime;
			fHistoryTimestamp= historyTimestamp;
			fStopped= stopped;
			fResult= result;
			fTotalCount= totalCount;
			fStartedCount= startedCount;
			fFailureCount= failureCount;
			fErrorCount= errorCount;
			fIgnoredCount= ignoredCount;
			fAssumptionFailureCount= assumptionFailureCount;
			fIncludeTags= includeTags;
			fExcludeTags= excludeTags;
		}

		static StoredSession from(TestRunSession session, String id, File historyFile, long historyTimestamp) {
			IJavaProject project= session.getLaunchedProject();
			String launchConfigurationMemento= null;
			String launchMode= null;
			ILaunchConfiguration launchConfiguration= session.getRerunLaunchConfiguration();
			if (launchConfiguration != null) {
				try {
					launchConfigurationMemento=
							JUnitLaunchConfigurationConstants.getRerunLaunchConfigurationMemento(launchConfiguration);
					launchMode= session.getRerunLaunchMode();
				} catch (CoreException e) {
					LOG.log(e.getStatus());
				}
				if (launchConfigurationMemento == null || launchConfigurationMemento.isEmpty()
						|| launchMode == null || launchMode.isEmpty()) {
					launchConfigurationMemento= null;
					launchMode= null;
				}
			}
			return new StoredSession(id, historyFile, historyFile.length(), session.getTestRunName(),
					project == null ? null : project.getElementName(), launchConfigurationMemento, launchMode,
					session.getStartTime(), historyTimestamp, session.isStopped(), session.getTestResult(true),
					session.getTotalCount(), session.getStartedCount(), session.getFailureCount(), session.getErrorCount(),
					session.getIgnoredCount(), session.getAssumptionFailureCount(), session.getIncludeTags(),
					session.getExcludeTags());
		}
	}

	private static final class RestoredTestRunSession extends TestRunSession {
		private final StoredSession fStoredSession;
		private final ILaunchConfiguration fRerunLaunchConfiguration;
		private final Result fHeaderResult;
		private boolean fLoadingContents;
		private boolean fContentsLoaded;
		private boolean fValid= true;

		RestoredTestRunSession(StoredSession storedSession) {
			super(storedSession.fTestRunName, resolveProject(storedSession.fProjectName));
			fStoredSession= storedSession;
			fRerunLaunchConfiguration= resolveLaunchConfiguration(storedSession.fLaunchConfigurationMemento);
			fHeaderResult= storedSession.fResult;
			fStartTime= storedSession.fStartTime;
			fTotalCount= storedSession.fTotalCount;
			fStartedCount= storedSession.fStartedCount;
			fFailureCount= storedSession.fFailureCount;
			fErrorCount= storedSession.fErrorCount;
			fIgnoredCount= storedSession.fIgnoredCount;
			fAssumptionFailureCount= storedSession.fAssumptionFailureCount;
			fIsStopped= storedSession.fStopped;
			setIncludeTags(storedSession.fIncludeTags);
			setExcludeTags(storedSession.fExcludeTags);
		}

		@Override
		public ILaunchConfiguration getRerunLaunchConfiguration() {
			return fRerunLaunchConfiguration != null && fRerunLaunchConfiguration.exists()
					? fRerunLaunchConfiguration : null;
		}

		@Override
		public String getRerunLaunchMode() {
			return getRerunLaunchConfiguration() == null ? null : fStoredSession.fLaunchMode;
		}

		@Override
		public ITestKind getTestRunnerKind() {
			ILaunchConfiguration configuration= getRerunLaunchConfiguration();
			return configuration == null ? super.getTestRunnerKind()
					: JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		}

		@Override
		public Result getTestResult(boolean includeChildren) {
			return fContentsLoaded ? super.getTestResult(includeChildren) : fHeaderResult;
		}

		@Override
		public double getElapsedTimeInSeconds() {
			return fContentsLoaded ? super.getElapsedTimeInSeconds() : Double.NaN;
		}

		@Override
		public synchronized void swapIn() {
			if (fContentsLoaded || fLoadingContents || !fValid)
				return;

			fLoadingContents= true;
			try {
				JUnitModel.importIntoTestRunSession(fStoredSession.fHistoryFile, this);
				fContentsLoaded= true;
			} catch (CoreException e) {
				LOG.log(e.getStatus());
				reset();
				fContentsLoaded= true;
				fValid= false;
				delete(fStoredSession.fHistoryFile);
			} finally {
				fLoadingContents= false;
			}
		}

		@Override
		public synchronized void swapOut() {
			if (!fValid || !fContentsLoaded || !canSwapOut())
				return;
			discardTestTree();
			fContentsLoaded= false;
		}

		@Override
		public void removeSwapFile() {
			fValid= false;
			// Keep the last published history generation intact. store() removes
			// unreferenced files after publishing the replacement index.
		}

		StoredSession reusableStoredSession() {
			if (fValid && fStoredSession.fHistoryFile.isFile()
					&& fStoredSession.fHistoryFile.length() == fStoredSession.fFileLength)
				return fStoredSession;
			return null;
		}

		boolean canWriteFromMemory() {
			return fValid && fContentsLoaded;
		}
	}
}
