package com.amlinv.util.service;

/**
 * Controller of services which
 * Created by art on 4/23/14.
 */
public class ServiceController {
    private Service service;
    private boolean started = false;
    private boolean stopping = false;
    private long    runCount = 0;

    public boolean isStarted() {
        return this.started;
    }

    public boolean isStopping() {
        return this.stopping;
    }

    public long getRunCount() {
        return  this.runCount;
    }

    public ServiceController (Service svc) {
        this.service = svc;
    }

    public void start ()
    throws ServiceAlreadyStartedException, ServiceAlreadyRanException, ServiceStoppingException, Exception
    {
        synchronized ( this ) {
            if ( this.started ) {
                throw new ServiceAlreadyStartedException();
            } else if ( this.stopping ) {
                throw new ServiceStoppingException();
            } else if ( ( this.runCount > 0 ) && ( ! this.service.canRunMultipleTimes() ) ) {
                throw new ServiceAlreadyRanException();
            }

            this.started = true;
        }

        this.service.startService();
    }

    public void stop ()
    throws ServiceNotStartedException, ServiceAlreadyStoppingException, ServiceAlreadyRanException, Exception
    {
        synchronized ( this ) {
            if ( ! this.started ) {
                throw new ServiceNotStartedException();
            } else if ( this.stopping ) {
                throw new ServiceAlreadyStoppingException();
            }

            this.stopping = true;
        }

        this.service.stopService();

        synchronized ( this ) {
            this.started = false;
        }
    }
}
