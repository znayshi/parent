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
import java.io.*;
import java.nio.file.*;
import org.apache.commons.vfs2.*;

import org.luwrain.core.*;
import org.luwrain.controls.*;
import org.luwrain.app.commander.fileops.*;

class OperationsNames
{
    protected final App app;

    OperationsNames(App app)
    {
	NullCheck.notNull(app, "app");
	this.app = app;
    }

    String copyOperationName(Path[] whatToCopy, Path copyTo)
    {
	if (whatToCopy.length < 1)
	    return "";
	if (whatToCopy.length > 1)
	    return app.getStrings().copyOperationName(whatToCopy[0].getFileName().toString() + ",...", copyTo.getFileName().toString());
	return app.getStrings().copyOperationName(whatToCopy[0].getFileName().toString(), copyTo.getFileName().toString());
    }

        String moveOperationName(Path[] whatToMove, Path moveTo)
    {
	if (whatToMove.length < 1)
	    return "";
	if (whatToMove.length > 1)
	    return app.getStrings().moveOperationName(whatToMove[0].getFileName().toString() + ",...", moveTo.getFileName().toString());
	return app.getStrings().moveOperationName(whatToMove[0].getFileName().toString(), moveTo.getFileName().toString());
    }


    /*
    String moveOperationName(File[] whatToMove, File moveTo)
    {
	if (whatToMove.length < 1)
	    return "";
	if (whatToMove.length > 1)
	    return app.getStrings().moveOperationName(whatToMove[0].getName() + ",...", moveTo.getName());
	return app.getStrings().moveOperationName(whatToMove[0].getName(), moveTo.getName());
    }
    */
}
