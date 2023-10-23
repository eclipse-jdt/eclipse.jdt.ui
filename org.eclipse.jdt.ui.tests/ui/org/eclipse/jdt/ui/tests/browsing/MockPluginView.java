/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

package org.eclipse.jdt.ui.tests.browsing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.browsing.PackagesView;
import org.eclipse.jdt.internal.ui.browsing.PackagesViewFlatContentProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTableViewer;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;


public class MockPluginView extends PackagesView {

	StructuredViewer fViewer;
	private IContentProvider fContentProvider;
	private boolean fRefreshHappened;
	private boolean fRemoveHappened;
	private boolean fAddHappened;

	private final List<Object> fRemovedObject;
	private final List<Object> fAddedObject;
	private final List<Object> fAddedParentObject;
	private final List<Object> fRefreshedObject;

	private boolean fRefreshLogging;

	// We have to make this static since it must be set
	// before we create an instance via showView.
	private static boolean fgListState;

	public MockPluginView() {
		super();
		fRefreshedObject= new ArrayList<>();
		fAddedObject= new ArrayList<>();
		fRemovedObject= new ArrayList<>();
		fAddedParentObject= new ArrayList<>();
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {

		//create viewer
		//super.createViewer(parent);
		fViewer= createViewer(parent);

		//create my contentProvider
		fContentProvider= super.createContentProvider();
		fContentProvider.inputChanged(fViewer, null, null);

		//set content provider
		fViewer.setContentProvider(fContentProvider);
		getSite().setSelectionProvider(fViewer);

	}

	/* Override so that tests can set layout state.
	 *
	 *
	 * @see org.eclipse.jdt.internal.ui.browsing.PackagesView#isInListState()
	 */
	@Override
	protected boolean isInListState(){
		return fgListState;
	}

	/**
	 * Set the view is in flat or hierarchical state.
	 * @param state
	 */
	static void setListState(boolean state){
		fgListState= state;
	}

	public void clear() {
		pushDisplay();

		fAddedObject.clear();
		fRefreshedObject.clear();
		fAddedParentObject.clear();
		fRemovedObject.clear();
	}


	@Override
	protected StructuredViewer createViewer(Composite parent){
		if(isInListState())
			return new TestProblemTableViewer(parent, SWT.MULTI);
		else
			return new TestProblemTreeViewer(parent, SWT.MULTI);
	}

	@Override
	public void dispose() {
		if (fViewer != null) {
			IContentProvider p = fViewer.getContentProvider();
			if(p!=null)
				p.dispose();
		}

		super.dispose();
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
	}

	public StructuredViewer getTreeViewer(){
		return fViewer;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#findElementToSelect(org.eclipse.jdt.core.IJavaElement)
	 */
	@Override
	protected IJavaElement findElementToSelect(IJavaElement je) {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#getHelpContextId()
	 */
	@Override
	protected String getHelpContextId() {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#isValidInput(java.lang.Object)
	 */
	@Override
	protected boolean isValidInput(Object element) {
		return false;
	}


	/**
	 * Returns a list of objects refreshed in the viewer.
	 * @return List
	 */
	public List<Object> getRefreshedObject() {
		return fRefreshedObject;
	}

	/**
	 * Returns true if a refresh action happened
	 * @return boolean
	 */
	public boolean hasRefreshHappened() {
		return fRefreshHappened;
	}

	/**
	 * Returns a list of the parents of added objects
	 * @return List
	 */
	public List<Object> getAddedParentObject() {
		return fAddedParentObject;
	}

	/**
	 * Returns a list of objects removed from the viewer
	 * @return List
	 */
	public List<Object> getRemovedObject() {
		return fRemovedObject;
	}

	/**
	 * Returns true if a remove action happened
     * @return boolean
	 */
	public boolean hasRemoveHappened() {
		return fRemoveHappened;
	}

	/**
	 * Returns the object added to the viewer
	 * @return List
	 */
	public List<Object> getAddedObject() {
		return fAddedObject;
	}

	/**
	 * Returns true if an add action happened on the viewer
	 * @return boolean
	 */
	public boolean hasAddHappened() {
		return fAddHappened;
	}

	/**
	 * force events from display
	 */
	public void pushDisplay() {
		boolean moreToDispatch= true;
		while (moreToDispatch) {
			Control ctrl= getTreeViewer().getControl();
			if (ctrl != null && !ctrl.isDisposed()) {
				moreToDispatch= getTreeViewer().getControl().getDisplay().readAndDispatch();
			} else
				moreToDispatch= false;
		}
	}

	private class TestProblemTreeViewer extends ProblemTreeViewer {

		public TestProblemTreeViewer(Composite parent, int flag) {
			super(parent, flag);
			super.setUseHashlookup(true);
		}

		@Override
		public void refresh(Object object) {
			fRefreshHappened= true;
			fRefreshedObject.add(object);
			if (fRefreshLogging)
				new Exception("Refresh tree item: " + object).printStackTrace(System.out);
		}

		@Override
		public void remove(Object object) {
			fRemoveHappened= true;
			fRemovedObject.add(object);
		}

		@Override
		public void add(Object parentObject, Object object) {
			fAddHappened= true;
			fAddedObject.add(object);
			fAddedParentObject.add(parentObject);
		}

	}

	private class TestProblemTableViewer extends ProblemTableViewer {

		public TestProblemTableViewer(Composite parent, int flag) {
			super(parent, flag);
			super.setUseHashlookup(true);
		}

		@Override
		public void refresh(Object object) {
			fRefreshHappened= true;
			fRefreshedObject.add(object);
			if (fRefreshLogging)
				new Exception("Refresh table item: " + object).printStackTrace(System.out);
			super.refresh(object);
		}

		@Override
		public void remove(Object object) {
			fRemoveHappened= true;
			fRemovedObject.add(object);
			super.remove(object);
		}

		@Override
		public void add(Object object) {
			fAddHappened= true;
			fAddedObject.add(object);
			super.add(object);
		}
	}

	public void setRefreshLoggingEnabled(boolean enabled) {
		fRefreshLogging= enabled;
		PackagesViewFlatContentProvider cp= (PackagesViewFlatContentProvider)fContentProvider;
		cp.setRefreshLoggingEnabled(enabled);
	}
}
