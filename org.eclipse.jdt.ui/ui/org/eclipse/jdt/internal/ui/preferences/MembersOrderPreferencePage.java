/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.Flags;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class MembersOrderPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String PREF_OUTLINE_SORT_OPTION= "outlinesortoption"; //$NON-NLS-1$

	private static String CONSTRUCTORS= "Constructors"; //$NON-NLS-1$
	private static String FIELDS= "Fields"; //$NON-NLS-1$
	private static String METHODS= "Methods"; //$NON-NLS-1$
	private static String STATIC_METHODS="Static Methods"; //$NON-NLS-1$
	private static String STATIC_FIELDS="Static Fields"; //$NON-NLS-1$
	private static String INIT="Initializers"; //$NON-NLS-1$
	private static String STATIC_INIT="Static Initializers"; //$NON-NLS-1$
	private static String TYPES="Types"; //$NON-NLS-1$
	
	private final int DEFAULT= 0;
	
	private static final String[] fgDefaultSortOrder= new String[] {TYPES, STATIC_INIT, INIT, STATIC_FIELDS, FIELDS, CONSTRUCTORS, STATIC_METHODS, METHODS};
	private static String[] fgSortOrder;

	private ListDialogField fSortOrderList;

	//this should be abstracted in PreferencePage	
	public static void initDefaults(IPreferenceStore store) {
		//basically create the cache
		String order= store.getString(PREF_OUTLINE_SORT_OPTION);
		if ("".equals(order)) { //$NON-NLS-1$
			//must copy elements into sortOrder
			fgSortOrder= new String[fgDefaultSortOrder.length];
			System.arraycopy(fgDefaultSortOrder, 0, fgSortOrder, 0, fgDefaultSortOrder.length);
			fgSortOrder= fgDefaultSortOrder;
		} else {
			StringTokenizer tokenizer= new StringTokenizer(order, ","); //$NON-NLS-1$
			fgSortOrder= new String[tokenizer.countTokens()];
			for (int i= 0; tokenizer.hasMoreTokens(); i++) {
				String token= tokenizer.nextToken();
				fgSortOrder[i]= token;
			}
		}
	}
	
	public MembersOrderPreferencePage() {
		//set the preference store
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		
		String upLabel = JavaUIMessages.getString("MembersOrderPreferencePage.button.up"); //$NON-NLS-1$
		String downLabel = JavaUIMessages.getString("MembersOrderPreferencePage.button.down"); //$NON-NLS-1$
		String[] buttonlabels= new String[] {upLabel, downLabel};
				
		fSortOrderList= new ListDialogField(new MemberSortAdapter(), buttonlabels, new MemberSortLabelProvider());
		fSortOrderList.setDownButtonIndex(1);
		fSortOrderList.setUpButtonIndex(0);
		fSortOrderList.setElements(getSortOrderList());
	}
	
	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.SORT_ORDER_PREFERENCE_PAGE);
	}		

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);
		
		GridData data= new GridData();
		data.verticalAlignment= GridData.FILL;
		data.horizontalAlignment= GridData.FILL;
		composite.setLayoutData(data);

		createSortOrderListDialogField(composite, 3);
		return composite;
	}


	private void createSortOrderListDialogField(Composite composite, int span) {

		Label sortLabel= new Label(composite, SWT.NONE);
		sortLabel.setText(JavaUIMessages.getString("MembersOrderPreferencePage.label.description"));  //$NON-NLS-1$

		GridData gridData= new GridData();
		gridData.horizontalAlignment= GridData.FILL;
		gridData.horizontalSpan= span;
		sortLabel.setLayoutData(gridData);
		
		fSortOrderList.doFillIntoGrid(composite,span);
	}


	/**
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}


	private List getSortOrderList() {		
		return arrayToList(fgSortOrder);
	}

	private List arrayToList(String[] array) {
		List list= new ArrayList();
		for (int i= 0; i < array.length; i++) {
			String s= array[i];
			list.add(s);
		}
		return list;
	}
	
	private List getCurrentSortOrder(){
		return fSortOrderList.getElements();	
		
	}
	
	/**
	 * Performs a save to the store by calling performOK.
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	protected void performApply() {
		performOk();
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fSortOrderList.removeAllElements();
		fSortOrderList.setElements(arrayToList(fgDefaultSortOrder));
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	//reorders elements in the Outline based on selection
	public boolean performOk() {
		//update outline view
	
		//save preferences
		IPreferenceStore store= getPreferenceStore();

		StringBuffer buf= new StringBuffer();
		List curr= getCurrentSortOrder();
		fgSortOrder= (String[]) curr.toArray(new String[curr.size()]);
		for (Iterator iter= curr.iterator(); iter.hasNext();) {
			String s= (String) iter.next();
			buf.append(s);
			buf.append(","); //$NON-NLS-1$
		}
		if(store!=null) {
			store.setValue(PREF_OUTLINE_SORT_OPTION, buf.toString());
			JavaPlugin.getDefault().savePluginPreferences();
		}
		return true;
	}	

	/**
	 * Method getIndexOf.
	 * @param s
	 * @return int
	 */
	private static int getIndexOf(String s) {
		for (int i= 0; i < fgSortOrder.length; i++) {
			String item= (String)fgSortOrder[i];
			if(s.equals(item))
				return i;
		}
		return 0;
	}

	
	public static int getMethodOffset(boolean isStatic){
		if(isStatic)
			return getIndexOf(STATIC_METHODS);
		return getIndexOf(METHODS);
	}
	
	public static int getFieldOffset(boolean isStatic){
		if(isStatic)
			return getIndexOf(STATIC_FIELDS);
		return getIndexOf(FIELDS);
	}
	
	public static int getInitOffset(boolean isStatic){
		if(isStatic)	
			return getIndexOf(STATIC_INIT);
		return getIndexOf(INIT);	
	}
		
	public static int getTypeOffset(){
		return getIndexOf(TYPES);	
	}

	public static int getConstructorOffset(){
		return getIndexOf(CONSTRUCTORS);
	}
	
	private class MemberSortAdapter implements IListAdapter {

		public MemberSortAdapter() {
		}

		/**
		* @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#customButtonPressed(DialogField, int)
		*/
		public void customButtonPressed(DialogField field, int index) {
		}

		/**
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#selectionChanged(DialogField)
		 */
		public void selectionChanged(DialogField field) {
		}

	}
	

	private class MemberSortLabelProvider implements ILabelProvider {

		public MemberSortLabelProvider() {
		}
		/**
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(Object)
		 */
		public String getText(Object element) {
			if (element instanceof String) {
				return (String) element;
				//@change
			} else
				return ""; //$NON-NLS-1$
		}

		/**
		 * Not implemented
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener) {
		}

		/**
		 * Not implemented
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(Object, String)
		 */
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		/**
		 * Not implemented
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener) {
		}

		/**
		 * Not implemented
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose
		 */
		public void dispose() {
		}

		/**
		* @see org.eclipse.jface.viewers.ILabelProvider#getImage(Object)
		*/
		public Image getImage(Object element) {
			//access to image registry
			ImageDescriptorRegistry registry= JavaPlugin.getImageDescriptorRegistry();
			ImageDescriptor descriptor= null;

			if (element instanceof String) {
				String s= (String) element;
				if (s.equals(FIELDS)) {
					//0 will give the default field image	
					descriptor= JavaElementImageProvider.getFieldImageDescriptor(false, Flags.AccPublic);
					return registry.get(descriptor);
				} else if (s.equals(CONSTRUCTORS)) {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, DEFAULT);
					//add a constructor adornment to the image descriptor
					ImageDescriptor adorneddesciptor= new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE);
					return registry.get(adorneddesciptor);
				} else if (s.equals(METHODS)) {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, Flags.AccPublic);
					return registry.get(descriptor);
				} else if (s.equals(STATIC_FIELDS)) {
					descriptor= JavaElementImageProvider.getFieldImageDescriptor(false, Flags.AccPublic);
					//add a constructor adornment to the image descriptor
					ImageDescriptor adorneddesciptor= new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.STATIC, JavaElementImageProvider.SMALL_SIZE);
					return registry.get(adorneddesciptor);
				} else if (s.equals(STATIC_METHODS)) {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, Flags.AccPublic);
					//add a constructor adornment to the image descriptor
					ImageDescriptor adorneddesciptor= new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.STATIC, JavaElementImageProvider.SMALL_SIZE);
					return registry.get(adorneddesciptor);
				} else if (s.equals(INIT)) {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, DEFAULT);
					return registry.get(descriptor);
				} else if (s.equals(STATIC_INIT)) {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, DEFAULT);
					ImageDescriptor adorneddesciptor= new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.STATIC, JavaElementImageProvider.SMALL_SIZE);
					return registry.get(adorneddesciptor);
				} else if (s.equals(TYPES)) {
					descriptor= JavaElementImageProvider.getTypeImageDescriptor(false, true, Flags.AccPublic);
					return registry.get(descriptor);
				} else {
					//@change
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, DEFAULT);
					return registry.get(descriptor);
				}

			}
			return null;
		}

	}

}
