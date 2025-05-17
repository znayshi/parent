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
import org.apache.logging.log4j.*;

import static org.luwrain.core.NullCheck.*;

final class ObjRegistry implements ExtObjects
{
    static private final Logger log = LogManager.getLogger();
    static private final class Entry<E> 
    {
	final Extension ext;
	final String name;
	final E obj;
	Entry(Extension ext, String name, E obj)
	{
	    notEmpty(name, "name");
	    notNull(obj, "obj");
	    this.ext = ext;
	    this.name = name;
	    this.obj = obj;
	}
    }

    private Map<String, Entry<Shortcut>> shortcuts = new HashMap<>();
    private Map<String, Entry<Worker>> workers = new HashMap<>();
    private Map<String, Entry<org.luwrain.speech.Engine>> speechEngines = new HashMap<>();
    private Map<String, Entry<MediaResourcePlayer>> players = new HashMap<>();
        private Map<String, Entry<PropertiesProvider>> propsProviders = new HashMap<>();

    boolean add(Extension ext, ExtensionObject obj)
    {
	//ext can be null
	notNull(obj, "obj");
	final String name = obj.getExtObjName();
	if (name == null || name.trim().isEmpty())
	    return false;
	boolean res = false;

	if (obj instanceof Shortcut)
	{
	    final Shortcut shortcut = (Shortcut)obj;
	    if (!shortcuts.containsKey(name))
	    {
		shortcuts.put(name, new Entry<>(ext, name, shortcut));
		res = true;
	    }
	}

		if (obj instanceof MediaResourcePlayer)
	{
	    final MediaResourcePlayer player = (MediaResourcePlayer)obj;
	    if (!players.containsKey(name))
	    {
		players.put(name, new Entry<>(ext, name, player));
		res = true;
	    }
	}

				if (obj instanceof org.luwrain.speech.Engine)
	{
	    final org.luwrain.speech.Engine engine = (org.luwrain.speech.Engine)obj;
	    if (!speechEngines.containsKey(name))
	    {
		speechEngines.put(name, new Entry<>(ext, name, engine));
		res = true;
	    }
	}

								if (obj instanceof Worker)
	{
	    final Worker worker = (Worker)obj;
	    if (!workers.containsKey(name))
	    {
		workers.put(name, new Entry<>(ext, name, worker));
		res = true;
	    }
	}

																if (obj instanceof PropertiesProvider)
	{
	    final PropertiesProvider provider = (PropertiesProvider)obj;
	    if (!propsProviders.containsKey(name))
	    {
		propsProviders.put(name, new Entry<>(ext, name, provider));
		res = true;
	    }
	}

																if (!res)
	    log.warn("Failed to add an extension object of class " + obj.getClass().getName() + " with name \'" + name + "\'");
	return res;
    }

    void deleteByExt(Extension ext)
    {
	notNull(ext, "ext");
	removeEntriesByExt(shortcuts, ext);
	removeEntriesByExt(workers, ext);
	removeEntriesByExt(speechEngines, ext);
	removeEntriesByExt(players, ext);
    }

    Shortcut getShortcut(String name)
    {
	NullCheck.notEmpty(name, "name");
	if (!shortcuts.containsKey(name))
	    return null;
	return shortcuts.get(name).obj;
    }

    String[] getShortcutNames()
    {
	final List<String> res = new ArrayList<>();
	for(Map.Entry<String, Entry<Shortcut>> e: shortcuts.entrySet())
	    res.add(e.getKey());
	final String[] str = res.toArray(new String[res.size()]);
	Arrays.sort(str);
	return str;
    }

    @Override public MediaResourcePlayer[] getMediaResourcePlayers()
    {
	final List<MediaResourcePlayer> res = new ArrayList<>();
	for(Map.Entry<String, Entry<MediaResourcePlayer>> e: players.entrySet())
	    res.add(e.getValue().obj);
	return res.toArray(new MediaResourcePlayer[res.size()]);
    }

    boolean hasSpeechEngine(String name)
    {
	NullCheck.notEmpty(name, "name");
	return speechEngines.containsKey(name);
    }

        org.luwrain.speech.Engine[] getSpeechEngines()
    {
	final List<org.luwrain.speech.Engine> res = new ArrayList<>();
	for(Map.Entry<String, Entry<org.luwrain.speech.Engine>> e: speechEngines.entrySet())
	    res.add(e.getValue().obj);
	return res.toArray(new org.luwrain.speech.Engine[res.size()]);
    }

            Worker[] getWorkers()
    {
	final List<Worker> res = new ArrayList<>();
	for(Map.Entry<String, Entry<Worker>> e: workers.entrySet())
	    res.add(e.getValue().obj);
	return res.toArray(new Worker[res.size()]);
    }

                PropertiesProvider[] getPropertiesProviders()
    {
	final List<PropertiesProvider> res = new ArrayList<>();
	for(Map.Entry<String, Entry<PropertiesProvider>> e: propsProviders.entrySet())
	    res.add(e.getValue().obj);
	return res.toArray(new PropertiesProvider[res.size()]);
    }

    /*
                    TextEditingExtension[] getTextEditingExtensions()
    {
	final List<TextEditingExtension> res = new ArrayList();
	for(Map.Entry<String, Entry<TextEditingExtension>> e: textEditingExts.entrySet())
	    res.add(e.getValue().obj);
	return res.toArray(new TextEditingExtension[res.size()]);
    }
    */


    static void issueResultingMessage(Luwrain luwrain, int exitCode, String[] lines)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNullItems(lines, "lines");
	final StringBuilder b = new StringBuilder();
	if (lines.length >= 1)
	{
	    b.append(lines[0]);
	    for(int i = 1;i < lines.length;++i)
		b.append(" " + lines[i]);
	}
	final String text = new String(b).trim();
	if (!text.isEmpty())
	    luwrain.message(text, exitCode == 0?Luwrain.MessageType.DONE:Luwrain.MessageType.ERROR); else
	    if (exitCode == 0)
		luwrain.message(luwrain.i18n().getStaticStr("OsCommandFinishedSuccessfully"), Luwrain.MessageType.DONE); else
		luwrain.message(luwrain.i18n().getStaticStr("OsCommandFailed"), Luwrain.MessageType.ERROR);
    }

    //    void takeObjects(Extension ext, Luwrain luwrain)
    void takeObjects(Extension ext, ExtensionObject[] extObjects)
    {
	//	notNull(ext, "ext");
	//	notNull(luwrain, "luwrain");
	notNullItems(extObjects, "extObjects");
	//	for(ExtensionObject s: ext.getExtObjects(luwrain))
	for(final ExtensionObject obj: extObjects)
			if (!add(ext, obj))
			    log.warn("The extension object \'" + obj.getExtObjName() + "\' of the extension " + ext.getClass().getName() + " has been refused by  the object registry");
    }

    static private <E> void removeEntriesByExt(Map<String, Entry<E>> map, Extension ext)
    {
	NullCheck.notNull(map, "map");
	NullCheck.notNull(ext, "ext");
	//	final Map<String, org.luwrain.core.ObjRegistry.Entry> entryMap = (Map<String, Entry>)map;
	final List<String> deleting = new ArrayList<>();
	for(Map.Entry<String, org.luwrain.core.ObjRegistry.Entry<E>> e: map.entrySet())
	if (e.getValue().ext == ext)
	    deleting.add(e.getKey());
    for(String s: deleting)
	map.remove(s);
    }

    /*
    static private final class JobCommand implements Command
    {
	private final String name;
	private final JobLauncher job;
	private final boolean showResultMessage;

	JobCommand(String name, Job job, boolean showResultMessage)
	{
	    NullCheck.notEmpty(name, "name");
	    NullCheck.notNull(job, "job");
	    this.name = name;
	    this.job = job;
	    this.showResultMessage = showResultMessage;
	}

	@Override public String getName()
	{
	    return name;
	}

	@Override public void onCommand(Luwrain luwrain)
	{
	    notNull(luwrain, "luwrain");
	}
    }

    static private final class JobShortcut implements Shortcut
    {
	private final String name;
	private final Job job;
	private final boolean showResultMessage;

	JobShortcut(String name, Job job, boolean showResultMessage)
	{
	    notEmpty(name, "name");
	    notNull(job, "job");
	    this.name = name;
	    this.job = job;
	    this.showResultMessage = showResultMessage;
	}

	@Override public String getExtObjName()
	{
	    return name;
	}

	@Override public Application[] prepareApp(String[] args)
	{
	    notNullItems(args, "args");
	    return null;
	}
    }
    */
}
