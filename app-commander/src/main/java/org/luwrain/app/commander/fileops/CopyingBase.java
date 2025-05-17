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
import org.luwrain.util.*;
import org.luwrain.app.commander.*;

abstract class CopyingBase extends Operation
{
    private long totalBytes = 0;
    private long processedBytes = 0;
    private int percent = 0;
    private int lastPercent = 0;

    CopyingBase(OperationListener listener, String name)
    {
	super(listener, name);
    }

        @Override public int getPercent()
    {
	return percent;
    }

    protected void copy(Path[] toCopy, Path dest) throws IOException
    {
	NullCheck.notNullItems(toCopy, "toCopy");
	NullCheck.notEmptyArray(toCopy, "toCopy");
	NullCheck.notNull(dest, "dest");
	for(Path p: toCopy)
	    if (!p.isAbsolute())
		throw new IllegalArgumentException("Paths of all source files must be absolute");
	// Calculating the total size of the source files
	totalBytes = 0;
	for(Path f: toCopy)
	{
	    status("calculating size of " + f);
	    totalBytes += getTotalSize(f);
	}
	status("total size is " + String.valueOf(totalBytes));
	Path d = dest;
	if (!d.isAbsolute())
	{
	    final Path parent = toCopy[0].getParent();
	    NullCheck.notNull(parent, "parent");
	    d = parent.resolve(d);
	    status("absolute destination path:" + d.toString());
	}
	// Checking that d is not a child of any item of toCopy
	for(Path path: toCopy)
	    if (d.startsWith(path))
		throw new IOException(SOURCE_IS_A_PARENT_OF_THE_DEST);
	if (toCopy.length == 1)
	    singleSource(toCopy[0], d); else
	    multipleSource(toCopy, d);
    }

    private void singleSource(Path fileFrom, Path dest) throws IOException
    {
	status("single source mode:copying " + fileFrom + " to " + dest);
	// If the destination directory already exists, just copying whatever fileFrom is
	if (isDirectory(dest, true))
	{
	    status("" + dest + " exists and is a directory (or a symlink to a directory), copying the source file to it");
	    copyRecurse(new Path[]{fileFrom}, dest);
	    return;
	}
	// We sure the destination isn't a directory, maybe even doesn't exist
		    // If fileFrom is a directory, we should copy its content to newly created directory
	if (isDirectory(fileFrom, false))
	{
	    status("" + fileFrom + " is a directory and isn\'t a symlink");
	    if (exists(dest, false)) // Dest can exist, but it's certainly not a directory
	    {
		switch(confirmOverwrite(dest))
		{
		case SKIP:
		    return;
		case CANCEL:
		    throw new IOException(INTERRUPTED);
		}
		status("deleting previously existing " + dest.toString());
		Files.delete(dest);
	    }
	    Files.createDirectories(dest);
	    // Copying the content of fileFrom to the newly created directory
	    copyRecurse(getDirContent(fileFrom), dest);
	    return;
	}
	// We sure that fileFrom and dest aren't directories, but dest may exist
	if (!Files.isSymbolicLink(fileFrom) && !isRegularFile(fileFrom, false))
	{
	    status("" + fileFrom + "is not a symlink and is not a regular file, nothing to do");
	    return;
	}
	status("" + fileFrom + " is a symlink or a regular file");
	if (exists(dest, false))
	{
	    status("" + dest + " exists, trying to overwrite it");
	    switch(confirmOverwrite(dest))
	    {
	    case SKIP:
		return;
	    case CANCEL:
		throw new IOException(INTERRUPTED);
	    }
	    Files.delete(dest);
	}
	// We must be sure that the parent directory of dest exists (but not dest itself)
	if (dest.getParent() != null)
	    Files.createDirectories(dest.getParent());
	copySingleFile(fileFrom, dest);//This takes care if fromFile is a symlink
    }

    private void multipleSource(Path[] toCopy, Path dest) throws IOException
    {
	status("multiple source mode");
	if (exists(dest, false) && !isDirectory(dest, true))
	{
	    status("" + dest.toString() + " exists and is not a directory");
	    switch(confirmOverwrite(dest))
	    {
	    case SKIP:
		return;
	    case CANCEL:
		throw new IOException(INTERRUPTED);
	    }
	    status("deleting previously existing " + dest.toString());
	    Files.delete(dest);
	}
	if (!exists(dest, false))//just for the case dest is a symlink to a directory
	    Files.createDirectories(dest);
	copyRecurse(toCopy, dest);
    }

    private void copyRecurse(Path[] filesFrom, Path fileTo) throws IOException
    {
	NullCheck.notNullItems(filesFrom, "filesFrom");
	NullCheck.notNull(fileTo, "fileTo");
	status("copyRecurse:copying " + filesFrom.length + " entries to " + fileTo);
	//toFile should already exist and should be a directory
	for(Path f: filesFrom)
	{
	    if (!isDirectory(f, false))
	    {
		status("" + f.toString() + " is not a directory, copying it");
		copyFileToDir(f, fileTo);
		continue;
	    }
	    status("" + f.toString() + " is a directory");
	    final Path newDest = fileTo.resolve(f.getFileName());
	    status("new destination is " + newDest.toString());
	    if (exists(newDest, false) && !isDirectory(newDest, true))
	    {
		status("" + newDest + " already exists and isn\'t a directory, asking confirmation and trying to delete it");
		switch(confirmOverwrite(newDest))
		{
		case SKIP:
		    continue;
		case CANCEL:
		    throw new IOException("INTERRUPTED");
		}
		status("deleting previously existing " + newDest.toString());
		Files.delete(newDest);
	    }
	    if (!exists(newDest, false))//just for the case newDest  is a symlink to a directory
		Files.createDirectories(newDest);
	    status("" + newDest + " prepared");
	    copyRecurse(getDirContent(f), newDest);
	}
    }

    private void copyFileToDir(Path file, Path destDir) throws IOException
    {
	NullCheck.notNull(file, "file");
	NullCheck.notNull(destDir, "destDir");
	copySingleFile(file, destDir.resolve(file.getFileName()));
    }

    private void copySingleFile(Path fromFile, Path toFile) throws IOException
    {
	NullCheck.notNull(fromFile, "fromFile");
	NullCheck.notNull(toFile, "toFile");
	if (exists(toFile, false))
	{
	    status("" + toFile + " already exists");
	    switch(confirmOverwrite(toFile))
	    {
	    case SKIP:
		return;
	    case CANCEL:
		throw new IOException(INTERRUPTED);
	    }
	    Files.delete(toFile);
	} // toFile exists
	if (Files.isSymbolicLink(fromFile))
	{
	    Files.createSymbolicLink(toFile, Files.readSymbolicLink(fromFile));
	    return;
	}
	try (final InputStream in = Files.newInputStream(fromFile)) {
	    try (final OutputStream out = Files.newOutputStream(toFile)) {
		StreamUtils.copyAllBytes(in, out,
					 (chunkNumBytes, totalNumBytes)->onNewChunk(chunkNumBytes), ()->interrupted);
		out.flush();
		if (interrupted)
		    throw new IOException("INTERRUPTED");
	    }
	}
    }

    private void onNewChunk(int bytes)
    {
	processedBytes += bytes;
	long lPercent = (processedBytes * 100) / totalBytes;
	percent = (int)lPercent;
	if (percent > lastPercent)
	{
	    onProgress(this);
	    lastPercent = percent;
	}
    }
}
