/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ptsupdater;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rickcharon
 */
public class PtsQuoteHistory {

  boolean isHistRequestCompleted = false;
  private final List<PtsPriceBar> priceBars;

  public boolean isIsHistRequestCompleted() {
    return isHistRequestCompleted;
  }

  public void setIsHistRequestCompleted(boolean isHistRequestCompleted) {
    this.isHistRequestCompleted = isHistRequestCompleted;
  }

  public PtsQuoteHistory() {
    priceBars = new ArrayList<PtsPriceBar>();
  }

  public void addHistoricalPriceBar(PtsPriceBar priceBar) {
    priceBars.add(priceBar);
  }

  public int getSize() {
    return priceBars.size();
  }

  public PtsPriceBar getFirstPriceBar() {
    return priceBars.get(0);
  }

  public List<PtsPriceBar> getAll() {
    return priceBars;
  }
}
