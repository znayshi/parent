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

package org.luwrain.controls.edit;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.core.queries.*;
import org.luwrain.controls.*;

import static org.luwrain.core.NullCheck.*;


//Completely skips EnvironmentEvent.CLEAR
public class MultilineEdit
{
    static public class ModificationResult
    {
	protected final boolean performed;
	protected final String stringArg;
	protected final char charArg;
	public ModificationResult(boolean performed, String stringArg, char charArg)
	{
	    this.performed = performed;
	    this.stringArg = stringArg;
	    this.charArg = charArg;
	}
	public ModificationResult(boolean performed)
	{
	    this(performed, null, '\0');
	}
	public ModificationResult(boolean performed, String stringArg)
	{
	    this(performed, stringArg, '\0');
	}
	public ModificationResult(boolean performed, char charArg)
	{
	    this(performed, null, charArg);
	}
	public final boolean isPerformed()
	{
	    return performed;
	}
	public final String getStringArg()
	{
	    return stringArg;
	}
	public final char getCharArg()
	{
	    return charArg;
	}
    }

    //FIXME:getLineCount() never returns zero
    /**
     * The model for {@link MultilineEdit}. It is supposed that this
     * interface is a front-end for {@link MutableLines} in conjunction with
     * {@link HotPointControl}, but you may use it freely as it is
     * necessary for a particular purpose. See 
     * {@link MultilineEditTranslator} for a default implementation.
     * <p>
     * {@code MultilineEdit} guarantees that each user action results exactly in
     * a single call of some method of this interface.  This allows substitution
     * of any method, which makes changes in the model, by any number of
     * other methods in any order, and this will keep all structures
     * consistent.
     * <p>
     * If some operation is addressed at the position outside of the stored
     * text, the result may be undefined. The implementation of this
     * interface should not issue any speech output.
     *
     * @see MultilineEditTranslator
     */
    public interface Model extends Lines
    {
	int getHotPointX();
	int getHotPointY();
	String getTabSeq();
	//Processes only chars within line bounds,  neither end of line not end of text not processed
	ModificationResult deleteChar(int pos, int lineIndex);

	//Expects ending point always after starting
	ModificationResult deleteRegion(int fromX, int fromY, int toX, int toY);

	ModificationResult insertRegion(int x, int y, String[] lines);

	/**
	 * Puts one or several characters at some position. The position expects
	 * to be valid in the content of the model, except of the case when there
	 * are no lines at all, {@code lineIndex} equals to zero and {@code pos}
	 * equals to zero. In this case the method must insert one empty line
	 * prior to making any required changes.
	 * <p>
	 * After performing the operation, the method must prepare the {@link ModificationResult}
	 * object. If the {@code chars} arguments has the
	 * length greater than one, the string argument of the result must be set
	 * to the value of {@code chars} argument.
	 * <p>
	 * If {@code chars } argument has the length equals to one, this single
	 * character must be returned as the character argument of the result. If
	 * the method is requested to insert a single spacing character, the
	 * string argument of the result must contain the last word prior to the
	 * inserting position. If there is no any word prior to the inserting
	 * position, the result may have the string argument empty.
	 *
	 * @param pos The position on the line to put characters at
	 * @param lineIndex The index of the line to put characters on
	 * @param chars The characters to put
	 * @return The {@link ModificationResult} object with the flag if the operation was performed and other corresponding information
	 */
	ModificationResult putChars(int pos, int lineIndex, String chars);

	ModificationResult mergeLines(int firstLineIndex);

	/**
	 * Splits the specified line at the specified position. This method
	 * removes on the line all the content after the specified position and puts
	 * the deleted fragment on new line which is inserted just after
	 * modified. If the position is given outside of the stored text, the
	 * behaviour of this method is undefined.
	 *
	 * @param pos The 0-based position to split line at
	 * @param lineIndex The 0-based index of the line to split
	 * @return The fragment moved onto newly inserted line
	 */
	ModificationResult splitLine(int pos, int lineIndex);
    }

    public interface Appearance
    {
	boolean onBackspaceDeleteChar(ModificationResult result);
	boolean onBackspaceMergeLines(ModificationResult result);
	boolean onBackspaceTextBegin();
	boolean onChar(ModificationResult result);
	boolean onDeleteChar(ModificationResult result);
	boolean onDeleteCharMergeLines(ModificationResult result);
	boolean onDeleteCharTextEnd();
	boolean onSplitLines(ModificationResult result);
	boolean onTab(ModificationResult result);
    }

    static public final class Params
    {
	public ControlContext context = null;
	public Model model = null;
	public Appearance appearance = null;
	public AbstractRegionPoint regionPoint = null;
    }

    protected final ControlContext context;
    protected final Model model;
    protected final Appearance appearance;
    protected final AbstractRegionPoint regionPoint;
    protected final ClipboardTranslator clipboardTranslator;
    protected final RegionTextQueryTranslator regionTextQueryTranslator;

    public MultilineEdit(Params params)
    {
	notNull(params, "params");
	notNull(params.model, "params.model");
			  notNull(params.appearance, "params.appearance");
	notNull(params.regionPoint, "params.regionPoint");
	this.context = params.context;
	this.regionPoint = params.regionPoint;
	this.model = params.model;
	this.appearance = params.appearance;
	this.clipboardTranslator = new ClipboardTranslator(new LinesClipboardProvider(model, ()->context.getClipboard()){
		@Override public boolean onClipboardCopy(int fromX, int fromY, int toX, int toY, boolean withDeleting)
		{
		    if (!super.onClipboardCopy(fromX, fromY, toX, toY, false))
			return false;
		    if (!withDeleting)
			return true;
		    final ModificationResult res = model.deleteRegion(fromX, fromY, toX, toY);
		    return res != null?res.isPerformed():false;
		}
		@Override public boolean onDeleteRegion(int fromX, int fromY, int toX, int toY)
		{
		    final ModificationResult res = model.deleteRegion(fromX, fromY, toX, toY);
		    return res != null?res.isPerformed():null;
		}
	    }, regionPoint, EnumSet.noneOf(ClipboardTranslator.Flags.class));
	this.regionTextQueryTranslator = new RegionTextQueryTranslator(new LinesRegionTextQueryProvider(model), regionPoint, EnumSet.noneOf(RegionTextQueryTranslator.Flags.class));
    }

    public Model getMultilineEditModel()
    {
	return model;
    }

    public Appearance getMultilineEditAppearance()
    {
	return appearance;
    }

    public String[] getRegionText()
    {
	final RegionTextQuery query = new RegionTextQuery();
	if (!regionTextQueryTranslator.onAreaQuery(query, model.getHotPointX(), model.getHotPointY()))
	    return null;
	final String res = query.getAnswer();
	if (res == null)
	    return null;
	if (res.isEmpty())
	    return new String[0];
	return res.split("\n", -1);
    }

    public boolean onInputEvent(InputEvent event)
    {
	NullCheck.notNull(event, "event");
	if (!event.isSpecial())//&&
	    return onChar(event);
	if (event.isModified())
	    return false;
	switch(event.getSpecial())
	{
	case BACKSPACE:
	    return onBackspace(event);
	case DELETE:
	    return onDelete(event);
	case TAB:
	    return onTab(event);
	case ENTER:
	    return onEnter(event);
	default:
	    return false;
	}
    }

    public boolean onSystemEvent(SystemEvent event)
    {
	notNull(event, "event");
	if (event.getType() != SystemEvent.Type.REGULAR)
	    return false;
	switch(event.getCode())
	{
	case CLEAR:
	    return false;
	case CLIPBOARD_PASTE:
	    return onClipboardPaste();
	default:
	    if (clipboardTranslator.onSystemEvent(event, model.getHotPointX(), model.getHotPointY()))
		return true;
	    return regionTextQueryTranslator.onSystemEvent(event, model.getHotPointX(), model.getHotPointY());
	}
    }

    public boolean onAreaQuery(AreaQuery query)
    {
	notNull(query, "query");
	return regionTextQueryTranslator.onAreaQuery(query, model.getHotPointX(), model.getHotPointY());
    }

    protected boolean onBackspace(InputEvent event)
    {
	if (model.getHotPointY() >= model.getLineCount())
	    return false;
	if (model.getHotPointX() <= 0 && model.getHotPointY() <= 0)
	    return appearance.onBackspaceTextBegin();
	if (model.getHotPointX() <= 0)
	{
	    final ModificationResult res = model.mergeLines(model.getHotPointY() - 1);
	    return appearance.onBackspaceMergeLines(res);
	}
	final ModificationResult res = model.deleteChar(model.getHotPointX() - 1, model.getHotPointY());
	return appearance.onBackspaceDeleteChar(res);
    }

    protected boolean onDelete(InputEvent event)
    {
	if (model.getHotPointY() >= model.getLineCount())
	    return false;
	final String line = model.getLine(model.getHotPointY());
	if (line == null)
	    return false;
	if (model.getHotPointX() < line.length())
	{
	    final ModificationResult res = model.deleteChar(model.getHotPointX(), model.getHotPointY());
	    return appearance.onDeleteChar(res);
	}
	if (model.getHotPointY() + 1 >= model.getLineCount())
	    return appearance.onDeleteCharTextEnd();
	final ModificationResult res = model.mergeLines(model.getHotPointY());
	return appearance.onDeleteCharMergeLines(res);
    }

    protected boolean onTab(InputEvent event)
    {
	final String tabSeq = model.getTabSeq();
	if (tabSeq == null)
	    return false;
	final ModificationResult res = model.putChars(model.getHotPointX(), model.getHotPointY(), tabSeq);
	return appearance.onTab(res);
    }

    protected boolean onEnter(InputEvent event)
    {
	final ModificationResult res = model.splitLine(model.getHotPointX(), model.getHotPointY());
	return appearance.onSplitLines(res);
    }

    protected boolean onChar(InputEvent event)
    {
	final char c = event.getChar();
	final ModificationResult res = model.putChars(model.getHotPointX(), model.getHotPointY(), Character.valueOf(c).toString());
	return appearance.onChar(res);
    }

    protected boolean onClipboardPaste()
    {
	if (context.getClipboard().isEmpty())
	    return false;
	final ModificationResult res = model.insertRegion(model.getHotPointX(), model.getHotPointY(), context.getClipboard().getStrings());
	return res != null?res.isPerformed():false;
    }
}
