/*
 * Created on Apr 13, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.search.JavaSearchPage;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @author tma
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SearchParticipantsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private CheckedListDialogField fParticipantList;
	
	static class ParticipantLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return getImage(element);
		}

		public String getColumnText(Object element, int columnIndex) {
			return getText(element);
		}
		
		public String getText(Object element) {
			return ((IConfigurationElement)element).getAttribute("label"); //$NON-NLS-1$
		}

	}

	public SearchParticipantsPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setTitle(PreferenceMessages.getString("SearchParticipantsPreferencePage.title"));		  //$NON-NLS-1$
		setDescription(PreferenceMessages.getString("SearchParticipantsPreferencePage.description")); //$NON-NLS-1$
		
		String[] buttonLabels= new String[] { 
			PreferenceMessages.getString("SearchParticipantsPreferencePage.select_all"), //$NON-NLS-1$
			PreferenceMessages.getString("SearchParticipantsPreferencePage.deselect_all"),  //$NON-NLS-1$
		};
		
		fParticipantList= new CheckedListDialogField(null, buttonLabels, new ParticipantLabelProvider());
		fParticipantList.setLabelText(PreferenceMessages.getString("SearchParticipantsPreferencePage.participants.label")); //$NON-NLS-1$
		fParticipantList.setCheckAllButtonIndex(0);
		fParticipantList.setUncheckAllButtonIndex(1);
		
		initialize(readActiveParticipants());

	}

	private void initialize(Map activeParticipants) {
		fParticipantList.setElements(Arrays.asList(Platform.getPluginRegistry().getConfigurationElementsFor(JavaSearchPage.PARTICIPANT_EXTENSION_POINT)));
		fParticipantList.setCheckedElements(activeParticipants.values());
	}

	protected void performDefaults() {
    	initialize(readDefaultActiveParticipants());
		
		super.performDefaults();	
    }


    public boolean performOk() {
  		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
  		
  		List checked= fParticipantList.getCheckedElements();
  		
  		writeActiveParticpants(checked);
        return true;
    }

	
	private final static String STORE_ACTIVE_PARTICIPANTS= "JAVA_SEARCH_ACTIVE_PARTICIPANTS"; //$NON-NLS-1$

	private static void writeActiveParticpants(Collection participantIDs) {
		String[] ids= new String[participantIDs.size()];
		Iterator participants= participantIDs.iterator();
		int i= 0;
		while (participants.hasNext()) {
			IConfigurationElement element= (IConfigurationElement)participants.next();
			ids[i++]=  element.getAttribute("id"); //$NON-NLS-1$
		}
		String idString= packOrderList(ids);
		JavaPlugin.getDefault().getPreferenceStore().putValue(STORE_ACTIVE_PARTICIPANTS, idString);
  		JavaPlugin.getDefault().savePluginPreferences();
	}	
	
	public static Map readActiveParticipants() {
		String idString= JavaPlugin.getDefault().getPreferenceStore().getString(STORE_ACTIVE_PARTICIPANTS);
		return createActiveParticipants(idString);
	}
	
	private static Map readDefaultActiveParticipants() {
		String idString= JavaPlugin.getDefault().getPreferenceStore().getDefaultString(STORE_ACTIVE_PARTICIPANTS);
		return createActiveParticipants(idString);
	}

	private static Map createActiveParticipants(String idList) {
		String[] ids= unpackOrderList(idList);
		HashMap activeParticipants= new HashMap();
		if (ids != null) {
			IConfigurationElement[] allParticipants= Platform.getPluginRegistry().getConfigurationElementsFor(JavaSearchPage.PARTICIPANT_EXTENSION_POINT);
			for (int i= 0; i < allParticipants.length; i++) {
				for (int j= 0; j < ids.length; j++) {
					if (ids[j].equals(allParticipants[i].getAttribute("id"))) //$NON-NLS-1$
						activeParticipants.put(ids[j], allParticipants[i]);
				}
			}
		}
		return activeParticipants;
	}

	private static String[] unpackOrderList(String str) {
		StringTokenizer tok= new StringTokenizer(str, ";"); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < nTokens; i++) {
			res[i]= tok.nextToken();
		}
		return res;
	}
	
	private static String packOrderList(String[] orderList) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < orderList.length; i++) {
			buf.append((String) orderList[i]);
			buf.append(';');
		}
		return buf.toString();
	}	

	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		
			Composite composite= new Composite(parent, SWT.NONE);
			
			GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.marginWidth= 0;
			layout.marginHeight= 0;
			
			composite.setLayout(layout);
			
			fParticipantList.doFillIntoGrid(composite, 3);
			LayoutUtil.setHorizontalSpan(fParticipantList.getLabelControl(null), 2);
			LayoutUtil.setWidthHint(fParticipantList.getLabelControl(null), convertWidthInCharsToPixels(40));
			LayoutUtil.setHorizontalGrabbing(fParticipantList.getListControl(null));
			
			fParticipantList.getTableViewer().setSorter(new ViewerSorter() {});
			
			Dialog.applyDialogFont(composite);
			return composite;
	}
	
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub
	}
}
