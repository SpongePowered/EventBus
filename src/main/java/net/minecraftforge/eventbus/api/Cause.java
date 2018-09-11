package net.minecraftforge.eventbus.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("unchecked")
public class Cause implements Iterable<Object>
{

    private final Object[] cause;
    // lazy load
    @Nullable
    private ImmutableList<Object> immutableCauses;

    /**
     * Constructs a new cause.
     *
     * @param causes The causes
     */
    private Cause(Object[] causes)
    {
        final Object[] objects = new Object[causes.length];
        for (int index = 0; index < causes.length; index++)
        {
            objects[index] = checkNotNull(causes[index], "Null cause element!");
        }
        this.cause = objects;
    }

    /**
     * Constructs a new cause.
     *
     * @param causes The causes
     */
    private Cause(Collection<Object> causes)
    {
        final Object[] objects = new Object[causes.size()];
        int index = 0;
        for (Object cause : causes)
        {
            objects[index++] = checkNotNull(cause, "Null cause element!");
        }
        this.cause = objects;
    }

    /**
     * Creates a new {@link Builder} to make a new {@link Cause}.
     *
     * @return The new builder
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Constructs a new cause with the specified event context and cause.
     *
     * @param cause The direct object cause
     * @return The constructed cause
     */
    public static Cause of(@Nonnull Object cause)
    {
        checkNotNull(cause, "Cause cannot be null!");
        return new Cause(new Object[]{cause});
    }

    /**
     * Constructs a new cause with the specified event context and causes.
     *
     * @param cause  The direct object cause
     * @param causes Other associated causes
     * @return The built cause
     */
    public static Cause of(@Nonnull Object cause, Object... causes)
    {
        checkNotNull(cause, "Cause cannot be null");
        Builder builder = builder();
        builder.append(cause);
        for (Object namedCause : causes)
        {
            builder.append(namedCause);
        }
        return builder.build();
    }

    /**
     * Constructs a new cause with the specified event context and causes.
     *
     * @param iterable The associated causes
     * @return The built cause
     */
    public static Cause of(@Nonnull Iterable<Object> iterable)
    {
        checkNotNull(iterable, "Iterable cannot be null");
        Builder builder = builder();
        iterable.forEach(builder::append);
        return builder.build();
    }

    /**
     * Gets the root {@link Object} of this cause.
     *
     * @return The root object cause for this cause
     */
    @Nonnull
    public Object root()
    {
        return this.cause[0];
    }

    /**
     * Gets the first <code>T</code> object of this {@link Cause}, if available.
     *
     * @param target The class of the target type
     * @param <T>    The type of object being queried for
     * @return The first element of the type, if available
     */
    @Nonnull
    public <T> Optional<T> first(@Nonnull Class<T> target)
    {
        checkArgument(target != null, "The provided class cannot be null!");
        for (Object aCause : this.cause)
        {
            if (target.isInstance(aCause))
            {
                return Optional.of((T) aCause);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the last object instance of the {@link Class} of type
     * <code>T</code>.
     *
     * @param target The class of the target type
     * @param <T>    The type of object being queried for
     * @return The last element of the type, if available
     */
    @Nonnull
    public <T> Optional<T> last(@Nonnull Class<T> target)
    {
        checkArgument(target != null, "The provided class cannot be null!");
        for (int i = this.cause.length - 1; i >= 0; i--)
        {
            if (target.isInstance(this.cause[i]))
            {
                return Optional.of((T) this.cause[i]);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the object immediately before the object that is an instance of the
     * {@link Class} passed in.
     *
     * @param clazz The class of the object
     * @return The object
     */
    @Nonnull
    public Optional<?> before(@Nonnull Class<?> clazz)
    {
        checkArgument(clazz != null, "The provided class cannot be null!");
        if (this.cause.length == 1)
        {
            return Optional.empty();
        }
        for (int i = 0; i < this.cause.length; i++)
        {
            if (clazz.isInstance(this.cause[i]) && i > 0)
            {
                return Optional.of(this.cause[i - 1]);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the object immediately after the object that is an instance of the
     * {@link Class} passed in.
     *
     * @param clazz The class to type check
     * @return The object after, if available
     */
    @Nonnull
    public Optional<?> after(@Nonnull Class<?> clazz)
    {
        checkArgument(clazz != null, "The provided class cannot be null!");
        if (this.cause.length == 1)
        {
            return Optional.empty();
        }
        for (int i = 0; i < this.cause.length; i++)
        {
            if (clazz.isInstance(this.cause[i]) && i + 1 < this.cause.length)
            {
                return Optional.of(this.cause[i + 1]);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether the target class matches any object of this {@link Cause}
     * .
     *
     * @param target The class of the target type
     * @return True if found, false otherwise
     */
    public boolean containsType(@Nonnull Class<?> target)
    {
        checkArgument(target != null, "The provided class cannot be null!");
        for (Object aCause : this.cause)
        {
            if (target.isInstance(aCause))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if this cause contains of any of the provided {@link Object}. This
     * is the equivalent to checking based on {@link #equals(Object)} for each
     * object in this cause.
     *
     * @param object The object to check if it is contained
     * @return True if the object is contained within this cause
     */
    public boolean contains(@Nonnull Object object)
    {
        checkNotNull(object, "Object cannot be null");
        for (Object aCause : this.cause)
        {
            if (aCause.equals(object))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets an {@link ImmutableList} of all objects that are instances of the
     * given {@link Class} type <code>T</code>.
     *
     * @param <T>    The type of objects to query for
     * @param target The class of the target type
     * @return An immutable list of the objects queried
     */
    @Nonnull
    public <T> List<T> allOf(@Nonnull Class<T> target)
    {
        checkNotNull(target, "Target class cannot be null!");
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (Object aCause : this.cause)
        {
            if (target.isInstance(aCause))
            {
                builder.add((T) aCause);
            }
        }
        return builder.build();
    }

    /**
     * Gets an immutable {@link List} with all object causes that are not
     * instances of the provided {@link Class}.
     *
     * @param ignoredClass The class of object types to ignore
     * @return The list of objects not an instance of the provided class
     */
    @Nonnull
    public List<Object> noneOf(@Nonnull Class<?> ignoredClass)
    {
        checkNotNull(ignoredClass, "Ignored class cannot be null");
        ImmutableList.Builder<Object> builder = ImmutableList.builder();
        for (Object cause : this.cause)
        {
            if (!ignoredClass.isInstance(cause))
            {
                builder.add(cause);
            }
        }
        return builder.build();
    }

    /**
     * Gets an {@link List} of all causes within this {@link Cause}.
     * <p>The ordering is very important as the direct first object
     * within a {@link Cause} is the most direct object that is
     * "causing" an event to be thrown. Additional objects may be
     * considered as "indirect" or "associated" such as
     * players riding an entity that the entity being ridden caused
     * some collisions to occur.</p>
     *
     * @return An immutable list of all the causes
     */
    @Nonnull
    public List<Object> all()
    {
        if (this.immutableCauses == null)
        {
            this.immutableCauses = ImmutableList.copyOf(this.cause);
        }
        return this.immutableCauses;
    }

    /**
     * Creates a new {@link Cause} where the objects are added at the end of the
     * cause array of objects.
     *
     * @param additional The additional object to add
     * @return The new cause
     */
    @Nonnull
    public Cause with(@Nonnull Object additional)
    {
        checkNotNull(additional, "No null arguments allowed!");
        List<Object> list = new ArrayList<>();
        list.add(additional);
        return with(list);
    }

    /**
     * Creates a new {@link Cause} where the objects are added at the end of the
     * cause array of objects.
     *
     * @param additional  The additional object to add
     * @param additionals The remaining objects to add
     * @return The new cause
     */
    @Nonnull
    public Cause with(@Nonnull Object additional, Object... additionals)
    {
        checkNotNull(additional, "No null arguments allowed!");
        List<Object> list = new ArrayList<>();
        list.add(additional);
        for (Object object : additionals)
        {
            checkNotNull(object, "Cannot add null objects!");
            list.add(object);
        }
        return with(list);
    }

    /**
     * Creates a new {@link Cause} where the objects are added at the end of the
     * cause array of objects.
     *
     * @param iterable The additional objects
     * @return The new cause
     */
    @Nonnull
    public Cause with(@Nonnull Iterable<Object> iterable)
    {
        Cause.Builder builder = new Builder().from(this);
        for (Object o : iterable)
        {
            checkNotNull(o, "Cannot add null causes");
            builder.append(o);
        }
        return builder.build();
    }

    /**
     * Merges this cause with the other cause.
     *
     * @param cause The cause to merge with this
     * @return The new merged cause
     */
    @Nonnull
    public Cause with(@Nonnull Cause cause)
    {
        Cause.Builder builder = builder().from(this);
        for (int i = 0; i < cause.cause.length; i++)
        {
            builder.append(cause.cause[i]);
        }
        return builder.build();
    }

    @Override
    @Nonnull
    public Iterator<Object> iterator()
    {
        return new Itr();
    }

    @Override
    public boolean equals(@Nullable Object object)
    {
        if (object instanceof Cause)
        {
            Cause cause = ((Cause) object);
            return Arrays.equals(this.cause, cause.cause);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.cause);
    }

    @Override
    public String toString()
    {
        String causeString = "Cause[Stack={";
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < this.cause.length; i++)
        {
            joiner.add(this.cause[i].toString());
        }
        return causeString + joiner.toString() + "}]";
    }

    public static final class Builder
    {

        final List<Object> causes = new ArrayList<>();

        Builder()
        {

        }

        /**
         * Appends the specified object to the cause.
         *
         * @param cause The object to append to the cause.
         * @return The modified builder, for chaining
         */
        public Builder append(Object cause)
        {
            checkNotNull(cause, "Cause cannot be null!");
            if (!this.causes.isEmpty() && this.causes.get(this.causes.size() - 1) == cause)
            {
                return this;
            }
            this.causes.add(cause);
            return this;
        }

        /**
         * Inserts the specified object into the cause.
         *
         * @param position The position to insert into
         * @param cause    The object to insert into the cause
         * @return The modified builder, for chaining
         */
        public Builder insert(int position, Object cause)
        {
            checkNotNull(cause, "Cause cannot be null!");
            this.causes.add(position, cause);
            return this;
        }

        /**
         * Appends all specified objects onto the cause.
         *
         * @param causes The objects to add onto the cause
         * @return The modified builder, for chaining
         */
        public Builder appendAll(Collection<Object> causes)
        {
            checkNotNull(causes, "Causes cannot be null!");
            causes.forEach(this::append);
            return this;
        }

        /**
         * Constructs a new {@link Cause} with information added to the builder.
         *
         * @return The built cause
         */
        public Cause build()
        {
            checkState(!this.causes.isEmpty(), "Cannot create an empty Cause!");
            return new Cause(this.causes);
        }

        public Builder from(Cause value)
        {
            for (int i = 0; i < value.cause.length; i++)
            {
                this.causes.add(value.cause[i]);
            }
            return this;
        }

        public Builder reset()
        {
            this.causes.clear();
            return this;
        }
    }

    private class Itr implements Iterator<Object>
    {

        private int index = 0;

        Itr()
        {
        }

        @Override
        public Object next()
        {
            if (this.index >= Cause.this.cause.length)
            {
                throw new NoSuchElementException();
            }
            return Cause.this.cause[this.index++];
        }

        @Override
        public boolean hasNext()
        {
            return this.index != Cause.this.cause.length;
        }

    }

}
