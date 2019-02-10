package com.valentinesdayhearts.app.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.valentinesdayhearts.app.ValentinesDayHeartsApp;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 800;
		config.height = 480;
		config.title = "Valentines Day Hearts";
		config.addIcon("data/Icon.png", Files.FileType.Internal);
		new LwjglApplication(new ValentinesDayHeartsApp(), config);
	}
}
