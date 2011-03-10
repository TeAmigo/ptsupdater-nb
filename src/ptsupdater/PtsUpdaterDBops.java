/*********************************************************************
 * File path:     petrasys/utils/DBops.java
 * Version:       
 * Description:   
 * Author:        Rick Charon <rickcharon@gmail.com>
 * Created at:    Tue Nov 16 09:22:38 2010
 * Modified at:   Thu Nov 18 09:06:55 2010
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************/
package ptsupdater;

import java.sql.*;
import java.util.ArrayList;
import javax.swing.JOptionPane;
public class PtsUpdaterDBops {

  static private Connection tradesConnection = null;

  public PtsUpdaterDBops() {
  }

  public static Connection setuptradesConnection() {

    try {
      Class.forName("org.postgresql.Driver");
      tradesConnection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/trading", "trader1", "trader1");
    } catch (Exception ex) {
      System.err.println("EXCEPTION: " + ex.getMessage());

    } finally {
      return tradesConnection;
    }

  }

  public static ArrayList distinctSymolsList() {
    CallableStatement callStmt = null;
    ArrayList<String> retList = new ArrayList<String>();
    try {
      callStmt = PtsUpdaterDBops.setuptradesConnection().prepareCall("select * from distinctQuoteSymbols();",
              ResultSet.TYPE_SCROLL_INSENSITIVE,
              ResultSet.CONCUR_READ_ONLY);
      ResultSet res = callStmt.executeQuery();
      while(res.next()) {
        retList.add(res.getString("symbol"));
      }
    } catch (SQLException ex) {
      System.err.println("SQLException  in distinctSymolsList(): " + ex.getMessage());
    } finally {
      return retList;
    }
  }

  public static ArrayList<SymbolMaxDateLastExpiry> SymbolsMaxDateLastExpiryList() {
    CallableStatement callStmt = null;
    ArrayList<SymbolMaxDateLastExpiry> retList = new ArrayList<SymbolMaxDateLastExpiry>();
    try {
      callStmt = PtsUpdaterDBops.setuptradesConnection().prepareCall("select * from symbolMaxDateLastExpiryList();",
              ResultSet.TYPE_SCROLL_INSENSITIVE,
              ResultSet.CONCUR_READ_ONLY);
      ResultSet res = callStmt.executeQuery();
      while(res.next()) {
        SymbolMaxDateLastExpiry sme = new SymbolMaxDateLastExpiry();
        sme.symbol = res.getString("symbol");
        sme.maxDate = res.getTimestamp("maxdate");
        sme.lastExpiry = res.getInt("maxexpiry");
        retList.add(sme);
      }
    } catch (SQLException ex) {
      System.err.println("SQLException  in distinctSymbolMaxDateLastExpiryList(): " + ex.getMessage());
    } finally {
      return retList;
    }
  }


  public static CallableStatement datedRangeBySymbol(String sym, Timestamp beginDate, Timestamp endDate) {
    CallableStatement ret = null;
    try {
      String callStr = "select * from datedRangeBySymbol('" + sym + "', '" + beginDate + "', '"
              + endDate + "');";
      ret = PtsUpdaterDBops.setuptradesConnection().
              prepareCall(callStr, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return ret;
    }
  }

  /**
   * rpc - 3/7/10 10:17 AM - Get a dated range for a symbol with a specific expiry
   * @param sym
   * @param expiry
   * @param beginDate
   * @param endDate
   * @return
   */
  public static PreparedStatement datedRangeBySymbolAndExpiry(String sym, int expiry,
          Timestamp beginDate, Timestamp endDate) {
    PreparedStatement pstmt = null;
    try {
      pstmt = PtsUpdaterDBops.setuptradesConnection().prepareStatement("SELECT datetime, open, high,low, close, "
              + "volume FROM quotes1min"
              + "where symbol=? and "
              + "datetime >= ? and "
              + "datetime <= ?  and expiry=? order by datetime;");
      pstmt.setString(1, sym);
      pstmt.setTimestamp(2, beginDate);
      pstmt.setTimestamp(3, endDate);
      pstmt.setInt(4, expiry);
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }

  public static PreparedStatement minMaxDateBySymbolAndExpiry(String symbol, int expiry) {
    PreparedStatement pstmt = null;
    try {
      pstmt = PtsUpdaterDBops.setuptradesConnection().prepareStatement("SELECT min(datetime) as minDate, max(datetime) as maxDate FROM quotes1min "
              + "WHERE symbol= ? and expiry= ?");
      pstmt.setString(1, symbol);
      pstmt.setInt(2, expiry);
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }

  /**
   * rpc - 3/7/10 10:26 AM - This works because the last symbol in the quotes1min table is
   * assumed to be the current working expiry. If it isn't, this could be a problem,
   * @param symbol the UL
   * @return a PreparedStatement that has 1 row, 1 column, with int max(expiry)
   */
  public static int maxExpiryWithDataBySymbol(String symbol) {
    PreparedStatement pstmt = null;
    int expiry = 0;
    try {
      pstmt = PtsUpdaterDBops.setuptradesConnection().prepareStatement("SELECT max(expiry) FROM quotes1min  WHERE symbol= ?");
      pstmt.setString(1, symbol);
      ResultSet res = pstmt.executeQuery();
      if (res.next()) {
        expiry = res.getInt(1);
      } else {
        throw new Exception("No result set returned.");
      }
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return expiry;
    }
  }

  /**
   * rpc - 4/18/10 1:19 PM Get the min and max Date in DB by UL
   * @param sym - UL
   * @return The relevant PreparedStatement.
   */
  public static PreparedStatement minMaxDatesBySym(String sym) {
    PreparedStatement pstmt = null;
    try {
      pstmt = PtsUpdaterDBops.setuptradesConnection().prepareStatement(
              "SELECT min(datetime), max(datetime)  FROM quotes1min where symbol=?");
      pstmt.setString(1, sym);
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }

  public static PreparedStatement activeFuturesDetails() {
    PreparedStatement pstmt = null;
    try {
      pstmt = PtsUpdaterDBops.setuptradesConnection().prepareStatement(
              "select * from activeFuturesDetails()");
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }

  public static PreparedStatement distinctSymbolInfos() {
    PreparedStatement pstmt = null;
    try {
      pstmt = PtsUpdaterDBops.setuptradesConnection().prepareStatement("SELECT distinct  symbol, exchange, multiplier, "
              + "priceMagnifier, minTick, fullName "
              + "FROM futuresContractDetails");
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }

  public static PreparedStatement MultiplierAndMagnifier(String sym) {
    PreparedStatement pstmt = null;
    try {
      pstmt = PtsUpdaterDBops.setuptradesConnection().prepareStatement("SELECT distinct multiplier, priceMagnifier "
              + "FROM futuresContractDetails WHERE symbol = '" + sym + "'");
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }

  public static PreparedStatement exchangeBySymbolandExpiry(String symbol, int expiry) {
    PreparedStatement pstmt = null;
    try {
      pstmt = PtsUpdaterDBops.setuptradesConnection().prepareStatement("SELECT exchange FROM futuresContractDetails WHERE "
              + "symbol= ? and expiry= ?");
      pstmt.setString(1, symbol);
      pstmt.setInt(2, expiry);
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }


  public static PreparedStatement getExpirysForUpdate(Connection con, String ul, int beginDate, int endDate) {
    PreparedStatement pstmt = null;
    try {
      pstmt = con.prepareStatement("SELECT * FROM futuresContractDetails "
              + "where symbol='" + ul + "'and expiry  >= " + beginDate + " and expiry <= " + endDate
              + " order by expiry");
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }

  public static PreparedStatement getActiveContracts(Connection con) {
    PreparedStatement pstmt = null;
    try {
      pstmt = con.prepareStatement("SELECT * FROM futuresContractDetails "
              + "where active = 1 order by symbol, expiry");
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }

 public static PreparedStatement updateBeginEndDatesForExpiry1(Connection con, String ul, int expiry,
          String beginDate, String endDate) {

    PreparedStatement pstmt = null;
    try {
      pstmt = con.prepareStatement("update futuresContractDetails "
              + " set active=1, beginDate  =  '" + beginDate + "', endDate = '"
              + endDate + "' where symbol='" + ul + "' and expiry = " + expiry);
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }

  public static PreparedStatement updateBeginEndDatesForExpiry(Connection con) {

    PreparedStatement pstmt = null;
    try {
      pstmt = con.prepareStatement("update futuresContractDetails "
              + " set active=1, beginDate  = ?, endDate = ? where symbol = ? and expiry = ?");
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }

  public static String createCompressionTable(int compressionFactor) {
    String dbTableName = "quotes" + compressionFactor + "min";
    try {
      PreparedStatement createCompressionTable =
              PtsUpdaterDBops.setuptradesConnection().prepareStatement("CREATE TABLE IF NOT EXISTS "
              + dbTableName
              + " (" + "symbol VARCHAR( 15 ) NOT NULL , "
              + "datetime DATETIME NOT NULL , "
              + "open DOUBLE NOT NULL , "
              + "high DOUBLE NOT NULL , "
              + "low DOUBLE NOT NULL , "
              + "close DOUBLE NOT NULL , "
              + "volume BIGINT( 20 ) NOT NULL, "
              + "PRIMARY KEY(symbol, datetime))");
      createCompressionTable.execute();
      createCompressionTable.close();
    } catch (SQLException ex) {
      System.err.println("EXCEPTION: " + ex.getMessage());
      dbTableName = null;
    } finally {
      return dbTableName;
    }
  }

  public static PreparedStatement insertIntoCompressionTable(String table) {
    PreparedStatement pstmt = null;
    try {
      pstmt =
              PtsUpdaterDBops.setuptradesConnection().
              prepareStatement("REPLACE INTO " + table + " VALUES (?, ? , ?, ?, ?, ?, ?)");
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return pstmt;
    }
  }


  public static int getNextPaperOrderID() {
    int id = -1;
    try {
      PreparedStatement pstmt =
              PtsUpdaterDBops.setuptradesConnection().
              prepareStatement("SELECT max(OrderID) FROM PaperOrders");
      ResultSet res = pstmt.executeQuery();
      res.next(); //To get a lastexpiry for loop, so should be one extra early expiry
      id = res.getInt(1);
    } catch (SQLException ex) {
      System.err.println("SQLException in playItForward(): " + ex.getMessage());
    } finally {
      return (id + 1);
    }
  }

  public static CallableStatement playItForwardBySymbol(String sym, Timestamp beginDate, double high, double low) {
    CallableStatement ret = null;

    try {
      String callStr = "select * from playitforward('" + sym + "', '" + beginDate + "', '"
              + high + "', '" + low + "');";
      ret = PtsUpdaterDBops.setuptradesConnection().
              prepareCall(callStr, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
    } finally {
      return ret;
    }
  }


  public static void main(String[] args) {
    try {
//      PtsUpdaterDBops.getPaperTrades();
      Connection con = PtsUpdaterDBops.setuptradesConnection();
      con.setAutoCommit(false);
      String ul = "AUD";
      java.util.Date dd = new java.util.Date(110, 00, 03);
      PreparedStatement pstmt = PtsUpdaterDBops.playItForwardBySymbol(ul, new Timestamp(dd.getTime()), 0.90, 0.88);
      ResultSet res = pstmt.executeQuery();
      boolean ret = res.next();
      Date dout = res.getDate("datetime");
      Double open = res.getDouble("open");
      Double high = res.getDouble("high");
      Double low = res.getDouble("low");
      Double close = res.getDouble("close");
      int vol = res.getInt("volume");
      con.close();
    } catch (SQLException ex) {
      System.err.println("EXCEPTION: " + ex.getMessage());
    }


  }
}
