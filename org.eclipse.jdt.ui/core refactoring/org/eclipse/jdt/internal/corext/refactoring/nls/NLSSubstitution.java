/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

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
	
	public NLSElement fNLSElement;	
    private AccessorClassInfo fAccessorClassInfo;

    private static String fPrefix = "";
    
    public NLSSubstitution(int state, String value, NLSElement element) {        
		fNLSElement= element;
		fValue = value;
		fState = state;
		fInitialState = state;
		fInitialValue = value;
		Assert.isTrue(state == EXTERNALIZED || state == IGNORED || state == INTERNALIZED);
	}
    
	public NLSSubstitution(int state, String key, String value, NLSElement element, AccessorClassInfo accessorClassInfo) {
	    this(state, value, element);
	    if (state != EXTERNALIZED) {
	        throw new IllegalArgumentException("Set to INTERNALIZE/IGNORED State with different Constructor");	        
	    }
	    fKey = key;
	    fInitialKey = key;
	    fAccessorClassInfo = accessorClassInfo;
	}
	
	//util
	public static int countItems(NLSSubstitution[] elems, int task){
		Assert.isTrue(task == NLSSubstitution.EXTERNALIZED 
				   || task == NLSSubstitution.IGNORED 
				   || task == NLSSubstitution.INTERNALIZED);
		int result= 0;
		for (int i= 0; i < elems.length; i++){
			if (elems[i].fState == task) {
				result++;
			}
		}	
		return result;   
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
            throw new IllegalStateException("Must be in Externalized State !");
        }
        this.fKey = key;
    }    
    
    public void setValue(String value) {
        if (fState != EXTERNALIZED) {
            throw new IllegalStateException("Must be in Externalized State !");
        }
        this.fValue = value;
    }
    
    public String getValue() {
        return fValue;
    }
    
    public int getState() {
        return fState;
    }
    
    public void setState(int state) {
        this.fState = state;
    }
    
    public boolean hasStateChanged() {     
        return fState != fInitialState;
    }
    
    public boolean hasChanged() {
        return hasStateChanged() || 
        ((fValue != null) && (fInitialValue != null) && !fInitialValue.equals(fValue)) ||
        ((fKey != null) && (fInitialKey != null) && !fInitialKey.equals(fKey)) ||
        ((fInitialValue == null) && (fValue != null));
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
        fPrefix = prefix;
    }
    
    public boolean hasDuplicateKey(NLSSubstitution[] substitutions) {
        if (fState == EXTERNALIZED) {            
            int counter = 0;
            for (int i = 0; i < substitutions.length; i++) {
                NLSSubstitution substitution = substitutions[i];                                
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
    		int counter = 0;
    		fKey = createKey(counter);
    		while(true) {
    			int i;
    			for (i = 0; i < substitutions.length; i++) {
    				NLSSubstitution substitution = substitutions[i];
    				if ((substitution == this) || (substitution.fState != EXTERNALIZED)) continue;
    				if (substitution.getKey().equals(getKey())) {
    				    fKey = createKey(counter++);
    				    break;    				
    				}
    			}
    			if (i == substitutions.length) return;
    		}
    	}
    }

    public void revert() {
        fState = fInitialState;
        fKey = fInitialKey;
        fValue = fInitialValue;        
    }
    
    private String createKey(int counter) {
    	return String.valueOf(counter);
    }    
}
