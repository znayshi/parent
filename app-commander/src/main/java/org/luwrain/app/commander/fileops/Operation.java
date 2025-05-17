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

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

import org.luwrain.core.*;
import org.luwrain.app.commander.*;

public abstract class Operation implements Runnable
{
    static public final String
	INTERRUPTED = "LWR_INTERRUPTED",
	SOURCE_IS_A_PARENT_OF_THE_DEST = "LWR_SOURCE_IS_A_PARENT_OF_THE_DEST",
	MOVE_DEST_NOT_DIR = "LWR_MOVE_DEST_NOT_DIR";

    public enum ConfirmationChoices {
	OVERWRITE,
	SKIP,
	CANCEL
    };

    private final OperationListener listener;
    public final String name;
    private boolean finished = false;
    private Throwable ex = null;
    private boolean finishingAccepted = false ;
    protected boolean interrupted = false;

    Operation(OperationListener listener, String name)
    {
	this.listener = listener;
	this.name = name;
    }

    protected abstract void work() throws IOException;
    public abstract int getPercent();

    public void run()
    {
	this.ex = null;
	try {
	    try {
		work();
	    }
	    catch (Throwable e)
	    {
		Log.error("commander", name + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
		e.printStackTrace();
		this.ex = e;
	    }
	}
	finally {
	    finished = true;
	    listener.onOperationProgress(this);
	}
    }

    public synchronized void interrupt()
    {
	interrupted = true;
    }

    public boolean isDone()
    {
	return finished;
    }

    public boolean finishingAccepted()
    {
	if (finishingAccepted)
	    return true;
	finishingAccepted = true;
	return false;
    }

    public Throwable getException()
    {
	return this.ex;
    }

    static protected boolean isDirectory(Path path, boolean followSymlinks) throws IOException
    {
	if (followSymlinks)
	    return Files.isDirectory(path); else
	    return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
    }

    static protected Path[] getDirContent(final Path path) throws IOException
    {
	final List<Path> res = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
	    for (Path p : directoryStream) 
		res.add(p);
	} 
	return res.toArray(new Path[res.size()]);
    }

    static protected boolean isRegularFile(Path path, boolean followSymlinks) throws IOException
    {
	if (followSymlinks)
	    return Files.isRegularFile(path); else
	    return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
    }

    static protected boolean exists(Path path, boolean followSymlinks) throws IOException
    {
	    if (followSymlinks)
		return Files.exists(path); else
		return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
    }

    protected void deleteFileOrDir(Path p) throws IOException
    {
	if (isDirectory(p, false))
	{
	    final Path[] content = getDirContent(p);
	    for(Path pp: content)
		deleteFileOrDir(pp);
	}
	Files.delete(p);
    }

    protected void status(String message)
    {
	Log.debug("fileops", message);
    }

    protected ConfirmationChoices confirmOverwrite(Path path)
    {
	/*
FIXME:
	NullCheck.notNull(path, "path");
	return listener.confirmOverwrite(path);
	*/
	return null;
    }

    protected void onProgress(Operation op)
    {
	NullCheck.notNull(op, "op");
	listener.onOperationProgress(op);
    }

    static long getTotalSize(Path p) throws IOException
    {
	if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
	    return Files.size(p);
	if (!Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS))
	    return 0;
	long res = 0;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(p)) {
		for (Path pp : directoryStream) 
		    res += getTotalSize(pp);
	    } 
	return res;
    }

    static protected void ensureValidLocalPath(Path[] path)
    {
	for(Path p: path)
	    ensureValidLocalPath(p);
    }

        static protected void ensureValidLocalPath(Path path)
    {
	    if (!path.isAbsolute())
		throw new IllegalArgumentException(path.toString() + " can't be relative");
    }
}
