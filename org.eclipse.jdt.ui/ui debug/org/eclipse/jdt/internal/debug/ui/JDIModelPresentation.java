/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaMethodEntryBreakpoint;
import org.eclipse.jdt.debug.core.IJavaModifiers;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaRunToLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;

import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.ui.IPreferencesConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageDescriptor;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

/**
 * @see IDebugModelPresentation
 */
public class JDIModelPresentation extends LabelProvider implements IDebugModelPresentation, IPropertyChangeListener, IDebugEventListener {

	protected HashMap fAttributes= new HashMap(3);

	static final Point BIG_SIZE= new Point(22, 16);
	protected ImageDescriptorRegistry fRegistry= JavaPlugin.getImageDescriptorRegistry();
	
	// Thread label resource String keys
	private static final String THREAD_PREFIX= "jdi_thread.";
	private static final String LABEL= THREAD_PREFIX + "label.";
	private static final String RUNNING_SYS= LABEL + "running_sys";
	private static final String RUNNING_USR= LABEL + "running_usr";
	private static final String TERMINATED_SYS= LABEL + "terminated_sys";
	private static final String TERMINATED_USR= LABEL + "terminated_usr";
	private static final String STEPPING_SYS= LABEL + "stepping_sys";
	private static final String STEPPING_USR= LABEL + "stepping_usr";
	private static final String EXCEPTION_SYS= LABEL + "exception_sys";
	private static final String EXCEPTION_USR= LABEL + "exception_usr";
	private static final String METHOD_ENTRY_SYS= LABEL + "methodentry_sys";
	private static final String METHOD_ENTRY_USR= LABEL + "methodentry_usr";
	private static final String BREAKPOINT_SYS= LABEL + "breakpoint_sys";
	private static final String BREAKPOINT_USR= LABEL + "breakpoint_usr";
	private static final String ACCESS_SYS= LABEL + "access_sys";
	private static final String ACCESS_USR= LABEL + "access_usr";
	private static final String MODIFICATION_SYS= LABEL + "modification_sys";
	private static final String MODIFICATION_USR= LABEL + "modification_usr";	
	private static final String RUN_TO_LINE_SYS= LABEL + "run_to_line_sys";
	private static final String RUN_TO_LINE_USR= LABEL + "run_to_line_usr";
	private static final String SUSPENDED_SYS= LABEL + "suspended_sys";
	private static final String SUSPENDED_USR= LABEL + "suspended_usr";

	private static final String PREFIX= "jdi_model_presentation.";
	private static final String TERMINATED= "terminated";
	private static final String NOT_RESPONDING= PREFIX + "not_responding";
	
	private static final String NO_RETURN_VALUE= PREFIX + "no_return_value";
	
	private static final String LINE= "line";
	private static final String HITCOUNT= "hitCount";
	private static final String BREAKPOINT_FORMAT= PREFIX + "format";	

	protected final static String EXCEPTION= PREFIX + "exception.";
	protected final static String CAUGHT= EXCEPTION + "caught";
	protected final static String UNCAUGHT= EXCEPTION + "uncaught";
	protected final static String CAUGHTANDUNCAUGHT= EXCEPTION + "caughtanduncaught";
	
	protected final static String WATCHPOINT= PREFIX + "watchpoint.";
	protected final static String ACCESS= WATCHPOINT + "access";
	protected final static String MODIFICATION= WATCHPOINT + "modification";
	protected final static String ACCESSANDMODIFICATION= WATCHPOINT + "accessandmodification";

	protected static final String fgStringName= "java.lang.String";
	
	/**
	 * The siganture of <code>java.lang.Object.toString()</code>,
	 * used to evaluate 'toString()' for displaying details of values.
	 */
	private static final String fgToStringSignature = "()Ljava/lang/String;"; //$NON-NLS-1$
	/**
	 * The selector of <code>java.lang.Object.toString()</code>,
	 * used to evaluate 'toString()' for displaying details of values.
	 */
	private static final String fgToString = "toString"; //$NON-NLS-1$

	protected JavaElementLabelProvider fJavaLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
	
	/**
	 * A pool of threads per VM that are suspended. The
	 * table is updated as threads suspend/resume. Used
	 * to perform 'toString' evaluations. Keys are debug
	 * targets, and values are <code>List</code>s of
	 * threads.
	 */
	private Hashtable fThreadPool; 

	public JDIModelPresentation() {
		super();
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(this);
	}
	
	public void dispose() {
		super.dispose();
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.removePropertyChangeListener(this);
		disposeThreadPool();
	}
	
	/**
	 * If the thread pool was used, it is disposed, and this
	 * presentation is removed as a debug event listener.
	 */
	protected void disposeThreadPool() {
		if (fThreadPool != null) {
			DebugPlugin.getDefault().removeDebugEventListener(this);
			fThreadPool.clear();
		}
	}

	/**
	 * Initializes the thread pool with all suspended Java
	 * threads. Registers this presentation as a debug event
	 * handler.
	 */
	protected void initializeThreadPool() {
		fThreadPool = new Hashtable();
		IDebugTarget[] targets= DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
		for (int i = 0; i < targets.length; i++) {
			if (targets[i] instanceof IJavaDebugTarget) {
				List suspended = new ArrayList();
				fThreadPool.put(targets[i], suspended);
				try {
					IThread[] threads = targets[i].getThreads();
					for (int j = 0; j < threads.length; j++) {
						if (threads[j].isSuspended()) {
							suspended.add(threads[j]);
						}
					}
				} catch (DebugException e) {
					DebugUIUtils.logError(e);
				}
			}
		}
		DebugPlugin.getDefault().addDebugEventListener(this);
	}
	
	/**
	 * Returns the "toString" of the given value
	 * 
	 * @see IDebugModelPresentation#getDetail(IValue)
	 */
	public String getDetail(IValue v) {
		if (v instanceof IJavaValue) {
			// get a thread for an evaluation
			IJavaValue value = (IJavaValue)v;
			IJavaThread thread = getEvaluationThread((IJavaDebugTarget)value.getDebugTarget());
			if (thread != null) {
				try {
					return evaluateToString(value, thread);
				} catch (DebugException e) {
					// return the exception's message
					return e.getStatus().getMessage();
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns a thread from the specified VM that can be
	 * used for an evaluationm or <code>null</code> if
	 * none.
	 * <p>
	 * This presentation maintains a pool of suspended
	 * threads per VM. Any suspended thread in the same
	 * VM may be used. The pool is lazily initialized on
	 * the first call to this method.
	 * </p>
	 * 
	 * @param debug target the target in which a thread is 
	 * 	required
	 * @return thread or <code>null</code>
	 */
	protected IJavaThread getEvaluationThread(IJavaDebugTarget target) {
		if (fThreadPool == null) {
			initializeThreadPool();
		}
		List threads = (List)fThreadPool.get(target);
		if (threads != null && !threads.isEmpty()) {
			return (IJavaThread)threads.get(0);
		}
		return null;
	}
	
	/**
	 * Returns the result of sending 'toString' to the given
	 * value (unless the value is null or is a primitive). If the
	 * evaluation takes > 3 seconds, this method returns.
	 * 
	 * @param value the value to send the message 'toString'
	 * @param thread the thread in which to perform the message
	 *  send
	 * @return the result of sending 'toString', as a String
	 * @exception DebugException if thrown by a model element
	 */
	protected synchronized String evaluateToString(final IJavaValue value, final IJavaThread thread) throws DebugException {
		final IJavaObject object;
		if (value instanceof IJavaObject) {
			object = (IJavaObject)value;
		} else {
			object = null;
		}
		if (object == null || !thread.isSuspended()) {
			// primitive or thread is no longer suspended
			return value.getValueString();
		}
		
		final IJavaValue[] toString = new IJavaValue[1];
		final DebugException[] ex = new DebugException[1];
		Runnable eval= new Runnable() {
			public void run() {
				try {
					toString[0] = object.sendMessage(JDIModelPresentation.fgToString, JDIModelPresentation.fgToStringSignature, null, thread, false);
				} catch (DebugException e) {
					ex[0]= e;
				}					
				synchronized (JDIModelPresentation.this) {
					JDIModelPresentation.this.notifyAll();
				}
			}
		};
		
		int timeout = 3000;
		Thread evalThread = new Thread(eval);
		evalThread.start();
		try {
			wait(timeout);
		} catch (InterruptedException e) {
		}
		
		if (ex[0] != null) {
			throw ex[0];
		}
		
		if (toString[0] != null) {
			return toString[0].getValueString();
		}	
		
		return "Error: timeout evaluating #toString()";
	}
			
	/**
	 * Returns a label for the item
	 */
	public String getText(Object item) {
		try {
			boolean showQualified= isShowQualifiedNames();
			if (item instanceof IJavaVariable) {
				return getVariableText((IJavaVariable) item);
			} else if (item instanceof IStackFrame) {
				return getStackFrameText((IStackFrame) item);
			} else if (item instanceof IMarker) {
				IBreakpoint breakpoint = getBreakpoint((IMarker)item);
				if (breakpoint != null) {
					return getBreakpointText(breakpoint);
				}
				return null;
			} else if (item instanceof IBreakpoint) {
				return getBreakpointText((IBreakpoint)item);
			} else {
				String label= null;
				if (item instanceof IJavaThread) {
					label= getThreadText((IJavaThread) item, showQualified);
				} else if (item instanceof IJavaDebugTarget) {
					label= getDebugTargetText((IJavaDebugTarget) item, showQualified);
				} else if (item instanceof IJavaValue) {
					label= getValueText((IJavaValue) item);
				}
				if (item instanceof ITerminate) {
					if (((ITerminate) item).isTerminated()) {
						label= DebugUIUtils.getResourceString(PREFIX + TERMINATED) + label;
						return label;
					}
				}
				return label;
			}
		} catch (CoreException e) {
			return getResourceString(NOT_RESPONDING);
		}
	}

	protected IBreakpoint getBreakpoint(IMarker marker) {
		return DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(marker);
		}
	
	/**
	 * Build the text for an IJavaThread.
	 */
	protected String getThreadText(IJavaThread thread, boolean qualified) throws CoreException {
		if (thread.isTerminated()) {
			if (thread.isSystemThread()) {
				return getFormattedString(TERMINATED_SYS, thread.getName());
			} else {
				return getFormattedString(TERMINATED_USR, thread.getName());
			}
		}
		if (thread.isStepping()) {
			if (thread.isSystemThread()) {
				return getFormattedString(STEPPING_SYS, thread.getName());
			} else {
				return getFormattedString(STEPPING_USR, thread.getName());
			}
		}
		if (!thread.isSuspended()) {
			if (thread.isSystemThread()) {
				return getFormattedString(RUNNING_SYS, thread.getName());
			} else {
				return getFormattedString(RUNNING_USR, thread.getName());
			}
		}
		IJavaBreakpoint breakpoint= (IJavaBreakpoint)thread.getBreakpoint();
		if (breakpoint != null) {
			String typeName= getMarkerTypeName(breakpoint, qualified);
			if (breakpoint instanceof IJavaExceptionBreakpoint) {
				if (thread.isSystemThread()) {
					return getFormattedString(EXCEPTION_SYS, new String[] {thread.getName(), typeName});
				} else {
					return getFormattedString(EXCEPTION_USR, new String[] {thread.getName(), typeName});
				}
			}
			if (breakpoint instanceof IJavaWatchpoint) {
				IJavaWatchpoint wp = (IJavaWatchpoint)breakpoint;
				IField field = wp.getField();
				String fieldName = "";
				if (field != null) {
					fieldName = field.getElementName();
				}
				if (wp.isAccessSuspend(thread.getDebugTarget())) {
					if (thread.isSystemThread()) {
						return getFormattedString(ACCESS_SYS, new String[] {thread.getName(), fieldName, typeName});
					} else {
						return getFormattedString(ACCESS_USR, new String[] {thread.getName(), fieldName, typeName});
					}
				} else {
					// modification
					if (thread.isSystemThread()) {
						return getFormattedString(MODIFICATION_SYS, new String[] {thread.getName(), fieldName, typeName});
					} else {
						return getFormattedString(MODIFICATION_USR, new String[] {thread.getName(), fieldName, typeName});
					}
				}
			}
			if (breakpoint instanceof IJavaMethodEntryBreakpoint) {
				IJavaMethodEntryBreakpoint me= (IJavaMethodEntryBreakpoint)breakpoint;
				IMethod method= me.getMethod();
				String methodName= "";
				if (method != null) {
					methodName= method.getElementName();
				}
				if (thread.isSystemThread()) {
					return getFormattedString(METHOD_ENTRY_SYS, new String[] {thread.getName(), methodName, typeName});
				} else {
					return getFormattedString(METHOD_ENTRY_USR, new String[] {thread.getName(), methodName, typeName});
				}
			}
			if (breakpoint instanceof IJavaLineBreakpoint) {
				int lineNumber= ((IJavaLineBreakpoint)breakpoint).getLineNumber();
				if (lineNumber > -1) {
					if (thread.isSystemThread()) {
						if (breakpoint instanceof IJavaRunToLineBreakpoint) {
							return getFormattedString(RUN_TO_LINE_SYS, new String[] {thread.getName(), String.valueOf(lineNumber), typeName});
						} else {
							return getFormattedString(BREAKPOINT_SYS, new String[] {thread.getName(), String.valueOf(lineNumber), typeName});
						}
					} else {
						if (breakpoint instanceof IJavaRunToLineBreakpoint) {
							return getFormattedString(RUN_TO_LINE_USR, new String[] {thread.getName(), String.valueOf(lineNumber), typeName});
						} else {
							return getFormattedString(BREAKPOINT_USR, new String[] {thread.getName(), String.valueOf(lineNumber), typeName});
						}
					}
				}
			}
		}

		// Otherwise, it's just suspended
		if (thread.isSystemThread()) {
			return getFormattedString(SUSPENDED_SYS, thread.getName());
		} else {
			return getFormattedString(SUSPENDED_USR, thread.getName());
		}
	}

	/**
	 * Build the text for an IJavaThread.
	 */
	protected String getDebugTargetText(IJavaDebugTarget debugTarget, boolean qualified) throws DebugException {
		String labelString= debugTarget.getName();
		if (!qualified) {
			int index= labelString.lastIndexOf('.');
			if (index != -1) {
				labelString= labelString.substring(index + 1);
			}
		}
		return labelString;
	}

	/**
	 * Build the text for an IJavaValue.
	 */
	protected String getValueText(IJavaValue value) throws DebugException {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		boolean showHexValues= store.getBoolean(IPreferencesConstants.SHOW_HEX_VALUES);		
		boolean showCharValues= store.getBoolean(IPreferencesConstants.SHOW_CHAR_VALUES);
		boolean showUnsignedValues= store.getBoolean(IPreferencesConstants.SHOW_UNSIGNED_VALUES);
		
		String refTypeName= value.getReferenceTypeName();
		String valueString= value.getValueString();
		boolean isString= refTypeName.equals(fgStringName);
		String signature= value.getSignature();
		if ("V".equals(signature)) {
			valueString= getResourceString(NO_RETURN_VALUE);
		}
		boolean isObject= isObjectValue(signature);
		boolean isArray= getArrayDimension(signature) > 0 ? true : false;
		StringBuffer buffer= new StringBuffer();
		// Always show type name for objects & arrays (but not Strings)
		if ((isObject || isArray) && !isString && (refTypeName.length() > 0)) {
			String qualTypeName= getQualifiedName(refTypeName);
			if (isArray) {
				qualTypeName= adjustTypeNameForArrayIndex(qualTypeName, value.getArrayLength());
			}
			buffer.append(qualTypeName);
			buffer.append(' ');
		}
		
		// Put double quotes around Strings
		if (valueString != null && (isString || valueString.length() > 0)) {
			if (isString) {
				buffer.append('"');
			}
			buffer.append(valueString);
			if (isString) {
				buffer.append('"');
			}
		}
		
		// show unsigned value second, if applicable
		if (showUnsignedValues) {
			buffer= appendUnsignedText(value, buffer);
		}
		// show hex value third, if applicable
		if (showHexValues) {
			buffer= appendHexText(value, buffer);
		}
		// show byte character value last, if applicable
		if (showCharValues) {
			buffer= appendCharText(value, buffer);
		}
		
		return buffer.toString();
	}
	

	private StringBuffer appendUnsignedText(IJavaValue value, StringBuffer buffer) throws DebugException {
		String unsignedText= getValueUnsignedText(value);
		if (unsignedText != null) {
			buffer.append(" [");
			buffer.append(unsignedText);
			buffer.append("]");
		}
		return buffer;	
	}
		
	private StringBuffer appendHexText(IJavaValue value, StringBuffer buffer) throws DebugException {
		String hexText = getValueHexText(value);
		if (hexText != null) {
			buffer.append(" [");
			buffer.append(hexText);
			buffer.append("]");
		}		
		return buffer;
	}
	
	private StringBuffer appendCharText(IJavaValue value, StringBuffer buffer) throws DebugException {
		String charText= getValueCharText(value);
		if (charText != null) {
			buffer.append(" [");
			buffer.append(charText);
			buffer.append("]");
		}		
		return buffer;
	}
	
	/**
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty() != IPreferencesConstants.VARIABLE_RENDERING) {
			return;
		}
		IDebugTarget[] targets= DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
		if (targets == null || targets.length == 0) {
			return;
		}
		for (int i=0; i<targets.length; i++) {
			if (targets[i] instanceof IJavaDebugTarget && !targets[i].isTerminated()) {
				 fireEvent(new DebugEvent(targets[i], DebugEvent.CHANGE));
				 break;
			}
		}
	}
	
	/**
	 * Fire a debug event
	 */
	public void fireEvent(DebugEvent event) {
		DebugPlugin.getDefault().fireDebugEvent(event);
	}	

	/**
	 * Given a JNI-style signature String for a IJavaValue, return true
	 * if the signature represents an Object or an array of Objects.
	 */
	protected boolean isObjectValue(String signature) {
		if (signature == null) {
			return false;
		}
		char sigChar= ' ';
		for (int i= 0; i < signature.length(); i++) {
			sigChar= signature.charAt(i);
			if (sigChar == '[') {
				continue;
			}
			break;
		}
		if ((sigChar == 'L') || (sigChar == 'Q')) {
			return true;
		}
		return false;
	}
	
	/**
	 * Given a JNI-style signature String for a IJavaValue, return true
	 * if the signature represents a ByteValue
	 */
	protected boolean isByteValue(String signature) {
		if (signature == null) {
			return false;
		}
		return signature.equals("B");
	}
	
	/**
	 * Returns the character string of a byte or <code>null</code if
	 * the value is not a byte.
	 */
	protected String getValueCharText(IJavaValue value) throws DebugException {
		String sig= value.getSignature();
		if (sig == null || sig.length() > 1) {
			return null;
		}
		
		String valueString= value.getValueString();
		int intValue= 0;	
		switch (sig.charAt(0)) {
			case 'B' : // byte
				intValue= Integer.parseInt(valueString);
				intValue= intValue & 0xFF; // Only lower 8 bits
				break;
			case 'I' : // int
				intValue= Integer.parseInt(valueString);
				if (intValue > 255 || intValue < 0) {
					return null;
				}
				break;
			case 'S' : // short
				intValue= Integer.parseInt(valueString);
				if (intValue > 255 || intValue < 0) {
					return null;
				}
				break;
			case 'J' :
				long longValue= Long.parseLong(valueString);
				if (longValue > 255 || longValue < 0) {
					// Out of character range
					return null;
				}
				intValue= (int) longValue;
				break;
			default :
				return null;
		};
		String c = "";
		if (Character.getType((char) intValue) == Character.CONTROL) {
			Character ctrl = new Character((char) (intValue + 64));
			c = "^" + ctrl;
			switch (intValue) { // common use
				case 0: c += " (NUL)"; break;
				case 8: c += " (BS)"; break;
				case 9: c += " (TAB)"; break;
				case 10: c += " (LF)"; break;
				case 13: c += " (CR)"; break;
				case 21: c += " (NL)"; break;
				case 27: c += " (ESC)"; break;
				case 127: c += " (DEL)"; break;
			}
		} else {
			c += new Character((char)intValue);
		}
		return c;
	}

	protected String getMarkerTypeName(IJavaBreakpoint breakpoint, boolean qualified) throws CoreException {
		String typeName= "";
		IType type = breakpoint.getType();
		if (type != null) {
			typeName= type.getFullyQualifiedName();
			if (!qualified) {
				int index= typeName.lastIndexOf('.');
				if (index != -1) {
					typeName= typeName.substring(index + 1);
				}
			}
		}
		return typeName;
	}

	/**
	 * Maps a Java element to an appropriate image.
	 */
	public Image getImage(Object item) {
		try {
			if (item instanceof IJavaVariable) {
				return getVariableImage((IAdaptable) item);
			}
			if (item instanceof IMarker) {
				IBreakpoint bp = getBreakpoint((IMarker)item);
				if (bp != null && bp instanceof IJavaBreakpoint) {
					return getBreakpointImage((IJavaBreakpoint)bp);
				}
			}
			if (item instanceof IJavaBreakpoint) {
				return getBreakpointImage((IJavaBreakpoint)item);
			}
		} catch (CoreException e) {
		}
		return null;
	}

	protected Image getBreakpointImage(IJavaBreakpoint breakpoint) throws CoreException {
		if (breakpoint instanceof IJavaExceptionBreakpoint) {
			return getExceptionBreakpointImage((IJavaExceptionBreakpoint)breakpoint);
		} if (breakpoint instanceof IJavaRunToLineBreakpoint) {
			return null;
		} else {
			return getJavaBreakpointImage(breakpoint);
		}
	}

	protected Image getExceptionBreakpointImage(IJavaExceptionBreakpoint exception) throws CoreException {
		if (!exception.isEnabled()) {
			return DebugUITools.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED);
		} else if (exception.isChecked()) {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		} else {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ERROR);
		}
	}

	protected Image getJavaBreakpointImage(IJavaBreakpoint breakpoint) throws CoreException {
		if (!breakpoint.isEnabled()) {
			return DebugUITools.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED);
		}
		if (breakpoint.isInstalled()) {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_BREAKPOINT_INSTALLED);
		}
		return DebugUITools.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT);
	}

	protected Image getVariableImage(IAdaptable element) {
		JavaElementImageDescriptor descriptor= new JavaElementImageDescriptor(
			computeBaseImageDescriptor(element), computeAdornmentFlags(element), BIG_SIZE);

		return fRegistry.get(descriptor);			
	}
	
	private ImageDescriptor computeBaseImageDescriptor(IAdaptable element) {
		IJavaVariable javaVariable= (IJavaVariable) element.getAdapter(IJavaVariable.class);
		if (javaVariable != null) {
			try {
				if (javaVariable.isPublic())
					return JavaPluginImages.DESC_MISC_PUBLIC;
				if (javaVariable.isProtected())
					return JavaPluginImages.DESC_MISC_PROTECTED;
				if (javaVariable.isPrivate())
					return JavaPluginImages.DESC_MISC_PRIVATE;
			} catch (DebugException e) {
			}
		}
		return JavaPluginImages.DESC_MISC_DEFAULT;
	}
	
	private int computeAdornmentFlags(IAdaptable element) {
		int flags= 0;
		IJavaModifiers javaProperties= (IJavaModifiers)element.getAdapter(IJavaModifiers.class);
		try {
			if (javaProperties != null) {
				if (javaProperties.isFinal()) {
					flags |= JavaElementImageDescriptor.FINAL;
				}
				if (javaProperties.isStatic()) {
					flags |= JavaElementImageDescriptor.STATIC;
				}
			}
		} catch(DebugException e) {
			// fall through
		}
		return flags;
	}

	/**
	 * @see IDebugModelPresentation
	 */
	public IEditorInput getEditorInput(Object item) {
		try {
			if (item instanceof IMarker) {
				item = getBreakpoint((IMarker)item);
			}
			if (item instanceof IJavaBreakpoint) {
				item= ((IJavaBreakpoint)item).getType();
			}
			if (item instanceof IType) {
				promptForSource((IType)item);
			}
			return EditorUtility.getEditorInput(item);
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * @see IDebugModelPresentaion
	 */
	public String getEditorId(IEditorInput input, Object inputObject) {
		IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
		IEditorDescriptor descriptor= registry.getDefaultEditor(input.getName());
		if (descriptor != null)
			return descriptor.getId();
		
		return null;
	}

	/**
	 * @see IDebugModelPresentation
	 */
	public void setAttribute(String id, Object value) {
		if (value == null) {
			return;
		}
		fAttributes.put(id, value);
	}

	protected boolean isShowQualifiedNames() {
		Boolean showQualified= (Boolean) fAttributes.get(DISPLAY_QUALIFIED_NAMES);
		showQualified= showQualified == null ? Boolean.FALSE : showQualified;
		return showQualified.booleanValue();
	}

	protected boolean isShowVariableTypeNames() {
		Boolean show= (Boolean) fAttributes.get(DISPLAY_VARIABLE_TYPE_NAMES);
		show= show == null ? Boolean.FALSE : show;
		return show.booleanValue();
	}

	protected String getVariableText(IJavaVariable var) throws DebugException {
		String varLabel= var.getName();
		if (varLabel != null) {
			boolean showTypes= isShowVariableTypeNames();
			int spaceIndex= varLabel.lastIndexOf(' ');
			StringBuffer buff= new StringBuffer();
			String typeName= var.getReferenceTypeName();
			if (showTypes && spaceIndex == -1) {
				typeName= getQualifiedName(typeName);
				if (typeName.length() > 0) {
					buff.append(typeName);
					buff.append(' ');
				}
			}
			if (spaceIndex != -1 && !showTypes) {
				varLabel= varLabel.substring(spaceIndex + 1);
			}
			buff.append(varLabel);

			IJavaValue javaValue= (IJavaValue) var.getValue();
			String valueString= getValueText(javaValue);
			if (valueString.length() > 0) {
				buff.append("= ");
				buff.append(valueString);
			}
			return buff.toString();
		}
		return "";
	}

	/**
	 * Given a JNI-style signature for a variable, return the dimension of
	 * the array the variable represents, or 0 if it is not an array.
	 */
	protected int getArrayDimension(String signature) {
		if (signature == null) {
			return 0;
		}
		int dimCount= 0;
		for (int i= 0; i < signature.length(); i++) {
			if (signature.charAt(i) == '[') {
				dimCount++;
			} else {
				break;
			}
		}
		return dimCount;
	}

	/**
	 * Given the reference type name of an array type, insert the array length
	 * in between the '[]' for the first dimension and return the result.
	 */
	protected String adjustTypeNameForArrayIndex(String typeName, int arrayIndex) {
		int firstBracket= typeName.indexOf("[]");
		if (firstBracket < 0) {
			return typeName;
		}
		StringBuffer buffer= new StringBuffer(typeName);
		buffer.insert(firstBracket + 1, Integer.toString(arrayIndex));
		return buffer.toString();
	}
	
	protected String getValueUnsignedText(IJavaValue value) throws DebugException {
		String sig= value.getSignature();
		if (sig == null || sig.length() > 1) {
			return null;
		}

		switch (sig.charAt(0)) {
			case 'B' : // byte
				int byteVal= Integer.parseInt(value.getValueString());
				if (byteVal < 0) {
					byteVal = byteVal & 0xFF;
					return Integer.toString(byteVal);					
				}
			default :
				return null;
		}
	}

	protected String getValueHexText(IJavaValue value) throws DebugException {
		String sig= value.getSignature();
		if (sig == null || sig.length() > 1) {
			return null;
		}

		StringBuffer buff= new StringBuffer();
		switch (sig.charAt(0)) {
			case 'B' :
				buff.append("0x");
				int byteVal = Integer.parseInt(value.getValueString());
				byteVal = byteVal & 0xFF;
				buff.append(Integer.toHexString(byteVal));
				break;
			case 'I' :
				buff.append("0x");
				buff.append(Integer.toHexString(Integer.parseInt(value.getValueString())));
				break;			
			case 'S' :
				buff.append("0x");
				int shortVal = Integer.parseInt(value.getValueString());
				shortVal = shortVal & 0xFFFF;
				buff.append(Integer.toHexString(shortVal));
				break;
			case 'J' :
				buff.append("0x");
				buff.append(Long.toHexString(Long.parseLong(value.getValueString())));
				break;
			case 'C' :
				buff.append("\\u");
				String hexString= Integer.toHexString(value.getValueString().charAt(0));
				int length= hexString.length();
				while (length < 4) {
					buff.append('0');
					length++;
				}
				buff.append(hexString);
				break;
		}
		return buff.toString();
	}

	protected String getBreakpointText(IBreakpoint breakpoint) throws CoreException {

		if (breakpoint instanceof IJavaExceptionBreakpoint) {
			return getExceptionBreakpointText((IJavaExceptionBreakpoint)breakpoint);
		}
		if (breakpoint instanceof IJavaWatchpoint) {
			return getWatchpointText((IJavaWatchpoint)breakpoint);
		} else if (breakpoint instanceof IJavaLineBreakpoint) {
			return getLineBreakpointText((IJavaLineBreakpoint)breakpoint);
		}

		return "";
	}

	protected String getExceptionBreakpointText(IJavaExceptionBreakpoint breakpoint) throws CoreException {

		StringBuffer buffer = new StringBuffer();
		IType type = breakpoint.getType();
		if (type != null) {
			boolean showQualified= isShowQualifiedNames();
			if (showQualified) {
				buffer.append(type.getFullyQualifiedName());
			} else {
				buffer.append(type.getElementName());
			}
		}
		int hitCount= breakpoint.getHitCount();
		if (hitCount > 0) {
			buffer.append(" [");
			buffer.append(DebugUIUtils.getResourceString(PREFIX + HITCOUNT));
			buffer.append(' ');
			buffer.append(hitCount);
			buffer.append(']');
		}		

		String state= null;
		boolean c= breakpoint.isCaught();
		boolean u= breakpoint.isUncaught();
		if (c && u) {
			state= CAUGHTANDUNCAUGHT;
		} else if (c) {
			state= CAUGHT;
		} else if (u) {
			state= UNCAUGHT;
		}
		String label= null;
		if (state == null) {
			label= buffer.toString();
		} else {
			String format= DebugUIUtils.getResourceString(BREAKPOINT_FORMAT);
			state= DebugUIUtils.getResourceString(state);
			label= MessageFormat.format(format, new Object[] {state, buffer});
		}
		return label;

	}

	protected String getLineBreakpointText(IJavaLineBreakpoint breakpoint) throws CoreException {

		boolean showQualified= isShowQualifiedNames();
		IType type= breakpoint.getType();
		IMember member= breakpoint.getMember();
		if (type != null) {
			StringBuffer label= new StringBuffer();
			if (showQualified) {
				label.append(type.getFullyQualifiedName());
			} else {
				label.append(type.getElementName());
			}
			int lineNumber= breakpoint.getLineNumber();
			if (lineNumber > 0) {
				label.append(" [");
				label.append(DebugUIUtils.getResourceString(PREFIX + LINE));
				label.append(' ');
				label.append(lineNumber);
				label.append(']');

			}
			int hitCount= breakpoint.getHitCount();
			if (hitCount > 0) {
				label.append(" [");
				label.append(DebugUIUtils.getResourceString(PREFIX + HITCOUNT));
				label.append(' ');
				label.append(hitCount);
				label.append(']');
			}
			if (member != null) {
				label.append(" - ");
				label.append(fJavaLabelProvider.getText(member));
			}
			return label.toString();
		}
		return "";

	}
	
	protected String getWatchpointText(IJavaWatchpoint watchpoint) throws CoreException {
		
		boolean showQualified= isShowQualifiedNames();
		String lineInfo= getLineBreakpointText(watchpoint);

		String state= null;
		boolean access= watchpoint.isAccess();
		boolean modification= watchpoint.isModification();
		if (access && modification) {
			state= ACCESSANDMODIFICATION;
		} else if (access) {
			state= ACCESS;
		} else if (modification) {
			state= MODIFICATION;
		}		
		String label= null;
		if (state == null) {
			label= lineInfo;
		} else {
			String format= DebugUIUtils.getResourceString(BREAKPOINT_FORMAT);
			state= DebugUIUtils.getResourceString(state);
			label= MessageFormat.format(format, new Object[] {state, lineInfo});
		}
		return label;	
	}	

	protected String getStackFrameText(IStackFrame stackFrame) throws DebugException {
		IJavaStackFrame frame= (IJavaStackFrame) stackFrame.getAdapter(IJavaStackFrame.class);
		if (frame != null) {
			StringBuffer label= new StringBuffer();

			// receiver name
			String rec= frame.getReceivingTypeName();
			label.append(getQualifiedName(rec));

			// declaring type name if different
			String dec= frame.getDeclaringTypeName();
			if (!dec.equals(rec)) {
				label.append('(');
				label.append(getQualifiedName(dec));
				label.append(')');
			}

			// append a dot separator and method name
			label.append('.');
			label.append(frame.getMethodName());

			List args= frame.getArgumentTypeNames();
			if (args.isEmpty()) {
				label.append("()");
			} else {
				label.append('(');
				Iterator iter= args.iterator();
				while (iter.hasNext()) {
					label.append(getQualifiedName((String) iter.next()));
					if (iter.hasNext()) {
						label.append(", ");
					}
				}
				label.append(')');
			}

			int lineNumber= frame.getLineNumber();
			if (lineNumber >= 0) {
				label.append(' ');
				label.append(DebugUIUtils.getResourceString(PREFIX + LINE));
				label.append(' ');
				label.append(lineNumber);
			}
			return label.toString();

		}
		return null;
	}

	protected String getQualifiedName(String qualifiedName) {
		if (!isShowQualifiedNames()) {
			int index= qualifiedName.lastIndexOf('.');
			if (index >= 0) {
				return qualifiedName.substring(index + 1);
			}
		}
		return qualifiedName;
	}

	/**
	 * Plug in the single argument to the resource String for the key to get a formatted resource String
	 */
	public static String getFormattedString(String key, String arg) {
		return getFormattedString(key, new String[] {arg});
	}

	/**
	 * Plug in the arguments to the resource String for the key to get a formatted resource String
	 */
	public static String getFormattedString(String key, String[] args) {
		String string= DebugUIUtils.getResourceString(key);
		return MessageFormat.format(string, args);
	}
	
	/**
	 * Convenience method to get a resource string
	 */
	public static String getResourceString(String key) {
		return DebugUIUtils.getResourceString(key);
	}

	/**
	 * Prompts for source if required
	 */
	protected void promptForSource(IType type) {
		if (type.isBinary()) {
			IPackageFragmentRoot root = (IPackageFragmentRoot)type.getClassFile().getParent().getParent();
			if (root.isArchive()) {
				try {
					if (root.getSourceAttachmentPath() == null && SourceAttachmentWizard.isOkToPrompt(root)) {
						Shell shell = JavaPlugin.getActiveWorkbenchShell();
						SourceAttachmentWizard wizard= new SourceAttachmentWizard(root);
						WizardDialog wd = new WizardDialog(shell, wizard);
						wd.open();
					}
				} catch (JavaModelException e) {
				}
			}
		}
	}
	/**
	 * When a thread suspends, add it to the thread pool for that
	 * VM. When a thread resumes, remove it from the thread pool.
	 * 
	 * @see IDebugEventListener#handleDebugEvent(DebugEvent)
	 */
	public void handleDebugEvent(DebugEvent event) {
		if (event.getSource() instanceof IJavaThread) {
			IJavaThread thread = (IJavaThread)event.getSource();
			if (event.getKind() == DebugEvent.RESUME) {
				List threads = (List)fThreadPool.get(thread.getDebugTarget());
				if (threads != null) {
					threads.remove(thread);
				}
			} else if (event.getKind() == DebugEvent.SUSPEND) {
				IDebugTarget target = thread.getDebugTarget();
				List threads = (List)fThreadPool.get(target);
				if (threads == null) {
					threads = new ArrayList();
					fThreadPool.put(target, threads);
				}
				threads.add(thread);	
			}
		}
	}

}
