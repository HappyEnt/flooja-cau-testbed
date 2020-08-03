/**
 * Created by andreas on 21.10.14.
 */
import java.awt.event.MouseEvent;

public interface MouseTimeListener {
  /**
   * @param source
   * @param time
   * @param extendedModifiers see {@link java.awt.event.MouseEvent}'s .._DOWN_MASK constants
   */
  void timePressed(Object source, long time, int extendedModifiers);
  /**
   *
   * @param source
   * @param time
   * @param extendedModifiers see {@link java.awt.event.MouseEvent}'s .._DOWN_MASK constants
   */
  void timeDragged(Object source, long time, int extendedModifiers);
  /**
   *
   * @param source
   * @param time
   * @param extendedModifiers see {@link java.awt.event.MouseEvent}'s ..._DOWN_MASK constants
   */
  void timeReleased(Object source, long time, int extendedModifiers);
  /**
   *
   * @param source
   * @param time
   * @param extendedModifiers see {@link java.awt.event.MouseEvent}'s ..._DOWN_MASK constants
   */
  void timeClicked(Object source, long time, int extendedModifiers);
}
