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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.InterfaceIndicatorLabelDecorator;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Tests that a busy type index leaves the decoration job free instead of blocking it.
 */
public class InterfaceIndicatorLabelDecoratorTest extends CoreTests {

	/**
	 * Records the overlays a decorator adds.
	 */
	private static class RecordingDecoration implements IDecoration {

		final List<ImageDescriptor> overlays= new ArrayList<>();

		@Override
		public void addOverlay(ImageDescriptor overlay) {
			overlays.add(overlay);
		}

		@Override
		public void addOverlay(ImageDescriptor overlay, int quadrant) {
			overlays.add(overlay);
		}

		@Override
		public void addPrefix(String prefix) {
		}

		@Override
		public void addSuffix(String suffix) {
		}

		@Override
		public void setForegroundColor(Color color) {
		}

		@Override
		public void setBackgroundColor(Color color) {
		}

		@Override
		public void setFont(Font font) {
		}

		@Override
		public IDecorationContext getDecorationContext() {
			return DecorationContext.DEFAULT_CONTEXT;
		}
	}

	/**
	 * Records the label updates a decorator requests.
	 */
	private static class RecordingDecorator extends InterfaceIndicatorLabelDecorator {

		final AtomicInteger fireCount= new AtomicInteger();

		@Override
		protected void fireChange(IJavaElement[] elements) {
			fireCount.incrementAndGet();
			super.fireChange(elements);
		}

		@Override
		protected void fireChange() {
			fireCount.incrementAndGet();
			super.fireChange();
		}
	}

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragment fPackage;

	private InterfaceIndicatorLabelDecorator fDecorator;

	@Before
	public void setUp() throws Exception {
		fJProject1= pts.getProject();
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackage= sourceFolder.createPackageFragment("test1", false, null);
		fDecorator= new InterfaceIndicatorLabelDecorator();
	}

	@After
	public void tearDown() throws Exception {
		getIndexManager().enable();
		fDecorator.dispose();
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	private static IndexManager getIndexManager() {
		return JavaModelManager.getIndexManager();
	}

	private ICompilationUnit createInterface() throws Exception {
		return fPackage.createCompilationUnit("IBase.java", """
			package test1;
			public interface IBase {
			}
			""", false, null);
	}

	/**
	 * Closes the unit so that the decorator has to ask the index instead of reading the flags from
	 * the already open model element.
	 */
	private static void close(ICompilationUnit cu) throws Exception {
		cu.close();
	}

	private RecordingDecoration decorate(Object element) {
		RecordingDecoration decoration= new RecordingDecoration();
		fDecorator.decorate(element, decoration);
		return decoration;
	}

	@Test
	public void testInterfaceOverlayFromOpenUnit() throws Exception {
		ICompilationUnit cu= createInterface();
		cu.open(null);

		RecordingDecoration decoration= decorate(cu);

		assertEquals(1, decoration.overlays.size());
		assertEquals(JavaPluginImages.DESC_OVR_INTERFACE, decoration.overlays.get(0));
	}

	@Test
	public void testInterfaceOverlayFromIndex() throws Exception {
		ICompilationUnit cu= createInterface();
		JavaProjectHelper.mustPerformDummySearch();
		close(cu);

		RecordingDecoration decoration= decorate(cu);

		assertEquals(1, decoration.overlays.size());
		assertEquals(JavaPluginImages.DESC_OVR_INTERFACE, decoration.overlays.get(0));
	}

	@Test
	public void testNoOverlayForPlainClass() throws Exception {
		ICompilationUnit cu= fPackage.createCompilationUnit("Plain.java", """
			package test1;
			public class Plain {
			}
			""", false, null);

		assertTrue(decorate(cu).overlays.isEmpty());
	}

	/**
	 * The decoration job must not wait for the index. The overlay is skipped and requested again
	 * once the index is ready.
	 */
	@Test
	public void testSkipsOverlayWhileIndexIsBusy() throws Exception {
		ICompilationUnit cu= createInterface();
		JavaProjectHelper.mustPerformDummySearch();
		close(cu);

		List<LabelProviderChangedEvent> events= Collections.synchronizedList(new ArrayList<>());
		ILabelProviderListener listener= events::add;
		fDecorator.addListener(listener);

		IndexManager indexManager= getIndexManager();
		indexManager.disable();
		RecordingDecoration decoration;
		try {
			// enqueue indexing work that the disabled indexer cannot process
			indexManager.indexAll(fJProject1.getProject());
			decoration= decorate(cu);
		} finally {
			indexManager.enable();
		}

		assertTrue("no overlay may be computed while the index is busy", decoration.overlays.isEmpty());

		assertTrue("a label update must be requested once the index is ready",
				new DisplayHelper() {
					@Override
					protected boolean condition() {
						return !events.isEmpty();
					}
				}.waitForCondition(Display.getDefault(), 10000));

		Object[] changed= events.get(0).getElements();
		assertNotNull(changed);
		assertEquals(1, changed.length);
		assertEquals(cu, changed[0]);

		assertEquals("the overlay is computed once the index is ready",
				JavaPluginImages.DESC_OVR_INTERFACE, decorate(cu).overlays.get(0));
	}

	@Test
	public void testNoLabelUpdateWhenIndexWasReady() throws Exception {
		ICompilationUnit cu= createInterface();
		JavaProjectHelper.mustPerformDummySearch();
		close(cu);

		List<LabelProviderChangedEvent> events= Collections.synchronizedList(new ArrayList<>());
		fDecorator.addListener(events::add);

		decorate(cu);
		DisplayHelper.sleep(Display.getDefault(), 500);

		assertTrue("nothing was skipped, so no update may be requested", events.isEmpty());
	}

	/**
	 * The workbench may dispose the decorator while the decoration job is still inside
	 * {@code decorate()}. Nothing may be scheduled after disposal.
	 */
	@Test
	public void testDecorateAfterDisposeSchedulesNothing() throws Exception {
		ICompilationUnit cu= createInterface();
		JavaProjectHelper.mustPerformDummySearch();
		close(cu);

		RecordingDecorator decorator= new RecordingDecorator();
		decorator.dispose();

		IndexManager indexManager= getIndexManager();
		indexManager.disable();
		try {
			indexManager.indexAll(fJProject1.getProject());
			decorator.decorate(cu, new RecordingDecoration());
		} finally {
			indexManager.enable();
		}

		// once the index is ready again a job scheduled by the disposed decorator would have fired
		JavaProjectHelper.mustPerformDummySearch();
		DisplayHelper.sleep(Display.getDefault(), 1000);

		assertEquals("a disposed decorator must not request label updates", 0, decorator.fireCount.get());
	}
}
