/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ptsupdater;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rickcharon 
 * Basic Date and Calendar Operations
 */
public class PtsDateOps {

  public static final SimpleDateFormat backwardDateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yy zzz");
  public static final SimpleDateFormat expiryFormat = new SimpleDateFormat("yyyyMMdd");
  public static final SimpleDateFormat strFormat = new SimpleDateFormat("MM/dd/yy hh:mm");
  public static final SimpleDateFormat fileFormat = new SimpleDateFormat("MM-dd-yyyy-HHmmss");
  public static final SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  public static final SimpleDateFormat downloadFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
  public static final SimpleDateFormat dbShortFormat = new SimpleDateFormat("yyyy-MM-dd");
  public static final SimpleDateFormat stdShortFormat = new SimpleDateFormat("MM/dd/yy");

  /**
   * rpc - 2/24/10 1:07 PM
   * @return String representation of now in "MM/dd/yy hh:mm" format
   */
  public static String nowPrettyString() {
    return PtsDateOps.strFormat.format(new Date());
  }

  public static String prettyString(Date dateIn) {
    return PtsDateOps.strFormat.format(dateIn);
  }

  /**
   * rpc - 2/24/10 1:06 PM
   * @return String representation of now in format suitable for file naming, MM-dd-yyyy-HHmmss
   */
  public static String nowFileNameString() {
    return PtsDateOps.fileFormat.format(new Date());
  }

  public static String fileFormatString(Date dateIn) {
    return PtsDateOps.fileFormat.format(dateIn);
  }

  public static String dbFormatString(Date dateIn) {
    return PtsDateOps.dbFormat.format(dateIn);
  }

  public static Date dateFromDbFormatString(String strIn) {
    Date date = null;
    try {
      date = dbFormat.parse(strIn);
    } catch (ParseException ex) {
      System.err.println("EXCEPTION: " + ex.getMessage());
    } finally {
      return date;
    }
  }

  public static Date dateFromStdShortFormatString(String strIn) {
    Date date = null;
    try {
      date = stdShortFormat.parse(strIn);
    } catch (ParseException ex) {
      System.err.println("EXCEPTION: " + ex.getMessage());
    } finally {
      return date;
    }
  }

  /**
   *
   * @param strIn
   * @return
   */
  public static Date dateFromExpiryFormatString(String strIn) {
    Date date = null;
    try {
      date = expiryFormat.parse(strIn);
    } catch (ParseException ex) {
      System.err.println("EXCEPTION: " + ex.getMessage());
    } finally {
      return date;
    }
  }

  /**
   * rpc - 2/24/10 1:04 PM
   * @param expiry - a java.util.Date that is the expiry date
   * @return a String representation of the date in IB-TWS expiry format, yyyyMMdd.
   */
  public static String expiryFormatString(Date expiry) {
    return PtsDateOps.expiryFormat.format(expiry);
  }

  public static String dbShortFormatString(int intDate) {
    String dOut = null;
    try {
      Date d1 = PtsDateOps.expiryFormat.parse(Integer.toString(intDate));
      dOut = PtsDateOps.dbShortFormat.format(d1);
    } catch (ParseException ex) {
      System.err.println("EXCEPTION: " + ex.getMessage());
    } finally {
      return dOut;
    }
  }

  /**
   *
   * @param expiry Date of expiry
   * @return int representation of expiry date in IB-TWS format yyyyMMdd.
   */
  public static int expiryFormatInt(Date expiry) {
    return Integer.parseInt(PtsDateOps.expiryFormat.format(expiry));
  }

  /**
   * rpc - 2/24/10 1:05 PM
   * @return The date Now in IB-TWS expiry format, yyyyMMdd.
   */
  public static String expiryNowFormatString() {
    return PtsDateOps.expiryFormat.format(new Date());
  }

  /**
   * rpc - 2/24/10 12:53 PM 
   * @param dateIn - the java.util.date that will have days added or subtracted
   * @param howManyDays - Number of days to add or subract, negative int subracts
   * @return a Date set to the desired date
   * @see Date
   * @see Calendar
   */
  public static Date addOrSubractDaysFromDate(Date dateIn, int howManyDays) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(dateIn);
    cal.add(Calendar.DATE, howManyDays);
    return (Date) cal.getTime();
  }

  //1/9/11 4:28 PM Muchos Gracias to
  //http://stackoverflow.com/questions/567659/calculate-elapsed-time-in-java-groovy
  // I'm only needing weeks, hours, and minutes for current purposes,
  public static String elapsedTimeString(long millisStart, long millisEnd) {
    long diff = millisEnd - millisStart;

    String retStr = "";
    long secondInMillis = 1000;
    long minuteInMillis = secondInMillis * 60;
    long hourInMillis = minuteInMillis * 60;
    long dayInMillis = hourInMillis * 24;
    long yearInMillis = dayInMillis * 365;

    long elapsedYears = diff / yearInMillis;
    diff = diff % yearInMillis;
    long elapsedDays = diff / dayInMillis;
    diff = diff % dayInMillis;
    long elapsedHours = diff / hourInMillis;
    diff = diff % hourInMillis;
    long elapsedMinutes = diff / minuteInMillis;
    diff = diff % minuteInMillis;
    long elapsedSeconds = diff / secondInMillis;
    retStr = elapsedDays + "D:" + elapsedHours + "H:" + elapsedMinutes + "M";
    return retStr;
  }

  public static void main(String[] args) {
    Calendar c1 = Calendar.getInstance(), c2 = Calendar.getInstance();
    c1.setTime(new Date());
    c2.setTime(new Date());
    c2.add(Calendar.MONTH, 1);
    c2.add(Calendar.HOUR, 3);
    c2.add(Calendar.MINUTE, 10);
    PtsDateOps.elapsedTimeString(c1.getTimeInMillis(), c2.getTimeInMillis());
    System.out.println(PtsDateOps.elapsedTimeString(c1.getTimeInMillis(), c2.getTimeInMillis()));
    if (c1.after(c2)) {
      int i = 1;
    }
    int j = 2;
  }
}
