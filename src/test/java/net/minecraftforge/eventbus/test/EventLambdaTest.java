package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class EventLambdaTest {
    boolean hit;
    @Test
    public void eventLambda() {
        final IEventBus iEventBus = IEventBus.create();
        iEventBus.addListener((Event e)-> hit = true);
        iEventBus.post(new Event());
        assertTrue(hit, "Hit event");
    }

    public void consumeSubEvent(SubEvent e) {
        hit = true;
    }
    @Test
    void eventSubLambda() {
        final IEventBus iEventBus = IEventBus.create();
        iEventBus.addListener(this::consumeSubEvent);
        iEventBus.post(new SubEvent());
        assertTrue(hit, "Hit subevent");
        hit = false;
        iEventBus.post(new Event());
        assertTrue(!hit, "Didn't hit parent event");
    }

    @Test
    void eventGenericThing() {
        // pathological test because you can't derive the lambda types in all cases...
        IEventBus bus = IEventBus.create();
        registerSomeGodDamnWrapper(bus, CancellableEvent.class, this::subEventFunction);
        final CancellableEvent event = new CancellableEvent();
        bus.post(event);
        assertTrue(event.isCanceled(), "Event got cancelled");
        final SubEvent subevent = new SubEvent();
        bus.post(subevent);
    }

    private boolean subEventFunction(final CancellableEvent event) {
        return event instanceof CancellableEvent;
    }

    public <T extends Event> void registerSomeGodDamnWrapper(IEventBus bus, Class<T> tClass, Function<T, Boolean> func) {
        bus.addListener(EventPriority.NORMAL, false, tClass, (T event) -> {
            if (func.apply(event)) {
                event.setCanceled(true);
            }
        });
    }
    // faked asm processing for easy testing
    public static class SubEvent extends Event {
        private static ListenerList LISTENER_LIST;
        protected void setup()
        {
            super.setup();
            if (LISTENER_LIST != null)
            {
                return;
            }
            LISTENER_LIST = new ListenerList(super.getListenerList());
        }
        @Override
        public ListenerList getListenerList() {
            return LISTENER_LIST;
        }
    }

    public static class CancellableEvent extends Event {
        private static ListenerList LISTENER_LIST;
        protected void setup()
        {
            super.setup();
            if (LISTENER_LIST != null)
            {
                return;
            }
            LISTENER_LIST = new ListenerList(super.getListenerList());
        }
        @Override
        public ListenerList getListenerList() {
            return LISTENER_LIST;
        }

        @Override
        public boolean isCancelable() {
            return true;
        }
    }
}
