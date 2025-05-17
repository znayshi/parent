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

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

import static org.luwrain.core.NullCheck.*;

public class InputEventObj extends MapScriptObject
{
    protected final InputEvent event;

    public InputEventObj(InputEvent event)
    {
	notNull(event, "event");
	this.event = event;
	members.put("special", event.isSpecial()?event.getSpecial().toString():null);//FIXME: To be deleted
		members.put("code", event.isSpecial()?event.getSpecial().toString():null);
	members.put("ch", event.isSpecial()?null:new String(new StringBuilder().append(event.getChar())));
	members.put("withAlt", Boolean.valueOf(event.withAlt()));
	members.put("withAltOnly", Boolean.valueOf(event.withAltOnly()));
	members.put("withControl", Boolean.valueOf(event.withControl()));
	members.put("withControlOnly", Boolean.valueOf(event.withControlOnly()));
	members.put("withShift", Boolean.valueOf(event.withShift()));
	members.put("withShiftOnly", Boolean.valueOf(event.withShiftOnly()));
	members.put("modified", Boolean.valueOf(event.isModified()));
    }
}
