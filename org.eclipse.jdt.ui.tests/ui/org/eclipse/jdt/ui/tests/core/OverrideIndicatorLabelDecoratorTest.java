/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vogella GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.OverrideIndicatorLabelDecorator;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class OverrideIndicatorLabelDecoratorTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragment fPackage;

	private OverrideIndicatorLabelDecorator fDecorator;

	@Before
	public void setUp() throws Exception {
		fJProject1= pts.getProject();
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackage= sourceFolder.createPackageFragment("test1", false, null);
		fDecorator= new OverrideIndicatorLabelDecorator(null);
	}

	@After
	public void tearDown() throws Exception {
		fDecorator.dispose();
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	private ICompilationUnit createCU(String name, String contents) throws Exception {
		return fPackage.createCompilationUnit(name, contents, false, null);
	}

	private IMethod createOverridingFoo() throws Exception {
		createCU("Base.java", """
			package test1;
			public class Base {
			    public void foo() {}
			}
			""");
		ICompilationUnit sub= createCU("Sub.java", """
			package test1;
			public class Sub extends Base {
			    @Override public void foo() {}
			}
			""");
		return sub.getType("Sub").getMethod("foo", new String[0]);
	}

	@Test
	public void testOverrides() throws Exception {
		assertEquals(JavaElementImageDescriptor.OVERRIDES, fDecorator.computeAdornmentFlags(createOverridingFoo()));
	}

	@Test
	public void testImplements() throws Exception {
		createCU("IBase.java", """
			package test1;
			public interface IBase {
			    void foo();
			}
			""");
		ICompilationUnit sub= createCU("Sub.java", """
			package test1;
			public class Sub implements IBase {
			    @Override public void foo() {}
			}
			""");
		IMethod foo= sub.getType("Sub").getMethod("foo", new String[0]);

		assertEquals(JavaElementImageDescriptor.IMPLEMENTS, fDecorator.computeAdornmentFlags(foo));
	}

	@Test
	public void testSynchronizedOverride() throws Exception {
		createCU("Base.java", """
			package test1;
			public class Base {
			    public void foo() {}
			}
			""");
		ICompilationUnit sub= createCU("Sub.java", """
			package test1;
			public class Sub extends Base {
			    @Override public synchronized void foo() {}
			}
			""");
		IMethod foo= sub.getType("Sub").getMethod("foo", new String[0]);

		assertEquals(JavaElementImageDescriptor.OVERRIDES | JavaElementImageDescriptor.SYNCHRONIZED,
				fDecorator.computeAdornmentFlags(foo));
	}

	@Test
	public void testNoIndicatorWithoutSuperType() throws Exception {
		ICompilationUnit cu= createCU("Standalone.java", """
			package test1;
			public class Standalone {
			    public void foo() {}
			    private void bar() {}
			    public static void baz() {}
			    public Standalone() {}
			}
			""");

		assertEquals(0, fDecorator.computeAdornmentFlags(cu.getType("Standalone").getMethod("foo", new String[0])));
		assertEquals(0, fDecorator.computeAdornmentFlags(cu.getType("Standalone").getMethod("bar", new String[0])));
		assertEquals(0, fDecorator.computeAdornmentFlags(cu.getType("Standalone").getMethod("baz", new String[0])));
		assertEquals(0, fDecorator.computeAdornmentFlags(cu.getType("Standalone").getMethod("Standalone", new String[0])));
	}

	/**
	 * Without a listener the decorator does not watch builds, so it must never skip a hierarchy,
	 * not even while a build is running.
	 */
	@Test
	public void testDecoratesDuringBuildWithoutListener() throws Exception {
		IMethod foo= createOverridingFoo();

		int[] duringBuild= { -1 };
		runBuildWithProbe(() -> duringBuild[0]= fDecorator.computeAdornmentFlags(foo));

		assertEquals(JavaElementImageDescriptor.OVERRIDES, duringBuild[0]);
	}

	@Test
	public void testSkipsUncachedHierarchyDuringBuild() throws Exception {
		IMethod foo= createOverridingFoo();
		List<LabelProviderChangedEvent> events= Collections.synchronizedList(new ArrayList<>());
		ILabelProviderListener listener= events::add;

		// registered before the probe: resource change listeners are notified in registration order,
		// and a wrong order would fail this test rather than let it pass without testing anything
		fDecorator.addListener(listener);

		int[] duringBuild= { -1 };
		runBuildWithProbe(() -> duringBuild[0]= fDecorator.computeAdornmentFlags(foo));

		assertEquals("no super type hierarchy may be built while a build is running", 0, duringBuild[0]);
		assertEquals("the indicator is computed again once the build is done",
				JavaElementImageDescriptor.OVERRIDES, fDecorator.computeAdornmentFlags(foo));

		assertTrue("a label update must be requested for the skipped element",
				new DisplayHelper() {
					@Override
					protected boolean condition() {
						return !events.isEmpty();
					}
				}.waitForCondition(Display.getDefault(), 5000));
		assertArrayEquals(new Object[] { foo }, events.get(0).getElements());
	}

	@Test
	public void testNoLabelUpdateWhenNothingWasSkipped() throws Exception {
		createOverridingFoo();
		List<LabelProviderChangedEvent> events= Collections.synchronizedList(new ArrayList<>());
		fDecorator.addListener(events::add);

		runBuildWithProbe(() -> {
			// nothing decorated during this build
		});
		DisplayHelper.sleep(Display.getDefault(), 500);

		assertTrue("no listener must be notified when no decoration was skipped", events.isEmpty());
	}

	/**
	 * Runs a workspace build and executes the probe from a PRE_BUILD listener, so while the build
	 * is running.
	 */
	private void runBuildWithProbe(Runnable probe) throws Exception {
		IResourceChangeListener probeListener= event -> probe.run();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(probeListener, IResourceChangeEvent.PRE_BUILD);
		try {
			ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
		} finally {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(probeListener);
		}
	}
}
