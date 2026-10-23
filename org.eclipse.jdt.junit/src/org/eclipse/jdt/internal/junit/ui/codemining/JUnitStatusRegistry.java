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

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestRunSession;

import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.TestMethodFinder;

/**
 * Remembers the result of the last run of each test method, so that it can be shown by the JUnit
 * status code mining. Only results are kept, not the test run sessions.
 * <p>
 * Several results of one run can belong to the same method (parameterized, repeated and dynamic
 * tests): the worst one is kept. The first result of a method in a new run replaces the
 * results of the previous runs for this method.
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
		volatile long fRun;

		final Map<List<String>, Result> fResults= new HashMap<>();
	}

	/**
	 * The method of a test, as given by the unique id of a JUnit 5 test.
	 *
	 * @param className the binary name of the class, with dollars for member types
	 * @param methodName the name of the method
	 * @param parameterTypes the fully qualified names of the parameter types
	 */
	public record MethodReference(String className, String methodName, String[] parameterTypes) {
	}

	// fully qualified class name -> name of the launched project -> method name -> results
	private final Map<String, Map<String, Map<String, MethodResults>>> fResults= new ConcurrentHashMap<>();

	private final Map<ITestRunSession, Long> fRuns= new WeakHashMap<>();

	private final AtomicLong fRunCounter= new AtomicLong();

	private final List<Runnable> fFinishedListeners= new CopyOnWriteArrayList<>();

	private final TestRunListener fListener= new TestRunListener() {
		@Override
		public void sessionStarted(ITestRunSession session) {
			getRun(session);
		}

		@Override
		public void testCaseFinished(ITestCaseElement testCaseElement) {
			record(testCaseElement);
		}

		@Override
		public void sessionFinished(ITestRunSession session) {
			notifyListeners();
		}
	};

	private final IResourceChangeListener fProjectListener= event -> {
		if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
			if (event.getDelta() != null) {
				for (IResourceDelta delta : event.getDelta().getAffectedChildren(IResourceDelta.REMOVED)) {
					removeProject(delta.getResource().getName()); // deleted, renamed or moved
				}
			}
		} else if (event.getResource() instanceof IProject project) {
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

	private long getRun(ITestRunSession session) {
		synchronized (fRuns) {
			return fRuns.computeIfAbsent(session, _ -> fRunCounter.incrementAndGet());
		}
	}

	private void record(ITestCaseElement testCaseElement) {
		ITestRunSession session= testCaseElement.getTestRunSession();
		IJavaProject launchedProject= session.getLaunchedProject();
		if (launchedProject == null) {
			return;
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
			int bracket= methodName.indexOf('[');
			methodName= bracket > 0 ? methodName.substring(0, bracket) : methodName;
		}
		record(getRun(session), launchedProject.getElementName(), className, methodName, parameterTypes, testCaseElement.getTestResult(false));
	}

	/**
	 * Reads the unique id of a JUnit 5 test, such as
	 * <code>[engine:junit-jupiter]/[class:p.Outer]/[nested-class:Inner]/[test-template:m(java.lang.String)]/[test-template-invocation:#1]</code>.
	 *
	 * @param uniqueId the unique id, can be <code>null</code>
	 * @return the class and the method of the test, or <code>null</code> if the id has none
	 */
	public static MethodReference parseUniqueId(String uniqueId) {
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
				case "class" -> className= value; //$NON-NLS-1$
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
		return className == null || methodName == null ? null : new MethodReference(className, methodName, parameterTypes);
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
	 * Records the result of a test method in a test run. The results of a method of an earlier run
	 * are replaced, the ones of the other methods are kept. The project of the test class is only
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
			if (run < methodResults.fRun) {
				return; // result of an older run, which finished late
			}
			if (run > methodResults.fRun) {
				methodResults.fRun= run;
				methodResults.fResults.clear();
			}
			methodResults.fResults.merge(types, result, JUnitStatusRegistry::worst);
		}
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
				for (IType supertype : fType.newSupertypeHierarchy(monitor).getAllSupertypes(fType)) {
					if (!"java.lang.Object".equals(supertype.getFullyQualifiedName())) { //$NON-NLS-1$
						methods.addAll(Arrays.asList(supertype.getMethods()));
					}
				}
				fInheritedMethods= List.copyOf(methods);
			}
			return fInheritedMethods;
		}

		/**
		 * Returns the result of the last run of the given method.
		 *
		 * @param method the test method, declared by the class
		 * @return the result, or <code>null</code> if the method has not been run
		 * @throws JavaModelException if the method cannot be resolved
		 */
		public Result findResult(IMethod method) throws JavaModelException {
			return fMethods == null ? null : match(method, fMethods.get(method.getElementName()));
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
				Set<String> unresolved= new HashSet<>(fMethods.keySet());
				for (IMethod method : fType.getMethods()) {
					unresolved.remove(method.getElementName());
					combined= worst(combined, match(method, fMethods.get(method.getElementName())));
				}
				// the other recorded methods count only if a superclass has them, not if they were renamed or deleted
				if (!unresolved.isEmpty()) {
					for (IMethod method : getInheritedMethods(monitor)) {
						if (unresolved.contains(method.getElementName())) {
							combined= worst(combined, match(method, fMethods.get(method.getElementName())));
						}
					}
				}
			}
			// the tests of the member classes belong to the class, such as the ones run by @RunWith(Enclosed.class)
			for (Results member : fMembers) {
				combined= worst(combined, member.findTypeResult(monitor));
			}
			return combined;
		}
	}

	private static Result match(IMethod method, Map<List<String>, Result> results) throws JavaModelException {
		Result combined= null;
		if (results != null) {
			for (Map.Entry<List<String>, Result> entry : results.entrySet()) {
				if (entry.getKey() == null || isMethodOf(method, entry.getKey())) {
					combined= worst(combined, entry.getValue());
				}
			}
		}
		return combined;
	}

	/**
	 * Tells whether the parameter types given by the test runner are the ones of the method. They
	 * can be written differently from the source (member classes, erased type variables, arrays):
	 * in this case, the method matches if it is the only one with its name and the types look like
	 * the declared ones, so that a method whose parameters were edited does not get the old result.
	 */
	private static boolean isMethodOf(IMethod method, List<String> parameterTypes) throws JavaModelException {
		IMethod found= TestMethodFinder.findMethod(method.getDeclaringType(), method.getElementName(), parameterTypes.toArray(String[]::new));
		if (found != null) {
			return method.equals(found);
		}
		String[] declared= method.getParameterTypes();
		if (declared.length != parameterTypes.size()
				|| Arrays.stream(method.getDeclaringType().getMethods()).filter(m -> m.getElementName().equals(method.getElementName())).count() != 1) {
			return false;
		}
		Set<String> typeVariables= new HashSet<>();
		for (ITypeParameter typeParameter : method.getTypeParameters()) {
			typeVariables.add(typeParameter.getElementName());
		}
		for (ITypeParameter typeParameter : method.getDeclaringType().getTypeParameters()) {
			typeVariables.add(typeParameter.getElementName());
		}
		for (int i= 0; i < declared.length; i++) {
			if (!isSimilar(parameterTypes.get(i), declared[i], typeVariables)) {
				return false;
			}
		}
		return true;
	}

	/** Tells whether the type name of the runner can be the erasure of the declared type signature. */
	private static boolean isSimilar(String recorded, String declared, Set<String> typeVariables) {
		String element= Signature.getElementType(Signature.getTypeErasure(declared));
		String declaredName= Signature.getSimpleName(Signature.toString(element));
		if (typeVariables.contains(declaredName)) {
			return true; // erased to its bound
		}
		String recordedName= recorded.replace("[]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		recordedName= recordedName.substring(Math.max(recordedName.lastIndexOf('.'), recordedName.lastIndexOf('$')) + 1);
		return recordedName.equals(declaredName);
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
				members.add(getResults(memberType, depth + 1));
			}
		}
		// the name of a member class is written with a dollar by JUnit 5 and with a dot by the other kinds
		Set<String> names= new LinkedHashSet<>(List.of(type.getFullyQualifiedName('.'), type.getFullyQualifiedName()));
		IJavaProject project= type.getJavaProject();
		Map<String, MethodResults> latest= new HashMap<>();
		for (String name : names) {
			Map<String, Map<String, MethodResults>> launchedProjects= fResults.get(name);
			if (launchedProjects == null) {
				continue;
			}
			for (Map.Entry<String, Map<String, MethodResults>> launched : launchedProjects.entrySet()) {
				if (!launched.getKey().equals(project.getElementName()) && !isFoundFrom(launched.getKey(), type)) {
					continue;
				}
				// the latest run of each method wins, whatever the launched project
				launched.getValue().forEach((method, results) -> latest.merge(method, results, (first, second) -> first.fRun >= second.fRun ? first : second));
			}
		}
		if (latest.isEmpty()) {
			return new Results(type, null, members);
		}
		Map<String, Map<List<String>, Result>> copy= new HashMap<>();
		synchronized (fResults) {
			latest.forEach((method, results) -> copy.put(method, new HashMap<>(results.fResults)));
		}
		return new Results(type, copy, members);
	}

	/** Tells whether the class is found in the given project because it is on its classpath. */
	private static boolean isFoundFrom(String launchedProjectName, IType type) {
		IJavaProject launched= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(launchedProjectName));
		try {
			IType found= launched.exists() ? launched.findType(type.getFullyQualifiedName('.')) : null;
			return found != null && found.getJavaProject().equals(type.getJavaProject());
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
			return false;
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
