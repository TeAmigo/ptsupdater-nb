/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ptsupdater;

//import petrasys.utils.MsgBox;

import ptsutils.PtsIBWrapperAdapter;
import java.util.Date;
import org.joda.time.DateTime;

//import petrasys.instruments.PriceBar;
//import petrasys.PeTraSys;

/**
 *
 * @author rickcharon
 */
public class PtsHistoricalFromTWS extends PtsIBWrapperAdapter  {

    PtsHistoricalPriceDataDownloader myMate;

    /**
     *
     * @return
     */
    public PtsHistoricalPriceDataDownloader getMyMate() {
    return myMate;
  }

    public void setMyMate(PtsHistoricalPriceDataDownloader myMate) {
    this.myMate = myMate;
  }

  @Override
  public void error(String str) {
    System.err.println("Error in HistoricalFromTWS: " + str);
  }

  @Override
  public void error(int id, int errorCode, String errorMsg) {
    System.err.println("error() in HistoricalFromTWS, id = " + id + ", errorCode  = " + errorCode +
            ", errorMsg: " + errorMsg);
  }



  @Override
  public void currentTime(long time) {
    //1/18/11 1:53 PM time is in seconds since epoch, so need to * 1000 to get millis
    DateTime dd = new DateTime(time * 1000);
    System.out.println("Current Server Time from TWS: " + dd);
  }




    /**
     *
     */
    public PtsHistoricalFromTWS() {
    
  }

    // Incoming!!! Incoming!!!
    @Override
    public void historicalData(int reqId, String date, double open, double high,
            double low, double close, int volume, int count, double WAP, boolean hasGaps) {
      //PeTraSys.writeToReport("In historicalData in historicalFromTWS");
      try {
        //QuoteHistory qh = traderAssistant.getStrategy(reqId).getQuoteHistory();
        if (date.startsWith("finished")) {
          myMate.getQh().setIsHistRequestCompleted(true);
          System.out.println("Downloaded " + myMate.getQh().getSize());
          //PeTraSys.writeToReport(msg);
          //eventReport.report(msg);
          //synchronized (this) {
          //isPendingHistRequest = false;
          //rpc - 3/2/10 5:25 PM - Getting ILLEGAL MONITOR STATE with notifyAll
          myMate.releaseGuard();
          //notifyAll();
          //}
        } else {
          //Strategy strategy = traderAssistant.getStrategy(reqId);
          //long priceBarDate = (Long.parseLong(date) + strategy.getBarSize().toSeconds()) * 1000;
          // rpc - 2/7/10 5:08 PM Above was adding a bar, mistake.
          long priceBarDate = Long.parseLong(date) * 1000;
          PtsPriceBar priceBar = new PtsPriceBar(priceBarDate, open, high, low, close, volume);
          myMate.getQh().addHistoricalPriceBar(priceBar);
//        if (priceBarDate <= System.currentTimeMillis()) { //is the bar completed?
//          strategy.validateIndicators();
//        }
        }
      } catch (Exception t) {
        // Do not allow exceptions come back to the socket -- it will cause disconnects
        System.err.println("Exception in historicalData " + t.getMessage());
        //eventReport.report(t);
      }
    }

//    @Override
//    public void run() {
//      try {
//        while (true) {
//          PeTraSys.writeToReport("In run in historicalFromTWS");
//          //wait();
//          int i = 1;
//        }
//
//      } catch (Exception ex) {
//        MsgBox.err2(ex);
//      }
//    }
  }