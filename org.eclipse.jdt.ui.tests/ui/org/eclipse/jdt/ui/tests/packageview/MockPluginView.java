/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.ui.tests.packageview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;
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
	public List fRefreshedObjects;
	private boolean fRemoveHappened;
	private boolean fAddHappened;
	
	private Object fRemovedObject;
	private Object fAddedObject;
	private Object fAddedParentObject;


	/**
	 * Constructor for MockPluginView.
	 */
	public MockPluginView() {
		super();
		fRefreshedObjects= new ArrayList();
	}
	
	public void resetRefreshedObjects(){
		fRefreshedObjects= new ArrayList();
	}


	/**
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
	
	
		
		//create viewer
		fViewer= createViewer(parent);
		
		//create my contentProvider
		contentProvider= createContentProvider();
		contentProvider.inputChanged(fViewer, null, null);
		
		//JavaCore.removeElementChangedListener(contentProvider);
		
		//set content provider
		fViewer.setContentProvider(contentProvider);
		
		
	}
	
	/**
	 * Method createViewer.
	 * @param parent
	 * @return TreeViewer
	 */
	public TreeViewer createViewer(Composite parent) {
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

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
	}
	
	public TreeViewer getTreeViewer(){
		return fViewer;
	}
	
	/**
	 * Returns the fRefreshHappened.
	 * @return boolean
	 */
	public boolean isFRefreshHappened() {
		return fRefreshHappened;
	}

	/**
	 * Sets the fRefreshHappened.
	 * @param fRefreshHappened The fRefreshHappened to set
	 */
	public void setFRefreshHappened(boolean fRefreshHappened) {
		fRefreshHappened= fRefreshHappened;
	}

	/**
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#findElementToSelect(org.eclipse.jdt.core.IJavaElement)
	 */
	protected IJavaElement findElementToSelect(IJavaElement je) {
		return null;
	}

	/**
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#getHelpContextId()
	 */
	protected String getHelpContextId() {
		return null;
	}

	/**
	 * @see org.eclipse.jdt.internal.ui.browsing.JavaBrowsingPart#isValidInput(java.lang.Object)
	 */
	protected boolean isValidInput(Object element) {
		return false;
	}
	
	private class TestProblemTreeViewer extends ProblemTreeViewer{
		
		
		public TestProblemTreeViewer(Composite parent, int flag){
			super(parent,flag);
		}
		
		public void refresh(Object object){
			fRefreshHappened= true;
			fRefreshedObjects.add(object);	
			super.refresh(object);
		}
		
		public void remove (Object object){
			fRemoveHappened= true;
			fRemovedObject= object;
			super.remove(object);	
		}
		
		public void add(Object parentObject, Object object){
			fAddHappened= true;
			super.add(parentObject, object);	
			fAddedObject= object;
			fAddedParentObject= parentObject;
		}
	}

	/**
	 * Returns the fRefreshedObject.
	 * @return Object
	 */
	public boolean getFRefreshedObject(Object c) {
		return fRefreshedObjects.contains(c);
	}

	/**
	 * Sets the fRefreshedObject.
	 * @param fRefreshedObject The fRefreshedObject to set
	 */
	public void setFRefreshedObject(Object fRefreshedObject) {
		fRefreshedObjects.add(fRefreshedObject);
	}

	/**
	 * Returns the fAddedParentObject.
	 * @return Object
	 */
	public Object getFAddedParentObject() {
		return fAddedParentObject;
	}

	/**
	 * Returns the fAddHappened.
	 * @return boolean
	 */
	public boolean isFAddHappened() {
		return fAddHappened;
	}

	/**
	 * Returns the fRemovedObject.
	 * @return Object
	 */
	public Object getFRemovedObject() {
		return fRemovedObject;
	}

	/**
	 * Returns the fRemoveHappened.
	 * @return boolean
	 */
	public boolean isFRemoveHappened() {
		return fRemoveHappened;
	}

	/**
	 * Sets the fAddedParentObject.
	 * @param fAddedParentObject The fAddedParentObject to set
	 */
	public void setFAddedParentObject(Object fAddedParentObject) {
		fAddedParentObject= fAddedParentObject;
	}

	/**
	 * Sets the fAddHappened.
	 * @param fAddHappened The fAddHappened to set
	 */
	public void setFAddHappened(boolean fAddHappened) {
		fAddHappened= fAddHappened;
	}

	/**
	 * Sets the fRemovedObject.
	 * @param fRemovedObject The fRemovedObject to set
	 */
	public void setFRemovedObject(Object fRemovedObject) {
		fRemovedObject= fRemovedObject;
	}

	/**
	 * Sets the fRemoveHappened.
	 * @param fRemoveHappened The fRemoveHappened to set
	 */
	public void setFRemoveHappened(boolean fRemoveHappened) {
		fRemoveHappened= fRemoveHappened;
	}

	/**
	 * Returns the fAddedObject.
	 * @return Object
	 */
	public Object getFAddedObject() {
		return fAddedObject;
	}

	/**
	 * Sets the fAddedObject.
	 * @param fAddedObject The fAddedObject to set
	 */
	public void setFAddedObject(Object fAddedObject) {
		fAddedObject= fAddedObject;
	}
	
	/**
	 * Method setFolding.
	 * @param b
	 */
	public void setFolding(boolean b) {
		JavaPlugin.getDefault().getPreferenceStore().setValue(AppearancePreferencePage.PREF_FOLD_PACKAGES_IN_PACKAGE_EXPLORER, b);
	}


}
