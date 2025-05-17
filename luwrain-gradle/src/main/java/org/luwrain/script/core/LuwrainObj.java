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

package org.luwrain.script.core;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.*;

import org.luwrain.core.*;
import org.luwrain.script.*;
import org.luwrain.util.*;

import static org.luwrain.script.ScriptUtils.*;
import static org.luwrain.core.NullCheck.*;

public final class LuwrainObj extends LuwrainObjBase
{
    @HostAccess.Export public final LogObj log;
    @HostAccess.Export public final ConstObj constants = new ConstObj();
    @HostAccess.Export public final PopupsObj popups;

    final Object syncObj;
    final Map<String, List<Value> > hooks = new HashMap<>();
    final List<ExtensionObject> extObjs = new ArrayList<>();
    final I18nObj i18nObj;
    final List<Command> commands = new ArrayList<>();

    LuwrainObj(Luwrain luwrain, Object syncObj, Module module)
    {
	super(module, luwrain);
	notNull(luwrain, "luwrain");
	notNull(syncObj, "syncObj");
	notNull(module, "module");
	this.syncObj = syncObj;
	//	this.module = module;
	this.log = new LogObj(luwrain);
	this.i18nObj = new I18nObj(luwrain);
	this.popups = new PopupsObj(luwrain);
    }

        @HostAccess.Export public final ProxyExecutable escapeString = this::escapeStringImpl;
            private Object escapeStringImpl(Value[] args)
    {
	if (!notNullAndLen(args, 2))
	    return false;
	final String
	style = ScriptUtils.asString(args[0]),
	text = ScriptUtils.asString(args[1]);
	if (style == null || text == null)
	    return false;
	return luwrain.escapeString(style, text);
    }

            @HostAccess.Export public final ProxyExecutable deleteFile = this::deleteFileImpl;
            private Object deleteFileImpl(Value[] args)
    {
	if (!notNullAndLen(args, 1))
	    return false;
	final String fileName = ScriptUtils.asString(args[0]);
	if (fileName == null)
	    return false;
	new File(fileName).delete();
	return true;
	    }



    @HostAccess.Export public final ProxyExecutable addCommand = this::addCommandImpl;
            private Object addCommandImpl(Value[] args)
    {
	if (!notNullAndLen(args, 2))
	    return false;
	if (!args[0].isString() || !args[1].canExecute())
	    return false;
	final String name = args[0].asString();
	if (name.trim().isEmpty())
	    return false;
	commands.add(new CommandImpl(module, name.trim(), args[1]));
	return true;
	    }

    @HostAccess.Export public final ProxyExecutable addHook = this::addHookImpl;
    private Object addHookImpl(Value[] args)
    {
	if (!notNullAndLen(args, 2))
	    return false;
	if (!args[0].isString() || !args[1].canExecute())
	    return false;
	final String name = args[0].asString();
	if (name.trim().isEmpty())
	    return false;
	List<Value> h = this.hooks.get(name);
	if (h == null)
	{
	    h = new ArrayList<>();
	    this.hooks.put(name, h);
	}
	h.add(args[1]);
	return true;
    }

    @HostAccess.Export public final ProxyExecutable addShortcut = this::addShortcutImpl;
                private Object addShortcutImpl(Value[] args)
    {
	if (!notNullAndLen(args, 2))
	    return false;
	if (!args[0].isString() || !args[1].canInstantiate())
	    return false;
	final String name = args[0].asString();
	if (name.trim().isEmpty())
	    return false;
	extObjs.add(new ShortcutImpl(module, name.trim(), luwrain.getFileProperty(Luwrain.PROP_DIR_DATA), args[1]));
	return true;
	    }

    @HostAccess.Export public final ProxyExecutable addWorker = this::addWorkerImpl;
        private Object addWorkerImpl(Value[] args)
    {
	if (!notNullAndLen(args, 4))
	    return false;
	if (!args[0].isString() ||
	    !args[1].isNumber() ||
	    !args[2].isNumber() ||
!args[3].canExecute())
	    return false;
	final String name = args[0].asString();
	final int firstLaunchDelay = args[1].asInt();
	final int launchPeriod = args[2].asInt();
	if (name.trim().isEmpty())
	    return false;
	if (firstLaunchDelay == 0 || launchPeriod == 0)
	    return false;
	extObjs.add(new WorkerImpl(this, name.trim(), firstLaunchDelay, launchPeriod, args[3]));
	return true;
    }

    @HostAccess.Export public final ProxyExecutable executeBkg = this::executeBkgImpl;
    private Object executeBkgImpl(Value[] values)
    {
	if (!notNullAndLen(values, 1))
	    return false;
	if (values[0].isNull() || !values[0].canExecute())
	    return false;
	final FutureTask<Object> task = new FutureTask<>(()->{
		synchronized(syncObj) {
		    try {
			values[0].execute(new Object[0]);
		    }
		    catch(Throwable e)
		    {
			luwrain.crash(e);
		    }
		}
		return null;
	    });
	luwrain.executeBkg(task);
	return true;
    }

    @HostAccess.Export public final ProxyExecutable i18n = this::i18nImpl;
    private Object i18nImpl(Value[] args)
    {
	i18nObj.refresh();
	return i18nObj;
    }

    @HostAccess.Export public final ProxyExecutable isDigit = this::isDigitImpl;
    private Object isDigitImpl(Value[] values)
    {
	if (!notNullAndLen(values, 1))
	    return false;
	if (!values[0].isString() || values[0].asString().length() != 1)
	    return false;
	return Character.isDigit(values[0].asString().charAt(0));
    }

    @HostAccess.Export public final ProxyExecutable isLetter = this::isLetterImpl;
            private Object isLetterImpl(Value[] values)
    {
	if (!ScriptUtils.notNullAndLen(values, 1))
	    return false;
	if (!values[0].isString() || values[0].asString().length() != 1)
	    return false;
	return Character.isLetter(values[0].asString().charAt(0));
    }

    @HostAccess.Export public final ProxyExecutable isLetterOrDigit = this::isLetterOrDigitImpl;
            private Object isLetterOrDigitImpl(Value[] values)
    {
	if (!ScriptUtils.notNullAndLen(values, 1))
	    return false;
	if (!values[0].isString() || values[0].asString().length() != 1)
	    return false;
	return Character.isLetterOrDigit(values[0].asString().charAt(0));
    }

    @HostAccess.Export public final ProxyExecutable isSpace = this::isSpaceImpl;
                private Object isSpaceImpl(Value[] values)
    {
	if (!ScriptUtils.notNullAndLen(values, 1))
	    return false;
	if (!values[0].isString() || values[0].asString().length() != 1)
	    return false;
	return Character.isWhitespace(values[0].asString().charAt(0));
    }

    @HostAccess.Export public final ProxyExecutable launchApp = this::launchAppImpl;
    private Object launchAppImpl(Value[] values)
    {
	if (notNullAndLen(values, 2))
	{
	    if (!values[1].hasArrayElements())
		return false;
	    final String[] args = asStringArray(values[1]);
	    if (args == null)
		return false;
	    if (values[0].isString())
	    {
		luwrain.launchApp(values[0].asString(), args);
		return true;
	    }
	    if (!values[0].canInstantiate())
		throw new IllegalArgumentException("The first argument to Luwrian.launchApp() must be a string or an executable object");
	    if (module.internalCoreFuncs == null)
		throw new IllegalArgumentException("This script core can launch apps by their names only");
	    module.internalCoreFuncs.launchApp(new AppImpl(module, values[0]));
	    return true;
	}
	if (!notNullAndLen(values, 1))
	    return false;
	if (values[0].isString())
	{
	    luwrain.launchApp(values[0].asString());
	    return true;
	}
	if (!values[0].canInstantiate())
	    throw new IllegalArgumentException("The first argument to Luwrian.launchApp() must be a string or an executable object");
	if (module.internalCoreFuncs == null)
	    throw new IllegalArgumentException("This script core can launch apps by their names only");
	module.internalCoreFuncs.launchApp(new AppImpl(module, values[0]));
	return true;
    }

    //FIXME: Speak numbers (or anything other than String)
    @HostAccess.Export public final ProxyExecutable message = this::messageImpl;
        private Object messageImpl(Value[] values)
    {
	if (!notNull(values))
	    return false;
	if (values.length < 1 || values.length > 2)
	    return true;
	if (!values[0].isString())
	    return false;
	final Luwrain.MessageType messageType;
	if (values.length == 2)
	{
	    if (!values[1].isString())
		return false;
messageType = ConstObj.getMessageType(values[1].asString());
	} else
	    messageType = null;
	if (messageType != null)
	    	luwrain.message(values[0].asString(), messageType); else
	luwrain.message(values[0].asString());
	return true;
    }

    @HostAccess.Export public final ProxyExecutable newJob = this::newJobImpl;
    private JobInstanceObj newJobImpl(Value[] values)
    {
	if (values.length < 2 || values.length > 4)
	    return null;
	if (values[0] == null || values[0].isNull() || !values[0].isString())
	    return null;
	final String name = values[0].asString();
	final String[] args = asStringArray(values[1]);
	final String dir;
	if (values.length < 3 || values[2] == null || values[2].isNull() || !values[2].isString())
	    dir = ""; else
	    dir = values[2].asString();
	final Value finishedFunc;
	if (values.length < 4 || values[3] == null || values[3].isNull() || !values[3].canExecute())
	    finishedFunc = null; else
	    finishedFunc = values[3];
	final Job res = luwrain.newJob(name, args != null?args:new String[0], dir, EnumSet.noneOf(Luwrain.JobFlags.class), new Job.Listener(){
		@Override public void onInfoChange(Job instance, String type, List<String> value){}
		@Override public void onStatusChange(Job instance)
		{
		    NullCheck.notNull(instance, "instance");
		    if (finishedFunc == null || instance.getStatus() != Job.Status.FINISHED)
			return;
		    synchronized(syncObj) {
			finishedFunc.execute(new Object[]{Boolean.valueOf(instance.isFinishedSuccessfully()), Integer.valueOf(instance.getExitCode())});
		    }
		}
	    });
	return res != null?new JobInstanceObj(res):null;
    }

    @HostAccess.Export public final ProxyExecutable openUrl = this::openUrlImpl;
    private Object openUrlImpl(Value[] args)
    {
	if (!notNullAndLen(args, 1))
	    return false;
	if (!args[0].isString())
	    return false;
	luwrain.runUiSafely(()->{
		luwrain.openUrl(args[0].asString());
	    });
	return true;
    }

    @HostAccess.Export public final ProxyExecutable playSound = this::playSoundImpl;
    private Object playSoundImpl(Value[] values)
    {
	if (!notNullAndLen(values, 1))
	    return false;
	if (!values[0].isString())
	    return false;
	final Sounds sound = ConstObj.getSound(values[0].asString());
	if (sound == null)
	    return false;
	luwrain.playSound(sound);
	return true;
    }

    @HostAccess.Export public final ProxyExecutable readTextFile = this::readTextFileImpl;
    private Object readTextFileImpl(Value[] args)
    {
	if (!notNullAndLen(args, 1))
	    return new ScriptException("readTextFile takes exactly one non-null argument");
	final String fileName = ScriptUtils.asString(args[0]);
	if (fileName == null || fileName.isEmpty())
	    throw new ScriptException("readTextFile() takes a non-empty string with the name of the file as the furst argument");
	try {
	    final String text = FileUtils.readTextFileSingleString(new File(fileName), "UTF-8");
	    return ProxyArray.fromArray((Object[])FileUtils.universalLineSplitting(text));
	}
	catch(IOException e)
	{
	    throw new ScriptException(e);
	}
    }

        @HostAccess.Export public final ProxyExecutable writeTextFile = this::writeTextFileImpl;
    private Object writeTextFileImpl(Value[] args)
    {
	if (!notNullAndLen(args, 2))
	    return new ScriptException("readTextFile takes exactly one non-null argument");
	final String fileName = ScriptUtils.asString(args[0]);
	if (fileName == null || fileName.isEmpty())
	    throw new ScriptException("readTextFile() takes a non-empty string with the name of the file as the furst argument");
	final String[] lines = ScriptUtils.asStringArray(args[1]);
	if (lines == null)
	    throw new IllegalArgumentException("No lines to write");
	try {
	    FileUtils.writeTextFileMultipleStrings(new File(fileName), lines, "UTF-8", null);
	    return true;
	}
	catch(IOException e)
	{
	    throw new ScriptException(e);
	}
    }


    @HostAccess.Export public final ProxyExecutable quit = this::quitImpl;
        private Object quitImpl(Value[] values)
    {
	if (values != null && values.length > 0)
	    return false;
	luwrain.xQuit();
	return true;
	}


    @HostAccess.Export public final ProxyExecutable urlGet = this::urlGetImpl;
    private Object urlGetImpl(Value[] args)
    {
	if (!notNullAndLen(args, 1))
	    return null;
	if (!args[0].isString())
	    return null;
	try {
	    try (final BufferedReader r = new BufferedReader(new InputStreamReader(new URL(args[0].asString()).openStream()))) {
		final StringBuilder b = new StringBuilder();
		String line = r.readLine();
		while (line != null)
		{
		    b.append(line).append(System.lineSeparator());
		    line = r.readLine();
		}
		return new String(b);
	    }
	}
	catch(Throwable e)
	{
	    throw new ScriptException(e);
	}
    }

    @HostAccess.Export public final ProxyExecutable createWizardArea = this::createWizardAreaImpl;
    private Object createWizardAreaImpl(Value[] args)
    {
	final Value onInput;
	if (args != null && args.length != 0 &&
	    args[0] != null && !args[0].isNull())
	{
	    onInput = args[0].getMember("input");
	    if (onInput != null && !onInput.isNull() && !onInput.canExecute())
		throw new IllegalArgumentException("The input member of the first argument to Luwrain.createWizardArea() must be a function");
	} else
	    onInput = null;
	return new org.luwrain.script.controls.WizardAreaObj(new org.luwrain.controls.DefaultControlContext(luwrain), module,
							     (onInput != null && !onInput.isNull())?onInput:null);
    }

    @HostAccess.Export public ProxyExecutable parseXml = this::parseXmlImpl;
    private Object parseXmlImpl(Value[] args)
    {
	if (args == null || args.length < 1 || args.length > 2)
	    throw new IllegalArgumentException("Luwrain.parseXml() takes one or two string arguments");
	if (!args[0].isString())
	    throw new IllegalArgumentException("Luwrain.parseXml() takes a string as the first argument");
	if (args.length == 2 && !args[1].isString())
	    throw new IllegalArgumentException("Luwrain.parseXml() takes a string as the second argument");
	final var p = org.jsoup.parser.Parser.xmlParser();
	final org.jsoup.nodes.Document doc;
	if (args.length == 1)
	    doc = p.parseInput(args[0].asString(), ""); else
	    doc = p.parseInput(args[0].asString(), args[1].asString());
	return doc != null?new org.luwrain.script.ml.JSoupDocObj(doc):null;
    }

    @HostAccess.Export public final ProxyExecutable fetchUrl = AsyncFunction.create(module.context, module.syncObj, (args, res)->{
	    if (!notNullAndLen(args, 1))
		throw new IllegalArgumentException("Luwrain.fetchUrl takes exactly one argument");
	    if (!args[0].isString())
		throw new IllegalArgumentException("Luwrain.fetchUrl() takes a string as its first argument");
	    luwrain.executeBkg(()->{
		    try {
			try (final BufferedReader r = new BufferedReader(new InputStreamReader(new URL(args[0].asString()).openStream()))) {
			    final StringBuilder b = new StringBuilder();
			    String line = r.readLine();
			    while (line != null)
			    {
				b.append(line).append(System.lineSeparator());
				line = r.readLine();
			    }
			    luwrain.runUiSafely(()->res.complete(new String(b)));
			}
		    }
		    catch(Throwable e)
		    {
			Log.error("proba", e.getMessage());
			throw new ScriptException(e);
		    }
		});
	});

    @HostAccess.Export public final ProxyExecutable speak = this::speakImpl;
    private Object speakImpl(Value[] values)
    {
	if (notNullAndLen(values, 2))
		{
		    if (!values[0].isString() || !values[1].isString())
	    return false;
		    final String text = values[0].asString();
		    final String sound = values[1].asString();
		    if (sound.isEmpty())
			return false;
		    final Sounds s = ConstObj.getSound(sound);
		    if (s == null)
			return false;
		    luwrain.playSound(s);
	luwrain.speak(text);
	return true;
		}
	if (!notNullAndLen(values, 1))
	    return false;
	if (!values[0].isString())
	    return false;
	luwrain.speak(values[0].asString());
	return true;
    }
}
