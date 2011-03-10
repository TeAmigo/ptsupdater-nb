/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ptsupdater;

import com.ib.client.AnyWrapper;
import com.ib.client.EClientSocket;

/**
 *
 * @author rickcharon
 * //rpc - NOTE:3/12/10 6:48 AM - This is necessary to cache the clientID
 */
public class PtsMySocket extends EClientSocket {

  int clientId;

  public int getClientId() {
    return clientId;
  }

  public void setClientId(int clientId) {
    this.clientId = clientId;
  }



  public PtsMySocket(AnyWrapper anyWrapper, int clientId) {
    super(anyWrapper);
    this.clientId = clientId;
  }


  public void connect() {
    eConnect(PtsIBConnectionManager.getHost(), PtsIBConnectionManager.getPort(), clientId);
  }

  public void disConnect() {
    eDisconnect();
    PtsIBConnectionManager.removeClientId(clientId);
  }

}

