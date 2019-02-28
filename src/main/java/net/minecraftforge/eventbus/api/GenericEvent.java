/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.minecraftforge.eventbus.api;

import java.lang.reflect.Type;

import net.minecraftforge.eventbus.ListenerList;

/**
 * Implements {@link IGenericEvent} to provide filterable events based on generic type data.
 *
 * Subclasses should extend this if they wish to expose a secondary type based filter (the generic type).
 *
 * @param <T> The type to filter this generic event for
 */
public class GenericEvent<T> extends Event implements IGenericEvent<T>
{
    private Class<T> type;
    @Deprecated
    /**
     * FOR TESTING ONLY - THIS WILL BE REMOVED!
     */
    protected GenericEvent(Class<T> type) {
        this(Cause.of("DUMMY GENERIC EVENT CAUSE"), type);
    }
    protected GenericEvent(Cause cause, Class<T> type)
    {
        super(cause);
        this.type = type;
    }

    @Override
    public Type getGenericType()
    {
        return type;
    }

    //Default things that are added by EventSubclassTransformer, but as we are excluded from transformers we must add ourselves.
    private static ListenerList LISTENER_LIST;
    public GenericEvent(Cause cause) {
        super(cause);
    }

    @Override
    protected void setup()
    {
        super.setup();
        if (LISTENER_LIST != null)
            return;
        LISTENER_LIST = new ListenerList(super.getListenerList());
    }

    @Override
    public ListenerList getListenerList()
    {
        return LISTENER_LIST;
    }
}
