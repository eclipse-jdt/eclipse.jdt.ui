/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code.flow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;

public abstract class FlowInfo {
	
	// Return statement handling.
	protected static final int NOT_POSSIBLE=	0;
	protected static final int UNDEFINED= 		1;
	protected static final int NO_RETURN= 		2;
	protected static final int PARTIAL_RETURN= 	3;
	protected static final int VOID_RETURN= 	4;
	protected static final int VALUE_RETURN=	5;
	protected static final int THROW=			6;

	// Local access handling.
	public static final int UNUSED=				0;
	public static final int READ= 				1 << 0;
	public static final int READ_POTENTIAL=		1 << 1;
	public static final int WRITE= 				1 << 2;
	public static final int WRITE_POTENTIAL=	1 << 3;
	public static final int UNKNOWN= 			1 << 4;
	
	// Table to merge access modes for condition statements. 
	private static final int[][] ACCESS_MODE_CONDITIONAL_TABLE= {
	/*	  					  UNUSED		   READ			    READ_POTENTIAL   WRTIE			  WRITE_POTENTIAL  UNKNOWN */
	/* UNUSED */			{ UNUSED,		   READ_POTENTIAL,  READ_POTENTIAL,  WRITE_POTENTIAL, WRITE_POTENTIAL, UNKNOWN },
	/* READ */				{ READ_POTENTIAL,  READ,			READ_POTENTIAL,  UNKNOWN,		  UNKNOWN,         UNKNOWN },
	/* READ_POTENTIAL */	{ READ_POTENTIAL,  READ_POTENTIAL,  READ_POTENTIAL,  UNKNOWN,		  UNKNOWN,         UNKNOWN },
	/* WRITE */				{ WRITE_POTENTIAL, UNKNOWN,			UNKNOWN,		 WRITE,			  WRITE_POTENTIAL, UNKNOWN },
	/* WRITE_POTENTIAL */   { WRITE_POTENTIAL, UNKNOWN,			UNKNOWN,		 WRITE_POTENTIAL, WRITE_POTENTIAL, UNKNOWN },
	/* UNKNOWN */ 			{ UNKNOWN, 		   UNKNOWN,			UNKNOWN,		 UNKNOWN, 		  UNKNOWN,		   UNKNOWN }
	};
	
	// Table to change access mode if there is an open branch statement
	private static final int[] ACCESS_MODE_OPEN_BRANCH_TABLE= {
	/*	UNUSED	READ			READ_POTENTIAL  WRTIE				WRITE_POTENTIAL  UNKNOWN */
		UNUSED,	READ_POTENTIAL,	READ_POTENTIAL,	WRITE_POTENTIAL,	WRITE_POTENTIAL, UNKNOWN
	};
	
	// Table to merge return modes for condition statements (y: fReturnKind, x: other.fReturnKind)
	private static final int[][] RETURN_KIND_CONDITIONAL_TABLE = {
	/* 						  NOT_POSSIBLE		UNDEFINED		NO_RETURN		PARTIAL_RETURN	VOID_RETURN		VALUE_RETURN	THROW */
	/* NOT_POSSIBLE */		{ NOT_POSSIBLE,		NOT_POSSIBLE,	NOT_POSSIBLE, 	NOT_POSSIBLE,	NOT_POSSIBLE,	NOT_POSSIBLE,	NOT_POSSIBLE	},
	/* UNDEFINED */			{ NOT_POSSIBLE,		UNDEFINED,		NO_RETURN,		PARTIAL_RETURN, VOID_RETURN,	VALUE_RETURN,	THROW 			},	
	/* NO_RETURN */			{ NOT_POSSIBLE,		NO_RETURN,		NO_RETURN,		PARTIAL_RETURN,	PARTIAL_RETURN, PARTIAL_RETURN, NO_RETURN 		},
	/* PARTIAL_RETURN */	{ NOT_POSSIBLE,		PARTIAL_RETURN,	PARTIAL_RETURN,	PARTIAL_RETURN, PARTIAL_RETURN, PARTIAL_RETURN, PARTIAL_RETURN	},
	/* VOID_RETURN */		{ NOT_POSSIBLE,		VOID_RETURN,	PARTIAL_RETURN,	PARTIAL_RETURN, VOID_RETURN,	NOT_POSSIBLE,	VOID_RETURN		},
	/* VALUE_RETURN */		{ NOT_POSSIBLE,		VALUE_RETURN,	PARTIAL_RETURN, PARTIAL_RETURN, NOT_POSSIBLE,	VALUE_RETURN,	VALUE_RETURN	},
	/* THROW */				{ NOT_POSSIBLE,		THROW,			NO_RETURN,		PARTIAL_RETURN, VOID_RETURN,	VALUE_RETURN,	THROW			}
	};
		
	// Table to merge return modes for sequential statements (y: fReturnKind, x: other.fReturnKind)
	private static final int[][] RETURN_KIND_SEQUENTIAL_TABLE = {
	/* 						  NOT_POSSIBLE		UNDEFINED		NO_RETURN		PARTIAL_RETURN	VOID_RETURN		VALUE_RETURN	THROW */
	/* NOT_POSSIBLE */		{ NOT_POSSIBLE,		NOT_POSSIBLE,	NOT_POSSIBLE, 	NOT_POSSIBLE,	NOT_POSSIBLE,	NOT_POSSIBLE,	NOT_POSSIBLE	},
	/* UNDEFINED */			{ NOT_POSSIBLE,		UNDEFINED,		NO_RETURN,		PARTIAL_RETURN,	VOID_RETURN,	VALUE_RETURN,	THROW			},
	/* NO_RETURN */			{ NOT_POSSIBLE,		NO_RETURN,		NO_RETURN,		PARTIAL_RETURN,	VOID_RETURN,	VALUE_RETURN,	THROW			},
	/* PARTIAL_RETURN */	{ NOT_POSSIBLE,		PARTIAL_RETURN,	PARTIAL_RETURN,	PARTIAL_RETURN,	VOID_RETURN,	VALUE_RETURN,	THROW			},
	/* VOID_RETURN */		{ NOT_POSSIBLE,		VOID_RETURN,	VOID_RETURN,	PARTIAL_RETURN,	VOID_RETURN,	NOT_POSSIBLE,	NOT_POSSIBLE	},
	/* VALUE_RETURN */		{ NOT_POSSIBLE,		VALUE_RETURN,	VALUE_RETURN,	PARTIAL_RETURN,	NOT_POSSIBLE,	VALUE_RETURN,	NOT_POSSIBLE	},
	/* THROW */				{ NOT_POSSIBLE,		THROW,			THROW,			PARTIAL_RETURN,	VOID_RETURN,	VALUE_RETURN,	THROW			}
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
		mergeAccessModeConditional(info, context);
		mergeExecutionFlowConditional(info, context);
	}
	
	protected void mergeSequential(FlowInfo info, FlowContext context) {
		mergeAccessModeSequential(info, context);
		mergeExecutionFlowSequential(info, context);
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
		fReturnKind= RETURN_KIND_SEQUENTIAL_TABLE[fReturnKind][otherInfo.fReturnKind];
		mergeBranches(otherInfo, context);
	}
	
	private void mergeExecutionFlowConditional(FlowInfo otherInfo, FlowContext context) {
		if (!context.considerExecutionFlow())
			return;
			
		fReturnKind= RETURN_KIND_CONDITIONAL_TABLE[fReturnKind][otherInfo.fReturnKind];
		mergeBranches(otherInfo, context);			
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
		if (branches()) {
			if (context.considerAccessMode() && fAccessModes != null) {
				for (int i= 0; i < fAccessModes.length; i++)
					fAccessModes[i]= ACCESS_MODE_OPEN_BRANCH_TABLE[getIndex(fAccessModes[i])];
			}
			if (context.considerExecutionFlow() && fReturnKind == VALUE_RETURN)
				fReturnKind= PARTIAL_RETURN;
		}
	}
	
	//---- Local access handling --------------------------------------------------
	
	/**
	 * Returns an array of <code>LocalVariableBinding</code> that conform to the given
	 * access mode <code>mode</code>.
	 * 
	 * @param context the flow context object used to compute this flow info
	 * @param the access type. Valid values are <code>READ</code>, <code>WRITE</code>,
	 *  <code>UNKNOWN</code> and any combination of them.
	 * @return an array of local variable bindings conforming to the given type.
	 */
	public LocalVariableBinding[] get(FlowContext context, int mode) {
		List result= new ArrayList();
		int[] locals= getAccessModes();
		if (locals == null)
			return EMPTY_ARRAY;
		for (int i= 0; i < locals.length; i++) {
			int accessMode= locals[i];
			if ((accessMode & mode) != 0)
				result.add(context.getLocalFromIndex(i));
		}
		return (LocalVariableBinding[])result.toArray(new LocalVariableBinding[result.size()]);
	}
	
	/**
	 * Checks whether the given local variable binding has the given access mode
	 * 
	 * @return <code>true</code> if the binding has the given access mode. 
	 * 	<code>False</code> otherwise
	 */
	public boolean hasAccessMode(FlowContext context, LocalVariableBinding local, int mode) {
		int index= context.getIndexFromLocal(local);
		if (index == -1)
			return false;
		return (fAccessModes[index] & mode) != 0;
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
						int accessMode= fAccessModes[i];
						int otherMode= others[i];
						if (accessMode == UNUSED) {
							fAccessModes[i]= otherMode;
						} else if (accessMode == WRITE_POTENTIAL && otherMode == READ) {
							fAccessModes[i]= READ;
						} else if (accessMode == WRITE_POTENTIAL && otherMode == WRITE) {
							fAccessModes[i]= WRITE;
						}
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
		if (context.considerExecutionFlow() && (fReturnKind == VALUE_RETURN || fReturnKind == VOID_RETURN))
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


