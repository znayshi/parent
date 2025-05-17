/*
   Copyright 2012-2024 Michael Pozhidaev <msp@luwrain.org>

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

package org.luwrain.core;

import java.io.*;

public final class ScriptFile extends ScriptSource
{
    private final String component, file, dataDir;

    public ScriptFile(String component, String file, String dataDir)
    {
	NullCheck.notEmpty(component, "component");
	NullCheck.notEmpty(file, "file");
	NullCheck.notEmpty(dataDir, "dataDir");
	this.component = component;
	this.file = file;
	this.dataDir = dataDir;
    }

    public String getComponent()
    {
	return this.component;
    }

    public String getFile()
    {
	return this.file;
    }

    public String getDataDir()
    {
	return this.dataDir;
    }

    public File asFile()
    {
	return new File(file);
    }

    public File getDataDirAsFile()
    {
	return new File(dataDir);
    }

    @Override public String toString()
    {
	return file;
    }
}
