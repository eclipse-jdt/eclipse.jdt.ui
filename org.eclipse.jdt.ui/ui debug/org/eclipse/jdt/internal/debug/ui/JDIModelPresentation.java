/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui;

import java.text.MessageFormat;
import java.util.*;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.debug.core.*;
import org.eclipse.jdt.internal.ui.*;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.launcher.DebugOverlayDescriptorFactory;
import org.eclipse.jdt.internal.ui.viewsupport.OverlayIconManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;

/**
 * @see IDebugModelPresentation
 */
public class JDIModelPresentation extends LabelProvider implements IDebugModelPresentation, IPropertyChangeListener {

	protected HashMap fAttributes= new HashMap(3);

	static final Point BIG_SIZE= new Point(22, 16);
	protected OverlayIconManager fIconManager= new OverlayIconManager(new DebugOverlayDescriptorFactory(), BIG_SIZE);

	// Thread label resource String keys
	private static final String THREAD_PREFIX= "jdi_thread.";
	private static final String LABEL= THREAD_PREFIX + "label.";
	private static final String RUNNING_SYS= LABEL + "running_sys";
	private static final String RUNNING_USR= LABEL + "running_usr";
	private static final String TERMINATED_SYS= LABEL + "terminated_sys";
	private static final String TERMINATED_USR= LABEL + "terminated_usr";
	private static final String STEPPING_SYS= LABEL + "stepping_sys";
	private static final String STEPPING_USR= LABEL + "stepping_usr";
	private static final String SUSPENDED_SYS= LABEL + "suspended_sys";
	private static final String SUSPENDED_USR= LABEL + "suspended_usr";

	private static final String PREFIX= "jdi_model_presentation.";
	private static final String TERMINATED= "terminated";
	private static final String NOT_RESPONDING= PREFIX + "not_responding";
	private static final String LINE= "line";
	
	private static final String NO_RETURN_VALUE= PREFIX + "no_return_value";

	protected static final String fgStringName= "java.lang.String";

	protected JavaElementLabelProvider fJavaLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);

	public JDIModelPresentation() {
		super();
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(this);
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
		} catch (DebugException e) {
			return getResourceString(NOT_RESPONDING);
		}
	}

	/**
	 * Build the text for an IJavaThread.
	 */
	protected String getThreadText(IJavaThread thread, boolean qualified) throws DebugException {
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
		IJavaBreakpoint breakpoint= thread.getBreakpoint();
		if (breakpoint != null && breakpoint.exists()) {			
			return breakpoint.getThreadText(thread.getName(), qualified, thread.isSystemThread());
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

	/**
	 * Maps a Java element to an appropriate image.
	 */
	public Image getImage(Object item) {
		if (item instanceof IJavaVariable) {
			return getVariableImage((IAdaptable) item);
		}
		if (item instanceof IMarker) {
			IBreakpointManager manager= getBreakpointManager();
			IBreakpoint breakpoint= manager.getBreakpoint((IMarker)item);
			if (breakpoint instanceof IJavaBreakpoint) {
				return getBreakpointImage((IJavaBreakpoint)breakpoint);
			}
		}
		return null;
	}

	protected Image getBreakpointImage(IJavaBreakpoint breakpoint) {
		if (breakpoint instanceof IJavaExceptionBreakpoint) {
			return getExceptionBreakpointImage((IJavaExceptionBreakpoint)breakpoint);
		} if (breakpoint instanceof IJavaRunToLineBreakpoint) {
			return null;
		} else {
			return getLineBreakpointImage(breakpoint);
		}
	}

	protected Image getExceptionBreakpointImage(IJavaExceptionBreakpoint breakpoint) {
		if (!breakpoint.isEnabled()) {
			return DebugUITools.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED);
		} else if (breakpoint.isChecked()) {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		} else {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ERROR);
		}
	}

	protected Image getLineBreakpointImage(IJavaBreakpoint breakpoint) {
		if (!breakpoint.isEnabled()) {
			return DebugUITools.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED);
		}
		if (breakpoint.isInstalled()) {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_BREAKPOINT_INSTALLED);
		}
		return DebugUITools.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT);
	}

	protected Image getVariableImage(IAdaptable element) {
		IJavaVariable javaVariable= (IJavaVariable) element.getAdapter(IJavaVariable.class);
		if (javaVariable != null) {
			try {
				if (javaVariable.isPublic())
					return fIconManager.getIcon(JavaPluginImages.IMG_MISC_PUBLIC, element);
				if (javaVariable.isProtected())
					return fIconManager.getIcon(JavaPluginImages.IMG_MISC_PROTECTED, element);
				if (javaVariable.isPrivate())
					return fIconManager.getIcon(JavaPluginImages.IMG_MISC_PRIVATE, element);
			} catch (DebugException e) {
			}
			return fIconManager.getIcon(JavaPluginImages.IMG_MISC_DEFAULT, element);
		}
		return null;
	}

	/**
	 * @see IDebugModelPresentation
	 */
	public IEditorInput getEditorInput(Object item) {
		if (item instanceof IMarker) {
			IMarker marker= (IMarker) item;
			IBreakpoint breakpoint= getBreakpointManager().getBreakpoint(marker);
			if (breakpoint instanceof IJavaBreakpoint) {
				item= ((IJavaBreakpoint)breakpoint).getInstalledType();
			}
		}
		if (item instanceof IType) {
			promptForSource((IType)item);
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

	protected String getMarkerText(IMarker marker) {
		IBreakpoint breakpoint= (IBreakpoint)getBreakpointManager().getBreakpoint(marker);
		if (breakpoint instanceof IJavaLineBreakpoint) {
			IMember member= ((IJavaLineBreakpoint)breakpoint).getMethod();
			if (member == null) {
				member= ((IJavaLineBreakpoint)breakpoint).getMember();
			}
			return ((IJavaLineBreakpoint)breakpoint).getMarkerText(isShowQualifiedNames(), fJavaLabelProvider.getText(member));
		}
		else if (breakpoint instanceof IJavaExceptionBreakpoint) {			
			return ((IJavaExceptionBreakpoint)breakpoint).getMarkerText(isShowQualifiedNames());
		}
		return "";
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
	 * Returns the breakpoint manager
	 */
	private IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}
		
}
