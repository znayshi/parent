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

package org.luwrain.cpanel;

import static org.luwrain.core.NullCheck.*;

public class SimpleElement implements Element
{
    protected Element parent;
    protected String value;

    public SimpleElement(Element parent, String value)
    {
	notNull(parent, "parent");
	notEmpty(value, "value");
	this.parent = parent;
	this.value = value;
    }

    @Override public Element getParentElement()
    {
	return parent;
    }

    @Override public String toString()
    {
	return value;
    }

    @Override public boolean equals(Object o)
    {
	if (o == null || !(o instanceof SimpleElement))
	    return false;
	return value == ((SimpleElement)o).value;
    }

    @Override public int hashCode()
    {
	return value.hashCode();
    }
}
