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

package org.luwrain.app.commander;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.app.base.*;
import org.luwrain.app.commander.fileops.*;

final class App extends AppBase<Strings>
{
    enum Side {LEFT, RIGHT};

    final String startFrom;
    final List<Operation> operations = new ArrayList<>();
    final OperationListener opListener = newOperationListener();
    private Settings sett = null;
    private Conv conv = null;
    private Hooks hooks = null;
    private MainLayout mainLayout = null;
    private OperationsLayout operationsLayout = null;

    App(String startFrom)
    {
	super(Strings.NAME, Strings.class, "luwrain.commander");
	if (startFrom != null && !startFrom.isEmpty())
	    this.startFrom = startFrom; else
	    this.startFrom = null;
    }
    App() { this(null); }

    @Override public AreaLayout onAppInit()
    {
	this.sett = Settings.create(getLuwrain());
	this.conv = new Conv(this);
	this.hooks = new Hooks(this);
	this.mainLayout = new MainLayout(this);
	this.operationsLayout = new OperationsLayout(this);
	setAppName(getStrings().appName());
	return mainLayout.getAreaLayout();
    }

    void runOperation(Operation op)
    {
	this.operations.add(0, op);
	getLuwrain().executeBkg(new FutureTask<>(op, null));
    }

    boolean allOperationsFinished()
    {
	for(Operation op:operations)
	    if (!op.isDone())
		return false;
	return true;
    }

    boolean closeOperation(int index)
    {
	if (index < 0 || index >= operations.size())
	    throw new IllegalArgumentException("index (" + String.valueOf(index) + ") must be positive and less than the number of operations (" + String.valueOf(operations.size()) + ")");
	if (!operations.get(index).isDone())
	    return false;
	operations.remove(index);
	operationsLayout.operationsArea.refresh();
	return true;
    }

    String getOperationResultDescr(Operation op)
    {
	/*
	  switch(op.getResult().getType())
	  {
	  case OK:
	  return getStrings().opResultOk();
	  case SOURCE_PARENT_OF_DEST:
	  return "Целевой каталог является подкаталогом родительского";
	  case MOVE_DEST_NOT_DIR:
	  return "Целевой путь не указывает на каталог";
	  case INTERRUPTED:
	  return getStrings().opResultInterrupted();
	  case EXCEPTION:
	  if (op.getResult().getException() != null)
	  return getLuwrain().i18n().getExceptionDescr(op.getResult().getException());
	  return "Нет информации об ошибке";
	  default:
	  return "";
	  }
	*/
	return "";
    }

    private OperationListener newOperationListener()
    {
	return new OperationListener(){
	    @Override public void onOperationProgress(Operation operation)
	    {
		getLuwrain().runUiSafely(()->{
			operationsLayout.operationsArea.redraw();
			if (operation.isDone())
			{
			    if (operation.getException() == null)
				getLuwrain().playSound(Sounds.DONE); else
				getLuwrain().playSound(Sounds.ERROR);
			    mainLayout.leftPanel.reread(false);
			    mainLayout.rightPanel.reread(false);
			}
		    });
	    }
	};
    }

    @Override public boolean onEscape()
    {
	closeApp();
	return true;
    }

    @Override public void closeApp()
    {
	if (!allOperationsFinished())
	{
	    getLuwrain().message(getStrings().notAllOperationsFinished(), Luwrain.MessageType.ERROR);
	    return;
	}
	if (mainLayout != null)
	{
	    mainLayout.leftPanel.close();
	    mainLayout.rightPanel.close();	    
	}
	super.closeApp();
    }

    void layout(AreaLayout layout)
    {
	getLayout().setBasicLayout(layout);
	getLuwrain().announceActiveArea();
    }

    void layout(AreaLayout layout, Area activeArea)
    {
	getLayout().setBasicLayout(layout);
	getLuwrain().announceActiveArea();
	getLuwrain().setActiveArea(activeArea);
    }

    Layouts layouts()
    {
	return new Layouts(){
	    @Override public void main()
	    {
		setAreaLayout(mainLayout);
		getLuwrain().announceActiveArea();
	    }
	    @Override public void operations()
	    {
		setAreaLayout(operationsLayout);
		getLuwrain().announceActiveArea();
	    }
	};
    }

    Settings getSett() { return this.sett; }
    Conv getConv() { return this.conv; }
    Hooks getHooks() { return this.hooks; }

    interface Layouts
    {
	void main();
	void operations();
    }
}
