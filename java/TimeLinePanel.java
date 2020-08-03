import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;

/**
 * Created by andreas on 18.10.14.
 */
public class TimeLinePanel extends JPanel {
  private final List<Integer> moteOrder;
  private final TimeCursor timeCursor;
  private final TimePerPixel timePerPixel;
  private final Map<Integer, ObservableValue<Boolean>> moteVisibilities;
  private final static int interMoteStrutSpacing = 5;
  private final BoundedTimeValue currentTime;

  public Map<Integer, ObservableValue<Boolean>> getMoteVisibilities() {
    return Collections.unmodifiableMap(moteVisibilities);
  }
  private Map<Integer, JComponent> nodeBoxes;
  private Map<Integer, JLabel> moteLabels;

  public TimeLinePanel(TimePerPixel timePerPixel, BoundedTimeValue currentTime) {
    super();
    this.currentTime = currentTime;
    currentTime.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        repaint();
      }
    });
    if (timePerPixel == null)
      throw new IllegalArgumentException();

    this.timePerPixel = timePerPixel;
    timePerPixel.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        repaint();
      }
    });
    this.timeCursor = new TimeCursor();
    timeCursor.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        repaint();
      }
    });

    this.moteVisibilities = new HashMap<Integer, ObservableValue<Boolean>>();
    this.nodeBoxes = new HashMap<Integer, JComponent>();
    setLayout(new GridBagLayout());
    moteOrder = new ArrayList<Integer>();
    moteLabels = new HashMap<Integer, JLabel>();
    setBorder(new EmptyBorder(20, 0, 0, 0));

    // mouse time listener
    MouseTimeListener mouseTimeListener = new MouseTimeListener() {
      @Override
      public void timePressed(Object source, long time, int extendedModifiers) {
        timeCursor.startAt(time, source);
        timeCursor.setEndTime(null);
      }

      @Override
      public void timeDragged(Object source, long time, int extendedModifiers) {
        timeCursor.setEndTime(time);
      }

      @Override
      public void timeReleased(Object source, long time, int extendedModifiers) {
        timeCursor.setEndTime(null);
      }

      @Override
      public void timeClicked(Object source, long time, int extendedModifiers) {
      }
    };
    TimePlot.addMouseTimeListener(mouseTimeListener);
  }

  public void addNodeBox(final int id, final Box nodeBox) {
//  GridBagConstraints:  gridx, gridy, gridwidth, gridheight, weightx, weighty, anchor, fill, insets, ipadx, ipady
    this.nodeBoxes.put(id, nodeBox);

    int gridy = moteOrder.size() + 1;
    moteOrder.add(id);

    final JLabel nodeLabel = new JLabel("" + id, SwingConstants.CENTER);
    nodeLabel.setPreferredSize(new Dimension(40, nodeLabel.getPreferredSize().height));
    moteLabels.put(id, nodeLabel);
    MouseListener labelMouseListener = new MouseListener() {
      private boolean started = false;
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {

      }

      @Override
      public void mousePressed(MouseEvent mouseEvent) {
        started = true;
      }

      @Override
      public void mouseReleased(MouseEvent mouseEvent) {
        Component c = SwingUtilities.getDeepestComponentAt(TimeLinePanel.this, nodeLabel.getX() + mouseEvent.getX(), nodeLabel.getY() + mouseEvent.getY());
        if (c != null && started && c instanceof JLabel) {
          JLabel otherNodeLabel = (JLabel) c;
          if (otherNodeLabel.getX() >= nodeLabel.getX() && otherNodeLabel.getX() <= nodeLabel.getX() + nodeLabel.getWidth()) {
            int otherId = Integer.parseInt(otherNodeLabel.getText());
            if (otherId != id) {
              moteOrder.remove(Integer.valueOf(id));
              moteOrder.add(moteOrder.indexOf(otherId), id);
              rebuildAfterReorder();
            }
          }
        }
        started = false;
      }

      @Override
      public void mouseEntered(MouseEvent mouseEvent) {
      }

      @Override
      public void mouseExited(MouseEvent mouseEvent) {
      }
    };
    nodeLabel.addMouseListener(labelMouseListener);

    final ObservableValue<Boolean> vis = new ObservableValue<Boolean>(true);
    moteVisibilities.put(id, vis);
    vis.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        nodeLabel.setVisible(vis.getValue());
        nodeBox.setVisible(vis.getValue());
      }
    });

    putNodeInGrid(moteOrder.size()-1);
  }

  private void putNodeInGrid(int iNode) {
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = iNode;
    Insets insets = new Insets(0, 0, interMoteStrutSpacing, 0);
    c.insets = insets;
    c.fill = GridBagConstraints.VERTICAL;
    add(moteLabels.get(moteOrder.get(iNode)), c);

    c = new GridBagConstraints();
    c.gridx = 1;
    c.gridy = iNode;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = insets;
    add(nodeBoxes.get(moteOrder.get(iNode)), c);
  }

  private void rebuildAfterReorder() {
    this.removeAll();
    for (int iNode = 0; iNode < moteOrder.size(); ++iNode)
      putNodeInGrid(iNode);
    revalidate();
    repaint();
  }

  public void moveCursorTo(long time) {
    timeCursor.setEndTime(null);
    timeCursor.startAt(time, null);
    moveTimeToCenter(time);
  }

  public void moveTimeToCenter(long time) {
    long halfVisibleTime = (long)(TimePlot.getDisplayedWidth() / 2d * timePerPixel.getValue());
    currentTime.setValue(time - halfVisibleTime);
  }

  public List<Integer> getMoteOrder() {
    return Collections.unmodifiableList(moteOrder);
  }

  public void setMoteOrder(List<Integer> newOrder) {
    if (newOrder == null)
      throw new IllegalArgumentException();

    for (int pos = 0; pos < newOrder.size(); ++pos) {
      int id = newOrder.get(pos);
      moteOrder.remove(Integer.valueOf(id));
      moteOrder.add(pos, id);
    }

    rebuildAfterReorder();
  }

  public ObservableValue<Boolean> getMoteVisibility(int id) {
    ObservableValue<Boolean> vis = moteVisibilities.get(id);
    if (vis == null)
      moteVisibilities.put(id, vis = new ObservableValue<Boolean>(true));
    return vis;
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);

    if (timeCursor.getStartTime() != null) {
      g.setColor(Color.BLACK);
      int timeLinesOffset = 0;
      for (JComponent noteBox : nodeBoxes.values()) {
        if (noteBox.isVisible()) {
          timeLinesOffset = noteBox.getX();
          break;
        }
      }
      // drag start
      int startX = timeLinesOffset + (int) ((timeCursor.getStartTime() - currentTime.getValue()) / timePerPixel.getValue());
      g.drawLine(startX, 0, startX, getHeight());
      int endX = startX;

      TimeValue t1 = new TimeValue(timeCursor.getStartTime() - TimePlot.startTime);
      String cursorText = String.format(
        "%d:%02d.%03d%03d",
        t1.minutes(),
        t1.remainingSeconds(),
        t1.remainingMilliSeconds(),
        t1.remainingMicroSeconds());

      if (timeCursor.getEndTime() != null) {
        // current position
        endX = timeLinesOffset + (int) ((timeCursor.getEndTime() - currentTime.getValue()) / timePerPixel.getValue());
        g.drawLine(endX, 0, endX, getHeight());

        TimeValue t2 = new TimeValue(timeCursor.getEndTime() - TimePlot.startTime);

        cursorText += String.format(
          " -> %d:%02d.%03d%03d (%ss)",
          t2.minutes(),
          t2.remainingSeconds(),
          t2.remainingMilliSeconds(),
          t2.remainingMicroSeconds(),
          FloatUtils.convert((timeCursor.getEndTime() - timeCursor.getStartTime()) * 1e-8, 2));

        Object source = timeCursor.getSource();
        if (source instanceof CurrentPlot) {
          Double averageCurrent = ((CurrentPlot) source).getTrace().averageIn(timeCursor.getStartTime(), timeCursor.getEndTime());
          if (averageCurrent != null)
            cursorText += String.format(", avg. current: %sA", FloatUtils.convert(averageCurrent / 1e3, 2));
        }
      }

      FontMetrics fm = g.getFontMetrics();
      Rectangle visibleRect = getVisibleRect();
      int textY = visibleRect.y + fm.getHeight();
      int textWidth = fm.stringWidth(cursorText);
      int textX = Math.max(Math.min(endX, visibleRect.x + visibleRect.width - textWidth), visibleRect.x);
      g.setColor(Color.LIGHT_GRAY);
      g.fillRect(textX, visibleRect.y, textWidth, fm.getHeight() + 4);
      g.setColor(Color.BLACK);
      g.drawString(cursorText, textX, textY);
    }
  }
}
