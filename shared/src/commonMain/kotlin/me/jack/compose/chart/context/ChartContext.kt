package me.jack.compose.chart.context

fun ChartContext.isElementAvailable(key: ChartContext.Key<*>): Boolean {
    return null != get(key = key)
}

interface ChartContext {
    /**
     * Key for the elements of [ChartContext]. [E] is a type of element with this key.
     */
    interface Key<E : Element>

    operator fun <E : Element> get(key: Key<E>): E?

    /**
     * Accumulates entries of this context starting with [initial] value and applying [operation]
     * from left to right to current accumulator value and each element of this context.
     */
    fun <R> fold(initial: R, operation: (R, Element) -> R): R

    operator fun plus(context: ChartContext): ChartContext {
        return if (context === ChartContext) this else
            context.fold(this) { acc, element ->
                val removed = acc.minusKey(element.key)
                if (removed === ChartContext) element else {
                    CombinedElement(removed, element)
                }
            }
    }

    /**
     * Returns a context containing elements from this context, but without an element with
     * the specified [key].
     */
    fun minusKey(key: Key<*>): ChartContext

    /**
     * An element of the [ChartContext]. An element of the coroutine context is a singleton context by itself.
     */
    interface Element : ChartContext {
        /**
         * A key of this coroutine context element.
         */
        val key: Key<*>

        override operator fun <E : Element> get(key: Key<E>): E? =
            @Suppress("UNCHECKED_CAST")
            if (this.key == key) this as E else null

        override fun <R> fold(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun minusKey(key: Key<*>): ChartContext =
            if (this.key == key) ChartContext else this
    }

    companion object : ChartContext {
        override fun <E : Element> get(key: Key<E>): E? {
            return null
        }

        override fun <R> fold(initial: R, operation: (R, Element) -> R): R {
            return initial
        }

        override fun plus(context: ChartContext): ChartContext = context
        override fun minusKey(key: Key<*>): ChartContext = this
    }
}


private class CombinedElement(
    private val left: ChartContext,
    private val element: ChartContext.Element
) : ChartContext {

    override fun <E : ChartContext.Element> get(key: ChartContext.Key<E>): E? {
        var cur = this
        while (true) {
            cur.element[key]?.let { return it }
            val next = cur.left
            if (next is CombinedElement) {
                cur = next
            } else {
                return next[key]
            }
        }
    }

    override fun <R> fold(initial: R, operation: (R, ChartContext.Element) -> R): R =
        operation(left.fold(initial, operation), element)

    override fun minusKey(key: ChartContext.Key<*>): ChartContext {
        element[key]?.let { return left }
        val newLeft = left.minusKey(key)
        return when {
            newLeft === left -> this
            newLeft === ChartContext -> element
            else -> CombinedElement(newLeft, element)
        }
    }

    private fun size(): Int {
        var cur = this
        var size = 2
        while (true) {
            cur = cur.left as? CombinedElement ?: return size
            size++
        }
    }

    private fun contains(element: ChartContext.Element): Boolean =
        get(element.key) == element

    private fun containsAll(context: CombinedElement): Boolean {
        var cur = context
        while (true) {
            if (!contains(cur.element)) return false
            val next = cur.left
            if (next is CombinedElement) {
                cur = next
            } else {
                return contains(next as ChartContext.Element)
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is CombinedElement && other.size() == size() && other.containsAll(this)

    override fun hashCode(): Int = left.hashCode() + element.hashCode()

    override fun toString(): String =
        "[" + fold("") { acc, element ->
            if (acc.isEmpty()) element.toString() else "$acc, $element"
        } + "]"
}
