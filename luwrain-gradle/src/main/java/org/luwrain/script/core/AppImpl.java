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
import java.util.concurrent.*;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.*;

import org.luwrain.core.*;

import static org.luwrain.core.NullCheck.*;
import static org.luwrain.script.ScriptUtils.*;

final class AppImpl implements Application
{
    final Module module;
    final  Value construct;
    private Value instance = null;
    private Luwrain luwrain = null;
    private String name = "";
    private AreaLayout layout = null;

    AppImpl(Module module, Value construct)
    {
	notNull(module, "module");
	notNull(construct, "construct");
	this.module = module;
	this.construct = construct;
    }

    @Override public void onAppClose()
    {
    }

    @Override public String getAppName()
    {
	return name != null?name:"";
    }

    @Override public AreaLayout getAreaLayout()
    {
	return this.layout;
    }

    @Override public InitResult onLaunchApp(Luwrain luwrain)
    {
	this.luwrain = luwrain;
	this.instance = module.execNewInstance(construct, new Object[]{new ControlObj()});
	return new InitResult();
    }

    public final class ControlObj
    {
	@HostAccess.Export public ProxyExecutable setLayout = this::setLayoutImpl;
	private Object setLayoutImpl(Value[] args)
	{
	    final Area a = args[0].asHostObject();
	    AppImpl.this.layout = new AreaLayout(a);
	    return null;
	}

			@HostAccess.Export public ProxyExecutable setName = this::setNameImpl;
	private Object setNameImpl(Value[] args)
	{
	    if (!notNullAndLen(args, 1) || !args[0].isString())
		throw new IllegalArgumentException("App.setName() takes exactly one string argument");
	    AppImpl.this.name = args[0].asString();
	    return null;
	}


		@HostAccess.Export public ProxyExecutable close = this::closeImpl;
	private Object closeImpl(Value[] args)
	{
	    AppImpl.this.luwrain.closeApp();
	    return null;
	}

	
    }
}
