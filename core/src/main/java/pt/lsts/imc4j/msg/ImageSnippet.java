package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;
import pt.lsts.imc4j.util.SerializationUtils;

/**
 * Small image encoded as several frames (transferable over low bandwidth lossy links).
 */
public class ImageSnippet extends Message {
	public static final int ID_STATIC = 704;

	@FieldType(
			type = IMCField.TYPE_UINT16
	)
	public int snippet_id = 0;

	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int total_frames = 0;

	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int frame_id = 0;

	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public CODEC codec = CODEC.values()[0];

	@FieldType(
			type = IMCField.TYPE_RAWDATA
	)
	public byte[] data = new byte[0];

	public String abbrev() {
		return "ImageSnippet";
	}

	public int mgid() {
		return 704;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeShort(snippet_id);
			_out.writeByte(total_frames);
			_out.writeByte(frame_id);
			_out.writeByte((int)(codec != null? codec.value() : 0));
			SerializationUtils.serializeRawdata(_out, data);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			snippet_id = buf.getShort() & 0xFFFF;
			total_frames = buf.get() & 0xFF;
			frame_id = buf.get() & 0xFF;
			codec = CODEC.valueOf(buf.get() & 0xFF);
			data = SerializationUtils.deserializeRawdata(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum CODEC {
		CODEC_JPEG2000(0l);

		protected long value;

		CODEC(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static CODEC valueOf(long value) throws IllegalArgumentException {
			for (CODEC v : CODEC.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for CODEC: "+value);
		}
	}
}
