/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dnd;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

public class LocalSelectionTransfer extends ByteArrayTransfer {

	// First attempt to create a UUID for the type name to make sure that
	// different Eclipse applications use different "types" of
	// <code>LocalSelectionTransfer</code>
	private static final String TYPE_NAME= "local-selection-transfer-format" + (new Long(System.currentTimeMillis())).toString(); //$NON-NLS-1$;
	private static final int TYPEID= registerType(TYPE_NAME);
	
	private static final LocalSelectionTransfer INSTANCE= new LocalSelectionTransfer();
	
	private ISelection fSelection;
	private int fSelectionSetTime;
	
	private LocalSelectionTransfer() {
	}
	
	/**
	 * Returns the singleton.
	 */
	public static LocalSelectionTransfer getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Sets the transfer data for local use.
	 */	
	public void setSelection(ISelection s) {
		fSelection= s;
	}
	
	/**
	 * Returns the local transfer data.
	 */
	public ISelection getSelection() {
		return fSelection;
	}
	
	public void javaToNative(Object object, TransferData transferData) {
		// No encoding needed since this is a hardcoded string read and written in the same process.
		// See nativeToJava below
		byte[] check= TYPE_NAME.getBytes();
		super.javaToNative(check, transferData);
	}

	public Object nativeToJava(TransferData transferData) {
		Object result= super.nativeToJava(transferData);
		if (isInvalidNativeType(result)) {
			JavaPlugin.logErrorMessage(JavaUIMessages.getString("LocalSelectionTransfer.errorMessage")); //$NON-NLS-1$
		}
		return fSelection;
	}

	private boolean isInvalidNativeType(Object result) {
		// No encoding needed since this is a hardcoded string read and written in the same process.
		// See javaToNative above
		return !(result instanceof byte[]) || !TYPE_NAME.equals(new String((byte[])result));
	}
	
	/**
	 * The type id used to identify this transfer.
	 */
	protected int[] getTypeIds() {
		return new int[] {TYPEID};
	}
	
	protected String[] getTypeNames(){
		return new String[] {TYPE_NAME};
	}	
	
	public int getSelectionSetTime() {
		return fSelectionSetTime;
	}

	public void setSelectionSetTime(int time) {
		fSelectionSetTime= time;
	}

}