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

import java.util.*;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.script.core.*;

import static org.luwrain.core.NullCheck.*;

public final class ScriptUtils
{
    static public boolean isNull(Object obj)
    {
	if (obj == null || !(obj instanceof Value))
	    return true;
	final Value value = (Value)obj;
	return value.isNull();
    }

    static public boolean notNull(Value[] values)
    {
	if (values == null)
	    return false;
	for(int i = 0;i < values.length;i++)
	    if (values[i] == null || values[i].isNull())
		return false;
	return true;
    }

    static public boolean notNullAndLen(Value[] values, int len)
    {
	if (!notNull(values))
	    return false;
	return values.length == len;
    }

    static public List<Object> getArrayItems(Object o)
    {
	if (o == null || !(o instanceof Value))
	    return null;
	final Value value = (Value)o;
	if (!value.hasArrayElements())
	    return null;
	final List<Object> res = new ArrayList<>();
	for(long i = 0;i < value.getArraySize();i++)
	    res.add(value.getArrayElement(i));
	return res;
    }

    static public Value getMember(Object obj, String name)
    {
	NullCheck.notEmpty(name, "name");
	if (obj == null || !(obj instanceof Value))
	    return null;
	final Value value = (Value)obj;
	if (value.isNull() || !value.hasMembers())
	    return null;
	return value.getMember(name);
    }

        static public boolean isBoolean(Object obj)
    {
	if (obj == null || !(obj instanceof Value))
	    return false;
	final Value value = (Value)obj;
	return !value.isNull() && value.isBoolean();
	    }

        static public boolean asBoolean(Object obj)
    {
	if (obj == null || !(obj instanceof Value))
	    return false;
	final Value value = (Value)obj;
	if (value.isNull() || !value.isBoolean())
	    return false;
	return value.asBoolean();
	    }

            static public Number asNumber(Object obj)
    {
	if (obj == null || !(obj instanceof Value))
	    return 0;
	final Value value = (Value)obj;
	if (value.isNull() || !value.isNumber())
	    return null;
	if (value.fitsInByte())
	    return Byte.valueOf(value.asByte());
	if (value.fitsInShort())
	    return Short.valueOf(value.asShort());
	if (value.fitsInInt())
	    return Integer.valueOf(value.asInt());
	if (value.fitsInLong())
	    return Long.valueOf(value.asLong());
	if (value.fitsInFloat())
	    return Float.valueOf(value.asFloat());
	return Double.valueOf(value.asDouble());
    }

    static public int asInt(Object obj)
    {
	if (obj == null || !(obj instanceof Value))
	    return 0;
	final Value value = (Value)obj;
	if (value.isNull() || !value.isNumber())
	    return 0;
	return value.asInt();
    }

    static public String asString(Object obj)
    {
	if (obj == null || !(obj instanceof Value))
	    return null;
	final Value value = (Value)obj;
	if (value.isNull() || !value.isString())
	    return null;
	return value.asString();
	    }

        static public Object[] asArray(Object o)
    {
	if (o == null || !(o instanceof Value))
	    return null;
	final Value v = (Value)o;
		if (v.isNull() || !v.hasArrayElements())
	    return null;
	final Object[] res = new Object[(int)v.getArraySize()];
	for(int i = 0;i < res.length;i++)
	    res[i] = v.getArrayElement(i);
	return res;
    }

    static public List<Object> asList(Object o)
    {
	if (o == null || !(o instanceof Value))
	    return null;
	final Value v = (Value)o;
		if (v.isNull() || !v.hasArrayElements())
	    return null;
		final var res = new ArrayList<Object>();
		final long count = v.getArraySize();
		res.ensureCapacity((int)count);
		for(int i = 0;i < count;i++)
		    res.add(v.getArrayElement(i));
	return res;
    }

    static public List<Object> asListOfNativeObjects(Object value)
    {
	final var l = asList(value);
	if (l == null)
	    return null;
	final var res = new ArrayList<Object>();
	res.ensureCapacity(l.size());
	l.forEach(o -> {
		if (o instanceof Value v)
		{
		    if (v.isHostObject())
			res.add(v.asHostObject()); else
			if (v.isString())
			    res.add(v.asString()); else
			if (v.isBoolean())
			    res.add(Boolean.valueOf(v.asBoolean())); else
			    if (v.isNumber())
			    {
				final long num = v.asLong();
				if (num <= Integer.MAX_VALUE && num >= Integer.MIN_VALUE)
				    res.add(Integer.valueOf(Long.valueOf(num).intValue()));
				res.add(Long.valueOf(num));
			    }
		}});
	return res;
    }


        static public String[] asStringArray(Object o)
    {
		if (o == null || !(o instanceof Value))
	    return null;
	final Value v = (Value)o;
	if (v.isNull() || !v.hasArrayElements())
	    return null;
	final String[] res = new String[(int)v.getArraySize()];
	for(int i = 0;i < res.length;i++)
	    res[i] = v.getArrayElement(i).asString();
	return res;
    }

    static public Proxy getArray(Object[] items)
    {
	NullCheck.notNullItems(items, "items");
	return ProxyArray.fromArray((Object[])items);
    }

        static public <E> Proxy getArray(List<E> items)
    {
	NullCheck.notNull(items, "items");
	return ProxyArray.fromArray((Object[])items.toArray(new Object[items.size()]));
    }

        static public <E> Proxy createEnumSet(Set<E> s)
    {
	NullCheck.notNull(s, "s");
	final List<String> res = new ArrayList<>();
	for(E o: s)
	    res.add(o.toString().toLowerCase());
	return ProxyArray.fromArray((Object[])res.toArray(new String[res.size()]));
    }

    @SuppressWarnings("unchecked")
    static public Object getEnumItemByStr(Class enumClass, String itemName)
    {
	NullCheck.notNull(enumClass, "enumClass");
	NullCheck.notEmpty(itemName, "itemName");
	final EnumSet allItems = EnumSet.allOf(enumClass);
	for(Object s: allItems)
	    if (s.toString().equals(itemName))
		return s;
	return null;
    }

    @SuppressWarnings("unchecked")
    static public Set getEnumByArrayObj(Class enumClass, Object arrayObj)
    {
	NullCheck.notNull(enumClass, "enumClass");
	NullCheck.notNull(arrayObj, "arrayObj");
	final String[] items = asStringArray(arrayObj);
	if (items == null)
	    return null;
	final Set res = EnumSet.noneOf(enumClass);
	for(Object o: items)
	{
	    if (o == null)
		continue;
	    final String str = asString(o);
	    if (str == null)
		continue;
	    final Object enumItem = getEnumItemByStr(enumClass, str.toUpperCase());
	    if (enumItem != null)
		res.add(enumItem);
	}
	return res;
    }
}
