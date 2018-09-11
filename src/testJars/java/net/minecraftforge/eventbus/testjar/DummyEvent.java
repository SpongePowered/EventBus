package net.minecraftforge.eventbus.testjar;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Cause;
import net.minecraftforge.eventbus.api.Event;

public class DummyEvent extends Event {

    public DummyEvent(Cause cause) {
        super(cause);
    }

    public static class GoodEvent extends DummyEvent {
        public GoodEvent(Cause cause) {
            super(cause);
        }
    }
    public static class BadEvent extends DummyEvent {
        public BadEvent(Cause cause) {
            super(cause);
        }
    }

    public static class CancelableEvent extends DummyEvent {
        public CancelableEvent(Cause cause) {
            super(cause);
        }

        @Override
        public boolean isCancelable() {
            return true;
        }
    }

    @HasResult
    public static class ResultEvent extends DummyEvent {
        public ResultEvent(Cause cause) {
            super(cause);
        }
    }
}
