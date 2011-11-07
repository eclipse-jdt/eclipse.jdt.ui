/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;

/**
 * Helper to test the PackageExplorerContentProvider.
 *
 * @since 2.1
 */
public class MockPluginView extends PackageExplorerPart {

	TreeViewer fViewer;
	private ITreeContentProvider contentProvider;
	private boolean fRefreshHappened;

	private boolean fRemoveHappened;
	private boolean fAddHappened;

	private final List fRefreshedObjects;
	private final List fRemovedObjects;

	private Object fAddedObject;
	private Object fAddedParentObject;

	/**
	 * Constructor for MockPluginView.
	 */
	public MockPluginView() {
		super();
		fRefreshedObjects= new ArrayList();
		fRemovedObjects= new ArrayList();
	}

	/**
	 * Creates only the viewer and the content provider.
	 *
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {

		//create viewer
		fViewer= createViewer(parent);

		//create my contentProvider
		contentProvider= createContentProvider();
		contentProvider.inputChanged(fViewer, null, null);

		//set content provider
		fViewer.setContentProvider(contentProvider);

	}

	private TreeViewer createViewer(Composite parent) {
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

	public TreeViewer getTreeViewer(){
		return fViewer;
	}

	private class TestProblemTreeViewer extends ProblemTreeViewer{

		public TestProblemTreeViewer(Composite parent, int flag){
			super(parent,flag);
		}

		public void refresh(Object object){
			fRefreshHappened= true;
			fRefreshedObjects.add(object);
		}

		public void refresh(final Object element, final boolean updateLabels) {
			fRefreshHappened= true;
			fRefreshedObjects.add(element);
		}

		public void remove(Object object) {
			fRemoveHappened= true;
			fRemovedObjects.add(object);
		}

		public void add(Object parentObject, Object object){
			fAddHappened= true;
			fAddedObject= object;
			fAddedParentObject= parentObject;
		}
		
		@Override
		public Widget[] testFindItems(Object element) {
			return new Widget[1]; // for https://bugs.eclipse.org/311212
		}
	}

	/**
	 * Returns whether the given object was refreshed.
	 * 
	 * @param c the object to test
	 * @return <code>true</code> if the object was refreshed
	 */
	public boolean wasObjectRefreshed(Object c) {
		return fRefreshedObjects.contains(c);
	}

	public List getRefreshedObject(){
		return fRefreshedObjects;
	}

	/**
	 * Returns the object added to the tree viewer
	 * @return Object
	 */
	public Object getParentOfAddedObject() {
		return fAddedParentObject;
	}

	/**
	 * Returns true if something was added to the viewer
	 * @return boolean
	 */
	public boolean hasAddHappened() {
		return fAddHappened;
	}

	/**
	 * Returns true if an object was removed from the viewer
	 * @return boolean
	 */
	public boolean hasRemoveHappened() {
		return fRemoveHappened;
	}
	/**
	 * Returns the object removed from the viewer
	 * @return Object
	 */
	public List getRemovedObjects() {
		return fRemovedObjects;
	}

	/**
	 * Returns the object added to the viewer
	 * @return Object
	 */
	public Object getAddedObject() {
		return fAddedObject;
	}

	/**
	 * Returns true if a refresh happened
	 * @return boolean
	 */
	public boolean hasRefreshHappened() {
		return fRefreshHappened;
	}

	/**
	 * Sets the folding preference.
	 * 
	 * @param fold <code>true</code> to enable folding, <code>false</code> otherwise
	 */
	public void setFolding(boolean fold) {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER, fold);
	}

	/**
	 *
	 */
	public void clear() {
		fRefreshedObjects.clear();
		fRemovedObjects.clear();
		fAddHappened= false;
		fRemoveHappened= false;
		fRefreshHappened= false;
		fAddedObject= null;
	}
}
