/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - created by splitting MembersOrderPreferenceCache in JDT UI
 *******************************************************************************/
package org.eclipse.jdt.internal.core.manipulation;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

/**
 * @since 1.10
 */
public class MembersOrderPreferenceCacheCommon {

	/**
	 * A named preference that defines how member elements are ordered by the
	 * Java views using the <code>JavaElementSorter</code>.
	 * <p>
	 * Value is of type <code>String</code>: A comma separated list of the
	 * following entries. Each entry must be in the list, no duplication. List
	 * order defines the sort order.
	 * <ul>
	 * <li><b>T</b>: Types</li>
	 * <li><b>C</b>: Constructors</li>
	 * <li><b>I</b>: Initializers</li>
	 * <li><b>M</b>: Methods</li>
	 * <li><b>F</b>: Fields</li>
	 * <li><b>SI</b>: Static Initializers</li>
	 * <li><b>SM</b>: Static Methods</li>
	 * <li><b>SF</b>: Static Fields</li>
	 * </ul>
	 * </p>
	 */
	public static final String APPEARANCE_MEMBER_SORT_ORDER= "outlinesortoption"; //$NON-NLS-1$

	/**
	 * A named preference that defines how member elements are ordered by visibility in the
	 * Java views using the <code>JavaElementSorter</code>.
	 * <p>
	 * Value is of type <code>String</code>: A comma separated list of the
	 * following entries. Each entry must be in the list, no duplication. List
	 * order defines the sort order.
	 * <ul>
	 * <li><b>B</b>: Public</li>
	 * <li><b>V</b>: Private</li>
	 * <li><b>R</b>: Protected</li>
	 * <li><b>D</b>: Default</li>
	 * </ul>
	 * </p>
	 */
	public static final String APPEARANCE_VISIBILITY_SORT_ORDER= "org.eclipse.jdt.ui.visibility.order"; //$NON-NLS-1$

	/**
	 * A named preferences that controls if Java elements are also sorted by
	 * visibility.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER= "org.eclipse.jdt.ui.enable.visibility.order"; //$NON-NLS-1$


	public static final int TYPE_INDEX= 0;
	public static final int CONSTRUCTORS_INDEX= 1;
	public static final int METHOD_INDEX= 2;
	public static final int FIELDS_INDEX= 3;
	public static final int INIT_INDEX= 4;
	public static final int STATIC_FIELDS_INDEX= 5;
	public static final int STATIC_INIT_INDEX= 6;
	public static final int STATIC_METHODS_INDEX= 7;
	public static final int ENUM_CONSTANTS_INDEX= 8;
	public static final int N_CATEGORIES= ENUM_CONSTANTS_INDEX + 1;

	protected static final int PUBLIC_INDEX= 0;
	protected static final int PRIVATE_INDEX= 1;
	protected static final int PROTECTED_INDEX= 2;
	protected static final int DEFAULT_INDEX= 3;
	protected static final int N_VISIBILITIES= DEFAULT_INDEX + 1;

	protected int[] fCategoryOffsets= null;

	protected boolean fSortByVisibility;
	protected int[] fVisibilityOffsets= null;

	private IEclipsePreferences fPreferences;
	private IEclipsePreferences fDefaultPreferenceStore;

	public MembersOrderPreferenceCacheCommon() {
		fPreferences= null;
		fDefaultPreferenceStore= null;
		fCategoryOffsets= null;
	}

	public void install() {
		fPreferences= InstanceScope.INSTANCE.getNode(JavaManipulation.getPreferenceNodeId());
		fDefaultPreferenceStore= DefaultScope.INSTANCE.getNode(JavaManipulation.getPreferenceNodeId());
		fVisibilityOffsets= null;
		boolean defaultValue= fDefaultPreferenceStore.getBoolean(APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, false);
		fSortByVisibility= fPreferences.getBoolean(APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER, defaultValue);
		JavaManipulationPlugin.getDefault().setMembersOrderPreferenceCacheCommon(this);
	}

	public void dispose() {
		fPreferences= null;
		fDefaultPreferenceStore= null;
	}

	public static boolean isMemberOrderProperty(String property) {
		return APPEARANCE_MEMBER_SORT_ORDER.equals(property)
				|| APPEARANCE_VISIBILITY_SORT_ORDER.equals(property)
				|| APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER.equals(property);
	}

	public void propertyChange(String property) {
		if (null != property) switch (property) {
		case APPEARANCE_MEMBER_SORT_ORDER:
			fCategoryOffsets= null;
			break;
		case APPEARANCE_VISIBILITY_SORT_ORDER:
			fVisibilityOffsets= null;
			break;
		case APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER:
			String key= APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER;
			boolean defaultValue= fDefaultPreferenceStore.getBoolean(key, false);
			fSortByVisibility= fPreferences.getBoolean(key, defaultValue);
			break;
		default:
			break;
		}
	}

	public int getCategoryIndex(int kind) {
		if (fCategoryOffsets == null) {
			fCategoryOffsets= getCategoryOffsets();
		}
		return fCategoryOffsets[kind];
	}

	protected int[] getCategoryOffsets() {
		int[] offsets= new int[N_CATEGORIES];
		String key= APPEARANCE_MEMBER_SORT_ORDER;
		boolean success= fillCategoryOffsetsFromPreferenceString(fPreferences.get(key, ""), offsets); //$NON-NLS-1$
		if (!success) {
			String defaultValue= fDefaultPreferenceStore.get(key, ""); //$NON-NLS-1$
			fPreferences.put(key, defaultValue);
			fillCategoryOffsetsFromPreferenceString(defaultValue, offsets);
		}
		return offsets;
	}

	protected boolean fillCategoryOffsetsFromPreferenceString(String str, int[] offsets) {
		StringTokenizer tokenizer= new StringTokenizer(str, ","); //$NON-NLS-1$
		int i= 0;
		offsets[ENUM_CONSTANTS_INDEX]= i++; // enum constants always on top

		while (tokenizer.hasMoreTokens()) {
			String token= tokenizer.nextToken().trim();
			if (null != token) switch (token) {
			case "T": //$NON-NLS-1$
				offsets[TYPE_INDEX]= i++;
				break;
			case "M": //$NON-NLS-1$
				offsets[METHOD_INDEX]= i++;
				break;
			case "F": //$NON-NLS-1$
				offsets[FIELDS_INDEX]= i++;
				break;
			case "I": //$NON-NLS-1$
				offsets[INIT_INDEX]= i++;
				break;
			case "SF": //$NON-NLS-1$
				offsets[STATIC_FIELDS_INDEX]= i++;
				break;
			case "SI": //$NON-NLS-1$
				offsets[STATIC_INIT_INDEX]= i++;
				break;
			case "SM": //$NON-NLS-1$
				offsets[STATIC_METHODS_INDEX]= i++;
				break;
			case "C": //$NON-NLS-1$
				offsets[CONSTRUCTORS_INDEX]= i++;
				break;
			default:
				break;
			}
		}
		return i == N_CATEGORIES;
	}

	public boolean isSortByVisibility() {
		return fSortByVisibility;
	}

	public int getVisibilityIndex(int modifierFlags) {
		if (fVisibilityOffsets == null) {
			fVisibilityOffsets= getVisibilityOffsets();
		}
		int kind= DEFAULT_INDEX;
		if (Flags.isPublic(modifierFlags)) {
			kind= PUBLIC_INDEX;
		} else if (Flags.isProtected(modifierFlags)) {
			kind= PROTECTED_INDEX;
		} else if (Flags.isPrivate(modifierFlags)) {
			kind= PRIVATE_INDEX;
		}

		return fVisibilityOffsets[kind];
	}

	protected int[] getVisibilityOffsets() {
		int[] offsets= new int[N_VISIBILITIES];
		String key= APPEARANCE_VISIBILITY_SORT_ORDER;
		boolean success= fillVisibilityOffsetsFromPreferenceString(fPreferences.get(key, ""), offsets); //$NON-NLS-1$
		if (!success) {
			String defaultValue= fDefaultPreferenceStore.get(key, ""); //$NON-NLS-1$
			fPreferences.put(key, defaultValue);
			fillVisibilityOffsetsFromPreferenceString(defaultValue, offsets);
		}
		return offsets;
	}

	protected boolean fillVisibilityOffsetsFromPreferenceString(String str, int[] offsets) {
		StringTokenizer tokenizer= new StringTokenizer(str, ","); //$NON-NLS-1$
		int i= 0;
		while (tokenizer.hasMoreTokens()) {
			String token= tokenizer.nextToken().trim();
			if (null != token) switch (token) {
			case "B": //$NON-NLS-1$
				offsets[PUBLIC_INDEX]= i++;
				break;
			case "V": //$NON-NLS-1$
				offsets[PRIVATE_INDEX]= i++;
				break;
			case "R": //$NON-NLS-1$
				offsets[PROTECTED_INDEX]= i++;
				break;
			case "D": //$NON-NLS-1$
				offsets[DEFAULT_INDEX]= i++;
				break;
			default:
				break;
			}
		}
		return i == N_VISIBILITIES;
	}

}
