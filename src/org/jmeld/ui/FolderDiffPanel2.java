/*
   JMeld is a visual diff and merge tool.
   Copyright (C) 2007  Kees Kuip
   This library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2.1 of the License, or (at your option) any later version.
   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.
   You should have received a copy of the GNU Lesser General Public
   License along with this library; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor,
   Boston, MA  02110-1301  USA
 */
package org.jmeld.ui;

import org.jdesktop.swingx.decorator.*;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.treetable.*;
import org.jmeld.settings.*;
import org.jmeld.ui.action.*;
import org.jmeld.ui.swing.*;
import org.jmeld.ui.swing.table.*;
import org.jmeld.ui.util.*;
import org.jmeld.ui.util.*;
import org.jmeld.util.file.*;
import org.jmeld.util.node.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class FolderDiffPanel2
       extends FolderDiffForm
{
  private JMeldPanel    mainPanel;
  private FolderDiff2   diff;
  private ActionHandler actionHandler;

  FolderDiffPanel2(
    JMeldPanel  mainPanel,
    FolderDiff2 diff)
  {
    this.mainPanel = mainPanel;
    this.diff = diff;

    init();
  }

  private void init()
  {
    actionHandler = new ActionHandler();

    hierarchyComboBox.setModel(
      new DefaultComboBoxModel(DirectorySettings.DirectoryView.values()));
    hierarchyComboBox.setSelectedItem(getSettings().getView());
    hierarchyComboBox.setFocusable(false);

    initActions();

    onlyRightButton.setText(null);
    onlyRightButton.setIcon(ImageUtil.getImageIcon("jmeld_only-right"));
    onlyRightButton.setFocusable(false);
    onlyRightButton.setSelected(getSettings().getOnlyRight());

    leftRightChangedButton.setText(null);
    leftRightChangedButton.setIcon(
      ImageUtil.getImageIcon("jmeld_left-right-changed"));
    leftRightChangedButton.setFocusable(false);
    leftRightChangedButton.setSelected(getSettings().getLeftRightChanged());

    onlyLeftButton.setText(null);
    onlyLeftButton.setIcon(ImageUtil.getImageIcon("jmeld_only-left"));
    onlyLeftButton.setFocusable(false);
    onlyLeftButton.setSelected(getSettings().getOnlyLeft());

    leftRightUnChangedButton.setText(null);
    leftRightUnChangedButton.setIcon(
      ImageUtil.getImageIcon("jmeld_left-right-unchanged"));
    leftRightUnChangedButton.setFocusable(false);
    leftRightUnChangedButton.setSelected(
      getSettings().getLeftRightUnChanged());

    expandAllButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    expandAllButton.setContentAreaFilled(false);
    expandAllButton.setText(null);
    expandAllButton.setIcon(ImageUtil.getSmallImageIcon("stock_expand-all"));
    expandAllButton.setPressedIcon(
      ImageUtil.createDarkerIcon((ImageIcon) expandAllButton.getIcon()));
    expandAllButton.setFocusable(false);

    collapseAllButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    collapseAllButton.setContentAreaFilled(false);
    collapseAllButton.setText(null);
    collapseAllButton.setIcon(
      ImageUtil.getSmallImageIcon("stock_collapse-all"));
    collapseAllButton.setPressedIcon(
      ImageUtil.createDarkerIcon((ImageIcon) collapseAllButton.getIcon()));
    collapseAllButton.setFocusable(false);

    folder1Label.init();
    folder1Label.setText(
      diff.getLeftFolderName(),
      diff.getRightFolderName());

    folder2Label.init();
    folder2Label.setText(
      diff.getRightFolderName(),
      diff.getLeftFolderName());

    folderTreeTable.setTreeTableModel(
      new FolderDiffTreeTableModel(getRootNode()));
    folderTreeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    folderTreeTable.setToggleClickCount(1);
    folderTreeTable.setTerminateEditOnFocusLost(false);
    folderTreeTable.setRowSelectionAllowed(true);

    folderTreeTable.setHighlighters(
      new HighlighterPipeline(
        new Highlighter[]
        {
          new AlternateRowHighlighter(
            Color.white,
            Colors.getTableRowHighLighterColor(),
            Color.black),
        }));
  }

  private void initActions()
  {
    MeldAction action;

    action = actionHandler.createAction(this, "SelectNextRow");
    installKey("DOWN", action);

    action = actionHandler.createAction(this, "SelectPreviousRow");
    installKey("UP", action);

    action = actionHandler.createAction(this, "NextNode");
    installKey("RIGHT", action);

    action = actionHandler.createAction(this, "PreviousNode");
    installKey("LEFT", action);

    action = actionHandler.createAction(this, "OpenFileComparison");
    installKey("ENTER", action);

    action = actionHandler.createAction(this, "OpenFileComparisonBackground");
    installKey("alt ENTER", action);

    action = actionHandler.createAction(this, "ExpandAll");
    expandAllButton.setAction(action);

    action = actionHandler.createAction(this, "CollapseAll");
    collapseAllButton.setAction(action);

    action = actionHandler.createAction(this, "Filter");
    onlyRightButton.setAction(action);
    leftRightChangedButton.setAction(action);
    onlyLeftButton.setAction(action);
    leftRightUnChangedButton.setAction(action);
    hierarchyComboBox.setAction(action);
  }

  private void installKey(
    String     key,
    MeldAction action)
  {
    SwingUtil.installKey(folderTreeTable, key, action);
  }

  public String getTitle()
  {
    return diff.getLeftFolderShortName() + " - "
    + diff.getRightFolderShortName();
  }

  private TreeNode getRootNode()
  {
    return filter(diff.getRootNode());
  }

  private TreeNode filter(JMDiffNode diffNode)
  {
    List<JMDiffNode>    nodes;
    Map<String, UINode> map;
    UINode              uiParentNode;
    UINode              uiNode;
    String              parentName;
    UINode              rootNode;
    JMDiffNode          parent;

    // Filter the nodes:
    nodes = new ArrayList();
    for (JMDiffNode node : diff.getNodes())
    {
      if (!node.isLeaf())
      {
        continue;
      }

      if (node.isCompareEqual(JMDiffNode.Compare.Equal))
      {
        if (leftRightUnChangedButton.isSelected())
        {
          nodes.add(node);
        }
      }
      else if (node.isCompareEqual(JMDiffNode.Compare.NotEqual))
      {
        if (leftRightChangedButton.isSelected())
        {
          nodes.add(node);
        }
      }
      else if (node.isCompareEqual(JMDiffNode.Compare.RightMissing))
      {
        if (onlyLeftButton.isSelected())
        {
          nodes.add(node);
        }
      }
      else if (node.isCompareEqual(JMDiffNode.Compare.LeftMissing))
      {
        if (onlyRightButton.isSelected())
        {
          nodes.add(node);
        }
      }
    }

    rootNode = new UINode("<root>", false);

    // Build the hierarchy:
    if (hierarchyComboBox.getSelectedItem() == DirectorySettings.DirectoryView.packageView)
    {
      map = new HashMap<String, UINode>();

      for (JMDiffNode node : nodes)
      {
        parent = node.getParent();
        uiNode = new UINode(node);

        if (parent != null)
        {
          parentName = parent.getName();
          uiParentNode = map.get(parentName);
          if (uiParentNode == null)
          {
            uiParentNode = new UINode(parentName, false);
            rootNode.addChild(uiParentNode);
            map.put(parentName, uiParentNode);
          }
          uiParentNode.addChild(uiNode);
        }
        else
        {
          rootNode.addChild(uiNode);
        }
      }
    }
    else if (hierarchyComboBox.getSelectedItem() == DirectorySettings.DirectoryView.fileView)
    {
      for (JMDiffNode node : nodes)
      {
        rootNode.addChild(new UINode(node));
      }
    }
    else if (hierarchyComboBox.getSelectedItem() == DirectorySettings.DirectoryView.directoryView)
    {
      map = new HashMap<String, UINode>();

      for (JMDiffNode node : nodes)
      {
        addDirectoryViewNode(rootNode, map, node);
      }
    }

    return rootNode;
  }

  private void addDirectoryViewNode(
    UINode              rootNode,
    Map<String, UINode> map,
    JMDiffNode          node)
  {
    /*
    UINode     uiNode;
    JMDiffNode parent;
    UINode     uiParentNode;
    String     parentName;

    uiNode = new UINode(node);

    parent = node.getParent();
    if (parent != null)
    {
      parentName = parent.getName();
      uiParentNode = map.get(parentName);
      if (uiParentNode == null)
      {
        addDirectoryViewNode(rootNode, map, parent);

        if(parent.getParent
        uiParentNode = new UINode(parentName, false);

        uiParentNode = map.get(parentName);
        addChild(uiParentNode);
        map.put(parentName, uiParentNode);

        System.out.println("parentName = " + parentName);
        uiParentNode = map.get(parentName);
        System.out.println("node = " + uiParentNode);
      }

      uiParentNode.addChild(new UINode(node));
    }
    else
    {
      rootNode.addChild(uiNode);
      map.put(uiNode.getName(), uiNode);
      System.out.println("put(" + uiNode.getName() + ", " + uiNode);
    }
    */
  }

  public void doSelectPreviousRow(ActionEvent ae)
  {
    int row;

    row = folderTreeTable.getSelectedRow() - 1;
    row = row < 0 ? (folderTreeTable.getRowCount() - 1) : row;
    folderTreeTable.setRowSelectionInterval(row, row);
    folderTreeTable.scrollRowToVisible(row);
  }

  public void doSelectNextRow(ActionEvent ae)
  {
    int row;

    row = folderTreeTable.getSelectedRow() + 1;
    row = row >= folderTreeTable.getRowCount() ? 0 : row;
    folderTreeTable.setRowSelectionInterval(row, row);
    folderTreeTable.scrollRowToVisible(row);
  }

  public void doNextNode(ActionEvent ae)
  {
    int row;

    row = folderTreeTable.getSelectedRow();
    if (row == -1)
    {
      return;
    }

    if (folderTreeTable.isCollapsed(row))
    {
      folderTreeTable.expandRow(row);
    }

    doSelectNextRow(ae);
  }

  public void doPreviousNode(ActionEvent ae)
  {
    int row;

    row = folderTreeTable.getSelectedRow();
    if (row == -1)
    {
      return;
    }

    if (folderTreeTable.isExpanded(row))
    {
      folderTreeTable.collapseRow(row);
    }

    doSelectPreviousRow(ae);
  }

  public void doOpenFileComparisonBackground(ActionEvent ae)
  {
    doOpenFileComparison(ae, true);
  }

  public void doOpenFileComparison(ActionEvent ae)
  {
    doOpenFileComparison(ae, false);
  }

  private void doOpenFileComparison(
    ActionEvent ae,
    boolean     background)
  {
    int        row;
    TreePath   path;
    UINode     node;
    JMDiffNode diffNode;

    row = folderTreeTable.getSelectedRow();
    if (row == -1)
    {
      return;
    }

    path = folderTreeTable.getPathForRow(row);
    if (path == null)
    {
      return;
    }

    node = (UINode) path.getLastPathComponent();
    if (node == null)
    {
      return;
    }

    diffNode = node.getDiffNode();
    if (diffNode == null)
    {
      return;
    }

    mainPanel.openFileComparison(diffNode, background);
  }

  @Override
  public boolean checkExit()
  {
    return false;
  }

  public void doExpandAll(ActionEvent ae)
  {
    folderTreeTable.expandAll();
  }

  public void doCollapseAll(ActionEvent ae)
  {
    folderTreeTable.collapseAll();
  }

  public void doFilter(ActionEvent ae)
  {
    ((JMTreeTableModel) folderTreeTable.getTreeTableModel()).setRoot(
      getRootNode());
  }

  private DirectorySettings getSettings()
  {
    return JMeldSettings.getInstance().getDirectory();
  }
}
