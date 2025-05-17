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

package org.luwrain.script.core;

import java.io.*;
import java.util.*;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.*;

import org.luwrain.core.*;
import org.luwrain.script.*;

import static org.luwrain.core.NullCheck.*;

final class JobImpl implements JobLauncher
{
    private final Luwrain luwrain;
    private final Object syncObj;
    private final String name;
    private final Value func;

    JobImpl(Luwrain luwrain, Object syncObj, String name, Value func)
    {
	notNull(luwrain, "luwrain");
	notNull(syncObj, "syncObj");
	notEmpty(name, "name");
	notNull(func, "func");
	this.luwrain = luwrain;
	this.syncObj = syncObj;
	this.name = name;
	this.func = func;
    }

    @Override public String getExtObjName()
    {
	return name;
    }

    @Override public Job launch(Job.Listener listener, String[] args, String dir)
    {
	NullCheck.notNull(listener, "listener");
	NullCheck.notNullItems(args, "args");
	//FIXME: dir processing
	try {
	    final Instance instance = new Instance(listener);
	    synchronized(syncObj) {
	    final Value res = func.execute(new Object[]{
		    "RUN",
		    instance,
		    ProxyArray.fromArray((Object[])args)
		});
	    if (res != null && res.isBoolean() && res.asBoolean())
		return instance;
	    return null;
	    }
	}
	catch(Throwable e)
	{
	    luwrain.crash(e);
	    return null;
	}
    }

    @Override public Set<Flags> getJobFlags()
    {
	return EnumSet.noneOf(Flags.class);
    }

    public final class Instance implements Job, ProxyObject
    {
	private final Listener listener;
	private String name = "";
	Instance(Listener listener)
	{
	    notNull(listener, "listener");
	    this.listener = listener;
	}
	@Override public String getInstanceName() { return name; }
	@Override public Status getStatus()
	{
	    return null;
	}
	@Override public int getExitCode()
	{
	    return 0;
	}
	@Override public boolean isFinishedSuccessfully()
	{
	    return true;
	}
	@Override public List<String> getInfo(String infoType)
	{
	    return Arrays.asList();
	}
	@Override public void stop()
	{
	}
	@Override public Object getMember(String name)
	{
	    if (name == null)
		return null;
	    switch(name)
	    {
	    case "name":
		return name;
	    default:
		return null;
	    }
	}
	@Override public boolean hasMember(String name)
	{
	    switch(name)
	    {
	    case "name":
		return true;
	    default:
		return false;
	    }
	}
	@Override public Object getMemberKeys()
	{
	    return new String[]{
		"name",
	    };
	}
	@Override public void putMember(String name, Value value)
	{
	    if (name == null || name.trim().isEmpty() || value == null)
		return;
	    switch(name)
	    {
	    case "name":
		if (value.isString() && !value.asString().trim().isEmpty() && this.name.isEmpty())
		    this.name = value.asString().trim();
		return;
	    default:
		throw new ScriptException("The command tool instance object doesn't have any member with the name '" + name + "'");
	    }
	}
    }
}
