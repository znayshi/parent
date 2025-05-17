/*
   Copyright 2012-2021 Michael Pozhidaev <msp@luwrain.org>

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

//LWR_API 1.0

package org.luwrain.popups;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.core.queries.*;
import org.luwrain.controls.*;
import org.luwrain.io.*;
import org.luwrain.util.*;

public final class Popups
{
    static final String LOG_COMPONENT = "popups";
    static public final Set<Popup.Flags> DEFAULT_POPUP_FLAGS = EnumSet.noneOf(Popup.Flags.class);

    static public String text(Luwrain luwrain,
			      String name, String prefix, String text,
			      StringAcceptance acceptance, Luwrain.SpeakableTextType speakableTextType)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	NullCheck.notNull(prefix, "prefix");
	NullCheck.notNull(text, "text");
	NullCheck.notNull(speakableTextType, "speakableTextType");
	final SimpleEditPopup popup = new SimpleEditPopup(luwrain, name, prefix, text, DEFAULT_POPUP_FLAGS){
		@Override protected String getSpeakableText(String prefix, String text)
		{
		    return prefix + luwrain.getSpeakableText(text, speakableTextType);
		}
		@Override public boolean onOk()
		{
		    if (acceptance != null && !acceptance.acceptable(text))
			return false;
		    return true;
		}
	    };
	luwrain.popup(popup);
	if (popup.closing.cancelled())
	    return null;
	return popup.text ();
    }

    static public String text(Luwrain luwrain, String name, String prefix,
			      String text, StringAcceptance acceptance)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	NullCheck.notNull(prefix, "prefix");
	NullCheck.notNull(text, "text");
	return text(luwrain, name, prefix, text, acceptance, Luwrain.SpeakableTextType.PROGRAMMING);
    }

    static public String text(Luwrain luwrain, String name, String prefix, String text)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	NullCheck.notNull(prefix, "prefix");
	NullCheck.notNull(text, "text");
	return text(luwrain, name, prefix, text, null);
    }

    static public String textNotEmpty(Luwrain luwrain, String name, String prefix, String text)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notEmpty(name, "name");
	NullCheck.notEmpty(prefix, "prefix");
	NullCheck.notNull(text, "text");
	return text(luwrain, name, prefix, text, (input)->{
		if (input.trim().isEmpty())
		{
		    luwrain.message("Значение не должно быть пустым", Luwrain.MessageType.ERROR);
		    return false;
		}
		return true;
	    });
    }

    static public String editWithHistory(Luwrain luwrain,
					 String name, String prefix, String text, 
					 Set<String> history, Set<Popup.Flags> popupFlags)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	NullCheck.notNull(prefix, "prefix");
	NullCheck.notNull(text, "text");
	NullCheck.notNull(history, "history");
	NullCheck.notNull(popupFlags, "popupFlags");
	final EditListPopup popup = new EditListPopup(luwrain, 
						      new EditListPopupUtils.FixedModel(history.toArray(new String[history.size()])),
						      name, prefix, text, popupFlags);
	luwrain.popup(popup);
	if (popup.closing.cancelled())
	    return null;
	history.add(popup.text());
	return popup.text ();
    }

    static public String editWithHistory(Luwrain luwrain, String name,
					 String prefix, String text, Set<String> history)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	NullCheck.notNull(prefix, "prefix");
	NullCheck.notNull(text, "text");
	NullCheck.notNull(history, "history");
	return editWithHistory(luwrain, name, prefix, text, history, DEFAULT_POPUP_FLAGS);
    }

    static public Object fixedList(Luwrain luwrain,
				   String name, final Object[] items,
				   Set<Popup.Flags> popupFlags)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	NullCheck.notNullItems(items, "items");
	NullCheck.notNull(popupFlags, "popupFlags");
	final ListArea.Model<Object> model = new ListArea.Model<Object>(){
		@Override public int getItemCount() { return items.length; }
		@Override public Object getItem(int index) { return index < items.length?items[index]:null; }
		@Override public void refresh() {}
	    };
	final ListArea.Params<Object> params = new ListArea.Params<>();
	params.context = new DefaultControlContext(luwrain);
	params.name = name;
	params.model = model;
	params.appearance = new ListUtils.DefaultAppearance<>(params.context, Suggestions.POPUP_LIST_ITEM);
	//	params.flags = ListArea.Params.loadPopupFlags(luwrain.getRegistry());
	params.flags = EnumSet.of(ListArea.Flags.EMPTY_LINE_TOP);
	final ListPopup<Object> popup = new ListPopup<>(luwrain, params, popupFlags);
	luwrain.popup(popup);
	if (popup.closing.cancelled())
	    return null;
	return popup.selected();
    }

    static public Object fixedList(Luwrain luwrain,
				   String name, Object[] items)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	NullCheck.notNullItems(items, "items");
	return fixedList(luwrain, name, items, DEFAULT_POPUP_FLAGS);
    }

    static public File path(Luwrain luwrain, String name, String prefix, File startFrom, FileAcceptance acceptance)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notEmpty(name, "name");
	NullCheck.notEmpty(prefix, "prefix");
	final FilePopup popup = new FilePopup(luwrain, name, prefix,
					      acceptance, startFrom != null?startFrom:luwrain.getFileProperty(Luwrain.PROP_DIR_USERHOME), getUserHome(luwrain),
					      loadFilePopupFlags(luwrain), Popups.DEFAULT_POPUP_FLAGS){
		@Override public boolean onInputEvent(InputEvent event)
		{
		    NullCheck.notNull(event, "event");
		    if (event.isSpecial() && !event.isModified())
			switch(event.getSpecial())
			{
			case INSERT:
			    {
				if (text().isEmpty())
				    return false;
				final File file = FileUtils.ifNotAbsolute(luwrain.getFileProperty("luwrain.dir.userhome"), text());
				if (file.exists())
				{
				    if (file.isDirectory())
					luwrain.message(luwrain.i18n().getStaticStr("DirAlreadyExists"), Luwrain.MessageType.ERROR); else
					luwrain.message(luwrain.i18n().getStaticStr("FileAlreadyExists"), Luwrain.MessageType.ERROR);
				    return true;
				}
				if (file.mkdir())
				    luwrain.message(luwrain.i18n().getStaticStr("DirCreated"), Luwrain.MessageType.OK); else
				    luwrain.message(luwrain.i18n().getStaticStr("UnableToCreateDir"), Luwrain.MessageType.ERROR);
			    }
			    return true;
			}
		    return super.onInputEvent(event);
		}
		@Override public boolean onAreaQuery(AreaQuery query)
		{
		    NullCheck.notNull(query, "query");
		    switch(query.getQueryCode())
		    {
		    case AreaQuery.UNIREF_AREA:
			{
			    final File f = FileUtils.ifNotAbsolute(luwrain.getFileProperty("luwrain.dir.userhome"), text);
			    if (f.getAbsolutePath().isEmpty())
				return false;
			    ((UniRefAreaQuery)query).answer("file:" + f.getAbsolutePath());
			    return true;
			}
		    default:
			return super.onAreaQuery(query);
		    }
		}
	    };
	luwrain.popup(popup);
	if (popup.wasCancelled())
	    return null;
	return popup.result();
    }

        static public File path(Luwrain luwrain, String name, String prefix, FileAcceptance acceptance)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notEmpty(name, "name");
	NullCheck.notNull(prefix, "prefix");
		return path(luwrain, name, prefix, null, acceptance);
    }

    static public File path(Luwrain luwrain, String name, String prefix)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notEmpty(name, "name");
	NullCheck.notEmpty(prefix, "prefix");
	return path(luwrain, name, prefix, null);
    }

    static public File existingFile(Luwrain luwrain, String name, File startWith, String[] extensions)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notEmpty(name, "name");
	NullCheck.notNull(startWith, "startWith");
	NullCheck.notNullItems(extensions, "extensions");
	final Settings.UserInterface sett = Settings.createUserInterface(luwrain.getRegistry());
	final CommanderArea.Filter<File> filter;
	if (sett.getFilePopupSkipHidden(false))
	    filter = CommanderPopup.FILTER_NO_HIDDEN; else
	    filter = CommanderPopup.FILTER_ALL;
	final AtomicReference<Object> res = new AtomicReference<>(null);
	final CommanderPopup popup = new CommanderPopup(luwrain, name, 
							startWith, filter, DEFAULT_POPUP_FLAGS){
		@Override public boolean onSystemEvent(SystemEvent event)
		{
		    NullCheck.notNull(event, "event");
		    if (event.getType() != SystemEvent.Type.REGULAR)
			return super.onSystemEvent(event);
		    switch(event.getCode())
		    {
		    case ACTION:
			if (ActionEvent.isAction(event, "select"))
			    return closing.doOk();
			return super.onSystemEvent(event);
		    default:
			return super.onSystemEvent(event);
		    }
		}
		@Override public boolean onOk()
		{
		    final File file = getSelectedEntry();
		    if (file == null || !file.isFile())
			return false;
		    res.set(file);
		    return true;
		}
		@Override public Action[] getAreaActions()
		{
		    final Action[] a = super.getAreaActions();
		    final File file = getSelectedEntry();
		    if (file == null || !file.isFile())
			return a;
		    final Action[] res = Arrays.copyOf(a, a.length + 1);
		    res[res.length - 1] = new Action("select", "Выбрать файл");
		    return res;
		}
	    };
	luwrain.popup(popup);
	if (popup.isCancelled())
	    return null;
	return (File)res.get();
    }

    static public File existingFile(Luwrain luwrain, String name)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notEmpty(name, "name");
	return Popups.existingFile(luwrain, name, getUserHome(luwrain), new String[0]);
    }

    static public File existingDir(Luwrain luwrain, String name, File startWith)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notEmpty(name, "name");
	final Settings.UserInterface sett = Settings.createUserInterface(luwrain.getRegistry());
	final CommanderArea.Filter<File> filter;
	if (sett.getFilePopupSkipHidden(false))
	    filter = CommanderPopup.FILTER_NO_HIDDEN; else
	    filter = CommanderPopup.FILTER_ALL;
	final AtomicReference<Object> res = new AtomicReference<>(null);
	final CommanderPopup popup = new CommanderPopup(luwrain, name,
							startWith, filter, DEFAULT_POPUP_FLAGS){
		@Override public boolean onSystemEvent(SystemEvent event)
		{
		    NullCheck.notNull(event, "event");
		    if (event.getType() != SystemEvent.Type.REGULAR)
			return super.onSystemEvent(event);
		    switch(event.getCode())
		    {
		    case ACTION:
			if (ActionEvent.isAction(event, "select"))
			    return closing.doOk();
			return super.onSystemEvent(event);
		    default:
			return super.onSystemEvent(event);
		    }
		}
		@Override public boolean onOk()
		{
		    final File file = opened();
		    if (file == null)
			return false;
		    res.set(file);
		    return true;
		}
		@Override public Action[] getAreaActions()
		{
		    final Action[] a = super.getAreaActions();
		    final File file = opened();
		    if (file == null)
			return a;
		    final Action[] res = Arrays.copyOf(a, a.length + 1);
		    res[res.length - 1] = new Action("select", "Выбрать текущий каталог");
		    return res;
		}
	    };
	luwrain.popup(popup);
	if (popup.isCancelled())
	    return null;
	return (File)res.get();
    }

    static public File existingDir(Luwrain luwrain, String name)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notEmpty(name, "name");
	return existingDir(luwrain, name, getUserHome(luwrain));
    }

    static boolean mkdir(Luwrain luwrain, File createIn)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(createIn, "createIn");
	final FilePopup newDirPopup = new FilePopup(luwrain, "Новый каталог", "Имя нового каталога:",
						    null, new File(""), createIn, loadFilePopupFlags(luwrain), DEFAULT_POPUP_FLAGS){
		@Override public boolean onOk()
		{
		    final File file = result();
		    if (file == null)
			return false;
		    if (file.mkdir())
			return true;
		    luwrain.message(luwrain.i18n().getStaticStr("UnableToCreateDir"), Luwrain.MessageType.ERROR);
		    return false;
		}
	    };
	luwrain.popup(newDirPopup);
	return true;
    }

    static public File disks(Luwrain luwrain, String name)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	final DisksPopup popup = new DisksPopup(luwrain, name, DEFAULT_POPUP_FLAGS);
	luwrain.popup(popup);
	if (popup.wasCancelled())
	    return null;
	return popup.result();
    }

    static public String fixedEditList(Luwrain luwrain,
				       String name, String prefix, String text,
				       String[] items)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	NullCheck.notNull(prefix, "prefix");
	NullCheck.notNull(text, "text");
	NullCheck.notNullItems(items, "items");
	final EditListPopup popup = new EditListPopup(luwrain, new EditListPopupUtils.FixedModel(items),
						      name, prefix, text, DEFAULT_POPUP_FLAGS);
	luwrain.popup(popup);
	if (popup.closing.cancelled())
	    return null;
	return popup.text();
    }

    static public boolean confirmDefaultYes(Luwrain luwrain,
					    String name, String text)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	NullCheck.notNull(text, "text");
	final YesNoPopup popup = new YesNoPopup(luwrain, name, text, true, DEFAULT_POPUP_FLAGS);
	luwrain.popup(popup);
	if (popup.closing.cancelled())
	    return false;
	return popup.result();
    }

    static public boolean confirmDefaultNo(Luwrain luwrain,
					   String name, String text)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(name, "name");
	NullCheck.notNull(text, "text");
	final YesNoPopup popup = new YesNoPopup(luwrain, name, text, false, DEFAULT_POPUP_FLAGS);
	luwrain.popup(popup);
	if (popup.closing.cancelled())
	    return false;
	return popup.result();
    }

    static private File getUserHome(Luwrain luwrain)
    {
	NullCheck.notNull(luwrain, "luwrain");
	return luwrain.getFileProperty("luwrain.dir.userhome");
    }

    static public Set<FilePopup.Flags> loadFilePopupFlags(Luwrain luwrain)
    {
	NullCheck.notNull(luwrain, "luwrain");
	final Settings.UserInterface sett = Settings.createUserInterface(luwrain.getRegistry());
	if (sett.getFilePopupSkipHidden(false))
	    return EnumSet.of(FilePopup.Flags.SKIP_HIDDEN);
	return EnumSet.noneOf(FilePopup.Flags.class);
    }
}
