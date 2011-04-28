/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ptsupdater;

import ptsutils.PtsDBops;
import ptsutils.SymbolMaxDateLastExpiry;
import ptsutils.PtsIBConnectionManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import ptsutils.PtsMySocket;
import org.jfree.data.time.RegularTimePeriod;
import org.joda.time.DateTime;

/**
 *
 * @author rickcharon
 */
public class PtsUpdater {

  PtsMySocket socket;
  PtsHistoricalFromTWS histFromTWS;
  ArrayList<SymbolMaxDateLastExpiry> ContractInfoTable;

  public ArrayList<SymbolMaxDateLastExpiry> getContractInfoTable() {
    return ContractInfoTable;
  }

  public void setContractInfoTable(ArrayList<SymbolMaxDateLastExpiry> ContractInfoTable) {
    this.ContractInfoTable = ContractInfoTable;
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

  public PtsUpdater(int port) {
    histFromTWS = new PtsHistoricalFromTWS();
    PtsIBConnectionManager.setPort(port);
    socket = PtsIBConnectionManager.connect(histFromTWS);
  }

  private void bringSymbolsCurrent(PtsMySocket socket) {
    try {
      PtsHistoricalPriceDataDownloader histDownloader =
              new PtsHistoricalPriceDataDownloader(histFromTWS, socket, ContractInfoTable);
      histFromTWS.setMyMate(histDownloader);
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
    for (int i = 0; i < ContractInfoTable.size(); i++) {
      try {
        pstmt = PtsDBops.exchangeBySymbolandExpiry(ContractInfoTable.get(i).symbol, ContractInfoTable.get(i).expiry);
        ResultSet res = pstmt.executeQuery();
        String exchange;
        if (res.next()) {
          exchange = res.getString(1);
          ContractInfoTable.get(i).exchange = exchange;
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
    //ContractInfoTable = PtsDBops.SymbolsMaxDateLastExpiryList();
    for (SymbolMaxDateLastExpiry entry : ContractInfoTable) {
    }
    updateExchanges();
    SymbolMaxDateLastExpiry sym = null;
    // rpc - 2/4/11 1:35 PM Had a loop over ContractInfoTable.size(), caused that many full sets to download
    try {
      bringSymbolsCurrent(socket);
    } catch (Exception ex) {
      System.err.println("Exception in bringAllCurrent(): " + ex.getMessage());
    } finally {
      socket.disConnect();
    }
  }

  public void bringNewCurrent(int beforeE, int AfterE, int daysBefore) {
    SymbolMaxDateLastExpiry sym = null;
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.DATE, -daysBefore);
    calendar.set(Calendar.HOUR, 0);
    calendar.set(Calendar.MINUTE, 0);
    for (SymbolMaxDateLastExpiry symIn : ContractInfoTable) {
      symIn.beginDateToDownload = calendar.getTime();
    }
    //2/4/11 1:35 PM Had a loop over ContractInfoTable.size(), caused that many full sets to download
    try {
      bringSymbolsCurrent(socket);
    } catch (Exception ex) {
      System.err.println("Exception in bringAllCurrent(): " + ex.getMessage());
    } finally {
      socket.disConnect();
    }
  }

  public void createNewContractInfoTable() {
    ContractInfoTable = new ArrayList<SymbolMaxDateLastExpiry>();
  }

  /**
   * @param args either 0 or 1 - 0 updates all in db, 1 is a file to create a new ContractInfoTable
   */
  public static void main(String[] args) {
    PtsUpdater pts = new PtsUpdater(7496);
    pts.getSocket().reqCurrentTime();
    if (args.length == 0) {
      pts.setContractInfoTable(PtsDBops.SymbolsMaxDateLastExpiryList());
      for(SymbolMaxDateLastExpiry sym : pts.ContractInfoTable) {
        sym.lastDateToDownload = new DateTime().toCalendar(Locale.US);
      }
      pts.bringAllCurrent();
    } else if (args.length == 1) {
      // RPC 4/27/11 10:36 AM Works good for Currency futures, but not for Interest Rates futures, which need to be
      //loaded further back, e.g., the ZF that expires 20110331,
      //should be out after 2/27/11, and the 20110630 should be started up then
      //pts.bringNewCurrent(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
      pts.setContractInfoTable(SymbolMaxDateLastExpiry.createContractInfosFromFile(args[0]));
      pts.bringAllCurrent();
    } else {
      System.err.println("Wrong # of args ( 0 or 3 required)");
      System.err.println("0 args to update, 1 arg as name of input file with ContractInfoLines");
      System.err.println("Format is symbol, expiry, exchange, beginDateTime, endDateTime");
      System.err.println("EndDateTime can be omitted, format for dates is yyyy-MM-dd hh:mm");
//      System.err.println("3 args before date of Expiry, after date of Expiry,");
//      System.err.println("and days back from today e.g. 20110400, 20110700, 7");
      System.exit(1);
    }
    System.out.println("Updates Finished.");

  }
}
