import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * Created by andreas on 28.10.14.
 */
public class MillisecondRuler extends JComponent implements MouseListener, MouseMotionListener {
  private static final long MILLISECOND = 100000;
  private final BoundedTimeValue currentTime;
  private final TimePerPixel timePerPixel;
  private static final int HEIGHT = 20;
  private Set<MouseTimeListener> mouseTimeListeners;

  public MillisecondRuler(BoundedTimeValue currentTime, TimePerPixel timePerPixel) {
    super();

    if (currentTime == null)
      throw new IllegalArgumentException();
    if (timePerPixel == null)
      throw new IllegalArgumentException();

    this.currentTime = currentTime;
    this.timePerPixel = timePerPixel;

    currentTime.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        MillisecondRuler.this.repaint();
      }
    });
    timePerPixel.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        MillisecondRuler.this.repaint();
      }
    });

    mouseTimeListeners = new HashSet<MouseTimeListener>();
    addMouseMotionListener(this);
    addMouseListener(this);
  }

  public void addMouseTimeListener(MouseTimeListener listener) {
    if (listener == null)
      throw new IllegalArgumentException();
    mouseTimeListeners.add(listener);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    g.setColor(Color.WHITE);
    g.fillRect(0, 0, getWidth(), getHeight());

    final double timePerPixel = this.timePerPixel.getValue();
    final int width = getWidth();

    long drawTime = -((currentTime.getValue() - TimePlot.startTime) % (10 * MILLISECOND));
    int milliSeconds = 0;
    while (drawTime < 0) {
      drawTime += MILLISECOND;
      milliSeconds = (milliSeconds + 1) % 10;
    }
    int x = (int) (drawTime / timePerPixel);

    g.setColor(Color.GRAY);
    while (x <= width) {
      if (milliSeconds == 0)
        g.drawLine(x, 0, x, (int) (.75 * HEIGHT));
      else
        g.drawLine(x, 0, x, (int) (.375 * HEIGHT));
      milliSeconds = (milliSeconds + 1) % 10;
      drawTime += MILLISECOND;
      x = (int) (drawTime / timePerPixel);
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(super.getPreferredSize().width, HEIGHT);
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(super.getMaximumSize().width, HEIGHT);
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(super.getMinimumSize().width, HEIGHT);
  }

  @Override
  public void mouseClicked(MouseEvent mouseEvent) {

  }

  @Override
  public void mousePressed(MouseEvent mouseEvent) {
    long time = (long) (mouseEvent.getX() * timePerPixel.getValue()) + currentTime.getValue();
    for (MouseTimeListener l : mouseTimeListeners) {
      l.timePressed(this, time, mouseEvent.getModifiersEx());
    }
  }

  @Override
  public void mouseReleased(MouseEvent mouseEvent) {
    long time = (long) (mouseEvent.getX() * timePerPixel.getValue()) + currentTime.getValue();
    for (MouseTimeListener l : mouseTimeListeners) {
      l.timeReleased(this, time, mouseEvent.getModifiersEx());
    }
  }

  @Override
  public void mouseEntered(MouseEvent mouseEvent) {

  }

  @Override
  public void mouseExited(MouseEvent mouseEvent) {

  }

  @Override
  public void mouseDragged(MouseEvent mouseEvent) {
    long time = (long) (mouseEvent.getX() * timePerPixel.getValue()) + currentTime.getValue();
    for (MouseTimeListener l : mouseTimeListeners) {
      l.timeDragged(this, time, mouseEvent.getModifiersEx());
    }
  }

  @Override
  public void mouseMoved(MouseEvent mouseEvent) {

  }
}
