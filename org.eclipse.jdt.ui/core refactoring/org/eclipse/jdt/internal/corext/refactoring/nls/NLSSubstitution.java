/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.Properties;

import org.eclipse.jdt.internal.corext.Assert;

public class NLSSubstitution {
	public static final int EXTERNALIZED= 0;
	public static final int IGNORED= 1;
	public static final int INTERNALIZED= 2;

	public static final int DEFAULT= EXTERNALIZED;
	public static final int STATE_COUNT= 3;

	private int fState;
	private String fKey;
	private String fValue;

	private int fInitialState;
	private String fInitialKey;
	private String fInitialValue;
	
	private NLSElement fNLSElement;
	private AccessorClassInfo fAccessorClassInfo;
	
	private String fNewAccessorClassName;

	private static String fPrefix= ""; //$NON-NLS-1$


	public NLSSubstitution(int state, String value, NLSElement element) {
		fNLSElement= element;
		fValue= value;
		fState= state;
		fInitialState= state;
		fInitialValue= value;
		Assert.isTrue(state == EXTERNALIZED || state == IGNORED || state == INTERNALIZED);
	}

	/**
	 * value == null indicates a corrupt substitution. 
	 */
	public NLSSubstitution(int state, String key, String value, NLSElement element, AccessorClassInfo accessorClassInfo) {
		this(state,value,element);
		if (state != EXTERNALIZED) {
			throw new IllegalArgumentException("Set to INTERNALIZE/IGNORED State with different Constructor"); //$NON-NLS-1$
		}
		fKey= key;
		fInitialKey= key;
		fAccessorClassInfo= accessorClassInfo;
		fNewAccessorClassName= null;
	}

	//util
	public static int countItems(NLSSubstitution[] elems, int task) {
		Assert.isTrue(task == NLSSubstitution.EXTERNALIZED || task == NLSSubstitution.IGNORED || task == NLSSubstitution.INTERNALIZED);
		int result= 0;
		for (int i= 0; i < elems.length; i++) {
			if (elems[i].fState == task) {
				result++;
			}
		}
		return result;
	}

	public NLSElement getNLSElement() {
		return fNLSElement;
	}
	
	public String getKeyWithoutPrefix() {
		return fKey;
	}

	/**
	 * Returns key dependent on state. 
	 * @return prefix + key when 
	 */
	public String getKey() {
		if ((fState == EXTERNALIZED) && hasStateChanged()) {
			return fPrefix + fKey;
		}
		return fKey;
	}

	public void setKey(String key) {
		if (fState != EXTERNALIZED) {
			throw new IllegalStateException("Must be in Externalized State !"); //$NON-NLS-1$
		}
		fKey= key;
	}

	public void setValue(String value) {
		fValue= value;
	}
	
	public void setInitialValue(String value) {
		fInitialValue= value;
	}

	public String getValue() {
		return fValue;
	}

	public int getState() {
		return fState;
	}

	public void setState(int state) {
		fState= state;
	}
	
	public void setUpdatedAccessor(String accessorClassName) {
		fNewAccessorClassName= accessorClassName;
	}
	
	public String getUpdatedAccessor() {
		return fNewAccessorClassName;
	}

	public boolean hasStateChanged() {
		return fState != fInitialState;
	}
	
	public boolean isKeyRename() {
		return 	(fInitialKey != null && !fInitialKey.equals(fKey));
	}
	
	public boolean isValueRename() {
		return 	(fInitialValue != null && !fInitialValue.equals(fValue));
	}
	
	public boolean isAccessorRename() {
		return (fAccessorClassInfo != null) && (fNewAccessorClassName != null) && !fNewAccessorClassName.equals(fAccessorClassInfo.getName());
	}
	
	
	public boolean hasPropertyFileChange() {
		if (hasStateChanged()) {
			return true;
		}
		if (fState == EXTERNALIZED) {
			if (fInitialValue == null) {
				return true; // recreate entry in property file
			} else if (!fInitialValue.equals(fValue)) {
				return true; // change of value
			}
			if (!fInitialKey.equals(fKey)) {
				return true; // change of key
			}
		}
		return false;
	}
	
	public boolean hasSourceChange() {
		if (hasStateChanged()) {
			return true;
		}
		if (fState == EXTERNALIZED) {
			if (!fInitialKey.equals(fKey)) {
				return true; // change of key
			}
			if (isAccessorRename()) {
				return true;
			}
		} else {
			if (!fInitialValue.equals(fValue)) {
				return true; // change of value
			}
		}
		return false;
	}
	
	public int getInitialState() {
		return fInitialState;
	}

	public String getInitialKey() {
		return fInitialKey;
	}

	public String getInitialValue() {
		return fInitialValue;
	}

	public AccessorClassInfo getAccessorClassInfo() {
		return fAccessorClassInfo;
	}

	/**
	 * Prefix is valid for ALL Substitutions, that have changed into EXTERNALIZED state.
	 */
	public static void setPrefix(String prefix) {
		fPrefix= prefix;
	}

	public boolean hasDuplicateKey(NLSSubstitution[] substitutions) {
		if (fState == EXTERNALIZED) {
			int counter= 0;
			for (int i= 0; i < substitutions.length; i++) {
				NLSSubstitution substitution= substitutions[i];
				if (substitution.getState() == EXTERNALIZED) {
					if (substitution.getKey().equals(getKey())) {
						counter++;
					}
				}
			}
			if (counter > 1) {
				return true;
			}
		}
		return false;
	}

	public void generateKey(NLSSubstitution[] substitutions) {
		if (fState != EXTERNALIZED || ((fState == EXTERNALIZED) && hasStateChanged())) {
			int counter= 0;
			fKey= createKey(counter);
			while (true) {
				int i;
				for (i= 0; i < substitutions.length; i++) {
					NLSSubstitution substitution= substitutions[i];
					if ((substitution == this) || (substitution.fState != EXTERNALIZED))
						continue;
					if (substitution.getKey().equals(getKey())) {
						fKey= createKey(counter++);
						break;
					}
				}
				if (i == substitutions.length)
					return;
			}
		}
	}

	public static void updateSubtitutions(NLSSubstitution[] substitutions, Properties props, String accessorClassName) {
		for (int i= 0; i < substitutions.length; i++) {
			NLSSubstitution substitution= substitutions[i];
			if ((substitution.getState() == NLSSubstitution.EXTERNALIZED) && !substitution.hasStateChanged()) {
				substitution.setInitialValue(props.getProperty(substitution.getKey()));
				substitution.setUpdatedAccessor(accessorClassName);
			}
		}
	}



	public void revert() {
		fState= fInitialState;
		fKey= fInitialKey;
		fValue= fInitialValue;
	}

	private String createKey(int counter) {
		return String.valueOf(counter);
	}





}