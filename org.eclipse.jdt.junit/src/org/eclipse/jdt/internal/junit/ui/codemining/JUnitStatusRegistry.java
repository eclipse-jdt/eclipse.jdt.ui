/*******************************************************************************
 * Copyright (c) 2017, 2026 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation,
 *         originally in https://github.com/angelozerr/jdt-codemining (EPL)
 *     Kai Winter <kaiwinter@gmx.de> - JUnit 5 ParameterizedTest support in jdt-codemining
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui.codemining;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElementContainer;
import org.eclipse.jdt.junit.model.ITestRunSession;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.ITestSessionListener;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.TestMethodFinder;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Remembers the result of the last run of each test method, so that it can be shown by the JUnit
 * status code mining. Only results are kept, not the test run sessions.
 * <p>
 * Several results of one run can belong to the same method (parameterized, repeated and dynamic
 * tests): the worst one is kept. A result of a new run replaces the results of the previous runs
 * for the same parameter types, the overloads which were not run keep theirs. The outcome of a
 * test container (the class of a JUnit 5 test, for instance when its <code>@AfterAll</code>
 * method fails, or a disabled parameterized test) is recorded when the test run is finished,
 * the one of a rerun in the JUnit view when it is reported.
 * </p>
 * <p>
 * Known limits: the test methods of a concrete superclass show no result when only a subclass was
 * run (the runner reports the subclass, and the superclass cannot find its subclasses cheaply);
 * JUnit 3 and 4 do not tell the parameter types, so overloads with the same name
 * share their result; with these test kinds, a method whose name contains <code>[</code> is
 * recorded under the part before it, which is how parameterized tests of JUnit 4 are named. The
 * results of the classes of a closed or deleted project, launched from another project, stay in
 * memory until that project goes too: they are never read, since a class which does not exist
 * anymore has no code mining.
 * </p>
 * <p>
 * The results are recorded under the class that the test runner ran and the launched project; the
 * project of the test class is only looked for when the results are read. The results of a project
 * are forgotten when it is closed, deleted, renamed or moved; the others are kept, they are bounded
 * by the tests of the workspace. The results of a test run which is killed, and so fires no event, are shown
 * with the next refresh. The status of a class includes the results of the
 * test methods it inherits. A method declared in a superclass has its own result only when it is
 * looked up in the class that was run, which is not the case for an abstract superclass: no code
 * mining is shown there.
 * </p>
 * <p>
 * The test runs are tracked between {@link #start()} and {@link #dispose()}, which the plug-in
 * calls when it starts and stops.
 * </p>
 */
public final class JUnitStatusRegistry {

	private static final int MAX_MEMBER_DEPTH= 10;

	/**
	 * The results of one method in the last run which ran it, by parameter types (<code>null</code>
	 * if unknown).
	 */
	private static final class MethodResults {
		final Map<List<String>, RunResult> fResults= new HashMap<>();
	}

	/**
	 * The result of one method, with the parameter types given by the test runner, in a run. A
	 * rerun from the JUnit view comes after the run it belongs to and before the later runs.
	 */
	private static final class RunResult {
		final long fRun;

		final long fRerun;

		Result fResult;

		RunResult(long run, long rerun, Result result) {
			fRun= run;
			fRerun= rerun;
			fResult= result;
		}

		int compareTo(long run, long rerun) {
			return fRun != run ? Long.compare(fRun, run) : Long.compare(fRerun, rerun);
		}
	}

	/**
	 * The name under which the outcome of the class itself is recorded, which is not the one of a
	 * method.
	 */
	private static final String CLASS_LEVEL= ""; //$NON-NLS-1$

	private static final String ENCLOSED= "org.junit.experimental.runners.Enclosed"; //$NON-NLS-1$

	private static final String JUPITER_NESTED= "org.junit.jupiter.api.Nested"; //$NON-NLS-1$

	private static final String INITIALIZATION_ERROR= "initializationError"; //$NON-NLS-1$

	private static final String TEST_FACTORY_SEGMENT= "/[test-factory:"; //$NON-NLS-1$

	/**
	 * The method of a test, as given by the unique id of a JUnit 5 test.
	 *
	 * @param className the binary name of the class, with dollars for member types
	 * @param methodName the name of the method, <code>null</code> for the unique id of a class
	 * @param parameterTypes the fully qualified names of the parameter types
	 */
	public record MethodReference(String className, String methodName, String[] parameterTypes) {
	}

	// fully qualified class name -> name of the launched project -> method name -> results
	private final Map<String, Map<String, Map<String, MethodResults>>> fResults= new ConcurrentHashMap<>();

	private final Map<ITestRunSession, Long> fRuns= new WeakHashMap<>();

	private final Map<TestRunSession, ITestSessionListener> fRerunListeners= new WeakHashMap<>();

	private final AtomicLong fRunCounter= new AtomicLong();

	private final AtomicLong fRerunCounter= new AtomicLong();

	private final List<Runnable> fFinishedListeners= new CopyOnWriteArrayList<>();

	private final TestRunListener fListener= new TestRunListener() {
		@Override
		public void sessionLaunched(ITestRunSession session) {
			// the order of the launches, even if a run is slow to start its first test
			getRun(session);
		}

		@Override
		public void sessionStarted(ITestRunSession session) {
			getRun(session);
			if (session instanceof TestRunSession testRunSession) {
				RerunListener rerunListener= new RerunListener();
				synchronized (fRerunListeners) {
					fRerunListeners.put(testRunSession, rerunListener);
				}
				testRunSession.addTestSessionListener(rerunListener);
			}
		}

		@Override
		public void testCaseFinished(ITestCaseElement testCaseElement) {
			record(testCaseElement, getRun(testCaseElement.getTestRunSession()), 0);
		}

		@Override
		public void sessionFinished(ITestRunSession session) {
			recordContainers(session);
			notifyListeners();
		}
	};

	/**
	 * Observes the tests which are run again from the JUnit view, which the public listener does not
	 * report. A rerun replaces the earlier results of the test.
	 */
	private final class RerunListener implements ITestSessionListener {
		@Override
		public void testReran(TestCaseElement testCaseElement, Status status, String trace, String expectedResult, String actualResult) {
			if (recordRerun(testCaseElement)) {
				notifyListeners();
			}
		}

		@Override
		public boolean acceptsSwapToDisk() {
			return true;
		}

		@Override
		public void sessionStarted() {
			// not relevant
		}

		@Override
		public void sessionEnded(long elapsedTime) {
			// not relevant
		}

		@Override
		public void sessionStopped(long elapsedTime) {
			// not relevant
		}

		@Override
		public void sessionTerminated() {
			// not relevant
		}

		@Override
		public void testAdded(TestElement testElement) {
			// not relevant
		}

		@Override
		public void runningBegins() {
			// not relevant
		}

		@Override
		public void testStarted(TestCaseElement testCaseElement) {
			// not relevant
		}

		@Override
		public void testEnded(TestCaseElement testCaseElement) {
			// not relevant
		}

		@Override
		public void testFailed(TestElement testElement, Status status, String trace, String expected, String actual) {
			// not relevant
		}
	}

	private final IResourceChangeListener fProjectListener= event -> {
		if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
			if (event.getDelta() != null) {
				for (IResourceDelta delta : event.getDelta().getAffectedChildren(IResourceDelta.REMOVED)) {
					removeProject(delta.getResource().getName()); // deleted, renamed or moved
				}
			}
		} else if (event.getResource() instanceof IProject project) {
			removeClassesOf(project); // while its classes can still be found from the projects which were launched
			removeProject(project.getName()); // closed or about to be deleted
		}
	};

	/**
	 * Creates a registry which does not track the test runs until it is {@link #start() started}.
	 */
	public JUnitStatusRegistry() {
		// nothing to do
	}

	/**
	 * Starts tracking the test runs and the projects.
	 */
	public void start() {
		JUnitCore.addTestRunListener(fListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fProjectListener,
				IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * Stops tracking the test runs and forgets the results and the listeners.
	 */
	public void dispose() {
		JUnitCore.removeTestRunListener(fListener);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(fProjectListener);
		synchronized (fRerunListeners) {
			fRerunListeners.forEach(TestRunSession::removeTestSessionListener);
			fRerunListeners.clear();
		}
		fFinishedListeners.clear();
		fResults.clear();
	}

	/**
	 * Adds a listener called, in the thread of the test runner, when a test run is finished.
	 *
	 * @param listener the listener
	 */
	public void addFinishedListener(Runnable listener) {
		fFinishedListeners.add(listener);
	}

	/**
	 * Removes a listener added with {@link #addFinishedListener(Runnable)}.
	 *
	 * @param listener the listener
	 */
	public void removeFinishedListener(Runnable listener) {
		fFinishedListeners.remove(listener);
	}

	private void notifyListeners() {
		for (Runnable listener : fFinishedListeners) {
			try {
				listener.run();
			} catch (RuntimeException e) {
				JUnitPlugin.log(e);
			}
		}
	}

	private void removeProject(String projectName) {
		synchronized (fResults) {
			fResults.values().forEach(projects -> projects.remove(projectName));
			fResults.values().removeIf(Map::isEmpty);
		}
	}

	/**
	 * Forgets the results of the classes of a project which were recorded for a run launched from another project, which would
	 * otherwise be taken for the ones of the classes of the same name if the project is created again.
	 */
	private void removeClassesOf(IProject project) {
		IJavaProject source= JavaCore.create(project);
		if (!source.exists()) {
			return;
		}
		Map<String, List<String>> candidates= new HashMap<>(); // launched projects by class name
		synchronized (fResults) {
			fResults.forEach((className, launchedProjects) -> candidates.put(className, List.copyOf(launchedProjects.keySet())));
		}
		Map<String, List<String>> found= new HashMap<>();
		candidates.forEach((className, launchedProjects) -> {
			for (String launched : launchedProjects) {
				if (!launched.equals(source.getElementName()) && source.equals(findProjectOf(launched, className))) {
					found.computeIfAbsent(className, _ -> new ArrayList<>()).add(launched);
				}
			}
		});
		synchronized (fResults) {
			found.forEach((className, launchedProjects) -> {
				Map<String, Map<String, MethodResults>> results= fResults.get(className);
				if (results != null) {
					results.keySet().removeAll(launchedProjects);
				}
			});
			fResults.values().removeIf(Map::isEmpty);
		}
	}

	private long getRun(ITestRunSession session) {
		synchronized (fRuns) {
			return fRuns.computeIfAbsent(session, _ -> fRunCounter.incrementAndGet());
		}
	}

	private boolean record(ITestCaseElement testCaseElement, long run, long rerun) {
		return record(testCaseElement, run, rerun, testCaseElement.getTestResult(false));
	}

	private boolean record(ITestCaseElement testCaseElement, long run, long rerun, Result result) {
		IJavaProject launchedProject= testCaseElement.getTestRunSession().getLaunchedProject();
		if (launchedProject == null) {
			return false;
		}
		String className= testCaseElement.getTestClassName();
		String methodName= testCaseElement.getTestMethodName();
		String[] parameterTypes= null;
		// The unique id of JUnit 5 gives the method of the invocations of parameterized, repeated and dynamic tests
		MethodReference reference= testCaseElement instanceof TestElement element ? parseUniqueId(element.getUniqueId()) : null;
		if (reference != null) {
			className= reference.className();
			methodName= reference.methodName();
			parameterTypes= reference.parameterTypes();
		} else {
			// the other test kinds name a parameterized test like "method[0]"
			if (methodName != null) {
				int bracket= methodName.indexOf('[');
				if (bracket > 0) {
					methodName= methodName.substring(0, bracket);
				}
			}
			if (INITIALIZATION_ERROR.equals(methodName)) {
				methodName= CLASS_LEVEL; // JUnit 4 reports an invalid class, such as a non-static @BeforeClass, as this test
			} else if (testCaseElement instanceof TestElement element && element.getTestName().indexOf('(') < 0
					&& methodName != null && methodName.replace('$', '.').equals(className)) {
				methodName= CLASS_LEVEL; // JUnit 4 reports an @Ignore class as a test named after it, without a method
			}
		}
		record(run, rerun, launchedProject.getElementName(), className, methodName, parameterTypes, result);
		return true;
	}

	/**
	 * Records the outcome of the test containers of a finished test run: the one of a class, such as the
	 * failure of its <code>@AfterAll</code> method, belongs to no test method, and a disabled
	 * parameterized test has no invocation to report it. Only the JUnit 5 containers are known,
	 * since they have a unique id.
	 */
	public void recordContainers(ITestRunSession session) {
		recordContainers(session, session);
	}

	/**
	 * Records the result of a test which is run again from the JUnit view, in the session of its
	 * first run. It replaces the results of this run and of the earlier ones, not the ones of the
	 * runs which started after it.
	 *
	 * @param testCaseElement the test
	 * @return whether the result was recorded
	 */
	public boolean recordRerun(ITestCaseElement testCaseElement) {
		return record(testCaseElement, getRun(testCaseElement.getTestRunSession()), fRerunCounter.incrementAndGet());
	}

	private void recordContainers(ITestRunSession session, ITestElementContainer container) {
		IJavaProject launchedProject= session.getLaunchedProject();
		if (launchedProject == null) {
			return;
		}
		for (ITestElement child : container.getChildren()) {
			if (child instanceof TestSuiteElement suite) {
				MethodReference reference= parse(suite.getUniqueId());
				if (reference == null && isLegacyClassContainer(suite)) {
					// JUnit 3 and 4 have no unique id: the suite of a class is named after it
					reference= new MethodReference(suite.getSuiteTypeName(), null, null);
				}
				Result result= suite.getTestResult(false);
				if (suite.isAssumptionFailure() && result != Result.ERROR && result != Result.FAILURE) {
					// aborted by an assumption, such as in @BeforeAll: its tests are skipped, and keep no result of an earlier run
					result= Result.IGNORED;
					recordNotStarted(session, suite);
				}
				// A container that completed without a failure of its own replaces an earlier one, whatever its tests did. It
				// is recorded as UNDEFINED, which is the weakest result: the results of its tests decide, such as ignored ones.
				boolean completed= result == Result.UNDEFINED && suite.getProgressState() == ProgressState.COMPLETED;
				if (result == Result.UNDEFINED && isEmptyFactoryRun(session, suite)) {
					result= Result.OK;
				}
				if (reference != null && (result != Result.UNDEFINED || completed)) {
					String methodName= reference.methodName() == null ? CLASS_LEVEL : reference.methodName();
					record(getRun(session), launchedProject.getElementName(), reference.className(), methodName, reference.parameterTypes(), result);
				}
				recordContainers(session, suite);
			}
		}
	}

	private void recordNotStarted(ITestRunSession session, ITestElementContainer container) {
		for (ITestElement child : container.getChildren()) {
			if (child instanceof ITestCaseElement test && child.getProgressState() == ProgressState.NOT_STARTED) {
				record(test, getRun(session), 0, Result.IGNORED);
			} else if (child instanceof ITestElementContainer childContainer) {
				recordNotStarted(session, childContainer);
			}
		}
	}

	/**
	 * Tells whether the container is a test factory which returned no test in a run which completed. JUnit 5 only reports
	 * the end of the tests, so the container of a factory without tests is never completed, although it was run, unless
	 * one of its enclosing containers failed or was aborted.
	 */
	private static boolean isEmptyFactoryRun(ITestRunSession session, TestSuiteElement suite) {
		String uniqueId= suite.getUniqueId();
		boolean isFactory= uniqueId != null && uniqueId.lastIndexOf(TEST_FACTORY_SEGMENT) >= 0
				&& uniqueId.lastIndexOf(TEST_FACTORY_SEGMENT) == uniqueId.lastIndexOf("/["); //$NON-NLS-1$
		if (!isFactory || suite.getChildren().length > 0 || session.getProgressState() != ProgressState.COMPLETED) {
			return false;
		}
		for (ITestElementContainer parent= suite.getParentContainer(); parent instanceof TestElement element; parent= element.getParentContainer()) {
			Result result= element.getTestResult(false);
			if (result == Result.ERROR || result == Result.FAILURE || element.isAssumptionFailure()) {
				return false;
			}
		}
		return true;
	}

	private static boolean isLegacyClassContainer(TestSuiteElement suite) {
		String uniqueId= suite.getUniqueId();
		if (uniqueId != null && !uniqueId.isEmpty()) {
			return false;
		}
		String className= suite.getSuiteTypeName();
		return className != null && !className.isEmpty() && className.equals(suite.getTestName().replace('$', '.')) && className.indexOf('[') < 0;
	}

	/**
	 * Reads the unique id of a JUnit 5 test, such as
	 * <code>[engine:junit-jupiter]/[class:p.Outer]/[nested-class:Inner]/[test-template:m(java.lang.String)]/[test-template-invocation:#1]</code>.
	 *
	 * @param uniqueId the unique id, can be <code>null</code>
	 * @return the class and the method of the test, or <code>null</code> if the id has none
	 */
	public static MethodReference parseUniqueId(String uniqueId) {
		MethodReference reference= parse(uniqueId);
		return reference == null || reference.methodName() == null ? null : reference;
	}

	/** Reads the class and the method, if any, of a unique id. */
	private static MethodReference parse(String uniqueId) {
		if (uniqueId == null) {
			return null;
		}
		String className= null;
		String methodName= null;
		String[] parameterTypes= null;
		for (String segment : uniqueId.split("/")) { //$NON-NLS-1$
			int colon= segment.indexOf(':');
			if (!segment.startsWith("[") || !segment.endsWith("]") || colon < 0) { //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}
			String type= segment.substring(1, colon);
			String value= decode(segment.substring(colon + 1, segment.length() - 1));
			switch (type) {
				case "class", "runner" -> className= value; // the runner of JUnit Vintage is a class //$NON-NLS-1$ //$NON-NLS-2$
				case "nested-class" -> className= className == null ? null : className + '$' + value; //$NON-NLS-1$
				case "method", "test-template", "test-factory" -> { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					int parenthesis= value.indexOf('(');
					methodName= parenthesis < 0 ? value : value.substring(0, parenthesis);
					parameterTypes= parenthesis < 0 || !value.endsWith(")") ? null : parseParameterTypes(value.substring(parenthesis + 1, value.length() - 1)); //$NON-NLS-1$
				}
				default -> {
					// not relevant
				}
			}
		}
		return className == null ? null : new MethodReference(className, methodName, parameterTypes);
	}

	/**
	 * Decodes the percent-encoded characters of a segment of a unique id (a "+" is a plus).
	 */
	private static String decode(String segment) {
		if (segment.indexOf('%') < 0) {
			return segment;
		}
		StringBuilder decoded= new StringBuilder();
		ByteArrayOutputStream bytes= new ByteArrayOutputStream();
		for (int i= 0; i < segment.length(); i++) {
			char c= segment.charAt(i);
			if (c == '%' && i + 2 < segment.length() && Character.digit(segment.charAt(i + 1), 16) >= 0 && Character.digit(segment.charAt(i + 2), 16) >= 0) {
				bytes.write(Integer.parseInt(segment.substring(i + 1, i + 3), 16));
				i+= 2;
			} else {
				// the other characters are kept as they are, the encoded bytes are decoded together
				decoded.append(bytes.toString(StandardCharsets.UTF_8)).append(c);
				bytes.reset();
			}
		}
		return decoded.append(bytes.toString(StandardCharsets.UTF_8)).toString();
	}

	private static String[] parseParameterTypes(String types) {
		if (types.isBlank()) {
			return new String[0];
		}
		return Arrays.stream(types.split(",")).map(String::trim).toArray(String[]::new); //$NON-NLS-1$
	}

	/**
	 * Records the result of a test method in a test run. The results of an earlier run for the same
	 * parameter types are replaced, the ones of the other methods and overloads are kept. The project of the test class is only
	 * looked for when the results are read, because it can be slow.
	 *
	 * @param run the number identifying the test run, increasing with the start of the runs
	 * @param launchedProject the name of the project of the test run
	 * @param className the fully qualified name of the class, with dots or dollars for member types
	 * @param methodName the name of the method
	 * @param parameterTypes the parameter types supplied by the test runner, or <code>null</code>
	 * @param result the result
	 */
	public void record(long run, String launchedProject, String className, String methodName, String[] parameterTypes, Result result) {
		record(run, 0, launchedProject, className, methodName, parameterTypes, result);
	}

	private void record(long run, long rerun, String launchedProject, String className, String methodName, String[] parameterTypes, Result result) {
		if (launchedProject == null || className == null || methodName == null || result == null) {
			return;
		}
		String name= methodName;
		List<String> types= parameterTypes == null ? null : List.of(parameterTypes);
		// one lock for the lookup and the write, so that removing a project cannot lose the write
		synchronized (fResults) {
			MethodResults methodResults= fResults.computeIfAbsent(className, _ -> new ConcurrentHashMap<>())
					.computeIfAbsent(launchedProject, _ -> new ConcurrentHashMap<>())
					.computeIfAbsent(name, _ -> new MethodResults());
			Map<List<String>, RunResult> results= methodResults.fResults;
			if (results.entrySet().stream().anyMatch(entry -> entry.getValue().compareTo(run, rerun) > 0 && isSameMethod(entry.getKey(), types))) {
				return; // result of an older run, which finished late
			}
			// the unknown parameter types can be the ones of any overload
			results.entrySet().removeIf(entry -> entry.getValue().compareTo(run, rerun) < 0 && isSameMethod(entry.getKey(), types));
			RunResult existing= results.get(types);
			if (existing == null) {
				results.put(types, new RunResult(run, rerun, result));
			} else {
				existing.fResult= worst(existing.fResult, result);
			}
		}
	}

	private static boolean isSameMethod(List<String> parameterTypes, List<String> otherParameterTypes) {
		return parameterTypes == null || otherParameterTypes == null || parameterTypes.equals(otherParameterTypes);
	}

	/**
	 * The results of the last runs of a class, read once from the registry.
	 */
	public static final class Results {

		private final IType fType;

		// by method name, then by parameter types, null if the class has no result
		private final Map<String, Map<List<String>, Result>> fMethods;

		// the results of the member classes, read at the same time as the ones of the class
		private final List<Results> fMembers;

		private List<IMethod> fInheritedMethods;

		private Boolean fConcreteTestImplementor;

		// whether the runner of the enclosing class runs the tests of this member class
		private boolean fRunWithEnclosing;

		Results(IType type, Map<String, Map<List<String>, Result>> methods, List<Results> members) {
			fType= type;
			fMethods= methods;
			fMembers= members;
		}

		/**
		 * Returns the results of a member class, read with the ones of this class.
		 *
		 * @param memberType the member class
		 * @return its results, or <code>null</code> if it is not a member class of the class
		 */
		public Results getMember(IType memberType) {
			return fMembers.stream().filter(member -> member.fType.equals(memberType)).findFirst().orElse(null);
		}

		/**
		 * Returns the methods of the supertypes (superclasses, and interfaces for the default
		 * methods), found once for the snapshot. A cancelled search leaves it to the next call.
		 */
		private synchronized List<IMethod> getInheritedMethods(IProgressMonitor monitor) throws JavaModelException {
			if (fInheritedMethods == null) {
				List<IMethod> methods= new ArrayList<>();
				ITypeHierarchy hierarchy= fType.newSupertypeHierarchy(monitor);
				IType[] supertypes= hierarchy.getAllSupertypes(fType);
				for (IType supertype : supertypes) {
					if (!"java.lang.Object".equals(supertype.getFullyQualifiedName())) { //$NON-NLS-1$
						for (IMethod method : supertype.getMethods()) {
							// a method overridden by a subtype, where it may no longer be a test, is not inherited, except
							// for JUnit 4, which runs the methods annotated in any superclass
							if (!isOverridden(method, hierarchy, supertypes) || TestSearchEngine.hasJUnit4TestAnnotation(method)) {
								methods.add(method);
							}
						}
					}
				}
				fInheritedMethods= List.copyOf(methods);
			}
			return fInheritedMethods;
		}

		/** Tells whether a subtype of the declaring type of the method, in the hierarchy of the class, overrides it. */
		private boolean isOverridden(IMethod method, ITypeHierarchy hierarchy, IType[] supertypes) throws JavaModelException {
			IType declaring= method.getDeclaringType();
			for (IType type : supertypes) {
				if (!type.equals(declaring) && Arrays.asList(hierarchy.getAllSupertypes(type)).contains(declaring) && declares(type, method)) {
					return true;
				}
			}
			return declares(fType, method);
		}

		private static boolean declares(IType type, IMethod method) throws JavaModelException {
			for (IMethod declared : type.getMethods()) {
				if (method.isSimilar(declared)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Returns the result of the last run of the given method.
		 *
		 * @param method the test method, declared by the class
		 * @return the result, or <code>null</code> if the method has not been run
		 * @throws JavaModelException if the method cannot be resolved
		 */
		public Result findResult(IMethod method) throws JavaModelException {
			return fMethods == null ? null : match(method, fMethods.get(method.getElementName()), null);
		}

		/** Tells whether the method is still a test of the class: the JUnit 5 kind accepts the annotated tests of JUnit 4 and 5 and the ones of JUnit 3. */
		private boolean isTest(IMethod method) throws JavaModelException {
			if (fConcreteTestImplementor == null) {
				fConcreteTestImplementor= TestSearchEngine.isConcreteTestImplementor(fType);
			}
			return TestSearchEngine.isTestMethod(method, fType.getJavaProject(), TestKindRegistry.JUNIT5_TEST_KIND_ID, fConcreteTestImplementor);
		}

		/**
		 * Returns the combined result of the last run of the class, of the methods that it still
		 * declares or inherits, and of its member classes.
		 *
		 * @param monitor the monitor of the search of the superclasses, or <code>null</code>
		 * @return the result, or <code>null</code> if no method of the class has been run
		 * @throws JavaModelException if the methods of the class cannot be resolved
		 */
		public Result findTypeResult(IProgressMonitor monitor) throws JavaModelException {
			Result combined= null;
			if (fMethods != null) {
				combined= worst(combined, worstOf(fMethods.get(CLASS_LEVEL)));
				Map<String, Set<List<String>>> matched= new HashMap<>();
				for (IMethod method : fType.getMethods()) {
					String name= method.getElementName();
					Result result= match(method, fMethods.get(name), matched.computeIfAbsent(name, _ -> new HashSet<>()));
					if (isTest(method)) { // a method which is no longer a test does not count, but it is not a deleted one
						combined= worst(combined, result);
					}
				}
				// the other recorded methods count only if a superclass declares them, not if they were renamed or deleted
				Map<String, Map<List<String>, Result>> unmatched= new HashMap<>();
				fMethods.forEach((name, results) -> {
					if (!name.equals(CLASS_LEVEL)) {
						Map<List<String>, Result> rest= new HashMap<>(results);
						rest.keySet().removeAll(matched.getOrDefault(name, Set.of()));
						if (!rest.isEmpty()) {
							unmatched.put(name, rest);
						}
					}
				});
				if (!unmatched.isEmpty()) {
					for (IMethod method : getInheritedMethods(monitor)) {
						Result result= match(method, unmatched.get(method.getElementName()), null);
						if (result != null && isTest(method)) {
							combined= worst(combined, result);
						}
					}
				}
			}
			// the tests of the member classes run by the runner of the class belong to it, such as @Nested or @RunWith(Enclosed.class)
			for (Results member : fMembers) {
				if (member.fRunWithEnclosing) {
					combined= worst(combined, member.findTypeResult(monitor));
				}
			}
			return combined;
		}
	}

	private static Result worstOf(Map<List<String>, Result> results) {
		Result combined= null;
		if (results != null) {
			for (Result result : results.values()) {
				combined= worst(combined, result);
			}
		}
		return combined;
	}

	/**
	 * Combines the results recorded for the parameter types of the method.
	 *
	 * @param matched receives the parameter types of the results used, can be <code>null</code>
	 */
	private static Result match(IMethod method, Map<List<String>, Result> results, Set<List<String>> matched) throws JavaModelException {
		Result combined= null;
		if (results != null) {
			for (Map.Entry<List<String>, Result> entry : results.entrySet()) {
				if (entry.getKey() == null || isMethodOf(method, entry.getKey())) {
					combined= worst(combined, entry.getValue());
					if (matched != null) {
						matched.add(entry.getKey());
					}
				}
			}
		}
		return combined;
	}

	/**
	 * Tells whether the parameter types given by the test runner are the ones of the method. They
	 * can be written differently from the source (member classes, erased type variables, arrays):
	 * in this case, the method matches if it is the only one of its name whose types are the declared
	 * ones, or the erasure of the type variables, so that a method whose parameters were edited does
	 * not get the old result.
	 */
	private static boolean isMethodOf(IMethod method, List<String> parameterTypes) throws JavaModelException {
		IMethod found= TestMethodFinder.findMethod(method.getDeclaringType(), method.getElementName(), parameterTypes.toArray(String[]::new));
		if (found != null) {
			return method.equals(found);
		}
		if (!looksLike(method, parameterTypes)) {
			return false;
		}
		// with overloads, only if no other one looks like these types too
		for (IMethod other : method.getDeclaringType().getMethods()) {
			if (!other.equals(method) && other.getElementName().equals(method.getElementName()) && looksLike(other, parameterTypes)) {
				return false;
			}
		}
		return true;
	}

	private static boolean looksLike(IMethod method, List<String> parameterTypes) throws JavaModelException {
		String[] declared= method.getParameterTypes();
		if (declared.length != parameterTypes.size()) {
			return false;
		}
		Map<String, ITypeParameter> typeVariables= new HashMap<>();
		for (ITypeParameter typeParameter : method.getDeclaringType().getTypeParameters()) {
			typeVariables.put(typeParameter.getElementName(), typeParameter);
		}
		for (ITypeParameter typeParameter : method.getTypeParameters()) {
			typeVariables.put(typeParameter.getElementName(), typeParameter); // hides the one of the class
		}
		for (int i= 0; i < declared.length; i++) {
			if (!isSimilar(parameterTypes.get(i), declared[i], method.getDeclaringType(), typeVariables)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Tells whether the type name of the runner is the erasure of the declared type signature: the
	 * same array rank and the same type, or the bound of the type variable.
	 */
	private static boolean isSimilar(String recorded, String declared, IType declaringType, Map<String, ITypeParameter> typeVariables) throws JavaModelException {
		String erased= Signature.getTypeErasure(declared);
		String recordedElement= recorded.replace("[]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (Signature.getArrayCount(erased) != (recorded.length() - recordedElement.length()) / 2) {
			return false;
		}
		String element= Signature.getElementType(erased);
		String declaredName;
		ITypeParameter typeVariable= typeVariables.get(Signature.toString(element));
		if (typeVariable != null) {
			declaredName= erasedBound(typeVariable, declaringType, typeVariables, new HashSet<>());
		} else if (Signature.getTypeSignatureKind(element) == Signature.BASE_TYPE_SIGNATURE) {
			declaredName= Signature.toString(element);
		} else {
			declaredName= JavaModelUtil.getResolvedTypeName(element, declaringType, '.');
		}
		return declaredName != null && declaredName.equals(recordedElement.replace('$', '.'));
	}

	/** The class that a type variable erases to: its first bound, which can be a type variable too. */
	private static String erasedBound(ITypeParameter typeVariable, IType declaringType, Map<String, ITypeParameter> typeVariables, Set<String> visited) throws JavaModelException {
		String[] bounds= typeVariable.getBounds();
		if (bounds.length == 0) {
			return "java.lang.Object"; //$NON-NLS-1$
		}
		String erased= Signature.getTypeErasure(Signature.createTypeSignature(bounds[0], false));
		ITypeParameter next= typeVariables.get(Signature.toString(erased));
		if (next != null) {
			return visited.add(next.getElementName()) ? erasedBound(next, declaringType, typeVariables, visited) : null;
		}
		return JavaModelUtil.getResolvedTypeName(erased, declaringType, '.');
	}

	/**
	 * Reads the results of the last runs of a class and of its member classes, to find the ones of
	 * their members without reading the registry again.
	 *
	 * @param type the test class
	 * @return the results
	 * @throws JavaModelException if the member classes cannot be resolved
	 */
	public Results getResults(IType type) throws JavaModelException {
		return getResults(type, 0);
	}

	private Results getResults(IType type, int depth) throws JavaModelException {
		List<Results> members= new ArrayList<>();
		if (depth < MAX_MEMBER_DEPTH) {
			for (IType memberType : type.getTypes()) {
				Results member= getResults(memberType, depth + 1);
				member.fRunWithEnclosing= isRunWithEnclosing(type, memberType);
				members.add(member);
			}
		}
		// the name of a member class is written with a dollar by JUnit 5 and with a dot by the other kinds
		Set<String> names= new LinkedHashSet<>(List.of(type.getFullyQualifiedName('.'), type.getFullyQualifiedName()));
		IJavaProject project= type.getJavaProject();
		// by method name, then by parameter types, the latest run
		Map<String, Map<List<String>, RunResult>> latest= new HashMap<>();
		for (String name : names) {
			Map<String, Map<String, MethodResults>> launchedProjects= fResults.get(name);
			if (launchedProjects == null) {
				continue;
			}
			for (Map.Entry<String, Map<String, MethodResults>> launched : launchedProjects.entrySet()) {
				if (!launched.getKey().equals(project.getElementName()) && !isFoundFrom(launched.getKey(), type)) {
					continue;
				}
				// the latest run of each method and parameter types wins, whatever the launched project
				synchronized (fResults) {
					launched.getValue().forEach((method, results) -> results.fResults.forEach((types, result) -> latest
							.computeIfAbsent(method, _ -> new HashMap<>()).merge(types, result, (first, second) -> first.compareTo(second.fRun, second.fRerun) >= 0 ? first : second)));
				}
			}
		}
		if (latest.isEmpty()) {
			return new Results(type, null, members);
		}
		// the results of different launched projects or class names can overlap, such as unknown and known parameter types
		latest.values().forEach(results -> {
			// decided on the entries as they are, not on the ones left, so that the order does not matter
			List<List<String>> superseded= results.entrySet().stream()
					.filter(entry -> results.entrySet().stream()
							.anyMatch(other -> other != entry && other.getValue().compareTo(entry.getValue().fRun, entry.getValue().fRerun) > 0
									&& isSameMethod(other.getKey(), entry.getKey())))
					.map(Map.Entry::getKey).toList();
			results.keySet().removeAll(superseded);
		});
		Map<String, Map<List<String>, Result>> copy= new HashMap<>();
		synchronized (fResults) {
			latest.forEach((method, results) -> {
				Map<List<String>, Result> methodCopy= new HashMap<>();
				results.forEach((types, result) -> methodCopy.put(types, result.fResult));
				copy.put(method, methodCopy);
			});
		}
		return new Results(type, copy, members);
	}

	/**
	 * Tells whether the tests of a member class are run with the ones of its enclosing class: a
	 * JUnit Jupiter <code>@Nested</code> class, or a member of a class run by <code>Enclosed</code>.
	 * The other member classes are run independently.
	 */
	private static boolean isRunWithEnclosing(IType outer, IType member) throws JavaModelException {
		for (IAnnotation annotation : member.getAnnotations()) {
			if (TestSearchEngine.isOrIsMetaAnnotatedWith(annotation, member, member.getJavaProject(), JUPITER_NESTED)) {
				return true;
			}
		}
		for (IAnnotation annotation : outer.getAnnotations()) {
			String name= annotation.getElementName();
			if ("RunWith".equals(name) || "org.junit.runner.RunWith".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
				for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
					if (isEnclosed(outer, String.valueOf(pair.getValue()))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Tells whether the class literal of a <code>@RunWith</code>, as written, is the <code>Enclosed</code> runner. */
	private static boolean isEnclosed(IType outer, String literal) throws JavaModelException {
		String name= literal.endsWith(".class") ? literal.substring(0, literal.length() - ".class".length()) : literal; //$NON-NLS-1$ //$NON-NLS-2$
		if (name.indexOf('.') > 0) {
			return ENCLOSED.equals(name);
		}
		String[][] resolved= outer.resolveType(name);
		if (resolved != null) {
			for (String[] candidate : resolved) { // several with imports on demand
				if (ENCLOSED.equals(candidate[0].isEmpty() ? candidate[1] : candidate[0] + '.' + candidate[1])) {
					return true;
				}
			}
		}
		return false;
	}

	/** Tells whether the class is found in the given project because it is on its classpath. */
	private static boolean isFoundFrom(String launchedProjectName, IType type) {
		return type.getJavaProject().equals(findProjectOf(launchedProjectName, type.getFullyQualifiedName('.')));
	}

	/** Returns the project which holds the class, as found from the given project, or <code>null</code> if it is not found. */
	private static IJavaProject findProjectOf(String launchedProjectName, String className) {
		IJavaProject launched= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(launchedProjectName));
		try {
			IType found= launched.exists() ? launched.findType(className.replace('$', '.')) : null;
			return found == null ? null : found.getJavaProject();
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
			return null;
		}
	}

	/**
	 * Returns the most severe of two results, <code>null</code> being the least severe, then
	 * <code>UNDEFINED</code>. A class with ignored tests which all the others passed is passed:
	 * <code>IGNORED</code> ranks below <code>OK</code>, like the status of the JUnit view.
	 *
	 * @param first the first result, can be <code>null</code>
	 * @param second the second result, can be <code>null</code>
	 * @return the most severe result, <code>null</code> if both are
	 */
	public static Result worst(Result first, Result second) {
		return severity(second) > severity(first) ? second : first;
	}

	private static int severity(Result result) {
		if (result == Result.ERROR) {
			return 5;
		} else if (result == Result.FAILURE) {
			return 4;
		} else if (result == Result.OK) {
			return 3;
		} else if (result == Result.IGNORED) {
			return 2;
		} else if (result == Result.UNDEFINED) {
			return 1;
		}
		return 0;
	}
}
