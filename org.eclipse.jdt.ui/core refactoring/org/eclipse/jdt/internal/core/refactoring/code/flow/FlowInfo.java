/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code.flow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;

public abstract class FlowInfo {
	
	// Return statement handling.
	protected static final int UNDEFINED= 		0;
	protected static final int NO_RETURN= 		1;
	protected static final int PARTIAL_RETURN= 	2;
	protected static final int VOID_RETURN= 	3;
	protected static final int VALUE_RETURN=		4;
	protected static final int THROW=			5;

	// Local access handling.
	public static final int UNUSED=				0;
	public static final int READ= 				1 << 0;
	public static final int READ_POTENTIAL=		1 << 1;
	public static final int WRITE= 				1 << 2;
	public static final int WRITE_POTENTIAL=	1 << 3;
	public static final int UNKNOWN= 	1 << 4;
	
	// Table to merge access modes for condition statements. 
	private static final int[][] ACCESS_MODE_CONDITIONAL_TABLE= {
	/*	  					  UNUSED		   READ			    READ_POTENTIAL   WRTIE			  WRITE_POTENTIAL  UNKNOWN */
	/* UNUSED */			{ UNUSED,		   READ_POTENTIAL,  READ_POTENTIAL,  WRITE_POTENTIAL, WRITE_POTENTIAL, UNKNOWN},
	/* READ */				{ READ_POTENTIAL,  READ,			READ_POTENTIAL,  UNKNOWN,		  UNKNOWN,         UNKNOWN},
	/* READ_POTENTIAL */	{ READ_POTENTIAL,  READ_POTENTIAL,  READ_POTENTIAL,  UNKNOWN,		  UNKNOWN,         UNKNOWN},
	/* WRITE */				{ WRITE_POTENTIAL, UNKNOWN,			UNKNOWN,		 WRITE,			  WRITE_POTENTIAL, UNKNOWN},
	/* WRITE_POTENTIAL */   { WRITE_POTENTIAL, UNKNOWN,			UNKNOWN,		 WRITE_POTENTIAL, WRITE_POTENTIAL, UNKNOWN},
	/* UNKNOWN */ 			{ UNKNOWN, 		   UNKNOWN,			UNKNOWN,		 UNKNOWN, 		  UNKNOWN,		   UNKNOWN}
	};
		
	protected static final String UNLABELED = "@unlabeled"; //$NON-NLS-1$
	protected static final LocalVariableBinding[] EMPTY_ARRAY= new LocalVariableBinding[0];

	protected int fReturnKind;
	protected HashSet fBranches;
	protected int[] fAccessModes;
	
	protected FlowInfo() {
		this(UNDEFINED);
	}
	
	protected FlowInfo(int returnKind) {
		fReturnKind= returnKind;
	}
	
	//---- General Helpers ----------------------------------------------------------
	
	protected void assignExecutionFlow(FlowInfo right) {
		fReturnKind= right.fReturnKind;
		fBranches= right.fBranches;
	}
	
	protected void assignAccessMode(FlowInfo right) {
		fAccessModes= right.fAccessModes;
	}
	
	protected void assign(FlowInfo right) {
		assignExecutionFlow(right);
		assignAccessMode(right);
	}
	
	protected void mergeConditional(FlowInfo info, FlowContext context) {
		mergeExecutionFlowConditional(info, context);
		mergeBranches(info, context);
		mergeAccessModeConditional(info, context);
	}
	
	protected void mergeSequential(FlowInfo info, FlowContext context) {
		mergeExecutionFlowSequential(info, context);
		mergeBranches(info, context);
		mergeAccessModeSequential(info, context);
	}
	
	//---- Execution flow handling --------------------------------------------------
	
	protected HashSet getBranches() {
		return fBranches;
	}
	
	protected void removeLabel(char[] label) {
		if (fBranches != null) {
			fBranches.remove(makeString(label));
			if (fBranches.isEmpty())
				fBranches= null;
		}
	}
	
	public void setNoReturn() {
		fReturnKind= NO_RETURN;
	}
	
	public boolean isUndefined() {
		return fReturnKind == UNDEFINED;
	}
	
	public boolean isNoReturn() {
		return fReturnKind == NO_RETURN;
	}
	
	public boolean isPartialReturn() {
		return fReturnKind == PARTIAL_RETURN;
	}
	
	public boolean isVoidReturn() {
		return fReturnKind == VOID_RETURN;
	}
	
	public boolean isValueReturn() {
		return fReturnKind == VALUE_RETURN;
	}
	
	public boolean isReturn() {
		return fReturnKind == VOID_RETURN || fReturnKind == VALUE_RETURN;
	}
	
	public boolean isThrow() {
		return fReturnKind == THROW;
	}
	
	public boolean branches() {
		HashSet branches= getBranches();
		return branches != null && !branches.isEmpty();
	}

	protected static String makeString(char[] label) {
		if (label == null)
			return UNLABELED;
		else
			return new String(label);
	}
	
	private void mergeExecutionFlowSequential(FlowInfo otherInfo, FlowContext context) {
		if (!context.considerExecutionFlow())
			return;
		mergeBranches(otherInfo, context);
		fReturnKind= otherInfo.fReturnKind;
	}
	
	private void mergeExecutionFlowConditional(FlowInfo otherInfo, FlowContext context) {
		if (!context.considerExecutionFlow())
			return;
			
		mergeBranches(otherInfo, context);
			
		int other= otherInfo.fReturnKind;
		if (fReturnKind == UNDEFINED || fReturnKind == THROW) {
			fReturnKind= other;
			return;
		}
		if (fReturnKind == other || other == THROW)
			return;
		if (other == UNDEFINED)
			fReturnKind= UNDEFINED;	
			
		// The case fReturnKind == VOID_RETURN && other == VALUE_RETURN or vice versa
		// can not happen since it would produce a compile error
		
		fReturnKind= PARTIAL_RETURN;
	}
	
	private void mergeBranches(FlowInfo otherInfo, FlowContext context) {
		HashSet otherBranches= otherInfo.fBranches;
		if (otherBranches != null) {
			if (fBranches == null) {
				fBranches= otherBranches;
			} else {
				Iterator iter= otherBranches.iterator();
				while (iter.hasNext()) {
					Object elem= iter.next();
					fBranches.add(elem);
				}
			}
		}
	}
	
	//---- Local access handling --------------------------------------------------
	
	/**
	 * Returns the arguments that have to be passed if the selected code is transformed
	 * into a new method.
	 *
	 * @param context the flow context object used to compute this flow info
	 * @return the arguments to be passed to the newly extracted method
	 */
	public LocalVariableBinding[] getArguments(FlowContext context) {
		return get(context, READ | READ_POTENTIAL | UNKNOWN);
	}
	
	/**
	 * Returns the return values that have to be returned if the selected code is transformed
	 * into a new method.
	 *
	 * @param context the flow context object used to compute this flow info
	 * @return the return value to be returned from the extracted method
	 */
	public LocalVariableBinding[] getReturnValues(FlowContext context) {
		return get(context, WRITE | WRITE_POTENTIAL | UNKNOWN);
	}
	
	/**
	 * Returns an array of <code>LocalVariableBinding</code> that conform to the given
	 * access type <code>type</code>.
	 * 
	 * @param context the flow context object used to compute this flow info
	 * @param the access type. Valid values are <code>READ</code>, <code>WRITE</code>,
	 *  <code>UNKNOWN</code> and any combination of them.
	 * @return an array of local variable bindings conforming to the given type.
	 */
	public LocalVariableBinding[] get(FlowContext context, int type) {
		List result= new ArrayList();
		int[] locals= getAccessModes();
		if (locals == null)
			return EMPTY_ARRAY;
		for (int i= 0; i < locals.length; i++) {
			int accessType= locals[i];
			if ((accessType & type) != 0)
				result.add(context.getLocalFromIndex(i));
		}
		return (LocalVariableBinding[])result.toArray(new LocalVariableBinding[result.size()]);
	}
	
	protected int[] getAccessModes() {
		return fAccessModes;
	}
	
	protected void mergeAccessModeSequential(FlowInfo otherInfo, FlowContext context) {
		if (!context.considerAccessMode())
			return;
			
		int[] others= otherInfo.fAccessModes;
		if (others != null) {
			if (fAccessModes == null) {
				fAccessModes= others;
			} else {
				if (context.computeArguments()) {
					for (int i= 0; i < fAccessModes.length; i++) {
						if (fAccessModes[i] == UNUSED)
							fAccessModes[i]= others[i];
					}
				} else if (context.computeReturnValues()) {
					for (int i= 0; i < fAccessModes.length; i++) {
						int accessmode= fAccessModes[i];
						int othermode= others[i];
						if (accessmode == WRITE)
							continue;
						if (accessmode == WRITE_POTENTIAL) {
							if (othermode == WRITE)
								fAccessModes[i]= WRITE;
							continue;
						}
							
						if (others[i] != UNUSED)
							fAccessModes[i]= othermode;
					}
				}
			}
		}
	}
	
	protected void mergeAccessModeConditional(FlowInfo otherInfo, FlowContext context) {
		if (!context.considerAccessMode())
			return;
			
		int[] others= otherInfo.fAccessModes;
		if (others != null) {
			if (fAccessModes == null) {
				fAccessModes= others;
			} else {
				for (int i= 0; i < fAccessModes.length; i++) {
					fAccessModes[i]= ACCESS_MODE_CONDITIONAL_TABLE
						[getIndex(fAccessModes[i])]
						[getIndex(others[i])];
				}
			}
		}
	}

	protected void mergeEmptyCondition(FlowContext context) {
		if (context.considerExecutionFlow() && fReturnKind == VALUE_RETURN)
			fReturnKind= PARTIAL_RETURN;
			
		if (!context.considerAccessMode())
			return;
			
		if (fAccessModes != null) {
			for (int i= 0; i < fAccessModes.length; i++) {
				fAccessModes[i]= ACCESS_MODE_CONDITIONAL_TABLE
					[getIndex(fAccessModes[i])]
					[getIndex(UNUSED)];
			}
		}
	}
	
	private static int getIndex(int accessMode) {
		 // Fast log function
		 switch (accessMode) {
		 	case UNUSED:
		 		return 0;
		 	case READ:
		 		return 1;
		 	case READ_POTENTIAL:
		 		return 2;
		 	case WRITE:
		 		return 3;
		 	case WRITE_POTENTIAL:
		 		return 4;
		 	case UNKNOWN:
		 		return 5;
		 }
		 return -1;
	}	
}

