package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.*;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IEditorInput;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.*;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.launcher.DebugOverlayDescriptorFactory;
import org.eclipse.jdt.internal.ui.viewsupport.OverlayIconManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @see IDebugModelPresentation
 */
public class JDIModelPresentation extends LabelProvider implements IDebugModelPresentation {

	protected static final String DISPLAY_HEX_VALUES= "org.eclipse.jdt.ui.displayHexValues";
	protected HashMap fAttributes= new HashMap(3);

	protected OverlayIconManager fIconManager= new OverlayIconManager(new DebugOverlayDescriptorFactory());

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
	private static final String BREAKPOINT_SYS= LABEL + "breakpoint_sys";
	private static final String BREAKPOINT_USR= LABEL + "breakpoint_usr";
	private static final String RUN_TO_LINE_SYS= LABEL + "run_to_line_sys";
	private static final String RUN_TO_LINE_USR= LABEL + "run_to_line_usr";
	private static final String SUSPENDED_SYS= LABEL + "suspended_sys";
	private static final String SUSPENDED_USR= LABEL + "suspended_usr";

	private static final String PREFIX= "jdi_label_provider.";
	private static final String TERMINATED= "terminated";
	private static final String LINE= "line";
	private static final String HITCOUNT= "hitCount";

	protected final static String EXCEPTION= PREFIX + "exception.";
	protected final static String FORMAT= EXCEPTION + "format";
	protected final static String CAUGHT= EXCEPTION + "caught";
	protected final static String UNCAUGHT= EXCEPTION + "uncaught";
	protected final static String BOTH= EXCEPTION + "both";
	protected final static String DISABLED= EXCEPTION + "disabled";

	protected static final String fgStringName= "java.lang.String";

	protected JavaElementLabelProvider fJavaLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);

	/**
	 * Returns a label for the item
	 */
	public String getText(Object item) {
		boolean showQualified= isShowQualifiedNames();
		if (item instanceof IJavaVariable) {
			return getVariableText((IJavaVariable) item);
		} else if (item instanceof IStackFrame) {
			return getStackFrameText((IStackFrame) item);
		} else if (item instanceof IMarker) {
			return getMarkerText((IMarker) item);
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
	}

	/**
	 * Build the text for an IJavaThread.
	 */
	protected String getThreadText(IJavaThread thread, boolean qualified) {
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
		IMarker breakpoint= thread.getBreakpoint();
		if (breakpoint != null && breakpoint.exists()) {
			String typeName= getMarkerTypeName(breakpoint, qualified);
			if (JDIDebugModel.isExceptionBreakpoint(breakpoint)) {
				if (thread.isSystemThread()) {
					return getFormattedString(EXCEPTION_SYS, new String[] {thread.getName(), typeName});
				} else {
					return getFormattedString(EXCEPTION_USR, new String[] {thread.getName(), typeName});
				}
			}
			int lineNumber= breakpoint.getAttribute(IMarker.LINE_NUMBER, -1);
			if (lineNumber > -1) {
				if (thread.isSystemThread()) {
					if (JDIDebugModel.isRunToLineBreakpoint(breakpoint)) {
						return getFormattedString(RUN_TO_LINE_SYS, new String[] {thread.getName(), String.valueOf(lineNumber), typeName});
					} else {
						return getFormattedString(BREAKPOINT_SYS, new String[] {thread.getName(), String.valueOf(lineNumber), typeName});
					}
				} else {
					if (JDIDebugModel.isRunToLineBreakpoint(breakpoint)) {
						return getFormattedString(RUN_TO_LINE_USR, new String[] {thread.getName(), String.valueOf(lineNumber), typeName});
					} else {
						return getFormattedString(BREAKPOINT_USR, new String[] {thread.getName(), String.valueOf(lineNumber), typeName});
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
	protected String getDebugTargetText(IJavaDebugTarget debugTarget, boolean qualified) {
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
	protected String getValueText(IJavaValue value) {
		String refTypeName= value.getReferenceTypeName();
		String valueString= value.getValueString();
		boolean isString= refTypeName.equals(fgStringName);
		String signature= value.getSignature();
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
		if (isShowHexValues()) {
			String hexText= getValueHexText(value);
			if (hexText != null) {
				buffer.append(hexText);
			}
		}
		return buffer.toString();
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

	protected String getMarkerTypeName(IMarker marker, boolean qualified) {
		String typeName= "";
		typeName= JDIDebugModel.getType(marker).getFullyQualifiedName();
		if (!qualified) {
			int index= typeName.lastIndexOf('.');
			if (index != -1) {
				typeName= typeName.substring(index + 1);
			}
		}
		return typeName;
	}

	/**
	 * Maps a Java element to an appropriate image.
	 */
	public Image getImage(Object item) {
		if (item instanceof IJavaVariable) {
			return getVariableImage((IAdaptable) item);
		}
		if (item instanceof IMarker) {
			return getMarkerImage((IMarker) item);
		}
		return null;
	}

	protected Image getMarkerImage(IMarker breakpoint) {
		if (JDIDebugModel.isExceptionBreakpoint(breakpoint)) {
			return getExceptionBreakpointImage(breakpoint);
		} else {
			return getBreakpointImage(breakpoint);
		}
	}

	protected Image getExceptionBreakpointImage(IMarker exception) {
		IBreakpointManager manager= DebugPlugin.getDefault().getBreakpointManager();

		if (!manager.isEnabled(exception)) {
			return DebugUITools.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED);
		} else if (JDIDebugModel.isChecked(exception)) {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		} else {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ERROR);
		}
	}

	protected Image getBreakpointImage(IMarker breakpoint) {
		IBreakpointManager manager= DebugPlugin.getDefault().getBreakpointManager();
		if (!manager.isEnabled(breakpoint)) {
			return DebugUITools.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED);
		}
		if (JDIDebugModel.isInstalled(breakpoint)) {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_BREAKPOINT_INSTALLED);
		}
		return DebugUITools.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT);
	}

	protected Image getVariableImage(IAdaptable element) {
		IJavaVariable javaVariable= (IJavaVariable) element.getAdapter(IJavaVariable.class);
		if (javaVariable != null) {
			if (javaVariable.isPublic())
				return fIconManager.getIcon(JavaPluginImages.IMG_MISC_PUBLIC, element);
			if (javaVariable.isProtected())
				return fIconManager.getIcon(JavaPluginImages.IMG_MISC_PROTECTED, element);
			if (javaVariable.isPrivate())
				return fIconManager.getIcon(JavaPluginImages.IMG_MISC_PRIVATE, element);

			return fIconManager.getIcon(JavaPluginImages.IMG_MISC_DEFAULT, element);
		}
		return null;
	}

	/**
	 * @see IDebugModelPresentation
	 */
	public IEditorInput getEditorInput(Object item) {
		if (item instanceof IMarker) {
			IMarker m= (IMarker) item;
			item= JDIDebugModel.getType(m);
		}
		try {
			return EditorUtility.getEditorInput(item);
		} catch (JavaModelException e) {
			return null;
		}
	}

	/**
	 * @see IDebugModelPresentaion
	 */
	public String getEditorId(IEditorInput input, Object inputObject) {
		return EditorUtility.getEditorID(input, inputObject);
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

	protected boolean isShowHexValues() {
		Boolean show= (Boolean) fAttributes.get(DISPLAY_HEX_VALUES);
		show= show == null ? Boolean.FALSE : show;
		return show.booleanValue();
	}

	protected String getVariableText(IJavaVariable var) {
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

	protected String getValueHexText(IJavaValue value) {
		String sig= value.getSignature();
		if (sig == null || sig.length() > 1) {
			return null;
		}

		StringBuffer buff= new StringBuffer(" (");
		switch (sig.charAt(0)) {
			case 'B' :
			case 'I' :
			case 'S' :
				buff.append("0x");
				buff.append(Integer.toHexString(Integer.parseInt(value.getValueString())));
				buff.append(')');
				break;
			case 'J' :
				buff.append("0x");
				buff.append(Long.toHexString(Long.parseLong(value.getValueString())));
				buff.append(')');
				break;
			case 'C' :
				buff.append("\\u");
				String hexString= Integer.toHexString(Integer.parseInt(value.getValueString()));
				int length= hexString.length();
				while (length < 4) {
					buff.append('0');
					length++;
				}
				break;
		}
		return buff.toString();
	}

	protected String getMarkerText(IMarker marker) {

		if (JDIDebugModel.isExceptionBreakpoint(marker)) {
			return getExceptionBreakpointText(marker);
		}
		String markerModelId= DebugPlugin.getDefault().getBreakpointManager().getModelIdentifier(marker);
		if (markerModelId.equals(JDIDebugModel.getPluginIdentifier())) {
			return getLineBreakpointText(marker);
		}

		return "";
	}

	protected String getExceptionBreakpointText(IMarker breakpoint) {

		String name;
		boolean showQualified= isShowQualifiedNames();
		if (showQualified) {
			name= JDIDebugModel.getType(breakpoint).getFullyQualifiedName();
		} else {
			name= JDIDebugModel.getType(breakpoint).getElementName();
		}

		String state= null;
		boolean c= JDIDebugModel.isCaught(breakpoint);
		boolean u= JDIDebugModel.isUncaught(breakpoint);
		if (c && u) {
			state= BOTH;
		} else if (c) {
			state= CAUGHT;
		} else if (u) {
			state= UNCAUGHT;
		}
		String label= null;
		if (state == null) {
			label= name;
		} else {
			String format= DebugUIUtils.getResourceString(FORMAT);
			state= DebugUIUtils.getResourceString(state);
			label= MessageFormat.format(format, new Object[] {state, name});
		}
		return label;

	}

	protected String getLineBreakpointText(IMarker breakpoint) {

		boolean showQualified= isShowQualifiedNames();
		IType type= JDIDebugModel.getType(breakpoint);
		//method entry breakpoints
		IMember member= JDIDebugModel.getMethod(breakpoint);
		if (member == null) {
			member= JDIDebugModel.getMember(breakpoint);

		}
		if (type != null) {
			StringBuffer label= new StringBuffer();
			if (showQualified) {
				label.append(type.getFullyQualifiedName());
			} else {
				label.append(type.getElementName());
			}
			int lineNumber= DebugPlugin.getDefault().getBreakpointManager().getLineNumber(breakpoint);
			if (lineNumber > 0) {
				label.append(" [");
				label.append(DebugUIUtils.getResourceString(PREFIX + LINE));
				label.append(' ');
				label.append(lineNumber);
				label.append(']');

			}
			int hitCount= JDIDebugModel.getHitCount(breakpoint);
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

	protected String getStackFrameText(IStackFrame stackFrame) {
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

}
