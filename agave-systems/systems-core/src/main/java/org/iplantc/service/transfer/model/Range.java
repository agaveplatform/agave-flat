package org.iplantc.service.transfer.model;

/**
 * Describes a range of bytes.
 * 
 * @author Jerome Louvel
 */
public class Range {

    /**
     * Index for the first byte of an entity.
     */
    public final static long INDEX_FIRST = 0;

    /**
     * Index for the last byte of an entity.
     */
    public final static long INDEX_LAST = -1;

    /**
     * Maximum size available from the index.
     */
    public final static long SIZE_MAX = -1;

    /**
     * Index from which to start the range. If the index is superior or equal to
     * zero, the index will define the start of the range. If its value is
     * {@value #INDEX_LAST} (-1), then it defines the end of the range. The
     * default value is {@link #INDEX_FIRST} (0), starting at the first byte.
     */
    private volatile long index;

    /**
     * Size of the range in number of bytes. If the size is the maximum
     * available from the index, then use the {@value #SIZE_MAX} constant.
     */
    private volatile long size;

    /**
     * Default constructor defining a range starting on the first byte and with
     * a maximum size, i.e. covering the whole entity.
     */
    public Range() {
        this(INDEX_FIRST, SIZE_MAX);
    }

    /**
     * Constructor defining a range starting on the first byte and with the
     * given size.
     * 
     * @param size
     *            Size of the range in number of bytes.
     */
    public Range(long size) {
        this(INDEX_FIRST, size);
    }

    /**
     * Constructor.
     * 
     * @param index
     *            Index from which to start the range
     * @param size
     *            Size of the range in number of bytes.
     */
    public Range(long index, long size) {
        this.index = index;
        this.size = size;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof Range)
                && ((Range) object).getIndex() == getIndex()
                && ((Range) object).getSize() == getSize();
    }

    /**
     * Returns the index from which to start the range. If the index is superior
     * or equal to zero, the index will define the start of the range. If its
     * value is {@value #INDEX_LAST} (-1), then it defines the end of the range.
     * The default value is {@link #INDEX_FIRST} (0), starting at the first
     * byte.
     * 
     * @return The index from which to start the range.
     */
    public long getIndex() {
        return index;
    }

    /**
     * Returns the size of the range in number of bytes. If the size is the
     * maximum available from the index, then use the {@value #SIZE_MAX}
     * constant.
     * 
     * @return The size of the range in number of bytes.
     */
    public long getSize() {
        return size;
    }

    /**
     * Indicates if the given index is included in the range.
     * 
     * @param position
     *            The position to test.
     * @param totalSize
     * 
     * @return True if the given index is included in the range, false
     *         otherwise.
     */
    public boolean isIncluded(long position, long totalSize) {
        boolean result = false;

        if (getIndex() == INDEX_LAST) {
            // The range starts from the end
            result = (0 <= position) && (position < totalSize);

            if (result) {
                result = position >= (totalSize - getSize());
            }
        } else {
            // The range starts from the beginning
            result = position >= getIndex();

            if (result && (getSize() != SIZE_MAX)) {
                result = position < getIndex() + getSize();
            }
        }

        return result;
    }

    /**
     * Sets the index from which to start the range. If the index is superior or
     * equal to zero, the index will define the start of the range. If its value
     * is {@value #INDEX_LAST} (-1), then it defines the end of the range. The
     * default value is {@link #INDEX_FIRST} (0), starting at the first byte
     * 
     * @param index
     *            The index from which to start the range.
     */
    public void setIndex(long index) {
        this.index = index;
    }

    /**
     * Sets the size of the range in number of bytes. If the size is the maximum
     * available from the index, then use the {@value #SIZE_MAX} constant.
     * 
     * @param size
     *            The size of the range in number of bytes.
     */
    public void setSize(long size) {
        this.size = size;
    }
}