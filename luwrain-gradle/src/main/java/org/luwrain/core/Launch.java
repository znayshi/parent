/*
   Copyright 2012-2024 Michael Pozhidaev <msp@luwrain.org>

   This file is part of LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.core;

import java.util.*;
import java.io.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import org.luwrain.core.util.*;

import static org.luwrain.core.Base.*;
import static org.luwrain.core.NullCheck.*;

public final class Launch implements Runnable
{
    static private final String
	CMDARG_HELP = "--help",
	CMDARG_PRINT_LANG = "--print-lang",
	CMDARG_PRINT_DIRS = "--print-dirs",
	CMDARG_CREATE_PROFILE = "--create-profile",
	CMDARG_CREATE_PROFILE_IN = "--create-profile-in=";

    private final boolean standalone;
    private final ClassLoader classLoader;
    private final File dataDir;
    private final File userDataDir;
    private final File userHomeDir;
    private final String lang;
    private final CmdLine cmdLine;
    private final Registry registry;
    private final PropertiesRegistry props;
    private OperatingSystem os = null;
    private Interaction interaction = null;

    Launch(boolean standalone, String[] cmdLine, File dataDir, File userDataDir, File userHomeDir)
    {
	notNullItems(cmdLine, "cmdLine");
	notNull(dataDir, "dataDir");
	notNull(userDataDir, "userDataDir");
	notNull(userHomeDir, "userHomeDir");
	debug("starting LUWRAIN: Java " + System.getProperty("java.version") + " by " + System.getProperty("java.vendor") + " (installed in " + System.getProperty("java.home") + ")");
	initLog4j();
	new JniLoader().autoload(this.getClass().getClassLoader());
	this.standalone = standalone;
	this.cmdLine = new CmdLine(cmdLine);
	this.dataDir = dataDir;
	this.userDataDir = userDataDir;
	this.userHomeDir = userHomeDir;
	this.lang = Checks.detectLang(this.cmdLine);
	if (lang.isEmpty())
	{
	    fatal("unable to select a language to use");
	    System.exit(1);
	}
	final org.luwrain.core.properties.PropertiesFiles filesProps = new org.luwrain.core.properties.PropertiesFiles();
	filesProps.load(new File(dataDir, "properties"));
	if (standalone)
	{
	    final org.luwrain.core.properties.Basic basicProps = new org.luwrain.core.properties.Basic(dataDir, userDataDir, userHomeDir);
	    this.props = new PropertiesRegistry(new PropertiesProvider[]{
		    basicProps,
		    filesProps,
		    new org.luwrain.core.properties.Player(),
		    new org.luwrain.core.properties.Listening(),
		});
	    this.registry = loadMemRegistry(dataDir, lang);
	} else
	{
	    filesProps.load(new File(userDataDir, "properties"));
	    final org.luwrain.core.properties.Basic basicProps = new org.luwrain.core.properties.Basic(dataDir, userDataDir, userHomeDir);
	    this.props = new PropertiesRegistry(new PropertiesProvider[]{
		    basicProps,
		    filesProps,
		    new org.luwrain.core.properties.Player(),
		    new org.luwrain.core.properties.Listening(),
		});
	    this.registry = new org.luwrain.registry.fsdir.RegistryImpl(new File(new File(this.userDataDir, "registry"), getRegVersion()).toPath());
	}
	this.classLoader = this.getClass().getClassLoader();
    }

    @Override public void run()
    {
	handleCmdLine();
	try {
	    final UserProfile userProfile = new UserProfile(dataDir, userDataDir, getRegVersion(), lang);
	    userProfile.userProfileReady();
	    if (!standalone)
		userProfile.registryDirReady();
	    init();
	    new Core(cmdLine, classLoader, registry, os, interaction, props, lang, this.standalone).run();
	    interaction.close();
	    info("exiting LUWRAIN normally");
	    System.exit(0);
	}
	catch(Throwable e)
	{
	    error(e, "top level exception");
	    fatal("terminating LUWRAIN abnormally");
	    System.exit(1);
	}
    }

    private void init()
    {
	//time zone
	{
	    final Settings.DateTime sett = Settings.createDateTime(registry);
	    final String value = sett.getTimeZone("");
	    if (!value.trim().isEmpty())
	    {
		final TimeZone timeZone = TimeZone.getTimeZone(value.trim());
		if (timeZone != null)
		{
		    TimeZone.setDefault(timeZone);
		} else
		    warn("time zone " + value.trim() + " is unknown");
	    }
	}
	initOs();

	//Interaction
	final InteractionParamsLoader interactionParams = new InteractionParamsLoader();
	interactionParams.loadFromRegistry(registry);
	final String interactionClass = props.getProperty("luwrain.class.interaction");
	if (interactionClass.isEmpty())
	{
	    fatal("unable to load the interaction:no luwrain.class.interaction property in loaded properties");
	    System.exit(1);
	}
	interaction = (Interaction)org.luwrain.util.ClassUtils.newInstanceOf(this.classLoader, interactionClass, Interaction.class);
	if (interaction == null)
	{
	    fatal("Unable to create an instance of  the interaction class " + interactionClass);
	    System.exit(1);
	}
	if (!interaction.init(interactionParams,os))
	{
	    fatal("interaction initialization failed");
	    System.exit(1);
	}

	//Network
	final Settings.Network network = Settings.createNetwork(registry);
	//	System.getProperties().put("socksProxyHost", network.getSocksProxyHost(""));
	//	System.getProperties().put("socksProxyPort", network.getSocksProxyPort(""));
	if (!network.getHttpProxyHost("").isEmpty())
	{
	    System.setProperty("java.net.useSystemProxies", "true");
	    System.setProperty("http.proxyHost", network.getHttpProxyHost(""));
	    System.setProperty("https.proxyHost", network.getHttpProxyHost(""));
	    debug("using system proxy: " + System.getProperty("java.net.useSystemProxies"));
	    debug("HTTP proxy host is " + System.getProperty("http.proxyHost"));
	    debug("HTTPS proxy host is " + System.getProperty("https.proxyHost"));
	}
	if (!network.getHttpProxyPort("").isEmpty())
	{
	    System.setProperty("http.proxyPort", network.getHttpProxyPort(""));
	    System.setProperty("https.proxyPort", network.getHttpProxyPort(""));
	    debug("HTTP proxy port is " + System.getProperty("http.proxyPort"));
	    debug("HTTPS proxy port is " + System.getProperty("https.proxyPort"));
	}
	System.getProperties().put("http.proxyUser", network.getHttpProxyUser(""));
	System.getProperties().put("http.proxyPassword",network.getHttpProxyPassword("") );
    }

    private void initOs()
    {
	final String osClass = props.getProperty("luwrain.class.os");
	if (osClass.isEmpty())
	{
	    fatal("unable to load the operating system interface:no luwrain.class.os property in loaded core properties");
	    System.exit(1);
	}
	os = (OperatingSystem)org.luwrain.util.ClassUtils.newInstanceOf(classLoader, osClass, OperatingSystem.class);
	if (os == null)
	{
	    fatal("unable to create a new instance of the operating system class " + osClass);
	    System.exit(1);
	}
	final InitResult initRes = os.init(props);
	if (initRes == null || !initRes.isOk())
	{
	    if (initRes != null)
		fatal("unable to initialize operating system with " + os.getClass().getName() + ":" + initRes.toString()); else
		fatal("unable to initialize operating system with " + os.getClass().getName());
	    System.exit(1);
	}
    }

    private void handleCmdLine()
    {
	//Help
	if (cmdLine.used(CMDARG_HELP))
	{
	    System.out.println("Valid command line options are:");
	    System.out.println(CMDARG_HELP + " - print this help info and exit");
	    System.out.println(CMDARG_PRINT_LANG + " - print the chosen language and exit");
	    System.out.println(Checks.CMDARG_LANG + " - set the language to use");
	    System.out.println(CMDARG_PRINT_DIRS + " - print the detected values of the system directories and exit");
	    if (!standalone)
	    {
		System.out.println(CMDARG_CREATE_PROFILE + " - generate the user profile directory in its default location and exit");
		System.out.println(CMDARG_CREATE_PROFILE_IN + "<DESTDIR> - generate the user profile directory in <DESTDIR> and exit");
	    }
	    System.exit(0);
	}
	//Print the lang
	if (cmdLine.used(CMDARG_PRINT_LANG))
	{
	    System.out.println("Chosen language: " + lang);
	    System.exit(0);
	}
	//Print the dirs
	if (cmdLine.used(CMDARG_PRINT_DIRS))
	{
	    System.out.println("Data: " + dataDir.getAbsolutePath());
	    System.out.println("User profile: " + userDataDir.getAbsolutePath());
	    System.out.println("User home: " + userHomeDir.getAbsolutePath());
	    System.exit(0);
	}
	//Create profile in
	if (!standalone && cmdLine.getArgs(CMDARG_CREATE_PROFILE_IN).length > 0)
	{
	    final String[] destDirs = cmdLine.getArgs((CMDARG_CREATE_PROFILE_IN));
	    try {
		for(String d: destDirs)
		{
		    final File destDir = new File(d);
		    System.out.println("Creating user profile in " + destDir.getAbsolutePath());
		    final UserProfile profile = new UserProfile(dataDir, destDir, getRegVersion(), lang);
		    profile.userProfileReady();
		    profile.registryDirReady();
		}
		System.exit(0);
	    }
	    catch(IOException e)
	    {
		System.err.println("ERROR: " + e.getClass().getName() + ":" + e.getMessage());
		System.exit(1);
	    }
	}
	//create profile
	if (!standalone && cmdLine.used(CMDARG_CREATE_PROFILE))
	{
	    try {
		System.out.println("Creating user profile in " + userDataDir.getAbsolutePath());
		final UserProfile profile = new UserProfile(dataDir, userDataDir, getRegVersion(), lang);
		profile.userProfileReady();
		profile.registryDirReady();
		System.exit(0);
	    }
	    catch(IOException e)
	    {
		System.err.println("ERROR: " + e.getClass().getName() + ":" + e.getMessage());
		System.exit(1);
	    }
	}
    }

    private void initLog4j()
    {
	/*
	try {
	    final Properties props = new Properties();
	    try (final InputStream is = getClass().getResourceAsStream("log4j.properties")) {
		props.load(is);
	    }
	    org.apache.log4j.PropertyConfigurator.configure(props);
	}
	catch(IOException e)
	{
	    error("e, unable to init log4j");
	}
	*/
    }

    private String getRegVersion()
    {
	final String res = props.getProperty("luwrain.registry.version");
	if (res == null || res.trim().isEmpty())
	    return "default";
	return res.toLowerCase();
    }

    private Registry loadMemRegistry(File dataDir, String lang)
    {
	notNull(dataDir, "dataDir");
	notEmpty(lang, "lang");
	notNull(dataDir, "dataDir");
	final org.luwrain.registry.mem.RegistryImpl reg = new org.luwrain.registry.mem.RegistryImpl();
	try {
	    reg.load(new File(dataDir, "registry.dat"));
	    reg.load(new File(dataDir, "registry." + lang + ".dat"));
	    return reg;
	}
	catch(IOException e)
	{
	    error(e, "unable to load initial registry data");
	    System.exit(1);
	    return null;
	}
    }
}
