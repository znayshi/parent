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

import static org.luwrain.core.NullCheck.*;

final class OpenedPopup
{
    final Application app;
    final int index;//Popup index in the owning application
    final Popup.Position position;
    final Base.PopupStopCondition stopCondition;
    final Set<Popup.Flags> flags;

    OpenedPopup(Application app,
		int index,
		Popup.Position position,
		Base.PopupStopCondition stopCondition,
		Set<Popup.Flags> flags)
    {
	//app can be null
	notNull(position, "position");
	notNull(stopCondition, "stopCondition");
	notNull(flags, "flags");
	this.app = app;
	this.index = index;
	this.position = position;
	this.stopCondition = stopCondition;
	this.flags = flags;
    }
}
