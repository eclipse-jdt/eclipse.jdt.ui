/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 ******************************************************************************/

package org.eclipse.jdt.ui.tests.browsing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.browsing.PackagesView;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTableViewer;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;


public class MockPluginView extends PackagesView {

	StructuredViewer fViewer;
	private IContentProvider fContentProvider;
	private boolean fRefreshHappened;
	private boolean fRemoveHappened;
	private boolean fAddHappened;
	
	private List fRemovedObject;
	private List fAddedObject;
	private List fAddedParentObject;
	private List fRefreshedObject;

	private boolean fListState;
	
	public MockPluginView() {
		super();
		fRefreshedObject= new ArrayList();
		fAddedObject= new ArrayList();
		fRemovedObject= new ArrayList();
		fAddedParentObject= new ArrayList();
		fListState= false;
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {

		//create viewer
		//super.createViewer(parent);
		this.fViewer= createViewer(parent);

		//create my contentProvider
		fContentProvider= super.createContentProvider();
		fContentProvider.inputChanged(this.fViewer, null, null);

		//set content provider
		fViewer.setContentProvider(fContentProvider);

	}
	
	/* Override so that tests can set layout state.
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.browsing.PackagesView#isInListState()
	 */
	protected boolean isInListState(){
		return fListState;	
	}
	
	protected StructuredViewer createViewer(Composite parent){
		if(isInListState())
			return new TestProblemTableViewer(parent, SWT.MULTI);
		else
			return new TestProblemTreeViewer(parent, SWT.MULTI);
	}

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
	public void setFocus() {
	}
	
	public StructuredViewer getTreeViewer(){
		return fViewer;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#findElementToSelect(org.eclipse.jdt.core.IJavaElement)
	 */
	protected IJavaElement findElementToSelect(IJavaElement je) {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#getHelpContextId()
	 */
	protected String getHelpContextId() {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#isValidInput(java.lang.Object)
	 */
	protected boolean isValidInput(Object element) {
		return false;
	}
	
	
	public List getRefreshedObject() {
		return fRefreshedObject;
	}

	public boolean hasRefreshHappened() {
		return fRefreshHappened;
	}

	public List getAddedParentObject() {
		return fAddedParentObject;
	}

	public boolean hasAddHappened() {
		return fAddHappened;
	}

	public List getRemovedObject() {
		return fRemovedObject;
	}

	public boolean hasRemoveHappened() {
		return fRemoveHappened;
	}
	
	/**
	 * Returns the object added to the viewer
	 * @return Object
	 */
	public List getAddedObject() {
		return fAddedObject;
	}

	private class TestProblemTreeViewer extends ProblemTreeViewer {

		public TestProblemTreeViewer(Composite parent, int flag) {
			super(parent, flag);
		}

		public void refresh(Object object) {
			fRefreshHappened= true;
			fRefreshedObject.add(object);
		}

		public void remove(Object object) {
			fRemoveHappened= true;
			fRemovedObject.add(object);
		}

		public void add(Object parentObject, Object object) {
			fAddHappened= true;
			fAddedObject.add(object);
			fAddedParentObject.add(parentObject);
		}

	}
	
	private class TestProblemTableViewer extends ProblemTableViewer {

		public TestProblemTableViewer(Composite parent, int flag) {
			super(parent, flag);
		}

		public void refresh(Object object) {
			fRefreshHappened= true;
			fRefreshedObject.add(object);
		}

		public void remove(Object object) {
			fRemoveHappened= true;
			fRemovedObject.add(object);
		}

		public void add(Object object) {
			fAddHappened= true;
			fAddedObject.add(object);
		}

	}
}
