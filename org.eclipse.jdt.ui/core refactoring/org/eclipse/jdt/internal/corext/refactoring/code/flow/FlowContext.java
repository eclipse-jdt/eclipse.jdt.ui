/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.corext.Assert;

public class FlowContext {

	private static class Enum {
	}
	
	public static final Enum ARGUMENTS= 	new Enum();
	public static final Enum RETURN_VALUES= new Enum();
	
	private int fStart;
	private int fLength;
	private boolean fConsiderAccessMode;
	private boolean fLoopReentranceMode;
	private Enum fComputeMode;
	private LocalVariableBinding[] fLocals;
	private List fExceptionStack;
	
	public FlowContext(int start, int length) {
		fStart= start;
		fLength= length;
		fExceptionStack= new ArrayList(3);
	}
	
	public void setConsiderAccessMode(boolean b) {
		fConsiderAccessMode= b;
	}
	
	public void setComputeMode(Enum mode) {
		fComputeMode= mode;
	}
	
	void setLoopReentranceMode(boolean b) {
		fLoopReentranceMode= b;
	}
	
	int getArrayLength() {
		return fLength;
	}
	
	int getStartingIndex() {
		return fStart;
	}
	
	boolean considerAccessMode() {
		return fConsiderAccessMode;
	}
	
	boolean isLoopReentranceMode() {
		return fLoopReentranceMode;
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
	
	public int getIndexFromLocal(LocalVariableBinding local) {
		if (fLocals == null)
			return -1;
		for (int i= 0; i < fLocals.length; i++) {
			if (fLocals[i] == local)
				return i;
		}
		return -1;
	}
	
	void manageLocal(LocalVariableBinding local) {
		if (fLocals == null)
			fLocals= new LocalVariableBinding[fLength];
		fLocals[local.id - fStart]= local;
	}
	
	//---- Exception handling --------------------------------------------------------
	
	void pushExcptions(Argument[] catchArguments) {
		fExceptionStack.add(catchArguments);
	}
	
	void popExceptions() {
		Assert.isTrue(fExceptionStack.size() > 0);
		fExceptionStack.remove(fExceptionStack.size() - 1);
	}
	
	boolean isExceptionCaught(TypeBinding excpetionType) {
		for (Iterator iter= fExceptionStack.iterator(); iter.hasNext(); ) {
			Argument[] catchArguments= (Argument[])iter.next();
			for (int i= 0; i < catchArguments.length; i++) {
				Argument arg= catchArguments[i];
				if (arg.binding == null)
					continue;
				ReferenceBinding catchedType= (ReferenceBinding)catchArguments[i].binding.type;
				while (catchedType != null) {
					if (catchedType == excpetionType)
						return true;
					catchedType= catchedType.superclass();
				}
			}
		}
		return false;
	}
}