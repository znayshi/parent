/*
   Copyright 2012-2022 Michael Pozhidaev <michael.pozhidaev@gmail.com>

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

package org.luwrain.app.commander.fileops;

import java.io.*;
import java.nio.file.*;

import org.luwrain.core.*;
import org.luwrain.app.commander.*;

public final class Copy extends CopyingBase
{
    private final Path[] copyFrom;
    private final Path copyTo;

    public Copy(OperationListener listener, String opName,
	 Path[] copyFrom, Path copyTo)
    {
	super(listener, opName);
	NullCheck.notNullItems(copyFrom, "copyFrom");
	NullCheck.notEmptyArray(copyFrom, "copyFrom");
	NullCheck.notNull(copyTo, "copyTo");
	this.copyFrom = copyFrom.clone();
	this.copyTo = copyTo;
    }

    @Override protected void work() throws IOException
    {
	copy(copyFrom, copyTo);
    }
}
