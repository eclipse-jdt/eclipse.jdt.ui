/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dnd;

import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TransferData;

import org.eclipse.jface.util.Assert;

public class DelegatingDropAdapter implements DropTargetListener {

	private TransferDropTargetListener[] fListeners;
	
	private TransferDropTargetListener fChoosenListener;
	
	public DelegatingDropAdapter(TransferDropTargetListener[] listeners) {
		fListeners= listeners;
		Assert.isNotNull(listeners);
	}

	public void dragEnter(DropTargetEvent event) {
		fChoosenListener= null;
		event.currentDataType= selectPreferredListener(event.dataTypes);
		if (fChoosenListener != null)
			fChoosenListener.dragEnter(event);
	}
	
	public void dragLeave(DropTargetEvent event){
		if (fChoosenListener != null)
			fChoosenListener.dragLeave(event);
	}
	
	public void dragOperationChanged(DropTargetEvent event){
		if (fChoosenListener != null)
			fChoosenListener.dragOperationChanged(event);
	}
	
	public void dragOver(DropTargetEvent event){
		if (fChoosenListener != null)
			fChoosenListener.dragOver(event);
	}
	
	public void drop(DropTargetEvent event){
		if (fChoosenListener != null)
			fChoosenListener.drop(event);
		fChoosenListener= null;	
	}
	
	public void dropAccept(DropTargetEvent event){
		if (fChoosenListener != null)
			fChoosenListener.dropAccept(event);
	}
	
	private TransferData selectPreferredListener(TransferData[] dataTypes) {
		for (int i= 0; i < fListeners.length; i++) {
			TransferData data= getTransferData(dataTypes, fListeners[i]);
			if (data != null)
				return data;
		}
		return null;
	}
	
	private TransferData getTransferData(TransferData[] dataTypes, TransferDropTargetListener listener) {
		for (int i= 0; i < dataTypes.length; i++) {
			if (listener.getTransfer().isSupportedType(dataTypes[i])) {
				fChoosenListener= listener;
				return dataTypes[i];
			}
		}
		return null;
	}	
}