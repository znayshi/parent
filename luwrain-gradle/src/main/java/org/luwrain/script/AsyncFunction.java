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

package org.luwrain.script;

import java.util.concurrent.*;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.*;

import org.luwrain.core.*;
import static org.luwrain .core.NullCheck.*;

public interface AsyncFunction
{
    void run(Value[] args, CompletableFuture<Object> res);

    static public ProxyExecutable create(Context context, Object syncObj, AsyncFunction f)
    {
	notNull(context, "context");
	notNull(syncObj, "syncObj");
	notNull(f, "f");
	return (ProxyExecutable)(args)->{
	    final CompletableFuture<Object> res = new CompletableFuture<>();
	    f.run(args, res);
	    return wrapCompletableFuture(context, res, syncObj);
	};
    }

    static public Value wrapCompletableFuture(Context context, CompletableFuture<Object> f, Object syncObj)
    {
        final Value promiseConstructor = context.getBindings("js").getMember("Promise");
        return promiseConstructor.newInstance((ProxyExecutable) arguments -> {
		final Value resolve = arguments[0], reject = arguments[1];
		f.whenComplete((result, ex) -> {
			synchronized(syncObj){
			    if (result != null)
				resolve.execute(result); else
				reject.execute(ex);
			}
		    });
		return null;
	    });
    }
}
