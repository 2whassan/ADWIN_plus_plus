package de.tub.bdapro.adwin;

import de.tub.bdapro.adwin.core.histogram.Histogram;

/**
 * The {@link ADWINInterface } is the basic interface for an external usage of this library.
 */
public interface ADWINInterface {

    /**
     * Adds a new element to the ADWIN algorithm
     * @param element
     * @return a concept drift was found
     * @throws Exception
     */
    boolean addElement(double element) throws Exception;

    void adaptiveDrop();

    // Some utility methods for monitoring
    int getAdwinCount();
    int resetAdwinCount();
    int getNumElementsProcessed();
    int getSize();
    void terminateAdwin();
    int getMinLimit();
    int minWindowMovingRate();
    int waitingAfterDriftElements();


}
