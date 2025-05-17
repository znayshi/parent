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

import static org.luwrain.core.Base.*;
import static org.luwrain.core.NullCheck.*;

final class AppManager
{
    static private final String LOG_COMPONENT = Base.LOG_COMPONENT;

    private LaunchedApp desktopApp = null;
    private final ArrayList<LaunchedApp> apps = new ArrayList<>();
    private int activeAppIndex = -1;
    private final LaunchedAppPopups shell = new LaunchedAppPopups();
    private final List<OpenedPopup> popups = new ArrayList<>();

    void setDesktopApp(Application app)
    {
	notNull(app, "app");
	final var newApp = new LaunchedApp(app);
	if (!newApp.init())
	    throw new IllegalStateException("Unable to initialize the desktop app");
	this.desktopApp = newApp;
    }

    List<Application> getLaunchedApps()
    {
	final List<Application> res = new ArrayList<>();
	for(LaunchedApp a: apps)
	    res.add(a.app);
	return res;
    }

    boolean setActiveApp(Application app)
    {
	notNull(app, "app");
	final int index = findApp(app);
	if (index < 0)
	    return false;
	activeAppIndex = index;
	return true;
    }

    boolean isActiveApp(Application app)
    {
	notNull(app, "app");
	if (isDesktopApp(app) && activeAppIndex < 0)
	    return true;
	if (activeAppIndex < 0)
	    return false;
	if (apps.get(activeAppIndex).app == app)
	    return true;
	return false;
    }

    Application getActiveApp()
    {
	if (activeAppIndex < 0)
	    return hasDesktopApp()?desktopApp.app:null;
	return apps.get(activeAppIndex).app;
    }

    boolean newApp(Application app)
    {
	notNull(app, "app");
	final Application activeNow = activeAppIndex >= 0?apps.get(activeAppIndex).app:null;
	final int index = findApp(app);
	if (index >= 0)
	{
	    //FIXME:prepareForSwitching();
	    activeAppIndex = index;
	    return true;
	}
	final LaunchedApp launchedApp = new LaunchedApp(app);
	if (!launchedApp.init())
	    return false;
	launchedApp.activeAppBeforeLaunch = activeNow;
	apps.add(launchedApp);
	activeAppIndex = apps.size() - 1;
	return true;
    }

    void removeApp(Application app)
    {
	notNull(app, "app");
	final int index = findApp(app);
	if (index == -1)
	    throw new IllegalArgumentException("Trying to remove an unknown app of class " + app.getClass().getName());
	final LaunchedApp removedApp = apps.get(index);
	apps.remove(index);
	for(LaunchedApp a: apps)
	    if (a.activeAppBeforeLaunch == app)
		a.activeAppBeforeLaunch = null;
	if (apps.isEmpty())
	{
	    activeAppIndex = -1;
	    return;
	}
	if (removedApp.activeAppBeforeLaunch == null)
	{
	    activeAppIndex = apps.size() - 1;
	    return;
	}
	//Trying to activate the app which was active before the removed one has been launched
	activeAppIndex = findApp(removedApp.activeAppBeforeLaunch);
	if (activeAppIndex < 0)//We have a bug in this case
	    activeAppIndex = apps.size() - 1;
    }

    void switchNextApp()
    {
	if (apps.isEmpty())
	{
	    activeAppIndex = -1;
	    return;
	}
	if (activeAppIndex < 0)//We are in desktop
	{
	    //FIXME:general preparing for switching;
	    desktopApp.removeReviewWrappers();
	    activeAppIndex = 0;
	    return;
	}
	apps.get(activeAppIndex).removeReviewWrappers();
	++activeAppIndex;
	if (activeAppIndex >= apps.size())
	    activeAppIndex = 0;
    }

    boolean updateAppAreaLayout(Application app)
    {
	notNull(app, "app");
	if (isDesktopApp(app))
	    return this.desktopApp.refreshAreaLayout();
	final int index = findApp(app);
	if (index < 0)
	    return false;
	return apps.get(index).refreshAreaLayout();
    }

    /**
     * Sets the new active area for the application. Area argument may
     * designate the desired area by the reference of any kind, either to the
     * original area or to any of its wrappers. The area must present in area
     * layout associated with the application/ This method sets active only
     * non-popul areas.
     *
     * @param app The application to set active area for
     * @param area The reference to new active area (to the original area or to any wrapper)
     * @return True if new active area was set, false otherwise
     */
    boolean setActiveAreaForApp(Application app, Area area)
    {
	notNull(app, "app");
	notNull(area, "area");
	if (isDesktopApp(app))
	    return this.desktopApp.setActiveArea(area);
	final int index = findApp(app);
	if (index < 0)
	    return false;
	return apps.get(index).setActiveArea(area);
    }

    Area getFrontActiveAreaForActiveApp()
    {
	if (activeAppIndex < 0 && hasDesktopApp())
	    return this.desktopApp.getFrontActiveArea();
	if (activeAppIndex >= 0)
	    return apps.get(activeAppIndex).getFrontActiveArea();
	return null;
    }

    //The null app means system popup
    void addNewPopup(Application app, Area area, Popup.Position position, Base.PopupStopCondition stopCondition, Set<Popup.Flags> flags)
    {
	notNull(area, "area");
	notNull(position, "position");
	notNull(stopCondition, "stopCondition");
	final LaunchedAppPopups launchedApp;
	if (app != null)
	{
	    if (isDesktopApp(app))
		launchedApp = desktopApp; else
	    {
		final int index = findApp(app);
		if (index < 0)
		    return;
		launchedApp = apps.get(index);
	    }
	} else
	    launchedApp = shell;
	final int popupIndex = 	launchedApp.addPopup(area);
	popups.add(new OpenedPopup(app, popupIndex, position, stopCondition, flags));
    }

    void closeLastPopup()
    {
	if (popups.isEmpty())
	{
	    warn("trying to remove the last popup without any opened popups at all");
	    return;
	}
	final OpenedPopup removedPopup = popups.get(popups.size() - 1);
	popups.remove(popups.size() - 1);
	if (removedPopup.app != null)
	{
	    if (isDesktopApp(removedPopup.app))
		this.desktopApp.closeLastPopup(); else
	    {
		final int appIndex = findApp(removedPopup.app);
		if (appIndex >= 0)
		    apps.get(appIndex).closeLastPopup(); else
		    warn("the popup being closed is associated with unknown application");
	    }
	} else
	    shell.closeLastPopup();
    }

    boolean isLastPopupDiscontinued()
    {
	if (popups.isEmpty())
	    return true;
	return !popups.get(popups.size() - 1).stopCondition.continueEventLoop();
    }

    boolean hasAnyPopup()
    {
	return !popups.isEmpty();
    }

    //null is a valid argument;
    boolean hasPopupOfApp(Application app)
    {
	final LaunchedAppPopups launchedApp;
	if (app != null)
	{
	    if (isDesktopApp(app))
		launchedApp = desktopApp; else
	    {
		final int index = findApp(app);
		if (index < 0)
		    return false;
		launchedApp = apps.get(index);
	    }
	} else
	    launchedApp = shell;
	return launchedApp.popupWrappings.size() > 0;
    }

    Application getAppOfLastPopup()
    {
	return !popups.isEmpty()?popups.get(popups.size() - 1).app:null;
    }

    Area getFrontAreaOfTopPopup()
    {
	if (popups.isEmpty())
	    return null;
	final OpenedPopup popup = popups.get(popups.size() - 1);
	final LaunchedAppPopups launchedApp;
	if (popup.app != null)
	{
	    if (isDesktopApp(popup.app))
		launchedApp = this.desktopApp; else
	    {
		final int appIndex = findApp(popup.app);
		if (appIndex < 0)
		{
		    warn("The top popup is associated with unknown app");
		    return null;
		}
		launchedApp = apps.get(appIndex);
	    }
	} else
	    launchedApp = shell;
	return launchedApp.getFrontAreaOfPopup(popup.index);
    }

    AreaLayout getFrontAreaLayout(Application app)
    {
	notNull(app, "app");
	if (isDesktopApp(app))
	    return this.desktopApp.getFrontAreaLayout();
	final int index = findApp(app);
	if (index < 0)
	    return null;
	return apps.get(index).getFrontAreaLayout();
    }

    boolean isAppLaunched(Application app)
    {
	notNull(app, "app");
	if (isDesktopApp(app))
	    return true;
	return findApp(app) >= 0;
    }

    //app can't be null, environment popups should be processed with getCorrespondingEffectiveArea(area);
    //Area may be an area of any kind, either natural or wrapping;
    Area getCorrespondingEffectiveArea(Application app, Area area)
    {
	notNull(app, "app");
	notNull(area, "area");
	if (isDesktopApp(app))
	    return this.desktopApp.getCorrespondingFrontArea(area);
	final int index = findApp(app);
	if (index < 0)
	    return null;
	return apps.get(index).getCorrespondingFrontArea(area);
    }

    //Area may be an area of any kind, either natural or wrapping;
    Area getCorrespondingEffectiveArea(Area area)
    {
	notNull(area, "area");
	if (hasDesktopApp())
	{
	    final Area res = this.desktopApp.getCorrespondingFrontArea(area);
	    if (res != null)
		return res;
	}
	for(LaunchedApp a: apps)
	{
	    final Area res = a.getCorrespondingFrontArea(area);
	    if (res != null)
		return res;
	}
	final Area res = shell.getCorrespondingFrontArea(area);
	if (res != null)
	    return res;
	return null;
    }

    /**
     * Returns the area wrapping object for the required area. Provided
     * reference designates a cell in the application layout, pointing either
     * to the natural area, either to the security wrapper or to the review
     * wrapper. Search is carried out over all applications (including the
     * default application) and all environment popups. 
     *
     * @param area The area designating a cell in application layout by the natural area itself or by any of its wrappers
     * @return The area wrapping which corresponds to  the requested cell of the application layout
     */
    OpenedArea getAreaWrapping(Area area)
    {
	notNull(area, "area");
	if (hasDesktopApp())
	{
	    final OpenedArea res = this.desktopApp.getAreaWrapping(area);
	    if (res != null)
		return res;
	}
	for(LaunchedApp a: apps)
	{
	    final OpenedArea res = a.getAreaWrapping(area);
	    if (res != null)
		return res;
	}
	final OpenedArea res = shell.getAreaWrapping(area);
	if (res != null)
	    return res;
	return null;
    }

    boolean setAreaWrapper(Area area, AreaWrapperFactory factory)
    {
	notNull(area, "area");
	notNull(factory, "factory");
	final OpenedArea wrapping = getAreaWrapping(area);
	if (wrapping == null || wrapping.wrapper != null)
	    return false;
	final Area wrapper = factory.createAreaWrapper(area, wrapping);
	if (wrapper == null)
	    return false;
	wrapping.wrapper = wrapper;
	return true;
    }

    Popup.Position getPositionOfTopPopup()
    {
	if (popups.isEmpty())
	    return null;
	return popups.get(popups.size() - 1).position;
    }

    void onNewPopupOpening(Application app, Class newCopyClass)
    {
	if (isDesktopApp(app))
	    return;
	for(OpenedPopup p: popups)
	{
	    if (p.app != app || !p.flags.contains(Popup.Flags.NO_MULTIPLE_COPIES))
		continue;
	    Area area;
	    if (app != null)
	    {
		final int index = findApp(app);
		if (index < 0)
		{
		    error("popups contains a reference to the unregistered application " + app.getClass().getName());
		    continue;
		}
		area = apps.get(index).getNativeAreaOfPopup(p.index);
	    } else
		area = shell.getNativeAreaOfPopup(p.index);
	    if (area == null)
	    {
		error("unable to find a native area of the popup with index " + p.index + " of " + (app != null?" the application " + app.getClass().getName():" the environment"));
		continue;
	    }
	    if (area.getClass().equals(newCopyClass))
		p.stopCondition.cancel();
	}
    }

    void sendBroadcastEvent(org.luwrain.core.events.SystemEvent event)
    {
	notNull(event, "event");
	//	shell.sendBroadcastEvent(event);
	for(LaunchedApp a: apps)
	    a.sendBroadcastEvent(event);
    }

        private int findApp(Application app)
    {
	for(int i = 0;i < apps.size();i++)
	    if (apps.get(i).app == app)
		return i;
	return -1;
    }

    private boolean isDesktopApp(Application app)
    {
	if (app == null || this.desktopApp == null)
	    return false;
	return app == this.desktopApp.app;
    }

    private boolean hasDesktopApp()
    {
	return this.desktopApp != null;
    }
}
