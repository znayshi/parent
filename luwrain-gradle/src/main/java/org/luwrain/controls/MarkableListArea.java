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

//LWR_API 1.0

package org.luwrain.controls;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;

public class MarkableListArea extends ListArea<Object>
{
    public interface MarksInfo
    {
	boolean marked(Object o);
	void mark(Object o);
	void unmark(Object o);
	boolean toggleMark(Object o);
	void markOnly(Object[] o);
	void clearMarks();
	Object[] getAllMarked();
    }

    static public class Params extends ListArea.Params<Object>
    {
	public MarksInfo marksInfo = null;
    }

    protected final MarksInfo marksInfo;

    public MarkableListArea(MarkableListArea.Params params)
    {
	super(params);
	NullCheck.notNull(params.marksInfo, "params.marksInfo");
	this.marksInfo = params.marksInfo;
    }

    MarksInfo getMarksInfo()
    {
	return marksInfo;
    }

    @Override public boolean onInputEvent(InputEvent event)
    {
	NullCheck.notNull(event, "event");
	if (!event.isSpecial())
	    switch(event.getChar())
	    {
	    case ' ':
		regionPoint.reset();
		context.onAreaNewContent(this);
		return onToggleMark();
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
	case REGION_POINT:
	    marksInfo.clearMarks();
	    context.onAreaNewContent(this);
	    return super.onSystemEvent(event);
	default:
	    return super.onSystemEvent(event);
	}
    }

    protected boolean onToggleMark()
    {
	final Object selected = selected();
	if (selected == null)
	    return false;
	final boolean newState = marksInfo.toggleMark(selected);
	context.say(newState?"Отмечено":"Не отмечено");//fixme:
	context.onAreaNewContent(this);
	return true;
    }

    @Override public void refresh()
    {
	super.refresh();
	final List<Object> newItems = new ArrayList<>();
	final int count = listModel.getItemCount();
	for(int i = 0;i < count;++i)
	{
	    final Object o = listModel.getItem(i);
	    if (marksInfo.marked(o))
		newItems.add(o);
	}
	marksInfo.markOnly(newItems.toArray(new Object[newItems.size()]));
    }

    @Override public boolean onClipboardCopy(int fromX, int fromY, int toX, int toY, boolean withDeleting)
    {
	if (isEmpty() || withDeleting)
	    return false;
	if (fromX < 0 || toX < 0 ||
	    (fromX == toX && fromY == toY))
	{
	    final Object[] objs = marksInfo.getAllMarked();
	    if (objs == null || objs.length == 0)
		return super.onClipboardCopy(fromX, fromY, toX, toY, withDeleting);
	    return listClipboardSaver.saveToClipboard(this, new ListUtils.ArrayModel<Object>(()->{ return objs; }), listAppearance, 0, objs.length, context.getClipboard());
	}
	return super.onClipboardCopy(fromX, fromY, toX, toY, withDeleting);
    }
}
