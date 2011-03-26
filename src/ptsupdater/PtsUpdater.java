/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ptsupdater;

import com.ib.client.Contract;
import java.awt.Cursor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ListModel;

/**
 *
 * @author rickcharon
 */
public class PtsUpdater {

  PtsMySocket socket;
  PtsHistoricalFromTWS histFromTWS;
  ArrayList<SymbolMaxDateLastExpiry> symList;

  public ArrayList<SymbolMaxDateLastExpiry> getSymList() {
    return symList;
  }

  public void setSymList(ArrayList<SymbolMaxDateLastExpiry> symList) {
    this.symList = symList;
  }

  public PtsHistoricalFromTWS getHistFromTWS() {
    return histFromTWS;
  }

  public void setHistFromTWS(PtsHistoricalFromTWS histFromTWS) {
    this.histFromTWS = histFromTWS;
  }

  public PtsMySocket getSocket() {
    return socket;
  }

  public void setSocket(PtsMySocket socket) {
    this.socket = socket;
  }

  public PtsUpdater() {
    histFromTWS = new PtsHistoricalFromTWS();
    socket = PtsIBConnectionManager.connect(histFromTWS);
  }

  private void bringSymbolsCurrent(PtsMySocket socket) {
    try {
      Calendar startDate = Calendar.getInstance();
      Calendar endDate = Calendar.getInstance();
      endDate.setTime(new Date());
      //startDate.setTime(sym.maxDate);
      PtsHistoricalPriceDataDownloader histDownloader =
              new PtsHistoricalPriceDataDownloader(histFromTWS, socket, symList);
      histFromTWS.setMyMate(histDownloader);
      //histDownloader.setupDownloader(contract, startDate, endDate, PtsBarSize.Min1);
      //rpc - NOTE:7/8/10 5:55 PM - Put a new dialog in here to start thread, and join it after
      // a few seconds, and in the dialog have a box to kill the thread...
      Thread thread = new Thread(histDownloader);
      thread.setName("histDownloader");
      thread.start();
      thread.join();
    } catch (Exception ex) {
      System.err.println("Exception in bringSymbolCurrent(): " + ex.getMessage());
    } finally {
      int j = 3;
    }
  }

  private void updateExchanges() {
    PreparedStatement pstmt = null;
    for (int i = 0; i < symList.size(); i++) {
      try {
        pstmt = PtsUpdaterDBops.exchangeBySymbolandExpiry(symList.get(i).symbol, symList.get(i).lastExpiry);
        ResultSet res = pstmt.executeQuery();
        String exchange;
        if (res.next()) {
          exchange = res.getString(1);
          symList.get(i).exchange = exchange;
        } else {
          System.err.println("No Exchange String returned in bringSymbolCurrent()!");
          return;
        }
      } catch (SQLException ex) {
        Logger.getLogger(PtsUpdater.class.getName()).log(Level.SEVERE, null, ex);
      }

    }
  }

  public void bringAllCurrent() {
    //rpc - NOTE HERE:4/13/10 6:13 PM - Is fixed, try was killing socket, had to go outside for loop
    symList = PtsUpdaterDBops.SymbolsMaxDateLastExpiryList();
    updateExchanges();
    SymbolMaxDateLastExpiry sym = null;

    //2/4/11 1:35 PM Had a loop over symList.size(), caused that many full sets to download
    try {
//      for (int i = 0; i < symList.size(); i++) {
      bringSymbolsCurrent(socket);
//      }
//        sym = ;
//        ResultSet res = PtsUpdaterDBops.minMaxDatesBySym(sym).executeQuery();
//        if (res.next()) {
//          Timestamp maxD = res.getTimestamp(2);
//          endDateLabel.setText(DateOps.dbFormatString(maxD));
//        }
//        bringSymbolCurrent(sym, socket);
//        int j = 3;
//      }
    } catch (Exception ex) {
      System.err.println("Exception in bringAllCurrent(): " + ex.getMessage());
    } finally {
//      downloadWaitLabel.setText("Updating Done: " + sym + "....");
//      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      socket.disConnect();
    }
  }

  public void bringNewCurrent(int beforeE, int AfterE, int daysBefore) {
    //3/15/11 4:04 PM Need to get the symList differently, have it set the new syms
    symList = PtsUpdaterDBops.SymbolsExpirysBetweemDatesList(beforeE, AfterE);
    SymbolMaxDateLastExpiry sym = null;
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.DATE, -daysBefore);
    calendar.set(Calendar.HOUR, 0);
    calendar.set(Calendar.MINUTE, 0);
    for (SymbolMaxDateLastExpiry symIn : symList) {
      symIn.maxDate = calendar.getTime();
    }
    //2/4/11 1:35 PM Had a loop over symList.size(), caused that many full sets to download
    try {
      bringSymbolsCurrent(socket);
    } catch (Exception ex) {
      System.err.println("Exception in bringAllCurrent(): " + ex.getMessage());
    } finally {
      socket.disConnect();
    }
  }

  public void createNewSymList() {
    symList = new ArrayList<SymbolMaxDateLastExpiry>();

  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    PtsUpdater pts = new PtsUpdater();
    pts.getSocket().reqCurrentTime();
    if (args.length == 0) {
      pts.bringAllCurrent();
    } else if (args.length == 3) {
      pts.bringNewCurrent(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
    } else {
      System.out.println("Wrong # of args ( 0 or 3 required)");
      System.out.println("0 args to update,");
      System.out.println("3 args before date of Expiry, after date of Expiry,");
        System.out.println("and days back from today e.g. 20110400, 20110700, 7");
      System.exit(1);
    }
    System.out.println("Updates Finished.");

  }
}
