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
import java.util.concurrent.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import org.luwrain.io.CommanderUtilsVfs;
import org.apache.commons.vfs2.*;

import org.luwrain.core.*;
import org.luwrain.core.queries.*;
import org.luwrain.controls.*;
import org.luwrain.script.*;
import org.luwrain.io.*;


class PanelArea extends CommanderArea<FileObject>
{
    private final Luwrain luwrain;

    PanelArea(Params<FileObject> params, Luwrain luwrain)
    {
	super(params);
	this.luwrain = luwrain;
    }

    @Override public CommanderUtilsVfs.Model getCommanderModel()
    {
	return (CommanderUtilsVfs.Model)super.getCommanderModel();
    }

    void open(File file)
    {
	try {
	    open(getCommanderModel().getFileSystemManager().resolveFile(file.getAbsolutePath()));
	}
	catch(org.apache.commons.vfs2.FileSystemException e)
	{
	    throw new RuntimeException(e);
	}
    }

    @Override public boolean onAreaQuery(AreaQuery query)
    {
	if (query.getQueryCode() == AreaQuery.CURRENT_DIR && query instanceof CurrentDirQuery)
	{
	    final CurrentDirQuery currentDirQuery = (CurrentDirQuery)query;
	    final File f = asFile(opened());
	    if (f == null)
		return false;
	    currentDirQuery.answer(f.getAbsolutePath());
	    return true;
	}
	return super.onAreaQuery(query);
    }

    boolean isLocalDir()
    {
	final FileObject o = opened();
	if (o == null)
	    return false;
	return o instanceof org.apache.commons.vfs2.provider.local.LocalFile;
    }

    FileObject[] getToProcess()
    {
	final List<FileObject> res = new ArrayList<>();
	for(Object o: getMarked())
	    res.add((FileObject)o);
	if (!res.isEmpty())
	    return res.toArray(new FileObject[res.size()]);
	final FileObject entry = getSelectedEntry();
	return entry != null?new FileObject[]{entry}:new FileObject[0];
    }

    boolean openLocalPath(String path)
    {
	NullCheck.notNull(path, "path");
	try {
	    open(CommanderUtilsVfs.prepareLocation((CommanderUtilsVfs.Model)getCommanderModel(), path));
	    return true;
	}
	catch(org.apache.commons.vfs2.FileSystemException e)
	{
	    Log.error("commander", "opening " + path + ":" + e.getClass().getName() + ":" + e.getMessage());
	    return false;
	}
    }

    boolean openInitial(String path)
    {
	try {
	    return open(CommanderUtilsVfs.prepareLocation((CommanderUtilsVfs.Model)getCommanderModel(), path), false);
	}
	catch(org.apache.commons.vfs2.FileSystemException e)
	{
	    Log.error("commander", "opening " + path + ":" + e.getClass().getName() + ":" + e.getMessage());
	    return false;
	}
    }

    void showHidden()
    {
	setCommanderFilter(new CommanderUtils.AllEntriesFilter<>());
	reread(false);
    }

    void hideHidden()
    {
	setCommanderFilter(new CommanderUtilsVfs.NoHiddenFilter());
	reread(false);
    }

        static Path asPath(FileObject fileObject)
    {
	if (fileObject == null)
	    return null;
		    if (fileObject instanceof org.apache.commons.vfs2.provider.local.LocalFile)
			return Paths.get(fileObject.getName().getPath());
		    return null;
    }

    static File asFile(FileObject fileObject)
    {
	if (fileObject == null)
	    return null;
		    if (fileObject instanceof org.apache.commons.vfs2.provider.local.LocalFile)
			return new File(fileObject.getName().getPath());
		    return null;
    }

    URL asUrl(FileObject fileObject)
    {
	if (fileObject == null)
	    return null;
	if (fileObject instanceof org.apache.commons.vfs2.provider.ftp.FtpFileObject)
	{
	    final org.apache.commons.vfs2.provider.ftp.FtpFileObject ftpFile = (org.apache.commons.vfs2.provider.ftp.FtpFileObject)fileObject;
	    try {
		final java.net.URL root = new java.net.URL(ftpFile.getFileSystem().getRootURI());
		return new java.net.URL(root, fileObject.getName().getPath());
	    }
	    catch(MalformedURLException e)
	    {
		throw new IllegalArgumentException(e);
	    }
	}
	    return null;
    }

            static Path[] asPath(FileObject[] fileObjects)
    {
	NullCheck.notNullItems(fileObjects, "fileObjects");
	final List<Path> res = new ArrayList<>();
	for(FileObject f: fileObjects)
	{
	    final Path ff = asPath(f);
	    if (ff != null)
		res.add(ff);
	}
	return res.toArray(new Path[res.size()]);
    }

        static File[] asFile(FileObject[] fileObjects)
    {
	NullCheck.notNullItems(fileObjects, "fileObjects");
	final List<File> res = new ArrayList<>();
	for(FileObject f: fileObjects)
	{
	    final File ff = asFile(f);
	    if (ff != null)
		res.add(ff);
	}
	return res.toArray(new File[res.size()]);
    }

    static Params<FileObject> createParams(ControlContext controlContext)
    {
	NullCheck.notNull(controlContext, "controlContext");
	try {
	    Params<FileObject> params = CommanderUtilsVfs.createParams(controlContext);
	    params.flags = EnumSet.of(Flags.MARKING);
	    params.filter = new CommanderUtilsVfs.NoHiddenFilter();
	    params.clipboardSaver = new ListUtils.FunctionalClipboardSaver<>(
									     (entry)->asFile(entry.getNativeObj()),
									     (entry)->entry.getBaseName());
	    return params;
	}
	catch(org.apache.commons.vfs2.FileSystemException e)
	{
	    throw new RuntimeException(e);
	}
    }
}
