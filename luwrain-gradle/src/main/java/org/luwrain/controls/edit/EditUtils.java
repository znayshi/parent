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

package org.luwrain.controls.edit;

import java.util.*;
import java.util.function.*;

import org.luwrain.core.*;
import org.luwrain.controls.*;
import org.luwrain.controls.edit.MultilineEdit.ModificationResult;

import static org.luwrain.core.NullCheck.*;

public final class EditUtils
{
    static public void blockBounds(EditArea editArea, int lineIndex, BiPredicate<String, LineMarks> pred, BiConsumer<MarkedLines, Integer> accepting)
    {
	NullCheck.notNull(editArea, "editArea");
	NullCheck.notNull(pred, "pred");
	NullCheck.notNull(accepting, "accepting");
	blockBounds(editArea.getContent(), lineIndex, pred, accepting);
    }

    static public void blockBounds(FormArea formArea, int lineIndex, BiPredicate<String, LineMarks> pred, BiConsumer<MarkedLines, Integer> accepting)
    {
	NullCheck.notNull(formArea, "formArea");
	NullCheck.notNull(pred, "pred");
	NullCheck.notNull(accepting, "accepting");
	blockBounds(formArea.getMultilineEditContent(), lineIndex, pred, accepting);
    }

    static public void blockBounds(MarkedLines lines, int lineIndex, BiPredicate<String, LineMarks> pred, BiConsumer<MarkedLines, Integer> accepting)
    {
	NullCheck.notNull(lines, "lines");
	NullCheck.notNull(pred, "pred");
	NullCheck.notNull(accepting, "accepting");
	if (lineIndex < 0)
	    throw new IllegalArgumentException("lineINdex can't be negative");
	if (lineIndex >= lines.getLineCount())
	    return;
	if (!pred.test(lines.getLine(lineIndex), lines.getLineMarks(lineIndex)))
	    return;
	int fromPos = lineIndex, toPos = lineIndex;
	while(fromPos > 0 && pred.test(lines.getLine(fromPos - 1), lines.getLineMarks(fromPos - 1)))
	    fromPos--;
	while(toPos + 1 < lines.getLineCount() && pred.test(lines.getLine(toPos + 1), lines.getLineMarks(toPos + 1)))
	    toPos++;
	for(int i = fromPos;i <= toPos;i++)
	    accepting.accept(lines, Integer.valueOf(i));
    }

    static public class DefaultEditAreaAppearance extends DefaultMultilineEditAppearance implements EditArea.Appearance
    {
	protected final Luwrain.SpeakableTextType speakableTextType;
	public DefaultEditAreaAppearance(ControlContext context, Luwrain.SpeakableTextType speakableTextType)
	{
	    super(context);
	    this.speakableTextType = speakableTextType;
	}
	public DefaultEditAreaAppearance(ControlContext context)
	{
	    this(context, null);
	}
	    @Override public void announceLine(int index, String line)
    {
	notNull(line, "line");
	if (speakableTextType != null)
	    NavigationArea.defaultLineAnnouncement(context, index, context.getSpeakableText(line, speakableTextType
											    )); else
	NavigationArea.defaultLineAnnouncement(context, index, line);
    }
    }

        static public class DefaultMultilineEditAppearance implements MultilineEdit.Appearance
    {
	protected final ControlContext context;
	public DefaultMultilineEditAppearance(ControlContext context)
	{
	    NullCheck.notNull(context, "context");
	    this.context = context;
	}
	@Override public boolean onBackspaceTextBegin()
	{
	    context.setEventResponse(DefaultEventResponse.hint(Hint.BEGIN_OF_TEXT));
	    return true;
	}
	@Override public boolean onBackspaceMergeLines(ModificationResult res)
	{
	    NullCheck.notNull(res, "res");
	    if (!res.isPerformed())
		return false;
	    context.setEventResponse(DefaultEventResponse.hint(Hint.LINE_BOUND));
	    return true;
	}
	@Override public boolean onBackspaceDeleteChar(ModificationResult res)
	{
	    NullCheck.notNull(res, "res");
	    if (!res.isPerformed() || res.getCharArg() == '\0')
		return false;
	    context.setEventResponse(DefaultEventResponse.letter(res.getCharArg()));
	    return true;
	}
	@Override public boolean onDeleteChar(ModificationResult res)
	{
	    NullCheck.notNull(res, "res");
	    if (!res.isPerformed() || res.getCharArg() == '\0')
		return false;
	    context.setEventResponse(DefaultEventResponse.letter(res.getCharArg()));
	    return true;
	}
	@Override public boolean onDeleteCharTextEnd()
	{
	    context.setEventResponse(DefaultEventResponse.hint(Hint.END_OF_TEXT));
	    return true;
	}
	@Override public boolean onDeleteCharMergeLines(ModificationResult res)
	{
	    NullCheck.notNull(res, "res");
	    if (!res.isPerformed())
		return false;
	    context.setEventResponse(DefaultEventResponse.hint(Hint.LINE_BOUND)); 
	    return true;
	}
	@Override public boolean onTab(ModificationResult res)
	{
	    NullCheck.notNull(res, "res");
	    if (!res.isPerformed())
		return false;
	    context.setEventResponse(DefaultEventResponse.hint(Hint.TAB));
	    return true;
	}
	@Override public boolean onSplitLines(ModificationResult res)
	{
	    NullCheck.notNull(res, "res");
	    if (!res.isPerformed())
		return false;
	    final String line = res.getStringArg();
	    if (line == null || line.isEmpty())
		context.setEventResponse(DefaultEventResponse.hint(Hint.EMPTY_LINE)); else
		if (line.trim().isEmpty())
		    context.setEventResponse(DefaultEventResponse.hint(Hint.SPACES)); else
		    context.setEventResponse(DefaultEventResponse.text(line));
	    return true;
	}
	@Override public boolean onChar(ModificationResult res)
	{
	    NullCheck.notNull(res, "res");
	    if (!res.isPerformed())
		return false;
	    if (Character.isWhitespace(res.getCharArg()))
	    {
		final String word = res.getStringArg();
		if (word != null && !word.trim().isEmpty())
		    context.setEventResponse(DefaultEventResponse.text(word)); else
		    context.setEventResponse(DefaultEventResponse.letter(res.getCharArg()));
	    } else
		context.setEventResponse(DefaultEventResponse.letter(res.getCharArg()));
	    return true;
	}
    }


    /**
     * Implements a listener of all changes in 
     * {@link MultilineEdit.Model}. This class contains the abstract method 
     * {@code onMultilineEditChange} called each time when any changes occurred in
     * the state of the model.  This allows users to implement any necessary
     * actions, which should have effect if and only if something was changed
     * in the model and this class guarantees that {@code
     * onMultilineEditChange} is called strictly after changes in the model.
     *
     * @see MultilineEdit
     */
    static abstract public class CorrectorChangeListener implements MultilineEditCorrector
    {
	protected final MultilineEditCorrector corrector;
	public CorrectorChangeListener(MultilineEditCorrector corrector)
	{
	    NullCheck.notNull(corrector, "corrector");
	    this.corrector = corrector;
	}
	/** Called if the model gets some changes. There is a guarantee that this method
	 * is invoked strictly after the changes in the model.
	 */
	abstract public void onMultilineEditChange();
	@Override public int getLineCount()
	{
	    return corrector.getLineCount();
	}
	@Override public String getLine(int index)
	{
	    return corrector.getLine(index);
	}
	@Override public int getHotPointX()
	{
	    return corrector.getHotPointX();
	}
	@Override public int getHotPointY()
	{
	    return corrector.getHotPointY();
	}
	@Override public String getTabSeq()
	{
	    return corrector.getTabSeq();
	}
	@Override public ModificationResult deleteChar(int pos, int lineIndex)
	{
	    final ModificationResult res = corrector.deleteChar(pos, lineIndex);
	    if (res.isPerformed())
		onMultilineEditChange();
	    return res;
	}
	@Override public ModificationResult deleteRegion(int fromX, int fromY, int toX, int toY)
	{
	    final ModificationResult res = corrector.deleteRegion(fromX, fromY, toX, toY);
	    if (res.isPerformed())
		onMultilineEditChange();
	    return res;
	}
	@Override public ModificationResult insertRegion(int x, int y, String[] lines)
	{
	    final ModificationResult res = corrector.insertRegion(x, y, lines);
	    if (res.isPerformed())
		onMultilineEditChange();
	    return res;
	}
	@Override public ModificationResult putChars(int pos, int lineIndex, String str)
	{
	    final ModificationResult res = corrector.putChars(pos, lineIndex, str);
	    if (res.isPerformed())
		onMultilineEditChange();
	    return res;
	}
	@Override public ModificationResult mergeLines(int firstLineIndex)
	{
	    final ModificationResult res = corrector.mergeLines(firstLineIndex);
	    if (res.isPerformed())
		onMultilineEditChange();
	    return  res;
	}
	@Override public ModificationResult splitLine(int pos, int lineIndex)
	{
	    final ModificationResult res = corrector.splitLine(pos, lineIndex);
	    if (res.isPerformed())
		onMultilineEditChange();
	    return res;
	}
        @Override public ModificationResult doEditAction(TextEditAction action)
	{
	    final ModificationResult res = corrector.doEditAction(action);
	    if (res.isPerformed())
		onMultilineEditChange();
	    return res;
	}
    }

    static public class ActiveCorrector implements MultilineEditCorrector
    {
	protected MultilineEditCorrector activatedCorrector = null;
	protected MultilineEditCorrector defaultCorrector = null;
	public void setActivatedCorrector(MultilineEditCorrector corrector)
	{
	    NullCheck.notNull(corrector, "corrector");
	    this.activatedCorrector = corrector;
	}
	public void deactivateCorrector()
	{
	    this.activatedCorrector = null;
	}
	public void setDefaultCorrector(MultilineEditCorrector corrector)
	{
	    NullCheck.notNull(corrector, "corrector");
	    this.defaultCorrector = corrector;
	}
	public MultilineEditCorrector getDefaultCorrector()
	{
	    return defaultCorrector;
	}
	@Override public int getLineCount()
	{
	    if (activatedCorrector != null)
		return activatedCorrector.getLineCount();
	    return defaultCorrector.getLineCount();
	}
	@Override public String getLine(int index)
	{
	    if (activatedCorrector != null)
		return activatedCorrector.getLine(index);
	    return defaultCorrector.getLine(index);
	}
	@Override public int getHotPointX()
	{
	    if (activatedCorrector != null)
		return activatedCorrector.getHotPointX();
	    return defaultCorrector.getHotPointX();
	}
	@Override public int getHotPointY()
	{
	    if (activatedCorrector != null)
		return activatedCorrector.getHotPointY();
	    return defaultCorrector.getHotPointY();
	}
	@Override public String getTabSeq()
	{
	    if (activatedCorrector != null)
		return activatedCorrector.getTabSeq();
	    return defaultCorrector.getTabSeq();
	}
	@Override public ModificationResult deleteChar(int pos, int lineIndex)
	{
	    if (activatedCorrector != null)
		return activatedCorrector.deleteChar(pos, lineIndex);
	    return defaultCorrector.deleteChar(pos, lineIndex);
	}
	@Override public ModificationResult deleteRegion(int fromX, int fromY, int toX, int toY)
	{
	    if (activatedCorrector != null)
		return activatedCorrector.deleteRegion(fromX, fromY, toX, toY);
	    return defaultCorrector.deleteRegion(fromX, fromY, toX, toY);
	}
	@Override public ModificationResult insertRegion(int x, int y, String[] lines)
	{
	    NullCheck.notNullItems(lines, "lines");
	    if (activatedCorrector != null)
		return activatedCorrector.insertRegion(x, y, lines);
	    return defaultCorrector.insertRegion(x, y, lines);
	}
	@Override public ModificationResult putChars(int pos, int lineIndex, String str)
	{
	    NullCheck.notNull(str, "str");
	    if (activatedCorrector != null)
		return activatedCorrector.putChars(pos, lineIndex, str);
	    return defaultCorrector.putChars(pos, lineIndex, str);
	}
	@Override public ModificationResult mergeLines(int firstLineIndex)
	{
	    if (activatedCorrector != null)
		return activatedCorrector.mergeLines(firstLineIndex);
	    return defaultCorrector.mergeLines(firstLineIndex);
	}
	@Override public ModificationResult splitLine(int pos, int lineIndex)
	{
	    if (activatedCorrector != null)
		return activatedCorrector.splitLine(pos, lineIndex);
	    return defaultCorrector.splitLine(pos, lineIndex);
	}
	@Override public ModificationResult doEditAction(TextEditAction action)
	{
	    if (activatedCorrector != null)
		return activatedCorrector.doEditAction(action); else
		return defaultCorrector.doEditAction(action);
	}
    }

    static public class EmptyCorrector implements MultilineEditCorrector
    {
	protected final MultilineEditCorrector basicCorrector;
	public EmptyCorrector(MultilineEditCorrector basicCorrector)
	{
	    NullCheck.notNull(basicCorrector, "basicCorrector");
	    this.basicCorrector = basicCorrector;
	}
	public MultilineEditCorrector getBasicCorrector()
	{
	    return basicCorrector;
	}
	@Override public int getLineCount()
	{
	    return basicCorrector.getLineCount();
	}
	@Override public String getLine(int index)
	{
	    return basicCorrector.getLine(index);
	}
	@Override public int getHotPointX()
	{
	    return basicCorrector.getHotPointX();
	}
	@Override public int getHotPointY()
	{
	    return basicCorrector.getHotPointY();
	}
	@Override public String getTabSeq()
	{
	    return basicCorrector.getTabSeq();
	}
	@Override public ModificationResult deleteChar(int pos, int lineIndex)
	{
	    return basicCorrector.deleteChar(pos, lineIndex);
	}
	@Override public ModificationResult deleteRegion(int fromX, int fromY, int toX, int toY)
	{
	    return basicCorrector.deleteRegion(fromX, fromY, toX, toY);
	}
	@Override public ModificationResult insertRegion(int x, int y, String[] lines)
	{
	    NullCheck.notNullItems(lines, "lines");
	    return basicCorrector.insertRegion(x, y, lines);
	}
	@Override public ModificationResult putChars(int pos, int lineIndex, String str)
	{
	    NullCheck.notNull(str, "str");
	    return basicCorrector.putChars(pos, lineIndex, str);
	}
	@Override public ModificationResult mergeLines(int firstLineIndex)
	{
	    return basicCorrector.mergeLines(firstLineIndex);
	}
	@Override public ModificationResult splitLine(int pos, int lineIndex)
	{
	    return basicCorrector.splitLine(pos, lineIndex);
	}
	@Override public ModificationResult doEditAction(TextEditAction action)
	{
	    return basicCorrector.doEditAction(action);
	}
    }
}
