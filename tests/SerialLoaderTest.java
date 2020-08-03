import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;

public class SerialLoaderTest {

  @Test
  public void testLoadCsv() throws Exception {
    Reader reader = new StringReader(
        "# timestamp,observer_id,node_id,direction,output\n" +
        "1234567999.11145678,13,13,r,just, some text\n" +
               "123.45678,   13,13,r,same time other text\n" +
               "123.45679,   11,11,r,some other, text"
    );
    SerialLoader serialLoader = new SerialLoader(reader);
    serialLoader.load();
    SerialEvent[] actual = serialLoader.getEvents();
    SerialEvent[] expected =
        {
            new SerialEvent(12345678000L, 13, 13, SerialEvent.SerialDirection.r, "same time other text"),
            new SerialEvent(12345679000L, 11, 11, SerialEvent.SerialDirection.r, "some other, text"),
            new SerialEvent(123456799911145678L, 13, 13, SerialEvent.SerialDirection.r, "just, some text"),
        };

    assertArrayEquals(expected, actual);
  }
}