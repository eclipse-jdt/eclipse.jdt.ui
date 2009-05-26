/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.packageview;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.dnd.DND;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IFolder;

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.navigator.LocalSelectionTransfer;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.packageview.WorkingSetDropAdapter;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;

public class WorkingSetDropAdapterTest extends TestCase {

	private IJavaProject fProject;
	private PackageExplorerPart fPackageExplorer;
	private WorkingSetDropAdapter fAdapter;

	protected void setUp() throws Exception {
		super.setUp();
		fProject= JavaProjectHelper.createJavaProject("Test", "bin");
		IWorkbenchPage activePage= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		fPackageExplorer= (PackageExplorerPart)activePage.showView(JavaUI.ID_PACKAGES);
		fAdapter= new WorkingSetDropAdapter(fPackageExplorer);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
		IWorkbenchPage activePage= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		activePage.hideView(fPackageExplorer);
		assertTrue(fPackageExplorer.getTreeViewer().getTree().isDisposed());
		super.tearDown();
	}

	public void testInvalidTarget2() throws Exception {
		List selectedElements= new ArrayList();
		selectedElements.add(fProject);
		ITreeSelection selection= createSelection(selectedElements, null);

		performDnD(DND.DROP_NONE, selection, fProject);
	}

	public void testInvalidSource1() throws Exception {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "src");
		List selectedElements= new ArrayList();
		selectedElements.add(root);

		ITreeSelection selection= createSelection(selectedElements, null);
		IWorkingSet target= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Target", new IAdaptable[] {fProject});
		performDnD(DND.DROP_NONE, selection, target);
	}

	public void testInvalidSource2() throws Exception {
		JavaProjectHelper.addSourceContainer(fProject, "src");
		IFolder folder= fProject.getProject().getFolder("folder");
		folder.create(true, true, null);
		List selectedElements= new ArrayList();
		selectedElements.add(folder);

		ITreeSelection selection= createSelection(selectedElements, null);
		IWorkingSet target= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Target", new IAdaptable[] {fProject});
		performDnD(DND.DROP_NONE, selection, target);
	}

	public void testAddProject() throws Exception {
		List selectedElements= new ArrayList();
		selectedElements.add(fProject);
		ITreeSelection selection= createSelection(selectedElements, null);

		IWorkingSet target= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Target", new IAdaptable[0]);
		performDnD(DND.DROP_COPY, selection, target);
		IAdaptable[] elements= target.getElements();
		assertEquals(1, elements.length);
		assertEquals(fProject, elements[0]);
	}

	public void testMoveProject() throws Exception {
		List selectedElements= new ArrayList();
		selectedElements.add(fProject);
		IWorkingSet source= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Source", new IAdaptable[] {fProject});
		List treePathes= new ArrayList();
		treePathes.add(new TreePath(new Object[] {source, fProject}));
		ITreeSelection selection= createSelection(selectedElements, treePathes);

		IWorkingSet target= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Target", new IAdaptable[0]);
		performDnD(DND.DROP_MOVE, selection, target);
		IAdaptable[] elements= target.getElements();
		assertEquals(1, elements.length);
		assertEquals(fProject, elements[0]);
		elements= source.getElements();
		assertEquals(0, elements.length);
	}

	public void testMoveToOthersProject() throws Exception {
		List selectedElements= new ArrayList();
		selectedElements.add(fProject);
		IWorkingSet source= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"Source", new IAdaptable[] {fProject});
		List treePathes= new ArrayList();
		treePathes.add(new TreePath(new Object[] {source, fProject}));
		ITreeSelection selection= createSelection(selectedElements, treePathes);

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

	public void testRearrange1() throws Exception {
		IWorkingSet ws1= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"ws1", new IAdaptable[0]);
		IWorkingSet ws2= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"ws2", new IAdaptable[0]);
		IWorkingSet ws3= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"ws3", new IAdaptable[0]);
		fPackageExplorer.internalTestShowWorkingSets(new IWorkingSet[] {ws1, ws2, ws3});
		List selectedElements= new ArrayList();
		selectedElements.add(ws3);
		ITreeSelection selection= createSelection(selectedElements, null);
		performDnD(DND.DROP_MOVE, selection, ws1, JdtViewerDropAdapter.LOCATION_BEFORE);
		IWorkingSet[] actual= fPackageExplorer.getWorkingSetModel().getActiveWorkingSets();
		assertEquals(ws3, actual[0]);
		assertEquals(ws1, actual[1]);
		assertEquals(ws2, actual[2]);
	}

	public void testRearrange2() throws Exception {
		IWorkingSet ws1= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"ws1", new IAdaptable[0]);
		IWorkingSet ws2= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"ws2", new IAdaptable[0]);
		IWorkingSet ws3= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"ws3", new IAdaptable[0]);
		fPackageExplorer.internalTestShowWorkingSets(new IWorkingSet[] {ws1, ws2, ws3});
		List selectedElements= new ArrayList();
		selectedElements.add(ws3);
		ITreeSelection selection= createSelection(selectedElements, null);
		performDnD(DND.DROP_MOVE, selection, ws1, JdtViewerDropAdapter.LOCATION_AFTER);
		IWorkingSet[] actual= fPackageExplorer.getWorkingSetModel().getActiveWorkingSets();
		assertEquals(ws1, actual[0]);
		assertEquals(ws3, actual[1]);
		assertEquals(ws2, actual[2]);
	}

	public void testRearrange3() throws Exception {
		IWorkingSet ws1= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"ws1", new IAdaptable[0]);
		IWorkingSet ws2= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"ws2", new IAdaptable[0]);
		IWorkingSet ws3= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(
			"ws3", new IAdaptable[0]);
		fPackageExplorer.internalTestShowWorkingSets(new IWorkingSet[] {ws1, ws2, ws3});
		List selectedElements= new ArrayList();
		selectedElements.add(ws1);
		ITreeSelection selection= createSelection(selectedElements, null);
		performDnD(DND.DROP_MOVE, selection, ws3, JdtViewerDropAdapter.LOCATION_AFTER);
		IWorkingSet[] actual= fPackageExplorer.getWorkingSetModel().getActiveWorkingSets();
		assertEquals(ws2, actual[0]);
		assertEquals(ws3, actual[1]);
		assertEquals(ws1, actual[2]);
	}

	private ITreeSelection createSelection(List selectedElements, List treePathes) {
		if (treePathes == null) {
			treePathes= new ArrayList();
			for (Iterator iter= selectedElements.iterator(); iter.hasNext();) {
				treePathes.add(new TreePath(new Object[] { iter.next() }));
			}
		}
		return new TreeSelection((TreePath[])treePathes.toArray(new TreePath[treePathes.size()]),
			fPackageExplorer.getTreeViewer().getComparer());
	}

	private void performDnD(int validateResult, ITreeSelection selection, Object target) throws Exception {
		performDnD(validateResult, selection, target, DND.FEEDBACK_SELECT);

	}
	private void performDnD(int validateResult, ITreeSelection selection, Object target, int location) throws Exception {
		try {
			LocalSelectionTransfer.getInstance().setSelection(selection);
			fAdapter.internalTestSetLocation(location);
			int result= fAdapter.internalTestValidateTarget(target, DND.DROP_DEFAULT);
			assertEquals(validateResult, result);
			if (validateResult != DND.DROP_NONE)
				fAdapter.internalTestDrop(target, result);
		} finally {
			LocalSelectionTransfer.getInstance().setSelection(null);
		}
	}
}
