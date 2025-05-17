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

package org.luwrain.script.core;

import org.graalvm.polyglot.*;
import com.oracle.truffle.js.runtime.JSContextOptions;

import org.luwrain.core.*;

import static org.luwrain.core.NullCheck.*;

public final class Module implements AutoCloseable
{
    final Context context;
    final Object syncObj = new Object();
    private final Luwrain luwrain;
    final InternalCoreFuncs internalCoreFuncs;
    final LuwrainObj luwrainObj;

    public Module(Luwrain luwrain, InternalCoreFuncs internalCoreFuncs, Bindings bindings)
    {
	notNull(luwrain, "luwrain");
	this.luwrain = luwrain;
	this.internalCoreFuncs = internalCoreFuncs;
	final Engine engine = Engine.newBuilder()
	.option("engine.WarnInterpreterOnly", "false")
	.build();
	this.context = Context.newBuilder("js")
	.engine(engine)
	.allowExperimentalOptions(true)
	.allowHostAccess(HostAccess.ALL) //FIXME: Better to use .EXPLICIT
		.option(JSContextOptions.INTEROP_COMPLETE_PROMISES_NAME, "false")
	.build();
			this.luwrainObj = new LuwrainObj(luwrain, syncObj, this);
	this.context.getBindings("js").putMember("Luwrain", this.luwrainObj);
	if (bindings != null)
	    bindings.onBindings(context.getBindings("js"), luwrainObj.syncObj);

    }

    public Module(Luwrain luwrain, Bindings bindings)
    {
	this(luwrain, null, bindings);
    }

    public Module(Luwrain luwrain )
    {
	this(luwrain, null, null);
    }

    public Object eval(String exp)
    {
	synchronized(syncObj) {
	    return context.eval("js", exp);
	}
    }

    public void execFuncValue(Value func)
    {
	synchronized(syncObj){
		    func.execute();
	}
    }

    public Value execNewInstance(Value construct, Object[] args)
    {
	synchronized(syncObj){
	    return construct.newInstance(args);
	}
    }

    @Override public void close()
    {
	synchronized(syncObj) {
	    context.close();
	}
    }
}
