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

/**
 * A general interface for objects suitable for running in LUWRAIN as
 * an application. Applications in LUWRAIN are objects which provide some
 * interactive features to users.  They offer a set of interactive
 * areas forming a working space.
 * <p>
 * The application instance is created by some client code with only very
 * preliminary initialization. Main initialization happens in 
 * {@code onLaunchApp()} method when a reference to {@link Luwrain} object
 * obtained. This object is used for access to core features and as an
 * identifier of an particular application instance.
 * <p>
 * Each application class may have multiple instances running
 * simultaneously. However, LUWRAIN core can maintain so called mono
 * applications, which support only one instance at any given time (for
 * example, it is pointless to have multiple running instances of a
 * control panel). If the application is required to be a mono
 * application, it must implement {@link MonoApp} interface instead.
 *
 * @see Area Luwrain MonoApp
 * @since 1.0
 */
public interface Application
{
    void onAppClose();
    String getAppName();
    AreaLayout getAreaLayout();
    InitResult onLaunchApp(Luwrain luwrain);
}
