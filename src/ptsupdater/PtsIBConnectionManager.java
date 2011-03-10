/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ptsupdater;

import com.ib.client.EClientSocket;
//import com.jsystemtrader.platform.trader.IBWrapperAdapter;
import java.util.concurrent.ConcurrentSkipListSet;
//import petrasys.PeTraSys;
//import petrasys.utils.IBWrapperAdapter;

/**
 *
 * @author rickcharon - Keep track of open IB-TWS client id's, and
 * whatever else is necessary for IB connections, all methods are static
 */
public class PtsIBConnectionManager {

  private static ConcurrentSkipListSet inUseConnections = new ConcurrentSkipListSet<Integer>();
  private static ConcurrentSkipListSet inUseTickerIds = new ConcurrentSkipListSet<Integer>();
  private static String host = "";
  private static int port = 7496;

  /**
   * // rpc - 3/3/10 5:47 PM the host is usually blank string, i.e., localhost
   * @return
   */
  public static String getHost() {
    return host;
  }

  /**
   *
   * @param host TWS host machine, usually "" (blank string = local host
   */
  public static void setHost(String host) {
    PtsIBConnectionManager.host = host;
  }

  /**
   *
   * @return
   */
  public static int getPort() {
    return port;
  }

  /**
   *
   * @param port
   */
  public static void setPort(int port) {
    PtsIBConnectionManager.port = port;
  }


  
  /*
   * rpc - 2/23/10 7:25 AM This implements an ordered sequence of ints that can
   * be deleted from and filled in the middle as well as the end,
   */
  /**
   *
   * @return
   */
  public static int getClientId() {
    Integer retVal = 0;
    if (inUseConnections.isEmpty()) {
      inUseConnections.add(1);
      retVal = 1;
      return retVal;
    } else {
      int firstVal = Integer.parseInt(inUseConnections.first().toString());
      int lastVal = Integer.parseInt(inUseConnections.last().toString());
      for (retVal = firstVal; retVal <= lastVal; retVal++) {
        if (inUseConnections.higher(retVal) == null) {
          retVal++;
          break;
        }
        int higher = Integer.parseInt(inUseConnections.higher(retVal).toString());
        if (higher > retVal + 1) {
          retVal += 1;
          inUseConnections.add(retVal);
          return retVal;
        }
      }
    }
    //retVal++;
    inUseConnections.add(retVal);
    return retVal;
  }

  /**
   *
   * @param id
   */
  public static void removeClientId(int id) {
    inUseConnections.remove(id);
  }

  /**
   * rpc - 2/25/10 5:22 PM
   * Unique Ticker ID is needed for each contract connecting to IB
   * @return unique Ticker Id
   */
  public static int getTickerId() {
    Integer retVal = 0;
    if (inUseTickerIds.isEmpty()) {
      inUseTickerIds.add(1);
      retVal = 1;
      return retVal;
    } else {
      int firstVal = Integer.parseInt(inUseTickerIds.first().toString());
      int lastVal = Integer.parseInt(inUseTickerIds.last().toString());
      for (retVal = firstVal; retVal <= lastVal; retVal++) {
        if (inUseTickerIds.higher(retVal) == null) {
          retVal++;
          break;
        }
        int higher = Integer.parseInt(inUseTickerIds.higher(retVal).toString());
        if (higher > retVal + 1) {
          retVal += 1;
          inUseTickerIds.add(retVal);
          return retVal;
        }
      }
    }
    //retVal++;
    inUseTickerIds.add(retVal);
    return retVal;
  }

  /**
   *
   * @param id
   */
  public static void removeTickerId(int id) {
    inUseTickerIds.remove(id);
  }

  /**
   * // rpc - 3/2/10 7:45 AM
   * Generic Connect
   * @param caller must implement IBWrapperAdapter
   * @return the EClientSocket for this connection
   */
  public static  PtsMySocket connect(PtsIBWrapperAdapter caller) {

    PtsMySocket socket = new PtsMySocket(caller, PtsIBConnectionManager.getClientId());

    if (!socket.isConnected()) {
      socket.eConnect(PtsIBConnectionManager.getHost(), PtsIBConnectionManager.getPort(), socket.getClientId());
    }
    if (!socket.isConnected()) {
      System.err.println("Could not connect to TWS.");
    } else {
      System.err.println("Connected to TWS.");
    }

    // IB Log levels: 1=SYSTEM 2=ERROR 3=WARNING 4=INFORMATION 5=DETAIL
    //socket.setServerLogLevel(2);
    //socket.reqNewsBulletins(true);
    //serverVersion = socket.serverVersion();

    //eventReport.report("Connected to TWS");
    return socket;
  }

  public static void disConnect(PtsMySocket socket) {
    socket.disConnect();
  }

  /**
   *
   * @param args
   */
  public static void main(String args[]) {
    int tt, hold;
    for (int test = 0; test < 30; test++) {
      tt = PtsIBConnectionManager.getClientId();
      hold = tt;
    }
    for (int test = 7; test < 30; test += 7) {
      PtsIBConnectionManager.removeClientId(test);
    }
    for (int test = 0; test < 4; test++) {
      tt = PtsIBConnectionManager.getClientId();
      hold = tt;

    }
  }
}
