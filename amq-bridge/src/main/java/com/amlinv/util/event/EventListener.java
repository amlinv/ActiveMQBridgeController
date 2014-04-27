package com.amlinv.util.event;

/**
 * Created by art on 4/26/14.
 */
public interface EventListener<EVENT_TYPE> {
    void    onEvent(EVENT_TYPE event);
}
