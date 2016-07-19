package timber.log;

import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Timber.Tree Tree} for debug builds. Automatically infers the tag from the calling
 * class.
 */
public class AndroidDebugTree extends Timber.Tree {
  private static final int MAX_LOG_LENGTH = 4000;
  private static final int CALL_STACK_INDEX = 5;
  private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");

  /**
   * Extract the tag which should be used for the message from the {@code element}. By default
   * this will use the class name without any anonymous class suffixes (e.g., {@code Foo$1}
   * becomes {@code Foo}).
   * <p>
   * Note: This will not be called if a {@linkplain Timber#tag(String) manual tag} was specified.
   */
  protected String createStackElementTag(StackTraceElement element) {
    String tag = element.getClassName();
    Matcher m = ANONYMOUS_CLASS.matcher(tag);
    if (m.find()) {
      tag = m.replaceAll("");
    }
    return tag.substring(tag.lastIndexOf('.') + 1);
  }

  @Override final String getTag() {
    String tag = super.getTag();
    if (tag != null) {
      return tag;
    }

    // DO NOT switch this to Thread.getCurrentThread().getStackTrace(). The test will pass
    // because Robolectric runs them on the JVM but on Android the elements are different.
    StackTraceElement[] stackTrace = new Throwable().getStackTrace();
    if (stackTrace.length <= CALL_STACK_INDEX) {
      throw new IllegalStateException(
          "Synthetic stacktrace didn't have enough elements: are you using proguard?");
    }
    return createStackElementTag(stackTrace[CALL_STACK_INDEX]);
  }

  /**
   * Break up {@code message} into maximum-length chunks (if needed) and send to either
   * {@link Log#println(int, String, String) Log.println()} or
   * {@link Log#wtf(String, String) Log.wtf()} for logging.
   *
   * {@inheritDoc}
   */
  @Override protected void log(int priority, String tag, String message, Throwable t) {
    if (message.length() < MAX_LOG_LENGTH) {
      if (priority == Log.ASSERT) {
        Log.wtf(tag, message);
      } else {
        Log.println(priority, tag, message);
      }
      return;
    }

    // Split by line, then ensure each line can fit into Log's maximum length.
    for (int i = 0, length = message.length(); i < length; i++) {
      int newline = message.indexOf('\n', i);
      newline = newline != -1 ? newline : length;
      do {
        int end = Math.min(newline, i + MAX_LOG_LENGTH);
        String part = message.substring(i, end);
        if (priority == Log.ASSERT) {
          Log.wtf(tag, part);
        } else {
          Log.println(priority, tag, part);
        }
        i = end;
      } while (i < newline);
    }
  }
}
