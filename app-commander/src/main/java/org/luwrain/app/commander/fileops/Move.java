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

public class Move extends CopyingBase
{
    private final Path[] toMove;
    private final Path moveTo;

    public Move(OperationListener listener, String name,
	 Path[] toMove, Path moveTo)
    {
	super(listener, name);
	NullCheck.notNullItems(toMove, "toMove");
	NullCheck.notEmptyArray(toMove, "toMove");
	NullCheck.notNull(moveTo, "moveTo");
	this.toMove = toMove;
	this.moveTo = moveTo;
    }

    @Override protected void work() throws IOException
    {
	Path dest = moveTo;
	if (!dest.isAbsolute())
	{
	    final Path parent = toMove[0].getParent();
	    NullCheck.notNull(parent, "parent");
	    dest = parent.resolve(dest);
	}
	for(Path path: toMove)
	    if (dest.startsWith(path))
		throw new IOException(INTERRUPTED);
	if (toMove.length > 1)
	    multipleSource(dest); else
singleSource(dest);
    }

	private void multipleSource(Path dest) throws IOException
	{
	    NullCheck.notNull(dest, "dest");
	    //dest should be a directory (trying to implement the same behaviour as by 'mv' utility in Linux)
	    if (!isDirectory(dest, true))
		throw new IOException(MOVE_DEST_NOT_DIR);
	    //All paths must belong to the same partition
	    /*
	    for(Path p: toMove)
		if (!Files.getFileStore(p).equals(Files.getFileStore(dest)))
		{
		    movingThroughCopying();
		    return;
		}
	    */
	    //Do actual moving
	    for(Path p: toMove)
	    {
		final Path d = dest.resolve(p.getFileName());
		if (exists(d, false))
		{
		    switch(confirmOverwrite(d))
		    {
		    case SKIP:
			continue;
		    case CANCEL:
			throw new IOException(INTERRUPTED);
		    }
		    Files.delete(d);
		} //if exists
		Files.move(p, d, StandardCopyOption.ATOMIC_MOVE);
	    }
	}

    private void singleSource(Path dest) throws IOException
    {
	NullCheck.notNull(dest, "dest");
	final Path d;
	if (exists(dest, false) && isDirectory(dest, true))
	    d = dest.resolve(toMove[0].getFileName()); else
	    d = dest;
	if (exists(d, false))
	{
	    switch(confirmOverwrite(d))
	    {
	    case SKIP:
		return;
	    case CANCEL:
		throw new IOException(INTERRUPTED);
	    }
	    Files.delete(d);
	}
	status("singleSource:moving single path " + toMove[0].toString() + " to " + d.toString());
	try {
	Files.move(toMove[0], d, StandardCopyOption.ATOMIC_MOVE);
	}
	catch(java.nio.file.AtomicMoveNotSupportedException e)
	{
	    status("singleSource:atomic move failed, launching moving through copying");
	    //	    movingThroughCopying();
	    return;
	}
    }

    private void movingThroughCopying() throws IOException
    {
	status("performing moving through copying to " + moveTo.toString());
	copy(toMove, moveTo);
	status("deleting source files");
	for(Path p: toMove)
	{
	    status("deleting " + p.toString());
	    deleteFileOrDir(p);
	}
    }
}
