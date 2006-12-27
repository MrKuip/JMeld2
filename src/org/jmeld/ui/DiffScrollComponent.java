package org.jmeld.ui;

import org.jmeld.diff.*;
import org.jmeld.ui.text.*;
import org.jmeld.ui.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class DiffScrollComponent
       extends JComponent
       implements ChangeListener
{
  private BufferDiffPanel diffPanel;
  private int             fromPanelIndex;
  private int             toPanelIndex;
  private List<Command>   commands;
  private Object          antiAlias;

  public DiffScrollComponent(
    BufferDiffPanel diffPanel,
    int             fromPanelIndex,
    int             toPanelIndex)
  {
    this.diffPanel = diffPanel;
    this.fromPanelIndex = fromPanelIndex;
    this.toPanelIndex = toPanelIndex;

    getFromPanel().getScrollPane().getViewport().addChangeListener(this);
    getToPanel().getScrollPane().getViewport().addChangeListener(this);

    addMouseListener(getMouseListener());
    addMouseWheelListener(getMouseWheelListener());
  }

  public void stateChanged(ChangeEvent event)
  {
    repaint();
  }

  private MouseWheelListener getMouseWheelListener()
  {
    return new MouseWheelListener()
      {
        public void mouseWheelMoved(MouseWheelEvent me)
        {
          diffPanel.toNextDelta(me.getWheelRotation() > 0);
          repaint();
        }
      };
  }

  private MouseListener getMouseListener()
  {
    return new MouseAdapter()
      {
        public void mouseClicked(MouseEvent me)
        {
          executeCommand((double) me.getX(), (double) me.getY());
        }
      };
  }

  public boolean executeCommand(
    double x,
    double y)
  {
    if (commands == null)
    {
      return false;
    }

    for (Command command : commands)
    {
      if (command.contains(x, y))
      {
        command.execute();
        return true;
      }
    }

    return false;
  }

  public void paintComponent(Graphics g)
  {
    Rectangle  r;
    int        middle;
    Graphics2D g2;

    g2 = (Graphics2D) g;

    r = g.getClipBounds();
    g2.setColor(getBackground());
    g2.fill(r);

    middle = r.height / 2;
    g2.setColor(Color.LIGHT_GRAY);
    g2.drawLine(r.x + 20, r.y + middle, r.x + r.width - 20, r.y + middle);

    paintDiffs(g2);
  }

  private void paintDiffs(Graphics2D g2)
  {
    JViewport        viewportFrom;
    JViewport        viewportTo;
    JTextComponent   editorFrom;
    JTextComponent   editorTo;
    JMRevision       revision;
    BufferDocumentIF bdFrom;
    BufferDocumentIF bdTo;
    int              firstLineFrom;
    int              lastLineFrom;
    int              firstLineTo;
    int              lastLineTo;
    int              offset;
    Rectangle        r;
    Point            p;
    JMChunk          original;
    JMChunk          revised;
    Rectangle        viewportRect;
    Rectangle        fromRect;
    Rectangle        toRect;
    int              fromLine;
    int              toLine;
    int              x;
    int              y;
    int              width;
    int              height;
    Rectangle        bounds;
    int              x0;
    int              y0;
    int              x1;
    int              y1;
    Color            color;
    Color            darkerColor;
    Polygon          shape;
    Rectangle        rect;
    boolean          selected;
    int              selectionWidth;
    FilePanel        fromPanel;
    FilePanel        toPanel;

    bounds = g2.getClipBounds();

    revision = diffPanel.getCurrentRevision();
    if (revision == null)
    {
      return;
    }

    commands = new ArrayList<Command>();

    antiAlias = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

    // From side:
    fromPanel = getFromPanel();
    viewportFrom = fromPanel.getScrollPane().getViewport();
    editorFrom = fromPanel.getEditor();
    bdFrom = fromPanel.getBufferDocument();
    r = viewportFrom.getViewRect();

    // Calculate firstLine shown of the first document. 
    p = new Point(r.x, r.y);
    offset = editorFrom.viewToModel(p);
    firstLineFrom = bdFrom.getLineForOffset(offset);

    // Calculate lastLine shown of the first document. 
    p = new Point(r.x, r.y + r.height);
    offset = editorFrom.viewToModel(p);
    bdFrom = fromPanel.getBufferDocument();
    lastLineFrom = bdFrom.getLineForOffset(offset) + 1;

    // To side:
    toPanel = getToPanel();
    viewportTo = toPanel.getScrollPane().getViewport();
    editorTo = toPanel.getEditor();
    bdTo = toPanel.getBufferDocument();
    r = viewportTo.getViewRect();

    // Calculate firstLine shown of the second document. 
    p = new Point(r.x, r.y);
    offset = editorTo.viewToModel(p);
    firstLineTo = bdTo.getLineForOffset(offset);

    // Calculate lastLine shown of the second document. 
    p = new Point(r.x, r.y + r.height);
    offset = editorTo.viewToModel(p);
    lastLineTo = bdTo.getLineForOffset(offset) + 1;

    int count = 0;

    try
    {
      // Draw only the delta's that have some line's drawn in one of the viewports.
      for (JMDelta delta : revision.getDeltas())
      {
        original = delta.getOriginal();
        revised = delta.getRevised();

        // This delta is before the firstLine of the screen: Keep on searching!
        if (original.getAnchor() + original.getSize() < firstLineFrom
          && revised.getAnchor() + revised.getSize() < firstLineTo)
        {
          continue;
        }

        // This delta is after the lastLine of the screen: stop! 
        if (original.getAnchor() > lastLineFrom
          && revised.getAnchor() > lastLineTo)
        {
          break;
        }

        selected = (delta == diffPanel.getSelectedDelta());

        // OK, this delta has some visible lines. Now draw it!
        color = RevisionUtil.getColor(delta);
        darkerColor = RevisionUtil.getDarkerColor(delta);
        g2.setColor(color);

        // Draw original chunk:
        fromLine = original.getAnchor();
        toLine = original.getAnchor() + original.getSize();
        viewportRect = viewportFrom.getViewRect();
        offset = bdFrom.getOffsetForLine(fromLine);
        fromRect = editorFrom.modelToView(offset);
        offset = bdFrom.getOffsetForLine(toLine);
        toRect = editorFrom.modelToView(offset);

        x = 0;
        y = fromRect.y - viewportRect.y + 1;
        y = y < 0 ? 0 : y;
        width = 10;
        height = 0;
        if (fromRect.y <= viewportRect.y
          && toRect.y <= viewportRect.y + viewportRect.height)
        {
          height = toRect.y - viewportRect.y - 1;
        }
        else if (fromRect.y > viewportRect.y
          && toRect.y > viewportRect.y + viewportRect.height)
        {
          height = viewportRect.y + viewportRect.height - fromRect.y - 1;
        }
        else if (fromRect.y > viewportRect.y
          && toRect.y <= viewportRect.y + viewportRect.height)
        {
          height = toRect.y - fromRect.y - 1;
        }
        else if (fromRect.y <= viewportRect.y
          && toRect.y > viewportRect.y + viewportRect.height)
        {
          height = viewportRect.height - 1;
        }

        x0 = x + width;
        y0 = y;

        if (height > 0)
        {
          g2.setColor(color);
          g2.fillRect(x, y, width, height);
        }

        g2.setColor(darkerColor);
        g2.drawLine(x, y, x + width, y);
        if (height > 0)
        {
          g2.drawLine(x + width, y, x + width, y + height);
          g2.drawLine(x, y + height, x + width, y + height);
        }

        if (selected)
        {
          x = x + width + 1;
          selectionWidth = 5;

          g2.setColor(Color.yellow);
          g2.fillRect(x, y, selectionWidth, height);
          g2.setColor(Color.yellow.darker());
          g2.drawLine(x, y, x + selectionWidth, y);
          if (height > 0)
          {
            g2.drawLine(x + selectionWidth, y, x + selectionWidth, y + height);
            g2.drawLine(x, y + height, x + selectionWidth, y + height);
          }
        }

        // Draw revised chunk:
        fromLine = revised.getAnchor();
        toLine = revised.getAnchor() + revised.getSize();
        viewportRect = viewportTo.getViewRect();
        offset = bdTo.getOffsetForLine(fromLine);
        fromRect = editorTo.modelToView(offset);
        offset = bdTo.getOffsetForLine(toLine);
        toRect = editorTo.modelToView(offset);

        x = bounds.x + bounds.width - 10;
        y = fromRect.y - viewportRect.y + 1;
        y = y < 0 ? 0 : y;
        width = 10;
        height = 0;
        if (fromRect.y <= viewportRect.y
          && toRect.y <= viewportRect.y + viewportRect.height)
        {
          height = toRect.y - viewportRect.y - 1;
        }
        else if (fromRect.y > viewportRect.y
          && toRect.y > viewportRect.y + viewportRect.height)
        {
          height = viewportRect.y + viewportRect.height - fromRect.y - 1;
        }
        else if (fromRect.y > viewportRect.y
          && toRect.y <= viewportRect.y + viewportRect.height)
        {
          height = toRect.y - fromRect.y - 1;
        }
        else if (fromRect.y <= viewportRect.y
          && toRect.y > viewportRect.y + viewportRect.height)
        {
          height = viewportRect.height - 1;
        }

        x1 = x;
        y1 = y;

        if (height > 0)
        {
          g2.setColor(color);
          g2.fillRect(x, y, width, height);
        }

        g2.setColor(darkerColor);
        g2.drawLine(x, y, x + width, y);
        if (height > 0)
        {
          g2.drawLine(x, y, x, y + height);
          g2.drawLine(x, y + height, x + width, y + height);
        }

        if (selected)
        {
          selectionWidth = 5;
          x = x - selectionWidth;

          g2.setColor(Color.yellow);
          g2.fillRect(x, y, selectionWidth, height);
          g2.setColor(Color.yellow.darker());
          g2.drawLine(x, y, x + selectionWidth, y);
          if (height > 0)
          {
            g2.drawLine(x, y, x, y + height);
            g2.drawLine(x, y + height, x + selectionWidth, y + height);
          }
          g2.setColor(color);
        }

        // Draw the chunk connection:
        g2.setColor(darkerColor);
        g2.drawLine(x0, y0, x0 + 15, y0);
        setAntiAlias(g2);
        g2.drawLine(x0 + 15, y0, x1 - 15, y1);
        resetAntiAlias(g2);
        g2.drawLine(x1 - 15, y1, x1, y1);

        // Draw merge revised->original command.
        shape = new Polygon();
        shape.addPoint(x0, y0);
        shape.addPoint(x0 + 11, y0 - 4);
        shape.addPoint(x0 + 11, y0 + 4);
        setAntiAlias(g2);
        g2.setColor(color);
        g2.fill(shape);
        g2.setColor(darkerColor);
        g2.draw(shape);
        resetAntiAlias(g2);
        commands.add(
          new DiffChangeCommand(shape, delta, toPanelIndex, fromPanelIndex));

        // Draw delete original command
        if (original.getSize() > 0)
        {
          g2.setColor(Color.red);
          g2.drawLine(x0 + 3 - width, y0 + 3, x0 + 7 - width, y0 + 7);
          g2.drawLine(x0 + 7 - width, y0 + 3, x0 + 3 - width, y0 + 7);
          rect = new Rectangle(x0 + 2 - width, y0 + 2, 6, 6);
          commands.add(
            new DiffDeleteCommand(rect, delta, fromPanelIndex, toPanelIndex));
        }

        // Draw merge original->revised command.
        shape = new Polygon();
        shape.addPoint(x1, y1);
        shape.addPoint(x1 - 11, y1 - 4);
        shape.addPoint(x1 - 11, y1 + 4);
        setAntiAlias(g2);
        g2.setColor(color);
        g2.fillPolygon(shape);
        g2.setColor(darkerColor);
        g2.drawPolygon(shape);
        resetAntiAlias(g2);
        commands.add(
          new DiffChangeCommand(shape, delta, fromPanelIndex, toPanelIndex));

        // Draw delete revision command
        if (revised.getSize() > 0)
        {
          g2.setColor(Color.red);
          g2.drawLine(x1 + 3, y1 + 3, x1 + 7, y1 + 7);
          g2.drawLine(x1 + 7, y1 + 3, x1 + 3, y1 + 7);
          rect = new Rectangle(x1 + 2, y1 + 2, 6, 6);
          commands.add(
            new DiffDeleteCommand(rect, delta, toPanelIndex, fromPanelIndex));
        }
      }
    }
    catch (BadLocationException ex)
    {
      ex.printStackTrace();
    }

    resetAntiAlias(g2);
  }

  class DiffChangeCommand
         extends Command
  {
    DiffChangeCommand(
      Shape   shape,
      JMDelta delta,
      int     fromIndex,
      int     toIndex)
    {
      super(shape, delta, fromIndex, toIndex);
    }

    public void execute()
    {
      diffPanel.setSelectedDelta(delta);
      diffPanel.runChange(fromIndex, toIndex);
    }
  }

  class DiffDeleteCommand
         extends Command
  {
    DiffDeleteCommand(
      Shape   shape,
      JMDelta delta,
      int     fromIndex,
      int     toIndex)
    {
      super(shape, delta, fromIndex, toIndex);
    }

    public void execute()
    {
      diffPanel.setSelectedDelta(delta);
      diffPanel.runDelete(fromIndex, toIndex);
    }
  }

  abstract class Command
  {
    Rectangle bounds;
    JMDelta   delta;
    int       fromIndex;
    int       toIndex;

    Command(
      Shape   shape,
      JMDelta delta,
      int     fromIndex,
      int     toIndex)
    {
      this.bounds = shape.getBounds();
      this.delta = delta;
      this.fromIndex = fromIndex;
      this.toIndex = toIndex;
    }

    boolean contains(
      double x,
      double y)
    {
      return bounds.contains(x, y);
    }

    public abstract void execute();
  }

  private void setAntiAlias(Graphics2D g2)
  {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);
  }

  private void resetAntiAlias(Graphics2D g2)
  {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias);
  }

  private FilePanel getFromPanel()
  {
    return diffPanel.getFilePanel(fromPanelIndex);
  }

  private FilePanel getToPanel()
  {
    return diffPanel.getFilePanel(toPanelIndex);
  }
}