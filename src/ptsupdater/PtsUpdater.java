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
      //int expiry = sym.lastExpiry;
      //Date expD = PtsDateOps.dateFromExpiryFormatString(Integer.toString(expiry));
//      if (expD.before(new Date())) {
//        System.err.println("Need new Expiry (" + expiry + ") is latest in DB...(in bringSymbolCurrent())");
//        return;
//      }
//      Contract contract =
//              PtsContractFactory.makeContract(sym.symbol, "FUT", sym.exchange, Integer.toString(expiry), "USD");
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
    for(int i = 0; i < symList.size(); i++) {
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

  public void bringNewCurrent() {
    //rpc - NOTE HERE:4/13/10 6:13 PM - Is fixed, try was killing socket, had to go outside for loop
    //symList = PtsUpdaterDBops.SymbolsMaxDateLastExpiryList();
    updateExchanges();
    SymbolMaxDateLastExpiry sym = null;

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
    pts.bringAllCurrent();
    System.out.println("Updates Finished.");

  }
}