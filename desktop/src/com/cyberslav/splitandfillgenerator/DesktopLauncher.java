package com.cyberslav.splitandfillgenerator;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cyberslav.splitandfillgenerator.GeneratorApp;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher
{
	public static void main (String[] arg)
	{
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

		config.setForegroundFPS(30);
		config.setWindowedMode(1280, 720);
		config.setTitle("SplitAndFillGenerator");

		new Lwjgl3Application(new GeneratorApp(), config);
	}
}
