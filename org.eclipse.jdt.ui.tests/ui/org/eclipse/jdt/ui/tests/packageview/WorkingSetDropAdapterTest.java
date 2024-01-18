/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.dnd.DND;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IFolder;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.packageview.WorkingSetDropAdapter;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;

public class WorkingSetDropAdapterTest {

	private IJavaProject fProject;
	private PackageExplorerPart fPackageExplorer;
	private Accessor fPackageExplorerPartAccessor;
	private WorkingSetDropAdapter fAdapter;


	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("Test", "bin");
		IWorkbenchPage activePage= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		fPackageExplorer= (PackageExplorerPart)activePage.showView(JavaUI.ID_PACKAGES);
		fPackageExplorerPartAccessor= new Accessor(fPackageExplorer, PackageExplorerPart.class.getName(), getClass().getClassLoader());
		fAdapter= new WorkingSetDropAdapter(fPackageExplorer);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
		IWorkbenchPage activePage= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		activePage.hideView(fPackageExplorer);
		assertTrue(fPackageExplorer.getTreeViewer().getTree().isDisposed());
	}

	@Test
	public void testInvalidTarget2() throws Exception {
		ITreeSelection selection= createSelection(fProject, null);

		performDnD(DND.DROP_NONE, selection, fProject);
	}

	@Test
	public void testInvalidSource1() throws Exception {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "src");

		ITreeSelection selection= createSelection(root, null);
		IWorkingSet target= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Target", new IAdaptable[] {fProject});
		performDnD(DND.DROP_NONE, selection, target);
	}

	@Test
	public void testInvalidSource2() throws Exception {
		JavaProjectHelper.addSourceContainer(fProject, "src");
		IFolder folder= fProject.getProject().getFolder("folder");
		folder.create(true, true, null);

		ITreeSelection selection= createSelection(folder, null);
		IWorkingSet target= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Target", new IAdaptable[] {fProject});
		performDnD(DND.DROP_NONE, selection, target);
	}

	@Test
	public void testAddProject() throws Exception {
		ITreeSelection selection= createSelection(fProject, null);

		IWorkingSet target= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Target", new IAdaptable[0]);
		performDnD(DND.DROP_COPY, selection, target);
		IAdaptable[] elements= target.getElements();
		assertEquals(1, elements.length);
		assertEquals(fProject, elements[0]);
	}

	@Test
	public void testMoveProject() throws Exception {
		IWorkingSet source= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Source", new IAdaptable[] {fProject});
		List<TreePath> treePathes= new ArrayList<>();
		treePathes.add(new TreePath(new Object[] {source, fProject}));
		ITreeSelection selection= createSelection(fProject, treePathes);

		IWorkingSet target= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Target", new IAdaptable[0]);
		performDnD(DND.DROP_MOVE, selection, target);
		IAdaptable[] elements= target.getElements();
		assertEquals(1, elements.length);
		assertEquals(fProject, elements[0]);
		elements= source.getElements();
		assertEquals(0, elements.length);
	}

	@Test
	public void testMoveToOthersProject() throws Exception {
		IWorkingSet source= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Source", new IAdaptable[] {fProject});
		List<TreePath> treePathes= new ArrayList<>();
		treePathes.add(new TreePath(new Object[] {source, fProject}));
		ITreeSelection selection= createSelection(fProject, treePathes);

		IWorkingSet target= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Target", new IAdaptable[0]);
		target.setId(IWorkingSetIDs.OTHERS);
		performDnD(DND.DROP_MOVE, selection, target);
		IAdaptable[] elements= target.getElements();
		// assert that the target doesn't have an element yet. The others working set
		// is updated by the updater through a change of the source working set
		assertEquals(0, elements.length);
		elements= source.getElements();
		assertEquals(0, elements.length);
	}

	@Test
	public void testRearrange1() throws Exception {
		IWorkingSet workingSets[]= createJavaWorkingSets(new String[] { "ws1", "ws2", "ws3" });

		setWorkingSets(workingSets);
		ITreeSelection selection= createSelection(workingSets[2], null);
		performDnD(DND.DROP_MOVE, selection, workingSets[0], JdtViewerDropAdapter.LOCATION_BEFORE);
		IWorkingSet[] actual= fPackageExplorer.getWorkingSetModel().getActiveWorkingSets();
		assertEquals(workingSets[2], actual[0]);
		assertEquals(workingSets[0], actual[1]);
		assertEquals(workingSets[1], actual[2]);
	}

	private static IWorkingSet[] createJavaWorkingSets(String[] names) {
		IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
		for (String name : names) {
			IWorkingSet workingSet= workingSetManager.getWorkingSet(name);
			if (workingSet != null)
				workingSetManager.removeWorkingSet(workingSet);
		}
		IWorkingSet[] sets= new IWorkingSet[names.length];
		for (int i= 0; i < names.length; i++) {
			IWorkingSet workingSet= workingSetManager.createWorkingSet(names[i], new IAdaptable[0]);
			workingSet.setId(IWorkingSetIDs.JAVA);
			workingSetManager.addWorkingSet(workingSet);
			sets[i]= workingSet;
		}
		return sets;
	}

	@Test
	public void testRearrange2() throws Exception {
		IWorkingSet workingSets[]= createJavaWorkingSets(new String[] { "ws1", "ws2", "ws3" });

		setWorkingSets(workingSets);
		ITreeSelection selection= createSelection(workingSets[2], null);
		performDnD(DND.DROP_MOVE, selection, workingSets[0], JdtViewerDropAdapter.LOCATION_AFTER);
		IWorkingSet[] actual= fPackageExplorer.getWorkingSetModel().getActiveWorkingSets();
		assertEquals(workingSets[0], actual[0]);
		assertEquals(workingSets[2], actual[1]);
		assertEquals(workingSets[1], actual[2]);
	}

	@Test
	public void testRearrange3() throws Exception {
		IWorkingSet workingSets[]= createJavaWorkingSets(new String[] { "ws1", "ws2", "ws3" });

		setWorkingSets(workingSets);
		ITreeSelection selection= createSelection(workingSets[0], null);
		performDnD(DND.DROP_MOVE, selection, workingSets[2], JdtViewerDropAdapter.LOCATION_AFTER);
		IWorkingSet[] actual= fPackageExplorer.getWorkingSetModel().getActiveWorkingSets();
		assertEquals(workingSets[1], actual[0]);
		assertEquals(workingSets[2], actual[1]);
		assertEquals(workingSets[0], actual[2]);
	}

	private void setWorkingSets(IWorkingSet[] workingSets) {
		WorkingSetModel model= fPackageExplorer.getWorkingSetModel();
		if (model == null) {
			fPackageExplorerPartAccessor.invoke("createWorkingSetModel", null);
			model= fPackageExplorer.getWorkingSetModel();
		}
		model.setActiveWorkingSets(workingSets);
		model.configured();
		fPackageExplorer.rootModeChanged(PackageExplorerPart.WORKING_SETS_AS_ROOTS);
	}

	private ITreeSelection createSelection(Object selectedElement, List<TreePath> treePathes) {
		if (treePathes == null) {
			treePathes= new ArrayList<>();
			treePathes.add(new TreePath(new Object[] { selectedElement }));
		}
		return new TreeSelection(treePathes.toArray(new TreePath[treePathes.size()]),
			fPackageExplorer.getTreeViewer().getComparer());
	}

	private void performDnD(int validateResult, ITreeSelection selection, Object target) throws Exception {
		performDnD(validateResult, selection, target, DND.FEEDBACK_SELECT);

	}
	private void performDnD(int validateResult, ITreeSelection selection, Object target, int location) throws Exception {
		try {
			LocalSelectionTransfer.getTransfer().setSelection(selection);
			fAdapter.internalTestSetLocation(location);
			int result= fAdapter.internalTestValidateTarget(target, DND.DROP_DEFAULT);
			assertEquals(validateResult, result);
			if (validateResult != DND.DROP_NONE)
				fAdapter.internalTestDrop(result);
		} finally {
			LocalSelectionTransfer.getTransfer().setSelection(null);
		}
	}
}
