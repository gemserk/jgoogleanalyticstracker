package com.dmurph.tracking;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;

public class DesktopAnalyticsAutoConfigurator {
	/**
	 * Populates user language, color depth, screen resolution, and character encoding. Can't get flash version.
	 */
	static public void populateFromSystem(AnalyticsConfigData config) {
		config.setEncoding(System.getProperty("file.encoding"));

		String region = System.getProperty("user.region");
		if (region == null) {
			region = System.getProperty("user.country");
		}
		config.setUserLanguage(System.getProperty("user.language") + "-" + region);

		int screenHeight = 0;
		int screenWidth = 0;

		GraphicsEnvironment ge = null;
		GraphicsDevice[] gs = null;

		try {
			ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			gs = ge.getScreenDevices();

			// Get size of each screen
			for (int i = 0; i < gs.length; i++) {
				DisplayMode dm = gs[i].getDisplayMode();
				screenWidth += dm.getWidth();
				screenHeight += dm.getHeight();
			}
			if (screenHeight != 0 && screenWidth != 0) {
				config.setScreenResolution(screenWidth + "x" + screenHeight);
			}

			if (gs[0] != null) {
				String colorDepth = gs[0].getDisplayMode().getBitDepth() + "";
				for (int i = 1; i < gs.length; i++) {
					colorDepth += ", " + gs[i].getDisplayMode().getBitDepth();
				}
				config.setColorDepth(colorDepth);
			}
		} catch (HeadlessException e) {
			// report reasonable defaults.
			screenHeight = 1024;
			screenWidth = 768;
			config.setColorDepth("32");
		}
	}
}
