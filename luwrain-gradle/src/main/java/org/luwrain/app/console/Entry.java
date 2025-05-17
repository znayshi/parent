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

package org.luwrain.app.console;

import java.util.*;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.message.*;

final class Entry
{
    final String logger;
    final String message;
    final Throwable ex;
    Entry(LogEvent event)
    {
	this.logger = event.getLoggerName();
	this.message = event.getMessage().getFormattedMessage();
	this.ex = event.getMessage().getThrowable();
    }
}
