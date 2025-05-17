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

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.app.base.*;

final class UnixPropertiesLayout extends LayoutBase
{
    private final App app;
    final FormArea formArea;

    UnixPropertiesLayout(App app, File[] files, Runnable closing)
    {
	super(app);
	NullCheck.notNullItems(files, "files");
	NullCheck.notNull(closing, "closing");
	this.app = app;
	this.formArea = new FormArea(getControlContext()){
		@Override public String getAreaName()
		{
		    return app.getStrings().infoAreaName();
		}
	    };
	setAreaLayout(formArea, null);
    }
}
