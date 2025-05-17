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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.*;
import java.nio.file.*;

import org.luwrain.core.events.*;
import org.luwrain.core.queries.*;
import org.luwrain.i18n.*;
import org.luwrain.script.core.MapScriptObject;

import static org.luwrain.core.Base.*;
import static org.luwrain.core.NullCheck.*;
import static org.luwrain.script.Hooks.*;

final class LuwrainImpl implements Luwrain
{
    private final Core core;
    LuwrainImpl(Core core)
    {
	notNull(core, "core");
	this.core = core;
    }

    @Override public CmdLine getCmdLine()
    {
	core.mainCoreThreadOnly();
	return core.cmdLine;
    }

    @Override public String getActiveAreaText(AreaTextType type, boolean issueErrorMessages)
    {
	notNull(type, "type");
	core.mainCoreThreadOnly();
	final Area activeArea = core.getActiveArea(issueErrorMessages);
	if (activeArea == null)
	    return null;
	return new AreaText(activeArea).get(type);
    }

    @Override public String getActiveAreaAttr(AreaAttr attr)
    {
	NullCheck.notNull(attr, "attr");
	core.mainCoreThreadOnly();
	final Area area = core.tiles.getActiveArea();
	if (area == null)
	    return attr == AreaAttr.DIRECTORY?getFileProperty("luwrain.dir.userhome").getAbsolutePath():null;
	switch(attr)
	{
	case DIRECTORY:
	    {
		final CurrentDirQuery query = new CurrentDirQuery();
		if (!AreaQuery.ask(area, query))//FIXME:Security wrapper
		    return getFileProperty("luwrain.dir.userhome").toString();
		return query.getAnswer();
	    }
	case UNIREF:
	    {
		final UniRefAreaQuery query = new UniRefAreaQuery();
		if (!AreaQuery.ask(area, query))
		    return null;
		return query.getAnswer();
	    }
	case UNIREF_UNDER_HOT_POINT:
	    {
		final UniRefHotPointQuery query = new UniRefHotPointQuery();
		if (!AreaQuery.ask(area, query))
		    return null;
		return query.getAnswer();
	    }
	case URL:
	    {
		final UrlAreaQuery query = new UrlAreaQuery();
		if (AreaQuery.ask(area, query))
		    return query.getAnswer();
		final UniRefAreaQuery query2 = new UniRefAreaQuery();
		if (!AreaQuery.ask(area, query2))
		    return null;
		return extractUrl(query2.getAnswer());
	    }
	case URL_UNDER_HOT_POINT:
	    {
		final UrlHotPointQuery query = new UrlHotPointQuery();
		if (AreaQuery.ask(area, query))
		    return query.getAnswer();
		final UniRefHotPointQuery query2 = new UniRefHotPointQuery();
		if (!AreaQuery.ask(area, query2))
		    return null;
		return extractUrl(query2.getAnswer());
	    }
	default:
	    return null;
	}
    }

    @Override public boolean announcement(String text, AnnouncementType type, String component, String category, Map<String, String> args)
    {
	notNull(text, "text");
	notNull(type, "type");
	notEmpty(component, "component");
	notEmpty(category, "category");
	if (text.trim().isEmpty())
	    return true;
	final var a = args != null?new HashMap<String, Object>(args):new HashMap<String, Object>();
	final var res = new AtomicBoolean(false);
	runUiSafely(()->res.set(chainOfResponsibilityNoExc(this, ANNOUNCEMENT, new Object[]{
			new MapScriptObject().add("type", type.toString()).add("text", text).add("component", component).add("category", category).add("args", new MapScriptObject(a))
		    })));
	return res.get();
	    }

    @Override public void sendBroadcastEvent(SystemEvent e)
    {
	NullCheck.notNull(e, "e");
	core.enqueueEvent(e);
    }

    @Override public void sendInputEvent(InputEvent e)
    {
	NullCheck.notNull(e, "e");
	core.enqueueEvent(e);
    }

    @Override public boolean xQuit()
    {
	core.mainCoreThreadOnly();
	core.quit();
	return true;
    }

    @Override public Path getAppDataDir(String appName)
    {
	NullCheck.notEmpty(appName, "appName");
	if (appName.indexOf("/") >= 0)
	    throw new IllegalArgumentException("appName contains illegal characters");
	final Path res = getFileProperty("luwrain.dir.appdata").toPath().resolve(appName);
	try {
	    Files.createDirectories(res);
	    return res;
	}
	catch(IOException e)
	{
	    error("unable to prepare application data directory:" + res.toString() + ":" + e.getClass().getName() + ":" + e.getMessage());
	    return null;
	}
    }

    @Override public     void closeApp()
    {
	core.mainCoreThreadOnly();
	core.closeApp(this);
    }

    @Override public Registry getRegistry()
    {
	return core.registry();
    }

    @Override public I18n i18n()
    {
	return core.i18n;
    }

    @Override public void crash(org.luwrain.app.crash.App app)
    {
	NullCheck.notNull(app, "app");
	runUiSafely(()->core.launchAppCrash(app));
    }

        @Override public void crash(Throwable e)
    {
	NullCheck.notNull(e, "e");
	e.printStackTrace();
	final Luwrain instance = this;
	runUiSafely(()->core.launchAppCrash(this, e));
    }

    @Override public void launchApp(String shortcutName)
    {
	NullCheck.notNull(shortcutName, "shortcutName");
	runUiSafely(()->core.launchApp(shortcutName, new String[0]));
    }

    @Override public void launchApp(String shortcutName, String[] args)
    {
	NullCheck.notNull(shortcutName, "shortcutName");
	NullCheck.notNullItems(args, "args");
	runUiSafely(()->core.launchApp(shortcutName, args != null?args:new String[0]));
    }

    @Override public void message(String text)
    {
	NullCheck.notNull(text, "text");
	if (text.trim().isEmpty())
	    return;
	runUiSafely(()->{
		core.braille.textToSpeak(text);
		core.message(text, MessageType.REGULAR);
	    });
    }

    @Override public void message(String text, MessageType messageType)
    {
	NullCheck.notNull(text, "text");
	NullCheck.notNull(messageType, "messageType");
	if (text.trim().isEmpty())
	    return;
	runUiSafely(()->{
		core.braille.textToSpeak(text);
		core.message(text, messageType);
	    });
    }

    @Override public void message(String text, Sounds sound)
    {
	NullCheck.notNull(text, "text");
	NullCheck.notNull(sound, "sound");
	if (text.trim().isEmpty())
	    return;
	runUiSafely(()->{
		core.braille.textToSpeak(text);
		core.message(text, sound);
	    });
    }

    @Override public Object newExtObject(String  name)
						{
						notEmpty(name, "name");
						final var factories = core.extensions.getLoadedExtObjects(ObjFactory.class);
						for(ObjFactory f: factories)
						{
						final Object res = f.newObject(name);
						if (res != null)
						return res;
						}
						return null;
						}

        @Override public FileFetcher[] findFetchers(String url)
    {
	notEmpty(url, "url");
	final var fetchers = new ArrayList<FileFetcher>(core.extensions.getLoadedExtObjects(FileFetcher.class));
	fetchers.removeIf(f -> (!f.canHandleUrl(url)));
	return fetchers.toArray(new FileFetcher[fetchers.size()]);
    }

    @Override public void onAreaNewHotPoint(Area area)
    {
	notNull(area, "area");
	core.mainCoreThreadOnly();
	final Area frontArea = core.getFrontAreaFor(this, area);
	if (frontArea == null)//Area isn't known by the applications manager, generally admissible situation
	    return;
	if (frontArea == core.tiles.getActiveArea())
	    core.windowManager.redrawArea(frontArea);
    }

    @Override public void onAreaNewContent(Area area)
    {
	notNull(area, "area");
	core.mainCoreThreadOnly();
		final Area frontArea = core.getFrontAreaFor(this, area);
	if (frontArea == null)//The area isn't known by the appp manager
	    return;
	core.windowManager.redrawArea(frontArea);
	    }

    @Override public void onAreaNewName(Area area)
    {
	notNull(area, "area");
	core.mainCoreThreadOnly();
		final Area frontArea = core.getFrontAreaFor(this, area);
	if (frontArea == null)//Area isn't known by the applications manager, generally admissible situation
	    return;
	core.windowManager.redrawArea(frontArea);
    }

    @Override public void onAreaNewBackgroundSound(Area area)
    {
	notNull(area, "area");
	core.mainCoreThreadOnly();
	final Area frontArea = core.getFrontAreaFor(this, area);
	if (frontArea == null)//The area isn't known by the app manager
	    return;
	core.updateBackgroundSound(frontArea);
    }

    @Override public int getAreaVisibleHeight(Area area)
    {
	NullCheck.notNull(area, "area");
	core.mainCoreThreadOnly();
	return core.getAreaVisibleHeightIface(this, area);
    }

    @Override public int getAreaVisibleWidth(Area area)
    {
	NullCheck.notNull(area, "area");
	core.mainCoreThreadOnly();
	return core.getAreaVisibleWidthIface(this, area);
    }

    @Override public int getScreenWidth()
    {
	core.mainCoreThreadOnly();
	return core.getScreenWidthIface();
    }

    @Override public int getScreenHeight()
    {
	core.mainCoreThreadOnly();
	return core.getScreenHeightIface();
    }

    @Override public void announceActiveArea()
    {
	core.mainCoreThreadOnly();
	core.announceActiveAreaIface();
    }

    @Override public Clipboard getClipboard()
    {
	return core.getClipboard();
    }

    @Override public void onNewAreaLayout()
    {
	core.mainCoreThreadOnly();
	core.onNewAreaLayout(this);
    }

    @Override public void openFile(String fileName)
    {
	NullCheck.notNull(fileName, "fileName");
	runUiSafely(()->{
		String[] s = new String[1];
		s[0] = fileName;
		core.openFiles(s);
	    });
    }

    @Override public void openFiles(String[] fileNames)
    {
	NullCheck.notNullItems(fileNames, "fileNames");
	runUiSafely(()->core.openFiles(fileNames));
    }

    @Override public boolean openHelp(String sectName)
    {
	NullCheck.notEmpty(sectName, "sectName");
	core.mainCoreThreadOnly();
	final String url = core.helpSects.getSectionUrl(sectName);
	if (url.isEmpty())
	    return false;
	launchApp("reader", new String[]{url});
	return true;
    }


    @Override public String suggestContentType(java.net.URL url, ContentTypes.ExpectedType expectedType)
    {
	NullCheck.notNull(url, "url");
	NullCheck.notNull(expectedType, "expectedType");
	return core.contentTypes.suggestContentType(url, expectedType);
    }

    @Override public String suggestContentType(java.io.File file, ContentTypes.ExpectedType expectedType)
    {
	NullCheck.notNull(file, "file");
	NullCheck.notNull(expectedType, "expectedType");
	return core.contentTypes.suggestContentType(file, expectedType);
    }

    //sound can be null, means to stop a playback
    @Override public void playSound(Sounds sound)
    {
	runUiSafely(()->core.playSound(sound));
    }

        @Override public void playSound(File file)
    {
	NullCheck.notNull(file, "file");
	runUiSafely(()->core.playSound(file));
    }


    @Override public void popup(Popup popup)
    {
	notNull(popup, "popup");
	core.mainCoreThreadOnly();
	final Luwrain luwrainObj = popup.getLuwrainObject();
	if (luwrainObj == null)
	    throw new IllegalArgumentException("The popup doesn't contain any Luwrain object");
	final StopCondition stopCondition = ()->popup.isPopupActive();
		if (core.interfaces.forPopupsWithoutApp(luwrainObj))
	{
	    core.popup(null, popup, Popup.Position.BOTTOM, stopCondition, popup.getPopupFlags());
	    return;
	}
	final Application app = core.interfaces.findApp(luwrainObj);
	if (app == null)
	{
	    warn("trying to open a popup with the Luwrain obj which isn't allowed to open popups");
	    throw new IllegalArgumentException("the luwrain object provided by the popup isn't allowed to open popups");
	}
	core.popup(app, popup, Popup.Position.BOTTOM, stopCondition, popup.getPopupFlags());
    }

    @Override public boolean runCommand(String command)
    {
	NullCheck.notNull(command, "command");
	core.mainCoreThreadOnly();
	return core.runCommand(command);
    }

    @Override public Job newJob(String name, String[] args, String dir, Set<JobFlags> flags, Job.Listener listener)
    {
	notNull(name, "name");
	notNullItems(args, "args");
	notNull(dir, "dir");
	notNull(flags, "flags");
	core.mainCoreThreadOnly();
	return core.jobs.run(name, args, dir, listener != null?listener:new EmptyJobListener());
    }

    @Override public void speak(String text)
    {
	NullCheck.notNull(text, "text");
	runUiSafely(()->{
		core.braille.textToSpeak(text);
		core.speech.speak(core.speakingText.processRegular(text), 0, 0);
	    });
    }

    @Override public void speak(String text, Sounds sound)
    {
	NullCheck.notNull(text, "text");
	NullCheck.notNull(sound, "sound");
	runUiSafely(()->{
		playSound(sound);
		speak(text);
	    });
    }

    @Override public void speak(String text, int pitch)
    {
	NullCheck.notNull(text, "text");
	runUiSafely(()->{
		core.braille.textToSpeak(text);
		core.speech.speak(core.speakingText.processRegular(text), pitch, 0);
	    });
    }

    @Override public void speak(String text, int pitch, int rate)
    {
	NullCheck.notNull(text, "text");
	runUiSafely(()->core.speech.speak(core.speakingText.processRegular(text), pitch, rate));
    }

    @Override public void speakLetter(char letter)
    {
	core.braille.textToSpeak("" + letter);
	runUiSafely(()->{
		switch(letter)
		{
		case ' ':
		    sayHint(Hint.SPACE);
		    return;
		case '\t':
		    sayHint(Hint.TAB);
		    return;
		}
		final String value = i18n().hasSpecialNameOfChar(letter);
		if (value == null)
		    core.speech.speakLetter(letter, 0, 0); else
		    speak(value, Speech.PITCH_HINT);//FIXME:
	    });
    }

    @Override public void speakLetter(char letter, int pitch)
    {
	runUiSafely(()->{
		switch(letter)
		{
		case ' ':
		    sayHint(Hint.SPACE);
		    return;
		case '\t':
		    sayHint(Hint.TAB);
		    return;
		}
		final String value = i18n().hasSpecialNameOfChar(letter);
		if (value == null)
		    core.speech.speakLetter(letter, pitch, 0); else
		    speak(value, Speech.PITCH_HINT);
	    });
    }

    @Override public void speakLetter(char letter,
				      int pitch, int rate)
    {
	runUiSafely(()->{
		switch(letter)
		{
		case ' ':
		    sayHint(Hint.SPACE);
		    return;
		case '\t':
		    sayHint(Hint.TAB);
		    return;
		}
		final String value = i18n().hasSpecialNameOfChar(letter);
		if (value == null)
		    core.speech.speakLetter(letter, pitch, rate); else
		    speak(value, Speech.PITCH_HINT);
	    });
    }

    @Override public void silence()
    {
	runUiSafely(()->core.speech.silence());
    }

    @Override public void setActiveArea(Area area)
    {
	notNull(area, "area");
	core.mainCoreThreadOnly();
	final Application app = core.interfaces.findApp(this);
	if (app == null)
	    throw new IllegalArgumentException("Trying to use an illegal Luwrain obj");
	core.apps.setActiveAreaForApp(app, area);
	if (core.apps.isActiveApp(app) && !core.tiles.isPopupActive())
	    core.setAreaIntroduction();
	core.onNewAreasLayout();
    }

    @Override public String staticStr(LangStatic id)
    {
	NullCheck.notNull(id, "id");
	core.mainCoreThreadOnly();
	return i18n().staticStr(id);
    }

    @Override public UniRefInfo getUniRefInfo(String uniRef)
    {
	NullCheck.notNull(uniRef, "uniRef");
	return core.uniRefProcs.getInfo(uniRef);
    }

    @Override public boolean openUniRef(String uniRef)
    {
	notNull(uniRef, "uniRef");
	core.mainCoreThreadOnly();
	return core.uniRefProcs.open(uniRef);
    }

    @Override public boolean openUniRef(UniRefInfo uniRefInfo)
    {
	NullCheck.notNull(uniRefInfo, "uniRefInfo");
	core.mainCoreThreadOnly();
	return core.uniRefProcs.open(uniRefInfo.getValue());
    }

    @Override public boolean openUrl(String url)
    {
	notEmpty(url, "url");
	final var res = new AtomicBoolean(false);
	runUiSafely(()->res.set(chainOfResponsibilityNoExc(this, URL_OPEN, new Object[]{url})));
	return res.get();
    }

    @Override public void runUiSafely(Runnable runnable)
    {
	notNull(runnable, "runnable");
	if (!core.isMainCoreThread())
	    runInMainThread(runnable); else
	    runnable.run();
    }

    @Override public Object runLaterSync(Callable callable)
    {
	NullCheck.notNull(callable, "callable");
	final Core.CallableEvent event = new Core.CallableEvent(callable);
	core.enqueueEvent(event);
	try {
	    event.waitForBeProcessed();
	}
	catch(InterruptedException e)
	{
	    Thread.currentThread().interrupt();
	    return null;
	}
	return event.getResult();
    }

    @Override public Object callUiSafely(Callable callable)
    {
	NullCheck.notNull(callable, "callable");
	if (core.isMainCoreThread())
	{
	    try {
		return callable.call(); 
	    }
	    catch(Throwable e)
	    {
		error(e, "exception on processing of CallableEvent");
		return null;
	    }
	} else
	    return runLaterSync(callable);
    }

    @Override public int xGetSpeechRate()
    {
	core.mainCoreThreadOnly();
	return  core.speech.getRate();
    }

    @Override public void xSetSpeechRate(int value)
    {
	core.mainCoreThreadOnly();
	core.speech.setRate(value);
    }

    @Override public int xGetSpeechPitch()
    {
	core.mainCoreThreadOnly();
	return core.speech.getPitch();
    }

    @Override public void xSetSpeechPitch(int value)
    {
	core.mainCoreThreadOnly();
	core.speech.setPitch(value);
    }

    @Override public String[] getAllShortcutNames()
    {
	core.mainCoreThreadOnly();
	return core.objRegistry.getShortcutNames();
    }

    @Override public java.io.File getFileProperty(String propName)
    {
	NullCheck.notEmpty(propName, "propName");
	//FIXME:	core.mainCoreThreadOnly();
	return core.props.getFileProperty(propName);
    }

    /*
    @Override public OsCommand runOsCommand(String cmd, String dir,
					    OsCommand.Output output, OsCommand.Listener listener)
    {
	NullCheck.notEmpty(cmd, "cmd");
	NullCheck.notNull(dir, "dir");
	core.mainCoreThreadOnly();
	return core.os.runOsCommand(cmd, (!dir.isEmpty())?dir:getFileProperty("luwrain.dir.userhome").getAbsolutePath(), output, listener);
    }
    */

    @Override public String getProperty(String propName)
    {
	NullCheck.notEmpty(propName, "propName");
	//FIXME:	core.mainCoreThreadOnly();
	return core.props.getProperty(propName);
    }

    @Override public void setEventResponse(EventResponse eventResponse)
    {
	NullCheck.notNull(eventResponse, "eventResponse");
	core.mainCoreThreadOnly();
	core.setEventResponse(eventResponse);
    }

    @Override public org.luwrain.player.Player getPlayer()
    {
	return core.player;
    }

    @Override public MediaResourcePlayer[] getMediaResourcePlayers()
    {
	core.mainCoreThreadOnly();
	final List<MediaResourcePlayer> res = new ArrayList<>();
	res.add(core.wavePlayer);
	for(MediaResourcePlayer p: core.objRegistry.getMediaResourcePlayers())
	    res.add(p);
	return res.toArray(new MediaResourcePlayer[res.size()]);
    }

    @Override public String[] xGetLoadedSpeechFactories()
    {
	core.mainCoreThreadOnly();
	return new String[0];
    }

    @Override public boolean runWorker(String workerName)
    {
	NullCheck.notEmpty(workerName, "workerName");
	core.mainCoreThreadOnly();
	return core.workers.runExplicitly(workerName);
    }

    @Override public void executeBkg(Runnable runnable)
    {
	NullCheck.notNull(runnable, "runnable");
	//FIXME:maintaining the registry of executed tasks with their associations to Luwrain objects
	new Thread(()->{
		try {
		    runnable.run();
		}
		catch(Throwable e)
		{
		    error(e, "exception in a background thread");
		    crash(e);
		}
	}).start();
    }

    @Override public boolean registerExtObj(ExtensionObject extObj)
    {
	NullCheck.notNull(extObj, "extObj");
	core.mainCoreThreadOnly();
	if (this != core.luwrain)
	    throw new RuntimeException("registerExtObj() may be called only for privileged interfaces");
	return core.objRegistry.add(null, extObj);
    }

    @Override public org.luwrain.speech.Channel loadSpeechChannel(String engineName, String params) throws org.luwrain.speech.SpeechException
    {
	NullCheck.notEmpty(engineName, "engineName");
	NullCheck.notNull(params, "params");
	return core.speech.loadChannel(engineName, params);
    }

    @Override public boolean unloadDynamicExtension(String extId)
    {
	NullCheck.notEmpty(extId, "extId");
	core.mainCoreThreadOnly();
	return core.unloadDynamicExtension(extId);
    }

        @Override public String getSpeakableText(String text, SpeakableTextType type)
    {
	NullCheck.notNull(text, "text");
	NullCheck.notNull(type, "type");
	return core.i18n.getSpeakableText(text, type);
    }

        @Override public boolean runHooks(String hookName, HookRunner runner)
    {
	NullCheck.notEmpty(hookName, "hookName");
	NullCheck.notNull(runner, "runner");
	core.extensions.runHooks(hookName, runner);
	return true;
    }

    @Override public void showGraphical(Interaction.GraphicalMode graphicalMode)
    {
	NullCheck.notNull(graphicalMode, "graphicalMode");
	core.interaction.showGraphical(graphicalMode);
    }

    @Override public     ScriptFile[] getScriptFilesList(String componentName)
    {
	notEmpty(componentName, "componentName");
	final List<ScriptFile> res = core.extensions.getScriptFiles(componentName);
	return res.toArray(new ScriptFile[res.size()]);
    }

        @Override public String loadScript(ScriptSource scriptSource) throws ExtensionException
    {
	notNull(scriptSource, "scriptSource");
	return core.loadScript(scriptSource);
    }

    @Override public String escapeString(String style, String text)
    {
	notNull(style, "style");
	NullCheck.notNull(text, "text");
	return core.os.escapeString(style, text);
    }

@Override public java.io.File createTempFile(String prefix)
    {
	notNull(prefix, "prefix");
	return core.tempFiles.createTempFile(!prefix.trim().isEmpty()?prefix.trim():"unknown");
    }

    private void sayHint(Hint hint)
    {
	NullCheck.notNull(hint, "hint");
	final LangStatic staticStrId = EventResponses.hintToStaticStrMap(hint);
	if (staticStrId == null)
	    return;
	speak(i18n().staticStr(staticStrId), Speech.PITCH_HINT);
    }

    private void runInMainThread(Runnable runnable)
    {
	notNull(runnable, "runnable");
	core.enqueueEvent(new Core.RunnableEvent(()->{
		    try {
			runnable.run();
		    }
		    catch(Throwable e)
		    {
			crash(e);
		    }
	}));
    }

    static private String extractUrl(String uniRef)
    {
	NullCheck.notNull(uniRef, "uniRef");
	if (!uniRef.startsWith("url:"))
	    return null;
	return uniRef.substring("url:".length());
    }
}
