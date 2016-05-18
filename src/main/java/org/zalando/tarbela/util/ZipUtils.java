package org.zalando.tarbela.util;

import static com.google.common.collect.Lists.newArrayListWithCapacity;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.google.common.base.Preconditions;

/**
 * Supports iterating over two lists in parallel. Adapted from http://stackoverflow.com/a/1365837/600500.
 */
public class ZipUtils {

    /**
     * Iterates two iterables in parallel, passing each pair of items to a BiConsumer. This will stop as soon as one of
     * the iterables runs out of elements.
     *
     * @param   ct    the first iterable.
     * @param   cu    the second iterable.
     * @param   each  the consumer to call for each pair of items.
     *
     * @return  {@code true} if both iterables are exhausted, {@code false} if one of them has still items in it.
     */
    public static <T, U> boolean forEachPair(final Iterable<T> ct, final Iterable<U> cu, final BiConsumer<T, U> each) {
        final Iterator<T> it = ct.iterator();
        final Iterator<U> iu = cu.iterator();
        while (it.hasNext() && iu.hasNext()) {
            each.accept(it.next(), iu.next());
        }

        return !it.hasNext() && !iu.hasNext();
    }

    /**
     * Maps pairs of a entries from each list into a new (mutable) list. This happens in a non-lazy way (i.e. a fully
     * constructed list is returned), changes on the result list will not reflect back on the original list. Both
     * argument lists need to have the same size, which will also be the size of the returned list.
     *
     * @param   tList    first argument list
     * @param   uList    second argument list
     * @param   mapping  a mapping function, mapping a pair of elements into a new element.
     *
     * @return  the list of the results of the mapping.
     */
    public static <T, U, R> List<? extends R> mapPairs(final List<T> tList, final List<U> uList,
            final BiFunction<T, U, R> mapping) {
        Preconditions.checkArgument(tList.size() == uList.size(),
            "argument lists don't have the same size! (tList.size = %s <> %s = uList.size)", tList.size(),
            uList.size());

        final List<R> result = newArrayListWithCapacity(tList.size());
        forEachPair(tList, uList, (t, u) -> result.add(mapping.apply(t, u)));
        return result;
    }
}
