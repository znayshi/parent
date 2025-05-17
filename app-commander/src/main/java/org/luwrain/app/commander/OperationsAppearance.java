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

package org.luwrain.app.commander;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.controls.*;
import org.luwrain.app.commander.fileops.*;

import static org.luwrain.core.DefaultEventResponse.*;

class OperationsAppearance extends ListUtils.AbstractAppearance<Operation>
{
    private final App app;
    private final Luwrain luwrain;
    private final Strings strings;

    OperationsAppearance(App app)
    {
	NullCheck.notNull(app, "app");
	this.luwrain = app.getLuwrain();
	this.strings = app.getStrings();
	this.app = app;
    }

    @Override public void announceItem(Operation op, Set<Flags> flags)
    {
	NullCheck.notNull(op, "op");
	NullCheck.notNull(flags, "flags");
	final Sounds sound;
	if (op.isDone())
	{
	    if (op.getException() == null)
		sound = Sounds.SELECTED; else
		sound = Sounds.ATTENTION;
	} else
	    sound = Sounds.LIST_ITEM;
	luwrain.setEventResponse(listItem(sound, op.name, null));
    }

    @Override public String getScreenAppearance(Operation op, Set<Flags> flags)
    {
	NullCheck.notNull(op, "op");
	NullCheck.notNull(flags, "flags");
	return op.name;
    }
}
