package ai.vital.lucene.model

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.FieldInfosFormat;
import org.apache.lucene.codecs.LiveDocsFormat;
import org.apache.lucene.codecs.NormsFormat;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.SegmentInfoFormat;
import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.TermVectorsFormat;
import org.apache.lucene.codecs.lucene46.Lucene46Codec

class CustomLucenceCodec extends Codec {

	private Lucene46Codec _default = new Lucene46Codec()

	/**
	 * Creates a new codec.
	 * <p>
	 * The provided name will be written into the index segment: in order to
	 * for the segment to be read this class should be registered with Java's
	 * SPI mechanism (registered in META-INF/ of your jar file, etc).
	 * @param name must be all ascii alphanumeric, and less than 128 characters in length.
	 */
	protected CustomLucenceCodec(String name) {
		super(name)
	}

	@Override
	public DocValuesFormat docValuesFormat() {
		return _default.docValuesFormat();
	}

	@Override
	public FieldInfosFormat fieldInfosFormat() {
		return _default.fieldInfosFormat();
	}

	@Override
	public LiveDocsFormat liveDocsFormat() {
		return _default.liveDocsFormat();
	}

	@Override
	public NormsFormat normsFormat() {
		return _default.normsFormat();
	}

	@Override
	public PostingsFormat postingsFormat() {
		return _default.postingsFormat();
	}

	@Override
	public SegmentInfoFormat segmentInfoFormat() {
		return _default.segmentInfoFormat();
	}

	@Override
	public StoredFieldsFormat storedFieldsFormat() {
		return _default.storedFieldsFormat();
	}

	@Override
	public TermVectorsFormat termVectorsFormat() {
		return _default.termVectorsFormat();
	}
	
}
