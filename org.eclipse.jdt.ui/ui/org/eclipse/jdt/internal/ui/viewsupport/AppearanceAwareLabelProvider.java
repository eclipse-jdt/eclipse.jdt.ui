package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;

/**
 * JavaUILabelProvider that respects settings from the Appearance preference page.
 * Triggers a viewer update when a preference changes.
 */
public class AppearanceAwareLabelProvider extends JavaUILabelProvider implements IPropertyChangeListener {

	public final static int DEFAULT_TEXTFLAGS= JavaElementLabels.ROOT_VARIABLE | JavaElementLabels.M_PARAMETER_TYPES |  JavaElementLabels.M_APP_RETURNTYPE;
	public final static int DEFAULT_IMAGEFLAGS= JavaElementImageProvider.OVERLAY_ICONS;
	
	private int fTextFlagMask;
	private int fImageFlagMask;

	/**
	 * Constructor for AppearanceAwareLabelProvider.
	 */
	public AppearanceAwareLabelProvider(int textFlags, int imageFlags, ILabelDecorator[] labelDecorators) {
		super(textFlags, imageFlags, labelDecorators);
		initMasks();
		PreferenceConstants.getPreferenceStore().addPropertyChangeListener(this);
	}

	/**
	 * Creates a labelProvider with DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS and the ErrorTickLabelDecorator.
	 */	
	public AppearanceAwareLabelProvider() {
		this(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS, new ILabelDecorator[] { new ProblemsLabelDecorator(null) });
	}
	
	private void initMasks() {
		fTextFlagMask= -1;
		if (!PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE)) {
			fTextFlagMask ^= JavaElementLabels.M_APP_RETURNTYPE;
		}
		if (!PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES)) {
			fTextFlagMask ^= JavaElementLabels.P_COMPRESSED;
		}
		
		fImageFlagMask= -1;
	}

	/*
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (property.equals(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE)
				|| property.equals(PreferenceConstants.APPEARANCE_OVERRIDE_INDICATOR)
				|| property.equals(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW)
				|| property.equals(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES)) {
			initMasks();
			LabelProviderChangedEvent lpEvent= new LabelProviderChangedEvent(this, null); // refresh all
			fireLabelProviderChanged(lpEvent);
		}		
	}

	/*
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		PreferenceConstants.getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}

	/*
	 * @see JavaUILabelProvider#evaluateImageFlags()
	 */
	protected int evaluateImageFlags(Object element) {
		return getImageFlags() & fImageFlagMask;
	}

	/*
	 * @see JavaUILabelProvider#evaluateTextFlags()
	 */
	protected int evaluateTextFlags(Object element) {
		return getTextFlags() & fTextFlagMask;
	}

}
