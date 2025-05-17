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

package org.luwrain.registry.fsdir;

import org.luwrain.core.Registry;

class Value
{
    int type = Registry.INVALID;
    String strValue = "";
    int intValue = 0;
    boolean boolValue = false;

    Value(String value)
    {
	if (value == null)
	    throw new NullPointerException("value may not be null");
	type = Registry.STRING;
	strValue = value;
    }

    Value(int value)
    {
	type = Registry.INTEGER;
	intValue = value;
    }

    Value(boolean value)
    {
	type = Registry.BOOLEAN;
	boolValue = value;
    }
}
