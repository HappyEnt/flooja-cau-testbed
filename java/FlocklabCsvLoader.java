import java.io.*;
import java.math.BigInteger;

public abstract class FlocklabCsvLoader {
  private final BufferedReader fileReader;
  private long startTime;
  private long endTime;
  private int nSkipColumns;

  protected FlocklabCsvLoader(Reader reader) {
    assert reader != null;

    this.fileReader = new BufferedReader(reader);

    this.startTime = Long.MAX_VALUE;
    this.endTime = Long.MIN_VALUE;
  }

  public final void load() throws IOException {
    String line;

    while ((line = fileReader.readLine()) != null) {
      if (line.startsWith("#"))
        continue;

      String [] parts = line.split(",", 3);

      // time
      // TODO for now divide by ten because the plugin works in tenth of nanoseconds
      long time =  Long.parseLong(parts[0].trim())/10;

      // overseer id
      int overseerID = Integer.parseInt(parts[1].trim());


      startTime = Math.min(startTime, time);
      endTime = Math.max(startTime, time);
      processRow(time, overseerID, parts[2]);
    }
  }

  public final long getStartTime() {
    return startTime;
  }

  public final long getEndTime() {
    return endTime;
  }

  protected abstract void processRow(long time, int observerID, String rest);
}
