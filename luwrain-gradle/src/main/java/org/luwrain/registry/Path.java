/*
   Copyright 2012-2021 Michael Pozhidaev <msp@luwrain.org>

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

package org.luwrain.registry;

import org.luwrain.core.NullCheck;

//Root directory may not contain values;
public final class Path
{
    private final boolean absolute;
    private final String[] dirItems;
    private final String valueName;

    public Path(boolean absolute, String[] dirItems, String valueName)
    {
	NullCheck.notNullItems(dirItems, "dirItems");
	NullCheck.notNull(valueName, "valueName");
	this.absolute = absolute;
	this.dirItems = dirItems.clone();
	this.valueName = valueName;
	for(int i = 0;i < dirItems.length;++i)	    
	    if (dirItems[i].isEmpty())
		throw new IllegalArgumentException("dirItems[" + String.valueOf(i) + "] may not be empty");
    }

    public Path(boolean absolute, String[] dirItems)
    {
	this(absolute, dirItems, "");
    }

    public boolean isAbsolute()
    {
	return absolute;
    }

    public boolean isDirectory()
    {
	return valueName.isEmpty();
    }

    public boolean isRoot()
    {
	return absolute && dirItems.length < 1 && valueName.isEmpty();
    }

    public String[] dirItems()
    {
	return dirItems.clone();
    }

    public String valueName()
    {
	return valueName;
    }

    public Path getDirectory()
    {
	return new Path(absolute, dirItems);
    }

    public int getDirCount()
    {
	return dirItems.length;
    }

    public Path getParentOfDir()
    {
	if (dirItems.length < 1)
	    return this;
	String[] newItems = new String[dirItems.length - 1];
	for(int i = 0;i < dirItems.length - 1;++i)
	    newItems[i] = dirItems[i];
	return new Path(absolute, newItems);
    }

    public String getLastDirItem()
    {
	if (dirItems.length < 1)
	    return "";
	return dirItems[dirItems.length - 1];
    }

    public @Override String toString()
    {
	String res = absolute?"/":"";
	for(String s: dirItems)
	    res += s + "/";
	res += valueName;
	return res;
    }
}
