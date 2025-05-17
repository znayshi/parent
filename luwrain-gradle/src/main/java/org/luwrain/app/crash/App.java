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

package org.luwrain.app.crash;

import java.util.*;

import java.io.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.app.base.*;

public final class App extends AppBase<Strings>
{
    final Application srcApp;
    final Area srcArea;
    final Throwable ex;
    private MainLayout mainLayout = null;

    public App(Throwable ex, Application srcApp, Area srcArea)
    {
	super(Strings.NAME, Strings.class, "luwrain.crash");
	NullCheck.notNull(ex, "ex");
	this.ex = ex;
	this.srcApp = srcApp;
	this.srcArea = srcArea;
    }

    @Override public AreaLayout onAppInit()
    {
	this.mainLayout = new MainLayout(this);
	setAppName(getStrings().appName());
	return mainLayout.getLayout();
    }

        @Override public boolean onInputEvent(Area area, InputEvent event)
    {
	NullCheck.notNull(area, "area");
	if (super.onInputEvent(area, event))
	    return true;
	if (event.isSpecial())
	    switch(event.getSpecial())
	    {
	    case ESCAPE:
		closeApp();
		return true;
	    }
	return false;
    }
}
