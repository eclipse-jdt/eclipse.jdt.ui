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
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.Flags;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class MembersOrderPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String ALL_ENTRIES= "T,SI,SF,SM,I,F,C,M"; //$NON-NLS-1$
	private static final String PREF_OUTLINE_SORT_OPTION= PreferenceConstants.APPEARANCE_MEMBER_SORT_ORDER;

	public static final String CONSTRUCTORS= "C"; //$NON-NLS-1$
	public static final String FIELDS= "F"; //$NON-NLS-1$
	public static final String METHODS= "M"; //$NON-NLS-1$
	public static final String STATIC_METHODS= "SM"; //$NON-NLS-1$
	public static final String STATIC_FIELDS= "SF"; //$NON-NLS-1$
	public static final String INIT= "I"; //$NON-NLS-1$
	public static final String STATIC_INIT= "SI"; //$NON-NLS-1$
	public static final String TYPES= "T"; //$NON-NLS-1$

	private ListDialogField fSortOrderList;
	private final int DEFAULT= 0;


	private static List getSortOrderList(String string) {
		StringTokenizer tokenizer= new StringTokenizer(string, ","); //$NON-NLS-1$
		List entries= new ArrayList();
		for (int i= 0; tokenizer.hasMoreTokens(); i++) {
			String token= tokenizer.nextToken();
			entries.add(token);
		}
		return entries;
	}

	private static boolean isValidEntries(List entries) {
		StringTokenizer tokenizer= new StringTokenizer(ALL_ENTRIES, ","); //$NON-NLS-1$
		int i= 0;
		for (; tokenizer.hasMoreTokens(); i++) {
			String token= tokenizer.nextToken();
			if (!entries.contains(token))
				return false;
		}
		return i == entries.size();
	}

	public MembersOrderPreferencePage() {
		//set the preference store
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());

		String string= getPreferenceStore().getString(PREF_OUTLINE_SORT_OPTION);

		String upLabel= JavaUIMessages.getString("MembersOrderPreferencePage.button.up"); //$NON-NLS-1$
		String downLabel= JavaUIMessages.getString("MembersOrderPreferencePage.button.down"); //$NON-NLS-1$
		String[] buttonlabels= new String[] { upLabel, downLabel };

		fSortOrderList= new ListDialogField(null, buttonlabels, new MemberSortLabelProvider());
		fSortOrderList.setDownButtonIndex(1);
		fSortOrderList.setUpButtonIndex(0);

		//validate entries stored in store, false get defaults
		List entries= getSortOrderList(string);
		if (!isValidEntries(entries)) {
			string= JavaPlugin.getDefault().getPreferenceStore().getDefaultString(PREF_OUTLINE_SORT_OPTION);
			entries= getSortOrderList(string);
		}

		fSortOrderList.setElements(entries);
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.SORT_ORDER_PREFERENCE_PAGE);
	}

	/*
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
		data.horizontalAlignment= GridData.FILL_HORIZONTAL;
		composite.setLayoutData(data);

		createSortOrderListDialogField(composite, 3);
		return composite;
	}

	private void createSortOrderListDialogField(Composite composite, int span) {

		Label sortLabel= new Label(composite, SWT.NONE);
		sortLabel.setText(JavaUIMessages.getString("MembersOrderPreferencePage.label.description")); //$NON-NLS-1$

		GridData gridData= new GridData();
		gridData.horizontalAlignment= GridData.FILL_HORIZONTAL;
		gridData.horizontalSpan= span;
		sortLabel.setLayoutData(gridData);

		fSortOrderList.doFillIntoGrid(composite, span);
		LayoutUtil.setHorizontalGrabbing(fSortOrderList.getListControl(null));
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/*
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		String string= getPreferenceStore().getDefaultString(PREF_OUTLINE_SORT_OPTION);
		fSortOrderList.setElements(getSortOrderList(string));
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	//reorders elements in the Outline based on selection
	public boolean performOk() {
		//update outline view

		//save preferences
		IPreferenceStore store= getPreferenceStore();

		StringBuffer buf= new StringBuffer();
		List curr= fSortOrderList.getElements();
		for (Iterator iter= curr.iterator(); iter.hasNext();) {
			String s= (String) iter.next();
			buf.append(s);
			buf.append(',');
		}
		store.setValue(PREF_OUTLINE_SORT_OPTION, buf.toString());
		JavaPlugin.getDefault().savePluginPreferences();
		return true;
	}

	private class MemberSortLabelProvider extends LabelProvider {

		public MemberSortLabelProvider() {
		}
		
		/*
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(Object)
		 */
		public String getText(Object element) {

			if (element instanceof String) {
				String s= (String) element;
				if (s.equals(FIELDS)) {
					return JavaUIMessages.getString("MembersOrderPreferencePage.fields.label"); //$NON-NLS-1$
				} else if (s.equals(CONSTRUCTORS)) {
					return JavaUIMessages.getString("MembersOrderPreferencePage.constructors.label"); //$NON-NLS-1$
				} else if (s.equals(METHODS)) {
					return JavaUIMessages.getString("MembersOrderPreferencePage.methods.label"); //$NON-NLS-1$
				} else if (s.equals(STATIC_FIELDS)) {
					return JavaUIMessages.getString("MembersOrderPreferencePage.staticfields.label"); //$NON-NLS-1$
				} else if (s.equals(STATIC_METHODS)) {
					return JavaUIMessages.getString("MembersOrderPreferencePage.staticmethods.label"); //$NON-NLS-1$
				} else if (s.equals(INIT)) {
					return JavaUIMessages.getString("MembersOrderPreferencePage.initialisers.label"); //$NON-NLS-1$
				} else if (s.equals(STATIC_INIT)) {
					return JavaUIMessages.getString("MembersOrderPreferencePage.staticinitialisers.label"); //$NON-NLS-1$
				} else if (s.equals(TYPES)) {
					return JavaUIMessages.getString("MembersOrderPreferencePage.types.label"); //$NON-NLS-1$
				}
			}
			return ""; //$NON-NLS-1$
		}

		/*
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
				} else if (s.equals(CONSTRUCTORS)) {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, DEFAULT);
					//add a constructor adornment to the image descriptor
					descriptor= new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE);
				} else if (s.equals(METHODS)) {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, Flags.AccPublic);
				} else if (s.equals(STATIC_FIELDS)) {
					descriptor= JavaElementImageProvider.getFieldImageDescriptor(false, Flags.AccPublic);
					//add a constructor adornment to the image descriptor
					descriptor= new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.STATIC, JavaElementImageProvider.SMALL_SIZE);
				} else if (s.equals(STATIC_METHODS)) {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, Flags.AccPublic);
					//add a constructor adornment to the image descriptor
					descriptor= new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.STATIC, JavaElementImageProvider.SMALL_SIZE);
				} else if (s.equals(INIT)) {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, DEFAULT);
				} else if (s.equals(STATIC_INIT)) {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, DEFAULT);
					descriptor= new JavaElementImageDescriptor(descriptor, JavaElementImageDescriptor.STATIC, JavaElementImageProvider.SMALL_SIZE);
				} else if (s.equals(TYPES)) {
					descriptor= JavaElementImageProvider.getTypeImageDescriptor(false, true, Flags.AccPublic);
				} else {
					descriptor= JavaElementImageProvider.getMethodImageDescriptor(false, DEFAULT);
				}
				return registry.get(descriptor);
			}
			return null;
		}

	}

}
