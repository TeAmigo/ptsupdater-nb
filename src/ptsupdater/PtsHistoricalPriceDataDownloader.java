package ptsupdater;

import ptsutils.PtsDBops;
import ptsutils.SymbolMaxDateLastExpiry;
import ptsutils.PtsContractFactory;
import com.ib.client.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.text.*;
import java.util.*;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;
import org.joda.time.Seconds;
import ptsutils.PtsMySocket;

/**
 *
 * Retrieves historical price and volume data for a specified security and saves
 * it as date- and time-stamped OHLCV price bars. Up to one year of intraday
 * historical data can be downloaded for stocks, futures, forex, and indices.
 */
public class PtsHistoricalPriceDataDownloader implements Runnable {

  /*
   * rpc - 3/2/10 10:59 AM See pacing violations in IB-TWS Reference
   * "Do not make more than 60 historical data requests in any ten-minute period."
   * so, 6 a minute, or every 10 seconds is ok. add 100 millis for buffer.
   * // RPC 4/26/11 4:24 PM Am seeing pacing errors again, not sure why
   *
   */
  private static final int MAX_REQUEST_FREQUENCY_MILLIS = 10100;
  private static final String lineSep = System.getProperty("line.separator");
  private int tickerId;
  private String fileName;
  private PtsBarSize barSize;
  private PtsQuoteHistory quoteHistory;
  private List<PtsPriceBar> priceBars;
  private Contract contract;
  private PrintWriter writer;
  private boolean rthOnly;
  private boolean firstBarReached;
  private boolean isCancelled;
  private Calendar firstDate, lastDate;
  private Connection quotes1minConnection;
  private PreparedStatement stmtForQuotes;
  private PtsMySocket socket;
  private long lastRequestTime;
  private PtsHistoricalFromTWS histFromTWS;
  private boolean guard;
  ArrayList<SymbolMaxDateLastExpiry> ContractInfoTable;

  public PtsHistoricalPriceDataDownloader(PtsHistoricalFromTWS histFrom, PtsMySocket socket,
          ArrayList<SymbolMaxDateLastExpiry> ContractInfoTableIn) {
    try {
      ContractInfoTable = ContractInfoTableIn;
      quoteHistory = new PtsQuoteHistory();
      histFromTWS = histFrom;
      this.socket = socket;
      int i = 1;
    } catch (Exception ex) {
      System.err.println("Exception in PtsHistoricalPriceDataDownloader: " + ex.getMessage());
    }
  }

  private synchronized void blockOnGuard() {
    //This guard only loops once for each special event, which may not
    //be the event we're waiting for.
    while (guard) {
      try {
        wait();
        int i = 1;
      } catch (InterruptedException e) {
      }
    }
  }

  public synchronized void setGuard() {
    guard = true;
  }

  public synchronized void releaseGuard() {
    guard = false;
    notifyAll();
  }

  public PtsHistoricalFromTWS getHistFromTWS() {
    return histFromTWS;
  }

  public void setHistFromTWS(PtsHistoricalFromTWS histIn) {
    this.histFromTWS = histIn;
  }

  public PtsQuoteHistory getQh() {
    return quoteHistory;
  }

  public void setQh(PtsQuoteHistory qh) {
    this.quoteHistory = qh;
  }

  public void setupDownloader(Contract contract, Calendar startDate, Calendar endDate, PtsBarSize barSize) {
    this.barSize = barSize;
    this.rthOnly = false;
    this.contract = contract;
    tickerId = contract.m_conId;
    priceBars = new ArrayList<PtsPriceBar>();
    firstDate = startDate;
    //Date fd = startDate.getTime();
    lastDate = endDate;
    contract.m_includeExpired = true;
  }

  public void reqHistoricalData(int strategyId, Contract contract, Calendar calEndTime, String duration,
          String barSize, String whatToShow, int useRTH, int formatDate) throws InterruptedException {
    DateTime dtEnd = new DateTime(calEndTime);
    DateTime dtBegin = new DateTime(firstDate);
    Hours h = Hours.hoursBetween(dtBegin, dtEnd);
    if(h.getHours() < 24) {
      //duration = Integer.toString(h.getHours() * 3600);
      duration = Integer.toString(Seconds.secondsBetween(dtBegin, dtEnd).getSeconds());
    } else {
      int days = ((h.getHours() + 23) / 24) + 1;
      if(days > 6) {
        days = 6;
      }
      duration = Integer.toString(days) + " D";
    }
    //rpc - 3/2/10 8:12 AM - This is where the pacing is taken care of
    String strEndTime = PtsDateOps.downloadFormat.format(calEndTime.getTime());
    try {
      long elapsedSinceLastRequest = System.currentTimeMillis() - lastRequestTime;
      long remainingTime = MAX_REQUEST_FREQUENCY_MILLIS - elapsedSinceLastRequest;
      if (remainingTime > 0) {
        Thread.sleep(remainingTime);
      }
      quoteHistory.setIsHistRequestCompleted(false);

      System.out.println("Req Hist data for. " + contract.m_symbol
              + "-" + contract.m_expiry + " End time: " + strEndTime);
      lastRequestTime = System.currentTimeMillis();
      //if test for time needed then call getDurationStr
      socket.reqHistoricalData(strategyId, contract, strEndTime, duration, barSize,
              whatToShow, useRTH, formatDate);
      //int i = 1;
    } catch (Exception ex) {
      System.err.println("Exception in reqHistoricalData: " + ex.getMessage());
    }
  }

  private void runContract() {
    try {
      //setGuard();
      download();
      //blockOnGuard();
      if (!isCancelled) {
        dataToDatabase();
      }
    } catch (Exception ex) {
      System.err.println("Exception in runContract: " + ex.getMessage());
    }
  }

  private void doContinuousContractDownload() {
    histFromTWS.setMyMate(this);
    //connect();
    String sym, exchange, expiry;
    Calendar startDate = Calendar.getInstance();
    Calendar endDate = Calendar.getInstance();
    int rc = ContractInfoTable.size();

    for (int rownum = 0; rownum < rc; rownum++) {
      sym = (String) ContractInfoTable.get(rownum).symbol;
      exchange = (String) ContractInfoTable.get(rownum).exchange;
      expiry = Integer.toString(ContractInfoTable.get(rownum).expiry); //Integer.toString(expiration)
      contract = PtsContractFactory.makeContract(sym, "FUT", exchange, expiry, "USD");
      startDate.setTime(ContractInfoTable.get(rownum).beginDateToDownload);
      //Date fd = startDate.getTime();
      endDate.setTime(ContractInfoTable.get(rownum).lastDateToDownload.getTime());
      setupDownloader(contract, startDate, endDate, PtsBarSize.Min1);
      // // RPC 4/26/11 4:34 PM DEBUGGING step size violations
      //FOR DEBUGGING: if(sym.contentEquals("EUR")){runContract();}
      runContract();
    }
    int j = 3;
  }

  @Override
  public void run() {
    try {
      doContinuousContractDownload();
    } catch (Exception ex) {
      System.err.println("Exception in run: " + ex.getMessage());
    } finally {
      if (writer != null) {
        writer.close();
      }
    }
  }

  public void error(int errorCode, String errorMsg) {
    firstBarReached = (errorCode == 162 && errorMsg.contains("HMDS query returned no data"));
    if (firstBarReached) {
      firstBarReached = true;
      quoteHistory.setIsHistRequestCompleted(true);
      System.err.println("In HistoricalPriceDataDownloader.error() - firstBarReached");
      return;
    }
    if (errorCode == 162 || errorCode == 200 || errorCode == 321) {
      cancel();
      String msg = "Could not complete back data download." + lineSep + "Cause: " + errorMsg;
      System.err.println(msg);
    }
  }

  /**
   * rpc - 3/4/10 9:14 AM - Does the loop when more than one
   * call for historical data is necessary, call reqHistoricalData()
   * @throws InterruptedException
   */
  private void download() throws InterruptedException {
    int onlyRTHPriceBars = rthOnly ? 1 : 0;
    String infoType = contract.m_exchange.equalsIgnoreCase("IDEALPRO") ? "MIDPOINT" : "TRADES";
    Calendar calEndTime = (Calendar) lastDate.clone();
    Date et = calEndTime.getTime();
    Date fd = firstDate.getTime();
    isCancelled = false;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    String strEndTime = PtsDateOps.downloadFormat.format(calEndTime.getTime());
    long totalMillis = calEndTime.getTimeInMillis() - firstDate.getTimeInMillis();
    long lastDateMillis = calEndTime.getTimeInMillis();
    //rpc - 3/2/10 12:14 PM - This is loop for historic, get all dates for sym
    while (calEndTime.after(firstDate)) {
      reqHistoricalData(tickerId, contract, calEndTime, barSize.getHistRequestDuration(),
              barSize.toIBText(), infoType, onlyRTHPriceBars, 2);
      //rpc - 3/2/10 2:34 PM - Synching - HistoricalFromTWS releases guard
      setGuard();
      blockOnGuard();
      if (firstBarReached || isCancelled || quoteHistory.getSize() <= 1) {
        return;
      }
      // Use the timestamp of the first bar in the received
      // block of bars as the "end time" for the next historical data
      // request.
      long firstBarMillis = quoteHistory.getFirstPriceBar().getDate();
      calEndTime.setTimeInMillis(firstBarMillis);
      strEndTime = dateFormat.format(calEndTime.getTime());
      // Add the just received block of bars to the cumulative set of
      // bars and clear the current block for the next request
      List<PtsPriceBar> allBars = quoteHistory.getAll();
      priceBars.addAll(0, allBars);
      allBars.clear();
      quoteHistory.setIsHistRequestCompleted(false);
      long firstBar = firstDate.getTimeInMillis();
      while (priceBars.size() > 0 && priceBars.get(0).getDate() < firstBar) {
        priceBars.remove(0);
      }
    }
    //socket.disConnect();
  }

  /**
   *
   */
  public void cancel() {
    isCancelled = true;
    quoteHistory.setIsHistRequestCompleted(true);
  }

  public void setupQuotes1minConnection() {
    try {
      quotes1minConnection = PtsDBops.setuptradesConnection();
      stmtForQuotes = quotes1minConnection.prepareStatement(
              "INSERT INTO quotes1min VALUES (?, ? , ?, ?, ?, ?, ?, ?)");
    } catch (SQLException sqlex) {
      System.err.println("SQLException: " + sqlex.getMessage());
    }
  }

  private void dataToDatabase() {
    setupQuotes1minConnection();
    try {
      int size = priceBars.size();
      for (PtsPriceBar priceBar : priceBars) {
        java.sql.Timestamp dateIn = new java.sql.Timestamp(priceBar.getDate());
        //java.sql.Date dateIn = new java.sql.Date(udatein);
        stmtForQuotes.setString(1, contract.m_symbol);
        stmtForQuotes.setInt(2, Integer.parseInt(contract.m_expiry));
        stmtForQuotes.setTimestamp(3, dateIn);
        stmtForQuotes.setDouble(4, priceBar.getOpen());
        stmtForQuotes.setDouble(5, priceBar.getHigh());
        stmtForQuotes.setDouble(6, priceBar.getLow());
        stmtForQuotes.setDouble(7, priceBar.getClose());
        stmtForQuotes.setLong(8, priceBar.getVolume());
        stmtForQuotes.addBatch();
      }
      int[] updateCounts = stmtForQuotes.executeBatch();
      stmtForQuotes.close();
      quotes1minConnection.close();
    } catch (SQLException sqlex) {
      System.err.println("SQLException: " + sqlex.getMessage());
    }
  }

  private void writeBars() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy,HH:mm:ss,");
    long barsWritten = 0;
    int size = priceBars.size();
    for (PtsPriceBar priceBar : priceBars) {
      String dateTime = dateFormat.format(new java.util.Date(priceBar.getDate()));
      String line = dateTime + priceBar.getOpen() + "," + priceBar.getHigh() + ",";
      line += priceBar.getLow() + "," + priceBar.getClose();
      line += "," + priceBar.getVolume();
      writer.println(line);
      barsWritten++;

      if (barsWritten % 100 == 0) {
        double progress = (100 * barsWritten) / size;
      }
    }
  }

  public static void main(String[] args) {
    DateTime dtEnd = new DateTime("2011-03-24T22:38");
    DateTime dtBegin = new DateTime("2011-03-24T22:58");
    Days dbetween = Days.daysBetween(dtBegin, dtEnd);
    Hours h = Hours.hoursBetween(dtBegin, dtEnd);
    int days = ((h.getHours() + 23) / 24) + 1; // Auto round up, and add 1 because today counts
    Minutes m = Minutes.minutesBetween(dtBegin, dtEnd);
    Seconds s = Seconds.secondsBetween(dtBegin, dtEnd);
    int i = 3;
  }
}
