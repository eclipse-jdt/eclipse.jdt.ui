package org.eclipse.jdt.internal.ui.dnd;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import org.eclipse.jface.viewers.ISelection;

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
		super.javaToNative(object, transferData);
	}

	public Object nativeToJava(TransferData transferData) {
		super.nativeToJava(transferData);
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