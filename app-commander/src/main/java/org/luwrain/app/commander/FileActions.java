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
import org.luwrain.core.Log;
import org.luwrain.core.*;
import org.luwrain.controls.*;
import org.luwrain.app.commander.fileops.*;
import org.luwrain.io.json.*;
import static org.luwrain.util.PathUtils.*;

final class FileActions extends OperationsNames
{
    FileActions(App app)
    {
	super(app);
    }

    boolean size(PanelArea panelArea)
    {
	NullCheck.notNull(panelArea, "panelArea");
	final FileObject[] files = panelArea.getToProcess();
	if (files.length == 0)
	    return false;
	final App.TaskId taskId = app.newTaskId();
	return app.runTask(taskId, ()->{
		final long res;
		try {
		    res = getSize(files);
		}
		catch(org.apache.commons.vfs2.FileSystemException e)
		{
		    app.getLuwrain().crash(e);
		    return;
		}
		app.finishedTask(taskId, ()->app.getLuwrain().message(String.valueOf(res)));
	    });
    }

    private long getSize(FileObject fileObj) throws org.apache.commons.vfs2.FileSystemException
    {
	NullCheck.notNull(fileObj, "fileObj");
	if (fileObj.getType().hasChildren())
	    return getSize(fileObj.getChildren());
	if (fileObj.isFile() && !fileObj.isSymbolicLink())
    	    return fileObj.getContent().getSize();
	return 0;
    }

    private long getSize(FileObject[] files) throws org.apache.commons.vfs2.FileSystemException
    {
	NullCheck.notNullItems(files, "files");
	long sum = 0;
	for(FileObject f: files)
	    sum += getSize(f);
	return sum;
    }

    boolean localCopy(PanelArea copyFromArea, PanelArea copyToArea)
    {
	NullCheck.notNull(copyFromArea, "copyFromArea");
	NullCheck.notNull(copyToArea, "copyToArea");
	if (!copyFromArea.isLocalDir() || !copyToArea.isLocalDir())
	    return false;
	final Path copyFromDir = PanelArea.asPath(copyFromArea.opened());
	if (copyFromDir == null || !copyFromDir.isAbsolute() || !Files.isDirectory(copyFromDir))
	    return false;
	final Path[] filesToCopy = PanelArea.asPath(copyFromArea.getToProcess());
	if (filesToCopy.length == 0)
	    return false;
	final Path copyToDir = PanelArea.asPath(copyToArea.opened());
	if (copyToDir == null || !copyToDir.isAbsolute() || !Files.isDirectory(copyToDir))
	    return false;
	final Path dest = app.getConv().copy(copyFromDir, filesToCopy, copyToDir);
	if (dest == null)
	    return true;
	final String name = copyOperationName(filesToCopy, dest);
	final Copy copy = new Copy(app.opListener, name, filesToCopy, dest);
	app.runOperation(copy);
	return true;
    }

    boolean localMove(PanelArea moveFromArea, PanelArea moveToArea)
    {
	NullCheck.notNull(moveFromArea, "moveFromArea");
	NullCheck.notNull(moveToArea, "moveToArea");
	if (!moveFromArea.isLocalDir() || !moveToArea.isLocalDir())
	    return false;
	final Path moveFromDir = PanelArea.asPath(moveFromArea.opened());
	if (moveFromDir == null || !moveFromDir.isAbsolute() || !Files.isDirectory(moveFromDir))
	    return false;
	final Path[] filesToMove = PanelArea.asPath(moveFromArea.getToProcess());
	if (filesToMove.length == 0)
	    return false;
	final Path moveToDir = PanelArea.asPath(moveToArea.opened());
	if (moveToDir == null || !moveToDir.isAbsolute() || !Files.isDirectory(moveToDir))
	    return false;
	final Path dest = app.getConv().move(moveFromDir, filesToMove, moveToDir);
	if (dest == null)
	    return true;
		final String name = moveOperationName(filesToMove, dest);
	final Move move = new Move(app.opListener, name, filesToMove, dest);
	app.runOperation(move);
	return true;
    }

    boolean localMkdir(PanelArea panelArea)
    {
	NullCheck.notNull(panelArea, "panelArea");
	if (!panelArea.isLocalDir())
	    return false;
	final File createIn = PanelArea.asFile(panelArea.opened());
	if (createIn == null || !createIn.isAbsolute())
	    return false;
	final File newDir = app.getConv().mkdirPopup(createIn);
	if (newDir == null)
	    return true;
	try {
	    java.nio.file.Files.createDirectories(newDir.toPath());
	}
	catch (IOException e)
	{
	    app.getLuwrain().crash(e);
	    return true;
	}
	app.getLuwrain().message(app.getStrings().mkdirOkMessage(newDir.getName()), Luwrain.MessageType.OK);
	panelArea.reread(newDir.getName(), false);
	return true;
    }

    boolean localDelete(PanelArea area)
    {
	NullCheck.notNull(area, "area");
	if (!area.isLocalDir())
	    return false;
	final Path[] files = PanelArea.asPath(area.getToProcess());
	if (files.length == 0)
	    return false;
	if (!app.getConv().deleteConfirmation(files))
	    return true;
	final String opName = "Удаление";//app.getStrings().delOperationName(files);
	Log.debug("proba", "preparing");
	app.runOperation(new Delete(app.opListener, opName, files));
	return true;
    }

    boolean localRun(PanelArea panelArea)
    {
	NullCheck.notNull(panelArea, "panelArea");
	if (!panelArea.isLocalDir())
	    return false;
	final Path dir = PanelArea.asPath(panelArea.opened());
	if (dir == null)
	    return false;
	final Path[] toProcess = PanelArea.asPath(panelArea.getToProcess());
	if (toProcess.length == 0)
	    return false;
	final String cmd = app.getConv().run();
	if (cmd == null || cmd.trim().isEmpty())
	    return true;
	final StringBuilder b = new StringBuilder();
	final int pos = cmd.indexOf(" *");
	if (pos < 0)
	{
	    b.append(cmd.trim());
	    for(Path p: toProcess)
		b.append(" ").append(escapeBash(p.toString()));
	} else
	{
	    b.append(cmd.substring(0, pos).trim());
	    for(Path p: toProcess)
		b.append(" ").append(escapeBash(p.toString()));
	    b.append(" ").append(cmd.substring(pos + 2).trim());
	}
	app.getLuwrain().newJob("sys", new String[]{new String(b)}, dir.toAbsolutePath().toString(), EnumSet.noneOf(Luwrain.JobFlags.class), null);//FIXME: reread panel on finish
	return true;
    }

        boolean localMail(PanelArea panelArea)
    {
	NullCheck.notNull(panelArea, "panelArea");
	if (!panelArea.isLocalDir())
	    return false;
		final Path[] toProcess = PanelArea.asPath(panelArea.getToProcess());
	if (toProcess.length == 0)
	    return false;
	for(Path p: toProcess)
	    if (Files.isDirectory(p))
	    {
		app.message("Невозможно отправить по почте каталог", Luwrain.MessageType.ERROR);//FIXME:
		return true;
	    }
	final List<String> files = new ArrayList<>();
	for(Path p: toProcess)
	    files.add(p.toAbsolutePath().toString());
	Log.error("file-ops", "Failed to delete some files.");
//	final Log.Message message = new Log.Message(Log.Level.ERROR, "file-ops", "Failed to delete some files.");
//	message.setAttachments(files);
	app.getLuwrain().launchApp("message", new String[]{"Failed to delete some files"});
	return true;
    }


    boolean zipCompress(PanelArea panelArea)
    {
	NullCheck.notNull(panelArea, "panelArea");
	final Path[] toProcess = PanelArea.asPath(panelArea.getToProcess());
	if (toProcess.length == 0)
	    return false;
	final ZipCompress zipCompress = new ZipCompress(app.opListener, "zip", toProcess, Paths.get("/tmp/proba.zip"));
	final App.TaskId taskId = app.newTaskId();
	return app.runTask(taskId, ()->{
		zipCompress.run();
	    });
    }
}
