package org.eclipse.jdt.internal.ui.preferences;

import java.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
  */
public class MembersOrderPreferenceCache implements IPropertyChangeListener {
	
	public static final int TYPE_INDEX= 0;
	public static final int CONSTRUCTORS_INDEX= 1;
	public static final int METHOD_INDEX= 2;
	public static final int FIELDS_INDEX= 3;
	public static final int INIT_INDEX= 4;
	public static final int STATIC_FIELDS_INDEX= 5;
	public static final int STATIC_INIT_INDEX= 6;
	public static final int STATIC_METHODS_INDEX= 7;
	public static final int N_ENTRIES= STATIC_METHODS_INDEX + 1;	
	
	private int[] fOffsets= null;

	public void propertyChange(PropertyChangeEvent event) {
		if (PreferenceConstants.APPEARANCE_MEMBER_SORT_ORDER.equals(event.getProperty())) {
			fOffsets= null;
		}
	}

	public int getIndex(int kind) {
		if (fOffsets == null) {
			fOffsets= getOffsets();
		}
		return fOffsets[kind];
	}
	
	private int[] getOffsets() {
		int[] offsets= new int[N_ENTRIES];
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		String key= PreferenceConstants.APPEARANCE_MEMBER_SORT_ORDER;
		boolean success= fillOffsetsFromPreferenceString(store.getString(key), offsets);
		if (!success) {
			store.setToDefault(key);
			fillOffsetsFromPreferenceString(store.getDefaultString(key), offsets);	
		}
		return offsets;
	}		
	
	private boolean fillOffsetsFromPreferenceString(String str, int[] offsets) {
		StringTokenizer tokenizer= new StringTokenizer(str, ","); //$NON-NLS-1$
		int i= 0;
		while (tokenizer.hasMoreTokens()) {
			String token= tokenizer.nextToken().trim();
			if ("T".equals(token)) { //$NON-NLS-1$
				offsets[TYPE_INDEX]= i++;
			} else if ("M".equals(token)) { //$NON-NLS-1$
				offsets[METHOD_INDEX]= i++;
			} else if ("F".equals(token)) { //$NON-NLS-1$
				offsets[FIELDS_INDEX]= i++;
			} else if ("I".equals(token)) { //$NON-NLS-1$
				offsets[INIT_INDEX]= i++;
			} else if ("SF".equals(token)) { //$NON-NLS-1$
				offsets[STATIC_FIELDS_INDEX]= i++;
			} else if ("SI".equals(token)) { //$NON-NLS-1$
				offsets[STATIC_INIT_INDEX]= i++;
			} else if ("SM".equals(token)) { //$NON-NLS-1$
				offsets[STATIC_METHODS_INDEX]= i++;
			} else if ("C".equals(token)) { //$NON-NLS-1$
				offsets[CONSTRUCTORS_INDEX]= i++;
			}
		}
		return i == N_ENTRIES;
	}
	

}
