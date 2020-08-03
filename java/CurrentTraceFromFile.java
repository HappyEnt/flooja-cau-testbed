import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andreas on 03.11.14.
 */
public class CurrentTraceFromFile implements CurrentTrace {
  private static final int TIMESTAMP_SIZE = 8;
  private static final int VALUE_SIZE = 8;
  private static final int RECORD_SIZE = TIMESTAMP_SIZE + VALUE_SIZE;
  private static final int FOOTER_SIZE = 8;

  private final RandomAccessFile file;
  private final long nSamples;

  private final Long startTime;
  private final Long endTime;
  private final int nodeId;

  private final byte[] recordBuffer;
  private final ByteBuffer byteBuffer;
  private final long samplingPeriod;


  public CurrentTraceFromFile(File filePath) throws IOException {
    nodeId = Integer.parseInt(filePath.getName());
    recordBuffer = new byte[RECORD_SIZE];
    byteBuffer = ByteBuffer.wrap(recordBuffer).order(ByteOrder.LITTLE_ENDIAN);

    file = new RandomAccessFile(filePath, "r");
    if ((file.length() - FOOTER_SIZE) % RECORD_SIZE != 0)
      throw new IllegalArgumentException(String.format("illegal file size. Footer missing?"));
    nSamples = (file.length() - FOOTER_SIZE) / RECORD_SIZE;
    file.seek(file.length() - FOOTER_SIZE);
    file.readFully(recordBuffer, 0, 8);
    samplingPeriod = byteBuffer.getLong(0);
    if (nSamples > 0) {
      file.seek(0);
      file.readFully(recordBuffer, 0, 8);
      startTime = byteBuffer.getLong(0);
      file.seek((nSamples - 1) * RECORD_SIZE);
      file.readFully(recordBuffer, 0, 8);
      endTime = byteBuffer.getLong(0);
    } else {
      startTime = endTime = null;
    }
  }

  @Override
  public int getMoteId() {
    return nodeId;
  }

  @Override
  public Long getFirstTime() {
    return startTime;
  }

  @Override
  public Long getLastTime() {
    return endTime;
  }

  @Override
  public Double interpolationAt(long time) {
    Double result = null;
    try {
      long pos = searchInFileBinary(time);
      if (pos >= 0) {
        loadSample(pos);
        result = readValue();
      } else {
        long lowerPos = -(pos + 1) - 1;
        long upperPos = -(pos + 1);
        if (lowerPos >= 0 && upperPos < nSamples) {
          loadSample(upperPos);
          double upperVal = readValue();
          long upperTime = readTime();
          loadSample(lowerPos);
          long lowerTime = readTime();
          double lowerVal = readValue();
          if (lowerTime != upperTime)
            result = lowerVal + (time - lowerTime) / (double)(upperTime - lowerTime) * (upperVal - lowerVal);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  @Override
  public Double averageIn(long timeA, long timeB) {
    if (timeA > timeB) {
      long end = timeA;
      timeA = timeB;
      timeB = end;
    }
    try {
      long startPos = searchInFileBinary(timeA);
      if (startPos < 0)
        startPos = -(startPos + 1);
      long endPos = searchInFileBinary(timeB);
      if (endPos < 0)
        endPos = -(endPos + 1) - 1;
      long count = 0;
      double sum = 0d;
      for (long pos = startPos; pos < nSamples && pos <= endPos; ++pos) {
        ++count;
        loadSample(pos);
        sum += readValue();
      }
      if (count > 0)
        return sum / count;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void loadSample(long iSample) throws IOException {
    file.seek(iSample * RECORD_SIZE);
    file.readFully(recordBuffer, 0, RECORD_SIZE);
  }

  private long readTime() {
    return byteBuffer.getLong(0);
  }

  private double readValue() {
    return byteBuffer.getDouble(TIMESTAMP_SIZE);
  }

  private final List<Long> times = new ArrayList<Long>();
  private final List<Double> values = new ArrayList<Double>();
//  private final List<Samples<Double>> samplesChunks = new ArrayList<Samples<Double>>();
  private long cachedMaxDeltaT = Long.MAX_VALUE;
  private long cachedStartTime = Long.MAX_VALUE;
  private long cachedEndTime = Long.MIN_VALUE;

  @Override
  public SampleIterator<Double> getMeasurementsCovering(long startTime, long endTime, long maxDeltaT) throws IOException {
    if (nSamples > 0 && (maxDeltaT < cachedMaxDeltaT || startTime < cachedStartTime || endTime > cachedEndTime)) {
      times.clear();
      values.clear();

      long iSample = searchInFileBinary(startTime);
//      long iSample = searchInFileInterpolating(startTime);
      if (iSample < 0)
        iSample = Math.max(-(iSample + 2), 0);
      loadSample(iSample);


      long time = readTime();
      long dataMaxDeltaT = 0L;
      if (time <= endTime) {
        times.add(time);
        values.add(readValue());

        final long jumpWidth = Math.max(maxDeltaT / samplingPeriod, 1);

        do {
          long lastTime = time;
          iSample = Math.min(nSamples - 1, iSample + jumpWidth);

          loadSample(iSample);
          time = readTime();
          times.add(time);
          values.add(readValue());

          dataMaxDeltaT = Math.max(time - lastTime, dataMaxDeltaT);

        } while (time < endTime && iSample < nSamples - 1);
      }

      cachedStartTime = startTime;
      cachedEndTime = endTime;
      cachedMaxDeltaT = Math.min(maxDeltaT, dataMaxDeltaT);
    }
    return new ListSampleIterator<Double>(times, values);
  }


  public boolean hasMeasurements() {
    return startTime != null;
  }

  /**
   *
   * @param searchTime the time to search in the file
   * @return index of the sample with the specified time or -(index_where_it_should_be_inserted + 1)
   */
  private long searchInFileBinary(final long searchTime) throws IOException {
    long lower = 0;
    long upper = nSamples;

    long time = searchTime + 1; // just some value not equal to searchTime
    while (lower < upper) {
      long current = lower + (upper - lower) / 2;
      time = getTime(current);
      if (searchTime > time)
        lower = current + 1;
      else
        upper = current;
    }

    if (time == searchTime)
      return upper;
    else
      return -(upper + 1);
  }

  private long searchInFileInterpolating(final long searchTime) throws IOException {
    if (nSamples == 0)
      return -1;

    long lower = 0;
    long lowerTime = getTime(lower);
    long upper = nSamples - 1;
    long upperTime = getTime(upper);

    while (lowerTime <= searchTime && searchTime <= upperTime) {
      long mid = lower + (searchTime - lowerTime) / (upperTime - lowerTime) * (upper - lower);
      long time = getTime(mid);
      if (time < searchTime) {
        lower = mid + 1;
        lowerTime = getTime(lower);
      } else if (searchTime < time) {
        upper = mid - 1;
        upperTime = getTime(upper);
      } else
        return mid;
    }
    if (searchTime < lowerTime)
      return -lower - 1;
    else
      return -(upper + 1) - 1;
  }

  /**
   *
   * @param pos  0 <= _ < nSamples
   */
  private long getTime(long pos) throws IOException {
    file.seek(pos * RECORD_SIZE);
    file.readFully(recordBuffer, 0, TIMESTAMP_SIZE);
    return byteBuffer.getLong(0);
  }


//  private void defineNext() {
//    try {
//      loadSample(pos);
//      nextTime = readTime();
//      nextValue = readValue();
//      for (long i = pos + 1; i < nSamples; ++i) {
//        loadSample(i);
//        long time = readTime();
//        if (time - currentTime <= maxDeltaT) {
//          pos = i;
//          nextTime = time;
//          nextValue = readValue();
//        } else
//          break;
//      }
//    } catch (IOException e) {
//      e.printStackTrace();
//      pos = -1;
//    }
//  }

}
