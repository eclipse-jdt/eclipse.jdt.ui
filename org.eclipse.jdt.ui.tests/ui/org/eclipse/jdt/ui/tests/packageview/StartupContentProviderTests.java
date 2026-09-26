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
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaProject;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.packageview.LibraryContainer;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

/** Tests that presentation-only queries do not initialize the Java model. */
public class StartupContentProviderTests {

	private static final class CountingJavaProject extends JavaProject {
		private int fResolutions;

		CountingJavaProject(IJavaProject project) {
			super(project.getProject(), (JavaModel) project.getJavaModel());
		}

		@Override
		public IClasspathEntry[] getResolvedClasspath() throws JavaModelException {
			fResolutions++;
			return super.getResolvedClasspath();
		}
	}

	private static final class RecordingContentProvider extends PackageExplorerContentProvider {
		private final List<Object> fRefreshes= new ArrayList<>();

		RecordingContentProvider(boolean flatLayout) {
			super(true);
			setIsFlatLayout(flatLayout);
			setShowLibrariesNode(true);
		}

		Collection<Runnable> process(IJavaElementDelta delta) throws JavaModelException {
			Collection<Runnable> updates= new ArrayList<>();
			processAffectedChildren(new IJavaElementDelta[] { delta }, updates);
			return updates;
		}

		@Override
		protected void postRefresh(List<Object> toRefresh, boolean updateLabels, Collection<Runnable> runnables) {
			fRefreshes.addAll(toRefresh);
			super.postRefresh(toRefresh, updateLabels, runnables);
		}
	}

	private IJavaProject fProject;
	private boolean fAutoBuilding;

	@BeforeEach
	public void setUp() throws Exception {
		fAutoBuilding= ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding();
		CoreUtility.setAutoBuilding(false);
		fProject= JavaProjectHelper.createJavaProject("StartupContentProviderTests", "bin");
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "src");
		root.createPackageFragment("p", true, null).createCompilationUnit("A.java", "package p; class A {}", true, null);
	}

	@AfterEach
	public void tearDown() throws Exception {
		try {
			if (fProject != null)
				JavaProjectHelper.delete(fProject);
		} finally {
			CoreUtility.setAutoBuilding(fAutoBuilding);
		}
	}

	private CountingJavaProject coldProject() throws JavaModelException {
		fProject.close();
		CountingJavaProject project= new CountingJavaProject(fProject);
		assertFalse(project.isOpen(), "Test must start without an open project model");
		return project;
	}

	private static IPackageFragmentRoot sourceRoot(CountingJavaProject project) {
		return project.getPackageFragmentRoot(project.getProject().getFolder("src"));
	}

	private static void assertNotResolved(CountingJavaProject project) {
		assertEquals(0, project.fResolutions, "Presentation query resolved the project classpath");
		assertFalse(project.isOpen(), "Presentation query opened the project model");
	}

	@Test
	public void packageParentDoesNotOpenProject() throws Exception {
		CountingJavaProject project= coldProject();
		IPackageFragmentRoot root= sourceRoot(project);
		assertEquals(root, new StandardJavaElementContentProvider().getParent(root.getPackageFragment("p")));
		assertNotResolved(project);
	}

	@Test
	public void defaultPackageParentDoesNotOpenProject() throws Exception {
		CountingJavaProject project= coldProject();
		IPackageFragmentRoot root= sourceRoot(project);
		assertEquals(root, new StandardJavaElementContentProvider().getParent(root.getPackageFragment("")));
		assertNotResolved(project);
	}

	@Test
	public void sourceRootParentDoesNotResolveClasspath() throws Exception {
		CountingJavaProject project= coldProject();
		assertEquals(project, new StandardJavaElementContentProvider().getParent(sourceRoot(project)));
		assertNotResolved(project);
	}

	@Test
	public void compilationUnitParentIsAvailableFromHandle() throws Exception {
		CountingJavaProject project= coldProject();
		IPackageFragment pack= sourceRoot(project).getPackageFragment("p");
		assertEquals(pack, new StandardJavaElementContentProvider().getParent(pack.getCompilationUnit("A.java")));
		assertNotResolved(project);
	}

	@Test
	public void moduleInfoStillSkipsDefaultPackage() throws Exception {
		CountingJavaProject project= coldProject();
		IPackageFragmentRoot root= sourceRoot(project);
		assertEquals(root, new StandardJavaElementContentProvider().getParent(root.getPackageFragment("").getCompilationUnit("module-info.java")));
		assertNotResolved(project);
	}

	@Test
	public void projectSourceRootIsStillSkipped() throws Exception {
		JavaProjectHelper.removeSourceContainer(fProject, "src");
		JavaProjectHelper.addSourceContainer(fProject, "");
		CountingJavaProject project= coldProject();
		IPackageFragmentRoot root= project.getPackageFragmentRoot(project.getProject());
		assertEquals(project, new StandardJavaElementContentProvider().getParent(root.getPackageFragment("")));
		assertNotResolved(project);
	}

	@Test
	public void missingJavaHandleStillHasParent() throws Exception {
		CountingJavaProject project= coldProject();
		IPackageFragmentRoot root= sourceRoot(project);
		assertEquals(root, new StandardJavaElementContentProvider().getParent(root.getPackageFragment("not.present")));
		assertNotResolved(project);
	}

	@Test
	public void nullAndMissingResourcesStillHaveNoParent() {
		StandardJavaElementContentProvider provider= new StandardJavaElementContentProvider();
		assertNull(provider.getParent(null));
		assertNull(provider.getParent(fProject.getProject().getFile("missing.txt")));
	}

	@Test
	public void projectAndModelParentsRemainAvailable() throws Exception {
		CountingJavaProject project= coldProject();
		StandardJavaElementContentProvider provider= new StandardJavaElementContentProvider();
		assertEquals(project.getJavaModel(), provider.getParent(project));
		assertNull(provider.getParent(project.getJavaModel()));
		assertNotResolved(project);
	}

	@Test
	public void openingWorkingCopyDoesNotResolveClasspathInFlatLayout() throws Exception {
		assertWorkingCopyDeltaDoesNotResolve(true);
	}

	@Test
	public void openingWorkingCopyDoesNotResolveClasspathInHierarchicalLayout() throws Exception {
		assertWorkingCopyDeltaDoesNotResolve(false);
	}

	private void assertWorkingCopyDeltaDoesNotResolve(boolean flatLayout) throws Exception {
		CountingJavaProject project= coldProject();
		IPackageFragmentRoot root= sourceRoot(project);
		ICompilationUnit cu= root.getPackageFragment("p").getCompilationUnit("A.java");
		JavaElementDelta delta= new JavaElementDelta(root);
		delta.changed(cu, IJavaElementDelta.F_PRIMARY_WORKING_COPY);
		RecordingContentProvider provider= new RecordingContentProvider(flatLayout);
		try {
			assertTrue(provider.process(delta).isEmpty(), "Working-copy lifecycle change must not refresh the tree");
			assertNotResolved(project);
		} finally {
			provider.dispose();
		}
	}

	@Test
	public void mixedWorkingCopyAndStructuralChangesStillRefresh() throws Exception {
		CountingJavaProject project= coldProject();
		IPackageFragmentRoot root= sourceRoot(project);
		IPackageFragment pack= root.getPackageFragment("p");
		JavaElementDelta delta= new JavaElementDelta(root);
		delta.changed(pack.getCompilationUnit("A.java"), IJavaElementDelta.F_PRIMARY_WORKING_COPY);
		delta.changed(pack.getCompilationUnit("B.java"), IJavaElementDelta.F_CONTENT);
		RecordingContentProvider provider= new RecordingContentProvider(true);
		try {
			assertFalse(provider.process(delta).isEmpty(), "A structural sibling must not be discarded");
			assertTrue(provider.fRefreshes.contains(pack.getCompilationUnit("B.java")));
		} finally {
			provider.dispose();
		}
	}

	@Test
	public void binaryFolderChildrenStillRefreshLibraryContainer() throws Exception {
		IPackageFragmentRoot original= JavaProjectHelper.addClassFolder(fProject, "classes", null, null);
		CountingJavaProject project= coldProject();
		IPackageFragmentRoot root= project.getPackageFragmentRoot(original.getResource());
		JavaElementDelta delta= new JavaElementDelta(root);
		delta.changed(root.getPackageFragment("p").getClassFile("A.class"), IJavaElementDelta.F_CONTENT);
		RecordingContentProvider provider= new RecordingContentProvider(true);
		try {
			assertFalse(provider.process(delta).isEmpty());
			assertTrue(provider.fRefreshes.contains(new LibraryContainer(project)), "Keep the library-container refresh from bug 357450");
			assertTrue(provider.fRefreshes.contains(root.getResource()));
		} finally {
			provider.dispose();
		}
	}
}
