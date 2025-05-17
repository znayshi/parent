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

package org.luwrain.app.cpanel;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

class ControlPanelImpl implements org.luwrain.cpanel.ControlPanel
{
    private Luwrain luwrain;
    private ControlPanelApp app;

    ControlPanelImpl(Luwrain luwrain, ControlPanelApp app)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(app, "app");
	this.luwrain = luwrain;
	this.app = app;
    }

    @Override public void close()
    {
	app.closeApp();
    }

    @Override public void gotoSectionsTree()
    {
	app.gotoSections();
    }

    @Override public void refreshSectionsTree()
    {
app.refreshSectionsTree();
    }

    @Override public boolean onInputEvent(InputEvent event)
    {
	NullCheck.notNull(event, "event");
	if (event.isSpecial() && !event.isModified())
	    switch(event.getSpecial())
	    {
	    case ESCAPE:
		close();
		return true;
	    case TAB:
		gotoSectionsTree();
		return true;
	    }
	return false;
    }

    @Override public boolean onSystemEvent(SystemEvent event)
    {
	NullCheck.notNull(event, "event");
	if (event.getType() != SystemEvent.Type.REGULAR)
	    return false;
	switch(event.getCode())
	{
	case CLOSE:
	    close();
	    return true;
	default:
	    return false;
	}
    }

    @Override public Luwrain getCoreInterface()
    {
	return luwrain;
    }
}
