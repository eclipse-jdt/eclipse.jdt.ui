/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dnd;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jface.viewers.ISelection;

public class LocalSelectionTransfer extends ByteArrayTransfer {

	private static String TYPE_NAME;
	// First attempt to create a UUID for the type name to make sure that
	// different Eclipse applications use different "types" of
	// <code>LocalSelectionTransfer</code>
	static {
		TYPE_NAME= "local-selection-transfer-format";
		long time= System.currentTimeMillis();
		TYPE_NAME= TYPE_NAME + (new Long(time)).toString();
	}
	private static final int TYPEID= registerType(TYPE_NAME);
	
	private static final LocalSelectionTransfer INSTANCE= new LocalSelectionTransfer();
	
	private ISelection fSelection;
	
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
		byte[] check= TYPE_NAME.getBytes();
		super.javaToNative(check, transferData);
	}

	public Object nativeToJava(TransferData transferData) {
		Object result= super.nativeToJava(transferData);
		if (!(result instanceof byte[]) || !TYPE_NAME.equals(new String((byte[])result))) {
			JavaPlugin.logErrorMessage("Got wrong data in org.eclipse.jdt.internal.ui.dnd.LocalSelectionTranser::nativeToJava");
		}
		return fSelection;
	}
	
	/**
	 * The used type id to identify this transfer.
	 */
	protected int[] getTypeIds() {
		return new int[] {TYPEID};
	}
	
	protected String[] getTypeNames(){
		return new String[] {TYPE_NAME};
	}	
}