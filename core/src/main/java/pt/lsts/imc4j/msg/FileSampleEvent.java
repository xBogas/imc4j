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
 * Report a sample stored to disk.
 */
public class FileSampleEvent extends Message {
	public static final int ID_STATIC = 1102;

	/**
	 * Type of event.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public FSTYPE fstype = FSTYPE.values()[0];

	/**
	 * File name this event is related to.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String filename = "";

	public String abbrev() {
		return "FileSampleEvent";
	}

	public int mgid() {
		return 1102;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeByte((int)(fstype != null? fstype.value() : 0));
			SerializationUtils.serializePlaintext(_out, filename);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			fstype = FSTYPE.valueOf(buf.get() & 0xFF);
			filename = SerializationUtils.deserializePlaintext(buf);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum FSTYPE {
		FSTYPE_IMAGE(0l),

		FSTYPE_AUDIO(1l),

		FSTYPE_SONAR(2l),

		FSTYPE_OTHER(255l);

		protected long value;

		FSTYPE(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static FSTYPE valueOf(long value) throws IllegalArgumentException {
			for (FSTYPE v : FSTYPE.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for FSTYPE: "+value);
		}
	}
}
