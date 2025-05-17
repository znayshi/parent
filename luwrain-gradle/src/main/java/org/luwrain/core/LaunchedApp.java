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

import static org.luwrain.core.Base.*;
import static org.luwrain.core.NullCheck.*;

final class LaunchedApp extends LaunchedAppPopups
{
    static private final String LOG_COMPONENT = Base.LOG_COMPONENT;

    final Application app;
    AreaLayout.Type layoutType;
    Area[] areas;
    OpenedArea[] areaWrappings;
    int activeAreaIndex = 0;
    Application activeAppBeforeLaunch;

    LaunchedApp(Application app)
    {
	notNull(app, "app");
	this.app = app;
    }

    boolean init()
    {
	final AreaLayout layout = getValidAreaLayout();
	layoutType = layout.layoutType;
	areas = layout.getAreas();
	if (areas == null)
	{
	    error("application " + app.getClass().getName() + " has area layout without areas");
	    return false;
	}
	this.areaWrappings = new OpenedArea[areas.length];
	for(int i = 0;i < areas.length;++i)
	{
	    if (areas[i] == null)
	    {
		error("application " + app.getClass().getName() + " has a null area");
		return false;
	    }
	    areaWrappings[i] = new OpenedArea(areas[i]);
	}
	return true;
    }

    boolean refreshAreaLayout()
    {
	final Area previouslyActiveArea = areas[activeAreaIndex];
	final AreaLayout newLayout = getValidAreaLayout();
	final AreaLayout.Type newLayoutType = newLayout.layoutType;
	final Area[] newAreas = newLayout.getAreas();
	if (newAreas == null)
	{
	    error("application " + app.getClass().getName() + " has area layout without areas");
	    return false;
	}
	final OpenedArea[] newAreaWrappings = new OpenedArea[newAreas.length];
	for(int i = 0;i < newAreas.length;++i)
	{
	    if (newAreas[i] == null)
	    {
		error("application " + app.getClass().getName() + " has a null area");
		return false;
	    }
	    newAreaWrappings[i] = new OpenedArea(newAreas[i]);
	}
	layoutType = newLayoutType;
	areas = newAreas;
	areaWrappings = newAreaWrappings;
	activeAreaIndex = -1;
	for(int i = 0;i < areas.length;++i)
	    if (previouslyActiveArea == areas[i])
		activeAreaIndex = i;
	if (activeAreaIndex < 0 || activeAreaIndex > areas.length)
	    activeAreaIndex = 0;
	return true;
    }

    private AreaLayout getValidAreaLayout()
    {
	final AreaLayout layout;
	try {
	    layout = app.getAreaLayout();
	}
	catch (Throwable e)
	{
	    error(e, "application " + app.getClass().getName() + " has thrown an exception on getAreaLayout()");
	    return null;
	}
	if (layout == null)
	{
	    error("application " + app.getClass().getName() + " has returned an empty area layout");
	    return null;
	}
	if (!layout.isValid())
	{
	    error("application " + app.getClass().getName() + " has returned an invalid area layout");
	    return null;
	}
	return layout;
    }

    void removeReviewWrappers()
    {
	if (areaWrappings != null)
	    for(OpenedArea w: areaWrappings)
		w.wrapper = null;
    }

    //Takes the reference of any kind, either to original area  or to a wrapper
    boolean setActiveArea(Area area)
    {
	notNull(area, "area");
	if (areaWrappings == null)
	    return false;
	int index = 0;
	while(index < areaWrappings.length && !areaWrappings[index].hasArea(area))
	    ++index;
	if (index >= areaWrappings.length)
	    return false;
	activeAreaIndex = index;
	return true;
    }

    Area getFrontActiveArea()
    {
	if (activeAreaIndex < 0 || areaWrappings == null)
	    return null;
	return areaWrappings[activeAreaIndex].getFrontArea();
    }

    @Override public Area getCorrespondingFrontArea(Area area)
    {
	NullCheck.notNull(area, "area");
	for(OpenedArea w: areaWrappings)
	    if (w.hasArea(area))
		return w.getFrontArea();
	return super.getCorrespondingFrontArea(area);
    }

    @Override public OpenedArea getAreaWrapping(Area area)
    {
	NullCheck.notNull(area, "area");
	for(OpenedArea w: areaWrappings)
	    if (w.hasArea(area))
		return w;
	return super.getAreaWrapping(area);
    }

    AreaLayout getFrontAreaLayout()
    {
	final Area[] a = new Area[areas.length];
	for(int i = 0;i < areaWrappings.length;++i)
	    a[i] = areaWrappings[i].getFrontArea();
	return new AreaLayout(layoutType, a);
    }

    void sendBroadcastEvent(org.luwrain.core.events.SystemEvent event)
    {
	notNull(event, "event");
	//	super.sendBroadcastEvent(event);
	for(OpenedArea w: areaWrappings)
	    w.getFrontArea().onSystemEvent(event);
    }
}
