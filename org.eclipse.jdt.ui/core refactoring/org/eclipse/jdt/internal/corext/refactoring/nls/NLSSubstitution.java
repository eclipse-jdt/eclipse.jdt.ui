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
	
	public String fKey;
	public String fValue;
	public NLSElement fNLSElement;
	public int fState;
	public boolean putToPropertyFile= true;
    private int fOldState;
    private AccessorClassInfo fAccessorClassInfo;

    // TODO: makes things easier...
    private static String fPrefix;
    
    public NLSSubstitution(int state, String value, NLSElement element) {        
		fNLSElement= element;
		fValue = value;
		fState= state;
		fOldState = state;
		Assert.isTrue(state == EXTERNALIZED || state == IGNORED || state == INTERNALIZED);
	}
    
	public NLSSubstitution(int state, String key, String value, NLSElement element, AccessorClassInfo accessorClassInfo) {
	    this(state, value, element);
	    if (state != EXTERNALIZED) {
	        throw new IllegalArgumentException("Set to INTERNALIZE/IGNORED State with different Constructor");	        
	    }
	    fKey = key;	    
	    fAccessorClassInfo = accessorClassInfo;
	}
	
	//util
	public static int countItems(NLSSubstitution[] elems, int task){
		Assert.isTrue(task == NLSSubstitution.EXTERNALIZED 
				   || task == NLSSubstitution.IGNORED 
				   || task == NLSSubstitution.INTERNALIZED);
		int result= 0;
		for (int i= 0; i < elems.length; i++){
			if (elems[i].fState == task)
				result++;
		}	
		return result;   
	}
  
	public String getKeyWithPrefix(String prefix) {
	    return prefix + fKey;	
	}
	
    public String getKey() {
        return fKey;
    }
    
    public void setKey(String key) {
        this.fKey = key;
    }    
    
    public void setValue(String value) {
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
    
    public boolean hasChanged() {     
        return !(fState == fOldState);
    }

    public int getOldState() {
        return fOldState;
    }
    
    public AccessorClassInfo getAccessorClassInfo() {
        return fAccessorClassInfo;
    }
    
    public void setPrefix(String prefix) {
        fPrefix = prefix;
    }
    
    public boolean hasDuplicateKey(NLSSubstitution[] substitutions, String prefix) {
        if (fState == EXTERNALIZED) {
            String key;
            if (hasChanged()) {
                key = prefix + fKey;                
            } else {
                key = fKey;
            }
            int counter = 0;
            for (int i = 0; i < substitutions.length; i++) {
                NLSSubstitution substitution = substitutions[i];
                                
                if (substitution.getState() == EXTERNALIZED) {
                    if (substitution.hasChanged()) {
                        if (substitution.getKeyWithPrefix(prefix).equals(key)) {
                            counter++;
                        }
                    } else {
                        if (substitution.getKey().equals(key)) {
                            counter++;
                        }                    
                    }
                }
            }
            if (counter > 1) {
                return true;
            }
        }
        return false;
    }
    
    public void generateKey(NLSSubstitution[] substitutions, String keyPrefix) {
    	if (fState != EXTERNALIZED || ((fState == EXTERNALIZED) && hasChanged())) {    		
    		int counter = 0;
    		fKey = createKey(counter);
    		while(true) {
    			int i;
    			for (i = 0; i < substitutions.length; i++) {
    				NLSSubstitution substitution = substitutions[i];
    				if ((substitution == this) || (substitution.fState != EXTERNALIZED)) continue;
    				if (substitution.hasChanged()) {
    					if (substitution.getKey().equals(fKey)) {
        					fKey = createKey(counter++);
        					break;
        				}
    				} else {
    					if (substitution.getKey().equals(getKeyWithPrefix(keyPrefix))) {
    						fKey = createKey(counter++);
    						break;
    					}
    				}
    			}
    			if (i == substitutions.length) return;
    		}
    	}
    }
    
    private String createKey(int counter) {
    	return String.valueOf(counter);
    }    
}
