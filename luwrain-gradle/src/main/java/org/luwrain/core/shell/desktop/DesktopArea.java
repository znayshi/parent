/*
   Copyright 2012-2022 Michael Pozhidaev <msp@luwrain.org>

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

package org.luwrain.core.shell.desktop;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.core.shell.*;
import org.luwrain.io.json.*;

final class DesktopArea extends EditableListArea<DesktopItem> implements EditableListArea.ClickHandler<DesktopItem>
{
    private final Luwrain luwrain;

    DesktopArea(Luwrain luwrain, String areaName, Conversations conv)
    {
	super(createParams(luwrain, areaName, conv));
	NullCheck.notNull(luwrain, "luwrain");
	this.luwrain = luwrain;
	setListClickHandler(this);
    }

    static private EditableListArea.Params<DesktopItem> createParams(Luwrain luwrain, String areaName, Conversations conv)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(areaName, "areaName");
	NullCheck.notNull(conv, "conv");
	final EditableListArea.Params<DesktopItem> params = new EditableListArea.Params<>();
	params.context = new DefaultControlContext(luwrain);
	params.model = new Model(luwrain);
	params.appearance = new org.luwrain.core.shell.desktop.Appearance(luwrain);
	params.name = areaName;
	params.clipboardSaver = (area, model, appearance, fromIndex, toIndex, clipboard)->{
	    final List<UniRefInfo> u = new ArrayList<>();
	    final List<String> s = new ArrayList<String>();
	    for(int i = fromIndex;i < toIndex;++i)
	    {
		final DesktopItem  obj = model.getItem(i);
		final UniRefInfo uniRefInfo = obj.getUniRefInfo(luwrain);
		u.add(uniRefInfo);
		s.add(uniRefInfo.getTitle());
	    }
	    clipboard.set(u.toArray(new UniRefInfo[u.size()]), s.toArray(new String[s.size()]));
	    return true;
	};
	params.confirmation = (area,model,fromIndex,toIndex)->{
	    if (fromIndex + 1== toIndex)
		return conv.deleteItem(params.appearance.getScreenAppearance(model.getItem(fromIndex), EnumSet.noneOf(EditableListArea.Appearance.Flags.class)));
	    return conv.deleteItems(toIndex - fromIndex);
	};
	return params;
    }

    @Override public boolean onInputEvent(InputEvent event)
    {
	NullCheck.notNull(event, "event");
	if (event.isSpecial() && !event.isModified())
	    switch(event.getSpecial())
	    { 
	    case ESCAPE:
		return onEscape();
	    }
	return super.onInputEvent(event);
    }

    @Override public boolean onSystemEvent(SystemEvent event)
    {
	NullCheck.notNull(event, "event");
	if (event.getType() != SystemEvent.Type.REGULAR)
	    return super.onSystemEvent(event);
	switch(event.getCode())
	{
	case HELP:
	    return luwrain.openHelp("luwrain.default");
	case CLOSE:
	    luwrain.silence();
	    luwrain.message(luwrain.i18n().getStaticStr("DesktopNoApplication"), Sounds.NO_APPLICATIONS);
	    return true;
	default:
	    return super.onSystemEvent(event);
	}
    }

    @Override protected String noContentStr()
    {
	return "Рабочий стол пуст";//FIXME:
    }

    @Override public boolean onListClick(ListArea area, int index, DesktopItem item)
    {
	NullCheck.notNull(item, "item");
	    final UniRefInfo uniRefInfo = item.getUniRefInfo(luwrain);
	    return luwrain.openUniRef(uniRefInfo.getValue());
    }

    private boolean onEscape()
    {
	if (luwrain == null)
	    return false;
	final Settings.UserInterface sett = Settings.createUserInterface(luwrain.getRegistry());
	final String cmdName = sett.getDesktopEscapeCommand("");
	if (cmdName.trim().isEmpty())
	    return false;
	return luwrain.runCommand(cmdName.trim());
    }

    static private final class Model extends ArrayList<DesktopItem> implements EditableListArea.Model<DesktopItem>
    {
	private final Luwrain luwrain;
	private final Settings.UserInterface sett;
	Model(Luwrain luwrain)
	{
	    NullCheck.notNull(luwrain, "luwrain");
	    this.luwrain = luwrain;
	    this.sett = Settings.createUserInterface(luwrain.getRegistry());
	    addAll(Arrays.asList(DesktopItem.fromJson(sett.getDesktopContent(""))));
	}
	@Override public boolean removeFromModel(int fromIndex, int toIndex)
	{
	    if (fromIndex < 0)
		throw new IllegalArgumentException("fromIndex can't be negative");
	    if (toIndex < 0)
		throw new IllegalArgumentException("toIndex can't be negative");
	    if (fromIndex >= size() || toIndex > size())
		return false;
	    removeRange(fromIndex, toIndex);
	    save();
	    return true;
	}
	@Override public boolean addToModel(int index, java.util.function.Supplier<Object> supplier)
	{
	    NullCheck.notNull(supplier, "supplier");
	    if (index < 0)
		throw new IllegalArgumentException("index may not be negative");
	    final Object supplied = supplier.get();
	    if (supplied == null)
		return false;
	    final Object[] objs;
	    if (supplied instanceof Object[])
		objs = (Object[])supplied; else
		objs = new Object[]{supplied};
	    if (objs.length == 0)
		return false;
	    final List<DesktopItem> newItems = new ArrayList<>();
	    for(Object o: objs)
	    {
		final UniRefInfo info = UniRefUtils.make(luwrain, o);
		if (info == null)
		    return false;
		newItems.add(new DesktopItem(info));
	    }
	    addAll(index, newItems);
	    save();
	    return true;
	}
	@Override public int getItemCount()
	{
	    return size();
	}
	@Override public DesktopItem getItem(int index)
	{
	    return get(index);
	}
	@Override public void refresh()
	{
	}
	private void save()
	{
	    sett.setDesktopContent(DesktopItem.toJson(toArray(new DesktopItem[size()])));
	}
    }
}
