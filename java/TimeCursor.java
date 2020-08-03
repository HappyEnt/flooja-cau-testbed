import java.util.Observable;

/**
* Created by andreas on 21.10.14.
*/
class TimeCursor extends Observable {
  private Long startTime;
  private Long endTime;
  private Object source;

  public TimeCursor() {
    super();
    startTime = endTime = null;
  }

  public void startAt(Long startTime, Object source) {
    this.startTime = startTime;
    setChanged();
    notifyObservers();
    this.source = source;
  }

  public boolean isActive() {
    return getStartTime() != null && getEndTime() != null;
  }

  public void setEndTime(Long endTime) {
    if (this.endTime != null && endTime == null)
      this.startTime = null;
    this.endTime = endTime;
    setChanged();
    notifyObservers();
  }

  public Long getStartTime() {
    return this.startTime;
  }

  public Long getEndTime() {
    return this.endTime;
  }

  public Object getSource() {
    return source;
  }
}
