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

package org.luwrain.core;

import java.util.*;

public interface Job
{
    static public final int
	EXIT_CODE_OK = 0,
	EXIT_CODE_INVALID = -1,
	EXIT_CODE_INTERRUPTED = -2;

    public enum Status {RUNNING, FINISHED};

    	String getInstanceName();
	Status getStatus();
	int getExitCode();
	boolean isFinishedSuccessfully();
	List<String> getInfo(String infoType);
	void stop();

    public interface Listener
    {
	void onStatusChange(Job job);
	void onInfoChange(Job job, String infoType, List<String> value);
    }
}
