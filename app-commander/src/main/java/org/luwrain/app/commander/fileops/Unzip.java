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
import java.util.*;

import org.apache.commons.compress.archivers.zip.*;


import org.luwrain.core.*;
import org.luwrain.app.commander.*;
import org.luwrain.util.*;

public final class Unzip extends Operation
{
    static private final String
	CHARSET = "cp866";

    private final Path zipFile;
    private final Path destDir;

    public Unzip(OperationListener listener, String name, Path zipFile, Path destDir)
    {
	super(listener, name);
	ensureValidLocalPath(zipFile);
	ensureValidLocalPath(destDir);
	this.zipFile = zipFile;
	this.destDir = destDir;
}

    @Override public void work() throws IOException
    {
	try (final ZipFile zipFile = new ZipFile(this.zipFile.toFile(), CHARSET, false)) {
	    final Enumeration enumEntry = zipFile.getEntries();
	    while(enumEntry.hasMoreElements())
	    {
		final ZipArchiveEntry entry = (ZipArchiveEntry) enumEntry.nextElement();
		try (final InputStream is = zipFile.getInputStream(entry)) {
		    //handler.onZippedFile(entry.getName(), is);
		}
	    }
	}
    }

    @Override public int getPercent()
    {
	return 0;
    }
}
