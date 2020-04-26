package org.sirix.index.bplustree;

import com.google.common.io.ByteArrayDataInput;
import org.sirix.api.PageReadOnlyTrx;
import org.sirix.node.interfaces.DataRecord;
import org.sirix.page.AbstractForwardingPage;
import org.sirix.page.PageKind;
import org.sirix.page.PageReference;
import org.sirix.page.SerializationType;
import org.sirix.page.delegates.BitmapReferencesPage;
import org.sirix.page.interfaces.KeyValuePage;
import org.sirix.page.interfaces.Page;
import org.sirix.settings.Constants;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Inner node key/value page.
 *
 * @author Johannes Lichtenberger
 *
 * @param <K> the key
 * @param <V> the value
 */
public class BPlusInnerNodePage<K extends Comparable<? super K> & DataRecord, V extends DataRecord>
    extends AbstractForwardingPage implements KeyValuePage<K, V> {

  /** Key of record page. This is the base key of all contained nodes. */
  private final long mRecordPageKey;

  /** Key/Value records. */
  private final Map<K, V> mRecords;

  /** Sirix {@link PageReadOnlyTrx}. */
  private final PageReadOnlyTrx mPageReadTrx;

  /** Optional left page reference (leaf page). */
  private Optional<PageReference> mLeftPage;

  /** Optional right page reference (inner node page). */
  private Optional<PageReference> mRightPage;

  private final BitmapReferencesPage mDelegate;

  private final PageKind mPageKind;

  /** Determines the node kind. */
  public enum Kind {
    /** Leaf node. */
    LEAF,

    /** Inner node. */
    INNERNODE
  }

  /**
   * Create record page.
   *
   * @param recordPageKey base key assigned to this node page
   * @param pageReadTrx Sirix page reading transaction
   * @param pageKind determines if it's a leaf or inner node page
   * @param previousPageRefKey previous reference
   */
  public BPlusInnerNodePage(final @Nonnegative long recordPageKey, final PageKind pageKind,
      final long previousPageRefKey, final PageReadOnlyTrx pageReadTrx) {
    // Assertions instead of checkNotNull(...) checks as it's part of the
    // internal flow.
    assert recordPageKey >= 0 : "recordPageKey must not be negative!";
    assert pageKind != null;
    assert pageReadTrx != null : "pageReadTrx must not be null!";
    mRecordPageKey = recordPageKey;
    mRecords = new TreeMap<>();
    mPageReadTrx = pageReadTrx;
    mDelegate = new BitmapReferencesPage(Constants.INP_REFERENCE_COUNT);
    mPageKind = pageKind;
  }

  /**
   * Read node page.
   *
   * @param in input bytes to read page from
   * @param pageReadTrx {@link PageReadOnlyTrx}
   */
  protected BPlusInnerNodePage(final ByteArrayDataInput in, final PageReadOnlyTrx pageReadTrx) {
    mDelegate = null;
    // mDelegate = new PageDelegate(Constants.INP_REFERENCE_COUNT, in);
    mRecordPageKey = in.readLong();
    final int size = in.readInt();
    mRecords = new TreeMap<>();
    pageReadTrx.getResourceManager().getResourceConfig();
    for (int offset = 0; offset < size; offset++) {
      new VoidValue();
    }
    assert pageReadTrx != null : "pageReadTrx must not be null!";
    mPageReadTrx = pageReadTrx;
    mPageKind = PageKind.getKind(in.readByte());
  }

  public void setLeftPage(final Optional<PageReference> leftPage) {
    mLeftPage = leftPage;
  }

  public void setRightPage(final Optional<PageReference> rightPage) {
    mLeftPage = rightPage;
  }

  @Override
  public void serialize(final DataOutput out, final SerializationType type) throws IOException {
    super.serialize(out, type);
    out.writeLong(mRecordPageKey);
    out.writeInt(mRecords.size());
    serializePointer(mLeftPage, out);
    serializePointer(mRightPage, out);
    mPageReadTrx.getResourceManager().getResourceConfig();
    // for (final K record : mRecords.keySet()) {
    // persistenter.serialize(out, record, mPageReadTrx);
    // }
    out.writeByte(mPageKind.getID());
  }

  private void serializePointer(final Optional<PageReference> page, final DataOutput out)
      throws IOException {
    if (page.isPresent()) {
      out.writeBoolean(
          page.get().getKey() == org.sirix.settings.Constants.NULL_ID_LONG ? false : true);
    } else {
      out.writeBoolean(false);
    }
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return mRecords.entrySet();
  }

  @Override
  public Collection<V> values() {
    return mRecords.values();
  }

  @Override
  public long getPageKey() {
    return mRecordPageKey;
  }

  @Override
  public V getValue(final K key) {
    return mRecords.get(key);
  }

  @Override
  public void setEntry(final K key, final @Nullable V value) {
    mRecords.put(key, value);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <C extends KeyValuePage<K, V>> C newInstance(final @Nonnegative long recordPageKey,
      final PageKind pageKind, final long previousPageRefKey, final PageReadOnlyTrx pageReadTrx) {
    return (C) new BPlusInnerNodePage<K, V>(recordPageKey, pageKind, previousPageRefKey,
        pageReadTrx);
  }

  @Override
  public PageReadOnlyTrx getPageReadTrx() {
    return mPageReadTrx;
  }

  @Override
  protected Page delegate() {
    return mDelegate;
  }

  @Override
  public PageKind getPageKind() {
    return mPageKind;
  }

  @Override
  public int size() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Set<Entry<K, PageReference>> referenceEntrySet() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setPageReference(K key, PageReference reference) {
    // TODO Auto-generated method stub

  }

  @Override
  public PageReference getPageReference(K key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getPreviousReferenceKey() {
    // TODO Auto-generated method stub
    return -1;
  }
}
