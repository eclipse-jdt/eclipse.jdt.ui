/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code.flow;

import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;


public class FlowContext {

	private static class Enum {
	}
	
	public static final Enum ARGUMENTS= 	new Enum();
	public static final Enum RETURN_VALUES= new Enum();
	
	private int fStart;
	private int fLength;
	private boolean fConsiderExecutionFlow;
	private boolean fConsiderAccessMode;
	private Enum fComputeMode;
	private LocalVariableBinding[] fLocals;
	
	public FlowContext(int start, int length) {
		fStart= start;
		fLength= length;
	}
	
	public void setConsiderExecutionFlow(boolean b) {
		fConsiderExecutionFlow= b;
	}
	
	public void setConsiderAccessMode(boolean b) {
		fConsiderAccessMode= b;
	}
	
	public void setComputeMode(Enum mode) {
		fComputeMode= mode;
	}
	
	int getArrayLength() {
		return fLength;
	}
	
	int getStartingIndex() {
		return fStart;
	}
	
	boolean considerExecutionFlow() {
		return fConsiderExecutionFlow;
	}
	
	boolean considerAccessMode() {
		return fConsiderAccessMode;
	}
	
	boolean computeArguments() {
		return fComputeMode == ARGUMENTS;
	}
	
	boolean computeReturnValues() {
		return fComputeMode == RETURN_VALUES;
	}
	
	public LocalVariableBinding getLocalFromId(int id) {
		return getLocalFromIndex(id - fStart);
	}
	
	public LocalVariableBinding getLocalFromIndex(int index) {
		if (fLocals == null || index > fLocals.length)
			return null;
		return fLocals[index];
	}
	
	void manageLocal(LocalVariableBinding local) {
		if (fLocals == null)
			fLocals= new LocalVariableBinding[fLength];
		fLocals[local.id - fStart]= local;
	}	
}