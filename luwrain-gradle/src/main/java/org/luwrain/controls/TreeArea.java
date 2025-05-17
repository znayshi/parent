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

//LWR_API 1.0

package org.luwrain.controls;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import java.util.*;

public class TreeArea implements Area
{
    public interface ClickHandler
    {
	boolean onTreeClick(TreeArea area, Object obj);
    }

    public interface Model
    {
	Object getRoot();
	void beginChildEnumeration(Object obj);
	int getChildCount(Object parent);
	Object getChild(Object parent, int index);
	void endChildEnumeration(Object obj);
    }

    static public final class Params
    {
	public ControlContext context;
	public String name;
	public Model model;
	public ClickHandler clickHandler;
    }

    static protected class Node
    {
	Object obj = null;
	Node parent = null;
	Node children[] = null;//If children is null and node is not a leaf, it means this is a closed node without any info about its content
	boolean leaf = true;
	void makeLeaf()
	{
	    children = null;
	    leaf = true;
	} 
	String title() { return obj != null?obj.toString():""; }
    }

    static protected class VisibleItem
    {
	enum Type {LEAF, CLOSED, OPENED};

	Type type = Type.LEAF;
	String title = "";
	int level = 0;
	Node node;
    }

    protected final ControlContext context;
    protected final Model model;
    protected String name = "";
    protected Node root = null;
    protected VisibleItem[] items = null;
    protected int hotPointX = 0;
    protected int hotPointY = 0;
    protected ClickHandler clickHandler = null;

    public TreeArea(Params params)    
    {
	NullCheck.notNull(params, "params");
	NullCheck.notNull(params.context, "params.context");
	NullCheck.notNull(params.model, "params.model");
	NullCheck.notNull(params.name, "params.name");
	this.context = params.context;
	this.model = params.model;
	this.name = params.name;
	this.clickHandler = params.clickHandler;
	this.root = constructNode(model.getRoot(), null, true);//true means children should be expanded
	this.items = generateAllVisibleItems();
    }

    public ClickHandler getClickHandler()
    {
	return clickHandler;
    }

    public void setClickHandler(ClickHandler clickHandler)
    {
	NullCheck.notNull(clickHandler, "clickHandler");
	this.clickHandler = clickHandler;
    }

    public Model getModel()
    {
	return model;
    }

    /*
    public void setModel(Model model)
    {
	NullCheck.notNull(model, "model");
	this.model = model;
    }
    */


    public int getLineCount()
    {
	if (items == null || items.length < 1)
	    return 1;
	return items.length + 1;
    }

    public String getLine(int index)
    {
	if (items == null || items.length < 1 || index >= items.length)
	    return "";
	return constructLineForScreen(items[index]);
    }

    public int getHotPointX()
    {
	return hotPointX >= 0?hotPointX:0;
    }

    public int getHotPointY()
    {
	return hotPointY >= 0?hotPointY:0;
    }

    public boolean onInputEvent(InputEvent event)
    {
	NullCheck.notNull(event, "event");
	if (items == null || items.length < 1)
	{
	    context.setEventResponse(DefaultEventResponse.hint(Hint.NO_CONTENT));
	    return true;
	}
	//Space
	if (!event.isSpecial() && !event.isModified())
	    switch(event.getChar())
	    {
	    case ' ':
		return onKeySpace(event);
	    }
	if (event.isSpecial() && event.withShiftOnly())
	    switch(event.getSpecial())
	    {
	    case ARROW_LEFT:
		return onKeyLeft(event);
	    case ARROW_RIGHT:
		return onKeyRight(event);
	    }
	if (event.isSpecial() && !event.isModified())
	    switch (event.getSpecial())
	    {
	    case ENTER:
		return onKeyEnter(event);
	    case ARROW_DOWN:
		return onKeyDown(event, false);
	    case ALTERNATIVE_ARROW_DOWN:
		return onKeyDown(event, true);
	    case ARROW_UP:
		return onKeyUp(event, false);
	    case ALTERNATIVE_ARROW_UP:
		return onKeyUp(event, true);
	    case ARROW_RIGHT:
		return onExpand(event);
	    case ARROW_LEFT:
		return onCollapse(event);
	    }
	return false;
    }

    @Override public boolean onSystemEvent(SystemEvent event)
    {
	if (event.getCode() == SystemEvent.Code.REFRESH)
	{
	    refresh();
	    return true;
	}
	return false;
    }

    @Override public boolean onAreaQuery(AreaQuery query)
    {
	return false;
    }

    @Override public Action[] getAreaActions()
    {
	return new Action[0];
    }

    @Override public String getAreaName()
    {
	return name != null?name:"";
    }


    public void refresh()
    {
	final Object oldSelected = selected();
	final int oldHotPointY = hotPointY;
	Object newRoot = model.getRoot();
	if (newRoot == null)
	{
	    root = null;
	    items = null;
	    return;
	}
	if (root.obj.equals(newRoot))//FIXME:equals();
	{
	    root.obj = newRoot;
	    refreshNode(root);
	} else
	    root = constructNode(model.getRoot(), null, true); //true means expand children;
	items = generateAllVisibleItems();
	context.onAreaNewContent(this);
	if (oldSelected == null)
		selectFirstItem(); else
	{
	    if (!selectObject(oldSelected))
	{
	    if (items != null && oldHotPointY < items.length)
		hotPointY = oldHotPointY; else 
	    selectEmptyLastLine();
	    context.onAreaNewHotPoint(this);
	}
	}
    }

    public Object selected()
    {
	if (items == null || hotPointY < 0 || hotPointY >= items.length)
	    return null;
	return items[hotPointY].node.obj;
    }

    public boolean selectObject(Object obj)
    {
	if (items == null || items.length == 0)
	    return false;
	int k;
	for(k = 0;k < items.length;++k)
	    if (items[k].node.obj.equals(obj))
		break;
	if (k >= items.length)
	    return false;
	hotPointY = k;
	hotPointX = getInitialHotPointX(hotPointY);
	context.onAreaNewHotPoint(this);
	return true;
    }

    public void selectEmptyLastLine()
    {
	if (items == null || items.length < 1)
	    hotPointY = 0; else
	    hotPointY = items.length;
	hotPointX = 0;
	context.onAreaNewHotPoint(this);
    }

    public void selectFirstItem()
    {
	hotPointY = 0;
	hotPointX = getInitialHotPointX(0);
	context.onAreaNewHotPoint(this);
    }

    protected void onClick(Object obj)
    {
	if (clickHandler != null)
	    clickHandler.onTreeClick(this, obj);
    }

    //Changes only 'leaf' and 'children' fields;
    protected void fillChildrenForNonLeaf(Node node)
    {
	if (node == null || node.obj == null)
	    return;
	if (node.leaf || isLeaf(node.obj))
	{
	    node.makeLeaf();
	    return;
	}
	model.beginChildEnumeration(node.obj);
	final int count = model.getChildCount(node.obj);
	if (count < 1)
	{
	    node.makeLeaf();
	    model.endChildEnumeration(node.obj);
	    return;
	}
	node.leaf = false;
	node.children = new Node[count];
	for(int i = 0;i < count;++i)
	{
	    Node n = new Node();
	    node.children[i] = n;
	    n.obj = model.getChild(node.obj, i);
	    if (n.obj == null)
	    {
		node.makeLeaf();
		model.endChildEnumeration(node.obj);
		return;
	    }
	    n.leaf = isLeaf(n.obj);
	    n.children = null;
	    n.parent = node;
	}
	model.endChildEnumeration(node.obj);
    }

    protected Node constructNode(Object obj, Node parent, boolean fillChildren)
    {
	if (obj == null)
	    return null;
	Node node = new Node();
	node.obj = obj;
	node.parent = parent;
	node.leaf = isLeaf(obj);
	if (fillChildren && !node.leaf)
	fillChildrenForNonLeaf(node);
	return node;
    }

    protected void refreshNode(Node node)
    {
	if (node == null || node.obj == null)
	    return;
	if (node.leaf)
	{
	    node.leaf = isLeaf(node.obj);
	    return;
	}
	//Was not a leaf;
	if (isLeaf(node.obj))
	{
	    node.makeLeaf();
	    return;
	}
	//Was and remains a non-leaf;
	if (node.children == null)
	    return;
	model.beginChildEnumeration(node.obj);
	final int newCount = model.getChildCount(node.obj);
	if (newCount == 0)
	{
	    node.makeLeaf();
	    model.endChildEnumeration(node.obj);
	    return;
	}
	Node[] newNodes = new Node[newCount];
	for(int i = 0;i < newCount;++i)
	{
	    Object newObj = model.getChild(node.obj, i);
	    if (newObj == null)
	    {
		node.makeLeaf();
		model.endChildEnumeration(node.obj);
		return;
	    }
	    int k;
	    for(k = 0;k < node.children.length;++k)
		if (node.children[k].obj.equals(newObj))//FIXME:equals();
		    break;
	    if (k < node.children.length)
	    {
		newNodes[i] = node.children[k]; 
		newNodes[i].obj = newObj;
	    }else
		newNodes[i] = constructNode(newObj, node, false);
	}
	model.endChildEnumeration(node.obj);
	node.children = newNodes;
	for(Node n: node.children)
	    refreshNode(n);
    }

    protected boolean onKeySpace(InputEvent event)
    {
	if (items == null)
	    return false;
	if (hotPointY >= items.length)
	    return false;
	final VisibleItem item = items[hotPointY];
	if (item.node.obj != null)
	    onClick(item.node.obj);
	return true;
    }

    protected boolean onKeyEnter(InputEvent event)
    {
	if (event.isModified() || items == null || hotPointY >= items.length)
	    return false;
	final VisibleItem item = items[hotPointY];
	if (item.type == VisibleItem.Type.LEAF)
	{
	    onClick(item.node.obj);
	    return true;
	}
	if (item.type == VisibleItem.Type.CLOSED)
	{
	    fillChildrenForNonLeaf(item.node);
	    items = generateAllVisibleItems();
	    context.setEventResponse(DefaultEventResponse.hint(Hint.TREE_BRANCH_EXPANDED));
		context.onAreaNewContent(this);
		return true;
	}
	    if (item.type == VisibleItem.Type.OPENED)
	    {
		item.node.children = null;
		items = generateAllVisibleItems();
		context.setEventResponse(DefaultEventResponse.hint(Hint.TREE_BRANCH_COLLAPSED));
		context.onAreaNewContent(this);
		return true;
	    }
	    return false;
    }

    protected boolean onExpand(InputEvent event)
    {
	if (event.isModified() || items == null || hotPointY >= items.length)
	    return false;
	final VisibleItem item = items[hotPointY];
	switch(item.type)
	{
	case LEAF:
	case OPENED:
	    return false;
	case CLOSED:
	    fillChildrenForNonLeaf(item.node);
	    items = generateAllVisibleItems();
	    context.setEventResponse(DefaultEventResponse.hint(Hint.TREE_BRANCH_EXPANDED));
		context.onAreaNewContent(this);
		return true;
	default:
	    return false;
	}
    }

    protected boolean onCollapse(InputEvent event)
    {
	if (event.isModified() || items == null || hotPointY >= items.length)
	    return false;
	final VisibleItem item = items[hotPointY];
	switch(item.type)
	{
	case LEAF:
	case CLOSED:
	    return false;
	case OPENED:
		item.node.children = null;
		items = generateAllVisibleItems();
		context.setEventResponse(DefaultEventResponse.hint(Hint.TREE_BRANCH_COLLAPSED));
		context.onAreaNewContent(this);
		return true;
	default:
	    return false;
	}
    }

    protected boolean onKeyDown(InputEvent event, boolean briefAnnouncement)
    {
	if (event.isModified() || items == null)
	    return false;
	if (hotPointY  >= items.length)
	{
	    context.setEventResponse(DefaultEventResponse.hint(Hint.TREE_END));
	    return true;
	}
	++hotPointY;
	if (hotPointY >= items.length)
	{
	    hotPointX = 0;
	    context.setEventResponse(DefaultEventResponse.hint(Hint.EMPTY_LINE));
	} else
	{
	    hotPointX = getInitialHotPointX(hotPointY);
announce(items[hotPointY], briefAnnouncement);
	}
	context.onAreaNewHotPoint(this );
	return true;
    }

    protected boolean onKeyUp(InputEvent event, boolean briefAnnouncement)
    {
	if (event.isModified() || items == null)
	    return false;
	if (hotPointY  <= 0)
	{
	    context.setEventResponse(DefaultEventResponse.hint(Hint.TREE_BEGIN));
	    return true;
	}
	--hotPointY;
	hotPointX = getInitialHotPointX(hotPointY);
announce(items[hotPointY], briefAnnouncement);
	context.onAreaNewHotPoint(this );
	return true;
    }

    protected boolean onKeyRight(InputEvent event)
    {
	if (items == null || hotPointY >= items.length)
	    return false;
	final String value = items[hotPointY].title;
	final int offset = getInitialHotPointX(hotPointY);
	if (value.isEmpty())
	{
	    context.setEventResponse(DefaultEventResponse.hint(Hint.EMPTY_LINE));
	    return true;
	}
	if (hotPointX >= value.length() + offset)
	{
	    context.setEventResponse(DefaultEventResponse.hint(Hint.END_OF_LINE));
	    return true;
	}
	if (hotPointX < offset)
	    hotPointX = offset; else
	    hotPointX++;
	if (hotPointX >= value.length() + offset)
	    context.setEventResponse(DefaultEventResponse.hint(Hint.END_OF_LINE)); else
	    context.sayLetter(value.charAt(hotPointX - offset));
	context.onAreaNewHotPoint(this);
	return true;
    }

    protected boolean onKeyLeft(InputEvent event)
    {
	if (items == null || hotPointY >= items.length)
	    return false;
	final String value = items[hotPointY].title;
	final int offset = getInitialHotPointX(hotPointY);
	if (value.isEmpty())
	{
	    context.setEventResponse(DefaultEventResponse.hint(Hint.EMPTY_LINE));
	    return true;
	}
	if (hotPointX <= offset)
	{
	    context.setEventResponse(DefaultEventResponse.hint(Hint.BEGIN_OF_LINE));
	    return true;
	}
	if (hotPointX >= value.length() + offset)
	    hotPointX = value.length() + offset - 1; else
	    --hotPointX;
	context.sayLetter(value.charAt(hotPointX - offset));
	context.onAreaNewHotPoint(this);
	return true;
    }

    protected VisibleItem[] generateVisibleItems(Node node, int level)
    {
	if (node == null)
	    return null;
	VisibleItem itself = new VisibleItem();
	itself.node = node;
	itself.title = node.title();
	itself.level = level;
	if (node.leaf || node.children == null)
	{
	    itself.type = node.leaf?VisibleItem.Type.LEAF:VisibleItem.Type.CLOSED;
	    VisibleItem res[] = new VisibleItem[1];
	    res[0] = itself;
	    return res;
	}
	itself.type = VisibleItem.Type.OPENED;
	ArrayList<VisibleItem> items = new ArrayList<VisibleItem>();
	items.add(itself);
    	for(int i = 0;i < node.children.length;i++)
	{
	    VisibleItem c[] = generateVisibleItems(node.children[i], level + 1);
	    if (c == null)
		continue;
	    for(VisibleItem v: c)
		items.add(v);
	}
	VisibleItem res[] = new VisibleItem[items.size()];
	int k = 0;
	for(VisibleItem i: items)
	    res[k++] = i;
	return res;
    }

    protected VisibleItem[] generateAllVisibleItems()
    {
	if (root == null)
	    return null;
	return generateVisibleItems(root, 0);
    }

    protected boolean isLeaf(Object o)
    {
	NullCheck.notNull(o, "o");
	model.beginChildEnumeration(o);
	final boolean res = model.getChildCount(o) <= 0;
	model.endChildEnumeration(o);
	return res;
    }

    protected void announce(VisibleItem item, boolean briefAnnouncement)
    {
	NullCheck.notNull(item, "item");
	if (item.title.isEmpty())
	{
	    context.setEventResponse(DefaultEventResponse.hint(Hint.EMPTY_LINE));
	    return;
	}
	if (briefAnnouncement)
	{
	    context.setEventResponse(DefaultEventResponse.text(item.title));
	    return;
	}
	String res = item.title;
	final EventResponses.TreeItem.Type type;
	switch (item.type)
	{
	case OPENED:
	    type = EventResponses.TreeItem.Type.EXPANDED;
	    break;
	case CLOSED:
	    type = EventResponses.TreeItem.Type.COLLAPSED;
	    break;
	default:
	    	    type = EventResponses.TreeItem.Type.LEAF;
	}
	context.setEventResponse(DefaultEventResponse.treeItem(type, item.title, item.level + 1));
}

    protected String constructLineForScreen(VisibleItem item)
    {
	if (item == null)
	    return "";
	String res = "";
	for(int i = 0;i < item.level;++i)
	    res += "  ";
	switch(item.type)
	{
	case OPENED:
	    res += " -";
	    break;
	case CLOSED:
	    res += " +";
	    break;
	default:
	    res += "  ";
	}
	return res + (item.title != null?item.title:"");
    }

protected int getInitialHotPointX(int index)
    {
	if (items == null ||  index >= items.length)
	    return 0;
	return (items[index].level * 2) + 2;
    }
}
