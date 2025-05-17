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

//LWR_API 1.0

package org.luwrain.controls.wizard;

import java.util.*;

import groovy.lang.*;

import org.luwrain.core.*;
import org.luwrain.controls.*;
import org.luwrain.controls.WizardArea.*;

import static org.luwrain.core.NullCheck.*;


public class WizardGroovyController
{
    final Luwrain luwrain;
    final WizardArea area;
    final Map<String, Frame> frames = new HashMap<>();

    public WizardGroovyController(Luwrain luwrain, WizardArea area)
    {
	notNull(luwrain, "luwrain");
	notNull(area, "area");
	this.luwrain = luwrain;
	this.area = area;
    }

    public void call(String title, String firstFrame, Closure frames)
    {
	notNull(title, "title");
	notEmpty(firstFrame, "firstFrame");
	notNull(frames, "frames");
	frames.setDelegate(this);
	frames.call();
	final var frame = this.frames.get(firstFrame);
	if (frame == null)
	    throw new IllegalArgumentException("No first frame: " + firstFrame);
	area.show(frame);
    }

    public void frame(String id, Closure closure)
    {
	notEmpty(id, "id");
	notNull(closure, "closure");
	final var f = area.newFrame();
	final var d = new FrameDelegate(this, f);
	closure.setDelegate(d);
	closure.call();
	frames.put(id, f);
    }

    }
