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
import java.util.zip .*;

import org.luwrain.core.*;
import org.luwrain.app.commander.*;
import org.luwrain.util.*;

public final class ZipCompress extends Operation
{
    private final Path[] toCompress;
    private final Path zipFile;
    private ZipOutputStream zip = null;

    public ZipCompress(OperationListener listener, String name,
		Path[] toCompress, Path zipFile)
    {
	super(listener, name);
	ensureValidLocalPath(toCompress);
	ensureValidLocalPath(zipFile);
	this.toCompress = toCompress;
	this.zipFile = zipFile;
    }

    @Override protected void work() throws IOException
    {
	try (final OutputStream os = Files.newOutputStream(zipFile)) {
	    try(final ZipOutputStream zip = new ZipOutputStream(os)) {
		this.zip = zip;
		for(Path p: toCompress)
		    add(p);
	    }
	}
    }

    private void add(Path path) throws IOException
    {
	NullCheck.notNull(path, "path");
	if (Files.isSymbolicLink(path))
	    return;
	if (isRegularFile(path, false))
	{
	    addFile(path);
	    return;
	}
	for(Path p: getDirContent(path))
	    add(p);
    }

    private void addFile(Path file) throws IOException
    {
	NullCheck.notNull(file, "file");
	try (final InputStream is = Files.newInputStream(file)) {
	    final ZipEntry zipEntry = new ZipEntry(file.toString());
	    zip.putNextEntry(zipEntry);
	    StreamUtils.copyAllBytes(is, zip);            
	}
    }

    @Override public int getPercent()
    {
	return 0;
    }
}
