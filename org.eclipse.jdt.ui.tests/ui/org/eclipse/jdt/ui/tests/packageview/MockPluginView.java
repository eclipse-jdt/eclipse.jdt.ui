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

import org.eclipse.jdt.ui.PreferenceConstants;

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

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
	}
	
	public TreeViewer getTreeViewer(){
		return fViewer;
	}
	
	/**
	 * Returns true if a refresh happened
	 * @return boolean
	 */
	public boolean hasRefreshHappened() {
		return fRefreshHappened;
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
		}
		
		public void remove (Object object){
			fRemoveHappened= true;
			fRemovedObject= object;
		}
		
		public void add(Object parentObject, Object object){
			fAddHappened= true;
			fAddedObject= object;
			fAddedParentObject= parentObject;
		}
	}

	/**
	 * Returns the refreshed object.
	 * @return Object
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
	public boolean addHappened() {
		return fAddHappened;
	}

	/**
	 * Returns the object removed from the viewer
	 * @return Object
	 */
	public Object getRemovedObject() {
		return fRemovedObject;
	}

	/**
	 * Returns true if an object was removed from the viewer
	 * @return boolean
	 */
	public boolean removeHappened() {
		return fRemoveHappened;
	}

	/**
	 * Returns the object added to the viewer
	 * @return Object
	 */
	public Object getAddedObject() {
		return fAddedObject;
	}
	
	/**
	 * Sets the folding preference.
	 * @param fold
	 */
	public void setFolding(boolean fold) {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER, fold);
	}
}