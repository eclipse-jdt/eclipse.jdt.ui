/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class MultiStateCellEditor extends CellEditor {
	
	private int fValue;
	private int fStateCount;
	
	/**
	 * initial state will be 0
	 * @param stateCount must be > 1
	 */
	public MultiStateCellEditor(Composite parent, int stateCount) {
		this(parent, stateCount, 0);
	}
	
	/**
	 * @param stateCount must be > 1
	 * @param initialValue initialValue
	 */
	public MultiStateCellEditor(Composite parent, int stateCount, int initialValue) {
		super(parent);
		Assert.isTrue(stateCount > 1, "incorrect state count"); //$NON-NLS-1$
		fStateCount= stateCount;
		
		Assert.isTrue(initialValue >= 0 && initialValue < stateCount, "incorrect initial value"); //$NON-NLS-1$
		fValue= initialValue;
		
		setValueValid(true);
	}
	

	public void activate() {
		fValue= getNextValue(fStateCount, fValue);
		fireApplyEditorValue();
	}
	
	public static int getNextValue(int stateCount, int currentValue){
		Assert.isTrue(stateCount > 1, "incorrect state count"); //$NON-NLS-1$
		Assert.isTrue(currentValue >= 0 && currentValue < stateCount, "incorrect initial value"); //$NON-NLS-1$
		return (currentValue + 1) % stateCount;
	}

	protected Control createControl(Composite parent) {
		return null;
	}

	/**
	 * @return the Integer value
	 */
	protected Object doGetValue() {
		return new Integer(fValue);
	}

	/* (non-Javadoc)
	 * Method declared on CellEditor.
	 */
	protected void doSetFocus() {
		// Ignore
	}

	/**
	 * @param value an Integer value
	 * must be >=0 and < stateCount (value passed in the constructor)
	 */
	protected void doSetValue(Object value) {
		Assert.isTrue(value instanceof Integer, "value must be Integer"); //$NON-NLS-1$
		fValue = ((Integer) value).intValue();
		Assert.isTrue(fValue >= 0 && fValue < fStateCount, "invalid value"); //$NON-NLS-1$
	}
}