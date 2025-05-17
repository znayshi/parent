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

import org.luwrain.core.*;
import org.luwrain.core.events.*;

public class ProgressArea extends SimpleArea
{
    public ProgressArea(ControlContext environment)
    {
	super(environment);
    }

    public ProgressArea(ControlContext environment, String name)
    {
	super(environment, name);
    }

    public ProgressArea(ControlContext environment, String name,
			String[] lines)
    {
	super(environment, name, lines);
    }

    public void addProgressLine(String line)
    {
	NullCheck.notNull(line, "line");
	if (content.getLineCount() > 0)
	{
	    content.setLine(content.getLineCount() - 1, line);
	    content.addLine("");
	} else
	    addLine(line);
	environment.onAreaNewContent(this);
    }
}
